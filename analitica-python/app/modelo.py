"""
Modelos predictivos de asistencia.

Se entrenan dos modelos sobre el histórico de eventos finalizados:

1. Modelo de INSCRIPTOS: estima cuántos inscriptos tendrá un evento al cerrar la inscripción
   (sirve para eventos a varias semanas vista, cuando todavía se inscribe gente).
2. Modelo de TASA DE ASISTENCIA (asistentes / inscriptos). Se comparan por validación cruzada
   una regresión lineal regularizada (Ridge) y un Random Forest, y se queda el de menor error.

Retroalimentación con la encuesta de satisfacción: cada evento lleva como variable la
"satisfacción previa" de su organizador, es decir, el puntaje medio (1 a 5) que dieron los
asistentes a sus eventos ANTERIORES. Sólo se usan eventos con fecha anterior, para no "espiar"
el resultado del propio evento, y se suaviza hacia el promedio general cuando hay pocos datos.

Asistencia estimada = inscriptos proyectados × tasa estimada.
La incertidumbre se modela como normal (desvío = error residual de validación cruzada
+ dispersión entre árboles del bosque), y de ahí salen el intervalo del 80 %,
la probabilidad de sobrecapacidad y la de baja asistencia.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field
from datetime import date, datetime
from zoneinfo import ZoneInfo

import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import KFold, cross_val_predict
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from .config import UMBRAL_BAJA_ASISTENCIA

CATEGORICAS = ["tipo", "facultad", "clima"]
NUMERICAS_BASE = ["hora", "dia_semana", "mes", "duracion_min", "capacidad_sala", "satisfaccion_previa"]
NUMERICAS_TASA = NUMERICAS_BASE + ["inscriptos"]
ZONA = ZoneInfo("America/Argentina/Buenos_Aires")
Z80 = 1.2816  # cuantil normal para intervalo central del 80 %
SATISF_DEFECTO = 4.0  # puntaje supuesto cuando todavía no hay ninguna encuesta respondida
SUAVIZADO = 3        # "eventos ficticios" con el promedio general que se suman al del organizador

NOMBRES_VARIABLES = {
    "tipo": "Tipo de evento", "facultad": "Facultad", "clima": "Clima", "hora": "Horario",
    "dia_semana": "Día de la semana", "mes": "Mes", "duracion_min": "Duración",
    "capacidad_sala": "Capacidad de la sala", "inscriptos": "Inscriptos",
    "satisfaccion_previa": "Satisfacción previa",
}
PLURAL = {"CHARLA": "charlas", "SEMINARIO": "seminarios", "TALLER": "talleres", "JORNADA": "jornadas",
          "INSTITUCIONAL": "eventos institucionales"}
DIAS = ["lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"]


def _phi(x: float) -> float:
    """Función de distribución normal estándar."""
    return 0.5 * (1.0 + math.erf(x / math.sqrt(2.0)))


def preparar(df: pd.DataFrame) -> pd.DataFrame:
    """Agrega variables derivadas de fecha y hora."""
    d = df.copy()
    d["fecha"] = pd.to_datetime(d["fecha"])
    h = d["hora_inicio"].str.slice(0, 2).astype(int)
    m = d["hora_inicio"].str.slice(3, 5).astype(int)
    d["hora"] = h + m / 60.0
    d["dia_semana"] = d["fecha"].dt.weekday
    d["mes"] = d["fecha"].dt.month
    if "capacidad_sala" in d:
        d["capacidad_sala"] = d["capacidad_sala"].astype(float)
    return d


def satisfaccion_media(historico: pd.DataFrame) -> float:
    """Puntaje medio de todos los eventos con encuesta (o el valor por defecto si no hay)."""
    if "satisfaccion" not in historico:
        return SATISF_DEFECTO
    s = pd.to_numeric(historico["satisfaccion"], errors="coerce").dropna()
    return float(s.mean()) if len(s) else SATISF_DEFECTO


def satisfaccion_previa(historico: pd.DataFrame, organizador_id, antes_de, media: float) -> tuple[float, int]:
    """Satisfacción de los eventos del organizador anteriores a `antes_de`, suavizada hacia `media`.

    Devuelve (valor, cantidad de eventos con encuesta usados)."""
    if organizador_id is None or pd.isna(organizador_id) or "organizador_id" not in historico:
        return media, 0
    h = historico
    previos = pd.to_numeric(h.loc[(h["organizador_id"] == organizador_id) & (h["fecha"] < pd.Timestamp(antes_de)),
                                  "satisfaccion"], errors="coerce").dropna()
    n = len(previos)
    return (float(previos.sum()) + SUAVIZADO * media) / (n + SUAVIZADO), n


def agregar_satisfaccion_previa(df: pd.DataFrame) -> tuple[pd.DataFrame, float]:
    """Agrega la columna satisfaccion_previa a cada registro del histórico (sin fuga de datos)."""
    d = df.copy()
    for col in ("organizador_id", "satisfaccion"):
        if col not in d:
            d[col] = np.nan
    media = satisfaccion_media(d)
    d["satisfaccion_previa"] = [satisfaccion_previa(d, r.organizador_id, r.fecha, media)[0]
                                for r in d.itertuples()]
    return d, media


def _pipeline(estimador, numericas, escalar: bool) -> Pipeline:
    num = StandardScaler() if escalar else "passthrough"
    pre = ColumnTransformer([
        ("cat", OneHotEncoder(handle_unknown="ignore"), CATEGORICAS),
        ("num", num, numericas),
    ])
    return Pipeline([("pre", pre), ("modelo", estimador)])


def _bosque() -> RandomForestRegressor:
    return RandomForestRegressor(n_estimators=300, min_samples_leaf=3, random_state=42, n_jobs=-1)


@dataclass
class ModeloAsistencia:
    """Estado entrenado (se persiste completo con joblib)."""
    modelo_tasa: Pipeline
    nombre_modelo_tasa: str
    sigma_tasa: float
    modelo_inscriptos: Pipeline
    sigma_inscriptos_rel: float
    historico: pd.DataFrame
    metricas: dict
    importancias: list[dict]
    satisfaccion_media: float = SATISF_DEFECTO
    entrenado_en: str = field(default_factory=lambda: datetime.now(ZONA).isoformat(timespec="seconds"))

    # ------------------------------------------------------------------ entrenamiento
    @classmethod
    def entrenar(cls, registros: list[dict]) -> "ModeloAsistencia":
        df = preparar(pd.DataFrame(registros))
        df = df[df["inscriptos"] > 0].reset_index(drop=True)
        df, media_satisf = agregar_satisfaccion_previa(df)
        df["tasa"] = (df["asistentes"] / df["inscriptos"]).clip(0, 1)
        n = len(df)
        kf = KFold(n_splits=min(5, n), shuffle=True, random_state=42)

        # --- Tasa de asistencia: Ridge vs Random Forest
        X, y = df[CATEGORICAS + NUMERICAS_TASA], df["tasa"]
        candidatos = {
            "regresion_ridge": _pipeline(Ridge(alpha=1.0), NUMERICAS_TASA, escalar=True),
            "random_forest": _pipeline(_bosque(), NUMERICAS_TASA, escalar=False),
        }
        metricas, preds_cv = {}, {}
        for nombre, pipe in candidatos.items():
            p = np.clip(cross_val_predict(pipe, X, y, cv=kf), 0, 1)
            preds_cv[nombre] = p
            asist_pred = p * df["inscriptos"]
            metricas[nombre] = {
                "mae_tasa": round(float(mean_absolute_error(y, p)), 4),
                "mae_asistentes": round(float(mean_absolute_error(df["asistentes"], asist_pred)), 2),
                "r2_asistentes": round(float(r2_score(df["asistentes"], asist_pred)), 3),
            }
        # Línea base ingenua: tasa media histórica del tipo de evento
        base = df.groupby("tipo")["tasa"].transform("mean")
        metricas["linea_base_media_por_tipo"] = {
            "mae_tasa": round(float(mean_absolute_error(y, base)), 4),
            "mae_asistentes": round(float(mean_absolute_error(df["asistentes"], base * df["inscriptos"])), 2),
            "r2_asistentes": round(float(r2_score(df["asistentes"], base * df["inscriptos"])), 3),
        }
        elegido = min(candidatos, key=lambda k: metricas[k]["mae_asistentes"])
        sigma_tasa = float(np.std(y - preds_cv[elegido]))
        modelo_tasa = candidatos[elegido].fit(X, y)

        # --- Inscriptos (sin conocer los inscriptos, obviamente)
        Xi, yi = df[CATEGORICAS + NUMERICAS_BASE], df["inscriptos"]
        pipe_i = _pipeline(_bosque(), NUMERICAS_BASE, escalar=False)
        pi = cross_val_predict(pipe_i, Xi, yi, cv=kf)
        metricas["inscriptos_random_forest"] = {
            "mae": round(float(mean_absolute_error(yi, pi)), 2),
            "r2": round(float(r2_score(yi, pi)), 3),
        }
        sigma_insc_rel = float(np.std((yi - pi) / np.maximum(pi, 1)))
        modelo_insc = pipe_i.fit(Xi, yi)

        metricas["modelo_elegido"] = elegido
        metricas["registros"] = n
        con_encuesta = df["satisfaccion"].notna()
        metricas["encuestas"] = {
            "eventos_con_encuesta": int(con_encuesta.sum()),
            "respuestas": int(pd.to_numeric(df["respuestas"], errors="coerce").fillna(0).sum())
                          if "respuestas" in df else 0,
            "satisfaccion_media": round(media_satisf, 2),
        }
        return cls(
            modelo_tasa=modelo_tasa, nombre_modelo_tasa=elegido, sigma_tasa=sigma_tasa,
            modelo_inscriptos=modelo_insc, sigma_inscriptos_rel=sigma_insc_rel,
            historico=df, metricas=metricas, importancias=cls._importancias(modelo_tasa, elegido),
            satisfaccion_media=media_satisf,
        )

    @staticmethod
    def _importancias(pipe: Pipeline, nombre: str) -> list[dict]:
        """Importancia por variable original (suma las columnas one-hot de cada categórica)."""
        pre, est = pipe.named_steps["pre"], pipe.named_steps["modelo"]
        cols = pre.get_feature_names_out()
        if nombre == "random_forest":
            valores = est.feature_importances_
        else:
            valores = np.abs(est.coef_)
            valores = valores / valores.sum() if valores.sum() else valores
        acumulado: dict[str, float] = {}
        for c, v in zip(cols, valores):
            _, resto = c.split("__", 1)
            var = next((cat for cat in CATEGORICAS if resto.startswith(cat + "_")), resto)
            acumulado[var] = acumulado.get(var, 0.0) + float(v)
        total = sum(acumulado.values()) or 1.0
        return sorted(({"variable": k, "nombre": NOMBRES_VARIABLES.get(k, k), "importancia": round(v / total, 4)}
                       for k, v in acumulado.items()), key=lambda x: -x["importancia"])

    # ------------------------------------------------------------------ predicción
    def predecir(self, ev: dict, salas: list[dict], hoy: date | None = None) -> dict:
        hoy = hoy or datetime.now(ZONA).date()
        cap_ref = ev.get("capacidad_sala") or ev["cupo"]
        satisf, n_satisf = satisfaccion_previa(self.historico, ev.get("organizador_id"), ev["fecha"],
                                               self.satisfaccion_media)
        fila = preparar(pd.DataFrame([{**ev, "capacidad_sala": cap_ref, "satisfaccion_previa": satisf}]))
        insc_act = int(ev["inscriptos"])

        # 1) Inscriptos proyectados al cierre: se combina lo real con el modelo según la anticipación
        dias = (pd.Timestamp(ev["fecha"]).date() - hoy).days
        insc_modelo = float(self.modelo_inscriptos.predict(fila[CATEGORICAS + NUMERICAS_BASE])[0])
        peso_modelo = min(max(dias / 21.0, 0.0), 1.0)
        insc_proy = (1 - peso_modelo) * insc_act + peso_modelo * max(insc_act, insc_modelo)
        insc_proy = float(min(max(insc_proy, insc_act), ev["cupo"]))
        if insc_proy < 1:
            insc_proy = float(max(insc_modelo, 1.0)) if insc_act == 0 else float(insc_act)
            insc_proy = min(insc_proy, ev["cupo"])

        # 2) Tasa de asistencia
        fila["inscriptos"] = insc_proy
        Xt = fila[CATEGORICAS + NUMERICAS_TASA]
        tasa = float(np.clip(self.modelo_tasa.predict(Xt)[0], 0.02, 1.0))
        sigma = self.sigma_tasa
        if self.nombre_modelo_tasa == "random_forest":
            Xp = self.modelo_tasa.named_steps["pre"].transform(Xt)
            arboles = np.array([a.predict(Xp)[0] for a in self.modelo_tasa.named_steps["modelo"].estimators_])
            sigma = math.sqrt(sigma ** 2 + float(np.var(arboles)))

        # 3) Asistencia y su incertidumbre
        mu = tasa * insc_proy
        sd_insc = peso_modelo * self.sigma_inscriptos_rel * insc_proy * tasa
        sd = max(math.sqrt((sigma * insc_proy) ** 2 + sd_insc ** 2), 1.0)
        inf = int(max(0, round(mu - Z80 * sd)))
        sup = int(min(ev["cupo"], round(mu + Z80 * sd)))
        p_sobre = 1 - _phi((cap_ref + 0.5 - mu) / sd)
        p_baja = _phi((UMBRAL_BAJA_ASISTENCIA * cap_ref - mu) / sd)

        # 4) Sala recomendada: la más chica que cubra el percentil 90 de asistencia
        objetivo = mu + Z80 * sd
        candidatas = [s for s in salas if s.get("facultad") in (None, ev["facultad"])] or salas
        rec = None
        if candidatas:
            aptas = [s for s in candidatas if s["capacidad"] >= objetivo]
            rec = min(aptas, key=lambda s: s["capacidad"]) if aptas else max(candidatas, key=lambda s: s["capacidad"])
        sala_ok = None
        if ev.get("capacidad_sala"):
            sala_ok = bool(ev["capacidad_sala"] >= objetivo and UMBRAL_BAJA_ASISTENCIA * ev["capacidad_sala"] <= mu)

        return {
            "evento_id": ev["evento_id"],
            "asistencia_estimada": int(round(mu)),
            "intervalo_inferior": inf,
            "intervalo_superior": max(sup, inf),
            "prob_sobrecapacidad": round(float(p_sobre), 4),
            "prob_baja_asistencia": round(float(p_baja), 4),
            "inscriptos_actuales": insc_act,
            "inscriptos_proyectados": int(round(insc_proy)),
            "tasa_estimada": round(tasa, 4),
            "capacidad_referencia": int(cap_ref),
            "ocupacion_estimada": round(mu / cap_ref, 4),
            "sala_recomendada_id": rec["id"] if rec else None,
            "sala_recomendada": ({"id": rec["id"], "nombre": rec["nombre"], "capacidad": rec["capacidad"],
                                  "ocupacion_estimada": round(mu / rec["capacidad"], 4)} if rec else None),
            "sala_actual_adecuada": sala_ok,
            "modelo": self.nombre_modelo_tasa,
            "satisfaccion_previa": round(satisf, 2),
            "explicacion": self._explicar(ev, fila.iloc[0], insc_act, insc_proy, dias, tasa, mu, sd, cap_ref, rec,
                                          sala_ok, satisf, n_satisf),
        }

    def _explicar(self, ev, fila, insc_act, insc_proy, dias, tasa, mu, sd, cap, rec, sala_ok,
                  satisf=None, n_satisf=0) -> list[str]:
        h = self.historico
        msgs = []
        if dias > 0 and round(insc_proy) > insc_act:
            msgs.append(f"Faltan {dias} días: con {insc_act} inscriptos hoy se proyectan {round(insc_proy)} "
                        f"al cierre de la inscripción.")
        else:
            msgs.append(f"Se toman los {insc_act} inscriptos actuales.")
        tipo_hist = h[h["tipo"] == ev["tipo"]]["tasa"]
        if len(tipo_hist):
            msgs.append(f"Tasa de asistencia estimada: {tasa:.0%} (promedio histórico de "
                        f"{PLURAL[ev['tipo']]}: {tipo_hist.mean():.0%}).")
        if ev["clima"] == "LLUVIA":
            ll, ot = h[h["clima"] == "LLUVIA"]["tasa"], h[h["clima"] != "LLUVIA"]["tasa"]
            if len(ll) and len(ot):
                msgs.append(f"Con lluvia la asistencia histórica cae ~{(ot.mean() - ll.mean()) * 100:.0f} puntos.")
        if n_satisf:
            media = self.satisfaccion_media
            if satisf >= media + 0.1:
                efecto = "suele asistir más gente"
            elif satisf <= media - 0.1:
                efecto = "suele asistir menos gente"
            else:
                efecto = "en línea con el promedio"
            def coma(x):
                return f"{x:.1f}".replace(".", ",")
            msgs.append(f"Encuestas: los {n_satisf} eventos anteriores de este organizador tienen una satisfacción "
                        f"de {coma(satisf)}/5 (promedio general {coma(media)}/5): {efecto}.")
        dia = int(fila["dia_semana"])
        por_dia = h.groupby("dia_semana")["tasa"].mean()
        if dia in por_dia and por_dia[dia] < h["tasa"].mean() - 0.03:
            msgs.append(f"Los {DIAS[dia]} suelen tener menor asistencia ({por_dia[dia]:.0%}).")
        if sala_ok is False:
            p90 = mu + Z80 * sd
            if p90 > cap:
                msgs.append(f"La sala asignada ({cap} lugares) puede quedar chica: en un escenario alto "
                            f"asistirían hasta {round(p90)} personas.")
            else:
                msgs.append(f"La sala asignada ({cap} lugares) quedaría ocupada al {mu / cap:.0%}: "
                            f"está sobredimensionada.")
        if rec:
            msgs.append(f"Sala recomendada: {rec['nombre']} ({rec['capacidad']} lugares).")
        return msgs

    # ------------------------------------------------------------------ estado
    def estado(self) -> dict:
        return {
            "entrenado": True,
            "entrenado_en": self.entrenado_en,
            "registros": len(self.historico),
            "modelo_elegido": self.nombre_modelo_tasa,
            "metricas": self.metricas,
            "importancias": self.importancias,
            "satisfaccion_media": round(self.satisfaccion_media, 2),
        }

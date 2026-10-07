"""
Tendencias de participación (series temporales).

- Serie mensual por grupo (tipo de evento o facultad) de tasa de asistencia o asistentes promedio,
  suavizada con una media móvil de 3 meses ponderada por inscriptos.
- Pronóstico de los próximos meses con suavizado exponencial de Holt (nivel + tendencia).
"""
from __future__ import annotations

import numpy as np
import pandas as pd

DIAS = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"]


def holt(valores: list[float], horizonte: int, alfa: float = 0.5, beta: float = 0.2) -> list[float]:
    """Suavizado exponencial doble de Holt. Devuelve `horizonte` valores futuros."""
    v = [x for x in valores if x is not None and not np.isnan(x)]
    if not v:
        return []
    if len(v) == 1:
        return [v[0]] * horizonte
    nivel, tend = v[0], v[1] - v[0]
    for x in v[1:]:
        prev = nivel
        nivel = alfa * x + (1 - alfa) * (nivel + tend)
        tend = beta * (nivel - prev) + (1 - beta) * tend
    return [nivel + (k + 1) * tend for k in range(horizonte)]


def serie_mensual(h: pd.DataFrame, agrupar: str, metrica: str, horizonte: int) -> dict:
    d = h.copy()
    d["periodo"] = d["fecha"].dt.to_period("M")
    todos = pd.period_range(d["periodo"].min(), d["periodo"].max(), freq="M")
    grupos = sorted(d[agrupar].unique())
    datos = {str(p): {"periodo": str(p)} for p in todos}
    pronostico = {}
    futuros = pd.period_range(todos[-1] + 1, periods=horizonte, freq="M")
    for p in futuros:
        pronostico[str(p)] = {"periodo": str(p)}

    for g in grupos:
        m = (d[d[agrupar] == g].groupby("periodo")
             .agg(inscriptos=("inscriptos", "sum"), asistentes=("asistentes", "sum"), eventos=("evento_id", "count"))
             .reindex(todos, fill_value=0))
        ins = m["inscriptos"].rolling(3, min_periods=1).sum()
        asi = m["asistentes"].rolling(3, min_periods=1).sum()
        evs = m["eventos"].rolling(3, min_periods=1).sum()
        if metrica == "tasa":
            valor = (asi / ins.replace(0, np.nan)).round(4)
        else:
            valor = (asi / evs.replace(0, np.nan)).round(1)
        for p, v in valor.items():
            datos[str(p)][g] = None if pd.isna(v) else float(v)
        for p, v in zip(futuros, holt(valor.tolist(), horizonte)):
            v = max(0.0, min(1.0, v)) if metrica == "tasa" else max(0.0, v)
            pronostico[str(p)][g] = round(float(v), 4 if metrica == "tasa" else 1)

    # el último punto real también se incluye en el pronóstico, para que la línea sea continua
    ultimo = dict(datos[str(todos[-1])])
    return {
        "agrupar": agrupar,
        "metrica": metrica,
        "grupos": [str(g) for g in grupos],
        "datos": list(datos.values()),
        "pronostico": [ultimo] + list(pronostico.values()),
        "metodo": "Media móvil de 3 meses + suavizado exponencial de Holt",
    }


def resumen(h: pd.DataFrame) -> dict:
    def agg(col, etiquetas=None):
        g = (h.groupby(col).agg(eventos=("evento_id", "count"), inscriptos=("inscriptos", "sum"),
                                asistentes=("asistentes", "sum"), asistentes_prom=("asistentes", "mean"),
                                ocupacion=("ocupacion", "mean"))
             .reset_index())
        filas = []
        for _, r in g.iterrows():
            clave = r[col]
            filas.append({
                "grupo": etiquetas[int(clave)] if etiquetas else (f"{int(clave):02d}:00" if col == "hora_int" else str(clave)),
                "eventos": int(r["eventos"]),
                "tasa_asistencia": round(float(r["asistentes"] / r["inscriptos"]), 4) if r["inscriptos"] else 0,
                "asistentes_promedio": round(float(r["asistentes_prom"]), 1),
                "ocupacion_sala_promedio": round(float(r["ocupacion"]), 4),
            })
        return filas

    h = h.copy()
    h["hora_int"] = h["hora"].astype(int)
    h["ocupacion"] = h["asistentes"] / h["capacidad_sala"]
    return {
        "totales": {
            "eventos": int(len(h)),
            "inscriptos": int(h["inscriptos"].sum()),
            "asistentes": int(h["asistentes"].sum()),
            "tasa_asistencia": round(float(h["asistentes"].sum() / max(h["inscriptos"].sum(), 1)), 4),
            "eventos_sobrecapacidad": int((h["asistentes"] > h["capacidad_sala"]).sum()),
            "eventos_baja_asistencia": int((h["ocupacion"] < 0.4).sum()),
        },
        "por_tipo": agg("tipo"),
        "por_facultad": agg("facultad"),
        "por_horario": agg("hora_int"),
        "por_dia": agg("dia_semana", DIAS),
        "por_clima": agg("clima"),
    }

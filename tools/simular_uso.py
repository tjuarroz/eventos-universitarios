"""
Simulador de uso real de la plataforma.

Usa la API REST del core Java exactamente como lo haría el front-end, con usuarios reales del sistema.
Hace lo que pasaría en unas semanas de uso:

  1. Altas: se registran estudiantes nuevos y el administrador da de alta una sala.
  2. Planificación: los organizadores crean eventos próximos (algunos quedan en borrador) y los publican.
  3. Inscripciones: estudiantes, docentes y personal se inscriben según lo "atractivo" de cada evento;
     algunos cancelan.
  4. Eventos de HOY: se crean, se llenan de inscriptos, el personal de apoyo registra la asistencia
     (QR, código o lista) y el organizador los finaliza. Pasan a formar parte del historial.
  5. Analítica: se reentrena el modelo con el historial nuevo y se generan las predicciones de todos
     los eventos próximos.

Sólo usa la librería estándar de Python.

Con Docker (recomendado, no hace falta instalar nada):
    docker compose run --rm simulador

Desde tu máquina, con Python 3.10+ y el sistema levantado:
    python tools/simular_uso.py --api http://localhost:8080

Opciones:  --futuros 12  --hoy 4  --nuevos 25  --semilla 7   (python tools/simular_uso.py -h)
           --llenar 180 --cantidad 50   (sólo inscribe 50 personas al evento 180)
"""
from __future__ import annotations

import argparse
import json
import random
import string
import sys
import time
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta

CLAVE = "demo1234"
ORGANIZADORES = ["organizador@uni.edu.ar", "dferrari@uni.edu.ar", "mquiroga@uni.edu.ar", "piglesias@uni.edu.ar"]
FACULTADES = ["Ingeniería", "Ciencias Económicas", "Derecho", "Ciencias Sociales", "Ciencias Exactas"]

# tipo: (peso, inscriptos esperados, tasa de asistencia, duraciones en minutos)
TIPOS = {
    "CHARLA": (0.34, 60, 0.62, [60, 90]),
    "SEMINARIO": (0.22, 40, 0.70, [120, 180]),
    "TALLER": (0.24, 22, 0.82, [120, 180]),
    "JORNADA": (0.12, 110, 0.55, [240, 300]),
    "INSTITUCIONAL": (0.08, 120, 0.50, [90, 120]),
}
TEMAS = {
    "CHARLA": ["IA en la vida cotidiana", "Seguridad informática para estudiantes", "Primer empleo en tecnología",
               "Economía del conocimiento", "Ciudades inteligentes", "Salud mental en la universidad",
               "Startups universitarias", "Datos abiertos y transparencia", "Energías renovables en Argentina"],
    "SEMINARIO": ["Seminario de Ingeniería de Software", "Derecho y nuevas tecnologías", "Finanzas corporativas",
                  "Políticas educativas comparadas", "Estadística bayesiana", "Metodologías ágiles avanzadas"],
    "TALLER": ["Taller de React desde cero", "Taller de oratoria", "Taller de LinkedIn y búsqueda laboral",
               "Taller de Power BI", "Taller de SQL práctico", "Taller de escritura de tesis", "Taller de Figma"],
    "JORNADA": ["Jornada de Proyectos Finales", "Jornada de Vinculación con Empresas", "Jornada de Puertas Abiertas",
                "Jornada de Investigación Estudiantil"],
    "INSTITUCIONAL": ["Acto de colación de grado", "Presentación del calendario académico",
                      "Bienvenida a estudiantes de intercambio", "Asamblea informativa"],
}
NOMBRES = ["Valentino", "Emma", "Bautista", "Olivia", "Felipe", "Isabella", "Juan Cruz", "Renata", "Gael", "Pilar",
           "Ciro", "Victoria", "Dante", "Josefina", "León", "Guadalupe", "Máximo", "Morena", "Ignacio", "Zoe"]
APELLIDOS = ["Barrios", "Ledesma", "Villalba", "Quiroga", "Figueroa", "Correa", "Vega", "Coronel", "Paz", "Arias",
             "Rojas", "Godoy", "Domínguez", "Ojeda", "Cáceres", "Maidana", "Ferreyra", "Vera", "Toledo", "Peralta"]


# ------------------------------------------------------------------ cliente HTTP
class Api:
    def __init__(self, base: str):
        self.base = base.rstrip("/")
        self.tokens: dict[str, str] = {}

    def _pedir(self, metodo, ruta, token=None, cuerpo=None):
        datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
        req = urllib.request.Request(self.base + ruta, data=datos, method=metodo)
        req.add_header("Accept", "application/json")
        if datos is not None:
            req.add_header("Content-Type", "application/json")
        if token:
            req.add_header("Authorization", "Bearer " + token)
        try:
            with urllib.request.urlopen(req, timeout=120) as r:
                txt = r.read().decode()
                return r.status, (json.loads(txt) if txt else None)
        except urllib.error.HTTPError as e:
            txt = e.read().decode(errors="replace")
            try:
                return e.code, json.loads(txt)
            except ValueError:
                return e.code, {"mensaje": txt[:200]}

    def login(self, email):
        if email not in self.tokens:
            st, r = self._pedir("POST", "/api/auth/login", cuerpo={"email": email, "password": CLAVE})
            if st != 200:
                raise RuntimeError(f"No se pudo iniciar sesión como {email}: {r}")
            self.tokens[email] = r["token"]
        return self.tokens[email]

    def como(self, email, metodo, ruta, cuerpo=None):
        return self._pedir(metodo, ruta, self.login(email), cuerpo)

    def esperar(self, intentos=60):
        for _ in range(intentos):
            try:
                st, _ = self._pedir("POST", "/api/auth/login", cuerpo={"email": "x@x.com", "password": "x"})
                if st in (401, 400, 403):
                    return
            except Exception:
                pass
            time.sleep(3)
        raise RuntimeError(f"El core no responde en {self.base}")


def ahora() -> datetime:
    """Hora de Buenos Aires (el contenedor Docker corre en UTC)."""
    try:
        from zoneinfo import ZoneInfo
        return datetime.now(ZoneInfo("America/Argentina/Buenos_Aires")).replace(tzinfo=None)
    except Exception:
        return datetime.now()


def titulo(texto):
    print(f"\n=== {texto} " + "=" * max(0, 60 - len(texto)))


def msg(r):
    return (r or {}).get("mensaje", r)


# ------------------------------------------------------------------ fases
def fase_altas(api: Api, nuevos: int) -> None:
    titulo("1. Altas de usuarios y salas")
    creados = 0
    for _ in range(nuevos):
        n, a = random.choice(NOMBRES), random.choice(APELLIDOS)
        sufijo = "".join(random.choices(string.digits, k=4))
        base = (n.split()[0][0] + a).lower()
        for x, y in (("á", "a"), ("é", "e"), ("í", "i"), ("ó", "o"), ("ú", "u")):
            base = base.replace(x, y)
        cuerpo = {"nombre": n, "apellido": a, "email": f"{base}{sufijo}@uni.edu.ar", "password": CLAVE,
                  "categoria": random.choices(["ESTUDIANTE", "DOCENTE", "PERSONAL"], [0.85, 0.1, 0.05])[0],
                  "facultad": random.choice(FACULTADES)}
        st, _ = api._pedir("POST", "/api/auth/registro", cuerpo=cuerpo)
        creados += st == 201
    print(f"  {creados} usuarios nuevos se registraron desde la pantalla de registro")

    st, r = api.como("admin@uni.edu.ar", "POST", "/api/salas",
                     {"nombre": "Aula Híbrida 301", "edificio": "Edificio Central", "facultad": None,
                      "capacidad": 55, "equipamiento": "Cámara, micrófonos y pantalla para clases híbridas",
                      "activa": True})
    print("  Sala 'Aula Híbrida 301' creada por el administrador" if st == 201 else
          f"  Sala 'Aula Híbrida 301': {msg(r)}")


def personas(api: Api) -> list[dict]:
    """Usuarios con rol USUARIO (los que se inscriben)."""
    lista, pagina = [], 0
    while True:
        st, r = api.como("admin@uni.edu.ar", "GET", f"/api/usuarios?pagina={pagina}&tamanio=100")
        if st != 200:
            raise RuntimeError(f"No se pudo listar usuarios: {msg(r)}")
        lista += [u for u in r["content"] if u["rol"] == "USUARIO" and u["activo"]]
        if r.get("last", True):
            return lista
        pagina += 1


def crear_evento(api: Api, salas: list[dict], fecha: date, hora_ini: int, minuto_ini: int = 0,
                 tipo: str | None = None, sala_correcta: float = 0.6) -> dict | None:
    tipo = tipo or random.choices(list(TIPOS), [t[0] for t in TIPOS.values()])[0]
    _, esperados, tasa, duraciones = TIPOS[tipo]
    dur = random.choice(duraciones)
    ini = datetime.combine(fecha, datetime.min.time()) + timedelta(hours=hora_ini, minutes=minuto_ini)
    fin = min(ini + timedelta(minutes=dur), datetime.combine(fecha, datetime.min.time()) + timedelta(hours=23, minutes=59))
    fac = random.choice(FACULTADES)
    org = random.choice(ORGANIZADORES)
    cupo = int(esperados * random.uniform(1.1, 1.5)) // 5 * 5 + 5
    candidatas = [s for s in salas if s["facultad"] in (None, fac)] or salas
    # como en la vida real: a veces la sala se elige bien y a veces "la que haya"
    if random.random() < sala_correcta:
        aptas = sorted([s for s in candidatas if s["capacidad"] >= esperados * tasa], key=lambda s: s["capacidad"])
        orden = aptas + [s for s in candidatas if s not in aptas]
    else:
        orden = random.sample(candidatas, len(candidatas))
    cuerpo = {"titulo": random.choice(TEMAS[tipo]), "tipo": tipo, "facultad": fac,
              "descripcion": f"{tipo.capitalize()} abierta a toda la comunidad universitaria. "
                             f"Organiza la Facultad de {fac}.",
              "materiales": "Presentación y bibliografía disponibles en el campus virtual al finalizar.",
              "fecha": fecha.isoformat(), "horaInicio": ini.strftime("%H:%M"), "horaFin": fin.strftime("%H:%M"),
              "capacidad": cupo,
              "clima": random.choices(["SOLEADO", "NUBLADO", "LLUVIA"], [0.55, 0.25, 0.2])[0]}
    for sala in orden[:6] + [None]:
        cuerpo["salaId"] = sala["id"] if sala else None
        st, r = api.como(org, "POST", "/api/eventos", cuerpo)
        if st == 201:
            r["_org"] = org
            return r
        if st != 409:  # 409 = sala ocupada en ese horario → probar con otra
            print(f"  ! No se pudo crear '{cuerpo['titulo']}': {msg(r)}")
            return None
    return None


def inscribir(api: Api, ev: dict, gente: list[dict], factor: float = 1.0) -> list[str]:
    _, esperados, _, _ = TIPOS[ev["tipo"]]
    objetivo = min(ev["capacidad"], max(3, int(random.gauss(esperados, esperados * 0.25) * factor)), len(gente))
    # los de la misma facultad se interesan más
    pesos = [3 if p.get("facultad") == ev["facultad"] else 1 for p in gente]
    elegidos, vistos = [], set()
    while len(elegidos) < objetivo and len(vistos) < len(gente):
        p = random.choices(gente, pesos)[0]
        if p["email"] in vistos:
            continue
        vistos.add(p["email"])
        st, _ = api.como(p["email"], "POST", f"/api/eventos/{ev['id']}/inscripcion")
        if st == 201:
            elegidos.append(p["email"])
        elif st == 409:  # sin cupo u otra regla: se corta
            break
    return elegidos


def fase_planificacion(api: Api, salas, gente, cantidad: int) -> list[dict]:
    titulo("2. Planificación de eventos próximos e inscripciones")
    hoy = ahora().date()
    fechas = [hoy + timedelta(days=d) for d in range(2, 50) if (hoy + timedelta(days=d)).weekday() < 5]
    eventos = []
    for _ in range(cantidad):
        ev = crear_evento(api, salas, random.choice(fechas), random.choice([9, 10, 11, 14, 16, 18, 19]))
        if not ev:
            continue
        if random.random() < 0.85:  # la mayoría se publica; el resto queda en borrador
            api.como(ev["_org"], "POST", f"/api/eventos/{ev['id']}/publicar")
            dias = (date.fromisoformat(ev["fecha"]) - hoy).days
            # cuanto más lejos, menos gente se anotó todavía
            ya = inscribir(api, ev, gente, factor=max(0.15, 1 - dias / 50))
            cancelan = [e for e in ya if random.random() < 0.06]
            for email in cancelan:
                api.como(email, "DELETE", f"/api/eventos/{ev['id']}/inscripcion")
            print(f"  + {ev['fecha']} {ev['titulo']:<42} publicado · {len(ya) - len(cancelan)} inscriptos"
                  f"{f' ({len(cancelan)} ' + ('canceló' if len(cancelan) == 1 else 'cancelaron') + ')' if cancelan else ''}")
        else:
            print(f"  + {ev['fecha']} {ev['titulo']:<42} en borrador")
        eventos.append(ev)
    return eventos


def fase_hoy(api: Api, salas, gente, cantidad: int) -> list[dict]:
    titulo("3. Eventos de hoy: inscripción, asistencia y cierre")
    momento = ahora()
    if momento.hour >= 23:
        print("  Son más de las 23 h: se omite esta fase (volvé a correrla mañana).")
        return []
    hechos = []
    for i in range(cantidad):
        # horarios que todavía no terminaron, para que se pueda inscribir la gente
        h = min(22, momento.hour + 1 + i % 3)
        ev = crear_evento(api, salas, momento.date(), h, 0, sala_correcta=0.5)
        if not ev:
            continue
        api.como(ev["_org"], "POST", f"/api/eventos/{ev['id']}/publicar")
        inscriptos = inscribir(api, ev, gente)
        # predicción ANTES del evento (para comparar después con lo real)
        api.como(ev["_org"], "POST", f"/api/eventos/{ev['id']}/prediccion")

        # asistencia real: depende del tipo, del clima y un poco de azar
        _, _, tasa, _ = TIPOS[ev["tipo"]]
        tasa += {"SOLEADO": 0, "NUBLADO": -0.03, "LLUVIA": -0.14}[ev["clima"]] + random.gauss(0, 0.05)
        st, lista = api.como("apoyo@uni.edu.ar", "GET", f"/api/eventos/{ev['id']}/inscriptos")
        presentes = [x for x in lista if random.random() < tasa] if st == 200 else []
        metodos = {"QR": 0, "CODIGO": 0, "LISTA": 0}
        for x in presentes:
            metodo = random.choices(["QR", "CODIGO", "LISTA"], [0.6, 0.15, 0.25])[0]
            cuerpo = {"inscripcionId": x["id"], "metodo": "LISTA"} if metodo == "LISTA" else \
                     {"codigo": x["codigo"], "metodo": metodo}
            st, _ = api.como("apoyo@uni.edu.ar", "POST", f"/api/eventos/{ev['id']}/asistencias", cuerpo)
            metodos[metodo] += st == 200
        api.como(ev["_org"], "POST", f"/api/eventos/{ev['id']}/finalizar")
        print(f"  ✓ {ev['horaInicio'][:5]} {ev['titulo']:<40} {len(inscriptos)} inscriptos → {len(presentes)} asistieron "
              f"(QR {metodos['QR']}, código {metodos['CODIGO']}, lista {metodos['LISTA']}) · finalizado")
        hechos.append(ev)
    return hechos


def fase_analitica(api: Api) -> None:
    titulo("4. Analítica: reentrenamiento y predicciones")
    st, r = api.como("organizador@uni.edu.ar", "POST", "/api/analitica/entrenar")
    if st == 200:
        m = r["metricas"][r["modelo_elegido"]]
        print(f"  Modelo reentrenado con {r['registros']} eventos · elegido: {r['modelo_elegido']} "
              f"· error medio ±{m['mae_asistentes']} personas")
    else:
        print(f"  ! Entrenamiento: {msg(r)}")
    total = 0
    for org in ORGANIZADORES + ["admin@uni.edu.ar"]:
        st, r = api.como(org, "GET", "/api/predicciones/proximos?regenerar=true")
        if st == 200:
            total = max(total, len(r))
            riesgos = [p for p in r if p["probSobrecapacidad"] >= 0.3 or p["probBajaAsistencia"] >= 0.5]
            if org == "admin@uni.edu.ar":
                print(f"  {len(r)} eventos próximos con predicción · {len(riesgos)} con riesgo de sala chica o vacía")
                for p in riesgos[:6]:
                    tipo = "sala chica" if p["probSobrecapacidad"] >= 0.3 else "sala sobredimensionada"
                    print(f"    - {p['fecha']} {p['eventoTitulo']}: ~{p['asistenciaEstimada']} personas en "
                          f"{p.get('salaActualNombre') or 'sin sala'} ({tipo}) → sugerida {p.get('salaRecomendadaNombre')}")


def fase_llenar(api: Api, gente, evento_id: int, cantidad: int) -> None:
    titulo(f"Inscribiendo gente al evento {evento_id}")
    st, ev = api.como("admin@uni.edu.ar", "GET", f"/api/eventos/{evento_id}")
    if st != 200:
        raise RuntimeError(f"No se encontró el evento {evento_id}: {msg(ev)}")
    if ev["estado"] != "PUBLICADO":
        raise RuntimeError("El evento tiene que estar PUBLICADO para recibir inscripciones")
    random.shuffle(gente)
    nuevos = 0
    for p in gente:
        if nuevos >= cantidad:
            break
        st, r = api.como(p["email"], "POST", f"/api/eventos/{evento_id}/inscripcion")
        if st == 201:
            nuevos += 1
        elif st == 409 and "cupo" in str(msg(r)).lower():
            print("  Se completó el cupo.")
            break
    print(f"  '{ev['titulo']}': {nuevos} inscripciones nuevas (total {ev['inscriptos'] + nuevos} de {ev['capacidad']}).")
    print("  Abrí 'Ver predicción' → 'Recalcular' para ver cómo cambia la estimación.")


def main():
    ap = argparse.ArgumentParser(description="Genera actividad realista en la plataforma usando su API.")
    ap.add_argument("--api", default="http://localhost:8080", help="URL del core Java (default %(default)s)")
    ap.add_argument("--futuros", type=int, default=12, help="eventos próximos a crear (default %(default)s)")
    ap.add_argument("--hoy", type=int, default=4, help="eventos de hoy que se realizan y finalizan (default %(default)s)")
    ap.add_argument("--nuevos", type=int, default=25, help="usuarios nuevos a registrar (default %(default)s)")
    ap.add_argument("--llenar", type=int, default=None, metavar="ID",
                    help="sólo inscribe gente al evento con ese id (se ve en la URL) y termina")
    ap.add_argument("--cantidad", type=int, default=40, help="inscripciones a sumar con --llenar (default %(default)s)")
    ap.add_argument("--semilla", type=int, default=None, help="semilla aleatoria para repetir la misma simulación")
    a = ap.parse_args()
    random.seed(a.semilla)

    api = Api(a.api)
    print(f"Conectando con {api.base} …")
    api.esperar()
    inicio = time.time()

    if a.llenar:
        fase_llenar(api, personas(api), a.llenar, a.cantidad)
        return

    fase_altas(api, a.nuevos)
    st, salas = api.como("admin@uni.edu.ar", "GET", "/api/salas?soloActivas=true")
    gente = personas(api)
    print(f"  {len(gente)} personas disponibles para inscribirse · {len(salas)} salas activas")

    futuros = fase_planificacion(api, salas, gente, a.futuros)
    hoy = fase_hoy(api, salas, gente, a.hoy)
    fase_analitica(api)

    titulo("Listo")
    print(f"  {len(futuros)} eventos próximos y {len(hoy)} eventos realizados hoy en {time.time() - inicio:.0f} s.")
    print("  Entrá con organizador@uni.edu.ar / demo1234 y mirá el Panel y el Dashboard predictivo.")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, urllib.error.URLError) as e:
        print(f"\nERROR: {e}", file=sys.stderr)
        sys.exit(1)

"""
Genera db/init/02_datos_demo.sql con datos sintéticos pero realistas:
  - roles, usuarios (admin, organizadores, personal de apoyo y ~400 asistentes)
  - salas de distintas facultades
  - ~220 eventos FINALIZADOS en los últimos 18 meses (con inscripciones y asistencia real)
  - eventos PUBLICADOS y BORRADOR a futuro
  - encuestas de satisfacción de los asistentes de eventos finalizados

Cada organizador tiene una "calidad" propia: sus eventos gustan más o menos (puntaje de la encuesta)
y, como la gente se entera, a los eventos de los organizadores mejor valorados asiste más gente.
La sala desbordada y los eventos muy largos bajan la satisfacción.

Las fechas se escriben relativas al lunes de la semana actual (función lunes_actual()),
así la demo siempre tiene historial pasado y eventos próximos sin importar cuándo se levante,
y el día de la semana de cada evento se conserva.

Uso:  python tools/generar_datos_demo.py
"""
import random
import string
from datetime import date, timedelta
from pathlib import Path

random.seed(2026)

# Contraseña de todos los usuarios demo: demo1234 (hash bcrypt)
HASH = "$2b$10$zF8L1t7tbuzVS0.xUn3vTuhNYaYqfYAl5TJXAPs8aGtorFKqAqoTS"

FACULTADES = ["Ingeniería", "Ciencias Económicas", "Derecho", "Ciencias Sociales", "Ciencias Exactas"]
FAC_MULT = {"Ingeniería": 1.15, "Ciencias Económicas": 1.0, "Derecho": 0.85,
            "Ciencias Sociales": 0.95, "Ciencias Exactas": 0.8}

SALAS = [  # nombre, edificio, facultad, capacidad, equipamiento
    ("Aula 101", "Edificio Central", "Ingeniería", 40, "Proyector, pizarra"),
    ("Aula 205", "Edificio Central", "Ingeniería", 60, "Proyector, audio"),
    ("Laboratorio de Informática", "Pabellón Tecnológico", "Ingeniería", 30, "30 PCs, proyector"),
    ("Aula Magna", "Edificio Central", None, 300, "Escenario, audio, streaming"),
    ("Auditorio Norte", "Sede Norte", None, 150, "Audio, proyector doble"),
    ("Sala de Conferencias", "Ciencias Económicas", "Ciencias Económicas", 90, "Proyector, micrófonos"),
    ("Aula 12", "Ciencias Económicas", "Ciencias Económicas", 45, "Proyector"),
    ("Sala de Audiencias", "Derecho", "Derecho", 70, "Estrado, audio"),
    ("Aula 3", "Derecho", "Derecho", 35, "Pizarra"),
    ("Salón de Usos Múltiples", "Ciencias Sociales", "Ciencias Sociales", 120, "Sillas móviles, audio"),
    ("Aula Taller", "Ciencias Sociales", "Ciencias Sociales", 25, "Mesas de trabajo"),
    ("Laboratorio de Física", "Ciencias Exactas", "Ciencias Exactas", 28, "Equipamiento de laboratorio"),
]

TIPOS = {
    #            peso  inscriptos_base  tasa_asistencia  duración(min)
    "CHARLA":        (0.34, 70,  0.62, [60, 90]),
    "SEMINARIO":     (0.22, 45,  0.70, [120, 180]),
    "TALLER":        (0.24, 24,  0.82, [120, 180, 240]),
    "JORNADA":       (0.12, 140, 0.55, [240, 360, 480]),
    "INSTITUCIONAL": (0.08, 170, 0.50, [90, 120]),
}

TEMAS = {
    "CHARLA": ["Inteligencia artificial generativa", "Ciberseguridad para todos", "Economía argentina 2026",
               "Salidas laborales en tecnología", "Derechos digitales", "Cambio climático y ciudades",
               "Emprendedurismo universitario", "Ética en la ciencia de datos", "Finanzas personales",
               "Movilidad sustentable", "Blockchain más allá de las cripto", "Neurociencia del aprendizaje"],
    "SEMINARIO": ["Metodología de la investigación", "Derecho del consumidor", "Análisis de políticas públicas",
                  "Arquitectura de microservicios", "Econometría aplicada", "Teoría de grafos",
                  "Comunicación científica", "Gestión de proyectos ágiles"],
    "TALLER": ["Python para análisis de datos", "Oratoria y presentaciones", "Git y GitHub desde cero",
               "Redacción de CV", "Excel avanzado", "Diseño UX", "Docker en la práctica",
               "Escritura académica", "Primeros auxilios", "Fotografía con el celular"],
    "JORNADA": ["Jornada de Investigación", "Semana de la Ingeniería", "Jornada de Extensión Universitaria",
                "Feria de Empleo", "Jornada de Innovación Educativa", "Hackatón universitaria"],
    "INSTITUCIONAL": ["Acto de colación", "Bienvenida a ingresantes", "Presentación del plan estratégico",
                      "Aniversario de la universidad", "Entrega de becas"],
}

NOMBRES = ["Sofía", "Mateo", "Valentina", "Benjamín", "Martina", "Joaquín", "Catalina", "Tomás", "Lucía",
           "Santiago", "Camila", "Lautaro", "Julieta", "Thiago", "Agustina", "Facundo", "Micaela", "Nicolás",
           "Florencia", "Ignacio", "Paula", "Franco", "Rocío", "Gonzalo", "Milagros", "Ramiro", "Abril",
           "Emiliano", "Delfina", "Bruno", "Carla", "Lucas", "Antonella", "Federico", "Belén", "Maximiliano"]
APELLIDOS = ["González", "Rodríguez", "Gómez", "Fernández", "López", "Díaz", "Martínez", "Pérez", "García",
             "Sánchez", "Romero", "Sosa", "Álvarez", "Torres", "Ruiz", "Ramírez", "Flores", "Benítez",
             "Acosta", "Medina", "Herrera", "Suárez", "Aguirre", "Giménez", "Gutiérrez", "Pereyra", "Molina",
             "Castro", "Ortiz", "Silva", "Núñez", "Luna", "Juárez", "Cabrera", "Ríos", "Morales"]

HORAS = [9, 10, 11, 14, 16, 18, 19]

# ---------- encuestas de satisfacción ----------
# Calidad de cada organizador (id de usuario): suma o resta al puntaje medio de sus eventos
CALIDAD_ORG = {2: 0.45, 3: 0.0, 4: -0.55, 5: 0.2}
SATISF_TIPO = {"CHARLA": 3.9, "SEMINARIO": 4.0, "TALLER": 4.3, "JORNADA": 3.7, "INSTITUCIONAL": 3.5}
TASA_RESPUESTA = 0.55          # proporción de asistentes que contesta la encuesta
TASA_RESPUESTA_RECIENTE = 0.3  # eventos de las últimas 4 semanas: la encuesta sigue abierta
COMENTARIOS = {
    5: ["Excelente, muy claro y bien organizado.", "Me encantó, ojalá haya más actividades así.",
        "Muy buena propuesta, el expositor domina el tema."],
    4: ["Muy bueno, aprendí cosas útiles.", "Buen evento, solo faltó un poco más de tiempo para preguntas.",
        "Bien organizado y puntual."],
    3: ["Estuvo bien, aunque algo largo.", "Interesante pero muy teórico.", "Correcto, sin más."],
    2: ["La sala quedó chica y no se escuchaba bien.", "Empezó tarde y se extendió demasiado.",
        "Esperaba más contenido práctico."],
    1: ["Muy desorganizado.", "No había lugar para sentarse y no se veía la presentación."],
}
MES_ESTACIONALIDAD = {1: 0.35, 2: 0.45, 3: 1.05, 4: 1.1, 5: 1.1, 6: 1.0, 7: 0.6,
                      8: 1.05, 9: 1.1, 10: 1.1, 11: 0.95, 12: 0.7}
PROB_LLUVIA = {1: .25, 2: .25, 3: .28, 4: .25, 5: .2, 6: .18, 7: .18, 8: .18, 9: .22, 10: .28, 11: .28, 12: .25}

usados = set()


def codigo():
    while True:
        c = "".join(random.choices(string.ascii_uppercase + string.digits, k=8))
        if c not in usados:
            usados.add(c)
            return c


def q(s):
    return "NULL" if s is None else "'" + str(s).replace("'", "''") + "'"


def main():
    hoy = date.today()
    lunes = hoy - timedelta(days=hoy.weekday())
    out = []
    w = out.append

    w("-- Archivo generado por tools/generar_datos_demo.py  (contraseña de todos los usuarios: demo1234)")
    w("CREATE OR REPLACE FUNCTION lunes_actual() RETURNS DATE LANGUAGE sql STABLE AS "
      "$$ SELECT (CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::int - 1))::date $$;\n")

    # ---------- roles ----------
    w("INSERT INTO roles (id, nombre, descripcion) VALUES")
    w("(1,'ADMIN','Administra usuarios, roles, salas y todos los eventos'),")
    w("(2,'ORGANIZADOR','Crea y gestiona sus eventos, ve panel y predicciones'),")
    w("(3,'APOYO','Personal de apoyo: registra asistencia'),")
    w("(4,'USUARIO','Estudiante, docente o personal que se inscribe a eventos');\n")

    # ---------- usuarios ----------
    usuarios = [  # id, nombre, apellido, email, categoria, facultad, rol
        (1, "Admin", "Sistema", "admin@uni.edu.ar", "PERSONAL", None, 1),
        (2, "Laura", "Méndez", "organizador@uni.edu.ar", "DOCENTE", "Ingeniería", 2),
        (3, "Diego", "Ferrari", "dferrari@uni.edu.ar", "DOCENTE", "Ciencias Económicas", 2),
        (4, "Mariana", "Quiroga", "mquiroga@uni.edu.ar", "PERSONAL", "Ciencias Sociales", 2),
        (5, "Pablo", "Iglesias", "piglesias@uni.edu.ar", "DOCENTE", "Derecho", 2),
        (6, "Andrés", "Villalba", "apoyo@uni.edu.ar", "PERSONAL", None, 3),
        (7, "Estudiante", "Demo", "estudiante@uni.edu.ar", "ESTUDIANTE", "Ingeniería", 4),
    ]
    nid = 8
    emails = {u[3] for u in usuarios}
    for _ in range(420):
        n, a = random.choice(NOMBRES), random.choice(APELLIDOS)
        base = (n[0] + a).lower()
        for s, r in (("á", "a"), ("é", "e"), ("í", "i"), ("ó", "o"), ("ú", "u"), ("ñ", "n")):
            base = base.replace(s, r)
        email = f"{base}{random.randint(1, 999)}@uni.edu.ar"
        if email in emails:
            continue
        emails.add(email)
        cat = random.choices(["ESTUDIANTE", "DOCENTE", "PERSONAL"], [0.8, 0.13, 0.07])[0]
        usuarios.append((nid, n, a, email, cat, random.choice(FACULTADES), 4))
        nid += 1
    w("INSERT INTO usuarios (id, nombre, apellido, email, password_hash, categoria, facultad, rol_id) VALUES")
    w(",\n".join(f"({u[0]},{q(u[1])},{q(u[2])},{q(u[3])},{q(HASH)},{q(u[4])},{q(u[5])},{u[6]})"
                 for u in usuarios) + ";\n")
    asistentes_ids = [u[0] for u in usuarios if u[6] == 4]

    # ---------- salas ----------
    w("INSERT INTO salas (id, nombre, edificio, facultad, capacidad, equipamiento) VALUES")
    w(",\n".join(f"({i + 1},{q(s[0])},{q(s[1])},{q(s[2])},{s[3]},{q(s[4])})" for i, s in enumerate(SALAS)) + ";\n")

    def sala_para(facultad, cupo, acierta):
        """El organizador elige sala: a veces bien (capacidad cercana al cupo), a veces mal."""
        candidatas = [(i + 1, s) for i, s in enumerate(SALAS) if s[2] in (facultad, None)]
        if acierta:
            ok = [c for c in candidatas if c[1][3] >= cupo * 0.8] or candidatas
            return min(ok, key=lambda c: c[1][3])
        return random.choice(candidatas)

    eventos, inscripciones, asistencias = [], [], []
    para_encuesta = []
    eid, iid = 1, 1

    def crear_evento(offset, estado, historico):
        nonlocal eid, iid
        d = lunes + timedelta(days=offset)
        tipo = random.choices(list(TIPOS), [t[0] for t in TIPOS.values()])[0]
        _, base_insc, tasa, duraciones = TIPOS[tipo]
        fac = random.choice(FACULTADES)
        hora = random.choice(HORAS if tipo != "JORNADA" else [9, 10, 14])
        dur = random.choice(duraciones)
        if hora * 60 + dur > 22 * 60:
            hora = 9
        clima = "LLUVIA" if random.random() < PROB_LLUVIA[d.month] else random.choice(["SOLEADO", "SOLEADO", "NUBLADO"])
        progreso = (offset + 560) / 560  # 0 → 1 a lo largo del historial (tendencia creciente)

        inscriptos_esperados = base_insc * FAC_MULT[fac] * MES_ESTACIONALIDAD[d.month] * (0.9 + 0.25 * progreso)
        if hora in (18, 19) and tipo in ("CHARLA", "SEMINARIO"):
            inscriptos_esperados *= 1.1
        inscriptos = max(5, int(random.gauss(inscriptos_esperados, inscriptos_esperados * 0.22)))
        inscriptos = min(inscriptos, len(asistentes_ids) - 10)

        cupo = int(max(inscriptos, inscriptos_esperados) * random.uniform(1.0, 1.3)) // 5 * 5 + 5
        sala_id, sala = sala_para(fac, cupo * tasa, acierta=random.random() < 0.55)

        # ---------- tasa real de asistencia ----------
        t = tasa
        t += {"SOLEADO": 0.0, "NUBLADO": -0.03, "LLUVIA": -0.14}[clima]
        t += {9: -0.05, 10: 0.0, 11: 0.01, 14: -0.02, 16: 0.0, 18: -0.02, 19: -0.07}[hora]
        t -= 0.08 if d.weekday() == 4 else 0.0
        t -= 0.05 if dur >= 240 else 0.0
        t += 0.05 * progreso
        t += random.gauss(0, 0.06)
        t = min(0.98, max(0.15, t))

        titulo = f"{random.choice(TEMAS[tipo])}"
        if tipo == "JORNADA":
            titulo += f" {d.year}"
        desc = f"{tipo.capitalize()} organizada por la Facultad de {fac}. Actividad abierta a estudiantes, docentes y personal."
        org = random.choice([2, 3, 4, 5])
        # los organizadores mejor valorados atraen asistentes más comprometidos
        t = min(0.98, max(0.15, t + 0.12 * CALIDAD_ORG[org]))
        h_ini = f"{hora:02d}:00"
        fin_min = hora * 60 + dur
        h_fin = f"{fin_min // 60:02d}:{fin_min % 60:02d}"
        if not historico:
            inscriptos = int(inscriptos * random.uniform(0.15, 0.85))  # inscripción aún abierta
            if estado == "BORRADOR":
                inscriptos = 0
        eventos.append(f"({eid},{q(titulo)},{q(desc)},{q('Material disponible en el campus virtual.')},"
                       f"{q(tipo)},{q(fac)},lunes_actual() + {offset},{q(h_ini)},{q(h_fin)},{sala_id},{cupo},"
                       f"{q(clima)},{q(estado)},{org})")
        elegidos = random.sample(asistentes_ids, inscriptos)
        if historico and offset > -14 and 7 not in elegidos:
            elegidos[0] = 7  # estudiante@ asistió a los eventos de las últimas 2 semanas (encuestas pendientes)
        n_asist = round(inscriptos * t) if historico else 0
        presentes = []
        for k, uid in enumerate(elegidos):
            dias_antes = random.randint(1, 25)
            inscripciones.append(f"({iid},{eid},{uid},{q(codigo())},'ACTIVA',"
                                 f"(lunes_actual() + {offset - dias_antes})::timestamp + interval '{random.randint(8, 22)} hours')")
            if k < n_asist:
                metodo = random.choices(["QR", "CODIGO", "LISTA"], [0.6, 0.15, 0.25])[0]
                asistencias.append(f"({iid},{q(metodo)},6,(lunes_actual() + {offset})::timestamp + time {q(h_ini)})")
                presentes.append((iid, uid))
            iid += 1
        if historico:
            para_encuesta.append({"offset": offset, "org": org, "tipo": tipo, "dur": dur, "fin_min": fin_min,
                                  "sobrecupo": n_asist > sala[3], "presentes": presentes})
        eid += 1

    # Historial: ~220 eventos en los últimos ~18 meses (hasta la semana pasada)
    offset = -560
    while offset < -3:
        d = lunes + timedelta(days=offset)
        if d.weekday() < 5 and random.random() < 0.85 * MES_ESTACIONALIDAD[d.month]:
            crear_evento(offset, "FINALIZADO", True)
        offset += random.choice([1, 2, 3])

    # Próximos eventos
    for off in sorted(random.sample([o for o in range(7, 70) if (lunes + timedelta(days=o)).weekday() < 5], 16)):
        crear_evento(off, "PUBLICADO", False)
    for off in random.sample([o for o in range(20, 80) if (lunes + timedelta(days=o)).weekday() < 5], 3):
        crear_evento(off, "BORRADOR", False)

    def lotes(tabla, cols, filas, tam=1000):
        for i in range(0, len(filas), tam):
            w(f"INSERT INTO {tabla} ({cols}) VALUES")
            w(",\n".join(filas[i:i + tam]) + ";\n")

    lotes("eventos", "id, titulo, descripcion, materiales, tipo, facultad, fecha, hora_inicio, hora_fin, "
                     "sala_id, capacidad, clima, estado, organizador_id", eventos)
    lotes("inscripciones", "id, evento_id, usuario_id, codigo, estado, fecha_inscripcion", inscripciones)
    lotes("asistencias", "inscripcion_id, metodo, registrada_por, registrada_en", asistencias)

    # ---------- encuestas (con un generador aparte para no alterar el resto de los datos) ----------
    rnd = random.Random(77)
    encuestas = []
    for ev in para_encuesta:
        reciente = ev["offset"] > -28
        media = SATISF_TIPO[ev["tipo"]] + CALIDAD_ORG[ev["org"]]
        media -= 0.7 if ev["sobrecupo"] else 0.0
        media -= 0.25 if ev["dur"] >= 240 else 0.0
        media += rnd.gauss(0, 0.2)  # cada evento tiene su propia "suerte"
        for ins_id, uid in ev["presentes"]:
            if uid == 7 and reciente:
                continue  # estudiante@ deja pendientes sus encuestas recientes (para la demo)
            if rnd.random() > (TASA_RESPUESTA_RECIENTE if reciente else TASA_RESPUESTA):
                continue
            puntaje = min(5, max(1, round(rnd.gauss(media, 0.75))))
            recomendaria = puntaje >= 4 if rnd.random() < 0.9 else puntaje >= 3
            comentario = rnd.choice(COMENTARIOS[puntaje]) if rnd.random() < 0.25 else None
            dias = rnd.randint(0, min(6, -ev["offset"] - 1))
            hora = rnd.randint(ev["fin_min"] // 60, 23) if dias == 0 else rnd.randint(8, 23)
            encuestas.append(f"({ins_id},{puntaje},{'TRUE' if recomendaria else 'FALSE'},{q(comentario)},"
                             f"(lunes_actual() + {ev['offset'] + dias})::timestamp + interval '{hora} hours')")
    lotes("encuestas", "inscripcion_id, puntaje, recomendaria, comentario, respondida_en", encuestas)

    for t in ("roles", "usuarios", "salas", "eventos", "inscripciones", "asistencias", "encuestas"):
        w(f"SELECT setval(pg_get_serial_sequence('{t}','id'), (SELECT MAX(id) FROM {t}));")

    dest = Path(__file__).resolve().parent.parent / "db" / "init" / "02_datos_demo.sql"
    dest.write_text("\n".join(out), encoding="utf-8")
    print(f"{dest.name}: {len(usuarios)} usuarios, {len(eventos)} eventos, "
          f"{len(inscripciones)} inscripciones, {len(asistencias)} asistencias, {len(encuestas)} encuestas")


if __name__ == "__main__":
    main()

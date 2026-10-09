"""Pruebas de la retroalimentación con encuestas de satisfacción."""
import random
from datetime import date, timedelta

import pandas as pd

from app.modelo import ModeloAsistencia, agregar_satisfaccion_previa, satisfaccion_previa

CALIDAD = {1: 0.5, 2: -0.6}  # organizador 1 gusta mucho, organizador 2 poco


def historico_con_encuestas(n=120):
    rnd = random.Random(3)
    regs, hoy = [], date.today()
    for i in range(n):
        org = rnd.choice([1, 2])
        ins = rnd.randint(30, 120)
        tasa = 0.62 + 0.12 * CALIDAD[org] + rnd.uniform(-0.04, 0.04)
        regs.append({"evento_id": i + 1, "tipo": "CHARLA", "facultad": "Ingeniería",
                     "fecha": str(hoy - timedelta(days=3 * (n - i))), "hora_inicio": "10:00", "duracion_min": 90,
                     "clima": "SOLEADO", "capacidad_sala": 150, "inscriptos": ins, "asistentes": int(ins * tasa),
                     "organizador_id": org, "satisfaccion": round(3.9 + CALIDAD[org] + rnd.uniform(-0.2, 0.2), 2),
                     "respuestas": 20})
    return regs


def test_satisfaccion_previa_no_usa_el_propio_evento_ni_posteriores():
    df = pd.DataFrame({"organizador_id": [7, 7, 7], "satisfaccion": [5.0, 1.0, 3.0],
                       "fecha": pd.to_datetime(["2026-01-01", "2026-02-01", "2026-03-01"])})
    d, media = agregar_satisfaccion_previa(df)
    assert media == 3.0
    # el primer evento no tiene anteriores: queda en el promedio general
    assert d.loc[0, "satisfaccion_previa"] == 3.0
    # el segundo sólo ve el primero (5,0), suavizado hacia 3,0 con 3 eventos ficticios
    assert abs(d.loc[1, "satisfaccion_previa"] - (5.0 + 3 * 3.0) / 4) < 1e-9
    valor, n = satisfaccion_previa(d, 7, "2026-12-31", media)
    assert n == 3


def test_sin_encuestas_usa_valor_por_defecto():
    df = pd.DataFrame({"fecha": pd.to_datetime(["2026-01-01"])})
    d, media = agregar_satisfaccion_previa(df)
    assert media == 4.0 and d.loc[0, "satisfaccion_previa"] == 4.0


def test_organizador_mejor_valorado_tiene_mas_asistencia():
    m = ModeloAsistencia.entrenar(historico_con_encuestas())
    assert m.metricas["encuestas"]["eventos_con_encuesta"] == 120
    assert any(i["variable"] == "satisfaccion_previa" for i in m.importancias)
    base = {"evento_id": 1, "tipo": "CHARLA", "facultad": "Ingeniería",
            "fecha": date.today() + timedelta(days=1), "hora_inicio": "10:00", "duracion_min": 90,
            "clima": "SOLEADO", "inscriptos": 100, "cupo": 120, "capacidad_sala": 150, "sala_id": None}
    bueno = m.predecir({**base, "organizador_id": 1}, [])
    malo = m.predecir({**base, "organizador_id": 2}, [])
    assert bueno["satisfaccion_previa"] > malo["satisfaccion_previa"]
    assert bueno["asistencia_estimada"] > malo["asistencia_estimada"]
    assert any(t.startswith("Encuestas:") for t in bueno["explicacion"])

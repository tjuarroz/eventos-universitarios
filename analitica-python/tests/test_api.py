"""Pruebas del microservicio de analítica con datos sintéticos."""
import random
from datetime import date, timedelta

import jwt
import pytest
from fastapi.testclient import TestClient

from app import almacen
from app.config import JWT_SECRET
from app.main import app
from app.tendencias import holt

client = TestClient(app)


def token(rol="ORGANIZADOR"):
    return {"Authorization": "Bearer " + jwt.encode({"sub": "t@uni.edu.ar", "id": 1, "rol": rol}, JWT_SECRET)}


def historico(n=80):
    random.seed(1)
    tasas = {"CHARLA": .6, "SEMINARIO": .7, "TALLER": .85, "JORNADA": .5, "INSTITUCIONAL": .5}
    regs, hoy = [], date.today()
    for i in range(n):
        tipo = random.choice(list(tasas))
        clima = random.choice(["SOLEADO", "NUBLADO", "LLUVIA"])
        ins = random.randint(20, 150)
        t = tasas[tipo] - (0.15 if clima == "LLUVIA" else 0) + random.uniform(-.05, .05)
        regs.append({"evento_id": i + 1, "tipo": tipo, "facultad": random.choice(["Ingeniería", "Derecho"]),
                     "fecha": str(hoy - timedelta(days=5 * i + 7)), "hora_inicio": random.choice(["10:00", "18:00"]),
                     "duracion_min": 90, "clima": clima, "capacidad_sala": random.choice([40, 90, 150]),
                     "inscriptos": ins, "asistentes": int(ins * t)})
    return regs


SALAS = [{"id": 1, "nombre": "Aula chica", "capacidad": 40, "facultad": None},
         {"id": 2, "nombre": "Aula media", "capacidad": 90, "facultad": None},
         {"id": 3, "nombre": "Auditorio", "capacidad": 300, "facultad": None}]


@pytest.fixture(scope="module", autouse=True)
def entrenado(tmp_path_factory):
    almacen._ARCHIVO = tmp_path_factory.mktemp("d") / "modelo.joblib"
    r = client.post("/api/v1/modelo/entrenar", json={"registros": historico()}, headers=token("SERVICE"))
    assert r.status_code == 200, r.text
    return r.json()


def evento(**kw):
    base = {"evento_id": 999, "tipo": "CHARLA", "facultad": "Ingeniería", "fecha": str(date.today() + timedelta(days=2)),
            "hora_inicio": "18:00", "duracion_min": 90, "clima": "SOLEADO", "inscriptos": 120, "cupo": 150,
            "capacidad_sala": 40, "sala_id": 1}
    base.update(kw)
    return base


def test_requiere_token():
    assert client.get("/api/v1/modelo/estado").status_code == 401
    assert client.get("/api/v1/modelo/estado", headers=token("USUARIO")).status_code == 403


def test_estado_y_metricas(entrenado):
    assert entrenado["modelo_elegido"] in ("regresion_ridge", "random_forest")
    assert entrenado["metricas"]["registros"] == 80
    assert abs(sum(i["importancia"] for i in entrenado["importancias"]) - 1) < 0.01


def test_sala_chica_da_sobrecapacidad_y_recomienda_otra():
    r = client.post("/api/v1/predicciones", json={"evento": evento(), "salas": SALAS}, headers=token()).json()
    assert 50 <= r["asistencia_estimada"] <= 100
    assert r["prob_sobrecapacidad"] > 0.9
    assert r["sala_recomendada"]["capacidad"] >= r["asistencia_estimada"]
    assert r["sala_actual_adecuada"] is False
    assert r["intervalo_inferior"] <= r["asistencia_estimada"] <= r["intervalo_superior"]


def test_sala_grande_da_baja_asistencia():
    r = client.post("/api/v1/predicciones", json={"evento": evento(inscriptos=30, capacidad_sala=300, sala_id=3),
                                                  "salas": SALAS}, headers=token()).json()
    assert r["prob_baja_asistencia"] > 0.9
    assert r["sala_recomendada_id"] == 1


def test_lluvia_reduce_asistencia():
    sol = client.post("/api/v1/predicciones", json={"evento": evento(), "salas": SALAS}, headers=token()).json()
    lluvia = client.post("/api/v1/predicciones", json={"evento": evento(clima="LLUVIA"), "salas": SALAS},
                         headers=token()).json()
    assert lluvia["asistencia_estimada"] < sol["asistencia_estimada"]


def test_lote_y_tendencias():
    r = client.post("/api/v1/predicciones/lote", json={"eventos": [evento(), evento(evento_id=5)], "salas": SALAS},
                    headers=token("SERVICE"))
    assert [p["evento_id"] for p in r.json()["predicciones"]] == [999, 5]
    t = client.get("/api/v1/tendencias?agrupar=tipo&horizonte=2", headers=token()).json()
    assert set(t["grupos"]) <= {"CHARLA", "SEMINARIO", "TALLER", "JORNADA", "INSTITUCIONAL"}
    assert len(t["pronostico"]) == 3
    res = client.get("/api/v1/tendencias/resumen", headers=token()).json()
    assert res["totales"]["eventos"] == 80


def test_holt_sigue_tendencia():
    assert holt([1, 2, 3, 4, 5], 2)[1] > 5

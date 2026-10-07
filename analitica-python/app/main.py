"""Microservicio de analítica predictiva de asistencia (FastAPI)."""
import logging
from contextlib import asynccontextmanager
from typing import Literal

from fastapi import Depends, FastAPI, HTTPException, Query, status
from fastapi.middleware.cors import CORSMiddleware

from . import almacen, tendencias
from .config import CORS_ORIGINS, MIN_REGISTROS_ENTRENAMIENTO
from .esquemas import (EntrenarRequest, PrediccionLoteRequest, PrediccionLoteResponse, PrediccionRequest,
                       PrediccionResponse)
from .modelo import ModeloAsistencia
from .seguridad import Usuario, requiere_rol

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

STAFF = ("ADMIN", "ORGANIZADOR", "APOYO", "SERVICE")


@asynccontextmanager
async def lifespan(_: FastAPI):
    almacen.cargar()
    yield


app = FastAPI(
    title="Analítica predictiva de asistencia",
    version="1.0.0",
    description="Entrena modelos con el histórico de eventos y estima asistencia, riesgo de sobrecapacidad "
                "o baja asistencia, sala recomendada y tendencias de participación.",
    lifespan=lifespan,
)
app.add_middleware(CORSMiddleware, allow_origins=CORS_ORIGINS, allow_methods=["*"], allow_headers=["*"])


def _modelo() -> ModeloAsistencia:
    m = almacen.actual()
    if m is None:
        raise HTTPException(status.HTTP_409_CONFLICT, "El modelo todavía no fue entrenado")
    return m


@app.get("/api/v1/salud", tags=["General"])
def salud():
    m = almacen.actual()
    return {"estado": "ok", "modelo_entrenado": m is not None}


# ------------------------------------------------------------------ modelo
@app.post("/api/v1/modelo/entrenar", tags=["Modelo"])
def entrenar(req: EntrenarRequest, _: Usuario = Depends(requiere_rol("SERVICE", "ADMIN", "ORGANIZADOR"))):
    if len(req.registros) < MIN_REGISTROS_ENTRENAMIENTO:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY,
                            f"Se necesitan al menos {MIN_REGISTROS_ENTRENAMIENTO} registros")
    m = ModeloAsistencia.entrenar([r.model_dump() for r in req.registros])
    almacen.guardar(m)
    return m.estado()


@app.get("/api/v1/modelo/estado", tags=["Modelo"])
def estado(_: Usuario = Depends(requiere_rol(*STAFF))):
    m = almacen.actual()
    return m.estado() if m else {"entrenado": False}


# ------------------------------------------------------------------ predicciones
@app.post("/api/v1/predicciones", response_model=PrediccionResponse, tags=["Predicciones"])
def predecir(req: PrediccionRequest, _: Usuario = Depends(requiere_rol(*STAFF))):
    salas = [s.model_dump() for s in req.salas]
    return _modelo().predecir(req.evento.model_dump(), salas)


@app.post("/api/v1/predicciones/lote", response_model=PrediccionLoteResponse, tags=["Predicciones"])
def predecir_lote(req: PrediccionLoteRequest, _: Usuario = Depends(requiere_rol(*STAFF))):
    m = _modelo()
    salas = [s.model_dump() for s in req.salas]
    return {"predicciones": [m.predecir(e.model_dump(), salas) for e in req.eventos]}


# ------------------------------------------------------------------ tendencias
@app.get("/api/v1/tendencias", tags=["Tendencias"])
def serie(agrupar: Literal["tipo", "facultad"] = "tipo",
          metrica: Literal["tasa", "asistentes"] = "tasa",
          horizonte: int = Query(3, ge=1, le=12),
          _: Usuario = Depends(requiere_rol(*STAFF))):
    return tendencias.serie_mensual(_modelo().historico, agrupar, metrica, horizonte)


@app.get("/api/v1/tendencias/resumen", tags=["Tendencias"])
def resumen(_: Usuario = Depends(requiere_rol(*STAFF))):
    return tendencias.resumen(_modelo().historico)

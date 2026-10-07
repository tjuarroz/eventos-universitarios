"""Persistencia del modelo entrenado en disco (volumen Docker)."""
from __future__ import annotations

import logging
import threading

import joblib

from .config import DATA_DIR
from .modelo import ModeloAsistencia

log = logging.getLogger("analitica")
_ARCHIVO = DATA_DIR / "modelo.joblib"
_lock = threading.Lock()
_modelo: ModeloAsistencia | None = None


def cargar() -> None:
    global _modelo
    if _ARCHIVO.exists():
        try:
            _modelo = joblib.load(_ARCHIVO)
            log.info("Modelo cargado desde %s (%s registros)", _ARCHIVO, len(_modelo.historico))
        except Exception as e:  # archivo corrupto o de otra versión: se reentrena luego
            log.warning("No se pudo cargar el modelo guardado: %s", e)


def guardar(m: ModeloAsistencia) -> None:
    global _modelo
    with _lock:
        _modelo = m
        DATA_DIR.mkdir(parents=True, exist_ok=True)
        joblib.dump(m, _ARCHIVO)


def actual() -> ModeloAsistencia | None:
    return _modelo

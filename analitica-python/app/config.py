import os
from pathlib import Path

JWT_SECRET = os.getenv("JWT_SECRET", "cambiar-este-secreto-compartido-por-java-y-python-2026")
DATA_DIR = Path(os.getenv("DATA_DIR", "./data"))
CORS_ORIGINS = [o.strip() for o in os.getenv("CORS_ORIGINS", "http://localhost:5173,http://localhost:3000").split(",")]

# Umbral de "baja asistencia": menos del 40 % del aforo de la sala
UMBRAL_BAJA_ASISTENCIA = float(os.getenv("UMBRAL_BAJA_ASISTENCIA", "0.4"))
MIN_REGISTROS_ENTRENAMIENTO = 10

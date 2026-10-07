"""Modelos Pydantic (contratos REST en snake_case, compartidos con el core Java)."""
from datetime import date
from typing import Literal, Optional

from pydantic import BaseModel, Field

Tipo = Literal["CHARLA", "SEMINARIO", "TALLER", "JORNADA", "INSTITUCIONAL"]
Clima = Literal["SOLEADO", "NUBLADO", "LLUVIA"]


class RegistroHistorico(BaseModel):
    evento_id: int
    tipo: Tipo
    facultad: str
    fecha: date
    hora_inicio: str = Field(pattern=r"^\d{2}:\d{2}")
    duracion_min: int = Field(gt=0)
    clima: Clima
    capacidad_sala: int = Field(gt=0)
    inscriptos: int = Field(ge=0)
    asistentes: int = Field(ge=0)


class EntrenarRequest(BaseModel):
    registros: list[RegistroHistorico]


class EventoAPredecir(BaseModel):
    evento_id: int
    tipo: Tipo
    facultad: str
    fecha: date
    hora_inicio: str = Field(pattern=r"^\d{2}:\d{2}")
    duracion_min: int = Field(gt=0)
    clima: Clima = "SOLEADO"
    inscriptos: int = Field(ge=0)
    cupo: int = Field(gt=0)
    capacidad_sala: Optional[int] = None
    sala_id: Optional[int] = None


class SalaInfo(BaseModel):
    id: int
    nombre: str
    capacidad: int
    facultad: Optional[str] = None


class PrediccionRequest(BaseModel):
    evento: EventoAPredecir
    salas: list[SalaInfo] = []


class PrediccionLoteRequest(BaseModel):
    eventos: list[EventoAPredecir]
    salas: list[SalaInfo] = []


class SalaRecomendada(BaseModel):
    id: int
    nombre: str
    capacidad: int
    ocupacion_estimada: float


class PrediccionResponse(BaseModel):
    evento_id: int
    asistencia_estimada: int
    intervalo_inferior: int
    intervalo_superior: int
    prob_sobrecapacidad: float
    prob_baja_asistencia: float
    inscriptos_actuales: int
    inscriptos_proyectados: int
    tasa_estimada: float
    capacidad_referencia: int
    ocupacion_estimada: float
    sala_recomendada_id: Optional[int]
    sala_recomendada: Optional[SalaRecomendada]
    sala_actual_adecuada: Optional[bool]
    modelo: str
    explicacion: list[str]


class PrediccionLoteResponse(BaseModel):
    predicciones: list[PrediccionResponse]

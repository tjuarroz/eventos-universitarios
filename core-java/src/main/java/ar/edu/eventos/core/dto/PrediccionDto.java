package ar.edu.eventos.core.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PrediccionDto(Integer id, Integer eventoId, String eventoTitulo, LocalDate fecha, String tipo,
                            Integer asistenciaEstimada, Integer intervaloInferior, Integer intervaloSuperior,
                            double probSobrecapacidad, double probBajaAsistencia,
                            Integer salaRecomendadaId, String salaRecomendadaNombre, Integer salaRecomendadaCapacidad,
                            Integer salaActualId, String salaActualNombre, Integer salaActualCapacidad,
                            Integer capacidad, long inscriptos, String modelo, JsonNode detalle,
                            LocalDateTime generadaEn) {}

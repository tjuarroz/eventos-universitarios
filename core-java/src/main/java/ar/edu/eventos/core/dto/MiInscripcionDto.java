package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.EstadoEvento;
import ar.edu.eventos.core.domain.EstadoInscripcion;
import ar.edu.eventos.core.domain.TipoEvento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Inscripción vista por el propio usuario: incluye el código que se muestra como QR. */
public record MiInscripcionDto(Integer id, String codigo, EstadoInscripcion estado, LocalDateTime fechaInscripcion,
                               boolean asistio, Integer eventoId, String eventoTitulo, TipoEvento tipo,
                               String facultad, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                               String salaNombre, EstadoEvento estadoEvento) {}

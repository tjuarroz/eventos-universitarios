package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.Clima;
import ar.edu.eventos.core.domain.EstadoEvento;
import ar.edu.eventos.core.domain.TipoEvento;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventoDto(
        Integer id, String titulo, String descripcion, String materiales,
        TipoEvento tipo, String facultad, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
        Integer salaId, String salaNombre, String salaEdificio, Integer salaCapacidad,
        Integer capacidad, Clima clima, EstadoEvento estado,
        Integer organizadorId, String organizadorNombre,
        long inscriptos, long asistentes,
        boolean inscripto, String codigoInscripcion) {}

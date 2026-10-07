package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.Clima;
import ar.edu.eventos.core.domain.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventoRequest(
        @NotBlank @Size(max = 150) String titulo,
        String descripcion,
        String materiales,
        @NotNull TipoEvento tipo,
        @NotBlank @Size(max = 80) String facultad,
        @NotNull LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        Integer salaId,
        @NotNull @Positive Integer capacidad,
        Clima clima) {}

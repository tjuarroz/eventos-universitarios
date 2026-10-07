package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.Categoria;
import ar.edu.eventos.core.domain.EstadoInscripcion;
import ar.edu.eventos.core.domain.MetodoAsistencia;

import java.time.LocalDateTime;

/** Inscripción vista por el organizador / personal de apoyo (lista de asistencia). */
public record InscripcionDto(Integer id, Integer usuarioId, String nombre, String apellido, String email,
                             Categoria categoria, String facultad, String codigo, EstadoInscripcion estado,
                             LocalDateTime fechaInscripcion, boolean asistio, MetodoAsistencia metodo,
                             LocalDateTime registradaEn) {}

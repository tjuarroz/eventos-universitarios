package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.EstadoEvento;
import ar.edu.eventos.core.domain.TipoEvento;

import java.time.LocalDate;
import java.time.LocalTime;

public final class PanelDtos {
    private PanelDtos() {}

    public record PanelEvento(Integer id, String titulo, LocalDate fecha, LocalTime horaInicio, TipoEvento tipo,
                              String facultad, String salaNombre, Integer salaCapacidad, Integer capacidad,
                              long inscriptos, long asistentes, EstadoEvento estado, Integer asistenciaEstimada,
                              Double probSobrecapacidad) {}

    public record Resumen(long eventosTotales, long proximos, long finalizados, long borradores,
                          long inscriptosTotales, long asistentesTotales, double tasaAsistenciaPromedio,
                          long eventosConSobrecapacidad, long eventosConBajaAsistencia) {}
}

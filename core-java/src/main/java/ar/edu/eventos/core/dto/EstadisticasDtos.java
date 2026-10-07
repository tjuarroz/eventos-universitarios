package ar.edu.eventos.core.dto;

import java.util.List;

public final class EstadisticasDtos {
    private EstadisticasDtos() {}

    public record Conteo(String nombre, long inscriptos, long asistentes) {}

    public record EstadisticasEvento(Integer eventoId, String titulo, Integer capacidad, Integer salaCapacidad,
                                     long inscriptos, long asistentes, double tasaAsistencia, Double ocupacionSala,
                                     List<Conteo> porCategoria, List<Conteo> porFacultad, List<Conteo> porMetodo,
                                     PrediccionDto ultimaPrediccion) {}
}

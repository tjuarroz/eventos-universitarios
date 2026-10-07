package ar.edu.eventos.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;
import java.util.List;

/** Contratos (snake_case) con el microservicio Python de analítica. */
public final class AnaliticaDtos {
    private AnaliticaDtos() {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RegistroHistorico(Integer eventoId, String tipo, String facultad, LocalDate fecha, String horaInicio,
                                    int duracionMin, String clima, int capacidadSala, long inscriptos,
                                    long asistentes) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EntrenarRequest(List<RegistroHistorico> registros) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EventoAPredecir(Integer eventoId, String tipo, String facultad, LocalDate fecha, String horaInicio,
                                  int duracionMin, String clima, long inscriptos, int cupo, Integer capacidadSala,
                                  Integer salaId) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SalaInfo(Integer id, String nombre, int capacidad, String facultad) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PrediccionRequest(EventoAPredecir evento, List<SalaInfo> salas) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PrediccionLoteRequest(List<EventoAPredecir> eventos, List<SalaInfo> salas) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PrediccionResponse(Integer eventoId, int asistenciaEstimada, Integer intervaloInferior,
                                     Integer intervaloSuperior, double probSobrecapacidad, double probBajaAsistencia,
                                     Integer salaRecomendadaId, String modelo) {}
}

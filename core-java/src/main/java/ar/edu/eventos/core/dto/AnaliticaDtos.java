package ar.edu.eventos.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;
import java.util.List;

/** Contratos (snake_case) con el microservicio Python de analítica. */
public final class AnaliticaDtos {
    private AnaliticaDtos() {}

    /**
     * Un evento finalizado del histórico. {@code satisfaccion} es el puntaje medio (1 a 5) de su encuesta
     * ({@code null} si nadie respondió); el modelo lo usa como "satisfacción previa" del organizador.
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RegistroHistorico(Integer eventoId, String tipo, String facultad, LocalDate fecha, String horaInicio,
                                    int duracionMin, String clima, int capacidadSala, long inscriptos,
                                    long asistentes, Integer organizadorId, Double satisfaccion,
                                    long respuestas) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EntrenarRequest(List<RegistroHistorico> registros) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EventoAPredecir(Integer eventoId, String tipo, String facultad, LocalDate fecha, String horaInicio,
                                  int duracionMin, String clima, long inscriptos, int cupo, Integer capacidadSala,
                                  Integer salaId, Integer organizadorId) {}

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

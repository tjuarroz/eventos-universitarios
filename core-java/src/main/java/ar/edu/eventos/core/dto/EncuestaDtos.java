package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.TipoEvento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Encuesta de satisfacción posterior al evento. */
public final class EncuestaDtos {
    private EncuestaDtos() {}

    /** Evento al que el usuario asistió y cuya encuesta todavía puede responder (hasta {@code cierra}). */
    public record EncuestaPendiente(Integer inscripcionId, Integer eventoId, String titulo, TipoEvento tipo,
                                    String facultad, LocalDate fecha, LocalTime horaInicio, String salaNombre,
                                    String organizadorNombre, LocalDate cierra) {}

    public record ResponderEncuestaRequest(
            @NotNull Integer inscripcionId,
            @NotNull @Min(1) @Max(5) Integer puntaje,
            @NotNull Boolean recomendaria,
            @Size(max = 500) String comentario) {}

    public record EncuestaRespondida(Integer id, Integer eventoId, int puntaje, boolean recomendaria,
                                     LocalDateTime respondidaEn) {}

    /** Resultados de la encuesta de un evento (anónimos: no se muestra quién respondió qué). */
    public record ResumenEncuestaEvento(Integer eventoId, String titulo, long asistentes, long respuestas,
                                        double tasaRespuesta, Double promedio, Double porcentajeRecomienda,
                                        List<Long> distribucion, List<String> comentarios) {}

    public record SatisfaccionGrupo(String grupo, long eventos, long respuestas, double promedio,
                                    double porcentajeRecomienda) {}

    public record SatisfaccionMes(String mes, long respuestas, double promedio) {}

    public record SatisfaccionGeneral(long respuestas, Double promedio, Double porcentajeRecomienda,
                                      Double tasaRespuesta, List<SatisfaccionGrupo> grupos,
                                      List<SatisfaccionMes> porMes) {}
}

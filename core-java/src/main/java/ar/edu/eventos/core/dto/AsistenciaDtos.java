package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.MetodoAsistencia;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class AsistenciaDtos {
    private AsistenciaDtos() {}

    /** Se indica inscripcionId (marcado desde la lista) o codigo (tipeado o leído del QR). */
    public record RegistrarAsistenciaRequest(Integer inscripcionId, String codigo, @NotNull MetodoAsistencia metodo) {}

    public record AsistenciaDto(Integer inscripcionId, String nombreCompleto, String email, String codigo,
                                MetodoAsistencia metodo, LocalDateTime registradaEn, boolean yaRegistrada) {}
}

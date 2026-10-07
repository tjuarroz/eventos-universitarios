package ar.edu.eventos.core.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(int estado, String error, String mensaje, Map<String, String> campos,
                            LocalDateTime timestamp) {
    public static ErrorResponse of(int estado, String error, String mensaje) {
        return new ErrorResponse(estado, error, mensaje, null, LocalDateTime.now());
    }
}

package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.Categoria;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record RegistroRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
            @NotNull Categoria categoria,
            @Size(max = 80) String facultad) {}

    public record AuthResponse(String token, UsuarioDto usuario) {}
}

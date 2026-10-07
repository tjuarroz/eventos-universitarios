package ar.edu.eventos.core.web;

import ar.edu.eventos.core.dto.AuthDtos.AuthResponse;
import ar.edu.eventos.core.dto.AuthDtos.LoginRequest;
import ar.edu.eventos.core.dto.AuthDtos.RegistroRequest;
import ar.edu.eventos.core.dto.UsuarioDto;
import ar.edu.eventos.core.security.Actual;
import ar.edu.eventos.core.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve un JWT")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un usuario (rol USUARIO) y devuelve un JWT")
    public AuthResponse registro(@Valid @RequestBody RegistroRequest req) {
        return auth.registrar(req);
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del usuario autenticado")
    public UsuarioDto me() {
        return auth.perfil(Actual.usuario().id());
    }
}

package ar.edu.eventos.core.web;

import ar.edu.eventos.core.domain.Rol;
import ar.edu.eventos.core.domain.RolNombre;
import ar.edu.eventos.core.dto.UsuarioDto;
import ar.edu.eventos.core.security.Actual;
import ar.edu.eventos.core.service.UsuarioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios y roles")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    public record CambioRol(@NotNull RolNombre rol) {}

    public record CambioActivo(boolean activo) {}

    @GetMapping("/usuarios")
    public Page<UsuarioDto> buscar(@RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   @RequestParam(defaultValue = "20") int tamanio) {
        return service.buscar(q, pagina, tamanio);
    }

    @GetMapping("/roles")
    public List<Rol> roles() {
        return service.roles();
    }

    @PutMapping("/usuarios/{id}/rol")
    public UsuarioDto cambiarRol(@PathVariable Integer id, @RequestBody @jakarta.validation.Valid CambioRol body) {
        return service.cambiarRol(id, body.rol(), Actual.usuario().id());
    }

    @PutMapping("/usuarios/{id}/activo")
    public UsuarioDto cambiarActivo(@PathVariable Integer id, @RequestBody CambioActivo body) {
        return service.cambiarActivo(id, body.activo(), Actual.usuario().id());
    }
}

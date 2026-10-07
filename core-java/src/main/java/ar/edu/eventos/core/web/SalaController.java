package ar.edu.eventos.core.web;

import ar.edu.eventos.core.dto.SalaDto;
import ar.edu.eventos.core.service.SalaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salas")
@Tag(name = "Salas")
public class SalaController {

    private final SalaService service;

    public SalaController(SalaService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalaDto> listar(@RequestParam(defaultValue = "false") boolean soloActivas) {
        return service.listar(soloActivas);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SalaDto crear(@Valid @RequestBody SalaDto dto) {
        return service.crear(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SalaDto actualizar(@PathVariable Integer id, @Valid @RequestBody SalaDto dto) {
        return service.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
    }
}

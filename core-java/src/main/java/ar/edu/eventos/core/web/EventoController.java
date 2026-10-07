package ar.edu.eventos.core.web;

import ar.edu.eventos.core.domain.Clima;
import ar.edu.eventos.core.domain.EstadoEvento;
import ar.edu.eventos.core.domain.TipoEvento;
import ar.edu.eventos.core.dto.EventoDto;
import ar.edu.eventos.core.dto.EventoRequest;
import ar.edu.eventos.core.security.Actual;
import ar.edu.eventos.core.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos")
public class EventoController {

    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }

    @GetMapping("/opciones")
    @Operation(summary = "Valores posibles de tipo, facultad y clima (para filtros y formularios)")
    public Map<String, Object> opciones() {
        return Map.of("tipos", TipoEvento.values(), "facultades", EventoService.FACULTADES,
                "climas", Clima.values(), "estados", EstadoEvento.values());
    }

    @GetMapping
    @Operation(summary = "Lista eventos con filtros (por defecto: publicados desde hoy)")
    public List<EventoDto> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false) String facultad,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoEvento estado) {
        return service.listar(new EventoService.Filtro(desde, hasta, tipo, facultad, q, estado), Actual.usuario());
    }

    @GetMapping("/{id}")
    public EventoDto obtener(@PathVariable Integer id) {
        return service.obtener(id, Actual.usuario());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    @Operation(summary = "Crea un evento en estado BORRADOR")
    public EventoDto crear(@Valid @RequestBody EventoRequest req) {
        return service.crear(req, Actual.usuario());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public EventoDto actualizar(@PathVariable Integer id, @Valid @RequestBody EventoRequest req) {
        return service.actualizar(id, req, Actual.usuario());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id, Actual.usuario());
    }

    @PostMapping("/{id}/publicar")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    @Operation(summary = "Publica el evento para que los usuarios se inscriban")
    public EventoDto publicar(@PathVariable Integer id) {
        return service.cambiarEstado(id, EstadoEvento.PUBLICADO, Actual.usuario());
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public EventoDto finalizar(@PathVariable Integer id) {
        return service.cambiarEstado(id, EstadoEvento.FINALIZADO, Actual.usuario());
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    public EventoDto cancelar(@PathVariable Integer id) {
        return service.cambiarEstado(id, EstadoEvento.CANCELADO, Actual.usuario());
    }
}

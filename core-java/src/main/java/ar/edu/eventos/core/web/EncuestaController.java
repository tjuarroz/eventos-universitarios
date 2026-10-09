package ar.edu.eventos.core.web;

import ar.edu.eventos.core.dto.EncuestaDtos.*;
import ar.edu.eventos.core.security.Actual;
import ar.edu.eventos.core.service.EncuestaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Encuestas de satisfacción")
public class EncuestaController {

    private final EncuestaService encuestas;

    public EncuestaController(EncuestaService encuestas) {
        this.encuestas = encuestas;
    }

    @GetMapping("/encuestas/pendientes")
    @Operation(summary = "Encuestas que el usuario autenticado tiene para responder (eventos finalizados a los que asistió)")
    public List<EncuestaPendiente> pendientes() {
        return encuestas.pendientes(Actual.usuario());
    }

    @PostMapping("/encuestas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Responde la encuesta de satisfacción de un evento (una sola vez por asistente)")
    public EncuestaRespondida responder(@Valid @RequestBody ResponderEncuestaRequest req) {
        return encuestas.responder(req, Actual.usuario());
    }

    @GetMapping("/eventos/{id}/encuestas")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR','APOYO')")
    @Operation(summary = "Resultados de la encuesta de un evento: promedio, distribución, % que recomienda y comentarios")
    public ResumenEncuestaEvento delEvento(@PathVariable Integer id) {
        return encuestas.resumenEvento(id, Actual.usuario());
    }

    @GetMapping("/encuestas/satisfaccion")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR','APOYO')")
    @Operation(summary = "Satisfacción general agrupada por tipo, facultad u organizador (sólo admin), y por mes")
    public SatisfaccionGeneral satisfaccion(@RequestParam(defaultValue = "tipo") String agrupar) {
        return encuestas.general(agrupar, Actual.usuario());
    }
}

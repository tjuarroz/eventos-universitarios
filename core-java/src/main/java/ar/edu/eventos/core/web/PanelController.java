package ar.edu.eventos.core.web;

import ar.edu.eventos.core.domain.EstadoEvento;
import ar.edu.eventos.core.dto.AnaliticaDtos.RegistroHistorico;
import ar.edu.eventos.core.dto.PanelDtos.PanelEvento;
import ar.edu.eventos.core.dto.PanelDtos.Resumen;
import ar.edu.eventos.core.dto.PrediccionDto;
import ar.edu.eventos.core.security.Actual;
import ar.edu.eventos.core.service.PanelService;
import ar.edu.eventos.core.service.PrediccionService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR','APOYO')")
@Tag(name = "Panel del organizador y predicciones")
public class PanelController {

    private final PanelService panel;
    private final PrediccionService predicciones;

    public PanelController(PanelService panel, PrediccionService predicciones) {
        this.panel = panel;
        this.predicciones = predicciones;
    }

    @GetMapping("/panel/eventos")
    @Operation(summary = "Tabla del panel: título, fecha, inscriptos, asistentes, estado y última predicción")
    public List<PanelEvento> eventos(
            @RequestParam(required = false) EstadoEvento estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return panel.eventos(Actual.usuario(), estado, desde, hasta);
    }

    @GetMapping("/panel/resumen")
    public Resumen resumen() {
        return panel.resumen(Actual.usuario());
    }

    @GetMapping("/panel/historico")
    @Operation(summary = "Dataset histórico de eventos finalizados (el mismo que se envía a analítica)")
    public List<RegistroHistorico> historico() {
        return panel.historico();
    }

    @PostMapping("/eventos/{id}/prediccion")
    @Operation(summary = "Genera una nueva predicción de asistencia para el evento")
    public PrediccionDto predecir(@PathVariable Integer id) {
        return predicciones.predecir(id, Actual.usuario());
    }

    @GetMapping("/eventos/{id}/prediccion")
    @Operation(summary = "Última predicción guardada del evento (204 si no hay)")
    public ResponseEntity<PrediccionDto> ultima(@PathVariable Integer id) {
        return predicciones.ultima(id).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/predicciones/proximos")
    @Operation(summary = "Predicciones de los próximos eventos (para el dashboard)")
    public List<PrediccionDto> proximos(@RequestParam(defaultValue = "false") boolean regenerar) {
        return predicciones.proximos(Actual.usuario(), regenerar);
    }

    @PostMapping("/analitica/entrenar")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR')")
    @Operation(summary = "Reentrena los modelos con el histórico actual")
    public JsonNode entrenar() {
        return predicciones.entrenar();
    }
}

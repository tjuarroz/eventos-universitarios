package ar.edu.eventos.core.web;

import ar.edu.eventos.core.dto.AsistenciaDtos.AsistenciaDto;
import ar.edu.eventos.core.dto.AsistenciaDtos.RegistrarAsistenciaRequest;
import ar.edu.eventos.core.dto.EstadisticasDtos.EstadisticasEvento;
import ar.edu.eventos.core.dto.InscripcionDto;
import ar.edu.eventos.core.dto.MiInscripcionDto;
import ar.edu.eventos.core.security.Actual;
import ar.edu.eventos.core.service.AsistenciaService;
import ar.edu.eventos.core.service.InscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Inscripciones y asistencia")
public class InscripcionController {

    private final InscripcionService inscripciones;
    private final AsistenciaService asistencias;

    public InscripcionController(InscripcionService inscripciones, AsistenciaService asistencias) {
        this.inscripciones = inscripciones;
        this.asistencias = asistencias;
    }

    @PostMapping("/eventos/{id}/inscripcion")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscribe al usuario autenticado; devuelve el código/QR de acceso")
    public MiInscripcionDto inscribirme(@PathVariable Integer id) {
        return inscripciones.inscribir(id, Actual.usuario());
    }

    @DeleteMapping("/eventos/{id}/inscripcion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarInscripcion(@PathVariable Integer id) {
        inscripciones.cancelar(id, Actual.usuario());
    }

    @GetMapping("/inscripciones/mias")
    public List<MiInscripcionDto> mias() {
        return inscripciones.mias(Actual.usuario());
    }

    @GetMapping("/eventos/{id}/inscriptos")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR','APOYO')")
    public List<InscripcionDto> inscriptos(@PathVariable Integer id) {
        return inscripciones.delEvento(id, Actual.usuario());
    }

    @PostMapping("/eventos/{id}/asistencias")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR','APOYO')")
    @Operation(summary = "Registra asistencia por lista, código o QR")
    public AsistenciaDto registrarAsistencia(@PathVariable Integer id,
                                             @Valid @RequestBody RegistrarAsistenciaRequest req) {
        return asistencias.registrar(id, req, Actual.usuario());
    }

    @DeleteMapping("/eventos/{id}/asistencias/{inscripcionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR','APOYO')")
    public void anularAsistencia(@PathVariable Integer id, @PathVariable Integer inscripcionId) {
        asistencias.anular(id, inscripcionId, Actual.usuario());
    }

    @GetMapping("/eventos/{id}/estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZADOR','APOYO')")
    public EstadisticasEvento estadisticas(@PathVariable Integer id) {
        return asistencias.estadisticas(id, Actual.usuario());
    }
}

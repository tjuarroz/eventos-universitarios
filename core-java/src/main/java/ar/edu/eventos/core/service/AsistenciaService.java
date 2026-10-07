package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.*;
import ar.edu.eventos.core.dto.AsistenciaDtos.AsistenciaDto;
import ar.edu.eventos.core.dto.AsistenciaDtos.RegistrarAsistenciaRequest;
import ar.edu.eventos.core.dto.EstadisticasDtos.Conteo;
import ar.edu.eventos.core.dto.EstadisticasDtos.EstadisticasEvento;
import ar.edu.eventos.core.exception.ApiException;
import ar.edu.eventos.core.repository.AsistenciaRepository;
import ar.edu.eventos.core.repository.InscripcionRepository;
import ar.edu.eventos.core.repository.UsuarioRepository;
import ar.edu.eventos.core.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistencias;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final EventoService eventoService;
    private final PrediccionService prediccionService;

    public AsistenciaService(AsistenciaRepository asistencias, InscripcionRepository inscripciones,
                             UsuarioRepository usuarios, EventoService eventoService,
                             PrediccionService prediccionService) {
        this.asistencias = asistencias;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
        this.eventoService = eventoService;
        this.prediccionService = prediccionService;
    }

    /** Registra la asistencia por lista (inscripcionId), código tipeado o QR (codigo). Es idempotente. */
    @Transactional
    public AsistenciaDto registrar(Integer eventoId, RegistrarAsistenciaRequest req, UsuarioAutenticado u) {
        Evento e = eventoService.buscar(eventoId);
        if (!eventoService.puedeOperar(e, u)) {
            throw ApiException.prohibido("No podés registrar asistencia en este evento");
        }
        if (e.getEstado() != EstadoEvento.PUBLICADO && e.getEstado() != EstadoEvento.FINALIZADO) {
            throw ApiException.conflicto("Sólo se registra asistencia en eventos publicados o finalizados");
        }
        Inscripcion i;
        if (req.inscripcionId() != null) {
            i = inscripciones.findById(req.inscripcionId())
                    .orElseThrow(() -> ApiException.noEncontrado("Inscripción"));
        } else if (req.codigo() != null && !req.codigo().isBlank()) {
            i = inscripciones.findByCodigoIgnoreCase(req.codigo().trim())
                    .orElseThrow(() -> ApiException.noEncontrado("Código de inscripción"));
        } else {
            throw ApiException.invalido("Indicá la inscripción o el código");
        }
        if (!i.getEvento().getId().equals(eventoId)) {
            throw ApiException.conflicto("El código corresponde a otro evento");
        }
        if (i.getEstado() != EstadoInscripcion.ACTIVA) {
            throw ApiException.conflicto("La inscripción está cancelada");
        }
        Usuario asistente = i.getUsuario();
        var previa = asistencias.findByInscripcionId(i.getId());
        if (previa.isPresent()) {
            Asistencia a = previa.get();
            return new AsistenciaDto(i.getId(), asistente.getNombreCompleto(), asistente.getEmail(), i.getCodigo(),
                    a.getMetodo(), a.getRegistradaEn(), true);
        }
        Asistencia a = new Asistencia();
        a.setInscripcion(i);
        a.setMetodo(req.metodo());
        a.setRegistradaPor(usuarios.getReferenceById(u.id()));
        asistencias.save(a);
        return new AsistenciaDto(i.getId(), asistente.getNombreCompleto(), asistente.getEmail(), i.getCodigo(),
                a.getMetodo(), a.getRegistradaEn(), false);
    }

    @Transactional
    public void anular(Integer eventoId, Integer inscripcionId, UsuarioAutenticado u) {
        Evento e = eventoService.buscar(eventoId);
        if (!eventoService.puedeOperar(e, u)) {
            throw ApiException.prohibido("No podés modificar la asistencia de este evento");
        }
        Asistencia a = asistencias.findByInscripcionId(inscripcionId)
                .filter(x -> x.getInscripcion().getEvento().getId().equals(eventoId))
                .orElseThrow(() -> ApiException.noEncontrado("Asistencia"));
        asistencias.delete(a);
    }

    @Transactional(readOnly = true)
    public EstadisticasEvento estadisticas(Integer eventoId, UsuarioAutenticado u) {
        Evento e = eventoService.buscar(eventoId);
        if (!eventoService.puedeOperar(e, u)) {
            throw ApiException.prohibido("No podés ver las estadísticas de este evento");
        }
        List<Inscripcion> ins = inscripciones.findActivasConUsuario(eventoId);
        Map<Integer, Asistencia> asis = asistencias.findDelEvento(eventoId).stream()
                .collect(Collectors.toMap(a -> a.getInscripcion().getId(), Function.identity()));

        Map<String, long[]> porCat = new TreeMap<>();
        Map<String, long[]> porFac = new TreeMap<>();
        Map<String, long[]> porMet = new TreeMap<>();
        for (Inscripcion i : ins) {
            boolean asistio = asis.containsKey(i.getId());
            sumar(porCat, i.getUsuario().getCategoria().name(), asistio);
            sumar(porFac, Optional.ofNullable(i.getUsuario().getFacultad()).orElse("Sin facultad"), asistio);
            if (asistio) sumar(porMet, asis.get(i.getId()).getMetodo().name(), true);
        }
        long inscriptos = ins.size();
        long asistentes = asis.size();
        Integer salaCap = e.getSala() == null ? null : e.getSala().getCapacidad();
        return new EstadisticasEvento(e.getId(), e.getTitulo(), e.getCapacidad(), salaCap, inscriptos, asistentes,
                inscriptos == 0 ? 0 : (double) asistentes / inscriptos,
                salaCap == null ? null : (double) asistentes / salaCap,
                aLista(porCat), aLista(porFac), aLista(porMet),
                prediccionService.ultima(eventoId).orElse(null));
    }

    private static void sumar(Map<String, long[]> m, String clave, boolean asistio) {
        long[] v = m.computeIfAbsent(clave, k -> new long[2]);
        v[0]++;
        if (asistio) v[1]++;
    }

    private static List<Conteo> aLista(Map<String, long[]> m) {
        return m.entrySet().stream().map(x -> new Conteo(x.getKey(), x.getValue()[0], x.getValue()[1])).toList();
    }
}

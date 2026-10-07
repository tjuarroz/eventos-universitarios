package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.*;
import ar.edu.eventos.core.dto.InscripcionDto;
import ar.edu.eventos.core.dto.MiInscripcionDto;
import ar.edu.eventos.core.exception.ApiException;
import ar.edu.eventos.core.repository.AsistenciaRepository;
import ar.edu.eventos.core.repository.InscripcionRepository;
import ar.edu.eventos.core.repository.UsuarioRepository;
import ar.edu.eventos.core.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InscripcionService {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sin 0/O, 1/I
    private final SecureRandom random = new SecureRandom();

    private final InscripcionRepository inscripciones;
    private final AsistenciaRepository asistencias;
    private final UsuarioRepository usuarios;
    private final EventoService eventoService;

    public InscripcionService(InscripcionRepository inscripciones, AsistenciaRepository asistencias,
                              UsuarioRepository usuarios, EventoService eventoService) {
        this.inscripciones = inscripciones;
        this.asistencias = asistencias;
        this.usuarios = usuarios;
        this.eventoService = eventoService;
    }

    @Transactional
    public MiInscripcionDto inscribir(Integer eventoId, UsuarioAutenticado u) {
        Evento e = eventoService.buscar(eventoId);
        if (e.getEstado() != EstadoEvento.PUBLICADO) {
            throw ApiException.conflicto("El evento no está abierto a inscripción");
        }
        if (LocalDateTime.of(e.getFecha(), e.getHoraFin()).isBefore(LocalDateTime.now())) {
            throw ApiException.conflicto("El evento ya terminó");
        }
        var existente = inscripciones.findByEventoIdAndUsuarioId(eventoId, u.id());
        if (existente.isPresent() && existente.get().getEstado() == EstadoInscripcion.ACTIVA) {
            throw ApiException.conflicto("Ya estás inscripto en este evento");
        }
        long ocupados = inscripciones.countByEventoIdAndEstado(eventoId, EstadoInscripcion.ACTIVA);
        if (ocupados >= e.getCapacidad()) {
            throw ApiException.conflicto("No quedan cupos disponibles");
        }
        Inscripcion i = existente.orElseGet(Inscripcion::new);
        if (i.getId() == null) {
            i.setEvento(e);
            i.setUsuario(usuarios.getReferenceById(u.id()));
            i.setCodigo(nuevoCodigo());
        }
        i.setEstado(EstadoInscripcion.ACTIVA);
        i.setFechaInscripcion(LocalDateTime.now());
        inscripciones.save(i);
        return aMia(i, false);
    }

    @Transactional
    public void cancelar(Integer eventoId, UsuarioAutenticado u) {
        Inscripcion i = inscripciones.findByEventoIdAndUsuarioId(eventoId, u.id())
                .filter(x -> x.getEstado() == EstadoInscripcion.ACTIVA)
                .orElseThrow(() -> ApiException.noEncontrado("Inscripción"));
        if (asistencias.findByInscripcionId(i.getId()).isPresent()) {
            throw ApiException.conflicto("Tu asistencia ya fue registrada; no podés cancelar la inscripción");
        }
        i.setEstado(EstadoInscripcion.CANCELADA);
    }

    @Transactional(readOnly = true)
    public List<MiInscripcionDto> mias(UsuarioAutenticado u) {
        List<Inscripcion> lista = inscripciones.findDelUsuario(u.id());
        Set<Integer> conAsistencia = lista.stream()
                .filter(i -> asistencias.findByInscripcionId(i.getId()).isPresent())
                .map(Inscripcion::getId).collect(Collectors.toSet());
        return lista.stream().map(i -> aMia(i, conAsistencia.contains(i.getId()))).toList();
    }

    /** Lista de inscriptos con su estado de asistencia (vista del organizador / personal de apoyo). */
    @Transactional(readOnly = true)
    public List<InscripcionDto> delEvento(Integer eventoId, UsuarioAutenticado u) {
        Evento e = eventoService.buscar(eventoId);
        if (!eventoService.puedeOperar(e, u)) {
            throw ApiException.prohibido("No podés ver los inscriptos de este evento");
        }
        Map<Integer, Asistencia> asis = asistencias.findDelEvento(eventoId).stream()
                .collect(Collectors.toMap(a -> a.getInscripcion().getId(), Function.identity()));
        return inscripciones.findActivasConUsuario(eventoId).stream().map(i -> {
            Usuario us = i.getUsuario();
            Asistencia a = asis.get(i.getId());
            return new InscripcionDto(i.getId(), us.getId(), us.getNombre(), us.getApellido(), us.getEmail(),
                    us.getCategoria(), us.getFacultad(), i.getCodigo(), i.getEstado(), i.getFechaInscripcion(),
                    a != null, a == null ? null : a.getMetodo(), a == null ? null : a.getRegistradaEn());
        }).toList();
    }

    private MiInscripcionDto aMia(Inscripcion i, boolean asistio) {
        Evento e = i.getEvento();
        return new MiInscripcionDto(i.getId(), i.getCodigo(), i.getEstado(), i.getFechaInscripcion(), asistio,
                e.getId(), e.getTitulo(), e.getTipo(), e.getFacultad(), e.getFecha(), e.getHoraInicio(),
                e.getHoraFin(), e.getSala() == null ? null : e.getSala().getNombre(), e.getEstado());
    }

    private String nuevoCodigo() {
        String c;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int k = 0; k < 8; k++) sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
            c = sb.toString();
        } while (inscripciones.existsByCodigo(c));
        return c;
    }
}

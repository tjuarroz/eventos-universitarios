package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.*;
import ar.edu.eventos.core.dto.EventoDto;
import ar.edu.eventos.core.dto.EventoRequest;
import ar.edu.eventos.core.exception.ApiException;
import ar.edu.eventos.core.repository.EventoRepository;
import ar.edu.eventos.core.repository.InscripcionRepository;
import ar.edu.eventos.core.repository.SalaRepository;
import ar.edu.eventos.core.repository.UsuarioRepository;
import ar.edu.eventos.core.security.UsuarioAutenticado;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventoService {

    public static final List<String> FACULTADES = List.of(
            "Ingeniería", "Ciencias Económicas", "Derecho", "Ciencias Sociales", "Ciencias Exactas");

    private final EventoRepository eventos;
    private final SalaRepository salas;
    private final UsuarioRepository usuarios;
    private final InscripcionRepository inscripciones;
    private final Conteos conteos;

    public EventoService(EventoRepository eventos, SalaRepository salas, UsuarioRepository usuarios,
                         InscripcionRepository inscripciones, Conteos conteos) {
        this.eventos = eventos;
        this.salas = salas;
        this.usuarios = usuarios;
        this.inscripciones = inscripciones;
        this.conteos = conteos;
    }

    /** Filtros del listado. Los usuarios comunes sólo ven eventos PUBLICADOS. */
    public record Filtro(LocalDate desde, LocalDate hasta, TipoEvento tipo, String facultad, String q,
                         EstadoEvento estado) {}

    @Transactional(readOnly = true)
    public List<EventoDto> listar(Filtro f, UsuarioAutenticado u) {
        EstadoEvento estado = u.esStaff() && f.estado() != null ? f.estado() : EstadoEvento.PUBLICADO;
        LocalDate desde = f.desde() != null ? f.desde()
                : (estado == EstadoEvento.PUBLICADO ? LocalDate.now() : null);

        Specification<Evento> spec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("sala", JoinType.LEFT);
                root.fetch("organizador", JoinType.LEFT);
            }
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("estado"), estado));
            if (desde != null) p.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            if (f.hasta() != null) p.add(cb.lessThanOrEqualTo(root.get("fecha"), f.hasta()));
            if (f.tipo() != null) p.add(cb.equal(root.get("tipo"), f.tipo()));
            if (f.facultad() != null && !f.facultad().isBlank()) p.add(cb.equal(root.get("facultad"), f.facultad()));
            if (f.q() != null && !f.q().isBlank()) {
                p.add(cb.like(cb.lower(root.get("titulo")), "%" + f.q().trim().toLowerCase() + "%"));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        Sort orden = estado == EstadoEvento.FINALIZADO
                ? Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("horaInicio"))
                : Sort.by("fecha", "horaInicio");
        return mapear(eventos.findAll(spec, orden), u.id());
    }

    @Transactional(readOnly = true)
    public EventoDto obtener(Integer id, UsuarioAutenticado u) {
        Evento e = buscar(id);
        if (e.getEstado() == EstadoEvento.BORRADOR && !puedeGestionar(e, u) && !"APOYO".equals(u.rol())) {
            throw ApiException.noEncontrado("Evento");
        }
        return mapear(List.of(e), u.id()).get(0);
    }

    @Transactional
    public EventoDto crear(EventoRequest req, UsuarioAutenticado u) {
        Evento e = new Evento();
        e.setOrganizador(usuarios.getReferenceById(u.id()));
        aplicar(req, e);
        eventos.save(e);
        return mapear(List.of(e), u.id()).get(0);
    }

    @Transactional
    public EventoDto actualizar(Integer id, EventoRequest req, UsuarioAutenticado u) {
        Evento e = buscarGestionable(id, u);
        if (e.getEstado() == EstadoEvento.FINALIZADO || e.getEstado() == EstadoEvento.CANCELADO) {
            throw ApiException.conflicto("No se puede editar un evento " + e.getEstado().name().toLowerCase());
        }
        aplicar(req, e);
        return mapear(List.of(e), u.id()).get(0);
    }

    @Transactional
    public void eliminar(Integer id, UsuarioAutenticado u) {
        Evento e = buscarGestionable(id, u);
        long ins = inscripciones.countByEventoIdAndEstado(id, EstadoInscripcion.ACTIVA);
        if (ins > 0 && !u.esAdmin()) {
            throw ApiException.conflicto("El evento tiene " + ins + " inscriptos: cancelalo en lugar de eliminarlo");
        }
        eventos.delete(e);
    }

    @Transactional
    public EventoDto cambiarEstado(Integer id, EstadoEvento nuevo, UsuarioAutenticado u) {
        Evento e = buscarGestionable(id, u);
        EstadoEvento actual = e.getEstado();
        boolean valido = switch (nuevo) {
            case PUBLICADO -> actual == EstadoEvento.BORRADOR;
            case FINALIZADO -> actual == EstadoEvento.PUBLICADO;
            case CANCELADO -> actual == EstadoEvento.BORRADOR || actual == EstadoEvento.PUBLICADO;
            case BORRADOR -> actual == EstadoEvento.PUBLICADO
                    && inscripciones.countByEventoIdAndEstado(id, EstadoInscripcion.ACTIVA) == 0;
        };
        if (!valido) {
            throw ApiException.conflicto("No se puede pasar de " + actual + " a " + nuevo);
        }
        if (nuevo == EstadoEvento.PUBLICADO && e.getFecha().isBefore(LocalDate.now())) {
            throw ApiException.conflicto("No se puede publicar un evento con fecha pasada");
        }
        e.setEstado(nuevo);
        return mapear(List.of(e), u.id()).get(0);
    }

    // ------------------------------------------------------------------ helpers

    public Evento buscar(Integer id) {
        return eventos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Evento"));
    }

    public boolean puedeGestionar(Evento e, UsuarioAutenticado u) {
        return u.esAdmin() || (u.esOrganizador() && e.getOrganizador().getId().equals(u.id()));
    }

    /** Admin, personal de apoyo o el organizador dueño del evento (para asistencia y estadísticas). */
    public boolean puedeOperar(Evento e, UsuarioAutenticado u) {
        return puedeGestionar(e, u) || "APOYO".equals(u.rol());
    }

    public Evento buscarGestionable(Integer id, UsuarioAutenticado u) {
        Evento e = buscar(id);
        if (!puedeGestionar(e, u)) {
            throw ApiException.prohibido("Sólo el organizador del evento o un administrador pueden modificarlo");
        }
        return e;
    }

    private void aplicar(EventoRequest r, Evento e) {
        if (!r.horaFin().isAfter(r.horaInicio())) {
            throw ApiException.invalido("La hora de fin debe ser posterior a la de inicio");
        }
        if (!FACULTADES.contains(r.facultad())) {
            throw ApiException.invalido("Facultad inválida");
        }
        Sala sala = null;
        if (r.salaId() != null) {
            sala = salas.findById(r.salaId()).orElseThrow(() -> ApiException.noEncontrado("Sala"));
            if (!sala.isActiva()) throw ApiException.invalido("La sala seleccionada no está activa");
            var choques = eventos.buscarSuperposiciones(sala.getId(), r.fecha(), r.horaInicio(), r.horaFin(),
                    e.getId() == null ? -1 : e.getId());
            if (!choques.isEmpty()) {
                Evento c = choques.get(0);
                throw ApiException.conflicto("La sala " + sala.getNombre() + " ya está reservada ese día de "
                        + c.getHoraInicio() + " a " + c.getHoraFin() + " (\"" + c.getTitulo() + "\")");
            }
        }
        if (e.getId() != null) {
            long ins = inscripciones.countByEventoIdAndEstado(e.getId(), EstadoInscripcion.ACTIVA);
            if (r.capacidad() < ins) {
                throw ApiException.conflicto("El cupo no puede ser menor a los " + ins + " inscriptos actuales");
            }
        }
        e.setTitulo(r.titulo().trim());
        e.setDescripcion(r.descripcion());
        e.setMateriales(r.materiales());
        e.setTipo(r.tipo());
        e.setFacultad(r.facultad());
        e.setFecha(r.fecha());
        e.setHoraInicio(r.horaInicio());
        e.setHoraFin(r.horaFin());
        e.setSala(sala);
        e.setCapacidad(r.capacidad());
        e.setClima(r.clima() == null ? Clima.SOLEADO : r.clima());
    }

    /** Convierte a DTO agregando conteos y si el usuario actual está inscripto. */
    public List<EventoDto> mapear(List<Evento> lista, Integer usuarioId) {
        List<Integer> ids = lista.stream().map(Evento::getId).toList();
        var cont = conteos.de(ids);
        Map<Integer, String> mias = ids.isEmpty() ? Map.of()
                : inscripciones.findActivasDelUsuarioEn(usuarioId, ids).stream()
                    .collect(Collectors.toMap(i -> i.getEvento().getId(), Inscripcion::getCodigo));
        return lista.stream().map(e -> {
            Sala s = e.getSala();
            Usuario o = e.getOrganizador();
            var c = cont.get(e.getId());
            return new EventoDto(e.getId(), e.getTitulo(), e.getDescripcion(), e.getMateriales(), e.getTipo(),
                    e.getFacultad(), e.getFecha(), e.getHoraInicio(), e.getHoraFin(),
                    s == null ? null : s.getId(), s == null ? null : s.getNombre(),
                    s == null ? null : s.getEdificio(), s == null ? null : s.getCapacidad(),
                    e.getCapacidad(), e.getClima(), e.getEstado(), o.getId(), o.getNombreCompleto(),
                    c.inscriptos(), c.asistentes(), mias.containsKey(e.getId()), mias.get(e.getId()));
        }).toList();
    }
}

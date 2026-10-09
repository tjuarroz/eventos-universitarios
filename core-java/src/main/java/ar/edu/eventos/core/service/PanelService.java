package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.EstadoEvento;
import ar.edu.eventos.core.domain.Evento;
import ar.edu.eventos.core.domain.Prediccion;
import ar.edu.eventos.core.dto.AnaliticaDtos.RegistroHistorico;
import ar.edu.eventos.core.dto.PanelDtos.PanelEvento;
import ar.edu.eventos.core.dto.PanelDtos.Resumen;
import ar.edu.eventos.core.repository.EventoRepository;
import ar.edu.eventos.core.repository.PrediccionRepository;
import ar.edu.eventos.core.security.UsuarioAutenticado;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PanelService {

    private final EventoRepository eventos;
    private final PrediccionRepository predicciones;
    private final Conteos conteos;
    private final JdbcTemplate jdbc;

    public PanelService(EventoRepository eventos, PrediccionRepository predicciones, Conteos conteos,
                        JdbcTemplate jdbc) {
        this.eventos = eventos;
        this.predicciones = predicciones;
        this.conteos = conteos;
        this.jdbc = jdbc;
    }

    /** Eventos visibles en el panel: el organizador ve los suyos; admin y apoyo ven todos. */
    @Transactional(readOnly = true)
    public List<Evento> eventosDelAlcance(UsuarioAutenticado u, EstadoEvento estado, LocalDate desde, LocalDate hasta) {
        Specification<Evento> spec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("sala", JoinType.LEFT);
            }
            List<Predicate> p = new ArrayList<>();
            if (u.esOrganizador()) p.add(cb.equal(root.get("organizador").get("id"), u.id()));
            if (estado != null) p.add(cb.equal(root.get("estado"), estado));
            if (desde != null) p.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            if (hasta != null) p.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            return cb.and(p.toArray(Predicate[]::new));
        };
        return eventos.findAll(spec, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("horaInicio")));
    }

    @Transactional(readOnly = true)
    public List<PanelEvento> eventos(UsuarioAutenticado u, EstadoEvento estado, LocalDate desde, LocalDate hasta) {
        List<Evento> lista = eventosDelAlcance(u, estado, desde, hasta);
        List<Integer> ids = lista.stream().map(Evento::getId).toList();
        var cont = conteos.de(ids);
        Map<Integer, Prediccion> preds = ids.isEmpty() ? Map.of() : predicciones.findUltimasDe(ids).stream()
                .collect(Collectors.toMap(p -> p.getEvento().getId(), Function.identity(), (a, b) -> a));
        return lista.stream().map(e -> {
            var c = cont.get(e.getId());
            Prediccion p = preds.get(e.getId());
            return new PanelEvento(e.getId(), e.getTitulo(), e.getFecha(), e.getHoraInicio(), e.getTipo(),
                    e.getFacultad(), e.getSala() == null ? null : e.getSala().getNombre(),
                    e.getSala() == null ? null : e.getSala().getCapacidad(), e.getCapacidad(),
                    c.inscriptos(), c.asistentes(), e.getEstado(),
                    p == null ? null : p.getAsistenciaEstimada(),
                    p == null ? null : p.getProbSobrecapacidad().doubleValue());
        }).toList();
    }

    @Transactional(readOnly = true)
    public Resumen resumen(UsuarioAutenticado u) {
        List<PanelEvento> todos = eventos(u, null, null, null);
        long proximos = todos.stream().filter(e -> e.estado() == EstadoEvento.PUBLICADO
                && !e.fecha().isBefore(LocalDate.now())).count();
        List<PanelEvento> fin = todos.stream().filter(e -> e.estado() == EstadoEvento.FINALIZADO).toList();
        long ins = fin.stream().mapToLong(PanelEvento::inscriptos).sum();
        long asi = fin.stream().mapToLong(PanelEvento::asistentes).sum();
        long sobre = fin.stream().filter(e -> e.salaCapacidad() != null && e.asistentes() > e.salaCapacidad()).count();
        long baja = fin.stream().filter(e -> e.salaCapacidad() != null && e.asistentes() < 0.4 * e.salaCapacidad()).count();
        long borradores = todos.stream().filter(e -> e.estado() == EstadoEvento.BORRADOR).count();
        return new Resumen(todos.size(), proximos, fin.size(), borradores, ins, asi,
                ins == 0 ? 0 : (double) asi / ins, sobre, baja);
    }

    /** Dataset histórico (un registro por evento finalizado) que se envía al microservicio de analítica. */
    public List<RegistroHistorico> historico() {
        return jdbc.query("""
                SELECT evento_id, tipo, facultad, fecha, to_char(hora_inicio, 'HH24:MI') AS hora_inicio,
                       duracion_min::int AS duracion_min, clima, capacidad_sala, inscriptos, asistentes,
                       organizador_id, satisfaccion, respuestas
                FROM v_historico_eventos ORDER BY fecha
                """, (rs, n) -> {
            BigDecimal satisf = rs.getBigDecimal("satisfaccion");
            return new RegistroHistorico(
                    rs.getInt("evento_id"), rs.getString("tipo"), rs.getString("facultad"),
                    rs.getDate("fecha").toLocalDate(), rs.getString("hora_inicio"), rs.getInt("duracion_min"),
                    rs.getString("clima"), rs.getInt("capacidad_sala"), rs.getLong("inscriptos"),
                    rs.getLong("asistentes"), rs.getInt("organizador_id"),
                    satisf == null ? null : satisf.doubleValue(), rs.getLong("respuestas"));
        });
    }
}

package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.Encuesta;
import ar.edu.eventos.core.domain.EstadoEvento;
import ar.edu.eventos.core.domain.EstadoInscripcion;
import ar.edu.eventos.core.domain.Evento;
import ar.edu.eventos.core.domain.Inscripcion;
import ar.edu.eventos.core.dto.EncuestaDtos.*;
import ar.edu.eventos.core.exception.ApiException;
import ar.edu.eventos.core.repository.AsistenciaRepository;
import ar.edu.eventos.core.repository.EncuestaRepository;
import ar.edu.eventos.core.repository.InscripcionRepository;
import ar.edu.eventos.core.security.UsuarioAutenticado;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Encuestas de satisfacción.
 * <p>
 * Cuando un evento pasa a FINALIZADO, cada persona que asistió ve la encuesta pendiente en la app
 * (no hace falta "enviarla": la pendiente se calcula a partir de la asistencia). La encuesta queda
 * abierta {@value #DIAS_ABIERTA} días. Los puntajes se agregan en la vista {@code v_historico_eventos}
 * y viajan al microservicio de analítica, que los usa como variable del modelo predictivo.
 */
@Service
public class EncuestaService {

    public static final int DIAS_ABIERTA = 30;

    private final EncuestaRepository encuestas;
    private final InscripcionRepository inscripciones;
    private final AsistenciaRepository asistencias;
    private final EventoService eventoService;
    private final JdbcTemplate jdbc;

    public EncuestaService(EncuestaRepository encuestas, InscripcionRepository inscripciones,
                           AsistenciaRepository asistencias, EventoService eventoService, JdbcTemplate jdbc) {
        this.encuestas = encuestas;
        this.inscripciones = inscripciones;
        this.asistencias = asistencias;
        this.eventoService = eventoService;
        this.jdbc = jdbc;
    }

    /** Encuestas que el usuario autenticado todavía tiene que responder. */
    @Transactional(readOnly = true)
    public List<EncuestaPendiente> pendientes(UsuarioAutenticado u) {
        LocalDate desde = LocalDate.now().minusDays(DIAS_ABIERTA);
        return encuestas.findPendientes(u.id(), desde).stream().map(i -> {
            Evento e = i.getEvento();
            return new EncuestaPendiente(i.getId(), e.getId(), e.getTitulo(), e.getTipo(), e.getFacultad(),
                    e.getFecha(), e.getHoraInicio(), e.getSala() == null ? null : e.getSala().getNombre(),
                    e.getOrganizador().getNombreCompleto(), e.getFecha().plusDays(DIAS_ABIERTA));
        }).toList();
    }

    @Transactional
    public EncuestaRespondida responder(ResponderEncuestaRequest req, UsuarioAutenticado u) {
        Inscripcion i = inscripciones.findById(req.inscripcionId())
                .orElseThrow(() -> ApiException.noEncontrado("Inscripción"));
        if (!i.getUsuario().getId().equals(u.id())) {
            throw ApiException.prohibido("Sólo podés responder la encuesta de tus propias inscripciones");
        }
        Evento e = i.getEvento();
        if (i.getEstado() != EstadoInscripcion.ACTIVA || asistencias.findByInscripcionId(i.getId()).isEmpty()) {
            throw ApiException.conflicto("La encuesta es sólo para quienes asistieron al evento");
        }
        if (e.getEstado() != EstadoEvento.FINALIZADO) {
            throw ApiException.conflicto("La encuesta se habilita cuando el evento finaliza");
        }
        if (e.getFecha().isBefore(LocalDate.now().minusDays(DIAS_ABIERTA))) {
            throw ApiException.conflicto("La encuesta de este evento ya cerró");
        }
        if (encuestas.existsByInscripcionId(i.getId())) {
            throw ApiException.conflicto("Ya respondiste la encuesta de este evento");
        }
        Encuesta x = new Encuesta();
        x.setInscripcion(i);
        x.setPuntaje(req.puntaje());
        x.setRecomendaria(req.recomendaria());
        String comentario = req.comentario() == null ? null : req.comentario().trim();
        x.setComentario(comentario == null || comentario.isEmpty() ? null : comentario);
        x = encuestas.save(x);
        return new EncuestaRespondida(x.getId(), e.getId(), x.getPuntaje(), x.getRecomendaria(), x.getRespondidaEn());
    }

    /** Resultados de la encuesta de un evento, para el organizador, el admin y el personal de apoyo. */
    @Transactional(readOnly = true)
    public ResumenEncuestaEvento resumenEvento(Integer eventoId, UsuarioAutenticado u) {
        Evento e = eventoService.buscar(eventoId);
        if (!eventoService.puedeOperar(e, u)) {
            throw ApiException.prohibido("No podés ver las encuestas de este evento");
        }
        List<Encuesta> lista = encuestas.findDelEvento(eventoId);
        long asistentes = asistencias.findDelEvento(eventoId).size();
        long n = lista.size();
        Long[] dist = {0L, 0L, 0L, 0L, 0L};
        long suma = 0;
        long recomiendan = 0;
        for (Encuesta x : lista) {
            dist[x.getPuntaje() - 1]++;
            suma += x.getPuntaje();
            if (Boolean.TRUE.equals(x.getRecomendaria())) recomiendan++;
        }
        List<String> comentarios = lista.stream().map(Encuesta::getComentario)
                .filter(c -> c != null && !c.isBlank()).limit(30).toList();
        return new ResumenEncuestaEvento(e.getId(), e.getTitulo(), asistentes, n,
                asistentes == 0 ? 0 : (double) n / asistentes,
                n == 0 ? null : (double) suma / n,
                n == 0 ? null : (double) recomiendan / n,
                List.of(dist), comentarios);
    }

    /**
     * Satisfacción general agrupada por tipo de evento, facultad u organizador (este último sólo
     * para el administrador), más la evolución mensual.
     */
    @Transactional(readOnly = true)
    public SatisfaccionGeneral general(String agrupar, UsuarioAutenticado u) {
        String columna = switch (agrupar == null ? "tipo" : agrupar) {
            case "tipo" -> "e.tipo";
            case "facultad" -> "e.facultad";
            case "organizador" -> {
                if (!u.esAdmin()) throw ApiException.prohibido("Sólo el administrador ve la satisfacción por organizador");
                yield "o.nombre || ' ' || o.apellido";
            }
            default -> throw ApiException.invalido("agrupar debe ser tipo, facultad u organizador");
        };
        String desde = """
                FROM encuestas en
                JOIN inscripciones i ON i.id = en.inscripcion_id
                JOIN eventos e       ON e.id = i.evento_id
                JOIN usuarios o      ON o.id = e.organizador_id
                """;
        List<SatisfaccionGrupo> grupos = jdbc.query(
                "SELECT " + columna + " AS grupo, COUNT(DISTINCT e.id) AS eventos, COUNT(en.id) AS respuestas, "
                        + "AVG(en.puntaje) AS promedio, AVG(CASE WHEN en.recomendaria THEN 1.0 ELSE 0.0 END) AS recomienda "
                        + desde + "GROUP BY 1" + ("organizador".equals(agrupar) ? ", o.id" : "") + " ORDER BY promedio DESC",
                (rs, k) -> new SatisfaccionGrupo(rs.getString("grupo"), rs.getLong("eventos"),
                        rs.getLong("respuestas"), rs.getDouble("promedio"), rs.getDouble("recomienda")));
        List<SatisfaccionMes> porMes = jdbc.query(
                "SELECT to_char(e.fecha, 'YYYY-MM') AS mes, COUNT(en.id) AS respuestas, AVG(en.puntaje) AS promedio "
                        + desde + "GROUP BY 1 ORDER BY 1",
                (rs, k) -> new SatisfaccionMes(rs.getString("mes"), rs.getLong("respuestas"), rs.getDouble("promedio")));

        long respuestas = grupos.stream().mapToLong(SatisfaccionGrupo::respuestas).sum();
        Double promedio = null;
        Double recomienda = null;
        if (respuestas > 0) {
            promedio = grupos.stream().mapToDouble(g -> g.promedio() * g.respuestas()).sum() / respuestas;
            recomienda = grupos.stream().mapToDouble(g -> g.porcentajeRecomienda() * g.respuestas()).sum() / respuestas;
        }
        Long asistentes = jdbc.queryForObject("""
                SELECT COUNT(*) FROM asistencias a
                JOIN inscripciones i ON i.id = a.inscripcion_id
                JOIN eventos e       ON e.id = i.evento_id
                WHERE e.estado = 'FINALIZADO'
                """, Long.class);
        Double tasa = asistentes == null || asistentes == 0 ? null : (double) respuestas / asistentes;
        return new SatisfaccionGeneral(respuestas, promedio, recomienda, tasa, grupos, porMes);
    }
}

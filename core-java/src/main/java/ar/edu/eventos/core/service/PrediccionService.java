package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.*;
import ar.edu.eventos.core.dto.AnaliticaDtos.*;
import ar.edu.eventos.core.dto.PrediccionDto;
import ar.edu.eventos.core.exception.ApiException;
import ar.edu.eventos.core.repository.EventoRepository;
import ar.edu.eventos.core.repository.PrediccionRepository;
import ar.edu.eventos.core.repository.SalaRepository;
import ar.edu.eventos.core.security.UsuarioAutenticado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PrediccionService {

    private final PrediccionRepository predicciones;
    private final EventoRepository eventos;
    private final SalaRepository salas;
    private final EventoService eventoService;
    private final PanelService panelService;
    private final Conteos conteos;
    private final AnaliticaClient analitica;
    private final ObjectMapper mapper;

    public PrediccionService(PrediccionRepository predicciones, EventoRepository eventos, SalaRepository salas,
                             EventoService eventoService, PanelService panelService, Conteos conteos,
                             AnaliticaClient analitica, ObjectMapper mapper) {
        this.predicciones = predicciones;
        this.eventos = eventos;
        this.salas = salas;
        this.eventoService = eventoService;
        this.panelService = panelService;
        this.conteos = conteos;
        this.analitica = analitica;
        this.mapper = mapper;
    }

    /** Envía el histórico de eventos finalizados al microservicio Python para (re)entrenar los modelos. */
    public JsonNode entrenar() {
        List<RegistroHistorico> registros = panelService.historico();
        if (registros.size() < 10) {
            throw ApiException.conflicto("Se necesitan al menos 10 eventos finalizados para entrenar (hay "
                    + registros.size() + ")");
        }
        return analitica.entrenar(new EntrenarRequest(registros));
    }

    @Transactional
    public PrediccionDto predecir(Integer eventoId, UsuarioAutenticado u) {
        Evento e = eventoService.buscar(eventoId);
        if (!eventoService.puedeOperar(e, u)) {
            throw ApiException.prohibido("No podés ver la predicción de este evento");
        }
        long inscriptos = conteos.de(eventoId).inscriptos();
        JsonNode resp = analitica.predecir(new PrediccionRequest(aPredecir(e, inscriptos), salasInfo()));
        Prediccion p = guardar(e, resp);
        return aDto(p, inscriptos);
    }

    @Transactional(readOnly = true)
    public Optional<PrediccionDto> ultima(Integer eventoId) {
        return predicciones.findFirstByEventoIdOrderByGeneradaEnDesc(eventoId)
                .map(p -> aDto(p, conteos.de(eventoId).inscriptos()));
    }

    /**
     * Predicciones de los próximos eventos (publicados o en borrador) del alcance del usuario.
     * Reutiliza la última predicción si tiene menos de una hora, salvo que se pida regenerar.
     */
    @Transactional
    public List<PrediccionDto> proximos(UsuarioAutenticado u, boolean regenerar) {
        List<Evento> lista = panelService.eventosDelAlcance(u, null, LocalDate.now(), null).stream()
                .filter(e -> e.getEstado() == EstadoEvento.PUBLICADO || e.getEstado() == EstadoEvento.BORRADOR)
                .sorted(Comparator.comparing(Evento::getFecha).thenComparing(Evento::getHoraInicio))
                .toList();
        if (lista.isEmpty()) return List.of();
        List<Integer> ids = lista.stream().map(Evento::getId).toList();
        var cont = conteos.de(ids);
        Map<Integer, Prediccion> previas = predicciones.findUltimasDe(ids).stream()
                .collect(Collectors.toMap(p -> p.getEvento().getId(), Function.identity(), (a, b) -> a));
        LocalDateTime limite = LocalDateTime.now().minusHours(1);

        List<Evento> aCalcular = lista.stream().filter(e -> regenerar || !previas.containsKey(e.getId())
                || previas.get(e.getId()).getGeneradaEn().isBefore(limite)).toList();
        if (!aCalcular.isEmpty()) {
            var req = new PrediccionLoteRequest(
                    aCalcular.stream().map(e -> aPredecir(e, cont.get(e.getId()).inscriptos())).toList(),
                    salasInfo());
            JsonNode resp = analitica.predecirLote(req);
            Map<Integer, Evento> porId = aCalcular.stream().collect(Collectors.toMap(Evento::getId, x -> x));
            for (JsonNode n : resp.path("predicciones")) {
                Evento e = porId.get(n.path("evento_id").asInt());
                if (e != null) previas.put(e.getId(), guardar(e, n));
            }
        }
        return lista.stream().filter(e -> previas.containsKey(e.getId()))
                .map(e -> aDto(previas.get(e.getId()), cont.get(e.getId()).inscriptos())).toList();
    }

    // ------------------------------------------------------------------ helpers

    private EventoAPredecir aPredecir(Evento e, long inscriptos) {
        Sala s = e.getSala();
        return new EventoAPredecir(e.getId(), e.getTipo().name(), e.getFacultad(), e.getFecha(),
                e.getHoraInicio().toString(), e.getDuracionMinutos(), e.getClima().name(), inscriptos,
                e.getCapacidad(), s == null ? null : s.getCapacidad(), s == null ? null : s.getId());
    }

    private List<SalaInfo> salasInfo() {
        return salas.findByActivaTrueOrderByCapacidadAsc().stream()
                .map(s -> new SalaInfo(s.getId(), s.getNombre(), s.getCapacidad(), s.getFacultad())).toList();
    }

    private Prediccion guardar(Evento e, JsonNode resp) {
        PrediccionResponse r;
        try {
            r = mapper.treeToValue(resp, PrediccionResponse.class);
        } catch (JsonProcessingException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Respuesta inválida del servicio de analítica");
        }
        Prediccion p = new Prediccion();
        p.setEvento(e);
        p.setAsistenciaEstimada(r.asistenciaEstimada());
        p.setIntervaloInferior(r.intervaloInferior());
        p.setIntervaloSuperior(r.intervaloSuperior());
        p.setProbSobrecapacidad(BigDecimal.valueOf(r.probSobrecapacidad()).setScale(4, RoundingMode.HALF_UP));
        p.setProbBajaAsistencia(BigDecimal.valueOf(r.probBajaAsistencia()).setScale(4, RoundingMode.HALF_UP));
        p.setSalaRecomendada(r.salaRecomendadaId() == null ? null : salas.findById(r.salaRecomendadaId()).orElse(null));
        p.setModelo(r.modelo() == null ? "desconocido" : r.modelo());
        p.setDetalle(resp.toString());
        return predicciones.save(p);
    }

    private PrediccionDto aDto(Prediccion p, long inscriptos) {
        Evento e = p.getEvento();
        Sala rec = p.getSalaRecomendada();
        Sala act = e.getSala();
        JsonNode detalle = null;
        try {
            if (p.getDetalle() != null) detalle = mapper.readTree(p.getDetalle());
        } catch (JsonProcessingException ignored) {
            // detalle no crítico
        }
        return new PrediccionDto(p.getId(), e.getId(), e.getTitulo(), e.getFecha(), e.getTipo().name(),
                p.getAsistenciaEstimada(), p.getIntervaloInferior(), p.getIntervaloSuperior(),
                p.getProbSobrecapacidad().doubleValue(), p.getProbBajaAsistencia().doubleValue(),
                rec == null ? null : rec.getId(), rec == null ? null : rec.getNombre(),
                rec == null ? null : rec.getCapacidad(),
                act == null ? null : act.getId(), act == null ? null : act.getNombre(),
                act == null ? null : act.getCapacidad(),
                e.getCapacidad(), inscriptos, p.getModelo(), detalle, p.getGeneradaEn());
    }
}

package ar.edu.eventos.core.service;

import ar.edu.eventos.core.repository.AsistenciaRepository;
import ar.edu.eventos.core.repository.InscripcionRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Calcula inscriptos y asistentes de varios eventos con dos consultas agregadas (evita N+1). */
@Component
public class Conteos {

    private final InscripcionRepository inscripciones;
    private final AsistenciaRepository asistencias;

    public Conteos(InscripcionRepository inscripciones, AsistenciaRepository asistencias) {
        this.inscripciones = inscripciones;
        this.asistencias = asistencias;
    }

    public record Par(long inscriptos, long asistentes) {}

    public Map<Integer, Par> de(Collection<Integer> eventoIds) {
        Map<Integer, Par> r = new HashMap<>();
        if (eventoIds.isEmpty()) return r;
        Map<Integer, Long> ins = aMapa(inscripciones.contarActivasPorEvento(eventoIds));
        Map<Integer, Long> asi = aMapa(asistencias.contarPorEvento(eventoIds));
        for (Integer id : eventoIds) {
            r.put(id, new Par(ins.getOrDefault(id, 0L), asi.getOrDefault(id, 0L)));
        }
        return r;
    }

    public Par de(Integer eventoId) {
        return de(List.of(eventoId)).get(eventoId);
    }

    private static Map<Integer, Long> aMapa(List<Object[]> filas) {
        Map<Integer, Long> m = new HashMap<>();
        for (Object[] f : filas) m.put((Integer) f[0], ((Number) f[1]).longValue());
        return m;
    }
}

package ar.edu.eventos.core.repository;

import ar.edu.eventos.core.domain.Encuesta;
import ar.edu.eventos.core.domain.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EncuestaRepository extends JpaRepository<Encuesta, Integer> {

    boolean existsByInscripcionId(Integer inscripcionId);

    /**
     * Inscripciones del usuario a eventos FINALIZADOS desde {@code desde}, a los que asistió
     * y cuya encuesta todavía no respondió.
     */
    @Query("""
           select i from Inscripcion i
             join fetch i.evento e
             join fetch e.organizador
             left join fetch e.sala
           where i.usuario.id = :usuarioId
             and i.estado = ar.edu.eventos.core.domain.EstadoInscripcion.ACTIVA
             and e.estado = ar.edu.eventos.core.domain.EstadoEvento.FINALIZADO
             and e.fecha >= :desde
             and exists (select a.id from Asistencia a where a.inscripcion = i)
             and not exists (select x.id from Encuesta x where x.inscripcion = i)
           order by e.fecha desc, e.horaInicio desc
           """)
    List<Inscripcion> findPendientes(@Param("usuarioId") Integer usuarioId, @Param("desde") LocalDate desde);

    @Query("select x from Encuesta x where x.inscripcion.evento.id = :eventoId order by x.respondidaEn desc")
    List<Encuesta> findDelEvento(@Param("eventoId") Integer eventoId);
}

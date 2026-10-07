package ar.edu.eventos.core.repository;

import ar.edu.eventos.core.domain.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    Optional<Asistencia> findByInscripcionId(Integer inscripcionId);

    @Query("select a from Asistencia a join fetch a.inscripcion i join fetch i.usuario where i.evento.id = :eventoId")
    List<Asistencia> findDelEvento(@Param("eventoId") Integer eventoId);

    @Query("""
           select a.inscripcion.evento.id, count(a) from Asistencia a
           where a.inscripcion.evento.id in :ids group by a.inscripcion.evento.id
           """)
    List<Object[]> contarPorEvento(@Param("ids") Collection<Integer> ids);
}

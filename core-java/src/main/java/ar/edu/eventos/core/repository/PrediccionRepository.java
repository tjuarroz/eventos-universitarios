package ar.edu.eventos.core.repository;

import ar.edu.eventos.core.domain.Prediccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrediccionRepository extends JpaRepository<Prediccion, Integer> {

    Optional<Prediccion> findFirstByEventoIdOrderByGeneradaEnDesc(Integer eventoId);

    /** Última predicción de cada evento del conjunto. */
    @Query("""
           select p from Prediccion p left join fetch p.salaRecomendada
           where p.evento.id in :ids and p.generadaEn = (
               select max(p2.generadaEn) from Prediccion p2 where p2.evento.id = p.evento.id)
           """)
    List<Prediccion> findUltimasDe(@Param("ids") Collection<Integer> ids);
}

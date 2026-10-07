package ar.edu.eventos.core.repository;

import ar.edu.eventos.core.domain.EstadoEvento;
import ar.edu.eventos.core.domain.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Integer>, JpaSpecificationExecutor<Evento> {

    List<Evento> findByEstadoAndFechaBefore(EstadoEvento estado, LocalDate fecha);

    /** Eventos que ocupan la misma sala en una franja superpuesta (evita doble reserva). excluirId = -1 si es alta. */
    @Query("""
           select e from Evento e
           where e.sala.id = :salaId and e.fecha = :fecha
             and e.estado <> ar.edu.eventos.core.domain.EstadoEvento.CANCELADO
             and e.horaInicio < :horaFin and e.horaFin > :horaInicio
             and e.id <> :excluirId
           """)
    List<Evento> buscarSuperposiciones(@Param("salaId") Integer salaId,
                                       @Param("fecha") LocalDate fecha,
                                       @Param("horaInicio") LocalTime horaInicio,
                                       @Param("horaFin") LocalTime horaFin,
                                       @Param("excluirId") Integer excluirId);
}

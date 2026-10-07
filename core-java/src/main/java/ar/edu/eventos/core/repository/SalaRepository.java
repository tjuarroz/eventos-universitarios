package ar.edu.eventos.core.repository;

import ar.edu.eventos.core.domain.Sala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, Integer> {
    List<Sala> findAllByOrderByCapacidadAsc();

    List<Sala> findByActivaTrueOrderByCapacidadAsc();

    boolean existsByNombreIgnoreCase(String nombre);
}

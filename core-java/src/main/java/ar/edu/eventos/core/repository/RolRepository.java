package ar.edu.eventos.core.repository;

import ar.edu.eventos.core.domain.Rol;
import ar.edu.eventos.core.domain.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombre(RolNombre nombre);
}

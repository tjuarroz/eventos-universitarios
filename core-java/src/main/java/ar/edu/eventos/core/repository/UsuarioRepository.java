package ar.edu.eventos.core.repository;

import ar.edu.eventos.core.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
           select u from Usuario u
           where (:q = '' or lower(u.nombre) like lower(concat('%', :q, '%'))
                  or lower(u.apellido) like lower(concat('%', :q, '%'))
                  or lower(u.email) like lower(concat('%', :q, '%')))
           order by u.rol.id, u.apellido, u.nombre
           """)
    Page<Usuario> buscar(@Param("q") String q, Pageable pageable);
}

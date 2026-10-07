package ar.edu.eventos.core.repository;

import ar.edu.eventos.core.domain.EstadoInscripcion;
import ar.edu.eventos.core.domain.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

    Optional<Inscripcion> findByEventoIdAndUsuarioId(Integer eventoId, Integer usuarioId);

    Optional<Inscripcion> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigo(String codigo);

    long countByEventoIdAndEstado(Integer eventoId, EstadoInscripcion estado);

    @Query("""
           select i from Inscripcion i join fetch i.usuario u
           where i.evento.id = :eventoId and i.estado = ar.edu.eventos.core.domain.EstadoInscripcion.ACTIVA
           order by u.apellido, u.nombre
           """)
    List<Inscripcion> findActivasConUsuario(@Param("eventoId") Integer eventoId);

    @Query("""
           select i from Inscripcion i join fetch i.evento e left join fetch e.sala
           where i.usuario.id = :usuarioId
           order by e.fecha desc, e.horaInicio desc
           """)
    List<Inscripcion> findDelUsuario(@Param("usuarioId") Integer usuarioId);

    @Query("""
           select i.evento.id, count(i) from Inscripcion i
           where i.estado = ar.edu.eventos.core.domain.EstadoInscripcion.ACTIVA and i.evento.id in :ids
           group by i.evento.id
           """)
    List<Object[]> contarActivasPorEvento(@Param("ids") Collection<Integer> ids);

    @Query("""
           select i from Inscripcion i
           where i.usuario.id = :usuarioId and i.evento.id in :ids
             and i.estado = ar.edu.eventos.core.domain.EstadoInscripcion.ACTIVA
           """)
    List<Inscripcion> findActivasDelUsuarioEn(@Param("usuarioId") Integer usuarioId,
                                               @Param("ids") Collection<Integer> ids);
}

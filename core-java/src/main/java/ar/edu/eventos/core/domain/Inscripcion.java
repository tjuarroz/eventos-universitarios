package ar.edu.eventos.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscripciones",
       uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "usuario_id"}))
@Getter @Setter @NoArgsConstructor
public class Inscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Código corto único; es también el contenido del QR que presenta el asistente. */
    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInscripcion estado = EstadoInscripcion.ACTIVA;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime fechaInscripcion;

    @PrePersist
    void prePersist() {
        if (fechaInscripcion == null) fechaInscripcion = LocalDateTime.now();
    }
}

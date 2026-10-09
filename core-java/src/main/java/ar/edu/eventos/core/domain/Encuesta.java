package ar.edu.eventos.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Encuesta de satisfacción de un asistente sobre un evento finalizado.
 * Hay como máximo una por inscripción (cada persona responde una sola vez por evento).
 */
@Entity
@Table(name = "encuestas")
@Getter @Setter @NoArgsConstructor
public class Encuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id", unique = true)
    private Inscripcion inscripcion;

    /** 1 = muy malo … 5 = excelente. */
    @Column(nullable = false)
    private Integer puntaje;

    @Column(nullable = false)
    private Boolean recomendaria;

    @Column(length = 500)
    private String comentario;

    @Column(name = "respondida_en", nullable = false)
    private LocalDateTime respondidaEn;

    @PrePersist
    void prePersist() {
        if (respondidaEn == null) respondidaEn = LocalDateTime.now();
    }
}

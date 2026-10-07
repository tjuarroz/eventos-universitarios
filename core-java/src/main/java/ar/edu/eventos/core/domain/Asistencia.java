package ar.edu.eventos.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "asistencias")
@Getter @Setter @NoArgsConstructor
public class Asistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id", unique = true)
    private Inscripcion inscripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoAsistencia metodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrada_por")
    private Usuario registradaPor;

    @Column(name = "registrada_en", nullable = false)
    private LocalDateTime registradaEn;

    @PrePersist
    void prePersist() {
        if (registradaEn == null) registradaEn = LocalDateTime.now();
    }
}

package ar.edu.eventos.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "predicciones")
@Getter @Setter @NoArgsConstructor
public class Prediccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @Column(name = "asistencia_estimada", nullable = false)
    private Integer asistenciaEstimada;

    @Column(name = "intervalo_inferior")
    private Integer intervaloInferior;

    @Column(name = "intervalo_superior")
    private Integer intervaloSuperior;

    @Column(name = "prob_sobrecapacidad", nullable = false, precision = 5, scale = 4)
    private BigDecimal probSobrecapacidad;

    @Column(name = "prob_baja_asistencia", nullable = false, precision = 5, scale = 4)
    private BigDecimal probBajaAsistencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_recomendada_id")
    private Sala salaRecomendada;

    @Column(nullable = false)
    private String modelo;

    /** Respuesta completa del microservicio de analítica (JSON). */
    @Column(columnDefinition = "text")
    private String detalle;

    @Column(name = "generada_en", nullable = false)
    private LocalDateTime generadaEn;

    @PrePersist
    void prePersist() {
        if (generadaEn == null) generadaEn = LocalDateTime.now();
    }
}

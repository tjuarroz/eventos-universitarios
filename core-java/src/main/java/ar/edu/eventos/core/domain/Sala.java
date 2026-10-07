package ar.edu.eventos.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "salas")
@Getter @Setter @NoArgsConstructor
public class Sala {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private String edificio;

    private String facultad;

    @Column(nullable = false)
    private Integer capacidad;

    private String equipamiento;

    @Column(nullable = false)
    private boolean activa = true;
}

package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.Sala;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SalaDto(Integer id,
                      @NotBlank @Size(max = 80) String nombre,
                      @NotBlank @Size(max = 80) String edificio,
                      @Size(max = 80) String facultad,
                      @NotNull @Positive Integer capacidad,
                      @Size(max = 300) String equipamiento,
                      Boolean activa) {
    public static SalaDto de(Sala s) {
        return new SalaDto(s.getId(), s.getNombre(), s.getEdificio(), s.getFacultad(), s.getCapacidad(),
                s.getEquipamiento(), s.isActiva());
    }
}

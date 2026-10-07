package ar.edu.eventos.core.dto;

import ar.edu.eventos.core.domain.Categoria;
import ar.edu.eventos.core.domain.Usuario;

public record UsuarioDto(Integer id, String nombre, String apellido, String email, Categoria categoria,
                         String facultad, String rol, boolean activo) {
    public static UsuarioDto de(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getApellido(), u.getEmail(), u.getCategoria(),
                u.getFacultad(), u.getRol().getNombre().name(), u.isActivo());
    }
}

package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.Rol;
import ar.edu.eventos.core.domain.RolNombre;
import ar.edu.eventos.core.domain.Usuario;
import ar.edu.eventos.core.dto.UsuarioDto;
import ar.edu.eventos.core.exception.ApiException;
import ar.edu.eventos.core.repository.RolRepository;
import ar.edu.eventos.core.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioDto> buscar(String q, int pagina, int tamanio) {
        return usuarios.buscar(q == null ? "" : q.trim(), PageRequest.of(pagina, Math.min(tamanio, 100)))
                .map(UsuarioDto::de);
    }

    @Transactional(readOnly = true)
    public List<Rol> roles() {
        return roles.findAll();
    }

    @Transactional
    public UsuarioDto cambiarRol(Integer id, RolNombre rol, Integer adminId) {
        if (id.equals(adminId)) {
            throw ApiException.invalido("No podés cambiar tu propio rol");
        }
        Usuario u = usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("Usuario"));
        u.setRol(roles.findByNombre(rol).orElseThrow(() -> ApiException.noEncontrado("Rol")));
        return UsuarioDto.de(u);
    }

    @Transactional
    public UsuarioDto cambiarActivo(Integer id, boolean activo, Integer adminId) {
        if (id.equals(adminId)) {
            throw ApiException.invalido("No podés deshabilitar tu propia cuenta");
        }
        Usuario u = usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("Usuario"));
        u.setActivo(activo);
        return UsuarioDto.de(u);
    }
}

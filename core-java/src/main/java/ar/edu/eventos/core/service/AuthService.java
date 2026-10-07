package ar.edu.eventos.core.service;

import ar.edu.eventos.core.domain.RolNombre;
import ar.edu.eventos.core.domain.Usuario;
import ar.edu.eventos.core.dto.AuthDtos.AuthResponse;
import ar.edu.eventos.core.dto.AuthDtos.LoginRequest;
import ar.edu.eventos.core.dto.AuthDtos.RegistroRequest;
import ar.edu.eventos.core.dto.UsuarioDto;
import ar.edu.eventos.core.exception.ApiException;
import ar.edu.eventos.core.repository.RolRepository;
import ar.edu.eventos.core.repository.UsuarioRepository;
import ar.edu.eventos.core.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Usuario u = usuarios.findByEmailIgnoreCase(req.email().trim())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("credenciales"));
        if (!u.isActivo()) {
            throw ApiException.prohibido("El usuario está deshabilitado");
        }
        return new AuthResponse(jwt.generar(u), UsuarioDto.de(u));
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflicto("Ya existe una cuenta con ese email");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setApellido(req.apellido().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setCategoria(req.categoria());
        u.setFacultad(req.facultad());
        u.setRol(roles.findByNombre(RolNombre.USUARIO).orElseThrow());
        usuarios.save(u);
        return new AuthResponse(jwt.generar(u), UsuarioDto.de(u));
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfil(Integer id) {
        return usuarios.findById(id).map(UsuarioDto::de).orElseThrow(() -> ApiException.noEncontrado("Usuario"));
    }
}

package ar.edu.eventos.core.security;

import ar.edu.eventos.core.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Emite y valida tokens JWT (HMAC-SHA). El mismo secreto lo usa el microservicio Python
 * para validar los tokens que le envía el front-end y el propio core.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiracion-minutos}") long expiracionMinutos) {
        if (secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos 32 bytes");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generar(Usuario u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(u.getEmail())
                .claim("id", u.getId())
                .claim("rol", u.getRol().getNombre().name())
                .claim("nombre", u.getNombreCompleto())
                .issuer("core-eventos")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    /** Token de corta duración para llamadas servicio a servicio (Java → Python). */
    public String generarServicio() {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject("core-eventos")
                .claim("id", 0)
                .claim("rol", "SERVICE")
                .issuer("core-eventos")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(5, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            Integer id = c.get("id", Integer.class);
            String rol = c.get("rol", String.class);
            return Optional.of(new UsuarioAutenticado(id, c.getSubject(), rol));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

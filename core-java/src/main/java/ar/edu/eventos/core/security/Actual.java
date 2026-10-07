package ar.edu.eventos.core.security;

import ar.edu.eventos.core.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acceso rápido al usuario autenticado de la request en curso. */
public final class Actual {
    private Actual() {}

    public static UsuarioAutenticado usuario() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof UsuarioAutenticado u) {
            return u;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "No autenticado");
    }
}

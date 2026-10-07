package ar.edu.eventos.core.security;

/** Datos del usuario tomados del JWT; se usa como principal en el SecurityContext. */
public record UsuarioAutenticado(Integer id, String email, String rol) {
    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }

    public boolean esOrganizador() {
        return "ORGANIZADOR".equals(rol);
    }

    public boolean esStaff() {
        return esAdmin() || esOrganizador() || "APOYO".equals(rol);
    }
}

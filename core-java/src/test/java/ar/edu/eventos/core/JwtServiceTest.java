package ar.edu.eventos.core;

import ar.edu.eventos.core.domain.Rol;
import ar.edu.eventos.core.domain.RolNombre;
import ar.edu.eventos.core.domain.Usuario;
import ar.edu.eventos.core.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwt = new JwtService("secreto-de-prueba-con-mas-de-32-bytes-ok", 60);

    @Test
    void generaYValidaToken() {
        Rol rol = new Rol();
        rol.setNombre(RolNombre.ORGANIZADOR);
        Usuario u = new Usuario();
        u.setId(42);
        u.setNombre("Laura");
        u.setApellido("Méndez");
        u.setEmail("laura@uni.edu.ar");
        u.setRol(rol);

        var datos = jwt.validar(jwt.generar(u)).orElseThrow();
        assertEquals(42, datos.id());
        assertEquals("laura@uni.edu.ar", datos.email());
        assertEquals("ORGANIZADOR", datos.rol());
        assertTrue(datos.esOrganizador());
    }

    @Test
    void rechazaTokenAlterado() {
        assertTrue(jwt.validar("esto.no.es-un-jwt").isEmpty());
        assertTrue(jwt.validar(jwt.generarServicio() + "x").isEmpty());
    }

    @Test
    void exigeSecretoLargo() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", 60));
    }
}

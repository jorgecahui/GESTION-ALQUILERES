package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UsuarioServiceTest {
    private UsuarioRepository usuarios;
    private UsuarioService servicio;

    @BeforeEach void preparar() {
        usuarios = mock(UsuarioRepository.class);
        servicio = new UsuarioService(usuarios, new BCryptPasswordEncoder(),
                DatosFase6.permisosAdmin(), DatosFase6.auditoria());
    }

    @Test void creaUsuarioConUsernameNormalizadoYHash() {
        when(usuarios.existsByUsernameIgnoreCase("operador1")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario creado = servicio.crear("Operador1", "Luis Gómez", "Clave123", "Clave123",
                RolUsuario.OPERADOR, true);

        assertEquals("operador1", creado.getUsername());
        assertEquals(RolUsuario.OPERADOR, creado.getRol());
        assertEquals(false, creado.getPasswordHash().contains("Clave123"));
    }

    @Test void rechazaUsernameDuplicado() {
        when(usuarios.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        assertThrows(NegocioException.class, () -> servicio.crear("admin", "Ana", "Clave123",
                "Clave123", RolUsuario.ADMIN, true));
    }

    @Test void noDesactivaNiDegradaAlUltimoAdminActivo() {
        Usuario admin = Usuario.builder().id(1L).username("admin").nombre("Ana")
                .rol(RolUsuario.ADMIN).activo(true).build();
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarios.countByRolAndActivoTrue(RolUsuario.ADMIN)).thenReturn(1L);

        assertThrows(NegocioException.class,
                () -> servicio.actualizar(1L, "Ana", RolUsuario.OPERADOR, true));
        assertThrows(NegocioException.class,
                () -> servicio.actualizar(1L, "Ana", RolUsuario.ADMIN, false));
    }

    @Test void permiteCambiarRolSiExisteOtroAdminActivo() {
        Usuario admin = Usuario.builder().id(1L).username("admin").nombre("Ana")
                .rol(RolUsuario.ADMIN).activo(true).build();
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarios.countByRolAndActivoTrue(RolUsuario.ADMIN)).thenReturn(2L);
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario actualizado = servicio.actualizar(1L, "Ana", RolUsuario.OPERADOR, true);

        assertEquals(RolUsuario.OPERADOR, actualizado.getRol());
        verify(usuarios).save(admin);
    }

    @Test void restablecePasswordConPolitica() {
        Usuario usuario = Usuario.builder().id(2L).username("operador").nombre("Luis")
                .rol(RolUsuario.OPERADOR).activo(true).passwordHash("viejo").build();
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        servicio.restablecerPassword(2L, "Nueva123", "Nueva123");

        assertEquals(true, encoder.matches("Nueva123", usuario.getPasswordHash()));
        assertEquals(0, usuario.getIntentosFallidos());
    }
}

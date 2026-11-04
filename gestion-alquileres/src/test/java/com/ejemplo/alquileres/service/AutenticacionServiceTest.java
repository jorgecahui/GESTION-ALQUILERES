package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AutenticacionServiceTest {
    private static final Instant AHORA = Instant.parse("2026-04-01T10:00:00Z");
    private UsuarioRepository usuarios;
    private AuditoriaService auditoria;
    private SesionActual sesion;
    private BCryptPasswordEncoder encoder;

    @BeforeEach void preparar() {
        usuarios = mock(UsuarioRepository.class);
        auditoria = mock(AuditoriaService.class);
        sesion = new SesionActual();
        encoder = new BCryptPasswordEncoder();
    }

    @Test void elPrimerAdminSeGuardaConHashBcrypt() {
        when(usuarios.count()).thenReturn(0L);
        when(usuarios.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        AutenticacionService servicio = servicio(AHORA);

        Usuario creado = servicio.crearPrimerAdmin("Admin", "Ana Pérez", "Clave123", "Clave123");

        assertEquals("admin", creado.getUsername());
        assertEquals(RolUsuario.ADMIN, creado.getRol());
        assertNotEquals("Clave123", creado.getPasswordHash());
        assertTrue(creado.getPasswordHash().startsWith("$2"));
        assertTrue(encoder.matches("Clave123", creado.getPasswordHash()));
    }

    @Test void loginCorrectoIniciaSesionYActualizaUltimoAcceso() {
        Usuario usuario = usuario("admin", "Clave123");
        when(usuarios.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(usuario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        AutenticacionService servicio = servicio(AHORA);

        Usuario ingresado = servicio.login("ADMIN", "Clave123");

        assertEquals("admin", ingresado.getUsername());
        assertTrue(sesion.haySesion());
        assertEquals("admin", sesion.username());
        assertEquals(java.time.LocalDateTime.of(2026, 4, 1, 10, 0), ingresado.getUltimoAcceso());
        assertEquals(0, ingresado.getIntentosFallidos());
    }

    @Test void loginIncorrectoUsaMensajeGenericoYCuentaIntentos() {
        Usuario usuario = usuario("admin", "Clave123");
        when(usuarios.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(usuario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        AutenticacionService servicio = servicio(AHORA);

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.login("admin", "otra"));
        assertTrue(error.getMessage().contains("Usuario o contraseña incorrectos"));
        assertEquals(1, usuario.getIntentosFallidos());
        assertFalse(sesion.haySesion());
    }

    @Test void cincoFallosBloqueanCincoMinutosYSeDesbloqueaConElTiempo() {
        Usuario usuario = usuario("admin", "Clave123");
        when(usuarios.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(usuario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        AutenticacionService servicio = servicio(AHORA);

        for (int intento = 0; intento < AutenticacionService.INTENTOS_MAXIMOS; intento++) {
            assertThrows(NegocioException.class, () -> servicio.login("admin", "mala"));
        }
        assertEquals(AutenticacionService.INTENTOS_MAXIMOS, usuario.getIntentosFallidos());
        assertEquals(java.time.LocalDateTime.of(2026, 4, 1, 10, 5), usuario.getBloqueadoHasta());

        AutenticacionService posterior = servicio(AHORA.plusSeconds(6 * 60));
        assertDoesNotThrow(() -> posterior.login("admin", "Clave123"));
        assertTrue(sesion.haySesion());
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test void usuarioInexistenteTambienUsaMensajeGenerico() {
        when(usuarios.findByUsernameIgnoreCase("nadie")).thenReturn(Optional.empty());
        AutenticacionService servicio = servicio(AHORA);

        NegocioException error = assertThrows(NegocioException.class, () -> servicio.login("nadie", "Clave123"));
        assertEquals("Usuario o contraseña incorrectos.", error.getMessage());
    }

    @Test void politicaDePasswordExigeLongitudLetrasYNumeros() {
        assertDoesNotThrow(() -> AutenticacionService.validarPassword("Clave123"));
        assertThrows(NegocioException.class, () -> AutenticacionService.validarPassword("Abc123"));
        assertThrows(NegocioException.class, () -> AutenticacionService.validarPassword("abcdefgh"));
        assertThrows(NegocioException.class, () -> AutenticacionService.validarPassword("12345678"));
    }

    private AutenticacionService servicio(Instant instante) {
        return new AutenticacionService(usuarios, encoder, auditoria, sesion,
                Clock.fixed(instante, ZoneOffset.UTC));
    }

    private Usuario usuario(String username, String password) {
        return Usuario.builder().id(1L).username(username).nombre("Ana Pérez")
                .passwordHash(encoder.encode(password)).rol(RolUsuario.ADMIN).activo(true).build();
    }
}

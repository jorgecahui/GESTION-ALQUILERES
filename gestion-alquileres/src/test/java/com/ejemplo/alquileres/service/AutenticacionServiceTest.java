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

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

class AutenticacionServiceTest {
    private static final Instant AHORA = Instant.parse("2026-04-01T10:00:00Z");
    private UsuarioRepository usuarios;
    private JlccAuditoriaService auditoria;
    private JlccSesionActual sesion;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        auditoria = mock(JlccAuditoriaService.class);
        sesion = new JlccSesionActual();
        encoder = new BCryptPasswordEncoder();
    }

    @Test
    void elPrimerAdminSeGuardaConHashBcrypt() {
        when(usuarios.count()).thenReturn(0L);
        when(usuarios.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        JlccAutenticacionService servicio = servicio(AHORA);

        Usuario creado = servicio.jlccCrearPrimerAdmin("Admin", "Ana Pérez", "Clave123", "Clave123");

        assertEquals("admin", creado.getUsername());
        assertEquals(RolUsuario.ADMIN, creado.getRol());
        assertNotEquals("Clave123", creado.getPasswordHash());
        assertTrue(creado.getPasswordHash().startsWith("$2"));
        assertTrue(encoder.matches("Clave123", creado.getPasswordHash()));
    }

    @Test
    void loginCorrectoIniciaSesionYActualizaUltimoAcceso() {
        Usuario usuario = usuario("admin", "Clave123");
        when(usuarios.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(usuario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        JlccAutenticacionService servicio = servicio(AHORA);

        Usuario ingresado = servicio.jlccLogin("ADMIN", "Clave123");

        assertEquals("admin", ingresado.getUsername());
        assertTrue(sesion.jlccHaySesion());
        assertEquals("admin", sesion.jlccUsername());
        assertEquals(java.time.LocalDateTime.of(2026, 4, 1, 10, 0), ingresado.getUltimoAcceso());
        assertEquals(0, ingresado.getIntentosFallidos());
    }

    @Test
    void loginIncorrectoUsaMensajeGenericoYCuentaIntentos() {
        Usuario usuario = usuario("admin", "Clave123");
        when(usuarios.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(usuario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        JlccAutenticacionService servicio = servicio(AHORA);

        JlccNegocioException error =
                assertThrows(JlccNegocioException.class, () -> servicio.jlccLogin("admin", "otra"));
        assertTrue(error.getMessage().contains("Usuario o contraseña incorrectos"));
        assertEquals(1, usuario.getIntentosFallidos());
        assertFalse(sesion.jlccHaySesion());
    }

    @Test
    void cincoFallosBloqueanCincoMinutosYSeDesbloqueaConElTiempo() {
        Usuario usuario = usuario("admin", "Clave123");
        when(usuarios.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(usuario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        JlccAutenticacionService servicio = servicio(AHORA);

        for (int intento = 0; intento < JlccAutenticacionService.JLCC_INTENTOS_MAXIMOS; intento++) {
            assertThrows(JlccNegocioException.class, () -> servicio.jlccLogin("admin", "mala"));
        }
        assertEquals(JlccAutenticacionService.JLCC_INTENTOS_MAXIMOS, usuario.getIntentosFallidos());
        assertEquals(java.time.LocalDateTime.of(2026, 4, 1, 10, 5), usuario.getBloqueadoHasta());

        JlccAutenticacionService posterior = servicio(AHORA.plusSeconds(6 * 60));
        assertDoesNotThrow(() -> posterior.jlccLogin("admin", "Clave123"));
        assertTrue(sesion.jlccHaySesion());
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    void usuarioInexistenteTambienUsaMensajeGenerico() {
        when(usuarios.findByUsernameIgnoreCase("nadie")).thenReturn(Optional.empty());
        JlccAutenticacionService servicio = servicio(AHORA);

        JlccNegocioException error =
                assertThrows(JlccNegocioException.class, () -> servicio.jlccLogin("nadie", "Clave123"));
        assertEquals("Usuario o contraseña incorrectos.", error.getMessage());
    }

    @Test
    void politicaDePasswordExigeLongitudLetrasYNumeros() {
        assertDoesNotThrow(() -> JlccAutenticacionService.jlccValidarPassword("Clave123"));
        assertThrows(JlccNegocioException.class, () -> JlccAutenticacionService.jlccValidarPassword("Abc123"));
        assertThrows(
                JlccNegocioException.class, () -> JlccAutenticacionService.jlccValidarPassword("abcdefgh"));
        assertThrows(
                JlccNegocioException.class, () -> JlccAutenticacionService.jlccValidarPassword("12345678"));
    }

    private JlccAutenticacionService servicio(Instant instante) {
        return new JlccAutenticacionService(
                usuarios, encoder, auditoria, sesion, Clock.fixed(instante, ZoneOffset.UTC));
    }

    private Usuario usuario(String username, String password) {
        return Usuario.builder()
                .id(1L)
                .username(username)
                .nombre("Ana Pérez")
                .passwordHash(encoder.encode(password))
                .rol(RolUsuario.ADMIN)
                .activo(true)
                .build();
    }
}

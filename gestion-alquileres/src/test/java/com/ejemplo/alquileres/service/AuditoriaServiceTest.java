package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.AuditoriaEvento;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.repository.AuditoriaRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

class AuditoriaServiceTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-04-02T08:30:00Z"), ZoneOffset.UTC);

    @Test
    void registraEventoSinDatosSensibles() {
        AuditoriaRepository repo = mock(AuditoriaRepository.class);
        when(repo.save(any(AuditoriaEvento.class))).thenAnswer(i -> i.getArgument(0));
        JlccSesionActual sesion = new JlccSesionActual();
        sesion.jlccIniciar(
                Usuario.builder()
                        .id(1L)
                        .username("admin")
                        .rol(com.ejemplo.alquileres.model.RolUsuario.ADMIN)
                        .build());
        JlccAuditoriaService servicio = new JlccAuditoriaService(repo, sesion, new JlccPermisos(sesion), CLOCK);

        servicio.jlccRegistrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "Configuracion",
                1L,
                "Se guardó la configuración. contraseña: secreta123");

        ArgumentCaptor<AuditoriaEvento> captor = ArgumentCaptor.forClass(AuditoriaEvento.class);
        org.mockito.Mockito.verify(repo).save(captor.capture());
        AuditoriaEvento evento = captor.getValue();
        assertEquals("admin", evento.getUsuario());
        assertEquals(AccionAuditoria.CAMBIO_CONFIGURACION, evento.getAccion());
        assertEquals(java.time.LocalDateTime.of(2026, 4, 2, 8, 30), evento.getFecha());
        assertFalse(evento.getDetalle().contains("secreta123"));
        assertTrue(evento.getDetalle().contains("***"));
    }

    @Test
    void registraFallosDeLoginConElUsuarioIntentado() {
        AuditoriaRepository repo = mock(AuditoriaRepository.class);
        when(repo.save(any(AuditoriaEvento.class))).thenAnswer(i -> i.getArgument(0));
        JlccSesionActual sesion = new JlccSesionActual();
        JlccAuditoriaService servicio = new JlccAuditoriaService(repo, sesion, new JlccPermisos(sesion), CLOCK);

        AuditoriaEvento evento =
                servicio.jlccRegistrarComo(
                        "intruso",
                        AccionAuditoria.LOGIN_FALLIDO,
                        "Usuario",
                        null,
                        "Credenciales inválidas");

        assertEquals("intruso", evento.getUsuario());
        assertEquals(AccionAuditoria.LOGIN_FALLIDO, evento.getAccion());
    }

    @Test
    void soloAdminPuedeConsultarLaBitacora() {
        AuditoriaRepository repo = mock(AuditoriaRepository.class);
        JlccSesionActual sesionConsulta = new JlccSesionActual();
        sesionConsulta.jlccIniciar(
                Usuario.builder()
                        .id(2L)
                        .username("consulta")
                        .rol(com.ejemplo.alquileres.model.RolUsuario.CONSULTA)
                        .build());
        JlccAuditoriaService servicio =
                new JlccAuditoriaService(repo, sesionConsulta, new JlccPermisos(sesionConsulta), CLOCK);

        assertThrows(JlccNegocioException.class, () -> servicio.jlccListar(null, null, null, null));
    }

    @Test
    void adminPuedeConsultarYLaAccionSeFiltraPorEnum() {
        AuditoriaRepository repo = mock(AuditoriaRepository.class);
        when(repo.buscar(null, null, "ana", AccionAuditoria.ANULACION_PAGO)).thenReturn(List.of());
        JlccSesionActual sesion = new JlccSesionActual();
        sesion.jlccIniciar(
                Usuario.builder()
                        .id(1L)
                        .username("admin")
                        .rol(com.ejemplo.alquileres.model.RolUsuario.ADMIN)
                        .build());
        JlccAuditoriaService servicio = new JlccAuditoriaService(repo, sesion, new JlccPermisos(sesion), CLOCK);

        assertEquals(List.of(), servicio.jlccListar(null, null, "ana", AccionAuditoria.ANULACION_PAGO));
    }
}

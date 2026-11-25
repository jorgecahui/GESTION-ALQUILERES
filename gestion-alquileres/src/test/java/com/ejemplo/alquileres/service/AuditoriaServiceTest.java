package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.exception.NegocioException;
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
        SesionActual sesion = new SesionActual();
        sesion.iniciar(
                Usuario.builder()
                        .id(1L)
                        .username("admin")
                        .rol(com.ejemplo.alquileres.model.RolUsuario.ADMIN)
                        .build());
        AuditoriaService servicio = new AuditoriaService(repo, sesion, new Permisos(sesion), CLOCK);

        servicio.registrar(
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
        SesionActual sesion = new SesionActual();
        AuditoriaService servicio = new AuditoriaService(repo, sesion, new Permisos(sesion), CLOCK);

        AuditoriaEvento evento =
                servicio.registrarComo(
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
        SesionActual sesionConsulta = new SesionActual();
        sesionConsulta.iniciar(
                Usuario.builder()
                        .id(2L)
                        .username("consulta")
                        .rol(com.ejemplo.alquileres.model.RolUsuario.CONSULTA)
                        .build());
        AuditoriaService servicio =
                new AuditoriaService(repo, sesionConsulta, new Permisos(sesionConsulta), CLOCK);

        assertThrows(NegocioException.class, () -> servicio.listar(null, null, null, null));
    }

    @Test
    void adminPuedeConsultarYLaAccionSeFiltraPorEnum() {
        AuditoriaRepository repo = mock(AuditoriaRepository.class);
        when(repo.buscar(null, null, "ana", AccionAuditoria.ANULACION_PAGO)).thenReturn(List.of());
        SesionActual sesion = new SesionActual();
        sesion.iniciar(
                Usuario.builder()
                        .id(1L)
                        .username("admin")
                        .rol(com.ejemplo.alquileres.model.RolUsuario.ADMIN)
                        .build());
        AuditoriaService servicio = new AuditoriaService(repo, sesion, new Permisos(sesion), CLOCK);

        assertEquals(List.of(), servicio.listar(null, null, "ana", AccionAuditoria.ANULACION_PAGO));
    }
}

package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.CanalRecordatorio;
import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.Recordatorio;
import com.ejemplo.alquileres.model.TipoRecordatorio;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.CuotaRepository;
import com.ejemplo.alquileres.repository.RecordatorioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RecordatorioServiceTest {
    private final RecordatorioRepository recordatorios = Mockito.mock(RecordatorioRepository.class);
    private final CuotaRepository cuotas = Mockito.mock(CuotaRepository.class);
    private final ContratoRepository contratos = Mockito.mock(ContratoRepository.class);
    private RecordatorioService servicio;
    private Contrato contrato;
    private Cuota vencida;
    private Cuota porVencer;
    private Cuota lejana;

    @BeforeEach void preparar() {
        servicio = new RecordatorioService(recordatorios, cuotas, contratos, DatosFase6.arrendador(),
                DatosFase6.servicioConfiguracion(), new CalculadoraMora(), DatosFase6.permisosAdmin(), DatosFase6.CLOCK);
        contrato = DatosFase6.contrato();
        contrato.getCuotas().clear();
        vencida = DatosFase6.cuota(20L, 3, LocalDate.of(2026, 2, 1), "1200.00", "0.00");
        porVencer = DatosFase6.cuota(21L, 4, LocalDate.of(2026, 2, 12), "1200.00", "0.00");
        lejana = DatosFase6.cuota(22L, 5, LocalDate.of(2026, 3, 5), "1200.00", "0.00");
        contrato.agregarCuota(vencida);
        contrato.agregarCuota(porVencer);
        contrato.agregarCuota(lejana);
    }

    @Test void sugerenciasExcluyenCuotasConRecordatorioRecienteYLasLejanas() {
        when(cuotas.pendientesConContrato()).thenReturn(List.of(vencida, porVencer, lejana));
        when(recordatorios.recientes(eq(20L), any())).thenReturn(List.of(Recordatorio.builder().id(1L).build()));
        when(recordatorios.recientes(eq(21L), any())).thenReturn(List.of());
        when(recordatorios.recientes(eq(22L), any())).thenReturn(List.of());

        List<RecordatorioService.Propuesta> sugerencias = servicio.sugerencias(DatosFase6.HOY);

        assertEquals(1, sugerencias.size());
        assertEquals(21L, sugerencias.get(0).cuotaId());
        assertEquals(TipoRecordatorio.PREVENTIVO, sugerencias.get(0).tipo());
    }

    @Test void generaMensajeVencidoConMoraYTelefonoNormalizado() {
        RecordatorioService.Propuesta propuesta = servicio.propuesta(vencida, DatosFase6.HOY);

        assertEquals("51987654321", propuesta.telefono());
        assertEquals(TipoRecordatorio.VENCIDO, propuesta.tipo());
        assertTrue(propuesta.mensaje().contains("Ana Pérez"));
        assertTrue(propuesta.mensaje().contains("S/ 1,210.80"));
        assertTrue(propuesta.mensaje().contains("9 días de atraso"));
        assertFalse(propuesta.mensaje().contains("{"));
    }

    @Test void generaPropuestaDeFinDeContrato() {
        RecordatorioService.Propuesta propuesta = servicio.propuestaFinContrato(contrato, DatosFase6.HOY);

        assertEquals(TipoRecordatorio.FIN_CONTRATO, propuesta.tipo());
        assertTrue(propuesta.mensaje().contains("CTR-000001"));
        assertTrue(propuesta.mensaje().contains("31 de diciembre de 2026"));
        assertFalse(propuesta.mensaje().contains("{"));
    }

    @Test void codificaLaUrlDeWhatsAppConTildesSaltosYMoneda() {
        String url = servicio.urlWhatsApp("987654321", "Hola señor Pérez,\nMonto: S/ 1,250.00");

        assertTrue(url.startsWith("https://wa.me/51987654321?text="));
        assertTrue(url.contains("%20"));
        assertTrue(url.contains("P%C3%A9rez"));
        assertTrue(url.contains("%0A"));
        assertTrue(url.contains("S%2F"));
        assertTrue(url.contains("%2C"));
    }

    @Test void registraElRecordatorioConTelefonoNormalizadoYFechaDelReloj() {
        when(cuotas.findById(20L)).thenReturn(Optional.of(vencida));
        when(recordatorios.save(any(Recordatorio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        RecordatorioService.Propuesta propuesta = new RecordatorioService.Propuesta(20L, 1L, "987654321",
                TipoRecordatorio.VENCIDO, "Mensaje original");

        Recordatorio guardado = servicio.registrar(propuesta, "  Mensaje editado  ", CanalRecordatorio.WHATSAPP);

        assertEquals("51987654321", guardado.getTelefono());
        assertEquals("Mensaje editado", guardado.getMensaje());
        assertEquals(CanalRecordatorio.WHATSAPP, guardado.getCanal());
        assertEquals(contrato, guardado.getContrato());
        assertEquals(LocalDateTime.of(2026, 2, 10, 12, 0), guardado.getFechaEnvio());
    }

    @Test void rechazaTelefonoInvalidoAlRegistrar() {
        RecordatorioService.Propuesta propuesta = new RecordatorioService.Propuesta(20L, 1L, "123",
                TipoRecordatorio.VENCIDO, "Mensaje");

        assertThrows(NegocioException.class,
                () -> servicio.registrar(propuesta, "Mensaje", CanalRecordatorio.COPIADO));
    }

    @Test void describeElUltimoRecordatorioEnLenguajeNatural() {
        assertEquals("—", servicio.ultimoTexto(null));
        assertEquals("hoy", servicio.ultimoTexto(LocalDateTime.of(2026, 2, 10, 8, 0)));
        assertEquals("ayer", servicio.ultimoTexto(LocalDateTime.of(2026, 2, 9, 8, 0)));
        assertEquals("hace 2 días", servicio.ultimoTexto(LocalDateTime.of(2026, 2, 8, 8, 0)));
    }

    @Test void convierteElUltimoEnvioPorCuotaEnMapa() {
        when(recordatorios.ultimosPorCuota()).thenReturn(List.<Object[]>of(
                new Object[]{20L, LocalDateTime.of(2026, 2, 8, 9, 0)}));

        assertEquals(LocalDateTime.of(2026, 2, 8, 9, 0), servicio.ultimosPorCuota().get(20L));
    }
}

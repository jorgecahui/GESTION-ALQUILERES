package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.model.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class ReciboPdfServiceTest {
    @Test
    void generaPdfParaReciboEmitidoYAnulado() {
        JlccPagoService pagos = Mockito.mock(JlccPagoService.class);
        JlccReciboPdfService recibos = new JlccReciboPdfService(pagos, datosArrendador());
        Pago emitido = pago(EstadoPago.EMITIDO);
        Pago anulado = pago(EstadoPago.ANULADO);
        when(pagos.jlccObtener(1L)).thenReturn(emitido);
        when(pagos.jlccObtener(2L)).thenReturn(anulado);

        assertPdf(recibos.jlccGenerar(1L));
        assertPdf(recibos.jlccGenerar(2L));
    }

    private void assertPdf(byte[] contenido) {
        assertTrue(contenido.length > 100);
        assertArrayEquals("%PDF".getBytes(), java.util.Arrays.copyOf(contenido, 4));
    }

    private DatosArrendador datosArrendador() {
        return DatosFase6.arrendador();
    }

    private Pago pago(EstadoPago estado) {
        Cuota cuota =
                Cuota.builder()
                        .montoCuota(new BigDecimal("1000.00"))
                        .montoPagado(new BigDecimal("1000.00"))
                        .build();
        Contrato contrato = Contrato.builder().cuotas(List.of(cuota)).build();
        cuota.setContrato(contrato);
        Pago pago =
                Pago.builder()
                        .recibo("R001-00000001")
                        .fechaPago(LocalDate.of(2026, 1, 12))
                        .medioPago(MedioPago.EFECTIVO)
                        .estado(estado)
                        .arrendatarioNombre("Ana Pérez")
                        .arrendatarioDocumento("12345678")
                        .inmuebleCodigo("INM-0001")
                        .inmuebleNombre("Departamento 101")
                        .montoCapital(new BigDecimal("1000.00"))
                        .montoMora(new BigDecimal("7.00"))
                        .montoTotal(new BigDecimal("1007.00"))
                        .contrato(contrato)
                        .build();
        pago.agregarDetalle(
                PagoDetalle.builder()
                        .cuota(cuota)
                        .item(1)
                        .concepto("Alquiler")
                        .periodo(LocalDate.of(2026, 1, 1))
                        .fechaVencimiento(LocalDate.of(2026, 1, 5))
                        .montoCapital(new BigDecimal("1000.00"))
                        .montoMora(new BigDecimal("7.00"))
                        .build());
        return pago;
    }
}

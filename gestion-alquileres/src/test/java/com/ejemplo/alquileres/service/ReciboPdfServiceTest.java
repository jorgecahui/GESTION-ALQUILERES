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
        PagoService pagos = Mockito.mock(PagoService.class);
        ReciboPdfService recibos = new ReciboPdfService(pagos, datosArrendador());
        Pago emitido = pago(EstadoPago.EMITIDO);
        Pago anulado = pago(EstadoPago.ANULADO);
        when(pagos.obtener(1L)).thenReturn(emitido);
        when(pagos.obtener(2L)).thenReturn(anulado);

        assertPdf(recibos.generar(1L));
        assertPdf(recibos.generar(2L));
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

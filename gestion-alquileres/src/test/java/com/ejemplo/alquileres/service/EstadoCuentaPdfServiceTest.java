package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.EstadoPago;
import com.ejemplo.alquileres.model.Pago;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.PagoRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

class EstadoCuentaPdfServiceTest {
    @Test
    void generaEstadoDeCuentaConPagos() {
        ContratoRepository contratos = Mockito.mock(ContratoRepository.class);
        PagoRepository pagos = Mockito.mock(PagoRepository.class);
        Contrato contrato = DatosFase6.contrato();
        Pago pago =
                DatosFase6.pago(
                        contrato,
                        contrato.getCuotas().get(0),
                        "R001-00000001",
                        "1200.00",
                        EstadoPago.EMITIDO);
        when(contratos.detalle(1L)).thenReturn(Optional.of(contrato));
        when(pagos.porContrato(1L)).thenReturn(List.of(pago));
        EstadoCuentaPdfService servicio =
                new EstadoCuentaPdfService(
                        contratos,
                        pagos,
                        new CalculadoraMora(),
                        DatosFase6.arrendador(),
                        DatosFase6.CLOCK);

        assertPdf(servicio.generar(1L));
    }

    @Test
    void generaEstadoDeCuentaSinPagos() {
        ContratoRepository contratos = Mockito.mock(ContratoRepository.class);
        PagoRepository pagos = Mockito.mock(PagoRepository.class);
        when(contratos.detalle(2L)).thenReturn(Optional.of(DatosFase6.contrato()));
        when(pagos.porContrato(2L)).thenReturn(List.of());
        EstadoCuentaPdfService servicio =
                new EstadoCuentaPdfService(
                        contratos,
                        pagos,
                        new CalculadoraMora(),
                        DatosFase6.arrendador(),
                        DatosFase6.CLOCK);

        assertPdf(servicio.generar(2L));
    }

    private void assertPdf(byte[] contenido) {
        assertTrue(contenido.length > 100);
        assertArrayEquals("%PDF".getBytes(), java.util.Arrays.copyOf(contenido, 4));
    }
}

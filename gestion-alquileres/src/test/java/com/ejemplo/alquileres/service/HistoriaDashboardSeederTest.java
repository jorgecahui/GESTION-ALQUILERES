package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Pago;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.PagoRepository;
import com.ejemplo.alquileres.repository.PersonaRepository;
import com.ejemplo.alquileres.seeder.HistoriaDashboardSeeder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HistoriaDashboardSeederTest {
    private final ContratoRepository contratos = Mockito.mock(ContratoRepository.class);
    private final PagoRepository pagos = Mockito.mock(PagoRepository.class);
    private final InmuebleRepository inmuebles = Mockito.mock(InmuebleRepository.class);
    private final PersonaRepository personas = Mockito.mock(PersonaRepository.class);

    @Test void reparaElDetalleFaltanteDeUnPagoDemo() {
        Contrato contrato = DatosFase6.contrato();
        Pago pago = pagoDemo(contrato);
        when(contratos.count()).thenReturn(1L);
        when(pagos.findAll()).thenReturn(List.of(pago));
        when(pagos.save(any(Pago.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        seeder().cargar();

        assertEquals(1, pago.getDetalles().size());
        assertEquals(contrato.getCuotas().get(0), pago.getDetalles().get(0).getCuota());
        assertEquals(new BigDecimal("1200.00"), pago.getDetalles().get(0).getMontoCapital());
    }

    @Test void completaElCronogramaDemoYUsaConceptosEnEspanol() {
        Contrato contrato = DatosFase6.contrato();
        contrato.setCodigo("CTR-DEV-TEST");
        when(contratos.count()).thenReturn(1L);
        when(contratos.findAll()).thenReturn(List.of(contrato));
        when(pagos.findAll()).thenReturn(List.of());
        when(contratos.save(any(Contrato.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        seeder().cargar();

        assertEquals(12, contrato.getCuotas().size());
        assertEquals("Renta enero 2026", contrato.getCuotas().get(0).getConcepto());
        assertEquals("Renta diciembre 2026", contrato.getCuotas().get(11).getConcepto());
        assertTrue(contrato.getCuotas().stream().allMatch(c -> c.getConcepto().startsWith("Renta ")));
    }

    @Test void noTocaPagosRealesSinDetalle() {
        Pago pago = Pago.builder().id(9L).recibo("R001-00000001")
                .contrato(DatosFase6.contrato()).fechaPago(LocalDate.of(2026, 1, 5))
                .montoCapital(new BigDecimal("1200.00")).build();
        when(contratos.count()).thenReturn(1L);
        when(pagos.findAll()).thenReturn(List.of(pago));

        seeder().cargar();

        assertTrue(pago.getDetalles().isEmpty());
        verify(pagos, never()).save(any(Pago.class));
    }

    private HistoriaDashboardSeeder seeder() {
        return new HistoriaDashboardSeeder(contratos, pagos, inmuebles, personas, DatosFase6.CLOCK);
    }

    private Pago pagoDemo(Contrato contrato) {
        return Pago.builder().id(1L).recibo("R001-DEV-1-1").contrato(contrato)
                .fechaPago(LocalDate.of(2026, 1, 5))
                .montoCapital(new BigDecimal("1200.00"))
                .montoMora(BigDecimal.ZERO).build();
    }
}

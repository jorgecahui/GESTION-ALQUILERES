package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.repository.ContratoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ContratoPdfServiceTest {
    @Test void generaHojaDeContratoCompleta() {
        ContratoRepository contratos = Mockito.mock(ContratoRepository.class);
        Contrato contrato = DatosFase6.contrato();
        contrato.setObservaciones("Se entrega con un juego de llaves.");
        when(contratos.detalle(1L)).thenReturn(Optional.of(contrato));
        ContratoPdfService servicio = new ContratoPdfService(contratos, DatosFase6.arrendador(), DatosFase6.CLOCK);

        byte[] contenido = servicio.generar(1L);

        assertTrue(contenido.length > 100);
        assertArrayEquals("%PDF".getBytes(), java.util.Arrays.copyOf(contenido, 4));
    }

    @Test void generaHojaSinGaranteNiTipoDeInmueble() {
        ContratoRepository contratos = Mockito.mock(ContratoRepository.class);
        Contrato contrato = DatosFase6.contrato();
        contrato.getInmueble().setTipo(null);
        contrato.setGarante(null);
        when(contratos.detalle(2L)).thenReturn(Optional.of(contrato));
        ContratoPdfService servicio = new ContratoPdfService(contratos, DatosFase6.arrendador(), DatosFase6.CLOCK);

        byte[] contenido = servicio.generar(2L);

        assertTrue(contenido.length > 100);
        assertArrayEquals("%PDF".getBytes(), java.util.Arrays.copyOf(contenido, 4));
    }
}

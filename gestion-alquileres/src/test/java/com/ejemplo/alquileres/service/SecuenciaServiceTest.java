package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ejemplo.alquileres.model.Secuencia;
import com.ejemplo.alquileres.repository.SecuenciaRepository;

import org.junit.jupiter.api.*;

import java.util.*;

class SecuenciaServiceTest {
    @Test
    void incrementaYFormatea() {
        SecuenciaRepository repo = mock(SecuenciaRepository.class);
        JlccSecuenciaService service = new JlccSecuenciaService(repo);
        when(repo.incrementar("INMUEBLE")).thenReturn(1);
        when(repo.findByClave("INMUEBLE"))
                .thenReturn(Optional.of(new Secuencia(1L, "INMUEBLE", 2L)));
        assertEquals(2L, service.jlccSiguiente("inmueble"));
        assertEquals("INM-0002", service.jlccFormatear("INM-", 2, 4));
        verify(repo).incrementar("INMUEBLE");
    }

    @Test
    void creaClaveNueva() {
        SecuenciaRepository repo = mock(SecuenciaRepository.class);
        JlccSecuenciaService service = new JlccSecuenciaService(repo);
        when(repo.incrementar("RECIBO-R001")).thenReturn(0, 1);
        when(repo.findByClave("RECIBO-R001"))
                .thenReturn(Optional.of(new Secuencia(1L, "RECIBO-R001", 1L)));
        assertEquals(1L, service.jlccSiguiente("recibo-r001"));
        verify(repo).saveAndFlush(any(Secuencia.class));
    }
}

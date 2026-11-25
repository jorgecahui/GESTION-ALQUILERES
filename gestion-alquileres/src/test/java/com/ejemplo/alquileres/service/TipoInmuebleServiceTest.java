package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.TipoInmueble;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.TipoInmuebleRepository;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class TipoInmuebleServiceTest {
    private final TipoInmuebleRepository repo = mock(TipoInmuebleRepository.class);
    private final InmuebleRepository inmuebles = mock(InmuebleRepository.class);
    private final JlccAuditoriaService auditoria = mock(JlccAuditoriaService.class);
    private final JlccTipoInmuebleService service =
            new JlccTipoInmuebleService(repo, inmuebles, DatosFase6.permisosAdmin(), auditoria);

    private TipoInmueble tipo() {
        return TipoInmueble.builder()
                .id(1L)
                .codigo("DEP")
                .nombre("Departamento")
                .activo(true)
                .build();
    }

    @Test
    void desactivaYReactiva() {
        TipoInmueble t = tipo();
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.jlccDesactivar(1L);
        assertFalse(t.isActivo());

        service.jlccReactivar(1L);
        assertTrue(t.isActivo());
        verify(auditoria, times(2)).jlccRegistrar(any(), any(), any(), any());
    }

    @Test
    void reactivarUnTipoYaActivoEsRechazado() {
        TipoInmueble t = tipo();
        when(repo.findById(1L)).thenReturn(Optional.of(t));

        JlccNegocioException error = assertThrows(JlccNegocioException.class, () -> service.jlccReactivar(1L));

        assertTrue(error.getMessage().contains("ya está activo"));
        verify(repo, never()).save(any());
    }

    @Test
    void editarNoCambiaElEstadoActivo() {
        TipoInmueble t = tipo();
        t.setActivo(false);
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        TipoInmueble cambios =
                TipoInmueble.builder()
                        .id(1L)
                        .codigo("DEP")
                        .nombre("Departamento actualizado")
                        .activo(true)
                        .build();

        service.jlccGuardar(cambios);

        assertFalse(t.isActivo());
        assertEquals("Departamento actualizado", t.getNombre());
    }

    @Test
    void consultaNoPuedeReactivarTipos() {
        JlccTipoInmuebleService soloConsulta =
                new JlccTipoInmuebleService(repo, inmuebles, DatosFase6.permisosConsulta(), auditoria);

        JlccNegocioException error =
                assertThrows(JlccNegocioException.class, () -> soloConsulta.jlccReactivar(1L));

        assertTrue(error.getMessage().contains("permiso"));
        verify(repo, never()).findById(any());
    }

    @Test
    void informaSiTieneReferencias() {
        when(inmuebles.existsByTipoId(1L)).thenReturn(true);
        assertTrue(service.jlccTieneReferencias(1L));

        when(inmuebles.existsByTipoId(1L)).thenReturn(false);
        assertFalse(service.jlccTieneReferencias(1L));
    }

    @Test
    void eliminarSinReferenciasBorraYAudita() {
        TipoInmueble t = tipo();
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        when(inmuebles.existsByTipoId(1L)).thenReturn(false);

        service.jlccEliminar(1L);

        verify(repo).deleteById(1L);
        verify(auditoria).jlccRegistrar(any(), any(), any(), contains("Eliminación definitiva"));
    }

    @Test
    void eliminarConReferenciasDesactivaSinBorrar() {
        TipoInmueble t = tipo();
        when(repo.findById(1L)).thenReturn(Optional.of(t));
        when(inmuebles.existsByTipoId(1L)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.jlccEliminar(1L);

        assertFalse(t.isActivo());
        verify(repo, never()).deleteById(any());
    }
}

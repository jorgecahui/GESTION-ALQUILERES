package com.ejemplo.alquileres.service;

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

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.Propietario;
import com.ejemplo.alquileres.model.TipoDocumento;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.PropietarioRepository;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PropietarioServiceTest {
    private final PropietarioRepository repo = mock(PropietarioRepository.class);
    private final InmuebleRepository inmuebles = mock(InmuebleRepository.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final PropietarioService service = new PropietarioService(repo,
            inmuebles, new ValidadorDocumento(),
            DatosFase6.permisosAdmin(), Clock.systemUTC(), auditoria);

    private Propietario propietario() {
        return Propietario.builder().id(1L).tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("45671234").nombre("María Elena Salazar").activo(true).build();
    }

    @Test void desactivaYReactiva() {
        Propietario p = propietario();
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.desactivar(1L);
        assertFalse(p.isActivo());

        service.reactivar(1L);
        assertTrue(p.isActivo());
        verify(auditoria, times(2)).registrar(any(), any(), any(), any());
    }

    @Test void reactivarUnPropietarioYaActivoEsRechazado() {
        Propietario p = propietario();
        when(repo.findById(1L)).thenReturn(Optional.of(p));

        NegocioException error = assertThrows(NegocioException.class,
                () -> service.reactivar(1L));

        assertTrue(error.getMessage().contains("ya está activo"));
        verify(repo, never()).save(any());
    }

    @Test void consultaNoPuedeReactivarPropietarios() {
        PropietarioService soloConsulta = new PropietarioService(repo,
                inmuebles, new ValidadorDocumento(),
                DatosFase6.permisosConsulta(), Clock.systemUTC(), auditoria);

        NegocioException error = assertThrows(NegocioException.class,
                () -> soloConsulta.reactivar(1L));

        assertTrue(error.getMessage().contains("permiso"));
        verify(repo, never()).findById(any());
    }

    @Test void informaSiTieneReferencias() {
        when(inmuebles.existsByPropietarioId(1L)).thenReturn(true);
        assertTrue(service.tieneReferencias(1L));

        when(inmuebles.existsByPropietarioId(1L)).thenReturn(false);
        assertFalse(service.tieneReferencias(1L));
    }

    @Test void eliminarSinReferenciasBorraYAudita() {
        Propietario p = propietario();
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(inmuebles.existsByPropietarioId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(repo).deleteById(1L);
        verify(auditoria).registrar(any(), any(), any(), contains("Eliminación definitiva"));
    }

    @Test void eliminarConReferenciasDesactivaSinBorrar() {
        Propietario p = propietario();
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(inmuebles.existsByPropietarioId(1L)).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.eliminar(1L);

        assertFalse(p.isActivo());
        verify(repo, never()).deleteById(any());
    }
}

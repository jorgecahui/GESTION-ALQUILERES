package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.*;

class InmuebleServiceTest {
    private final InmuebleRepository repo = mock(InmuebleRepository.class);
    private final JlccSecuenciaService secuencia = mock(JlccSecuenciaService.class);
    private final JlccInmuebleService service =
            new JlccInmuebleService(
                    repo,
                    mock(TipoInmuebleRepository.class),
                    mock(PropietarioRepository.class),
                    secuencia,
                    DatosFase6.permisosAdmin(),
                    Clock.systemUTC());

    @Test
    void generaCodigoYNoPermiteAlquiladoManual() {
        when(secuencia.jlccSiguiente("INMUEBLE")).thenReturn(1L);
        when(secuencia.jlccFormatear("INM-", 1L, 4)).thenReturn("INM-0001");
        assertEquals("INM-0001", service.jlccProximoCodigo());
        Inmueble i =
                Inmueble.builder()
                        .codigo("INM-0001")
                        .nombre("Depa")
                        .estado(EstadoInmueble.ALQUILADO)
                        .build();
        assertThrows(JlccNegocioException.class, () -> service.jlccGuardar(i));
    }

    @Test
    void noEliminaTipoConInmuebles() {
        TipoInmuebleRepository tipos = mock(TipoInmuebleRepository.class);
        InmuebleRepository inmuebles = mock(InmuebleRepository.class);
        JlccTipoInmuebleService servicio =
                new JlccTipoInmuebleService(
                        tipos, inmuebles, DatosFase6.permisosAdmin(), DatosFase6.auditoria());
        when(inmuebles.existsByTipoId(4L)).thenReturn(true);
        TipoInmueble tipo = TipoInmueble.builder().id(4L).activo(true).build();
        when(tipos.findById(4L)).thenReturn(java.util.Optional.of(tipo));
        servicio.jlccEliminar(4L);
        assertFalse(tipo.isActivo());
        verify(tipos, never()).deleteById(4L);
    }

    @Test
    void permiteEditarDatosDeInmuebleAlquilado() {
        when(repo.findById(1L))
                .thenReturn(
                        java.util.Optional.of(
                                Inmueble.builder()
                                        .id(1L)
                                        .codigo("INM-0001")
                                        .nombre("Viejo")
                                        .estado(EstadoInmueble.ALQUILADO)
                                        .build()));
        when(repo.existsByCodigoIgnoreCaseAndIdNot("INM-0001", 1L)).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Inmueble editado =
                Inmueble.builder()
                        .id(1L)
                        .codigo("INM-0001")
                        .nombre("Nuevo")
                        .estado(EstadoInmueble.ALQUILADO)
                        .build();
        Inmueble guardado = service.jlccGuardar(editado);
        assertEquals("Nuevo", guardado.getNombre());
        assertEquals(EstadoInmueble.ALQUILADO, guardado.getEstado());
    }

    @Test
    void noCambiaEstadoDeInmuebleAlquiladoManualmente() {
        when(repo.findById(1L))
                .thenReturn(
                        java.util.Optional.of(
                                Inmueble.builder()
                                        .id(1L)
                                        .codigo("INM-0001")
                                        .nombre("Viejo")
                                        .estado(EstadoInmueble.ALQUILADO)
                                        .build()));
        Inmueble editado =
                Inmueble.builder()
                        .id(1L)
                        .codigo("INM-0001")
                        .nombre("Viejo")
                        .estado(EstadoInmueble.DISPONIBLE)
                        .build();
        assertThrows(JlccNegocioException.class, () -> service.jlccGuardar(editado));
    }

    @Test
    void editarConservaTodosLosCamposDelInmueble() {
        when(repo.save(any()))
                .thenAnswer(
                        inv -> {
                            Inmueble x = inv.getArgument(0);
                            if (x.getId() == null) x.setId(1L);
                            return x;
                        });
        Inmueble nuevo =
                Inmueble.builder()
                        .codigo("INM-0001")
                        .nombre("Depto original")
                        .direccion("Av. Principal 123")
                        .referencia("Frente al parque")
                        .distrito("Miraflores")
                        .provincia("Lima")
                        .departamento("Lima")
                        .piso("3")
                        .nivel(2)
                        .orden(4)
                        .areaM2(new BigDecimal("85.50"))
                        .dormitorios(3)
                        .banos(2)
                        .estacionamientos(1)
                        .amoblado(true)
                        .rentaReferencial(new BigDecimal("2500.00"))
                        .estado(EstadoInmueble.DISPONIBLE)
                        .serviciosIncluidos("Agua, luz, internet")
                        .descripcion("Vista al parque")
                        .build();
        Inmueble creado = service.jlccGuardar(nuevo);
        assertEquals(1L, creado.getId());
        assertNotNull(creado.getFechaRegistro());
        LocalDate fechaRegistro = creado.getFechaRegistro();
        when(repo.findById(1L)).thenReturn(java.util.Optional.of(creado));
        when(repo.existsByCodigoIgnoreCaseAndIdNot("INM-0001", 1L)).thenReturn(false);

        Inmueble edicion =
                Inmueble.builder()
                        .id(1L)
                        .codigo("INM-0001")
                        .nombre("Depto actualizado")
                        .estado(EstadoInmueble.DISPONIBLE)
                        .amoblado(true)
                        .build();
        Inmueble actualizado = service.jlccGuardar(edicion);

        assertEquals("Depto actualizado", actualizado.getNombre());
        assertEquals("Av. Principal 123", actualizado.getDireccion());
        assertEquals("Frente al parque", actualizado.getReferencia());
        assertEquals("Miraflores", actualizado.getDistrito());
        assertEquals("Lima", actualizado.getProvincia());
        assertEquals("Lima", actualizado.getDepartamento());
        assertEquals("3", actualizado.getPiso());
        assertEquals(2, actualizado.getNivel());
        assertEquals(4, actualizado.getOrden());
        assertEquals(new BigDecimal("85.50"), actualizado.getAreaM2());
        assertEquals(3, actualizado.getDormitorios());
        assertEquals(2, actualizado.getBanos());
        assertEquals(1, actualizado.getEstacionamientos());
        assertTrue(actualizado.isAmoblado());
        assertEquals(new BigDecimal("2500.00"), actualizado.getRentaReferencial());
        assertEquals(EstadoInmueble.DISPONIBLE, actualizado.getEstado());
        assertEquals("Agua, luz, internet", actualizado.getServiciosIncluidos());
        assertEquals("Vista al parque", actualizado.getDescripcion());
        assertEquals(fechaRegistro, actualizado.getFechaRegistro());
    }

    @Test
    void rechazaPosicionDuplicadaEnNivelYOrden() {
        when(repo.existsByNivelAndOrdenAndEstadoNot(1, 2, EstadoInmueble.INACTIVO))
                .thenReturn(true);
        Inmueble nuevo =
                Inmueble.builder()
                        .codigo("INM-0002")
                        .nombre("Otro inmueble")
                        .nivel(1)
                        .orden(2)
                        .estado(EstadoInmueble.DISPONIBLE)
                        .build();
        JlccNegocioException error = assertThrows(JlccNegocioException.class, () -> service.jlccGuardar(nuevo));
        assertTrue(error.getMessage().contains("nivel 1"));
        assertTrue(error.getMessage().contains("orden 2"));
        verify(repo, never()).save(any());
    }

    @Test
    void nivelYOrdenPuedenQuedarNulos() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Inmueble nuevo =
                Inmueble.builder()
                        .codigo("INM-0003")
                        .nombre("Sin posición")
                        .estado(EstadoInmueble.DISPONIBLE)
                        .build();
        Inmueble guardado = service.jlccGuardar(nuevo);
        assertNull(guardado.getNivel());
        assertNull(guardado.getOrden());
        verify(repo, never()).existsByNivelAndOrdenAndEstadoNot(any(), any(), any());
    }

    @Test
    void validaRangoDeNivelYOrden() {
        Inmueble nivelInvalido =
                Inmueble.builder()
                        .codigo("INM-0004")
                        .nombre("Nivel inválido")
                        .nivel(101)
                        .orden(1)
                        .estado(EstadoInmueble.DISPONIBLE)
                        .build();
        assertThrows(JlccNegocioException.class, () -> service.jlccGuardar(nivelInvalido));
        Inmueble ordenInvalido =
                Inmueble.builder()
                        .codigo("INM-0005")
                        .nombre("Orden inválido")
                        .nivel(1)
                        .orden(0)
                        .estado(EstadoInmueble.DISPONIBLE)
                        .build();
        assertThrows(JlccNegocioException.class, () -> service.jlccGuardar(ordenInvalido));
    }
}

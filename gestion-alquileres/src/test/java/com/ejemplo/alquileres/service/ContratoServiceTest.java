package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.CuotaRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.PersonaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

class ContratoServiceTest {
    private final ContratoRepository contratos = Mockito.mock(ContratoRepository.class);
    private final CuotaRepository cuotas = Mockito.mock(CuotaRepository.class);
    private final InmuebleRepository inmuebles = Mockito.mock(InmuebleRepository.class);
    private final PersonaRepository personas = Mockito.mock(PersonaRepository.class);
    private final JlccSecuenciaService secuencias = Mockito.mock(JlccSecuenciaService.class);
    private final JlccAuditoriaService auditoria = DatosFase6.auditoria();
    private final Clock clock = DatosFase6.CLOCK;
    private JlccContratoService servicio;
    private Inmueble inmueble;
    private Persona arrendatario;

    @BeforeEach
    void preparar() {
        servicio =
                new JlccContratoService(
                        contratos,
                        cuotas,
                        inmuebles,
                        personas,
                        secuencias,
                        new JlccCronogramaGenerador(),
                        DatosFase6.servicioConfiguracion(),
                        DatosFase6.permisosAdmin(),
                        auditoria,
                        clock);
        inmueble = DatosFase6.inmueble();
        inmueble.setEstado(EstadoInmueble.DISPONIBLE);
        arrendatario = DatosFase6.arrendatario("987654321");
        arrendatario.setActivo(true);
        when(inmuebles.findById(1L)).thenReturn(Optional.of(inmueble));
        when(personas.findById(1L)).thenReturn(Optional.of(arrendatario));
        when(contratos.existeTraslape(any(), any(), any(), any())).thenReturn(false);
        when(contratos.save(any(Contrato.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(secuencias.jlccSiguiente("CONTRATO")).thenReturn(1L);
        when(secuencias.jlccFormatear("CTR-", 1L, 6)).thenReturn("CTR-000001");
    }

    @Test
    void creaContratoGeneraCronogramaYAlquilaElInmueble() {
        Contrato contrato =
                servicio.jlccCrear(solicitud(LocalDate.of(2026, 3, 1), 12, new BigDecimal("1000")));

        assertEquals("CTR-000001", contrato.getCodigo());
        assertEquals(12, contrato.getCuotas().size());
        assertEquals(LocalDate.of(2027, 2, 28), contrato.getFechaFin());
        assertEquals(EstadoContrato.VIGENTE, contrato.getEstado());
        assertEquals(EstadoInmueble.ALQUILADO, inmueble.getEstado());
        assertEquals(GarantiaEstado.RECIBIDA, contrato.getGarantiaEstado());
        verify(auditoria)
                .jlccRegistrar(
                        AccionAuditoria.CREACION_CONTRATO, "Contrato", null, "Código CTR-000001");
    }

    @Test
    void rechazaGaranteIgualAlArrendatario() {
        JlccContratoService.Solicitud solicitud =
                new JlccContratoService.Solicitud(
                        1L,
                        1L,
                        1L,
                        LocalDate.of(2026, 3, 1),
                        12,
                        5,
                        new BigDecimal("1000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        false,
                        3,
                        TipoMora.PORCENTAJE_DIARIO,
                        new BigDecimal("0.10"),
                        null);

        assertThrows(JlccNegocioException.class, () -> servicio.jlccCrear(solicitud));
    }

    @Test
    void renuevaTrasladaGarantiaYConservaElInmuebleAlquilado() {
        Contrato anterior = DatosFase6.contrato();
        anterior.setInmueble(inmueble);
        inmueble.setEstado(EstadoInmueble.ALQUILADO);
        anterior.setEstado(EstadoContrato.VIGENTE);
        anterior.setGarantiaMonto(new BigDecimal("1200.00"));
        anterior.setGarantiaEstado(GarantiaEstado.RECIBIDA);
        anterior.setIncrementoAnualPct(new BigDecimal("5"));
        when(contratos.detalle(1L)).thenReturn(Optional.of(anterior));

        Contrato nuevo =
                servicio.jlccRenovar(
                        1L,
                        new JlccContratoService.Solicitud(
                                1L,
                                1L,
                                null,
                                LocalDate.of(2027, 1, 1),
                                12,
                                5,
                                new BigDecimal("1260"),
                                new BigDecimal("5"),
                                BigDecimal.ZERO,
                                false,
                                3,
                                TipoMora.PORCENTAJE_DIARIO,
                                new BigDecimal("0.10"),
                                null));

        assertEquals(EstadoContrato.FINALIZADO, anterior.getEstado());
        assertEquals("Renovado", anterior.getMotivoTermino());
        assertSame(anterior, nuevo.getContratoAnterior());
        assertEquals(EstadoContrato.VIGENTE, nuevo.getEstado());
        assertEquals(LocalDate.of(2027, 1, 1), nuevo.getFechaInicio());
        assertEquals(new BigDecimal("1200.00"), nuevo.getGarantiaMonto());
        assertEquals(GarantiaEstado.RECIBIDA, nuevo.getGarantiaEstado());
        assertEquals(EstadoInmueble.ALQUILADO, inmueble.getEstado());
    }

    @Test
    void finalizaYRegistraLaLiquidacionDeGarantia() {
        Contrato contrato = DatosFase6.contrato();
        contrato.setEstado(EstadoContrato.VIGENTE);
        contrato.setGarantiaMonto(new BigDecimal("1200.00"));
        when(contratos.detalle(1L)).thenReturn(Optional.of(contrato));

        servicio.jlccTerminar(
                1L,
                LocalDate.of(2026, 6, 30),
                "Fin del plazo",
                false,
                new BigDecimal("1000"),
                new BigDecimal("200"),
                "Daño en la puerta");

        assertEquals(EstadoContrato.FINALIZADO, contrato.getEstado());
        assertEquals(LocalDate.of(2026, 6, 30), contrato.getFechaTerminoEfectiva());
        assertEquals(new BigDecimal("1000"), contrato.getGarantiaDevuelta());
        assertEquals(new BigDecimal("200"), contrato.getGarantiaDescontada());
        assertEquals("Daño en la puerta", contrato.getGarantiaMotivo());
        assertEquals(GarantiaEstado.RETENIDA_PARCIAL, contrato.getGarantiaEstado());
        assertEquals(EstadoInmueble.DISPONIBLE, contrato.getInmueble().getEstado());
        verify(cuotas).anularFuturas(1L, LocalDate.of(2026, 6, 30));
    }

    @Test
    void rescindirExigeMotivoYNoLiquidaSiExcedeLaGarantia() {
        Contrato contrato = DatosFase6.contrato();
        contrato.setGarantiaMonto(new BigDecimal("1200.00"));
        when(contratos.detalle(1L)).thenReturn(Optional.of(contrato));

        assertThrows(
                JlccNegocioException.class,
                () ->
                        servicio.jlccTerminar(
                                1L,
                                LocalDate.of(2026, 6, 30),
                                " ",
                                true,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                null));
        assertThrows(
                JlccNegocioException.class,
                () ->
                        servicio.jlccTerminar(
                                1L,
                                LocalDate.of(2026, 6, 30),
                                "Acuerdo",
                                false,
                                new BigDecimal("1000"),
                                new BigDecimal("300"),
                                "Daño"));
        verify(cuotas, never()).anularFuturas(any(), any());
        assertNotNull(contrato.getEstado());
    }

    private JlccContratoService.Solicitud solicitud(LocalDate inicio, int meses, BigDecimal renta) {
        return new JlccContratoService.Solicitud(
                1L,
                1L,
                null,
                inicio,
                meses,
                5,
                renta,
                BigDecimal.ZERO,
                new BigDecimal("1200"),
                true,
                3,
                TipoMora.PORCENTAJE_DIARIO,
                new BigDecimal("0.10"),
                null);
    }
}

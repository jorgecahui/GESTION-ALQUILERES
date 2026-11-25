package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.CuotaRepository;
import com.ejemplo.alquileres.repository.PagoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

class PagoServiceTest {
    private final ContratoRepository contratos = Mockito.mock(ContratoRepository.class);
    private final CuotaRepository cuotas = Mockito.mock(CuotaRepository.class);
    private final PagoRepository pagos = Mockito.mock(PagoRepository.class);
    private final JlccSecuenciaService secuencias = Mockito.mock(JlccSecuenciaService.class);
    private JlccPagoService servicio;
    private Contrato contrato;
    private Cuota cuota;

    @BeforeEach
    void preparar() {
        AppProperties propiedades = new AppProperties();
        propiedades.getRecibos().setSerie("R001");
        Clock clock = Clock.fixed(Instant.parse("2026-01-12T12:00:00Z"), ZoneOffset.UTC);
        servicio =
                new JlccPagoService(
                        pagos,
                        cuotas,
                        contratos,
                        secuencias,
                        new JlccCalculadoraMora(),
                        DatosFase6.servicioConfiguracion(propiedades),
                        DatosFase6.permisosAdmin(),
                        DatosFase6.auditoria(),
                        clock);
        cuota = cuota(10L, "1000.00", LocalDate.of(2026, 1, 5));
        contrato = contrato(cuota);
        when(contratos.detalle(1L)).thenReturn(Optional.of(contrato));
        when(cuotas.findById(10L)).thenReturn(Optional.of(cuota));
        when(secuencias.jlccSiguiente("RECIBO-R001")).thenReturn(1L);
        when(secuencias.jlccFormatear("R001-", 1L, 8)).thenReturn("R001-00000001");
        when(pagos.save(any(Pago.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void registraPagoCompletoYConservaLaNumeracion() {
        Pago pago =
                servicio.jlccRegistrar(
                        solicitud(
                                new JlccPagoService.Linea(
                                        10L,
                                        dinero("1000"),
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        null),
                                LocalDate.of(2026, 1, 5)));

        assertEquals("R001-00000001", pago.getRecibo());
        assertEquals(EstadoCuota.PAGADA, cuota.getEstado());
        assertEquals(dinero("1000"), cuota.getMontoPagado());
        assertEquals(dinero("1000"), pago.getMontoTotal());
    }

    @Test
    void pagoParcialRegistraPuntoDeControlDeMora() {
        Pago pago =
                servicio.jlccRegistrar(
                        solicitud(
                                new JlccPagoService.Linea(
                                        10L, dinero("400"), dinero("7"), BigDecimal.ZERO, null)));

        assertEquals(EstadoCuota.PARCIAL, cuota.getEstado());
        assertEquals(dinero("400"), cuota.getMontoPagado());
        assertEquals(dinero("7"), cuota.getMoraAcumulada());
        assertEquals(LocalDate.of(2026, 1, 12), cuota.getMoraDesde());
        assertEquals(dinero("407"), pago.getMontoTotal());
    }

    @Test
    void exigePagarOCondonarLaMoraAntesDelCapital() {
        JlccPagoService.Linea sinMora =
                new JlccPagoService.Linea(10L, dinero("400"), BigDecimal.ZERO, BigDecimal.ZERO, null);
        assertThrows(JlccNegocioException.class, () -> servicio.jlccRegistrar(solicitud(sinMora)));

        JlccPagoService.Linea condonada =
                new JlccPagoService.Linea(
                        10L, dinero("400"), BigDecimal.ZERO, dinero("7"), "Acuerdo de pago");
        servicio.jlccRegistrar(solicitud(condonada));
        assertEquals(dinero("7"), cuota.getMoraCondonada());
        assertEquals(EstadoCuota.PARCIAL, cuota.getEstado());
    }

    @Test
    void rechazaSobrepagoYDistribuyeDesdeLaCuotaMasAntigua() {
        assertThrows(
                JlccNegocioException.class,
                () ->
                        servicio.jlccRegistrar(
                                solicitud(
                                        new JlccPagoService.Linea(
                                                10L,
                                                dinero("1000.01"),
                                                dinero("7"),
                                                BigDecimal.ZERO,
                                                null))));
        Cuota segunda = cuota(11L, "500.00", LocalDate.of(2026, 1, 10));
        segunda.setContrato(contrato);
        when(cuotas.findByContratoIdOrderByFechaVencimientoAsc(1L))
                .thenReturn(List.of(cuota, segunda));

        List<JlccPagoService.Imputacion> propuesta =
                servicio.jlccProponerImputacion(1L, dinero("1014"), LocalDate.of(2026, 1, 12));
        assertEquals(2, propuesta.size());
        assertEquals(10L, propuesta.get(0).cuotaId());
        assertEquals(dinero("1000"), propuesta.get(0).capital());
        assertEquals(11L, propuesta.get(1).cuotaId());
    }

    @Test
    void anularRestauraExactamenteLaCuota() {
        cuota.setMontoPagado(dinero("400"));
        cuota.setMoraAcumulada(dinero("7"));
        cuota.setMoraCobrada(dinero("7"));
        cuota.setEstado(EstadoCuota.PARCIAL);
        Pago pago =
                Pago.builder()
                        .id(5L)
                        .recibo("R001-00000001")
                        .estado(EstadoPago.EMITIDO)
                        .fechaRegistro(java.time.LocalDateTime.of(2026, 1, 12, 12, 0))
                        .build();
        pago.agregarDetalle(
                PagoDetalle.builder()
                        .cuota(cuota)
                        .montoCapital(dinero("400"))
                        .montoMora(dinero("7"))
                        .montoMoraCondonada(BigDecimal.ZERO)
                        .moraAcumuladaAntes(BigDecimal.ZERO)
                        .estadoCuotaAntes(EstadoCuota.PENDIENTE)
                        .build());
        when(pagos.detalle(5L)).thenReturn(Optional.of(pago));
        when(pagos.activosDeCuota(10L)).thenReturn(List.of(pago));

        servicio.jlccAnular(5L, "Registro equivocado");
        assertEquals(EstadoPago.ANULADO, pago.getEstado());
        assertEquals(BigDecimal.ZERO.setScale(2), cuota.getMontoPagado());
        assertEquals(EstadoCuota.PENDIENTE, cuota.getEstado());
    }

    private JlccPagoService.Solicitud solicitud(JlccPagoService.Linea linea) {
        return solicitud(linea, LocalDate.of(2026, 1, 12));
    }

    private JlccPagoService.Solicitud solicitud(JlccPagoService.Linea linea, LocalDate fecha) {
        return new JlccPagoService.Solicitud(
                1L, fecha, MedioPago.EFECTIVO, null, null, List.of(linea), false);
    }

    private Contrato contrato(Cuota cuota) {
        Persona arrendatario =
                Persona.builder()
                        .nombres("Ana")
                        .apellidos("Pérez")
                        .numeroDocumento("12345678")
                        .build();
        Inmueble inmueble =
                Inmueble.builder().codigo("INM-0001").nombre("Departamento 101").build();
        Contrato contrato =
                Contrato.builder()
                        .id(1L)
                        .arrendatario(arrendatario)
                        .inmueble(inmueble)
                        .fechaInicio(LocalDate.of(2026, 1, 1))
                        .diasGracia(3)
                        .tipoMora(TipoMora.PORCENTAJE_DIARIO)
                        .valorMora(new BigDecimal("0.10"))
                        .cuotas(List.of(cuota))
                        .build();
        cuota.setContrato(contrato);
        return contrato;
    }

    private Cuota cuota(Long id, String importe, LocalDate vencimiento) {
        return Cuota.builder()
                .id(id)
                .concepto("Alquiler")
                .periodo(vencimiento.withDayOfMonth(1))
                .fechaVencimiento(vencimiento)
                .montoCuota(dinero(importe))
                .montoPagado(BigDecimal.ZERO)
                .moraAcumulada(BigDecimal.ZERO)
                .moraCobrada(BigDecimal.ZERO)
                .moraCondonada(BigDecimal.ZERO)
                .estado(EstadoCuota.PENDIENTE)
                .build();
    }

    private static BigDecimal dinero(String valor) {
        return new BigDecimal(valor).setScale(2);
    }
}

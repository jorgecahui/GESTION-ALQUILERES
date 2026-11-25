package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoContrato;
import com.ejemplo.alquileres.model.EstadoCuota;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.Pago;
import com.ejemplo.alquileres.model.Persona;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.PagoRepository;
import com.ejemplo.alquileres.repository.PersonaRepository;
import com.ejemplo.alquileres.seeder.HistoriaDashboardSeeder;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class HistoriaDashboardSeederTest {
    private final ContratoRepository contratos = Mockito.mock(ContratoRepository.class);
    private final PagoRepository pagos = Mockito.mock(PagoRepository.class);
    private final InmuebleRepository inmuebles = Mockito.mock(InmuebleRepository.class);
    private final PersonaRepository personas = Mockito.mock(PersonaRepository.class);
    private final ConfiguracionService configuracion = Mockito.mock(ConfiguracionService.class);

    @Test
    void reparaElDetalleFaltanteDeUnPagoDemo() {
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

    @Test
    void completaElCronogramaDemoYUsaConceptosEnEspanol() {
        Contrato contrato = DatosFase6.contrato();
        contrato.setCodigo("CTR-DEV-TEST");
        when(contratos.count()).thenReturn(1L);
        when(contratos.findAll()).thenReturn(List.of(contrato));
        when(pagos.findAll()).thenReturn(List.of());
        when(contratos.save(any(Contrato.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        seeder().cargar();

        assertEquals(12, contrato.getCuotas().size());
        assertEquals("Renta enero 2026", contrato.getCuotas().get(0).getConcepto());
        assertEquals("Renta diciembre 2026", contrato.getCuotas().get(11).getConcepto());
        assertTrue(
                contrato.getCuotas().stream().allMatch(c -> c.getConcepto().startsWith("Renta ")));
    }

    @Test
    void noTocaPagosRealesSinDetalle() {
        Pago pago =
                Pago.builder()
                        .id(9L)
                        .recibo("R001-00000001")
                        .contrato(DatosFase6.contrato())
                        .fechaPago(LocalDate.of(2026, 1, 5))
                        .montoCapital(new BigDecimal("1200.00"))
                        .build();
        when(contratos.count()).thenReturn(1L);
        when(pagos.findAll()).thenReturn(List.of(pago));

        seeder().cargar();

        assertTrue(pago.getDetalles().isEmpty());
        verify(pagos, never()).save(any(Pago.class));
    }

    @Test
    void generaDiezContratosConRepartoDeSemaforos() {
        when(contratos.count()).thenReturn(0L);
        when(configuracion.umbralLeveDias()).thenReturn(15);
        when(configuracion.cuotasRojo()).thenReturn(2);
        when(configuracion.moraDiasGracia()).thenReturn(3);

        List<Inmueble> alquilados = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            alquilados.add(
                    Inmueble.builder()
                            .id((long) i)
                            .codigo("INM-" + i)
                            .nombre("Unidad " + i)
                            .nivel((i - 1) / 5 + 1)
                            .orden((i - 1) % 5 + 1)
                            .estado(EstadoInmueble.ALQUILADO)
                            .rentaReferencial(new BigDecimal("1200.00"))
                            .build());
        }
        List<Persona> activas = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            activas.add(
                    Persona.builder()
                            .id((long) i)
                            .nombres("Inquilino " + i)
                            .apellidos("Demo")
                            .numeroDocumento(String.format("%08d", 73000000 + i))
                            .activo(true)
                            .build());
        }
        List<Contrato> guardados = new ArrayList<>();
        when(inmuebles.findAll()).thenReturn(alquilados);
        when(personas.findAll()).thenReturn(activas);
        when(contratos.save(any(Contrato.class)))
                .thenAnswer(
                        invocacion -> {
                            Contrato contrato = invocacion.getArgument(0);
                            guardados.add(contrato);
                            return contrato;
                        });
        when(pagos.save(any(Pago.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        seeder().cargar();

        assertEquals(10, guardados.size());
        assertTrue(guardados.stream().allMatch(c -> c.getEstado() == EstadoContrato.VIGENTE));
        MorosidadService morosidad =
                new MorosidadService(DatosFase6.servicioConfiguracion(), DatosFase6.CLOCK);
        Map<SemaforoMorosidad, Long> reparto =
                guardados.stream()
                        .collect(Collectors.groupingBy(morosidad::semaforo, Collectors.counting()));
        assertEquals(6L, reparto.getOrDefault(SemaforoMorosidad.AL_DIA, 0L));
        assertEquals(2L, reparto.getOrDefault(SemaforoMorosidad.ATRASO_LEVE, 0L));
        assertEquals(2L, reparto.getOrDefault(SemaforoMorosidad.MOROSO, 0L));
        for (Contrato contrato : guardados) {
            long vencidas = morosidad.vencidas(contrato).size();
            switch (morosidad.semaforo(contrato)) {
                case AL_DIA -> assertEquals(0, vencidas, contrato.getCodigo());
                case ATRASO_LEVE -> assertEquals(1, vencidas, contrato.getCodigo());
                case MOROSO -> assertEquals(2, vencidas, contrato.getCodigo());
            }
        }

        LocalDate hoy = DatosFase6.HOY;
        Map<String, Long> vencimientos =
                guardados.stream()
                        .collect(
                                Collectors.groupingBy(
                                        contrato -> categoriaVencimiento(contrato, hoy),
                                        Collectors.counting()));
        assertEquals(2L, vencimientos.getOrDefault("0-30", 0L));
        assertEquals(2L, vencimientos.getOrDefault("31-90", 0L));
        assertEquals(5L, vencimientos.getOrDefault("MAS-90", 0L));
        assertEquals(1L, vencimientos.getOrDefault("VENCIDO", 0L));

        List<Contrato> lejanos =
                guardados.stream().filter(contrato -> diasParaVencer(contrato, hoy) > 90).toList();
        assertEquals(5, lejanos.size());
        assertEquals(5, lejanos.stream().map(Contrato::getFechaFin).distinct().count());

        assertTrue(
                guardados.stream()
                        .anyMatch(
                                contrato ->
                                        morosidad.semaforo(contrato) == SemaforoMorosidad.MOROSO
                                                && diasParaVencer(contrato, hoy) > 90));
        assertTrue(
                guardados.stream()
                        .anyMatch(
                                contrato ->
                                        morosidad.semaforo(contrato) == SemaforoMorosidad.AL_DIA
                                                && diasParaVencer(contrato, hoy) >= 0
                                                && diasParaVencer(contrato, hoy) <= 30));

        Set<Long> alquiladosIds =
                alquilados.stream().map(Inmueble::getId).collect(Collectors.toSet());
        Set<Long> conContrato =
                guardados.stream()
                        .map(contrato -> contrato.getInmueble().getId())
                        .collect(Collectors.toSet());
        assertEquals(alquiladosIds, conContrato);

        for (Contrato contrato : guardados) {
            int meses =
                    (int)
                            ChronoUnit.MONTHS.between(
                                    contrato.getFechaInicio(), contrato.getFechaFin().plusDays(1));
            List<Cuota> cuotas =
                    contrato.getCuotas().stream()
                            .sorted(Comparator.comparingInt(Cuota::getNumero))
                            .toList();
            assertEquals(meses, cuotas.size(), contrato.getCodigo());
            assertEquals(1, cuotas.get(0).getNumero());
            assertEquals(meses, cuotas.get(meses - 1).getNumero());
            for (Cuota cuota : cuotas) {
                assertNotNull(cuota.getPeriodo());
                assertTrue(!cuota.getFechaVencimiento().isBefore(contrato.getFechaInicio()));
                assertTrue(!cuota.getFechaVencimiento().isAfter(contrato.getFechaFin()));
            }
        }

        Contrato vencido =
                guardados.stream()
                        .filter(contrato -> contrato.getFechaFin().isBefore(hoy))
                        .findFirst()
                        .orElseThrow();
        assertTrue(
                vencido.getCuotas().stream()
                        .allMatch(cuota -> cuota.getFechaVencimiento().isBefore(hoy)));
        assertEquals(
                2,
                vencido.getCuotas().stream()
                        .filter(cuota -> cuota.getEstado() == EstadoCuota.PENDIENTE)
                        .count());
    }

    private long diasParaVencer(Contrato contrato, LocalDate hoy) {
        return ChronoUnit.DAYS.between(hoy, contrato.getFechaFin());
    }

    private String categoriaVencimiento(Contrato contrato, LocalDate hoy) {
        long dias = diasParaVencer(contrato, hoy);
        if (dias < 0) return "VENCIDO";
        if (dias <= 30) return "0-30";
        if (dias <= 90) return "31-90";
        return "MAS-90";
    }

    private HistoriaDashboardSeeder seeder() {
        return new HistoriaDashboardSeeder(
                contratos, pagos, inmuebles, personas, configuracion, DatosFase6.CLOCK);
    }

    private Pago pagoDemo(Contrato contrato) {
        return Pago.builder()
                .id(1L)
                .recibo("R001-DEV-1-1")
                .contrato(contrato)
                .fechaPago(LocalDate.of(2026, 1, 5))
                .montoCapital(new BigDecimal("1200.00"))
                .montoMora(BigDecimal.ZERO)
                .build();
    }
}

package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoCuota;
import com.ejemplo.alquileres.model.SemaforoMorosidad;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

class MorosidadServiceTest {
    private final MorosidadService servicio = servicio();

    @Test
    void quedaAlDiaSinCuotasVencidas() {
        assertEquals(
                SemaforoMorosidad.AL_DIA,
                servicio.semaforo(contrato(cuota(LocalDate.of(2026, 1, 19)))));
    }

    @Test
    void marcaAtrasoLeveDentroDelUmbral() {
        assertEquals(
                SemaforoMorosidad.ATRASO_LEVE,
                servicio.semaforo(contrato(cuota(LocalDate.of(2026, 1, 10)))));
    }

    @Test
    void marcaMorosoPorDiasOPorCantidadDeCuotas() {
        assertEquals(
                SemaforoMorosidad.MOROSO,
                servicio.semaforo(contrato(cuota(LocalDate.of(2025, 12, 20)))));
        assertEquals(
                SemaforoMorosidad.MOROSO,
                servicio.semaforo(
                        contrato(
                                cuota(LocalDate.of(2026, 1, 10)),
                                cuota(LocalDate.of(2026, 1, 11)))));
    }

    private MorosidadService servicio() {
        AppProperties propiedades = new AppProperties();
        propiedades.getMorosidad().setUmbralLeveDias(15);
        propiedades.getMorosidad().setCuotasRojo(2);
        Clock clock = Clock.fixed(Instant.parse("2026-01-20T12:00:00Z"), ZoneOffset.UTC);
        return new MorosidadService(DatosFase6.servicioConfiguracion(propiedades), clock);
    }

    private Contrato contrato(Cuota... cuotas) {
        Contrato contrato = Contrato.builder().diasGracia(3).cuotas(List.of(cuotas)).build();
        for (Cuota cuota : cuotas) cuota.setContrato(contrato);
        return contrato;
    }

    private Cuota cuota(LocalDate vencimiento) {
        return Cuota.builder()
                .fechaVencimiento(vencimiento)
                .montoCuota(BigDecimal.TEN)
                .montoPagado(BigDecimal.ZERO)
                .estado(EstadoCuota.PENDIENTE)
                .build();
    }
}

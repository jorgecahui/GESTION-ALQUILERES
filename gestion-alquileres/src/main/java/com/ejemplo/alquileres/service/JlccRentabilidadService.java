package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JlccRentabilidadService {
    private final InmuebleRepository jlccInmuebles;
    private final ContratoRepository jlccContratos;
    private final PagoRepository jlccPagos;
    private final MantenimientoRepository jlccMantenimientos;
    private final Clock jlccClock;

    public record Fila(
            Long inmuebleId,
            String codigo,
            String inmueble,
            BigDecimal capital,
            BigDecimal mora,
            BigDecimal ingresos,
            BigDecimal gastos,
            BigDecimal neto,
            BigDecimal margen,
            long diasOcupado,
            BigDecimal ocupacion) {}

    public record Resumen(
            List<Fila> filas,
            BigDecimal ingresos,
            BigDecimal gastos,
            BigDecimal neto,
            BigDecimal margen) {}

    public Resumen jlccPorInmueble(LocalDate jlccDesde, LocalDate jlccHasta) {
        if (jlccDesde == null || jlccHasta == null || jlccHasta.isBefore(jlccDesde))
            throw new IllegalArgumentException("El período no es válido.");
        Map<Long, BigDecimal> jlccCapital = new HashMap<>(),
                jlccMora = new HashMap<>(),
                jlccGastos = new HashMap<>();
        for (Pago p : jlccPagos.listarPeriodo(jlccDesde, jlccHasta))
            if (p.getEstado() == EstadoPago.EMITIDO) {
                Long jlccId = p.getContrato().getInmueble().getId();
                jlccCapital.merge(jlccId, jlccNvl(p.getMontoCapital()), BigDecimal::add);
                jlccMora.merge(jlccId, jlccNvl(p.getMontoMora()), BigDecimal::add);
            }
        for (Mantenimiento m : jlccMantenimientos.gastosPropietario(jlccDesde, jlccHasta))
            jlccGastos.merge(m.getInmueble().getId(), jlccNvl(m.getCostoReal()), BigDecimal::add);
        long jlccTotalDias = ChronoUnit.DAYS.between(jlccDesde, jlccHasta) + 1;
        List<Fila> jlccFilas = new ArrayList<>();
        for (Inmueble i : jlccInmuebles.findAll()) {
            BigDecimal jlccCap = jlccDinero(jlccCapital.getOrDefault(i.getId(), BigDecimal.ZERO)),
                    jlccMor = jlccDinero(jlccMora.getOrDefault(i.getId(), BigDecimal.ZERO)),
                    jlccIngreso = jlccDinero(jlccCap.add(jlccMor)),
                    jlccGasto = jlccDinero(jlccGastos.getOrDefault(i.getId(), BigDecimal.ZERO)),
                    jlccNeto = jlccDinero(jlccIngreso.subtract(jlccGasto));
            long jlccOcupado =
                    jlccContratos.findByInmuebleIdOrderByFechaInicioDesc(i.getId()).stream()
                            .mapToLong(c -> jlccInterseccion(c, jlccDesde, jlccHasta))
                            .sum();
            jlccOcupado = Math.min(jlccTotalDias, jlccOcupado);
            BigDecimal jlccMargen =
                    jlccIngreso.signum() == 0
                            ? BigDecimal.ZERO.setScale(2)
                            : jlccDinero(
                                    jlccNeto.multiply(BigDecimal.valueOf(100))
                                            .divide(jlccIngreso, 2, RoundingMode.HALF_UP));
            jlccFilas.add(
                    new Fila(
                            i.getId(),
                            i.getCodigo(),
                            i.getNombre(),
                            jlccCap,
                            jlccMor,
                            jlccIngreso,
                            jlccGasto,
                            jlccNeto,
                            jlccMargen,
                            jlccOcupado,
                            jlccPorcentaje(jlccOcupado, jlccTotalDias)));
        }
        jlccFilas.sort(Comparator.comparing(Fila::neto).reversed());
        return jlccTotalizar(jlccFilas);
    }

    public Resumen jlccAnoActual(Long jlccInmuebleId) {
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        List<Fila> jlccFila =
                jlccPorInmueble(jlccHoy.withDayOfYear(1), jlccHoy).filas().stream()
                        .filter(f -> f.inmuebleId().equals(jlccInmuebleId))
                        .toList();
        return jlccTotalizar(jlccFila);
    }

    private Resumen jlccTotalizar(List<Fila> jlccFilas) {
        BigDecimal
                jlccIngreso =
                        jlccFilas.stream().map(Fila::ingresos).reduce(BigDecimal.ZERO, BigDecimal::add),
                jlccGasto = jlccFilas.stream().map(Fila::gastos).reduce(BigDecimal.ZERO, BigDecimal::add),
                jlccNeto = jlccIngreso.subtract(jlccGasto);
        return new Resumen(
                jlccFilas,
                jlccDinero(jlccIngreso),
                jlccDinero(jlccGasto),
                jlccDinero(jlccNeto),
                jlccIngreso.signum() == 0
                        ? BigDecimal.ZERO.setScale(2)
                        : jlccDinero(
                                jlccNeto.multiply(BigDecimal.valueOf(100))
                                        .divide(jlccIngreso, 2, RoundingMode.HALF_UP)));
    }

    private long jlccInterseccion(Contrato c, LocalDate jlccDesde, LocalDate jlccHasta) {
        LocalDate jlccFin =
                c.getFechaTerminoEfectiva() == null ? c.getFechaFin() : c.getFechaTerminoEfectiva();
        LocalDate jlccInicio = c.getFechaInicio().isAfter(jlccDesde) ? c.getFechaInicio() : jlccDesde;
        LocalDate jlccUltimo = jlccFin.isBefore(jlccHasta) ? jlccFin : jlccHasta;
        return jlccUltimo.isBefore(jlccInicio) ? 0 : ChronoUnit.DAYS.between(jlccInicio, jlccUltimo) + 1;
    }

    private static BigDecimal jlccNvl(BigDecimal n) {
        return n == null ? BigDecimal.ZERO : n;
    }

    private static BigDecimal jlccDinero(BigDecimal n) {
        return n.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal jlccPorcentaje(long a, long b) {
        return b == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(a)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(b), 2, RoundingMode.HALF_UP);
    }
}

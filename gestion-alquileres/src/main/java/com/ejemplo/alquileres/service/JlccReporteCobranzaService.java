package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.PagoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class JlccReporteCobranzaService {
    private final PagoRepository jlccPagos;

    public JlccReporteCobranzaService(PagoRepository p) {
        jlccPagos = p;
    }

    public record PuntoSerie(LocalDate fecha, BigDecimal total, long pagos) {}

    public record IngresoInmueble(String nombre, BigDecimal importe) {}

    public record Resumen(
            LocalDate desde,
            LocalDate hasta,
            BigDecimal total,
            BigDecimal anterior,
            Double variacion,
            boolean porMes,
            List<PuntoSerie> serie,
            List<IngresoInmueble> top,
            List<Pago> ultimos,
            List<Pago> cobros) {}

    public Resumen jlccResumen(Periodo.Rango jlccRango) {
        List<Pago> jlccTodos = jlccPagos.listarPeriodo(jlccRango.desde(), jlccRango.hasta()),
                jlccEmitidos = jlccTodos.stream().filter(p -> p.getEstado() == EstadoPago.EMITIDO).toList();
        Periodo.Rango jlccAnterior = jlccRango.anterior();
        BigDecimal jlccAntes =
                jlccSumar(
                        jlccPagos.listarPeriodo(jlccAnterior.desde(), jlccAnterior.hasta()).stream()
                                .filter(p -> p.getEstado() == EstadoPago.EMITIDO)
                                .toList());
        BigDecimal jlccTotal = jlccSumar(jlccEmitidos);
        Double jlccVariacion =
                jlccAntes.signum() == 0
                        ? null
                        : jlccTotal.subtract(jlccAntes)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(jlccAntes, 2, RoundingMode.HALF_UP)
                                .doubleValue();
        boolean jlccPorMes = jlccRango.dias() > 90;
        return new Resumen(
                jlccRango.desde(),
                jlccRango.hasta(),
                jlccTotal,
                jlccAntes,
                jlccVariacion,
                jlccPorMes,
                jlccSerie(jlccEmitidos, jlccRango, jlccPorMes),
                jlccTop(jlccEmitidos),
                jlccEmitidos.stream().limit(8).toList(),
                jlccTodos);
    }

    private List<PuntoSerie> jlccSerie(List<Pago> jlccLista, Periodo.Rango r, boolean jlccMes) {
        Map<LocalDate, BigDecimal> jlccMontos = new TreeMap<>();
        Map<LocalDate, Long> jlccCantidad = new TreeMap<>();
        for (Pago p : jlccLista) {
            LocalDate k = jlccMes ? YearMonth.from(p.getFechaPago()).atDay(1) : p.getFechaPago();
            jlccMontos.merge(k, p.getMontoTotal(), BigDecimal::add);
            jlccCantidad.merge(k, 1L, Long::sum);
        }
        List<PuntoSerie> jlccSalida = new ArrayList<>();
        for (LocalDate d = jlccMes ? YearMonth.from(r.desde()).atDay(1) : r.desde();
                !d.isAfter(r.hasta());
                d = jlccMes ? d.plusMonths(1) : d.plusDays(1))
            jlccSalida.add(
                    new PuntoSerie(
                            d, jlccMontos.getOrDefault(d, jlccCero()), jlccCantidad.getOrDefault(d, 0L)));
        return jlccSalida;
    }

    private List<IngresoInmueble> jlccTop(List<Pago> jlccLista) {
        Map<String, BigDecimal> m = new HashMap<>();
        for (Pago p : jlccLista) m.merge(p.getInmuebleNombre(), p.getMontoTotal(), BigDecimal::add);
        return m.entrySet().stream()
                .map(e -> new IngresoInmueble(e.getKey(), jlccDinero(e.getValue())))
                .sorted(Comparator.comparing(IngresoInmueble::importe).reversed())
                .limit(5)
                .toList();
    }

    private BigDecimal jlccSumar(List<Pago> p) {
        return jlccDinero(p.stream().map(Pago::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal jlccCero() {
        return new BigDecimal("0.00");
    }

    private static BigDecimal jlccDinero(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}

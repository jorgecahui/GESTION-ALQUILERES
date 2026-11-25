package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class JlccDashboardService {
    private final ContratoRepository jlccContratos;
    private final CuotaRepository jlccCuotas;
    private final InmuebleRepository jlccInmuebles;
    private final JlccCalculadoraMora jlccCalculadora;
    private final JlccMorosidadService jlccMorosidad;
    private final JlccConfiguracionService jlccConfiguracion;
    private final Clock jlccClock;

    public JlccDashboardService(
            ContratoRepository c,
            CuotaRepository q,
            InmuebleRepository i,
            JlccCalculadoraMora m,
            JlccMorosidadService s,
            JlccConfiguracionService p,
            Clock k) {
        jlccContratos = c;
        jlccCuotas = q;
        jlccInmuebles = i;
        jlccCalculadora = m;
        jlccMorosidad = s;
        jlccConfiguracion = p;
        jlccClock = k;
    }

    public record Cartera(BigDecimal total, long cuotas, long arrendatarios, double porcentaje) {}

    public record Ocupacion(long alquilados, long total, List<Tipo> porTipo) {}

    public record Tipo(String nombre, long cantidad) {}

    public record Mora(
            String arrendatario,
            String inmueble,
            BigDecimal deuda,
            long dias,
            SemaforoMorosidad semaforo) {}

    public record Vencimiento(
            Long id,
            String codigo,
            String inmueble,
            String arrendatario,
            LocalDate fin,
            long dias) {}

    public record Resumen(
            Cartera cartera,
            Ocupacion ocupacion,
            List<Mora> morosos,
            List<Vencimiento> vencimientos) {}

    public Resumen jlccResumen() {
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        List<Cuota> jlccVencidas =
                jlccCuotas.pendientesConContrato().stream()
                        .filter(
                                q ->
                                        q.getFechaVencimiento()
                                                .plusDays(q.getContrato().getDiasGracia())
                                                .isBefore(jlccHoy))
                        .toList();
        Map<Contrato, BigDecimal> jlccDeuda = new LinkedHashMap<>();
        for (Cuota q : jlccVencidas)
            jlccDeuda.merge(
                    q.getContrato(),
                    jlccCalculadora.jlccSaldoCapital(q).add(jlccCalculadora.jlccPendiente(q, q.getContrato(), jlccHoy)),
                    BigDecimal::add);
        long jlccVigentes =
                jlccContratos.cobrables().stream()
                        .filter(c -> c.getEstado() == EstadoContrato.VIGENTE)
                        .count();
        Cartera jlccCartera =
                new Cartera(
                        jlccDinero(jlccDeuda.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)),
                        jlccVencidas.size(),
                        jlccDeuda.size(),
                        jlccVigentes == 0 ? 0 : 100d * jlccDeuda.size() / jlccVigentes);
        List<Inmueble> jlccLista = jlccInmuebles.findAll();
        List<Inmueble> jlccActivos =
                jlccLista.stream().filter(i -> i.getEstado() != EstadoInmueble.INACTIVO).toList();
        long jlccAlquilados =
                jlccActivos.stream().filter(i -> i.getEstado() == EstadoInmueble.ALQUILADO).count();
        Map<String, Long> jlccTipos = new TreeMap<>();
        for (Inmueble i : jlccActivos)
            jlccTipos.merge(i.getTipo() == null ? "Sin tipo" : i.getTipo().getNombre(), 1L, Long::sum);
        Ocupacion jlccOcupacion =
                new Ocupacion(
                        jlccAlquilados,
                        jlccActivos.size(),
                        jlccTipos.entrySet().stream()
                                .map(e -> new Tipo(e.getKey(), e.getValue()))
                                .toList());
        List<Mora> jlccMorosos =
                jlccDeuda.entrySet().stream()
                        .map(
                                e ->
                                        new Mora(
                                                e.getKey().getArrendatario().getNombreCompleto(),
                                                e.getKey().getInmueble().getNombre(),
                                                jlccDinero(e.getValue()),
                                                jlccMaxDias(e.getKey(), jlccHoy),
                                                jlccMorosidad.jlccSemaforo(e.getKey())))
                        .sorted(Comparator.comparing(Mora::deuda).reversed())
                        .limit(5)
                        .toList();
        List<Vencimiento> jlccVencer =
                jlccContratos.porVencer(jlccHoy, jlccHoy.plusDays(jlccConfiguracion.jlccDiasVencimiento())).stream()
                        .map(
                                c ->
                                        new Vencimiento(
                                                c.getId(),
                                                c.getCodigo(),
                                                c.getInmueble().getNombre(),
                                                c.getArrendatario().getNombreCompleto(),
                                                c.getFechaFin(),
                                                ChronoUnit.DAYS.between(jlccHoy, c.getFechaFin())))
                        .toList();
        return new Resumen(jlccCartera, jlccOcupacion, jlccMorosos, jlccVencer);
    }

    private long jlccMaxDias(Contrato c, LocalDate h) {
        return c.getCuotas().stream()
                .filter(
                        q ->
                                q.getEstado() == EstadoCuota.PENDIENTE
                                        || q.getEstado() == EstadoCuota.PARCIAL)
                .mapToLong(q -> Math.max(0, ChronoUnit.DAYS.between(q.getFechaVencimiento(), h)))
                .max()
                .orElse(0);
    }

    private BigDecimal jlccDinero(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}

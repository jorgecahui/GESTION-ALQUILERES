package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.PagoRepository;
import com.ejemplo.alquileres.repository.PersonaRepository;
import com.ejemplo.alquileres.service.JlccConfiguracionService;
import com.ejemplo.alquileres.service.JlccNombresMes;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class JlccHistoriaDashboardSeeder {
    private static final int JLCC_CONTRATOS = 10;
    private static final int JLCC_PLAZO_MESES = 12;
    private static final int JLCC_DIA_PAGO = 5;

    private static final int[] JLCC_DIAS_HASTA_FIN = {10, 25, 45, 100, 130, 160, 75, 200, -10, 240};

    private final ContratoRepository jlccContratos;
    private final PagoRepository jlccPagos;
    private final InmuebleRepository jlccInmuebles;
    private final PersonaRepository jlccPersonas;
    private final JlccConfiguracionService jlccConfiguracion;
    private final Clock jlccClock;

    public JlccHistoriaDashboardSeeder(
            ContratoRepository jlccContratos,
            PagoRepository jlccPagos,
            InmuebleRepository jlccInmuebles,
            PersonaRepository jlccPersonas,
            JlccConfiguracionService jlccConfiguracion,
            Clock jlccClock) {
        this.jlccContratos = jlccContratos;
        this.jlccPagos = jlccPagos;
        this.jlccInmuebles = jlccInmuebles;
        this.jlccPersonas = jlccPersonas;
        this.jlccConfiguracion = jlccConfiguracion;
        this.jlccClock = jlccClock;
    }

    @Transactional
    public void jlccCargar() {
        if (jlccContratos.count() > 0) {
            jlccRepararDetallesDemo();
            jlccRepararCronogramasDemo();
            return;
        }

        LocalDate jlccHoy = LocalDate.now(jlccClock);
        List<Inmueble> jlccAlquilados =
                jlccInmuebles.findAll().stream()
                        .filter(i -> i.getEstado() == EstadoInmueble.ALQUILADO)
                        .sorted(
                                Comparator.comparing(
                                                Inmueble::getNivel,
                                                Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(
                                                Inmueble::getOrden,
                                                Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(
                                                Inmueble::getCodigo,
                                                Comparator.nullsLast(String::compareTo)))
                        .limit(JLCC_CONTRATOS)
                        .toList();
        List<Persona> jlccActivas =
                jlccPersonas.findAll().stream().filter(Persona::isActivo).limit(JLCC_CONTRATOS).toList();

        int jlccCantidad = Math.min(jlccAlquilados.size(), jlccActivas.size());
        int jlccNumeroPago = 1;
        for (int n = 0; n < jlccCantidad; n++) {
            Inmueble jlccInmueble = jlccAlquilados.get(n);
            Persona jlccArrendatario = jlccActivas.get(n);
            BigDecimal jlccRenta =
                    jlccDinero(
                            jlccInmueble.getRentaReferencial() == null
                                    ? BigDecimal.valueOf(1000)
                                    : jlccInmueble.getRentaReferencial());
            int jlccGracia = Math.max(0, jlccConfiguracion.jlccMoraDiasGracia());

            LocalDate jlccFinObjetivo = jlccHoy.plusDays(JLCC_DIAS_HASTA_FIN[n]);
            LocalDate jlccInicio = jlccFinObjetivo.plusDays(1).minusMonths(JLCC_PLAZO_MESES);
            LocalDate jlccFin = jlccInicio.plusMonths(JLCC_PLAZO_MESES).minusDays(1);

            Contrato jlccContrato =
                    Contrato.builder()
                            .codigo("CTR-DEV-" + (n + 1))
                            .inmueble(jlccInmueble)
                            .arrendatario(jlccArrendatario)
                            .fechaInicio(jlccInicio)
                            .fechaFin(jlccFin)
                            .diaPago(JLCC_DIA_PAGO)
                            .rentaMensual(jlccRenta)
                            .incrementoAnualPct(BigDecimal.ZERO)
                            .garantiaMonto(jlccRenta)
                            .garantiaEstado(GarantiaEstado.RECIBIDA)
                            .diasGracia(jlccGracia)
                            .tipoMora(TipoMora.PORCENTAJE_DIARIO)
                            .valorMora(new BigDecimal("0.10"))
                            .estado(EstadoContrato.VIGENTE)
                            .fechaRegistro(jlccInicio)
                            .build();

            List<Cuota> jlccCuotas = jlccCrearCuotas(jlccContrato, jlccRenta);
            jlccPrepararEstados(jlccCuotas, n, jlccHoy, jlccGracia);
            jlccContratos.save(jlccContrato);

            for (Cuota jlccCuota : jlccCuotas) {
                if (jlccCuota.getEstado() == EstadoCuota.PAGADA) {
                    Pago jlccPago = jlccCrearPago(jlccContrato, jlccCuota, jlccRenta, jlccNumeroPago++);
                    jlccPagos.save(jlccPago);
                }
            }

            jlccInmueble.setEstado(EstadoInmueble.ALQUILADO);
            jlccInmuebles.save(jlccInmueble);
        }
    }

    private List<Cuota> jlccCrearCuotas(Contrato jlccContrato, BigDecimal jlccRenta) {
        int jlccMeses =
                Math.max(
                        1,
                        (int)
                                ChronoUnit.MONTHS.between(
                                        jlccContrato.getFechaInicio(),
                                        jlccContrato.getFechaFin().plusDays(1)));
        List<Cuota> jlccCuotas = new ArrayList<>(jlccMeses);
        for (int jlccMes = 0; jlccMes < jlccMeses; jlccMes++) {
            LocalDate jlccPeriodo = jlccContrato.getFechaInicio().plusMonths(jlccMes).withDayOfMonth(1);
            LocalDate jlccVencimiento =
                    jlccPeriodo.withDayOfMonth(Math.min(JLCC_DIA_PAGO, jlccPeriodo.lengthOfMonth()));
            if (jlccMes == 0 && jlccVencimiento.isBefore(jlccContrato.getFechaInicio())) {
                jlccVencimiento = jlccContrato.getFechaInicio();
            }
            Cuota jlccCuota =
                    Cuota.builder()
                            .numero(jlccMes + 1)
                            .tipo(TipoCuota.RENTA)
                            .concepto("Renta " + JlccNombresMes.jlccDe(jlccPeriodo) + " " + jlccPeriodo.getYear())
                            .periodo(jlccPeriodo)
                            .fechaVencimiento(jlccVencimiento)
                            .montoCuota(jlccRenta)
                            .montoPagado(BigDecimal.ZERO)
                            .estado(EstadoCuota.PENDIENTE)
                            .moraDesde(jlccVencimiento)
                            .build();
            jlccContrato.agregarCuota(jlccCuota);
            jlccCuotas.add(jlccCuota);
        }
        return jlccCuotas;
    }

    private void jlccPrepararEstados(
            List<Cuota> jlccCuotas, int jlccIndiceContrato, LocalDate jlccHoy, int jlccGracia) {
        if (jlccIndiceContrato < 6) {
            for (Cuota jlccCuota : jlccCuotas) {
                if (jlccCuota.getFechaVencimiento().isBefore(jlccHoy)) jlccPagar(jlccCuota);
            }
            return;
        }

        if (jlccIndiceContrato < 8) {
            int jlccUmbral = Math.max(0, jlccConfiguracion.jlccUmbralLeveDias());
            int jlccObjetivo = jlccUltimaCuotaVencida(jlccCuotas, jlccHoy);
            if (jlccObjetivo < 0) jlccObjetivo = jlccCuotas.size() - 1;
            long jlccDias = ChronoUnit.DAYS.between(jlccCuotas.get(jlccObjetivo).getFechaVencimiento(), jlccHoy);
            if (jlccDias <= jlccGracia || jlccDias > jlccUmbral) {
                int jlccDiasAtraso = Math.max(jlccGracia + 1, Math.min(Math.max(1, jlccUmbral), 10));
                jlccAjustarVencimiento(jlccCuotas.get(jlccObjetivo), jlccHoy.minusDays(jlccDiasAtraso));
            }
            for (int i = 0; i < jlccCuotas.size(); i++) {
                if (i < jlccObjetivo) jlccPagar(jlccCuotas.get(i));
                else jlccPendiente(jlccCuotas.get(i));
            }
            return;
        }

        int jlccCuotasRojo = Math.max(1, jlccConfiguracion.jlccCuotasRojo());
        int jlccObjetivo = Math.min(Math.max(2, jlccCuotasRojo), 6);
        int jlccUltima = jlccUltimaCuotaVencida(jlccCuotas, jlccHoy);
        if (jlccUltima < 0) return;
        int jlccInicio = Math.max(0, jlccUltima - jlccObjetivo + 1);
        for (int i = 0; i < jlccCuotas.size(); i++) {
            if (i < jlccInicio) jlccPagar(jlccCuotas.get(i));
            else jlccPendiente(jlccCuotas.get(i));
        }
    }

    private int jlccUltimaCuotaVencida(List<Cuota> jlccCuotas, LocalDate jlccHoy) {
        for (int i = jlccCuotas.size() - 1; i >= 0; i--) {
            if (jlccCuotas.get(i).getFechaVencimiento().isBefore(jlccHoy)) return i;
        }
        return -1;
    }

    private void jlccAjustarVencimiento(Cuota jlccCuota, LocalDate jlccVencimiento) {
        LocalDate jlccPeriodo = jlccVencimiento.withDayOfMonth(1);
        jlccCuota.setFechaVencimiento(jlccVencimiento);
        jlccCuota.setPeriodo(jlccPeriodo);
        jlccCuota.setMoraDesde(jlccVencimiento);
        jlccCuota.setConcepto("Renta " + JlccNombresMes.jlccDe(jlccPeriodo) + " " + jlccPeriodo.getYear());
    }

    private void jlccPagar(Cuota jlccCuota) {
        jlccCuota.setMontoPagado(jlccDinero(jlccCuota.getMontoCuota()));
        jlccCuota.setEstado(EstadoCuota.PAGADA);
    }

    private void jlccPendiente(Cuota jlccCuota) {
        jlccCuota.setMontoPagado(BigDecimal.ZERO);
        jlccCuota.setEstado(EstadoCuota.PENDIENTE);
    }

    private Pago jlccCrearPago(Contrato jlccContrato, Cuota jlccCuota, BigDecimal jlccRenta, int jlccNumero) {
        LocalDate jlccFechaPago = jlccCuota.getFechaVencimiento().plusDays(1);
        Pago jlccPago =
                Pago.builder()
                        .serie("R001")
                        .numero(jlccNumero)
                        .recibo(
                                "R001-DEV-"
                                        + jlccContrato.getCodigo().substring("CTR-DEV-".length())
                                        + "-"
                                        + jlccCuota.getNumero())
                        .contrato(jlccContrato)
                        .fechaPago(jlccFechaPago)
                        .fechaRegistro(jlccFechaPago.atTime(10, 0))
                        .medioPago(MedioPago.EFECTIVO)
                        .montoCapital(jlccRenta)
                        .montoMora(BigDecimal.ZERO)
                        .montoTotal(jlccRenta)
                        .estado(EstadoPago.EMITIDO)
                        .arrendatarioNombre(jlccContrato.getArrendatario().getNombreCompleto())
                        .arrendatarioDocumento(jlccContrato.getArrendatario().getNumeroDocumento())
                        .inmuebleCodigo(jlccContrato.getInmueble().getCodigo())
                        .inmuebleNombre(jlccContrato.getInmueble().getNombre())
                        .build();
        jlccPago.agregarDetalle(jlccDetalle(jlccPago, jlccCuota, jlccRenta, BigDecimal.ZERO));
        return jlccPago;
    }

    private void jlccRepararDetallesDemo() {
        for (Pago jlccPago : jlccPagos.findAll()) {
            if (jlccPago.getRecibo() == null
                    || !jlccPago.getRecibo().startsWith("R001-DEV-")
                    || !jlccPago.getDetalles().isEmpty()) {
                continue;
            }
            Cuota jlccCuota =
                    jlccPago.getContrato().getCuotas().stream()
                            .filter(q -> q.getTipo() == TipoCuota.RENTA && q.getPeriodo() != null)
                            .filter(
                                    q ->
                                            YearMonth.from(q.getPeriodo())
                                                    .equals(YearMonth.from(jlccPago.getFechaPago())))
                            .findFirst()
                            .orElse(null);
            if (jlccCuota == null) continue;
            jlccPago.agregarDetalle(
                    jlccDetalle(jlccPago, jlccCuota, jlccNvl(jlccPago.getMontoCapital()), jlccNvl(jlccPago.getMontoMora())));
            jlccPagos.save(jlccPago);
        }
    }

    private void jlccRepararCronogramasDemo() {
        for (Contrato jlccContrato : jlccContratos.findAll()) {
            if (jlccContrato.getCodigo() == null || !jlccContrato.getCodigo().startsWith("CTR-DEV-"))
                continue;
            int jlccMeses =
                    (int)
                            ChronoUnit.MONTHS.between(
                                    jlccContrato.getFechaInicio(), jlccContrato.getFechaFin().plusDays(1));
            if (jlccMeses < 1) continue;
            for (int m = 0; m < jlccMeses; m++) {
                int jlccNumero = m + 1;
                LocalDate jlccPeriodo = jlccContrato.getFechaInicio().plusMonths(m).withDayOfMonth(1);
                String jlccConcepto = "Renta " + JlccNombresMes.jlccDe(jlccPeriodo) + " " + jlccPeriodo.getYear();
                Cuota jlccCuota =
                        jlccContrato.getCuotas().stream()
                                .filter(
                                        q ->
                                                q.getTipo() == TipoCuota.RENTA
                                                        && q.getNumero() == jlccNumero)
                                .findFirst()
                                .orElse(null);
                if (jlccCuota == null) {
                    LocalDate jlccVencimiento =
                            jlccPeriodo.withDayOfMonth(
                                    Math.min(jlccContrato.getDiaPago(), jlccPeriodo.lengthOfMonth()));
                    if (m == 0 && jlccVencimiento.isBefore(jlccContrato.getFechaInicio())) {
                        jlccVencimiento = jlccContrato.getFechaInicio();
                    }
                    jlccCuota =
                            Cuota.builder()
                                    .numero(m + 1)
                                    .tipo(TipoCuota.RENTA)
                                    .concepto(jlccConcepto)
                                    .periodo(jlccPeriodo)
                                    .fechaVencimiento(jlccVencimiento)
                                    .montoCuota(jlccContrato.getRentaMensual())
                                    .montoPagado(BigDecimal.ZERO)
                                    .estado(EstadoCuota.PENDIENTE)
                                    .moraDesde(jlccVencimiento)
                                    .build();
                    jlccContrato.agregarCuota(jlccCuota);
                } else {
                    jlccCuota.setConcepto(jlccConcepto);
                    if (jlccCuota.getMoraDesde() == null)
                        jlccCuota.setMoraDesde(jlccCuota.getFechaVencimiento());
                }
            }
            jlccContratos.save(jlccContrato);
        }
    }

    private PagoDetalle jlccDetalle(Pago jlccPago, Cuota jlccCuota, BigDecimal jlccCapital, BigDecimal jlccMora) {
        return PagoDetalle.builder()
                .pago(jlccPago)
                .cuota(jlccCuota)
                .item(1)
                .concepto(jlccCuota.getConcepto())
                .periodo(jlccCuota.getPeriodo())
                .fechaVencimiento(jlccCuota.getFechaVencimiento())
                .montoCapital(jlccCapital)
                .montoMora(jlccMora)
                .montoMoraCondonada(BigDecimal.ZERO)
                .moraAcumuladaAntes(BigDecimal.ZERO)
                .moraDesdeAntes(jlccCuota.getMoraDesde())
                .estadoCuotaAntes(EstadoCuota.PENDIENTE)
                .build();
    }

    private static BigDecimal jlccNvl(BigDecimal jlccValor) {
        return jlccValor == null ? BigDecimal.ZERO : jlccValor;
    }

    private static BigDecimal jlccDinero(BigDecimal jlccValor) {
        return (jlccValor == null ? BigDecimal.ZERO : jlccValor).setScale(2, RoundingMode.HALF_UP);
    }
}

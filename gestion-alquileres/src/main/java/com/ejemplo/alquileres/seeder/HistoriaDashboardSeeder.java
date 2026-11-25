package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.PagoRepository;
import com.ejemplo.alquileres.repository.PersonaRepository;
import com.ejemplo.alquileres.service.ConfiguracionService;
import com.ejemplo.alquileres.service.NombresMes;

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
public class HistoriaDashboardSeeder {
    private static final int CONTRATOS = 10;
    private static final int PLAZO_MESES = 12;
    private static final int DIA_PAGO = 5;

    private static final int[] DIAS_HASTA_FIN = {10, 25, 45, 100, 130, 160, 75, 200, -10, 240};

    private final ContratoRepository contratos;
    private final PagoRepository pagos;
    private final InmuebleRepository inmuebles;
    private final PersonaRepository personas;
    private final ConfiguracionService configuracion;
    private final Clock clock;

    public HistoriaDashboardSeeder(
            ContratoRepository contratos,
            PagoRepository pagos,
            InmuebleRepository inmuebles,
            PersonaRepository personas,
            ConfiguracionService configuracion,
            Clock clock) {
        this.contratos = contratos;
        this.pagos = pagos;
        this.inmuebles = inmuebles;
        this.personas = personas;
        this.configuracion = configuracion;
        this.clock = clock;
    }

    @Transactional
    public void cargar() {
        if (contratos.count() > 0) {
            repararDetallesDemo();
            repararCronogramasDemo();
            return;
        }

        LocalDate hoy = LocalDate.now(clock);
        List<Inmueble> alquilados =
                inmuebles.findAll().stream()
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
                        .limit(CONTRATOS)
                        .toList();
        List<Persona> activas =
                personas.findAll().stream().filter(Persona::isActivo).limit(CONTRATOS).toList();

        int cantidad = Math.min(alquilados.size(), activas.size());
        int numeroPago = 1;
        for (int n = 0; n < cantidad; n++) {
            Inmueble inmueble = alquilados.get(n);
            Persona arrendatario = activas.get(n);
            BigDecimal renta =
                    dinero(
                            inmueble.getRentaReferencial() == null
                                    ? BigDecimal.valueOf(1000)
                                    : inmueble.getRentaReferencial());
            int gracia = Math.max(0, configuracion.moraDiasGracia());

            LocalDate finObjetivo = hoy.plusDays(DIAS_HASTA_FIN[n]);
            LocalDate inicio = finObjetivo.plusDays(1).minusMonths(PLAZO_MESES);
            LocalDate fin = inicio.plusMonths(PLAZO_MESES).minusDays(1);

            Contrato contrato =
                    Contrato.builder()
                            .codigo("CTR-DEV-" + (n + 1))
                            .inmueble(inmueble)
                            .arrendatario(arrendatario)
                            .fechaInicio(inicio)
                            .fechaFin(fin)
                            .diaPago(DIA_PAGO)
                            .rentaMensual(renta)
                            .incrementoAnualPct(BigDecimal.ZERO)
                            .garantiaMonto(renta)
                            .garantiaEstado(GarantiaEstado.RECIBIDA)
                            .diasGracia(gracia)
                            .tipoMora(TipoMora.PORCENTAJE_DIARIO)
                            .valorMora(new BigDecimal("0.10"))
                            .estado(EstadoContrato.VIGENTE)
                            .fechaRegistro(inicio)
                            .build();

            List<Cuota> cuotas = crearCuotas(contrato, renta);
            prepararEstados(cuotas, n, hoy, gracia);
            contratos.save(contrato);

            for (Cuota cuota : cuotas) {
                if (cuota.getEstado() == EstadoCuota.PAGADA) {
                    Pago pago = crearPago(contrato, cuota, renta, numeroPago++);
                    pagos.save(pago);
                }
            }

            inmueble.setEstado(EstadoInmueble.ALQUILADO);
            inmuebles.save(inmueble);
        }
    }

    private List<Cuota> crearCuotas(Contrato contrato, BigDecimal renta) {
        int meses =
                Math.max(
                        1,
                        (int)
                                ChronoUnit.MONTHS.between(
                                        contrato.getFechaInicio(),
                                        contrato.getFechaFin().plusDays(1)));
        List<Cuota> cuotas = new ArrayList<>(meses);
        for (int mes = 0; mes < meses; mes++) {
            LocalDate periodo = contrato.getFechaInicio().plusMonths(mes).withDayOfMonth(1);
            LocalDate vencimiento =
                    periodo.withDayOfMonth(Math.min(DIA_PAGO, periodo.lengthOfMonth()));
            if (mes == 0 && vencimiento.isBefore(contrato.getFechaInicio())) {
                vencimiento = contrato.getFechaInicio();
            }
            Cuota cuota =
                    Cuota.builder()
                            .numero(mes + 1)
                            .tipo(TipoCuota.RENTA)
                            .concepto("Renta " + NombresMes.de(periodo) + " " + periodo.getYear())
                            .periodo(periodo)
                            .fechaVencimiento(vencimiento)
                            .montoCuota(renta)
                            .montoPagado(BigDecimal.ZERO)
                            .estado(EstadoCuota.PENDIENTE)
                            .moraDesde(vencimiento)
                            .build();
            contrato.agregarCuota(cuota);
            cuotas.add(cuota);
        }
        return cuotas;
    }

    private void prepararEstados(
            List<Cuota> cuotas, int indiceContrato, LocalDate hoy, int gracia) {
        if (indiceContrato < 6) {
            for (Cuota cuota : cuotas) {
                if (cuota.getFechaVencimiento().isBefore(hoy)) pagar(cuota);
            }
            return;
        }

        if (indiceContrato < 8) {
            int umbral = Math.max(0, configuracion.umbralLeveDias());
            int objetivo = ultimaCuotaVencida(cuotas, hoy);
            if (objetivo < 0) objetivo = cuotas.size() - 1;
            long dias = ChronoUnit.DAYS.between(cuotas.get(objetivo).getFechaVencimiento(), hoy);
            if (dias <= gracia || dias > umbral) {
                int diasAtraso = Math.max(gracia + 1, Math.min(Math.max(1, umbral), 10));
                ajustarVencimiento(cuotas.get(objetivo), hoy.minusDays(diasAtraso));
            }
            for (int i = 0; i < cuotas.size(); i++) {
                if (i < objetivo) pagar(cuotas.get(i));
                else pendiente(cuotas.get(i));
            }
            return;
        }

        int cuotasRojo = Math.max(1, configuracion.cuotasRojo());
        int objetivo = Math.min(Math.max(2, cuotasRojo), 6);
        int ultima = ultimaCuotaVencida(cuotas, hoy);
        if (ultima < 0) return;
        int inicio = Math.max(0, ultima - objetivo + 1);
        for (int i = 0; i < cuotas.size(); i++) {
            if (i < inicio) pagar(cuotas.get(i));
            else pendiente(cuotas.get(i));
        }
    }

    private int ultimaCuotaVencida(List<Cuota> cuotas, LocalDate hoy) {
        for (int i = cuotas.size() - 1; i >= 0; i--) {
            if (cuotas.get(i).getFechaVencimiento().isBefore(hoy)) return i;
        }
        return -1;
    }

    private void ajustarVencimiento(Cuota cuota, LocalDate vencimiento) {
        LocalDate periodo = vencimiento.withDayOfMonth(1);
        cuota.setFechaVencimiento(vencimiento);
        cuota.setPeriodo(periodo);
        cuota.setMoraDesde(vencimiento);
        cuota.setConcepto("Renta " + NombresMes.de(periodo) + " " + periodo.getYear());
    }

    private void pagar(Cuota cuota) {
        cuota.setMontoPagado(dinero(cuota.getMontoCuota()));
        cuota.setEstado(EstadoCuota.PAGADA);
    }

    private void pendiente(Cuota cuota) {
        cuota.setMontoPagado(BigDecimal.ZERO);
        cuota.setEstado(EstadoCuota.PENDIENTE);
    }

    private Pago crearPago(Contrato contrato, Cuota cuota, BigDecimal renta, int numero) {
        LocalDate fechaPago = cuota.getFechaVencimiento().plusDays(1);
        Pago pago =
                Pago.builder()
                        .serie("R001")
                        .numero(numero)
                        .recibo(
                                "R001-DEV-"
                                        + contrato.getCodigo().substring("CTR-DEV-".length())
                                        + "-"
                                        + cuota.getNumero())
                        .contrato(contrato)
                        .fechaPago(fechaPago)
                        .fechaRegistro(fechaPago.atTime(10, 0))
                        .medioPago(MedioPago.EFECTIVO)
                        .montoCapital(renta)
                        .montoMora(BigDecimal.ZERO)
                        .montoTotal(renta)
                        .estado(EstadoPago.EMITIDO)
                        .arrendatarioNombre(contrato.getArrendatario().getNombreCompleto())
                        .arrendatarioDocumento(contrato.getArrendatario().getNumeroDocumento())
                        .inmuebleCodigo(contrato.getInmueble().getCodigo())
                        .inmuebleNombre(contrato.getInmueble().getNombre())
                        .build();
        pago.agregarDetalle(detalle(pago, cuota, renta, BigDecimal.ZERO));
        return pago;
    }

    private void repararDetallesDemo() {
        for (Pago pago : pagos.findAll()) {
            if (pago.getRecibo() == null
                    || !pago.getRecibo().startsWith("R001-DEV-")
                    || !pago.getDetalles().isEmpty()) {
                continue;
            }
            Cuota cuota =
                    pago.getContrato().getCuotas().stream()
                            .filter(q -> q.getTipo() == TipoCuota.RENTA && q.getPeriodo() != null)
                            .filter(
                                    q ->
                                            YearMonth.from(q.getPeriodo())
                                                    .equals(YearMonth.from(pago.getFechaPago())))
                            .findFirst()
                            .orElse(null);
            if (cuota == null) continue;
            pago.agregarDetalle(
                    detalle(pago, cuota, nvl(pago.getMontoCapital()), nvl(pago.getMontoMora())));
            pagos.save(pago);
        }
    }

    private void repararCronogramasDemo() {
        for (Contrato contrato : contratos.findAll()) {
            if (contrato.getCodigo() == null || !contrato.getCodigo().startsWith("CTR-DEV-"))
                continue;
            int meses =
                    (int)
                            ChronoUnit.MONTHS.between(
                                    contrato.getFechaInicio(), contrato.getFechaFin().plusDays(1));
            if (meses < 1) continue;
            for (int m = 0; m < meses; m++) {
                int numero = m + 1;
                LocalDate periodo = contrato.getFechaInicio().plusMonths(m).withDayOfMonth(1);
                String concepto = "Renta " + NombresMes.de(periodo) + " " + periodo.getYear();
                Cuota cuota =
                        contrato.getCuotas().stream()
                                .filter(
                                        q ->
                                                q.getTipo() == TipoCuota.RENTA
                                                        && q.getNumero() == numero)
                                .findFirst()
                                .orElse(null);
                if (cuota == null) {
                    LocalDate vencimiento =
                            periodo.withDayOfMonth(
                                    Math.min(contrato.getDiaPago(), periodo.lengthOfMonth()));
                    if (m == 0 && vencimiento.isBefore(contrato.getFechaInicio())) {
                        vencimiento = contrato.getFechaInicio();
                    }
                    cuota =
                            Cuota.builder()
                                    .numero(m + 1)
                                    .tipo(TipoCuota.RENTA)
                                    .concepto(concepto)
                                    .periodo(periodo)
                                    .fechaVencimiento(vencimiento)
                                    .montoCuota(contrato.getRentaMensual())
                                    .montoPagado(BigDecimal.ZERO)
                                    .estado(EstadoCuota.PENDIENTE)
                                    .moraDesde(vencimiento)
                                    .build();
                    contrato.agregarCuota(cuota);
                } else {
                    cuota.setConcepto(concepto);
                    if (cuota.getMoraDesde() == null)
                        cuota.setMoraDesde(cuota.getFechaVencimiento());
                }
            }
            contratos.save(contrato);
        }
    }

    private PagoDetalle detalle(Pago pago, Cuota cuota, BigDecimal capital, BigDecimal mora) {
        return PagoDetalle.builder()
                .pago(pago)
                .cuota(cuota)
                .item(1)
                .concepto(cuota.getConcepto())
                .periodo(cuota.getPeriodo())
                .fechaVencimiento(cuota.getFechaVencimiento())
                .montoCapital(capital)
                .montoMora(mora)
                .montoMoraCondonada(BigDecimal.ZERO)
                .moraAcumuladaAntes(BigDecimal.ZERO)
                .moraDesdeAntes(cuota.getMoraDesde())
                .estadoCuotaAntes(EstadoCuota.PENDIENTE)
                .build();
    }

    private static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}

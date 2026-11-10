package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Datos idempotentes del perfil dev: seis meses de cobros para el dashboard. */
@Component
public class HistoriaDashboardSeeder {
    private final ContratoRepository contratos;
    private final PagoRepository pagos;
    private final InmuebleRepository inmuebles;
    private final PersonaRepository personas;
    private final Clock clock;

    public HistoriaDashboardSeeder(ContratoRepository contratos, PagoRepository pagos,
                                   InmuebleRepository inmuebles, PersonaRepository personas, Clock clock) {
        this.contratos = contratos;
        this.pagos = pagos;
        this.inmuebles = inmuebles;
        this.personas = personas;
        this.clock = clock;
    }

    @Transactional
    public void cargar() {
        if (contratos.count() > 0) {
            repararDetallesDemo();
            return;
        }
        List<Inmueble> disponibles = inmuebles.findAll().stream()
                .filter(i -> i.getEstado() == EstadoInmueble.DISPONIBLE).limit(4).toList();
        List<Persona> activas = personas.findAll().stream()
                .filter(Persona::isActivo).limit(4).toList();
        LocalDate hoy = LocalDate.now(clock);
        for (int n = 0; n < Math.min(disponibles.size(), activas.size()); n++) {
            Inmueble inmueble = disponibles.get(n);
            Persona arrendatario = activas.get(n);
            BigDecimal renta = BigDecimal.valueOf(900L + n * 350L).setScale(2);
            Contrato contrato = Contrato.builder()
                    .codigo("CTR-DEV-" + (n + 1))
                    .inmueble(inmueble)
                    .arrendatario(arrendatario)
                    .fechaInicio(hoy.minusMonths(6).withDayOfMonth(1))
                    .fechaFin(hoy.plusMonths(n == 0 ? 1 : 6)
                            .withDayOfMonth(hoy.plusMonths(n == 0 ? 1 : 6).lengthOfMonth()))
                    .diaPago(5)
                    .rentaMensual(renta)
                    .diasGracia(3)
                    .tipoMora(TipoMora.PORCENTAJE_DIARIO)
                    .valorMora(new BigDecimal("0.10"))
                    .estado(EstadoContrato.VIGENTE)
                    .fechaRegistro(hoy.minusMonths(6))
                    .build();
            List<Cuota> cuotas = new ArrayList<>();
            for (int m = 0; m < 8; m++) {
                LocalDate vencimiento = contrato.getFechaInicio().plusMonths(m).withDayOfMonth(5);
                Cuota cuota = Cuota.builder()
                        .numero(m + 1)
                        .tipo(TipoCuota.RENTA)
                        .concepto("Alquiler " + vencimiento.getMonth())
                        .periodo(vencimiento.withDayOfMonth(1))
                        .fechaVencimiento(vencimiento)
                        .montoCuota(renta)
                        .montoPagado(m < 6 ? renta : BigDecimal.ZERO)
                        .estado(m < 6 ? EstadoCuota.PAGADA : EstadoCuota.PENDIENTE)
                        .build();
                contrato.agregarCuota(cuota);
                cuotas.add(cuota);
            }
            contratos.save(contrato);
            inmueble.setEstado(EstadoInmueble.ALQUILADO);
            inmuebles.save(inmueble);
            for (int m = 0; m < 6; m++) {
                LocalDate fechaPago = contrato.getFechaInicio().plusMonths(m)
                        .withDayOfMonth(Math.min(5 + m % 3, fDate(contrato.getFechaInicio().plusMonths(m))));
                Pago pago = Pago.builder()
                        .serie("R001")
                        .numero((n + 1) * 100 + m)
                        .recibo("R001-DEV-" + (n + 1) + "-" + (m + 1))
                        .contrato(contrato)
                        .fechaPago(fechaPago)
                        .fechaRegistro(fechaPago.atTime(10, 0))
                        .medioPago(MedioPago.EFECTIVO)
                        .montoCapital(renta)
                        .montoMora(BigDecimal.ZERO)
                        .montoTotal(renta)
                        .estado(EstadoPago.EMITIDO)
                        .arrendatarioNombre(arrendatario.getNombreCompleto())
                        .arrendatarioDocumento(arrendatario.getNumeroDocumento())
                        .inmuebleCodigo(inmueble.getCodigo())
                        .inmuebleNombre(inmueble.getNombre())
                        .build();
                pago.agregarDetalle(detalle(pago, cuotas.get(m), renta, BigDecimal.ZERO));
                pagos.save(pago);
            }
        }
    }

    /**
     * Los respaldos creados antes de esta corrección no tenían detalle de pago.
     * Solo se reparan los recibos demo, nunca los pagos reales del usuario.
     */
    private void repararDetallesDemo() {
        for (Pago pago : pagos.findAll()) {
            if (pago.getRecibo() == null || !pago.getRecibo().startsWith("R001-DEV-")
                    || !pago.getDetalles().isEmpty()) {
                continue;
            }
            Cuota cuota = pago.getContrato().getCuotas().stream()
                    .filter(q -> q.getTipo() == TipoCuota.RENTA && q.getPeriodo() != null)
                    .filter(q -> YearMonth.from(q.getPeriodo()).equals(YearMonth.from(pago.getFechaPago())))
                    .findFirst().orElse(null);
            if (cuota == null) continue;
            pago.agregarDetalle(detalle(pago, cuota, nvl(pago.getMontoCapital()), nvl(pago.getMontoMora())));
            pagos.save(pago);
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

    private int fDate(LocalDate fecha) {
        return fecha.lengthOfMonth();
    }
}

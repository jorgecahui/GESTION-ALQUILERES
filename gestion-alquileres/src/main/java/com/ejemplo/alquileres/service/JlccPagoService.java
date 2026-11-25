package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JlccPagoService {
    private final PagoRepository jlccPagos;
    private final CuotaRepository jlccCuotas;
    private final ContratoRepository jlccContratos;
    private final JlccSecuenciaService jlccSecuencias;
    private final JlccCalculadoraMora jlccCalculadora;
    private final JlccConfiguracionService jlccConfiguracion;
    private final JlccPermisos jlccPermisos;
    private final JlccAuditoriaService jlccAuditoria;
    private final Clock jlccClock;

    public record Linea(
            Long cuotaId,
            BigDecimal capital,
            BigDecimal mora,
            BigDecimal condonada,
            String motivo) {}

    public record Solicitud(
            Long contratoId,
            LocalDate fecha,
            MedioPago medio,
            String referencia,
            String observacion,
            List<Linea> lineas,
            boolean permitirAdelantos) {}

    public record Imputacion(
            Long cuotaId,
            String concepto,
            LocalDate vencimiento,
            BigDecimal capital,
            BigDecimal mora,
            BigDecimal moraCondonada,
            String motivoCondonacion) {}

    @Transactional
    public Pago jlccRegistrar(Solicitud jlccSolicitud) {
        jlccPermisos.jlccExigirGestion();
        if (jlccSolicitud.lineas() != null
                && jlccSolicitud.lineas().stream()
                        .anyMatch(
                                l ->
                                        l != null
                                                && l.condonada() != null
                                                && l.condonada().signum() > 0)) {
            jlccPermisos.jlccExigirAdmin();
        }
        Contrato jlccContrato =
                jlccContratos
                        .detalle(jlccSolicitud.contratoId())
                        .orElseThrow(() -> new JlccNegocioException("Contrato inexistente"));
        jlccValidarSolicitud(jlccSolicitud, jlccContrato);
        String jlccSerie = jlccConfiguracion.jlccSerieRecibos();
        long jlccNumero = jlccSecuencias.jlccSiguiente("RECIBO-" + jlccSerie);
        Pago jlccPago =
                Pago.builder()
                        .serie(jlccSerie)
                        .numero(Math.toIntExact(jlccNumero))
                        .recibo(jlccSecuencias.jlccFormatear(jlccSerie + "-", jlccNumero, 8))
                        .contrato(jlccContrato)
                        .fechaPago(jlccSolicitud.fecha())
                        .fechaRegistro(LocalDateTime.now(jlccClock))
                        .medioPago(jlccSolicitud.medio())
                        .referencia(jlccSolicitud.referencia())
                        .observacion(jlccSolicitud.observacion())
                        .estado(EstadoPago.EMITIDO)
                        .arrendatarioNombre(jlccContrato.getArrendatario().getNombreCompleto())
                        .arrendatarioDocumento(jlccContrato.getArrendatario().getNumeroDocumento())
                        .inmuebleCodigo(jlccContrato.getInmueble().getCodigo())
                        .inmuebleNombre(jlccContrato.getInmueble().getNombre())
                        .montoCapital(jlccCero())
                        .montoMora(jlccCero())
                        .build();
        Set<Long> jlccUsadas = new HashSet<>();
        int jlccItem = 0;
        for (Linea jlccLinea : jlccSolicitud.lineas())
            jlccAplicarLinea(
                    jlccPago,
                    jlccContrato,
                    jlccSolicitud.fecha(),
                    jlccSolicitud.permitirAdelantos(),
                    jlccLinea,
                    ++jlccItem,
                    jlccUsadas);
        jlccPago.setMontoCapital(jlccDinero(jlccPago.getMontoCapital()));
        jlccPago.setMontoMora(jlccDinero(jlccPago.getMontoMora()));
        jlccPago.setMontoTotal(jlccDinero(jlccPago.getMontoCapital().add(jlccPago.getMontoMora())));
        Pago jlccGuardado = jlccPagos.save(jlccPago);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CREACION_PAGO,
                "Pago",
                jlccGuardado.getId(),
                "Recibo " + jlccGuardado.getRecibo());
        BigDecimal jlccCondonada =
                jlccGuardado.getDetalles().stream()
                        .map(PagoDetalle::getMontoMoraCondonada)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (jlccCondonada.signum() > 0) {
            jlccAuditoria.jlccRegistrar(
                    AccionAuditoria.CONDONACION_MORA,
                    "Pago",
                    jlccGuardado.getId(),
                    "Recibo "
                            + jlccGuardado.getRecibo()
                            + " · mora condonada "
                            + JlccPlantillasMensaje.jlccSoles(jlccCondonada));
        }
        return jlccGuardado;
    }

    public List<Imputacion> jlccProponerImputacion(
            Long jlccContratoId, BigDecimal jlccRecibido, LocalDate jlccFechaPago) {
        return jlccProponerImputacion(jlccContratoId, jlccRecibido, jlccFechaPago, false, false, null);
    }

    public List<Imputacion> jlccProponerImputacion(
            Long jlccContratoId, BigDecimal jlccRecibido, LocalDate jlccFechaPago, boolean jlccPermitirAdelantos) {
        return jlccProponerImputacion(jlccContratoId, jlccRecibido, jlccFechaPago, jlccPermitirAdelantos, false, null);
    }

    public List<Imputacion> jlccProponerImputacion(
            Long jlccContratoId,
            BigDecimal jlccRecibido,
            LocalDate jlccFechaPago,
            boolean jlccPermitirAdelantos,
            boolean jlccCondonarMora,
            String jlccMotivoCondonacion) {
        if (jlccRecibido == null || jlccRecibido.signum() <= 0)
            throw new JlccNegocioException("El monto recibido debe ser mayor que cero");
        if (jlccFechaPago == null) throw new JlccNegocioException("Indica la fecha de pago");
        if (jlccCondonarMora) jlccPermisos.jlccExigirAdmin();
        if (jlccCondonarMora && (jlccMotivoCondonacion == null || jlccMotivoCondonacion.isBlank())) {
            throw new JlccNegocioException("Indica el motivo de la condonación");
        }
        Contrato jlccContrato =
                jlccContratos
                        .detalle(jlccContratoId)
                        .orElseThrow(() -> new JlccNegocioException("Contrato inexistente"));
        BigDecimal jlccRestante = jlccDinero(jlccRecibido);
        List<Imputacion> jlccResultado = new ArrayList<>();
        for (Cuota jlccCuota : jlccCuotas.findByContratoIdOrderByFechaVencimientoAsc(jlccContratoId)) {
            if (jlccRestante.signum() == 0
                    || jlccCuota.getEstado() == EstadoCuota.ANULADA
                    || jlccCuota.getEstado() == EstadoCuota.PAGADA
                    || (!jlccPermitirAdelantos
                            && jlccCuota.getFechaVencimiento()
                                    .isAfter(jlccFechaPago.withDayOfMonth(jlccFechaPago.lengthOfMonth()))))
                continue;
            BigDecimal jlccMoraPendiente = jlccCalculadora.jlccPendiente(jlccCuota, jlccContrato, jlccFechaPago);
            BigDecimal jlccCondonada = jlccCondonarMora ? jlccMoraPendiente : BigDecimal.ZERO;
            BigDecimal jlccMora = jlccCondonarMora ? BigDecimal.ZERO : jlccMoraPendiente.min(jlccRestante);
            jlccRestante = jlccRestante.subtract(jlccMora);
            BigDecimal jlccCapital = jlccCalculadora.jlccSaldoCapital(jlccCuota).min(jlccRestante);
            jlccRestante = jlccRestante.subtract(jlccCapital);
            if (jlccMora.signum() > 0 || jlccCapital.signum() > 0) {
                jlccResultado.add(
                        new Imputacion(
                                jlccCuota.getId(),
                                jlccCuota.getConcepto(),
                                jlccCuota.getFechaVencimiento(),
                                jlccDinero(jlccCapital),
                                jlccDinero(jlccMora),
                                jlccDinero(jlccCondonada),
                                jlccCondonarMora ? jlccMotivoCondonacion.trim() : null));
            }
        }
        return jlccResultado;
    }

    @Transactional
    public void jlccAnular(Long jlccId, String jlccMotivo) {
        jlccPermisos.jlccExigirAdmin();
        if (jlccMotivo == null || jlccMotivo.isBlank())
            throw new JlccNegocioException("Indica el motivo de anulación");
        Pago jlccPago = jlccObtener(jlccId);
        if (jlccPago.getEstado() != EstadoPago.EMITIDO)
            throw new JlccNegocioException("El pago ya está anulado");
        for (PagoDetalle jlccDetalle : jlccPago.getDetalles()) {
            Pago jlccPosterior =
                    jlccPagos.activosDeCuota(jlccDetalle.getCuota().getId()).stream()
                            .filter(otro -> !otro.getId().equals(jlccPago.getId()))
                            .filter(
                                    otro ->
                                            otro.getFechaRegistro().isAfter(jlccPago.getFechaRegistro())
                                                    || (otro.getFechaRegistro()
                                                                    .equals(jlccPago.getFechaRegistro())
                                                            && otro.getId() > jlccPago.getId()))
                            .findFirst()
                            .orElse(null);
            if (jlccPosterior != null)
                throw new JlccNegocioException(
                        "Anule primero el pago posterior " + jlccPosterior.getRecibo());
            Cuota jlccCuota = jlccDetalle.getCuota();
            jlccCuota.setMontoPagado(jlccCuota.getMontoPagado().subtract(jlccDetalle.getMontoCapital()));
            jlccCuota.setMoraCobrada(jlccCuota.getMoraCobrada().subtract(jlccDetalle.getMontoMora()));
            jlccCuota.setMoraCondonada(
                    jlccCuota.getMoraCondonada().subtract(jlccDetalle.getMontoMoraCondonada()));
            jlccCuota.setMoraAcumulada(jlccDetalle.getMoraAcumuladaAntes());
            jlccCuota.setMoraDesde(jlccDetalle.getMoraDesdeAntes());
            jlccCuota.setEstado(jlccDetalle.getEstadoCuotaAntes());
        }
        jlccPago.setEstado(EstadoPago.ANULADO);
        jlccPago.setMotivoAnulacion(jlccMotivo.trim());
        jlccPago.setFechaAnulacion(LocalDateTime.now(jlccClock));
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.ANULACION_PAGO, "Pago", jlccPago.getId(), "Recibo " + jlccPago.getRecibo());
    }

    public Pago jlccObtener(Long jlccId) {
        return jlccPagos.detalle(jlccId).orElseThrow(() -> new JlccNegocioException("Pago inexistente"));
    }

    @Transactional(readOnly = true)
    public List<Contrato> jlccContratosCobrables() {
        return jlccContratos.cobrables();
    }

    public List<Pago> jlccListar(LocalDate jlccDesde, LocalDate jlccHasta) {
        return jlccPagos.buscar(jlccDesde, jlccHasta);
    }

    private void jlccAplicarLinea(
            Pago jlccPago,
            Contrato jlccContrato,
            LocalDate jlccFecha,
            boolean jlccPermitirAdelantos,
            Linea jlccLinea,
            int jlccItem,
            Set<Long> jlccUsadas) {
        if (jlccLinea == null || jlccLinea.cuotaId() == null || !jlccUsadas.add(jlccLinea.cuotaId()))
            throw new JlccNegocioException("Cada cuota solo puede figurar una vez en el pago");
        Cuota jlccCuota =
                jlccCuotas.findById(jlccLinea.cuotaId())
                        .orElseThrow(() -> new JlccNegocioException("Cuota inexistente"));
        if (!jlccCuota.getContrato().getId().equals(jlccContrato.getId())
                || jlccCuota.getEstado() == EstadoCuota.ANULADA
                || jlccCuota.getEstado() == EstadoCuota.PAGADA)
            throw new JlccNegocioException("La cuota no está disponible para cobro");
        if (!jlccPermitirAdelantos
                && jlccCuota.getFechaVencimiento().isAfter(jlccFecha.withDayOfMonth(jlccFecha.lengthOfMonth())))
            throw new JlccNegocioException("Activa Permitir adelantos para cobrar cuotas futuras");
        BigDecimal jlccCapital = jlccDinero(jlccLinea.capital()),
                jlccMora = jlccDinero(jlccLinea.mora()),
                jlccCondonada = jlccDinero(jlccLinea.condonada());
        if (jlccCapital.signum() < 0
                || jlccMora.signum() < 0
                || jlccCondonada.signum() < 0
                || jlccCapital.add(jlccMora).signum() <= 0)
            throw new JlccNegocioException("Los importes del pago deben ser positivos");
        BigDecimal jlccSaldo = jlccCalculadora.jlccSaldoCapital(jlccCuota),
                jlccMoraPendiente = jlccCalculadora.jlccPendiente(jlccCuota, jlccContrato, jlccFecha),
                jlccMoraTotal = jlccCalculadora.jlccTotalDevengado(jlccCuota, jlccContrato, jlccFecha);
        if (jlccCapital.compareTo(jlccSaldo) > 0
                || jlccMora.compareTo(jlccMoraPendiente) > 0
                || jlccCondonada.compareTo(jlccMoraPendiente.subtract(jlccMora)) > 0)
            throw new JlccNegocioException("El importe excede el saldo de la cuota");
        if (jlccCondonada.signum() > 0 && (jlccLinea.motivo() == null || jlccLinea.motivo().isBlank()))
            throw new JlccNegocioException("Indica el motivo de la condonación");
        if (jlccCapital.signum() > 0 && jlccMora.add(jlccCondonada).compareTo(jlccMoraPendiente) < 0)
            throw new JlccNegocioException(
                    "La mora pendiente debe pagarse o condonarse antes de aplicar capital");
        jlccPago.agregarDetalle(
                PagoDetalle.builder()
                        .cuota(jlccCuota)
                        .item(jlccItem)
                        .concepto(jlccCuota.getConcepto())
                        .periodo(jlccCuota.getPeriodo())
                        .fechaVencimiento(jlccCuota.getFechaVencimiento())
                        .montoCapital(jlccCapital)
                        .montoMora(jlccMora)
                        .montoMoraCondonada(jlccCondonada)
                        .motivoCondonacion(jlccLinea.motivo())
                        .moraAcumuladaAntes(jlccCuota.getMoraAcumulada())
                        .moraDesdeAntes(jlccCuota.getMoraDesde())
                        .estadoCuotaAntes(jlccCuota.getEstado())
                        .build());
        jlccCuota.setMoraAcumulada(jlccMoraTotal);
        jlccCuota.setMoraCobrada(jlccCuota.getMoraCobrada().add(jlccMora));
        jlccCuota.setMoraCondonada(jlccCuota.getMoraCondonada().add(jlccCondonada));
        jlccCuota.setMontoPagado(jlccCuota.getMontoPagado().add(jlccCapital));
        if (jlccCuota.getMontoPagado().compareTo(jlccCuota.getMontoCuota()) == 0)
            jlccCuota.setEstado(EstadoCuota.PAGADA);
        else if (jlccCuota.getMontoPagado().signum() > 0) jlccCuota.setEstado(EstadoCuota.PARCIAL);
        if (jlccCapital.signum() > 0) jlccCuota.setMoraDesde(jlccFecha);
        jlccPago.setMontoCapital(jlccPago.getMontoCapital().add(jlccCapital));
        jlccPago.setMontoMora(jlccPago.getMontoMora().add(jlccMora));
    }

    private void jlccValidarSolicitud(Solicitud s, Contrato c) {
        if (s.fecha() == null
                || s.fecha().isAfter(LocalDate.now(jlccClock))
                || s.fecha().isBefore(c.getFechaInicio()))
            throw new JlccNegocioException("La fecha de pago no es válida");
        if (s.medio() == null) throw new JlccNegocioException("Selecciona el medio de pago");
        if (s.lineas() == null || s.lineas().isEmpty())
            throw new JlccNegocioException("Agrega al menos una cuota");
    }

    private static BigDecimal jlccCero() {
        return new BigDecimal("0.00");
    }

    private static BigDecimal jlccDinero(BigDecimal jlccValor) {
        return (jlccValor == null ? BigDecimal.ZERO : jlccValor).setScale(2, RoundingMode.HALF_UP);
    }
}

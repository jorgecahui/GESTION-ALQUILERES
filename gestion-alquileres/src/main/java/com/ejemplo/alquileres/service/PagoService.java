package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordina la imputación y la reversión transaccional de los recibos de alquiler. */
@Service @RequiredArgsConstructor
public class PagoService {
    private final PagoRepository pagos; private final CuotaRepository cuotas; private final ContratoRepository contratos;
    private final SecuenciaService secuencias; private final CalculadoraMora calculadora; private final ConfiguracionService configuracion;
    private final Permisos permisos; private final AuditoriaService auditoria; private final Clock clock;
    public record Linea(Long cuotaId, BigDecimal capital, BigDecimal mora, BigDecimal condonada, String motivo) { }
    public record Solicitud(Long contratoId, LocalDate fecha, MedioPago medio, String referencia, String observacion, List<Linea> lineas, boolean permitirAdelantos) { }
    public record Imputacion(Long cuotaId, String concepto, LocalDate vencimiento, BigDecimal capital,
                             BigDecimal mora, BigDecimal moraCondonada, String motivoCondonacion) { }

    @Transactional
    public Pago registrar(Solicitud solicitud) {
        permisos.exigirGestion();
        if (solicitud.lineas() != null && solicitud.lineas().stream()
                .anyMatch(l -> l != null && l.condonada() != null && l.condonada().signum() > 0)) {
            permisos.exigirAdmin();
        }
        Contrato contrato = contratos.detalle(solicitud.contratoId()).orElseThrow(() -> new NegocioException("Contrato inexistente"));
        validarSolicitud(solicitud, contrato);
        String serie = configuracion.serieRecibos(); long numero = secuencias.siguiente("RECIBO-" + serie);
        Pago pago = Pago.builder().serie(serie).numero(Math.toIntExact(numero)).recibo(secuencias.formatear(serie + "-", numero, 8))
                .contrato(contrato).fechaPago(solicitud.fecha()).fechaRegistro(LocalDateTime.now(clock)).medioPago(solicitud.medio())
                .referencia(solicitud.referencia()).observacion(solicitud.observacion()).estado(EstadoPago.EMITIDO)
                .arrendatarioNombre(contrato.getArrendatario().getNombreCompleto()).arrendatarioDocumento(contrato.getArrendatario().getNumeroDocumento())
                .inmuebleCodigo(contrato.getInmueble().getCodigo()).inmuebleNombre(contrato.getInmueble().getNombre())
                .montoCapital(cero()).montoMora(cero()).build();
        Set<Long> usadas = new HashSet<>(); int item = 0;
        for (Linea linea : solicitud.lineas()) aplicarLinea(pago, contrato, solicitud.fecha(), solicitud.permitirAdelantos(), linea, ++item, usadas);
        pago.setMontoCapital(dinero(pago.getMontoCapital())); pago.setMontoMora(dinero(pago.getMontoMora()));
        pago.setMontoTotal(dinero(pago.getMontoCapital().add(pago.getMontoMora())));
        Pago guardado = pagos.save(pago);
        auditoria.registrar(AccionAuditoria.CREACION_PAGO, "Pago", guardado.getId(), "Recibo " + guardado.getRecibo());
        BigDecimal condonada = guardado.getDetalles().stream().map(PagoDetalle::getMontoMoraCondonada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (condonada.signum() > 0) {
            auditoria.registrar(AccionAuditoria.CONDONACION_MORA, "Pago", guardado.getId(),
                    "Recibo " + guardado.getRecibo() + " · mora condonada " + PlantillasMensaje.soles(condonada));
        }
        return guardado;
    }

    public List<Imputacion> proponerImputacion(Long contratoId, BigDecimal recibido, LocalDate fechaPago) {
        return proponerImputacion(contratoId, recibido, fechaPago, false, false, null);
    }
    public List<Imputacion> proponerImputacion(Long contratoId, BigDecimal recibido, LocalDate fechaPago, boolean permitirAdelantos) {
        return proponerImputacion(contratoId, recibido, fechaPago, permitirAdelantos, false, null);
    }
    public List<Imputacion> proponerImputacion(Long contratoId, BigDecimal recibido, LocalDate fechaPago,
                                                boolean permitirAdelantos, boolean condonarMora, String motivoCondonacion) {
        if (recibido == null || recibido.signum() <= 0) throw new NegocioException("El monto recibido debe ser mayor que cero");
        if (condonarMora) permisos.exigirAdmin();
        if (condonarMora && (motivoCondonacion == null || motivoCondonacion.isBlank())) {
            throw new NegocioException("Indica el motivo de la condonación");
        }
        Contrato contrato = contratos.detalle(contratoId).orElseThrow(() -> new NegocioException("Contrato inexistente"));
        BigDecimal restante = dinero(recibido); List<Imputacion> resultado = new ArrayList<>();
        for (Cuota cuota : cuotas.findByContratoIdOrderByFechaVencimientoAsc(contratoId)) {
            if (restante.signum() == 0 || cuota.getEstado() == EstadoCuota.ANULADA || cuota.getEstado() == EstadoCuota.PAGADA
                    || (!permitirAdelantos && cuota.getFechaVencimiento().isAfter(fechaPago.withDayOfMonth(fechaPago.lengthOfMonth())))) continue;
            BigDecimal moraPendiente = calculadora.pendiente(cuota, contrato, fechaPago);
            BigDecimal condonada = condonarMora ? moraPendiente : BigDecimal.ZERO;
            BigDecimal mora = condonarMora ? BigDecimal.ZERO : moraPendiente.min(restante);
            restante = restante.subtract(mora);
            BigDecimal capital = calculadora.saldoCapital(cuota).min(restante); restante = restante.subtract(capital);
            if (mora.signum() > 0 || capital.signum() > 0) {
                resultado.add(new Imputacion(cuota.getId(), cuota.getConcepto(), cuota.getFechaVencimiento(),
                        dinero(capital), dinero(mora), dinero(condonada), condonarMora ? motivoCondonacion.trim() : null));
            }
        }
        return resultado;
    }

    @Transactional
    public void anular(Long id, String motivo) {
        permisos.exigirAdmin();
        if (motivo == null || motivo.isBlank()) throw new NegocioException("Indica el motivo de anulación");
        Pago pago = obtener(id); if (pago.getEstado() != EstadoPago.EMITIDO) throw new NegocioException("El pago ya está anulado");
        for (PagoDetalle detalle : pago.getDetalles()) {
            Pago posterior = pagos.activosDeCuota(detalle.getCuota().getId()).stream().filter(otro -> !otro.getId().equals(pago.getId()))
                    .filter(otro -> otro.getFechaRegistro().isAfter(pago.getFechaRegistro()) || (otro.getFechaRegistro().equals(pago.getFechaRegistro()) && otro.getId() > pago.getId()))
                    .findFirst().orElse(null);
            if (posterior != null) throw new NegocioException("Anule primero el pago posterior " + posterior.getRecibo());
            Cuota cuota = detalle.getCuota(); cuota.setMontoPagado(cuota.getMontoPagado().subtract(detalle.getMontoCapital()));
            cuota.setMoraCobrada(cuota.getMoraCobrada().subtract(detalle.getMontoMora())); cuota.setMoraCondonada(cuota.getMoraCondonada().subtract(detalle.getMontoMoraCondonada()));
            cuota.setMoraAcumulada(detalle.getMoraAcumuladaAntes()); cuota.setMoraDesde(detalle.getMoraDesdeAntes()); cuota.setEstado(detalle.getEstadoCuotaAntes());
        }
        pago.setEstado(EstadoPago.ANULADO); pago.setMotivoAnulacion(motivo.trim()); pago.setFechaAnulacion(LocalDateTime.now(clock));
        auditoria.registrar(AccionAuditoria.ANULACION_PAGO, "Pago", pago.getId(), "Recibo " + pago.getRecibo());
    }

    public Pago obtener(Long id) { return pagos.detalle(id).orElseThrow(() -> new NegocioException("Pago inexistente")); }
    @Transactional(readOnly = true)
    public List<Contrato> contratosCobrables() { return contratos.cobrables(); }
    public List<Pago> listar(LocalDate desde, LocalDate hasta) { return pagos.buscar(desde, hasta); }

    private void aplicarLinea(Pago pago, Contrato contrato, LocalDate fecha, boolean permitirAdelantos, Linea linea, int item, Set<Long> usadas) {
        if (linea == null || linea.cuotaId() == null || !usadas.add(linea.cuotaId())) throw new NegocioException("Cada cuota solo puede figurar una vez en el pago");
        Cuota cuota = cuotas.findById(linea.cuotaId()).orElseThrow(() -> new NegocioException("Cuota inexistente"));
        if (!cuota.getContrato().getId().equals(contrato.getId()) || cuota.getEstado() == EstadoCuota.ANULADA || cuota.getEstado() == EstadoCuota.PAGADA) throw new NegocioException("La cuota no está disponible para cobro");
        if (!permitirAdelantos && cuota.getFechaVencimiento().isAfter(fecha.withDayOfMonth(fecha.lengthOfMonth()))) throw new NegocioException("Activa Permitir adelantos para cobrar cuotas futuras");
        BigDecimal capital = dinero(linea.capital()), mora = dinero(linea.mora()), condonada = dinero(linea.condonada());
        if (capital.signum() < 0 || mora.signum() < 0 || condonada.signum() < 0 || capital.add(mora).signum() <= 0) throw new NegocioException("Los importes del pago deben ser positivos");
        BigDecimal saldo = calculadora.saldoCapital(cuota), moraPendiente = calculadora.pendiente(cuota, contrato, fecha), moraTotal = calculadora.totalDevengado(cuota, contrato, fecha);
        if (capital.compareTo(saldo) > 0 || mora.compareTo(moraPendiente) > 0 || condonada.compareTo(moraPendiente.subtract(mora)) > 0) throw new NegocioException("El importe excede el saldo de la cuota");
        if (condonada.signum() > 0 && (linea.motivo() == null || linea.motivo().isBlank())) throw new NegocioException("Indica el motivo de la condonación");
        if (capital.signum() > 0 && mora.add(condonada).compareTo(moraPendiente) < 0) throw new NegocioException("La mora pendiente debe pagarse o condonarse antes de aplicar capital");
        pago.agregarDetalle(PagoDetalle.builder().cuota(cuota).item(item).concepto(cuota.getConcepto()).periodo(cuota.getPeriodo()).fechaVencimiento(cuota.getFechaVencimiento()).montoCapital(capital).montoMora(mora).montoMoraCondonada(condonada).motivoCondonacion(linea.motivo()).moraAcumuladaAntes(cuota.getMoraAcumulada()).moraDesdeAntes(cuota.getMoraDesde()).estadoCuotaAntes(cuota.getEstado()).build());
        cuota.setMoraAcumulada(moraTotal); cuota.setMoraCobrada(cuota.getMoraCobrada().add(mora)); cuota.setMoraCondonada(cuota.getMoraCondonada().add(condonada)); cuota.setMontoPagado(cuota.getMontoPagado().add(capital));
        if (cuota.getMontoPagado().compareTo(cuota.getMontoCuota()) == 0) cuota.setEstado(EstadoCuota.PAGADA);
        else if (cuota.getMontoPagado().signum() > 0) cuota.setEstado(EstadoCuota.PARCIAL);
        // El corte se guarda incluso cuando el capital deja la cuota pagada: permite
        // revertir exactamente el estado anterior desde PagoDetalle.
        if (capital.signum() > 0) cuota.setMoraDesde(fecha);
        pago.setMontoCapital(pago.getMontoCapital().add(capital)); pago.setMontoMora(pago.getMontoMora().add(mora));
    }
    private void validarSolicitud(Solicitud s, Contrato c) {
        if (s.fecha() == null || s.fecha().isAfter(LocalDate.now(clock)) || s.fecha().isBefore(c.getFechaInicio())) throw new NegocioException("La fecha de pago no es válida");
        if (s.medio() == null) throw new NegocioException("Selecciona el medio de pago");
        if (s.lineas() == null || s.lineas().isEmpty()) throw new NegocioException("Agrega al menos una cuota");
    }
    private static BigDecimal cero() { return new BigDecimal("0.00"); }
    private static BigDecimal dinero(BigDecimal valor) { return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP); }
}

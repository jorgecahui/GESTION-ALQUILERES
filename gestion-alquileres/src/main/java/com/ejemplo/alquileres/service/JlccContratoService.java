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
import java.util.List;

@Service
@RequiredArgsConstructor
public class JlccContratoService {
    private final ContratoRepository jlccContratos;
    private final CuotaRepository jlccCuotas;
    private final InmuebleRepository jlccInmuebles;
    private final PersonaRepository jlccPersonas;
    private final JlccSecuenciaService jlccSecuencias;
    private final JlccCronogramaGenerador jlccGenerador;
    private final JlccConfiguracionService jlccConfiguracion;
    private final JlccPermisos jlccPermisos;
    private final JlccAuditoriaService jlccAuditoria;
    private final Clock jlccClock;

    public record Solicitud(
            Long inmuebleId,
            Long arrendatarioId,
            Long garanteId,
            LocalDate inicio,
            int meses,
            int diaPago,
            BigDecimal renta,
            BigDecimal incremento,
            BigDecimal garantia,
            boolean garantiaRecibida,
            Integer gracia,
            TipoMora tipoMora,
            BigDecimal valorMora,
            String observaciones) {}

    @Transactional
    public Contrato jlccCrear(Solicitud s) {
        jlccPermisos.jlccExigirGestion();
        jlccValidar(s);
        Inmueble i =
                jlccInmuebles
                        .findById(s.inmuebleId())
                        .orElseThrow(() -> new JlccNegocioException("El inmueble no existe"));
        if (i.getEstado() != EstadoInmueble.DISPONIBLE) {
            throw new JlccNegocioException("El inmueble debe estar disponible");
        }
        LocalDate jlccFin = s.inicio().plusMonths(s.meses()).minusDays(1);
        if (jlccContratos.existeTraslape(i.getId(), EstadoContrato.VIGENTE, s.inicio(), jlccFin)) {
            throw new JlccNegocioException("El inmueble tiene un contrato vigente traslapado");
        }
        Persona a = jlccPersonaActiva(s.arrendatarioId(), "arrendatario");
        if (s.garanteId() != null && s.garanteId().equals(s.arrendatarioId())) {
            throw new JlccNegocioException("El garante debe ser distinto del arrendatario");
        }
        BigDecimal jlccGarantia = jlccNvl(s.garantia());
        Contrato c =
                Contrato.builder()
                        .codigo(jlccSecuencias.jlccFormatear("CTR-", jlccSecuencias.jlccSiguiente("CONTRATO"), 6))
                        .inmueble(i)
                        .arrendatario(a)
                        .garante(
                                s.garanteId() == null
                                        ? null
                                        : jlccPersonaActiva(s.garanteId(), "garante"))
                        .fechaInicio(s.inicio())
                        .fechaFin(jlccFin)
                        .diaPago(s.diaPago())
                        .rentaMensual(s.renta().setScale(2, RoundingMode.HALF_UP))
                        .incrementoAnualPct(jlccNvl(s.incremento()))
                        .garantiaMonto(jlccGarantia)
                        .garantiaEstado(
                                jlccGarantia.signum() == 0
                                        ? GarantiaEstado.SIN_GARANTIA
                                        : (s.garantiaRecibida()
                                                ? GarantiaEstado.RECIBIDA
                                                : GarantiaEstado.PENDIENTE))
                        .diasGracia(
                                s.gracia() == null ? jlccConfiguracion.jlccMoraDiasGracia() : s.gracia())
                        .tipoMora(s.tipoMora() == null ? jlccConfiguracion.jlccMoraTipo() : s.tipoMora())
                        .valorMora(
                                s.valorMora() == null ? jlccConfiguracion.jlccMoraValor() : s.valorMora())
                        .estado(EstadoContrato.VIGENTE)
                        .observaciones(s.observaciones())
                        .fechaRegistro(LocalDate.now(jlccClock))
                        .build();
        jlccGenerador
                .jlccGenerar(
                        s.inicio(),
                        s.meses(),
                        s.diaPago(),
                        c.getRentaMensual(),
                        c.getIncrementoAnualPct())
                .forEach(c::agregarCuota);
        i.setEstado(EstadoInmueble.ALQUILADO);
        Contrato jlccGuardado = jlccContratos.save(c);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CREACION_CONTRATO,
                "Contrato",
                jlccGuardado.getId(),
                "Código " + jlccGuardado.getCodigo());
        return jlccGuardado;
    }

    @Transactional
    public void jlccTerminar(
            Long jlccId,
            LocalDate jlccFecha,
            String jlccMotivo,
            boolean jlccRescindir,
            BigDecimal jlccDevuelta,
            BigDecimal jlccDescontada,
            String jlccMotivoGarantia) {
        jlccPermisos.jlccExigirAdmin();
        Contrato c =
                jlccContratos
                        .detalle(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("Contrato inexistente"));
        if (c.getEstado() != EstadoContrato.VIGENTE) {
            throw new JlccNegocioException("Solo se puede terminar un contrato vigente");
        }
        if (jlccFecha == null) {
            throw new JlccNegocioException("Indica la fecha de término");
        }
        if (jlccRescindir && (jlccMotivo == null || jlccMotivo.isBlank())) {
            throw new JlccNegocioException("El motivo de rescisión es obligatorio");
        }
        BigDecimal jlccGarantia = jlccNvl(c.getGarantiaMonto());
        BigDecimal jlccDevueltaFinal = jlccNvl(jlccDevuelta);
        BigDecimal jlccDescontadaFinal = jlccNvl(jlccDescontada);
        if (jlccDevueltaFinal.add(jlccDescontadaFinal).compareTo(jlccGarantia) > 0) {
            throw new JlccNegocioException("La liquidación excede la garantía");
        }
        if (jlccDescontadaFinal.signum() > 0 && (jlccMotivoGarantia == null || jlccMotivoGarantia.isBlank())) {
            throw new JlccNegocioException("Indica el motivo del descuento de la garantía");
        }
        jlccCuotas.anularFuturas(jlccId, jlccFecha);
        c.setEstado(jlccRescindir ? EstadoContrato.RESCINDIDO : EstadoContrato.FINALIZADO);
        c.setFechaTerminoEfectiva(jlccFecha);
        c.setMotivoTermino(jlccMotivo);
        c.setGarantiaDevuelta(jlccDevueltaFinal);
        c.setGarantiaDescontada(jlccDescontadaFinal);
        c.setGarantiaMotivo(jlccMotivoGarantia);
        c.setGarantiaEstado(
                jlccGarantia.signum() == 0
                        ? GarantiaEstado.SIN_GARANTIA
                        : (jlccDevueltaFinal.compareTo(jlccGarantia) < 0
                                ? GarantiaEstado.RETENIDA_PARCIAL
                                : GarantiaEstado.DEVUELTA));
        c.getInmueble().setEstado(EstadoInmueble.DISPONIBLE);
        jlccAuditoria.jlccRegistrar(
                jlccRescindir
                        ? AccionAuditoria.RESCISION_CONTRATO
                        : AccionAuditoria.FINALIZACION_CONTRATO,
                "Contrato",
                jlccId,
                "Contrato " + c.getCodigo());
    }

    @Transactional
    public Contrato jlccRenovar(Long jlccAnteriorId, Solicitud jlccPropuesta) {
        jlccPermisos.jlccExigirGestion();
        if (jlccPropuesta == null || jlccPropuesta.inicio() == null) {
            throw new JlccNegocioException("Indica la fecha de inicio de la renovación");
        }
        Contrato jlccAnterior =
                jlccContratos
                        .detalle(jlccAnteriorId)
                        .orElseThrow(() -> new JlccNegocioException("Contrato inexistente"));
        if (jlccAnterior.getEstado() != EstadoContrato.VIGENTE) {
            throw new JlccNegocioException("Solo se renueva un contrato vigente");
        }
        if (!jlccPropuesta.inmuebleId().equals(jlccAnterior.getInmueble().getId())
                || !jlccPropuesta.arrendatarioId().equals(jlccAnterior.getArrendatario().getId())) {
            throw new JlccNegocioException("La renovación conserva inmueble y arrendatario");
        }
        if (!jlccPropuesta.inicio().equals(jlccAnterior.getFechaFin().plusDays(1))) {
            throw new JlccNegocioException(
                    "La renovación debe iniciar al día siguiente del contrato anterior");
        }
        jlccAnterior.setEstado(EstadoContrato.FINALIZADO);
        jlccAnterior.setFechaTerminoEfectiva(jlccAnterior.getFechaFin());
        jlccAnterior.setMotivoTermino("Renovado");
        Solicitud s =
                new Solicitud(
                        jlccPropuesta.inmuebleId(),
                        jlccPropuesta.arrendatarioId(),
                        jlccPropuesta.garanteId(),
                        jlccPropuesta.inicio(),
                        jlccPropuesta.meses(),
                        jlccPropuesta.diaPago(),
                        jlccPropuesta.renta(),
                        jlccPropuesta.incremento(),
                        jlccNvl(jlccAnterior.getGarantiaMonto()),
                        true,
                        jlccPropuesta.gracia(),
                        jlccPropuesta.tipoMora(),
                        jlccPropuesta.valorMora(),
                        jlccPropuesta.observaciones());
        jlccAnterior.getInmueble().setEstado(EstadoInmueble.DISPONIBLE);
        Contrato jlccNuevo = jlccCrear(s);
        jlccNuevo.setContratoAnterior(jlccAnterior);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.RENOVACION_CONTRATO,
                "Contrato",
                jlccNuevo.getId(),
                "Renovación de " + jlccAnterior.getCodigo());
        return jlccNuevo;
    }

    @Transactional(readOnly = true)
    public List<Contrato> jlccListar() {
        return jlccContratos.listarTodos();
    }

    @Transactional(readOnly = true)
    public Contrato jlccObtener(Long jlccId) {
        return jlccContratos
                .detalle(jlccId)
                .orElseThrow(() -> new JlccNegocioException("Contrato inexistente"));
    }

    public JlccEstadoVisual jlccEstadoVisual(Contrato c) {
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        if (c.getEstado() != EstadoContrato.VIGENTE) {
            return JlccEstadoVisual.valueOf(c.getEstado().name());
        }
        if (c.getFechaFin().isBefore(jlccHoy)) {
            return JlccEstadoVisual.VENCIDO;
        }
        if (!c.getFechaFin().isAfter(jlccHoy.plusDays(jlccConfiguracion.jlccDiasVencimiento()))) {
            return JlccEstadoVisual.POR_VENCER;
        }
        return JlccEstadoVisual.VIGENTE;
    }

    public enum JlccEstadoVisual {
        VIGENTE("Vigente"),
        POR_VENCER("Por vencer"),
        VENCIDO("Vencido"),
        FINALIZADO("Finalizado"),
        RESCINDIDO("Rescindido");

        private final String jlccEtiqueta;

        JlccEstadoVisual(String jlccEtiqueta) {
            this.jlccEtiqueta = jlccEtiqueta;
        }

        public String jlccGetEtiqueta() {
            return jlccEtiqueta;
        }

        @Override
        public String toString() {
            return jlccEtiqueta;
        }
    }

    private void jlccValidar(Solicitud s) {
        if (s.inmuebleId() == null) {
            throw new JlccNegocioException("Selecciona un inmueble");
        }
        if (s.arrendatarioId() == null) {
            throw new JlccNegocioException("Selecciona un arrendatario");
        }
        if (s.inicio() == null) {
            throw new JlccNegocioException("Indica la fecha de inicio");
        }
        if (s.meses() < 1) {
            throw new JlccNegocioException("La duración debe ser de meses completos");
        }
        if (s.inicio().isBefore(LocalDate.now(jlccClock).minusMonths(12))) {
            throw new JlccNegocioException("La fecha de inicio es demasiado antigua");
        }
        if (s.diaPago() < 1 || s.diaPago() > 28) {
            throw new JlccNegocioException("El día de pago debe estar entre 1 y 28");
        }
        if (s.renta() == null || s.renta().signum() <= 0) {
            throw new JlccNegocioException("La renta debe ser mayor que cero");
        }
        if (s.gracia() != null && s.gracia() < 0
                || s.valorMora() != null && s.valorMora().signum() < 0) {
            throw new JlccNegocioException("Los parámetros de mora no pueden ser negativos");
        }
    }

    private Persona jlccPersonaActiva(Long jlccId, String jlccRol) {
        Persona p =
                jlccPersonas.findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("El " + jlccRol + " no existe"));
        if (!p.isActivo()) {
            throw new JlccNegocioException("El " + jlccRol + " debe estar activo");
        }
        return p;
    }

    private static BigDecimal jlccNvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

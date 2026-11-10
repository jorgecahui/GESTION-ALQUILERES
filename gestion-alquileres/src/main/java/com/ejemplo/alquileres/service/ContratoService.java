package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContratoService {
    private final ContratoRepository contratos;
    private final CuotaRepository cuotas;
    private final InmuebleRepository inmuebles;
    private final PersonaRepository personas;
    private final SecuenciaService secuencias;
    private final CronogramaGenerador generador;
    private final ConfiguracionService configuracion;
    private final Permisos permisos;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public record Solicitud(Long inmuebleId, Long arrendatarioId, Long garanteId, LocalDate inicio,
                            int meses, int diaPago, BigDecimal renta, BigDecimal incremento,
                            BigDecimal garantia, boolean garantiaRecibida, Integer gracia,
                            TipoMora tipoMora, BigDecimal valorMora, String observaciones) { }

    @Transactional
    public Contrato crear(Solicitud s) {
        permisos.exigirGestion();
        validar(s);
        Inmueble i = inmuebles.findById(s.inmuebleId())
                .orElseThrow(() -> new NegocioException("El inmueble no existe"));
        if (i.getEstado() != EstadoInmueble.DISPONIBLE) {
            throw new NegocioException("El inmueble debe estar disponible");
        }
        LocalDate fin = s.inicio().plusMonths(s.meses()).minusDays(1);
        if (contratos.existeTraslape(i.getId(), EstadoContrato.VIGENTE, s.inicio(), fin)) {
            throw new NegocioException("El inmueble tiene un contrato vigente traslapado");
        }
        Persona a = personaActiva(s.arrendatarioId(), "arrendatario");
        if (s.garanteId() != null && s.garanteId().equals(s.arrendatarioId())) {
            throw new NegocioException("El garante debe ser distinto del arrendatario");
        }
        BigDecimal garantia = nvl(s.garantia());
        Contrato c = Contrato.builder()
                .codigo(secuencias.formatear("CTR-", secuencias.siguiente("CONTRATO"), 6))
                .inmueble(i)
                .arrendatario(a)
                .garante(s.garanteId() == null ? null : personaActiva(s.garanteId(), "garante"))
                .fechaInicio(s.inicio())
                .fechaFin(fin)
                .diaPago(s.diaPago())
                .rentaMensual(s.renta().setScale(2, RoundingMode.HALF_UP))
                .incrementoAnualPct(nvl(s.incremento()))
                .garantiaMonto(garantia)
                .garantiaEstado(garantia.signum() == 0
                        ? GarantiaEstado.SIN_GARANTIA
                        : (s.garantiaRecibida() ? GarantiaEstado.RECIBIDA : GarantiaEstado.PENDIENTE))
                .diasGracia(s.gracia() == null ? configuracion.moraDiasGracia() : s.gracia())
                .tipoMora(s.tipoMora() == null ? configuracion.moraTipo() : s.tipoMora())
                .valorMora(s.valorMora() == null ? configuracion.moraValor() : s.valorMora())
                .estado(EstadoContrato.VIGENTE)
                .observaciones(s.observaciones())
                .fechaRegistro(LocalDate.now(clock))
                .build();
        generador.generar(s.inicio(), s.meses(), s.diaPago(), c.getRentaMensual(), c.getIncrementoAnualPct())
                .forEach(c::agregarCuota);
        i.setEstado(EstadoInmueble.ALQUILADO);
        Contrato guardado = contratos.save(c);
        auditoria.registrar(AccionAuditoria.CREACION_CONTRATO, "Contrato", guardado.getId(),
                "Código " + guardado.getCodigo());
        return guardado;
    }

    @Transactional
    public void terminar(Long id, LocalDate fecha, String motivo, boolean rescindir,
                         BigDecimal devuelta, BigDecimal descontada, String motivoGarantia) {
        permisos.exigirAdmin();
        Contrato c = contratos.detalle(id).orElseThrow(() -> new NegocioException("Contrato inexistente"));
        if (c.getEstado() != EstadoContrato.VIGENTE) {
            throw new NegocioException("Solo se puede terminar un contrato vigente");
        }
        if (fecha == null) {
            throw new NegocioException("Indica la fecha de término");
        }
        if (rescindir && (motivo == null || motivo.isBlank())) {
            throw new NegocioException("El motivo de rescisión es obligatorio");
        }
        BigDecimal garantia = nvl(c.getGarantiaMonto());
        BigDecimal devueltaFinal = nvl(devuelta);
        BigDecimal descontadaFinal = nvl(descontada);
        if (devueltaFinal.add(descontadaFinal).compareTo(garantia) > 0) {
            throw new NegocioException("La liquidación excede la garantía");
        }
        if (descontadaFinal.signum() > 0 && (motivoGarantia == null || motivoGarantia.isBlank())) {
            throw new NegocioException("Indica el motivo del descuento de la garantía");
        }
        cuotas.anularFuturas(id, fecha);
        c.setEstado(rescindir ? EstadoContrato.RESCINDIDO : EstadoContrato.FINALIZADO);
        c.setFechaTerminoEfectiva(fecha);
        c.setMotivoTermino(motivo);
        c.setGarantiaDevuelta(devueltaFinal);
        c.setGarantiaDescontada(descontadaFinal);
        c.setGarantiaMotivo(motivoGarantia);
        c.setGarantiaEstado(garantia.signum() == 0 ? GarantiaEstado.SIN_GARANTIA
                : (devueltaFinal.compareTo(garantia) < 0 ? GarantiaEstado.RETENIDA_PARCIAL : GarantiaEstado.DEVUELTA));
        c.getInmueble().setEstado(EstadoInmueble.DISPONIBLE);
        auditoria.registrar(rescindir ? AccionAuditoria.RESCISION_CONTRATO : AccionAuditoria.FINALIZACION_CONTRATO,
                "Contrato", id, "Contrato " + c.getCodigo());
    }

    @Transactional
    public Contrato renovar(Long anteriorId, Solicitud propuesta) {
        permisos.exigirGestion();
        Contrato anterior = contratos.detalle(anteriorId)
                .orElseThrow(() -> new NegocioException("Contrato inexistente"));
        if (anterior.getEstado() != EstadoContrato.VIGENTE) {
            throw new NegocioException("Solo se renueva un contrato vigente");
        }
        if (!propuesta.inmuebleId().equals(anterior.getInmueble().getId())
                || !propuesta.arrendatarioId().equals(anterior.getArrendatario().getId())) {
            throw new NegocioException("La renovación conserva inmueble y arrendatario");
        }
        if (!propuesta.inicio().equals(anterior.getFechaFin().plusDays(1))) {
            throw new NegocioException("La renovación debe iniciar al día siguiente del contrato anterior");
        }
        anterior.setEstado(EstadoContrato.FINALIZADO);
        anterior.setFechaTerminoEfectiva(anterior.getFechaFin());
        anterior.setMotivoTermino("Renovado");
        Solicitud s = new Solicitud(propuesta.inmuebleId(), propuesta.arrendatarioId(), propuesta.garanteId(),
                propuesta.inicio(), propuesta.meses(), propuesta.diaPago(), propuesta.renta(),
                propuesta.incremento(), nvl(anterior.getGarantiaMonto()), true, propuesta.gracia(),
                propuesta.tipoMora(), propuesta.valorMora(), propuesta.observaciones());
        anterior.getInmueble().setEstado(EstadoInmueble.DISPONIBLE);
        Contrato nuevo = crear(s);
        nuevo.setContratoAnterior(anterior);
        auditoria.registrar(AccionAuditoria.RENOVACION_CONTRATO, "Contrato", nuevo.getId(),
                "Renovación de " + anterior.getCodigo());
        return nuevo;
    }

    @Transactional(readOnly = true)
    public List<Contrato> listar() {
        return contratos.listarTodos();
    }

    @Transactional(readOnly = true)
    public Contrato obtener(Long id) {
        return contratos.detalle(id).orElseThrow(() -> new NegocioException("Contrato inexistente"));
    }

    public EstadoVisual estadoVisual(Contrato c) {
        LocalDate hoy = LocalDate.now(clock);
        if (c.getEstado() != EstadoContrato.VIGENTE) {
            return EstadoVisual.valueOf(c.getEstado().name());
        }
        if (c.getFechaFin().isBefore(hoy)) {
            return EstadoVisual.VENCIDO;
        }
        if (!c.getFechaFin().isAfter(hoy.plusDays(configuracion.diasVencimiento()))) {
            return EstadoVisual.POR_VENCER;
        }
        return EstadoVisual.VIGENTE;
    }

    public enum EstadoVisual { VIGENTE, POR_VENCER, VENCIDO, FINALIZADO, RESCINDIDO }

    private void validar(Solicitud s) {
        if (s.meses() < 1) {
            throw new NegocioException("La duración debe ser de meses completos");
        }
        if (s.inicio().isBefore(LocalDate.now(clock).minusMonths(12))) {
            throw new NegocioException("La fecha de inicio es demasiado antigua");
        }
        if (s.diaPago() < 1 || s.diaPago() > 28) {
            throw new NegocioException("El día de pago debe estar entre 1 y 28");
        }
        if (s.renta() == null || s.renta().signum() <= 0) {
            throw new NegocioException("La renta debe ser mayor que cero");
        }
        if (s.gracia() != null && s.gracia() < 0
                || s.valorMora() != null && s.valorMora().signum() < 0) {
            throw new NegocioException("Los parámetros de mora no pueden ser negativos");
        }
    }

    private Persona personaActiva(Long id, String rol) {
        Persona p = personas.findById(id)
                .orElseThrow(() -> new NegocioException("El " + rol + " no existe"));
        if (!p.isActivo()) {
            throw new NegocioException("El " + rol + " debe estar activo");
        }
        return p;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

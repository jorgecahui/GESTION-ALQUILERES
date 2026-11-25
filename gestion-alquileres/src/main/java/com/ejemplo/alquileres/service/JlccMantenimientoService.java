package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JlccMantenimientoService {
    private final MantenimientoRepository jlccMantenimientos;
    private final InmuebleRepository jlccInmuebles;
    private final ContratoRepository jlccContratos;
    private final CuotaRepository jlccCuotas;
    private final JlccSecuenciaService jlccSecuencias;
    private final JlccPermisos jlccPermisos;
    private final Clock jlccClock;

    public record Solicitud(
            Long inmuebleId,
            Long contratoId,
            LocalDate fechaReporte,
            String titulo,
            String descripcion,
            CategoriaMantenimiento categoria,
            PrioridadMantenimiento prioridad,
            ResponsablePago responsablePago,
            String tecnico,
            String telefonoTecnico,
            BigDecimal costoEstimado,
            LocalDate fechaProgramada,
            String observaciones) {}

    public record Resumen(long abiertos, long urgentes, long enProceso, BigDecimal gastoMes) {}

    public Page<Mantenimiento> jlccListar(
            String jlccTexto,
            EstadoMantenimiento jlccEstado,
            PrioridadMantenimiento jlccPrioridad,
            Long jlccInmuebleId,
            CategoriaMantenimiento jlccCategoria,
            int jlccPagina,
            int jlccTamano) {
        return jlccMantenimientos.buscar(
                jlccTexto == null ? "" : jlccTexto.trim(),
                jlccEstado,
                jlccPrioridad,
                jlccInmuebleId,
                jlccCategoria,
                PageRequest.of(
                        Math.max(0, jlccPagina),
                        jlccTamano,
                        Sort.by(Sort.Direction.DESC, "fechaReporte")
                                .and(Sort.by(Sort.Direction.DESC, "id"))));
    }

    public List<Mantenimiento> jlccPorInmueble(Long jlccId) {
        return jlccMantenimientos.porInmueble(jlccId);
    }

    public List<Mantenimiento> jlccGastosPropietario(LocalDate jlccDesde, LocalDate jlccHasta) {
        return jlccMantenimientos.gastosPropietario(jlccDesde, jlccHasta);
    }

    public Resumen jlccResumen() {
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        BigDecimal jlccGasto =
                jlccMantenimientos.gastosPropietario(jlccHoy.withDayOfMonth(1), jlccHoy).stream()
                        .map(Mantenimiento::getCostoReal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Resumen(
                jlccMantenimientos.countByEstado(EstadoMantenimiento.REPORTADO),
                jlccMantenimientos.countByEstadoAndPrioridad(
                        EstadoMantenimiento.REPORTADO, PrioridadMantenimiento.URGENTE),
                jlccMantenimientos.countByEstado(EstadoMantenimiento.EN_PROCESO),
                jlccDinero(jlccGasto));
    }

    @Transactional
    public Mantenimiento jlccRegistrar(Solicitud s) {
        jlccPermisos.jlccExigirGestion();
        jlccValidarRegistro(s);
        Inmueble jlccInmueble = jlccInmueble(s.inmuebleId());
        Contrato jlccContrato =
                s.contratoId() == null
                        ? jlccContratoVigente(jlccInmueble.getId())
                        : jlccContrato(s.contratoId());
        if (jlccContrato != null && !jlccContrato.getInmueble().getId().equals(jlccInmueble.getId()))
            throw new JlccNegocioException("El contrato debe pertenecer al inmueble.");
        Mantenimiento m =
                Mantenimiento.builder()
                        .codigo(
                                jlccSecuencias.jlccFormatear(
                                        "MNT-", jlccSecuencias.jlccSiguiente("MANTENIMIENTO"), 6))
                        .inmueble(jlccInmueble)
                        .contrato(jlccContrato)
                        .fechaReporte(s.fechaReporte())
                        .titulo(s.titulo().trim())
                        .descripcion(jlccTexto(s.descripcion()))
                        .categoria(s.categoria())
                        .prioridad(s.prioridad())
                        .responsablePago(
                                s.responsablePago() == null
                                        ? ResponsablePago.PROPIETARIO
                                        : s.responsablePago())
                        .tecnico(jlccTexto(s.tecnico()))
                        .telefonoTecnico(jlccTexto(s.telefonoTecnico()))
                        .costoEstimado(jlccDineroNulo(s.costoEstimado()))
                        .fechaProgramada(s.fechaProgramada())
                        .observaciones(jlccTexto(s.observaciones()))
                        .fechaRegistro(LocalDate.now(jlccClock))
                        .build();
        return jlccMantenimientos.save(m);
    }

    @Transactional
    public Mantenimiento jlccIniciar(Long jlccId, boolean jlccPonerInmueble) {
        jlccPermisos.jlccExigirGestion();
        Mantenimiento m = jlccObtener(jlccId);
        jlccExigir(m, EstadoMantenimiento.REPORTADO);
        if (jlccPonerInmueble) {
            if (m.getInmueble().getEstado() != EstadoInmueble.DISPONIBLE)
                throw new JlccNegocioException(
                        "Solo se puede poner en mantenimiento un inmueble disponible.");
            m.getInmueble().setEstado(EstadoInmueble.MANTENIMIENTO);
            m.setPusoInmuebleEnMantenimiento(true);
        }
        m.setEstado(EstadoMantenimiento.EN_PROCESO);
        return m;
    }

    @Transactional
    public Mantenimiento jlccResolver(Long jlccId, LocalDate jlccFecha, BigDecimal jlccCosto) {
        jlccPermisos.jlccExigirGestion();
        Mantenimiento m = jlccObtener(jlccId);
        jlccExigir(m, EstadoMantenimiento.EN_PROCESO);
        if (jlccFecha == null || jlccCosto == null)
            throw new JlccNegocioException("La fecha de resolución y el costo real son obligatorios.");
        if (jlccFecha.isBefore(m.getFechaReporte()) || jlccFecha.isAfter(LocalDate.now(jlccClock)))
            throw new JlccNegocioException("La fecha de resolución debe estar entre el reporte y hoy.");
        if (jlccCosto.signum() < 0) throw new JlccNegocioException("El costo real no puede ser negativo.");
        m.setFechaResolucion(jlccFecha);
        m.setCostoReal(jlccDinero(jlccCosto));
        m.setEstado(EstadoMantenimiento.RESUELTO);
        jlccRestaurarInmueble(m);
        return m;
    }

    @Transactional
    public Mantenimiento jlccCancelar(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        Mantenimiento m = jlccObtener(jlccId);
        if (m.getEstado() != EstadoMantenimiento.REPORTADO
                && m.getEstado() != EstadoMantenimiento.EN_PROCESO)
            throw new JlccNegocioException(
                    "Solo se puede cancelar un mantenimiento reportado o en proceso.");
        if (m.getCuotaCargo() != null) {
            Cuota q =
                    jlccCuotas.findById(m.getCuotaCargo().getId())
                            .orElseThrow(
                                    () -> new JlccNegocioException("No existe la cuota de cargo."));
            if (q.getMontoPagado().signum() > 0)
                throw new JlccNegocioException(
                        "No se puede cancelar: el cargo al arrendatario ya tiene pagos.");
            q.setEstado(EstadoCuota.ANULADA);
        }
        m.setEstado(EstadoMantenimiento.CANCELADO);
        jlccRestaurarInmueble(m);
        return m;
    }

    @Transactional
    public Cuota jlccGenerarCargo(Long jlccId, LocalDate jlccVencimiento) {
        jlccPermisos.jlccExigirGestion();
        Mantenimiento m = jlccObtener(jlccId);
        if (m.getCuotaCargo() != null)
            throw new JlccNegocioException(
                    "El cargo al arrendatario ya fue generado para este mantenimiento.");
        if (m.getEstado() != EstadoMantenimiento.RESUELTO)
            throw new JlccNegocioException(
                    "El cargo se genera únicamente al resolver el mantenimiento.");
        if (m.getResponsablePago() != ResponsablePago.ARRENDATARIO || m.getContrato() == null)
            throw new JlccNegocioException(
                    "El mantenimiento no tiene un contrato a cargo del arrendatario.");
        if (jlccVencimiento == null)
            throw new JlccNegocioException("La fecha de vencimiento del cargo es obligatoria.");
        Cuota q =
                Cuota.builder()
                        .contrato(m.getContrato())
                        .numero(Math.toIntExact(m.getId()))
                        .tipo(TipoCuota.OTRO)
                        .concepto("Reparación: " + m.getTitulo())
                        .periodo(jlccVencimiento.withDayOfMonth(1))
                        .fechaVencimiento(jlccVencimiento)
                        .montoCuota(jlccDinero(m.getCostoReal()))
                        .montoPagado(BigDecimal.ZERO.setScale(2))
                        .moraAcumulada(BigDecimal.ZERO.setScale(2))
                        .moraCobrada(BigDecimal.ZERO.setScale(2))
                        .moraCondonada(BigDecimal.ZERO.setScale(2))
                        .estado(EstadoCuota.PENDIENTE)
                        .build();
        q = jlccCuotas.save(q);
        m.setCuotaCargo(q);
        return q;
    }

    private void jlccValidarRegistro(Solicitud s) {
        if (s.inmuebleId() == null
                || s.titulo() == null
                || s.titulo().isBlank()
                || s.categoria() == null
                || s.prioridad() == null
                || s.fechaReporte() == null)
            throw new JlccNegocioException(
                    "Inmueble, título, categoría, prioridad y fecha de reporte son obligatorios.");
        if (s.fechaReporte().isAfter(LocalDate.now(jlccClock)))
            throw new JlccNegocioException("La fecha de reporte no puede ser futura.");
        if (s.costoEstimado() != null && s.costoEstimado().signum() < 0)
            throw new JlccNegocioException("Los costos no pueden ser negativos.");
    }

    private void jlccRestaurarInmueble(Mantenimiento m) {
        if (m.isPusoInmuebleEnMantenimiento()
                && m.getInmueble().getEstado() == EstadoInmueble.MANTENIMIENTO) {
            m.getInmueble().setEstado(EstadoInmueble.DISPONIBLE);
            m.setPusoInmuebleEnMantenimiento(false);
        }
    }

    private Mantenimiento jlccObtener(Long jlccId) {
        return jlccMantenimientos
                .findById(jlccId)
                .orElseThrow(() -> new JlccNegocioException("El mantenimiento no existe."));
    }

    private Inmueble jlccInmueble(Long jlccId) {
        return jlccInmuebles
                .findById(jlccId)
                .orElseThrow(() -> new JlccNegocioException("El inmueble no existe."));
    }

    private Contrato jlccContrato(Long jlccId) {
        return jlccContratos
                .findById(jlccId)
                .orElseThrow(() -> new JlccNegocioException("El contrato no existe."));
    }

    private Contrato jlccContratoVigente(Long jlccId) {
        return jlccContratos.findByInmuebleIdOrderByFechaInicioDesc(jlccId).stream()
                .filter(c -> c.getEstado() == EstadoContrato.VIGENTE)
                .findFirst()
                .orElse(null);
    }

    private void jlccExigir(Mantenimiento m, EstadoMantenimiento e) {
        if (m.getEstado() != e)
            throw new JlccNegocioException("La transición de " + m.getEstado() + " no está permitida.");
    }

    private static String jlccTexto(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static BigDecimal jlccDineroNulo(BigDecimal n) {
        return n == null ? null : jlccDinero(n);
    }

    private static BigDecimal jlccDinero(BigDecimal n) {
        return n.setScale(2, RoundingMode.HALF_UP);
    }
}

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

@Service
@RequiredArgsConstructor
public class JlccInmuebleService {
    private final InmuebleRepository jlccRepository;
    private final TipoInmuebleRepository jlccTipos;
    private final PropietarioRepository jlccPropietarios;
    private final JlccSecuenciaService jlccSecuencias;
    private final JlccPermisos jlccPermisos;
    private final Clock jlccClock;

    public Page<Inmueble> jlccListar(
            String jlccTexto, EstadoInmueble jlccEstado, Long jlccTipoId, int jlccPagina, int jlccTamano) {
        return jlccRepository.buscar(
                jlccTexto == null ? "" : jlccTexto.trim(),
                jlccEstado,
                jlccTipoId,
                PageRequest.of(Math.max(0, jlccPagina), jlccTamano, Sort.by("codigo")));
    }

    @Transactional
    public String jlccProximoCodigo() {
        return jlccSecuencias.jlccFormatear("INM-", jlccSecuencias.jlccSiguiente("INMUEBLE"), 4);
    }

    @Transactional
    public Inmueble jlccGuardar(Inmueble a) {
        jlccPermisos.jlccExigirGestion();
        if (a.getNombre() == null || a.getNombre().isBlank())
            throw new JlccNegocioException("El nombre del inmueble es obligatorio");
        if (a.getAreaM2() != null && a.getAreaM2().signum() <= 0)
            throw new JlccNegocioException("El área debe ser mayor que cero");
        if (a.getRentaReferencial() != null && a.getRentaReferencial().signum() < 0)
            throw new JlccNegocioException("La renta referencial no puede ser negativa");
        if (a.getNivel() != null && (a.getNivel() < -5 || a.getNivel() > 100))
            throw new JlccNegocioException("El nivel debe estar entre -5 y 100");
        if (a.getOrden() != null && a.getOrden() < 1)
            throw new JlccNegocioException("El orden debe ser mayor o igual a 1");
        boolean jlccNueva = a.getId() == null;
        if (jlccNueva && a.getEstado() == EstadoInmueble.ALQUILADO)
            throw new JlccNegocioException(
                    "El estado ALQUILADO solo puede establecerse desde un contrato");
        if (a.getNivel() != null && a.getOrden() != null) {
            boolean jlccOcupada =
                    jlccNueva
                            ? jlccRepository.existsByNivelAndOrdenAndEstadoNot(
                                    a.getNivel(), a.getOrden(), EstadoInmueble.INACTIVO)
                            : jlccRepository.existsByNivelAndOrdenAndEstadoNotAndIdNot(
                                    a.getNivel(), a.getOrden(), EstadoInmueble.INACTIVO, a.getId());
            if (jlccOcupada)
                throw new JlccNegocioException(
                        "Ya existe otro inmueble activo en el nivel "
                                + a.getNivel()
                                + " y orden "
                                + a.getOrden());
        }
        Inmueble i =
                jlccNueva
                        ? new Inmueble()
                        : jlccRepository
                                .findById(a.getId())
                                .orElseThrow(() -> new JlccNegocioException("El inmueble no existe"));
        if (!jlccNueva
                && a.getEstado() != null
                && i.getEstado() != a.getEstado()
                && (i.getEstado() == EstadoInmueble.ALQUILADO
                        || a.getEstado() == EstadoInmueble.ALQUILADO))
            throw new JlccNegocioException(
                    "No se puede cambiar el estado de un inmueble alquilado manualmente");
        String jlccCodigo =
                (a.getCodigo() == null || a.getCodigo().isBlank())
                        ? jlccProximoCodigo()
                        : a.getCodigo().trim().toUpperCase();
        if (jlccNueva
                ? jlccRepository.existsByCodigoIgnoreCase(jlccCodigo)
                : jlccRepository.existsByCodigoIgnoreCaseAndIdNot(jlccCodigo, a.getId()))
            throw new JlccNegocioException("Ya existe un inmueble con ese código");
        jlccCopiar(a, i);
        i.setCodigo(jlccCodigo);
        i.setTipo(
                a.getTipo() == null
                        ? null
                        : jlccTipos.findById(a.getTipo().getId())
                                .orElseThrow(() -> new JlccNegocioException("El tipo no existe")));
        i.setPropietario(
                a.getPropietario() == null
                        ? null
                        : jlccPropietarios
                                .findById(a.getPropietario().getId())
                                .orElseThrow(
                                        () -> new JlccNegocioException("El propietario no existe")));
        if (jlccNueva) i.setFechaRegistro(LocalDate.now(jlccClock));
        return jlccRepository.save(i);
    }

    private void jlccCopiar(Inmueble a, Inmueble i) {
        i.setNombre(a.getNombre().trim());
        if (a.getDireccion() != null) i.setDireccion(jlccVacio(a.getDireccion()));
        if (a.getReferencia() != null) i.setReferencia(jlccVacio(a.getReferencia()));
        if (a.getDistrito() != null) i.setDistrito(jlccVacio(a.getDistrito()));
        if (a.getProvincia() != null) i.setProvincia(jlccVacio(a.getProvincia()));
        if (a.getDepartamento() != null) i.setDepartamento(jlccVacio(a.getDepartamento()));
        if (a.getPiso() != null) i.setPiso(jlccVacio(a.getPiso()));
        if (a.getNivel() != null) i.setNivel(a.getNivel());
        if (a.getOrden() != null) i.setOrden(a.getOrden());
        if (a.getAreaM2() != null) i.setAreaM2(jlccEscalar(a.getAreaM2()));
        if (a.getDormitorios() != null) i.setDormitorios(a.getDormitorios());
        if (a.getBanos() != null) i.setBanos(a.getBanos());
        if (a.getEstacionamientos() != null) i.setEstacionamientos(a.getEstacionamientos());
        i.setAmoblado(a.isAmoblado());
        if (a.getRentaReferencial() != null)
            i.setRentaReferencial(jlccEscalar(a.getRentaReferencial()));
        if (a.getEstado() != null) i.setEstado(a.getEstado());
        if (a.getServiciosIncluidos() != null)
            i.setServiciosIncluidos(jlccVacio(a.getServiciosIncluidos()));
        if (a.getDescripcion() != null) i.setDescripcion(jlccVacio(a.getDescripcion()));
    }

    private static BigDecimal jlccEscalar(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String jlccVacio(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

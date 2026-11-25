package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
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
public class InmuebleService {
    private final InmuebleRepository repository;
    private final TipoInmuebleRepository tipos;
    private final PropietarioRepository propietarios;
    private final SecuenciaService secuencias;
    private final Permisos permisos;
    private final Clock clock;

    public Page<Inmueble> listar(
            String texto, EstadoInmueble estado, Long tipoId, int pagina, int tamano) {
        return repository.buscar(
                texto == null ? "" : texto.trim(),
                estado,
                tipoId,
                PageRequest.of(Math.max(0, pagina), tamano, Sort.by("codigo")));
    }

    @Transactional
    public String proximoCodigo() {
        return secuencias.formatear("INM-", secuencias.siguiente("INMUEBLE"), 4);
    }

    @Transactional
    public Inmueble guardar(Inmueble a) {
        permisos.exigirGestion();
        if (a.getNombre() == null || a.getNombre().isBlank())
            throw new NegocioException("El nombre del inmueble es obligatorio");
        if (a.getAreaM2() != null && a.getAreaM2().signum() <= 0)
            throw new NegocioException("El área debe ser mayor que cero");
        if (a.getRentaReferencial() != null && a.getRentaReferencial().signum() < 0)
            throw new NegocioException("La renta referencial no puede ser negativa");
        if (a.getNivel() != null && (a.getNivel() < -5 || a.getNivel() > 100))
            throw new NegocioException("El nivel debe estar entre -5 y 100");
        if (a.getOrden() != null && a.getOrden() < 1)
            throw new NegocioException("El orden debe ser mayor o igual a 1");
        boolean nueva = a.getId() == null;
        if (nueva && a.getEstado() == EstadoInmueble.ALQUILADO)
            throw new NegocioException(
                    "El estado ALQUILADO solo puede establecerse desde un contrato");
        if (a.getNivel() != null && a.getOrden() != null) {
            boolean ocupada =
                    nueva
                            ? repository.existsByNivelAndOrdenAndEstadoNot(
                                    a.getNivel(), a.getOrden(), EstadoInmueble.INACTIVO)
                            : repository.existsByNivelAndOrdenAndEstadoNotAndIdNot(
                                    a.getNivel(), a.getOrden(), EstadoInmueble.INACTIVO, a.getId());
            if (ocupada)
                throw new NegocioException(
                        "Ya existe otro inmueble activo en el nivel "
                                + a.getNivel()
                                + " y orden "
                                + a.getOrden());
        }
        Inmueble i =
                nueva
                        ? new Inmueble()
                        : repository
                                .findById(a.getId())
                                .orElseThrow(() -> new NegocioException("El inmueble no existe"));
        if (!nueva
                && a.getEstado() != null
                && i.getEstado() != a.getEstado()
                && (i.getEstado() == EstadoInmueble.ALQUILADO
                        || a.getEstado() == EstadoInmueble.ALQUILADO))
            throw new NegocioException(
                    "No se puede cambiar el estado de un inmueble alquilado manualmente");
        String codigo =
                (a.getCodigo() == null || a.getCodigo().isBlank())
                        ? proximoCodigo()
                        : a.getCodigo().trim().toUpperCase();
        if (nueva
                ? repository.existsByCodigoIgnoreCase(codigo)
                : repository.existsByCodigoIgnoreCaseAndIdNot(codigo, a.getId()))
            throw new NegocioException("Ya existe un inmueble con ese código");
        copiar(a, i);
        i.setCodigo(codigo);
        i.setTipo(
                a.getTipo() == null
                        ? null
                        : tipos.findById(a.getTipo().getId())
                                .orElseThrow(() -> new NegocioException("El tipo no existe")));
        i.setPropietario(
                a.getPropietario() == null
                        ? null
                        : propietarios
                                .findById(a.getPropietario().getId())
                                .orElseThrow(
                                        () -> new NegocioException("El propietario no existe")));
        if (nueva) i.setFechaRegistro(LocalDate.now(clock));
        return repository.save(i);
    }

    private void copiar(Inmueble a, Inmueble i) {
        i.setNombre(a.getNombre().trim());
        if (a.getDireccion() != null) i.setDireccion(vacio(a.getDireccion()));
        if (a.getReferencia() != null) i.setReferencia(vacio(a.getReferencia()));
        if (a.getDistrito() != null) i.setDistrito(vacio(a.getDistrito()));
        if (a.getProvincia() != null) i.setProvincia(vacio(a.getProvincia()));
        if (a.getDepartamento() != null) i.setDepartamento(vacio(a.getDepartamento()));
        if (a.getPiso() != null) i.setPiso(vacio(a.getPiso()));
        if (a.getNivel() != null) i.setNivel(a.getNivel());
        if (a.getOrden() != null) i.setOrden(a.getOrden());
        if (a.getAreaM2() != null) i.setAreaM2(escalar(a.getAreaM2()));
        if (a.getDormitorios() != null) i.setDormitorios(a.getDormitorios());
        if (a.getBanos() != null) i.setBanos(a.getBanos());
        if (a.getEstacionamientos() != null) i.setEstacionamientos(a.getEstacionamientos());
        i.setAmoblado(a.isAmoblado());
        if (a.getRentaReferencial() != null)
            i.setRentaReferencial(escalar(a.getRentaReferencial()));
        if (a.getEstado() != null) i.setEstado(a.getEstado());
        if (a.getServiciosIncluidos() != null)
            i.setServiciosIncluidos(vacio(a.getServiciosIncluidos()));
        if (a.getDescripcion() != null) i.setDescripcion(vacio(a.getDescripcion()));
    }

    private static BigDecimal escalar(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String vacio(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.TipoInmueble;
import com.ejemplo.alquileres.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JlccTipoInmuebleService {
    private final TipoInmuebleRepository jlccRepository;
    private final InmuebleRepository jlccInmuebles;
    private final JlccPermisos jlccPermisos;
    private final JlccAuditoriaService jlccAuditoria;

    public Page<TipoInmueble> jlccListar(String jlccTexto, int jlccPagina, int jlccTamano) {
        return jlccRepository.buscar(
                jlccTexto == null ? "" : jlccTexto.trim(),
                PageRequest.of(Math.max(0, jlccPagina), jlccTamano, Sort.by("nombre")));
    }

    public java.util.List<TipoInmueble> jlccActivos() {
        return jlccRepository.findAll(Sort.by("nombre")).stream()
                .filter(TipoInmueble::isActivo)
                .toList();
    }

    @Transactional
    public TipoInmueble jlccGuardar(TipoInmueble a) {
        jlccPermisos.jlccExigirGestion();
        if (a.getCodigo() == null || a.getCodigo().isBlank())
            throw new JlccNegocioException("El código es obligatorio");
        if (a.getNombre() == null || a.getNombre().isBlank())
            throw new JlccNegocioException("El nombre es obligatorio");
        boolean jlccNueva = a.getId() == null;
        if (jlccRepository.existsByCodigoIgnoreCase(a.getCodigo()) && jlccNueva)
            throw new JlccNegocioException("Ya existe un tipo con ese código");
        TipoInmueble t =
                jlccNueva
                        ? new TipoInmueble()
                        : jlccRepository
                                .findById(a.getId())
                                .orElseThrow(() -> new JlccNegocioException("El tipo no existe"));
        if (!jlccNueva
                && !t.getCodigo().equalsIgnoreCase(a.getCodigo())
                && jlccRepository.existsByCodigoIgnoreCase(a.getCodigo()))
            throw new JlccNegocioException("Ya existe un tipo con ese código");
        t.setCodigo(a.getCodigo().trim().toUpperCase());
        t.setNombre(a.getNombre().trim());
        t.setDescripcion(a.getDescripcion());
        if (jlccNueva) t.setActivo(a.isActivo());
        return jlccRepository.save(t);
    }

    @Transactional(readOnly = true)
    public boolean jlccTieneReferencias(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        return jlccTieneReferenciasInterno(jlccId);
    }

    @Transactional
    public void jlccEliminar(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        TipoInmueble t =
                jlccRepository
                        .findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("El tipo no existe"));
        if (jlccTieneReferenciasInterno(jlccId)) {
            jlccDesactivar(t);
            return;
        }
        jlccRepository.deleteById(jlccId);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "TipoInmueble",
                jlccId,
                "Eliminación definitiva de tipo " + t.getCodigo() + " · " + t.getNombre());
    }

    @Transactional
    public void jlccDesactivar(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        jlccDesactivar(
                jlccRepository
                        .findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("El tipo no existe")));
    }

    private void jlccDesactivar(TipoInmueble t) {
        t.setActivo(false);
        jlccRepository.save(t);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "TipoInmueble",
                t.getId(),
                "Desactivación de tipo " + t.getCodigo());
    }

    @Transactional
    public void jlccReactivar(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        TipoInmueble t =
                jlccRepository
                        .findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("El tipo no existe"));
        if (t.isActivo()) throw new JlccNegocioException("El tipo de inmueble ya está activo");
        t.setActivo(true);
        jlccRepository.save(t);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "TipoInmueble",
                jlccId,
                "Reactivación de tipo " + t.getCodigo());
    }

    private boolean jlccTieneReferenciasInterno(Long jlccId) {
        return jlccInmuebles.existsByTipoId(jlccId);
    }
}

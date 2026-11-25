package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.TipoInmueble;
import com.ejemplo.alquileres.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TipoInmuebleService {
    private final TipoInmuebleRepository repository;
    private final InmuebleRepository inmuebles;
    private final Permisos permisos;
    private final AuditoriaService auditoria;

    public Page<TipoInmueble> listar(String texto, int pagina, int tamano) {
        return repository.buscar(
                texto == null ? "" : texto.trim(),
                PageRequest.of(Math.max(0, pagina), tamano, Sort.by("nombre")));
    }

    public java.util.List<TipoInmueble> activos() {
        return repository.findAll(Sort.by("nombre")).stream()
                .filter(TipoInmueble::isActivo)
                .toList();
    }

    @Transactional
    public TipoInmueble guardar(TipoInmueble a) {
        permisos.exigirGestion();
        if (a.getCodigo() == null || a.getCodigo().isBlank())
            throw new NegocioException("El código es obligatorio");
        if (a.getNombre() == null || a.getNombre().isBlank())
            throw new NegocioException("El nombre es obligatorio");
        boolean nueva = a.getId() == null;
        if (repository.existsByCodigoIgnoreCase(a.getCodigo()) && nueva)
            throw new NegocioException("Ya existe un tipo con ese código");
        TipoInmueble t =
                nueva
                        ? new TipoInmueble()
                        : repository
                                .findById(a.getId())
                                .orElseThrow(() -> new NegocioException("El tipo no existe"));
        if (!nueva
                && !t.getCodigo().equalsIgnoreCase(a.getCodigo())
                && repository.existsByCodigoIgnoreCase(a.getCodigo()))
            throw new NegocioException("Ya existe un tipo con ese código");
        t.setCodigo(a.getCodigo().trim().toUpperCase());
        t.setNombre(a.getNombre().trim());
        t.setDescripcion(a.getDescripcion());
        if (nueva) t.setActivo(a.isActivo());
        return repository.save(t);
    }

    @Transactional(readOnly = true)
    public boolean tieneReferencias(Long id) {
        permisos.exigirGestion();
        return tieneReferenciasInterno(id);
    }

    @Transactional
    public void eliminar(Long id) {
        permisos.exigirGestion();
        TipoInmueble t =
                repository
                        .findById(id)
                        .orElseThrow(() -> new NegocioException("El tipo no existe"));
        if (tieneReferenciasInterno(id)) {
            desactivar(t);
            return;
        }
        repository.deleteById(id);
        auditoria.registrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "TipoInmueble",
                id,
                "Eliminación definitiva de tipo " + t.getCodigo() + " · " + t.getNombre());
    }

    @Transactional
    public void desactivar(Long id) {
        permisos.exigirGestion();
        desactivar(
                repository
                        .findById(id)
                        .orElseThrow(() -> new NegocioException("El tipo no existe")));
    }

    private void desactivar(TipoInmueble t) {
        t.setActivo(false);
        repository.save(t);
        auditoria.registrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "TipoInmueble",
                t.getId(),
                "Desactivación de tipo " + t.getCodigo());
    }

    @Transactional
    public void reactivar(Long id) {
        permisos.exigirGestion();
        TipoInmueble t =
                repository
                        .findById(id)
                        .orElseThrow(() -> new NegocioException("El tipo no existe"));
        if (t.isActivo()) throw new NegocioException("El tipo de inmueble ya está activo");
        t.setActivo(true);
        repository.save(t);
        auditoria.registrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "TipoInmueble",
                id,
                "Reactivación de tipo " + t.getCodigo());
    }

    private boolean tieneReferenciasInterno(Long id) {
        return inmuebles.existsByTipoId(id);
    }
}

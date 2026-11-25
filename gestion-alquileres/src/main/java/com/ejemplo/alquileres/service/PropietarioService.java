package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
@RequiredArgsConstructor
public class PropietarioService {
    private final PropietarioRepository repository;
    private final InmuebleRepository inmuebles;
    private final ValidadorDocumento documentos;
    private final Permisos permisos;
    private final Clock clock;
    private final AuditoriaService auditoria;

    public Page<Propietario> listar(String texto, Boolean activo, int pagina, int tamano) {
        return repository.buscar(
                texto == null ? "" : texto.trim(),
                activo,
                PageRequest.of(Math.max(0, pagina), tamano, Sort.by("nombre")));
    }

    public java.util.List<Propietario> activos() {
        return repository.findAll(Sort.by("nombre")).stream()
                .filter(Propietario::isActivo)
                .toList();
    }

    @Transactional
    public Propietario guardar(Propietario a) {
        permisos.exigirGestion();
        boolean nueva = a.getId() == null;
        if (a.getNombre() == null || a.getNombre().isBlank())
            throw new NegocioException("El nombre o razón social es obligatorio");
        String d = documentos.normalizar(a.getTipoDocumento(), a.getNumeroDocumento());
        boolean existe =
                nueva
                        ? repository.existsByTipoDocumentoAndNumeroDocumento(
                                a.getTipoDocumento(), d)
                        : repository.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(
                                a.getTipoDocumento(), d, a.getId());
        if (existe) throw new NegocioException("Ya existe un propietario con ese documento");
        Propietario p =
                nueva
                        ? new Propietario()
                        : repository
                                .findById(a.getId())
                                .orElseThrow(
                                        () -> new NegocioException("El propietario no existe"));
        p.setTipoDocumento(a.getTipoDocumento());
        p.setNumeroDocumento(d);
        p.setNombre(a.getNombre().trim());
        p.setTelefono(PersonaService.normalizarTelefono(a.getTelefono()));
        p.setEmail(vacio(a.getEmail()));
        p.setBanco(vacio(a.getBanco()));
        p.setCuenta(vacio(a.getCuenta()));
        if (nueva) {
            p.setActivo(a.isActivo());
            p.setFechaRegistro(LocalDate.now(clock));
        }
        return repository.save(p);
    }

    @Transactional(readOnly = true)
    public boolean tieneReferencias(Long id) {
        permisos.exigirGestion();
        return tieneReferenciasInterno(id);
    }

    @Transactional
    public void eliminar(Long id) {
        permisos.exigirGestion();
        Propietario p =
                repository
                        .findById(id)
                        .orElseThrow(() -> new NegocioException("El propietario no existe"));
        if (tieneReferenciasInterno(id)) {
            desactivar(p);
            return;
        }
        repository.deleteById(id);
        auditoria.registrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "Propietario",
                id,
                "Eliminación definitiva de propietario "
                        + p.getNombre()
                        + " ("
                        + p.getTipoDocumento()
                        + " "
                        + p.getNumeroDocumento()
                        + ")");
    }

    @Transactional
    public void desactivar(Long id) {
        permisos.exigirGestion();
        desactivar(
                repository
                        .findById(id)
                        .orElseThrow(() -> new NegocioException("El propietario no existe")));
    }

    private void desactivar(Propietario p) {
        p.setActivo(false);
        repository.save(p);
        auditoria.registrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "Propietario",
                p.getId(),
                "Desactivación de propietario " + p.getNombre());
    }

    @Transactional
    public void reactivar(Long id) {
        permisos.exigirGestion();
        Propietario p =
                repository
                        .findById(id)
                        .orElseThrow(() -> new NegocioException("El propietario no existe"));
        if (p.isActivo()) throw new NegocioException("El propietario ya está activo");
        p.setActivo(true);
        repository.save(p);
        auditoria.registrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "Propietario",
                id,
                "Reactivación de propietario " + p.getNombre());
    }

    private boolean tieneReferenciasInterno(Long id) {
        return inmuebles.existsByPropietarioId(id);
    }

    private static String vacio(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

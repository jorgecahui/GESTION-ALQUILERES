package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
@RequiredArgsConstructor
public class JlccPropietarioService {
    private final PropietarioRepository jlccRepository;
    private final InmuebleRepository jlccInmuebles;
    private final JlccValidadorDocumento jlccDocumentos;
    private final JlccPermisos jlccPermisos;
    private final Clock jlccClock;
    private final JlccAuditoriaService jlccAuditoria;

    public Page<Propietario> jlccListar(String jlccTexto, Boolean jlccActivo, int jlccPagina, int jlccTamano) {
        return jlccRepository.buscar(
                jlccTexto == null ? "" : jlccTexto.trim(),
                jlccActivo,
                PageRequest.of(Math.max(0, jlccPagina), jlccTamano, Sort.by("nombre")));
    }

    public java.util.List<Propietario> jlccActivos() {
        return jlccRepository.findAll(Sort.by("nombre")).stream()
                .filter(Propietario::isActivo)
                .toList();
    }

    @Transactional
    public Propietario jlccGuardar(Propietario a) {
        jlccPermisos.jlccExigirGestion();
        boolean jlccNueva = a.getId() == null;
        if (a.getNombre() == null || a.getNombre().isBlank())
            throw new JlccNegocioException("El nombre o razón social es obligatorio");
        String d = jlccDocumentos.jlccNormalizar(a.getTipoDocumento(), a.getNumeroDocumento());
        boolean jlccExiste =
                jlccNueva
                        ? jlccRepository.existsByTipoDocumentoAndNumeroDocumento(
                                a.getTipoDocumento(), d)
                        : jlccRepository.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(
                                a.getTipoDocumento(), d, a.getId());
        if (jlccExiste) throw new JlccNegocioException("Ya existe un propietario con ese documento");
        Propietario p =
                jlccNueva
                        ? new Propietario()
                        : jlccRepository
                                .findById(a.getId())
                                .orElseThrow(
                                        () -> new JlccNegocioException("El propietario no existe"));
        p.setTipoDocumento(a.getTipoDocumento());
        p.setNumeroDocumento(d);
        p.setNombre(a.getNombre().trim());
        p.setTelefono(JlccPersonaService.jlccNormalizarTelefono(a.getTelefono()));
        p.setEmail(jlccVacio(a.getEmail()));
        p.setBanco(jlccVacio(a.getBanco()));
        p.setCuenta(jlccVacio(a.getCuenta()));
        if (jlccNueva) {
            p.setActivo(a.isActivo());
            p.setFechaRegistro(LocalDate.now(jlccClock));
        }
        return jlccRepository.save(p);
    }

    @Transactional(readOnly = true)
    public boolean jlccTieneReferencias(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        return jlccTieneReferenciasInterno(jlccId);
    }

    @Transactional
    public void jlccEliminar(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        Propietario p =
                jlccRepository
                        .findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("El propietario no existe"));
        if (jlccTieneReferenciasInterno(jlccId)) {
            jlccDesactivar(p);
            return;
        }
        jlccRepository.deleteById(jlccId);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "Propietario",
                jlccId,
                "Eliminación definitiva de propietario "
                        + p.getNombre()
                        + " ("
                        + p.getTipoDocumento()
                        + " "
                        + p.getNumeroDocumento()
                        + ")");
    }

    @Transactional
    public void jlccDesactivar(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        jlccDesactivar(
                jlccRepository
                        .findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("El propietario no existe")));
    }

    private void jlccDesactivar(Propietario p) {
        p.setActivo(false);
        jlccRepository.save(p);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "Propietario",
                p.getId(),
                "Desactivación de propietario " + p.getNombre());
    }

    @Transactional
    public void jlccReactivar(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        Propietario p =
                jlccRepository
                        .findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("El propietario no existe"));
        if (p.isActivo()) throw new JlccNegocioException("El propietario ya está activo");
        p.setActivo(true);
        jlccRepository.save(p);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "Propietario",
                jlccId,
                "Reactivación de propietario " + p.getNombre());
    }

    private boolean jlccTieneReferenciasInterno(Long jlccId) {
        return jlccInmuebles.existsByPropietarioId(jlccId);
    }

    private static String jlccVacio(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

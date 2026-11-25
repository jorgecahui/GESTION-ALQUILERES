package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.PersonaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
@RequiredArgsConstructor
public class JlccPersonaService {
    private final PersonaRepository jlccRepository;
    private final JlccValidadorDocumento jlccDocumentos;
    private final JlccPermisos jlccPermisos;
    private final Clock jlccClock;
    private final JlccAuditoriaService jlccAuditoria;

    public Page<Persona> jlccListar(
            String jlccTexto, TipoDocumento jlccTipo, Boolean jlccActivo, int jlccPagina, int jlccTamano) {
        return jlccRepository.buscar(
                jlccTexto == null ? "" : jlccTexto.trim(),
                jlccTipo,
                jlccActivo,
                PageRequest.of(Math.max(0, jlccPagina), jlccTamano, Sort.by("nombres").ascending()));
    }

    @Transactional
    public Persona jlccGuardar(Persona jlccEntrada) {
        jlccPermisos.jlccExigirGestion();
        boolean jlccNueva = jlccEntrada.getId() == null;
        if (jlccEntrada.getNombres() == null || jlccEntrada.getNombres().isBlank())
            throw new JlccNegocioException("El nombre es obligatorio");
        String jlccDocumento =
                jlccDocumentos.jlccNormalizar(jlccEntrada.getTipoDocumento(), jlccEntrada.getNumeroDocumento());
        boolean jlccExiste =
                jlccNueva
                        ? jlccRepository.existsByTipoDocumentoAndNumeroDocumento(
                                jlccEntrada.getTipoDocumento(), jlccDocumento)
                        : jlccRepository.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(
                                jlccEntrada.getTipoDocumento(), jlccDocumento, jlccEntrada.getId());
        if (jlccExiste) throw new JlccNegocioException("Ya existe una persona con ese documento");
        Persona p =
                jlccNueva
                        ? new Persona()
                        : jlccRepository
                                .findById(jlccEntrada.getId())
                                .orElseThrow(() -> new JlccNegocioException("La persona ya no existe"));
        jlccCopiar(jlccEntrada, p);
        p.setNumeroDocumento(jlccDocumento);
        p.setTelefono(jlccNormalizarTelefono(p.getTelefono()));
        p.setTelefonoSecundario(jlccNormalizarTelefono(p.getTelefonoSecundario()));
        if (jlccNueva) p.setFechaRegistro(LocalDate.now(jlccClock));
        return jlccRepository.save(p);
    }

    @Transactional
    public void jlccDesactivar(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        Persona p =
                jlccRepository
                        .findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("La persona no existe"));
        p.setActivo(false);
        jlccRepository.save(p);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "Persona",
                jlccId,
                "Desactivación de persona " + p.getNombreCompleto());
    }

    @Transactional
    public void jlccReactivar(Long jlccId) {
        jlccPermisos.jlccExigirGestion();
        Persona p =
                jlccRepository
                        .findById(jlccId)
                        .orElseThrow(() -> new JlccNegocioException("La persona no existe"));
        if (p.isActivo()) throw new JlccNegocioException("La persona ya está activa");
        p.setActivo(true);
        jlccRepository.save(p);
        jlccAuditoria.jlccRegistrar(
                AccionAuditoria.CAMBIO_CONFIGURACION,
                "Persona",
                jlccId,
                "Reactivación de persona " + p.getNombreCompleto());
    }

    private void jlccCopiar(Persona a, Persona b) {
        b.setTipoDocumento(a.getTipoDocumento());
        b.setNombres(a.getNombres().trim());
        if (a.getApellidos() != null) b.setApellidos(jlccVacio(a.getApellidos()));
        if (a.getEmail() != null) b.setEmail(jlccVacio(a.getEmail()));
        if (a.getTelefono() != null) b.setTelefono(a.getTelefono());
        if (a.getTelefonoSecundario() != null) b.setTelefonoSecundario(a.getTelefonoSecundario());
        if (a.getDireccion() != null) b.setDireccion(jlccVacio(a.getDireccion()));
        if (a.getOcupacion() != null) b.setOcupacion(jlccVacio(a.getOcupacion()));
        if (a.getObservaciones() != null) b.setObservaciones(jlccVacio(a.getObservaciones()));
        if (a.getId() == null) b.setActivo(a.isActivo());
    }

    static String jlccNormalizarTelefono(String jlccValor) {
        if (jlccValor == null || jlccValor.isBlank()) return null;
        String n = jlccValor.replaceAll("[\\s()-]", "");
        if (n.startsWith("+51")) n = n.substring(3);
        else if (n.startsWith("0051")) n = n.substring(4);
        if (n.matches("9\\d{8}")) return "+51 " + n;
        if (n.matches("(?:0?1)?\\d{6,8}")) return n;
        throw new JlccNegocioException("El teléfono debe ser un móvil peruano válido o un fijo");
    }

    private static String jlccVacio(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}

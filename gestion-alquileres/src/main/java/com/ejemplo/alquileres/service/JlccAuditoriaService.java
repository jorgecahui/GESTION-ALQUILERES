package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.AuditoriaEvento;
import com.ejemplo.alquileres.repository.AuditoriaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JlccAuditoriaService {
    private final AuditoriaRepository jlccRepository;
    private final JlccSesionActual jlccSesion;
    private final JlccPermisos jlccPermisos;
    private final Clock jlccClock;

    @Transactional
    public AuditoriaEvento jlccRegistrar(
            AccionAuditoria jlccAccion, String jlccEntidad, Long jlccEntidadId, String jlccDetalle) {
        return jlccGuardar(jlccUsuarioActual(), jlccAccion, jlccEntidad, jlccEntidadId, jlccDetalle);
    }

    @Transactional
    public AuditoriaEvento jlccRegistrarComo(
            String jlccUsuario,
            AccionAuditoria jlccAccion,
            String jlccEntidad,
            Long jlccEntidadId,
            String jlccDetalle) {
        return jlccGuardar(
                jlccUsuario == null || jlccUsuario.isBlank() ? "sistema" : jlccUsuario,
                jlccAccion,
                jlccEntidad,
                jlccEntidadId,
                jlccDetalle);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaEvento> jlccListar(
            LocalDateTime jlccDesde, LocalDateTime jlccHasta, String jlccUsuario, AccionAuditoria jlccAccion) {
        jlccPermisos.jlccExigirAdmin();
        String jlccFiltroUsuario = jlccUsuario == null || jlccUsuario.isBlank() ? null : jlccUsuario.trim();
        return jlccRepository.buscar(jlccDesde, jlccHasta, jlccFiltroUsuario, jlccAccion);
    }

    public List<AccionAuditoria> jlccAcciones() {
        return List.of(AccionAuditoria.values());
    }

    static String jlccSanitizar(String jlccDetalle) {
        if (jlccDetalle == null) return null;
        String jlccLimpio =
                jlccDetalle.replaceAll(
                                "(?i)(contrase(?:n|ñ)a|password(?:_hash)?|clave|token)\\s*[:=]\\s*[^,;\\s]+",
                                "$1: ***")
                        .replaceAll("[\\r\\n]+", " ")
                        .trim();
        return jlccLimpio.length() > 500 ? jlccLimpio.substring(0, 500) : jlccLimpio;
    }

    private AuditoriaEvento jlccGuardar(
            String jlccUsuario,
            AccionAuditoria jlccAccion,
            String jlccEntidad,
            Long jlccEntidadId,
            String jlccDetalle) {
        if (jlccAccion == null)
            throw new IllegalArgumentException("La acción de auditoría es obligatoria.");
        return jlccRepository.save(
                AuditoriaEvento.builder()
                        .fecha(LocalDateTime.now(jlccClock))
                        .usuario(jlccUsuario == null || jlccUsuario.isBlank() ? "sistema" : jlccUsuario)
                        .accion(jlccAccion)
                        .entidad(jlccEntidad)
                        .entidadId(jlccEntidadId)
                        .detalle(jlccSanitizar(jlccDetalle))
                        .build());
    }

    private String jlccUsuarioActual() {
        String jlccUsername = jlccSesion.jlccUsername();
        return jlccUsername == null || jlccUsername.isBlank() ? "sistema" : jlccUsername;
    }
}

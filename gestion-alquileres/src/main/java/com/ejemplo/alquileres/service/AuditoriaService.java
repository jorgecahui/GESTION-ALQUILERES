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
public class AuditoriaService {
    private final AuditoriaRepository repository;
    private final SesionActual sesion;
    private final Permisos permisos;
    private final Clock clock;

    @Transactional
    public AuditoriaEvento registrar(
            AccionAuditoria accion, String entidad, Long entidadId, String detalle) {
        return guardar(usuarioActual(), accion, entidad, entidadId, detalle);
    }

    @Transactional
    public AuditoriaEvento registrarComo(
            String usuario,
            AccionAuditoria accion,
            String entidad,
            Long entidadId,
            String detalle) {
        return guardar(
                usuario == null || usuario.isBlank() ? "sistema" : usuario,
                accion,
                entidad,
                entidadId,
                detalle);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaEvento> listar(
            LocalDateTime desde, LocalDateTime hasta, String usuario, AccionAuditoria accion) {
        permisos.exigirAdmin();
        String filtroUsuario = usuario == null || usuario.isBlank() ? null : usuario.trim();
        return repository.buscar(desde, hasta, filtroUsuario, accion);
    }

    public List<AccionAuditoria> acciones() {
        return List.of(AccionAuditoria.values());
    }

    static String sanitizar(String detalle) {
        if (detalle == null) return null;
        String limpio =
                detalle.replaceAll(
                                "(?i)(contrase(?:n|ñ)a|password(?:_hash)?|clave|token)\\s*[:=]\\s*[^,;\\s]+",
                                "$1: ***")
                        .replaceAll("[\\r\\n]+", " ")
                        .trim();
        return limpio.length() > 500 ? limpio.substring(0, 500) : limpio;
    }

    private AuditoriaEvento guardar(
            String usuario,
            AccionAuditoria accion,
            String entidad,
            Long entidadId,
            String detalle) {
        if (accion == null)
            throw new IllegalArgumentException("La acción de auditoría es obligatoria.");
        return repository.save(
                AuditoriaEvento.builder()
                        .fecha(LocalDateTime.now(clock))
                        .usuario(usuario == null || usuario.isBlank() ? "sistema" : usuario)
                        .accion(accion)
                        .entidad(entidad)
                        .entidadId(entidadId)
                        .detalle(sanitizar(detalle))
                        .build());
    }

    private String usuarioActual() {
        String username = sesion.username();
        return username == null || username.isBlank() ? "sistema" : username;
    }
}

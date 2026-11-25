package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.RolUsuario;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JlccPermisos {
    public static final String JLCC_SIN_PERMISO = "No tiene permiso para esta acción.";

    private final JlccSesionActual jlccSesion;

    public RolUsuario jlccRol() {
        return jlccSesion.jlccRol();
    }

    public boolean jlccEsAdmin() {
        return jlccSesion.jlccRol() == RolUsuario.ADMIN;
    }

    public boolean jlccPuedeGestionar() {
        RolUsuario jlccRol = jlccSesion.jlccRol();
        return jlccRol == RolUsuario.ADMIN || jlccRol == RolUsuario.OPERADOR;
    }

    public boolean jlccPuedeConsultar() {
        return jlccSesion.jlccHaySesion();
    }

    public void jlccExigirGestion() {
        if (!jlccPuedeGestionar()) throw new JlccNegocioException(JLCC_SIN_PERMISO);
    }

    public void jlccExigirAdmin() {
        if (!jlccEsAdmin()) throw new JlccNegocioException(JLCC_SIN_PERMISO);
    }
}

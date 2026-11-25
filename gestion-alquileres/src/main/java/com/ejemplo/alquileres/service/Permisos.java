package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.RolUsuario;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Permisos {
    public static final String SIN_PERMISO = "No tiene permiso para esta acción.";

    private final SesionActual sesion;

    public RolUsuario rol() {
        return sesion.rol();
    }

    public boolean esAdmin() {
        return sesion.rol() == RolUsuario.ADMIN;
    }

    public boolean puedeGestionar() {
        RolUsuario rol = sesion.rol();
        return rol == RolUsuario.ADMIN || rol == RolUsuario.OPERADOR;
    }

    public boolean puedeConsultar() {
        return sesion.haySesion();
    }

    public void exigirGestion() {
        if (!puedeGestionar()) throw new NegocioException(SIN_PERMISO);
    }

    public void exigirAdmin() {
        if (!esAdmin()) throw new NegocioException(SIN_PERMISO);
    }
}

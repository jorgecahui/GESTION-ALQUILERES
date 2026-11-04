package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.RolUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Centraliza la matriz de permisos por rol. */
@Component
@RequiredArgsConstructor
public class Permisos {
    public static final String SIN_PERMISO = "No tiene permiso para esta acción.";

    private final SesionActual sesion;

    public RolUsuario rol() { return sesion.rol(); }
    public boolean esAdmin() { return sesion.rol() == RolUsuario.ADMIN; }
    public boolean puedeGestionar() {
        RolUsuario rol = sesion.rol();
        return rol == RolUsuario.ADMIN || rol == RolUsuario.OPERADOR;
    }
    public boolean puedeConsultar() { return sesion.haySesion(); }

    /** Crear/editar catálogos, contratos, pagos y mantenimiento. */
    public void exigirGestion() {
        if (!puedeGestionar()) throw new NegocioException(SIN_PERMISO);
    }

    /** Anular pagos, rescindir/finalizar contratos, condonar mora, configuración, usuarios y respaldo. */
    public void exigirAdmin() {
        if (!esAdmin()) throw new NegocioException(SIN_PERMISO);
    }
}

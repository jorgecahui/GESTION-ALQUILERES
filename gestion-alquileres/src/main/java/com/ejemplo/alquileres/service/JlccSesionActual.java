package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;

import org.springframework.stereotype.Component;

@Component
public class JlccSesionActual {
    private volatile Usuario jlccUsuario;

    public Usuario jlccUsuario() {
        return jlccUsuario;
    }

    public void jlccIniciar(Usuario jlccUsuario) {
        this.jlccUsuario = jlccUsuario;
    }

    public void jlccCerrar() {
        this.jlccUsuario = null;
    }

    public boolean jlccHaySesion() {
        return jlccUsuario != null;
    }

    public RolUsuario jlccRol() {
        return jlccUsuario == null ? null : jlccUsuario.getRol();
    }

    public String jlccUsername() {
        return jlccUsuario == null ? null : jlccUsuario.getUsername();
    }
}

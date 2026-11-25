package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;

import org.springframework.stereotype.Component;

@Component
public class SesionActual {
    private volatile Usuario usuario;

    public Usuario usuario() {
        return usuario;
    }

    public void iniciar(Usuario usuario) {
        this.usuario = usuario;
    }

    public void cerrar() {
        this.usuario = null;
    }

    public boolean haySesion() {
        return usuario != null;
    }

    public RolUsuario rol() {
        return usuario == null ? null : usuario.getRol();
    }

    public String username() {
        return usuario == null ? null : usuario.getUsername();
    }
}

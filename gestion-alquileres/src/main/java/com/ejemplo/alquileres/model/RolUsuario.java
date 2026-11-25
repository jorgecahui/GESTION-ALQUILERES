package com.ejemplo.alquileres.model;

public enum RolUsuario {
    ADMIN("Administrador"),
    OPERADOR("Operador"),
    CONSULTA("Consulta");

    private final String etiqueta;

    RolUsuario(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}

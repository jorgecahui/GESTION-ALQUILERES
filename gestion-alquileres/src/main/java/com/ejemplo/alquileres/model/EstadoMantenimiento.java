package com.ejemplo.alquileres.model;

public enum EstadoMantenimiento {
    REPORTADO("Reportado"),
    EN_PROCESO("En proceso"),
    RESUELTO("Resuelto"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoMantenimiento(String etiqueta) {
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

package com.ejemplo.alquileres.model;

public enum EstadoInmueble {
    DISPONIBLE("Disponible"),
    ALQUILADO("Alquilado"),
    MANTENIMIENTO("Mantenimiento"),
    INACTIVO("Inactivo");

    private final String etiqueta;

    EstadoInmueble(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
    @Override public String toString() { return etiqueta; }
}

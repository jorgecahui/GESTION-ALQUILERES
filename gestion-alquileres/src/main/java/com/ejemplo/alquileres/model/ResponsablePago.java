package com.ejemplo.alquileres.model;

public enum ResponsablePago {
    PROPIETARIO("Propietario"),
    ARRENDATARIO("Arrendatario");

    private final String etiqueta;

    ResponsablePago(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
    @Override public String toString() { return etiqueta; }
}

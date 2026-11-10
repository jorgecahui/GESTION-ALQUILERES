package com.ejemplo.alquileres.model;

public enum EstadoCuota {
    PENDIENTE("Pendiente"),
    PARCIAL("Parcial"),
    PAGADA("Pagada"),
    ANULADA("Anulada");

    private final String etiqueta;

    EstadoCuota(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
    @Override public String toString() { return etiqueta; }
}

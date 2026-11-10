package com.ejemplo.alquileres.model;

public enum EstadoPago {
    EMITIDO("Emitido"),
    ANULADO("Anulado");

    private final String etiqueta;

    EstadoPago(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
    @Override public String toString() { return etiqueta; }
}

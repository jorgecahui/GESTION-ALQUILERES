package com.ejemplo.alquileres.model;

public enum EstadoContrato {
    VIGENTE("Vigente"),
    FINALIZADO("Finalizado"),
    RESCINDIDO("Rescindido");

    private final String etiqueta;

    EstadoContrato(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
    @Override public String toString() { return etiqueta; }
}

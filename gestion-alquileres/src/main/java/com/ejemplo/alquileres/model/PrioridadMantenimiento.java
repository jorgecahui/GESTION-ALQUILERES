package com.ejemplo.alquileres.model;

public enum PrioridadMantenimiento {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta"),
    URGENTE("Urgente");

    private final String etiqueta;

    PrioridadMantenimiento(String etiqueta) {
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

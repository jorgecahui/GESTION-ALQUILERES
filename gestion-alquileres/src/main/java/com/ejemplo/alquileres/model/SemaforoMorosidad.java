package com.ejemplo.alquileres.model;

public enum SemaforoMorosidad {
    AL_DIA("Al día"),
    ATRASO_LEVE("Atraso leve"),
    MOROSO("Moroso");

    private final String etiqueta;

    SemaforoMorosidad(String etiqueta) {
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

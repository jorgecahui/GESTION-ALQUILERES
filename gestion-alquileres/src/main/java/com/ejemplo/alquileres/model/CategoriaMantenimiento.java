package com.ejemplo.alquileres.model;

public enum CategoriaMantenimiento {
    PLOMERIA("Plomería"),
    ELECTRICIDAD("Electricidad"),
    PINTURA("Pintura"),
    CARPINTERIA("Carpintería"),
    CERRAJERIA("Cerrajería"),
    LIMPIEZA("Limpieza"),
    OTROS("Otros");

    private final String etiqueta;

    CategoriaMantenimiento(String etiqueta) {
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

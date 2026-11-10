package com.ejemplo.alquileres.model;

public enum TipoMora {
    PORCENTAJE_DIARIO("Porcentaje diario"),
    MONTO_DIARIO("Monto diario");

    private final String etiqueta;

    TipoMora(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
    @Override public String toString() { return etiqueta; }
}

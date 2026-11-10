package com.ejemplo.alquileres.model;

public enum TipoRecordatorio {
    PREVENTIVO("Preventivo"),
    VENCIDO("Vencido"),
    FIN_CONTRATO("Fin de contrato");

    private final String etiqueta;

    TipoRecordatorio(String etiqueta) { this.etiqueta = etiqueta; }
    public String getEtiqueta() { return etiqueta; }
    @Override public String toString() { return etiqueta; }
}

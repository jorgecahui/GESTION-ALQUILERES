package com.ejemplo.alquileres.model;

public enum MedioPago {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    DEPOSITO("Depósito"),
    YAPE("Yape"),
    PLIN("Plin"),
    CHEQUE("Cheque"),
    OTRO("Otro");

    private final String etiqueta;

    MedioPago(String etiqueta) {
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

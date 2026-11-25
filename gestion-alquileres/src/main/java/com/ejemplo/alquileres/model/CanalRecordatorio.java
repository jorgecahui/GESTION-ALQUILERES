package com.ejemplo.alquileres.model;

public enum CanalRecordatorio {
    WHATSAPP("WhatsApp"),
    COPIADO("Copiado");

    private final String etiqueta;

    CanalRecordatorio(String etiqueta) {
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

package com.ejemplo.alquileres.ui;

import javafx.scene.control.Label;

public final class JlccChips {
    private JlccChips() {}

    public static Label jlccCrear(String jlccTexto, JlccTipo jlccTipo) {
        Label jlccChip = new Label(jlccTexto);
        jlccChip.getStyleClass().addAll("chip", jlccTipo.jlccClase);
        return jlccChip;
    }

    public enum JlccTipo {
        EXITO("chip-exito"),
        ACENTO("chip-acento"),
        AVISO("chip-aviso"),
        PELIGRO("chip-peligro"),
        NEUTRO("chip-neutro");
        private final String jlccClase;

        JlccTipo(String jlccClase) {
            this.jlccClase = jlccClase;
        }
    }
}

package com.ejemplo.alquileres.ui;

import javafx.scene.control.Label;

public final class Chips {
    private Chips() {}

    public static Label crear(String texto, Tipo tipo) {
        Label chip = new Label(texto);
        chip.getStyleClass().addAll("chip", tipo.clase);
        return chip;
    }

    public enum Tipo {
        EXITO("chip-exito"),
        ACENTO("chip-acento"),
        AVISO("chip-aviso"),
        PELIGRO("chip-peligro"),
        NEUTRO("chip-neutro");
        private final String clase;

        Tipo(String clase) {
            this.clase = clase;
        }
    }
}

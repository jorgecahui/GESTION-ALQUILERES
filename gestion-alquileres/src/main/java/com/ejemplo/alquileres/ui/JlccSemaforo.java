package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.model.SemaforoMorosidad;

import javafx.scene.control.Label;

public final class JlccSemaforo {
    private JlccSemaforo() {}

    public static Label jlccCrear(SemaforoMorosidad s) {
        Label l = new Label(jlccTexto(s));
        l.getStyleClass().addAll("chip", "semaforo", jlccEstilo(s));
        return l;
    }

    public static String jlccTexto(SemaforoMorosidad s) {
        return switch (s == null ? SemaforoMorosidad.AL_DIA : s) {
            case AL_DIA -> "Al día";
            case ATRASO_LEVE -> "Atraso leve";
            case MOROSO -> "Moroso";
        };
    }

    public static String jlccEstilo(SemaforoMorosidad s) {
        return switch (s == null ? SemaforoMorosidad.AL_DIA : s) {
            case AL_DIA -> "exito";
            case ATRASO_LEVE -> "advertencia";
            case MOROSO -> "peligro";
        };
    }
}

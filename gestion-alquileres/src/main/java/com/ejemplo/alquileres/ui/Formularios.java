package com.ejemplo.alquileres.ui;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

import java.util.regex.Pattern;

public final class Formularios {
    private Formularios() {}

    public static void soloDigitos(TextInputControl campo, int maximo) {
        campo.setTextFormatter(
                new TextFormatter<>(
                        c -> c.getControlNewText().matches("\\d{0," + maximo + "}") ? c : null));
    }

    public static void soloDecimal(TextInputControl campo) {
        soloDecimal(campo, 2);
    }

    public static void soloDecimal(TextInputControl campo, int decimales) {
        Pattern patron = Pattern.compile("\\d{0,8}([.,]\\d{0," + decimales + "})?");
        campo.setTextFormatter(
                new TextFormatter<>(
                        c -> patron.matcher(c.getControlNewText()).matches() ? c : null));
    }
}

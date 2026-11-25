package com.ejemplo.alquileres.ui;

import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

import java.util.regex.Pattern;

public final class JlccFormularios {
    private JlccFormularios() {}

    public static void jlccSoloDigitos(TextInputControl jlccCampo, int jlccMaximo) {
        jlccCampo.setTextFormatter(
                new TextFormatter<>(
                        c -> c.getControlNewText().matches("\\d{0," + jlccMaximo + "}") ? c : null));
    }

    public static void jlccSoloDecimal(TextInputControl jlccCampo) {
        jlccSoloDecimal(jlccCampo, 2);
    }

    public static void jlccSoloDecimal(TextInputControl jlccCampo, int jlccDecimales) {
        Pattern jlccPatron = Pattern.compile("\\d{0,8}([.,]\\d{0," + jlccDecimales + "})?");
        jlccCampo.setTextFormatter(
                new TextFormatter<>(
                        c -> jlccPatron.matcher(c.getControlNewText()).matches() ? c : null));
    }
}

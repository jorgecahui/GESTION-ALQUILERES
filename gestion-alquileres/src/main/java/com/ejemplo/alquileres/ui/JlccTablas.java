package com.ejemplo.alquileres.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class JlccTablas {
    public static final NumberFormat JLCC_MONEDA =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"));

    private JlccTablas() {}

    public static String jlccMoneda(BigDecimal jlccMonto) {
        return jlccMonto == null ? "" : JLCC_MONEDA.format(jlccMonto);
    }
}

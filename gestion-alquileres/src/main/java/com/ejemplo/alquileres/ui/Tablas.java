package com.ejemplo.alquileres.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class Tablas {
    public static final NumberFormat MONEDA =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"));

    private Tablas() {}

    public static String moneda(BigDecimal monto) {
        return monto == null ? "" : MONEDA.format(monto);
    }
}

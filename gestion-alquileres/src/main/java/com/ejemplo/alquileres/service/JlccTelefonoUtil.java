package com.ejemplo.alquileres.service;

public final class JlccTelefonoUtil {
    private JlccTelefonoUtil() {}

    public static String jlccNormalizarPeru(String jlccValor) {
        if (jlccValor == null || jlccValor.isBlank()) return null;
        String jlccLimpio = jlccValor.trim().replaceAll("[\\s()\\-.]", "");
        if (jlccLimpio.startsWith("+")) jlccLimpio = jlccLimpio.substring(1);
        if (jlccLimpio.startsWith("0051")) jlccLimpio = jlccLimpio.substring(2);
        if (jlccLimpio.matches("9\\d{8}")) return "51" + jlccLimpio;
        if (jlccLimpio.matches("519\\d{8}")) return jlccLimpio;
        return null;
    }

    public static String jlccLegible(String jlccValor) {
        String jlccNormalizado = jlccNormalizarPeru(jlccValor);
        if (jlccNormalizado == null) return jlccValor == null ? "" : jlccValor;
        String jlccNumero = jlccNormalizado.substring(2);
        return "+51 "
                + jlccNumero.substring(0, 3)
                + " "
                + jlccNumero.substring(3, 6)
                + " "
                + jlccNumero.substring(6);
    }
}

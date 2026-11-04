package com.ejemplo.alquileres.service;

/** Normaliza teléfonos peruanos al formato internacional usado por wa.me. */
public final class TelefonoUtil {
    private TelefonoUtil() { }

    /**
     * Acepta 987654321, +51987654321, 51987654321, 0051 987 654 321 y variantes
     * con espacios, guiones o paréntesis. Devuelve 51987654321 o null si no es válido.
     */
    public static String normalizarPeru(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.trim().replaceAll("[\\s()\\-.]", "");
        if (limpio.startsWith("+")) limpio = limpio.substring(1);
        if (limpio.startsWith("0051")) limpio = limpio.substring(2);
        if (limpio.matches("9\\d{8}")) return "51" + limpio;
        if (limpio.matches("519\\d{8}")) return limpio;
        return null;
    }

    /** Formato legible para la interfaz: +51 987 654 321. */
    public static String legible(String valor) {
        String normalizado = normalizarPeru(valor);
        if (normalizado == null) return valor == null ? "" : valor;
        String numero = normalizado.substring(2);
        return "+51 " + numero.substring(0, 3) + " " + numero.substring(3, 6) + " " + numero.substring(6);
    }
}

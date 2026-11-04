package com.ejemplo.alquileres.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Convierte un importe a la leyenda usada por los recibos internos. */
public final class NumeroALetras {
    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};
    private NumeroALetras() { }
    public static String importe(BigDecimal valor) {
        BigDecimal v = valor.setScale(2, RoundingMode.HALF_UP);
        long entero = v.longValue();
        int centimos = v.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        return (entero == 0 ? "CERO" : convertir(entero)) + " CON " + String.format("%02d", centimos) + "/100 SOLES";
    }
    static String convertir(long numero) {
        if (numero >= 1_000_000) { long millones = numero / 1_000_000, resto = numero % 1_000_000; String texto = millones == 1 ? "UN MILLON" : apocopar(convertir(millones)) + " MILLONES"; return resto == 0 ? texto : texto + " " + convertir(resto); }
        if (numero >= 1_000) { long miles = numero / 1_000, resto = numero % 1_000; String texto = miles == 1 ? "MIL" : apocopar(convertir(miles)) + " MIL"; return resto == 0 ? texto : texto + " " + convertir(resto); }
        if (numero == 100) return "CIEN";
        int centenas = (int) numero / 100, resto = (int) numero % 100;
        String decenas = resto < 30 ? UNIDADES[resto] : DECENAS[resto / 10] + (resto % 10 == 0 ? "" : " Y " + UNIDADES[resto % 10]);
        return (CENTENAS[centenas] + " " + decenas).trim();
    }
    private static String apocopar(String texto) { return texto.endsWith("UNO") ? texto.substring(0, texto.length() - 1) : texto; }
}

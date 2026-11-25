package com.ejemplo.alquileres.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class JlccNumeroALetras {
    private static final String[] JLCC_UNIDADES = {
        "",
        "UNO",
        "DOS",
        "TRES",
        "CUATRO",
        "CINCO",
        "SEIS",
        "SIETE",
        "OCHO",
        "NUEVE",
        "DIEZ",
        "ONCE",
        "DOCE",
        "TRECE",
        "CATORCE",
        "QUINCE",
        "DIECISEIS",
        "DIECISIETE",
        "DIECIOCHO",
        "DIECINUEVE",
        "VEINTE",
        "VEINTIUNO",
        "VEINTIDOS",
        "VEINTITRES",
        "VEINTICUATRO",
        "VEINTICINCO",
        "VEINTISEIS",
        "VEINTISIETE",
        "VEINTIOCHO",
        "VEINTINUEVE"
    };
    private static final String[] JLCC_DECENAS = {
        "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] JLCC_CENTENAS = {
        "",
        "CIENTO",
        "DOSCIENTOS",
        "TRESCIENTOS",
        "CUATROCIENTOS",
        "QUINIENTOS",
        "SEISCIENTOS",
        "SETECIENTOS",
        "OCHOCIENTOS",
        "NOVECIENTOS"
    };

    private JlccNumeroALetras() {}

    public static String jlccImporte(BigDecimal jlccValor) {
        BigDecimal v = jlccValor.setScale(2, RoundingMode.HALF_UP);
        long jlccEntero = v.longValue();
        int jlccCentimos = v.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        return (jlccEntero == 0 ? "CERO" : jlccConvertir(jlccEntero))
                + " CON "
                + String.format("%02d", jlccCentimos)
                + "/100 SOLES";
    }

    static String jlccConvertir(long jlccNumero) {
        if (jlccNumero >= 1_000_000) {
            long jlccMillones = jlccNumero / 1_000_000, jlccResto = jlccNumero % 1_000_000;
            String jlccTexto =
                    jlccMillones == 1 ? "UN MILLON" : jlccApocopar(jlccConvertir(jlccMillones)) + " MILLONES";
            return jlccResto == 0 ? jlccTexto : jlccTexto + " " + jlccConvertir(jlccResto);
        }
        if (jlccNumero >= 1_000) {
            long jlccMiles = jlccNumero / 1_000, jlccResto = jlccNumero % 1_000;
            String jlccTexto = jlccMiles == 1 ? "MIL" : jlccApocopar(jlccConvertir(jlccMiles)) + " MIL";
            return jlccResto == 0 ? jlccTexto : jlccTexto + " " + jlccConvertir(jlccResto);
        }
        if (jlccNumero == 100) return "CIEN";
        int jlccCentenas = (int) jlccNumero / 100, jlccResto = (int) jlccNumero % 100;
        String jlccDecenas =
                jlccResto < 30
                        ? JLCC_UNIDADES[jlccResto]
                        : JLCC_DECENAS[jlccResto / 10]
                                + (jlccResto % 10 == 0 ? "" : " Y " + JLCC_UNIDADES[jlccResto % 10]);
        return (JLCC_CENTENAS[jlccCentenas] + " " + jlccDecenas).trim();
    }

    private static String jlccApocopar(String jlccTexto) {
        return jlccTexto.endsWith("UNO") ? jlccTexto.substring(0, jlccTexto.length() - 1) : jlccTexto;
    }
}

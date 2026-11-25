package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class NumeroALetrasTest {
    @Test
    void convierteImportesRepresentativos() {
        assertEquals("CERO CON 00/100 SOLES", JlccNumeroALetras.jlccImporte(BigDecimal.ZERO));
        assertEquals("UNO CON 00/100 SOLES", JlccNumeroALetras.jlccImporte(BigDecimal.ONE));
        assertEquals("QUINCE CON 00/100 SOLES", JlccNumeroALetras.jlccImporte(new BigDecimal("15")));
        assertEquals("CIEN CON 00/100 SOLES", JlccNumeroALetras.jlccImporte(new BigDecimal("100")));
        assertEquals(
                "MIL DOSCIENTOS CINCUENTA CON 50/100 SOLES",
                JlccNumeroALetras.jlccImporte(new BigDecimal("1250.50")));
        assertEquals(
                "VEINTIUN MIL CON 00/100 SOLES", JlccNumeroALetras.jlccImporte(new BigDecimal("21000")));
    }
}

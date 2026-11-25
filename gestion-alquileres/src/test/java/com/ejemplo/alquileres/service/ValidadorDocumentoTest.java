package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.*;

import com.ejemplo.alquileres.model.TipoDocumento;

import org.junit.jupiter.api.*;

class ValidadorDocumentoTest {
    private final JlccValidadorDocumento validador = new JlccValidadorDocumento();

    @Test
    void aceptaDocumentosValidos() {
        assertEquals("12345678", validador.jlccNormalizar(TipoDocumento.DNI, "12345678"));
        assertTrue(validador.jlccEsValido(TipoDocumento.RUC, "20123456789"));
        assertTrue(validador.jlccEsValido(TipoDocumento.CE, "CE1234567"));
        assertTrue(validador.jlccEsValido(TipoDocumento.PASAPORTE, "AB123456"));
    }

    @Test
    void rechazaLongitudesPrefijosYCaracteresInvalidos() {
        assertFalse(validador.jlccEsValido(TipoDocumento.DNI, "1234567"));
        assertFalse(validador.jlccEsValido(TipoDocumento.RUC, "11123456789"));
        assertFalse(validador.jlccEsValido(TipoDocumento.CE, "ABC-123"));
        assertFalse(validador.jlccEsValido(TipoDocumento.PASAPORTE, "A12"));
    }
}

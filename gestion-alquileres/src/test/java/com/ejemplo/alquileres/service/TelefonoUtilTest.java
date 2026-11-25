package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TelefonoUtilTest {
    @Test
    void normalizaNumerosPeruanosValidos() {
        assertEquals("51987654321", JlccTelefonoUtil.jlccNormalizarPeru("987654321"));
        assertEquals("51987654321", JlccTelefonoUtil.jlccNormalizarPeru("+51987654321"));
        assertEquals("51987654321", JlccTelefonoUtil.jlccNormalizarPeru("51987654321"));
        assertEquals("51987654321", JlccTelefonoUtil.jlccNormalizarPeru("0051 987 654 321"));
        assertEquals("51987654321", JlccTelefonoUtil.jlccNormalizarPeru("+51 (987) 654-321"));
    }

    @Test
    void rechazaNumerosInvalidosOVacios() {
        assertNull(JlccTelefonoUtil.jlccNormalizarPeru("0987654321"));
        assertNull(JlccTelefonoUtil.jlccNormalizarPeru(""));
        assertNull(JlccTelefonoUtil.jlccNormalizarPeru("   "));
        assertNull(JlccTelefonoUtil.jlccNormalizarPeru(null));
        assertNull(JlccTelefonoUtil.jlccNormalizarPeru("123456789"));
        assertNull(JlccTelefonoUtil.jlccNormalizarPeru("+51 123 456 789"));
    }

    @Test
    void presentaElNumeroEnFormatoLegible() {
        assertEquals("+51 987 654 321", JlccTelefonoUtil.jlccLegible("987654321"));
        assertEquals("", JlccTelefonoUtil.jlccLegible(null));
        assertEquals("texto", JlccTelefonoUtil.jlccLegible("texto"));
    }
}

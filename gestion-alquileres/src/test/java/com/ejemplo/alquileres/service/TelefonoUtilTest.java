package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TelefonoUtilTest {
    @Test
    void normalizaNumerosPeruanosValidos() {
        assertEquals("51987654321", TelefonoUtil.normalizarPeru("987654321"));
        assertEquals("51987654321", TelefonoUtil.normalizarPeru("+51987654321"));
        assertEquals("51987654321", TelefonoUtil.normalizarPeru("51987654321"));
        assertEquals("51987654321", TelefonoUtil.normalizarPeru("0051 987 654 321"));
        assertEquals("51987654321", TelefonoUtil.normalizarPeru("+51 (987) 654-321"));
    }

    @Test
    void rechazaNumerosInvalidosOVacios() {
        assertNull(TelefonoUtil.normalizarPeru("0987654321"));
        assertNull(TelefonoUtil.normalizarPeru(""));
        assertNull(TelefonoUtil.normalizarPeru("   "));
        assertNull(TelefonoUtil.normalizarPeru(null));
        assertNull(TelefonoUtil.normalizarPeru("123456789"));
        assertNull(TelefonoUtil.normalizarPeru("+51 123 456 789"));
    }

    @Test
    void presentaElNumeroEnFormatoLegible() {
        assertEquals("+51 987 654 321", TelefonoUtil.legible("987654321"));
        assertEquals("", TelefonoUtil.legible(null));
        assertEquals("texto", TelefonoUtil.legible("texto"));
    }
}

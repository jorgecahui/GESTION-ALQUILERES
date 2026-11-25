package com.ejemplo.alquileres.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Set;

class NivelVistaTest {

    @Test
    void transicionesPermitidas() {
        assertTrue(JlccNivelVista.EDIFICIO.jlccPuedeIrA(JlccNivelVista.PISO));
        assertTrue(JlccNivelVista.PISO.jlccPuedeIrA(JlccNivelVista.UNIDAD));
        assertTrue(JlccNivelVista.PISO.jlccPuedeIrA(JlccNivelVista.EDIFICIO));
        assertTrue(JlccNivelVista.UNIDAD.jlccPuedeIrA(JlccNivelVista.PISO));
        assertTrue(JlccNivelVista.UNIDAD.jlccPuedeIrA(JlccNivelVista.EDIFICIO));
    }

    @Test
    void transicionesInvalidas() {
        assertFalse(JlccNivelVista.EDIFICIO.jlccPuedeIrA(JlccNivelVista.UNIDAD));
        assertFalse(JlccNivelVista.EDIFICIO.jlccPuedeIrA(JlccNivelVista.EDIFICIO));
        assertFalse(JlccNivelVista.PISO.jlccPuedeIrA(JlccNivelVista.PISO));
        assertFalse(JlccNivelVista.UNIDAD.jlccPuedeIrA(JlccNivelVista.UNIDAD));
    }

    @Test
    void destinoNuloNoEsUnaTransicion() {
        for (JlccNivelVista nivel : JlccNivelVista.values()) {
            assertFalse(nivel.jlccPuedeIrA(null));
        }
        assertTrue(JlccNivelVista.jlccDestinosPermitidos(null).isEmpty());
    }

    @Test
    void destinosPermitidosPorNivel() {
        assertEquals(Set.of(JlccNivelVista.PISO), JlccNivelVista.jlccDestinosPermitidos(JlccNivelVista.EDIFICIO));
        assertEquals(
                Set.of(JlccNivelVista.EDIFICIO, JlccNivelVista.UNIDAD),
                JlccNivelVista.jlccDestinosPermitidos(JlccNivelVista.PISO));
        assertEquals(
                Set.of(JlccNivelVista.EDIFICIO, JlccNivelVista.PISO),
                JlccNivelVista.jlccDestinosPermitidos(JlccNivelVista.UNIDAD));
    }

    @Test
    void volverSubeUnNivelYSeQuedaEnEdificio() {
        assertEquals(JlccNivelVista.EDIFICIO, JlccNivelVista.EDIFICIO.jlccVolver());
        assertEquals(JlccNivelVista.EDIFICIO, JlccNivelVista.PISO.jlccVolver());
        assertEquals(JlccNivelVista.PISO, JlccNivelVista.UNIDAD.jlccVolver());
    }

    @Test
    void volverSiempreEsUnaTransicionPermitidaOElMismoNivel() {
        for (JlccNivelVista nivel : JlccNivelVista.values()) {
            JlccNivelVista destino = nivel.jlccVolver();
            assertTrue(
                    destino == nivel || nivel.jlccPuedeIrA(destino),
                    nivel + " no puede volver a " + destino);
        }
    }
}

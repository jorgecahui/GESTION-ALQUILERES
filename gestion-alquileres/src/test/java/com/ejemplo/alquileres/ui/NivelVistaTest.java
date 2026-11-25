package com.ejemplo.alquileres.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Set;

class NivelVistaTest {

    @Test
    void transicionesPermitidas() {
        assertTrue(NivelVista.EDIFICIO.puedeIrA(NivelVista.PISO));
        assertTrue(NivelVista.PISO.puedeIrA(NivelVista.UNIDAD));
        assertTrue(NivelVista.PISO.puedeIrA(NivelVista.EDIFICIO));
        assertTrue(NivelVista.UNIDAD.puedeIrA(NivelVista.PISO));
        assertTrue(NivelVista.UNIDAD.puedeIrA(NivelVista.EDIFICIO));
    }

    @Test
    void transicionesInvalidas() {
        assertFalse(NivelVista.EDIFICIO.puedeIrA(NivelVista.UNIDAD));
        assertFalse(NivelVista.EDIFICIO.puedeIrA(NivelVista.EDIFICIO));
        assertFalse(NivelVista.PISO.puedeIrA(NivelVista.PISO));
        assertFalse(NivelVista.UNIDAD.puedeIrA(NivelVista.UNIDAD));
    }

    @Test
    void destinoNuloNoEsUnaTransicion() {
        for (NivelVista nivel : NivelVista.values()) {
            assertFalse(nivel.puedeIrA(null));
        }
        assertTrue(NivelVista.destinosPermitidos(null).isEmpty());
    }

    @Test
    void destinosPermitidosPorNivel() {
        assertEquals(Set.of(NivelVista.PISO), NivelVista.destinosPermitidos(NivelVista.EDIFICIO));
        assertEquals(
                Set.of(NivelVista.EDIFICIO, NivelVista.UNIDAD),
                NivelVista.destinosPermitidos(NivelVista.PISO));
        assertEquals(
                Set.of(NivelVista.EDIFICIO, NivelVista.PISO),
                NivelVista.destinosPermitidos(NivelVista.UNIDAD));
    }

    @Test
    void volverSubeUnNivelYSeQuedaEnEdificio() {
        assertEquals(NivelVista.EDIFICIO, NivelVista.EDIFICIO.volver());
        assertEquals(NivelVista.EDIFICIO, NivelVista.PISO.volver());
        assertEquals(NivelVista.PISO, NivelVista.UNIDAD.volver());
    }

    @Test
    void volverSiempreEsUnaTransicionPermitidaOElMismoNivel() {
        for (NivelVista nivel : NivelVista.values()) {
            NivelVista destino = nivel.volver();
            assertTrue(
                    destino == nivel || nivel.puedeIrA(destino),
                    nivel + " no puede volver a " + destino);
        }
    }
}

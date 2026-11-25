package com.ejemplo.alquileres.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

class MaquetaControllerModoTest {

    @Test
    void preferenciaInvalidaVuelveAEstado() {
        assertEquals(
                JlccMaquetaController.JlccModoVista.ESTADO,
                JlccMaquetaController.JlccModoVista.jlccDesdePreferencia("NO_EXISTE"));
        assertEquals(
                JlccMaquetaController.JlccModoVista.ESTADO,
                JlccMaquetaController.JlccModoVista.jlccDesdePreferencia(null));
        assertEquals(
                JlccMaquetaController.JlccModoVista.ESTADO,
                JlccMaquetaController.JlccModoVista.jlccDesdePreferencia(" "));
        assertEquals(
                JlccMaquetaController.JlccModoVista.RENTA,
                JlccMaquetaController.JlccModoVista.jlccDesdePreferencia("RENTA"));
    }

    @Test
    void etiquetasDelSelectorSonLasEsperadas() {
        assertEquals(
                List.of("Estado", "Mora", "Renta", "Vencimiento"),
                JlccMaquetaController.JlccModoVista.jlccEtiquetas());
    }
}

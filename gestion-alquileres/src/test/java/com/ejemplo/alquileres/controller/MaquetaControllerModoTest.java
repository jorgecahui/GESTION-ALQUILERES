package com.ejemplo.alquileres.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

class MaquetaControllerModoTest {

    @Test
    void preferenciaInvalidaVuelveAEstado() {
        assertEquals(
                MaquetaController.ModoVista.ESTADO,
                MaquetaController.ModoVista.desdePreferencia("NO_EXISTE"));
        assertEquals(
                MaquetaController.ModoVista.ESTADO,
                MaquetaController.ModoVista.desdePreferencia(null));
        assertEquals(
                MaquetaController.ModoVista.ESTADO,
                MaquetaController.ModoVista.desdePreferencia(" "));
        assertEquals(
                MaquetaController.ModoVista.RENTA,
                MaquetaController.ModoVista.desdePreferencia("RENTA"));
    }

    @Test
    void etiquetasDelSelectorSonLasEsperadas() {
        assertEquals(
                List.of("Estado", "Mora", "Renta", "Vencimiento"),
                MaquetaController.ModoVista.etiquetas());
    }
}

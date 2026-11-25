package com.ejemplo.alquileres;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ejemplo.alquileres.ui.JlccNavegacion;

import org.junit.jupiter.api.Test;

class NavegacionTest {
    private final JlccNavegacion navegacion = new JlccNavegacion();

    @Test
    void cadaDestinoDelMenuTieneUnFxml() {
        navegacion
                .jlccEntradas()
                .forEach(
                        entrada ->
                                assertNotNull(
                                        getClass().getResource(navegacion.jlccRecurso(entrada.clave())),
                                        entrada.clave()));
    }
}

package com.ejemplo.alquileres;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.ejemplo.alquileres.ui.Navegacion;
import org.junit.jupiter.api.Test;
class NavegacionTest {
    private final Navegacion navegacion = new Navegacion();
    @Test void cadaDestinoDelMenuTieneUnFxml() {
        navegacion.entradas().forEach(entrada ->
            assertNotNull(getClass().getResource(navegacion.recurso(entrada.clave())), entrada.clave()));
    }
}

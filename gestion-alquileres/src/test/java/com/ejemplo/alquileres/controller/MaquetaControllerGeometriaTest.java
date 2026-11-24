package com.ejemplo.alquileres.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.ui.BloqueMaqueta;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaquetaControllerGeometriaTest {

    @Test void cincoUnidadesQuedanEnUnaFilaCentrada() {
        MaquetaController.Geometria geometria = MaquetaController.calcularGeometria(
                unidades(1, 5));

        MaquetaController.PisoGeometria piso = geometria.pisos().get(1);
        assertEquals(1, piso.filas().size());
        assertEquals(5, piso.filas().get(0).bloques().size());

        List<Double> x = posicionesX(geometria, piso.filas().get(0).bloques());
        assertEquals(0.0, x.get(2), 0.001);
        assertEquals(x.get(0), -x.get(4), 0.001);
        assertEquals(x.get(1), -x.get(3), 0.001);
        assertEquals(x.get(1) - x.get(0), x.get(2) - x.get(1), 0.001);
    }

    @Test void doceUnidadesSeRepartenEnDosFilasIguales() {
        MaquetaController.Geometria geometria = MaquetaController.calcularGeometria(
                unidades(1, 12));

        MaquetaController.PisoGeometria piso = geometria.pisos().get(1);
        assertEquals(2, piso.filas().size());
        assertEquals(6, piso.filas().get(0).bloques().size());
        assertEquals(6, piso.filas().get(1).bloques().size());
        assertTrue(piso.filas().get(1).z() > piso.filas().get(0).z());

        List<Double> primeraFila = posicionesX(geometria, piso.filas().get(0).bloques());
        List<Double> segundaFila = posicionesX(geometria, piso.filas().get(1).bloques());
        assertEquals(primeraFila, segundaFila);
    }

    @Test void pisosConCantidadesDistintasQuedanCentrados() {
        List<BloqueMaqueta> bloques = new ArrayList<>();
        bloques.addAll(unidadesConIds(1, 5, 100));
        bloques.addAll(unidadesConIds(2, 4, 200));
        bloques.addAll(unidadesConIds(3, 6, 300));

        MaquetaController.Geometria geometria = MaquetaController.calcularGeometria(bloques);

        for (MaquetaController.PisoGeometria piso : geometria.pisos().values()) {
            double centro = piso.filas().stream()
                    .flatMap(fila -> fila.bloques().stream())
                    .mapToDouble(bloque -> geometria.posiciones().get(bloque.id()).getX())
                    .average()
                    .orElse(Double.NaN);
            assertEquals(0.0, centro, 0.001, "Piso " + piso.nivel());
        }
        assertTrue(geometria.pisos().get(3).ancho() > geometria.pisos().get(2).ancho());
    }

    private List<BloqueMaqueta> unidades(int nivel, int cantidad) {
        return unidadesConIds(nivel, cantidad, nivel * 100L);
    }

    private List<BloqueMaqueta> unidadesConIds(int nivel, int cantidad, long baseId) {
        List<BloqueMaqueta> bloques = new ArrayList<>();
        for (int orden = 1; orden <= cantidad; orden++) {
            long id = baseId + orden;
            bloques.add(new BloqueMaqueta(id, "Unidad " + id, nivel, orden,
                    EstadoInmueble.DISPONIBLE, null));
        }
        return bloques;
    }

    private List<Double> posicionesX(MaquetaController.Geometria geometria,
                                     List<BloqueMaqueta> bloques) {
        return bloques.stream()
                .map(bloque -> geometria.posiciones().get(bloque.id()).getX())
                .toList();
    }
}

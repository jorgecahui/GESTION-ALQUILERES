package com.ejemplo.alquileres.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.ui.BloqueMaqueta;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point3D;
import org.junit.jupiter.api.Test;

class MaquetaControllerEncuadreTest {

    private static final double ANCHO_VISTA = 1200;
    private static final double ALTO_VISTA = 800;
    private static final double FOV = 36;
    private static final double NEAR = 0.1;
    private static final double RELLENO = 0.88;

    @Test void elEncuadreDelPisoSoloTomaLosPuntosDeEsePiso() {
        MaquetaController.Geometria geometria = geometriaDeTresPisos();

        List<Point3D> piso1 = MaquetaController.puntosPiso(geometria, 1);
        List<Point3D> piso2 = MaquetaController.puntosPiso(geometria, 2);

        assertEquals(8 + 5 * 8, piso1.size(), "losa + 5 bloques");
        assertEquals(8 + 5 * 8, piso2.size(), "losa + 5 bloques");
        assertTrue(minY(piso1) > maxY(piso2),
                "los puntos del piso 1 deben quedar por encima de los del piso 2");
        assertTrue(MaquetaController.puntosPiso(geometria, 99).isEmpty());
        assertTrue(MaquetaController.puntosPiso(null, 1).isEmpty());
    }

    @Test void elEncuadreCentraElPisoYLoDejaDentroDeLaVista() {
        MaquetaController.Geometria geometria = geometriaDeTresPisos();
        List<Point3D> puntos = MaquetaController.puntosPiso(geometria, 2);

        double centroX = (minX(puntos) + maxX(puntos)) / 2;
        double centroY = (minY(puntos) + maxY(puntos)) / 2;
        double centroZ = (min(puntos, Point3D::getZ) + max(puntos, Point3D::getZ)) / 2;

        MaquetaController.Encuadre encuadre = MaquetaController.calcularEncuadre(
                puntos, ANCHO_VISTA, ALTO_VISTA, FOV, NEAR, RELLENO);

        assertNotNull(encuadre);
        assertEquals(centroX, encuadre.ojoX(), 0.5);
        assertEquals(centroY, encuadre.ojoY(), 0.5);
        assertEquals(centroZ, encuadre.centroZ(), 0.5);
        assertTrue(encuadre.distancia() > 0);

        double tanV = Math.tan(Math.toRadians(FOV / 2));
        double aspecto = ANCHO_VISTA / ALTO_VISTA;
        for (Point3D punto : puntos) {
            double profundidad = encuadre.distancia() + punto.getZ() - encuadre.centroZ();
            assertTrue(profundidad > NEAR, "el piso debe quedar delante de la cámara");
            double ndcX = (punto.getX() - encuadre.ojoX()) / (profundidad * tanV * aspecto);
            double ndcY = (punto.getY() - encuadre.ojoY()) / (profundidad * tanV);
            assertTrue(Math.abs(ndcX) <= 1, "x fuera de la vista: " + ndcX);
            assertTrue(Math.abs(ndcY) <= 1, "y fuera de la vista: " + ndcY);
        }
    }

    @Test void unaVistaMasPequenaNecesitaMasDistancia() {
        MaquetaController.Geometria geometria = geometriaDeTresPisos();
        List<Point3D> puntos = MaquetaController.puntosPiso(geometria, 2);

        MaquetaController.Encuadre grande = MaquetaController.calcularEncuadre(
                puntos, 1600, 1000, FOV, NEAR, RELLENO);
        MaquetaController.Encuadre pequeno = MaquetaController.calcularEncuadre(
                puntos, 600, 400, FOV, NEAR, RELLENO);

        assertNotNull(grande);
        assertNotNull(pequeno);
        assertTrue(pequeno.distancia() > grande.distancia());
    }

    @Test void sinPuntosNoHayEncuadre() {
        assertNull(MaquetaController.calcularEncuadre(
                List.of(), ANCHO_VISTA, ALTO_VISTA, FOV, NEAR, RELLENO));
        assertNull(MaquetaController.calcularEncuadre(
                List.of(new Point3D(0, 0, 0)), 0, ALTO_VISTA, FOV, NEAR, RELLENO));
        assertNull(MaquetaController.calcularEncuadre(
                null, ANCHO_VISTA, ALTO_VISTA, FOV, NEAR, RELLENO));
    }

    private MaquetaController.Geometria geometriaDeTresPisos() {
        List<BloqueMaqueta> bloques = new ArrayList<>();
        for (int nivel = 1; nivel <= 3; nivel++) {
            for (int orden = 1; orden <= 5; orden++) {
                long id = nivel * 100L + orden;
                bloques.add(new BloqueMaqueta(id, "Unidad " + id, nivel, orden,
                        EstadoInmueble.DISPONIBLE, null));
            }
        }
        return MaquetaController.calcularGeometria(bloques);
    }

    private static double minX(List<Point3D> puntos) {
        return min(puntos, Point3D::getX);
    }

    private static double maxX(List<Point3D> puntos) {
        return max(puntos, Point3D::getX);
    }

    private static double minY(List<Point3D> puntos) {
        return min(puntos, Point3D::getY);
    }

    private static double maxY(List<Point3D> puntos) {
        return max(puntos, Point3D::getY);
    }

    private static double min(List<Point3D> puntos,
                              java.util.function.ToDoubleFunction<Point3D> valor) {
        return puntos.stream().mapToDouble(valor).min().orElseThrow();
    }

    private static double max(List<Point3D> puntos,
                              java.util.function.ToDoubleFunction<Point3D> valor) {
        return puntos.stream().mapToDouble(valor).max().orElseThrow();
    }
}

package com.ejemplo.alquileres.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.model.EstadoInmueble;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class PlantaPisoTest {

    @Test
    void areaNulaOInvalidaUsaElAnchoPorDefecto() {
        assertEquals(JlccPlantaPiso.JLCC_ANCHO_POR_DEFECTO, JlccPlantaPiso.jlccAnchoPara(null), 0.001);
        assertEquals(JlccPlantaPiso.JLCC_ANCHO_POR_DEFECTO, JlccPlantaPiso.jlccAnchoPara(BigDecimal.ZERO), 0.001);
        assertEquals(
                JlccPlantaPiso.JLCC_ANCHO_POR_DEFECTO, JlccPlantaPiso.jlccAnchoPara(new BigDecimal("-10")), 0.001);
    }

    @Test
    void elAnchoRespetaElMinimoYElMaximo() {
        assertEquals(JlccPlantaPiso.JLCC_ANCHO_MINIMO, JlccPlantaPiso.jlccAnchoPara(BigDecimal.ONE), 0.001);
        assertEquals(JlccPlantaPiso.JLCC_ANCHO_MAXIMO, JlccPlantaPiso.jlccAnchoPara(new BigDecimal("500")), 0.001);
    }

    @Test
    void elAnchoEsProporcionalDentroDelRango() {
        double cincuenta = JlccPlantaPiso.jlccAnchoPara(new BigDecimal("50"));
        double setenta = JlccPlantaPiso.jlccAnchoPara(new BigDecimal("70"));

        assertEquals(50 * JlccPlantaPiso.JLCC_ANCHO_POR_M2, cincuenta, 0.001);
        assertEquals(70 * JlccPlantaPiso.JLCC_ANCHO_POR_M2, setenta, 0.001);
        assertTrue(setenta > cincuenta);
    }

    @Test
    void cincoUnidadesQuedanEnUnaHileraCentradaConPasilloAlFrente() {
        JlccPlantaPiso.Distribucion distribucion = JlccPlantaPiso.jlccCalcular(2, unidades(5));

        assertEquals(1, distribucion.filas());
        assertEquals(5, distribucion.recintos().size());
        assertEquals(0.0, centroX(distribucion), 0.001);
        for (JlccPlantaPiso.Recinto recinto : distribucion.recintos()) {
            assertEquals(0, recinto.fila());
            assertEquals(-JlccPlantaPiso.JLCC_ANCHO_PASILLO / 2, recinto.z(), 0.001);
        }
        assertEquals(
                distribucion.profundidad() / 2 - JlccPlantaPiso.JLCC_ANCHO_PASILLO / 2,
                distribucion.pasilloZ(),
                0.001);
    }

    @Test
    void ochoUnidadesSeRepartenEnDosHilerasEnfrentadas() {
        JlccPlantaPiso.Distribucion distribucion = JlccPlantaPiso.jlccCalcular(3, unidades(8));

        assertEquals(2, distribucion.filas());
        assertEquals(8, distribucion.recintos().size());
        long primera =
                distribucion.recintos().stream().filter(recinto -> recinto.fila() == 0).count();
        long segunda =
                distribucion.recintos().stream().filter(recinto -> recinto.fila() == 1).count();
        assertEquals(4, primera);
        assertEquals(4, segunda);
        assertEquals(0.0, distribucion.pasilloZ(), 0.001);

        for (JlccPlantaPiso.Recinto recinto : distribucion.recintos()) {
            double zEsperada =
                    recinto.fila() == 0
                            ? -(JlccPlantaPiso.JLCC_PROFUNDIDAD_RECINTO / 2 + JlccPlantaPiso.JLCC_ANCHO_PASILLO / 2)
                            : JlccPlantaPiso.JLCC_PROFUNDIDAD_RECINTO / 2 + JlccPlantaPiso.JLCC_ANCHO_PASILLO / 2;
            assertEquals(zEsperada, recinto.z(), 0.001);
        }
        assertEquals(0.0, centroXFila(distribucion, 0), 0.001);
        assertEquals(0.0, centroXFila(distribucion, 1), 0.001);
    }

    @Test
    void losRecintosDeUnaHileraNoSeSolapan() {
        JlccPlantaPiso.Distribucion distribucion =
                JlccPlantaPiso.jlccCalcular(
                        1,
                        unidadesConAreas(
                                new BigDecimal("25"),
                                new BigDecimal("60"),
                                new BigDecimal("90"),
                                new BigDecimal("40")));

        List<JlccPlantaPiso.Recinto> recintos = distribucion.recintos();
        for (int i = 1; i < recintos.size(); i++) {
            JlccPlantaPiso.Recinto anterior = recintos.get(i - 1);
            JlccPlantaPiso.Recinto actual = recintos.get(i);
            double bordeAnterior = anterior.x() + anterior.ancho() / 2;
            double bordeActual = actual.x() - actual.ancho() / 2;
            assertEquals(JlccPlantaPiso.JLCC_SEPARACION_RECINTOS, bordeActual - bordeAnterior, 0.001);
        }
    }

    @Test
    void laDistribucionEsDeterministaAunqueCambieElOrdenDeEntrada() {
        List<BloqueMaqueta> unidades = unidades(8);
        List<BloqueMaqueta> desordenadas = new ArrayList<>(unidades);
        Collections.reverse(desordenadas);

        assertEquals(JlccPlantaPiso.jlccCalcular(4, unidades), JlccPlantaPiso.jlccCalcular(4, desordenadas));
    }

    @Test
    void unidadSinAreaSeRotulaSinMetrosYUsaElAnchoPorDefecto() {
        BloqueMaqueta bloque = bloque(1L, 1, null);
        JlccPlantaPiso.Distribucion distribucion = JlccPlantaPiso.jlccCalcular(1, List.of(bloque));

        assertEquals(JlccPlantaPiso.JLCC_ANCHO_POR_DEFECTO, distribucion.recintos().get(0).ancho(), 0.001);
        assertFalse(JlccPlantaPiso.jlccTieneArea(bloque));
        assertTrue(JlccPlantaPiso.jlccTieneArea(bloque(2L, 1, new BigDecimal("45"))));
    }

    @Test
    void unPisoSinUnidadesNoGeneraRecintos() {
        JlccPlantaPiso.Distribucion distribucion = JlccPlantaPiso.jlccCalcular(5, List.of());

        assertTrue(distribucion.recintos().isEmpty());
        assertEquals(0, distribucion.filas());
        assertEquals(0.0, distribucion.ancho(), 0.001);
    }

    private List<BloqueMaqueta> unidades(int cantidad) {
        List<BigDecimal> areas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            areas.add(BigDecimal.valueOf(30L + i * 5L));
        }
        return unidadesConAreas(areas.toArray(new BigDecimal[0]));
    }

    private List<BloqueMaqueta> unidadesConAreas(BigDecimal... areas) {
        List<BloqueMaqueta> bloques = new ArrayList<>();
        for (int i = 0; i < areas.length; i++) {
            bloques.add(bloque(100L + i, i + 1, areas[i]));
        }
        return bloques;
    }

    private BloqueMaqueta bloque(Long id, int orden, BigDecimal area) {
        return new BloqueMaqueta(
                id,
                "INM-" + id,
                "Unidad " + orden,
                "Departamento",
                2,
                orden,
                EstadoInmueble.DISPONIBLE,
                null,
                null,
                null,
                false,
                area,
                new BigDecimal("1000.00"),
                null,
                null);
    }

    private double centroX(JlccPlantaPiso.Distribucion distribucion) {
        return centroXFila(distribucion, 0);
    }

    private double centroXFila(JlccPlantaPiso.Distribucion distribucion, int fila) {
        double izquierda =
                distribucion.recintos().stream()
                        .filter(recinto -> recinto.fila() == fila)
                        .mapToDouble(recinto -> recinto.x() - recinto.ancho() / 2)
                        .min()
                        .orElse(Double.NaN);
        double derecha =
                distribucion.recintos().stream()
                        .filter(recinto -> recinto.fila() == fila)
                        .mapToDouble(recinto -> recinto.x() + recinto.ancho() / 2)
                        .max()
                        .orElse(Double.NaN);
        return (izquierda + derecha) / 2;
    }
}

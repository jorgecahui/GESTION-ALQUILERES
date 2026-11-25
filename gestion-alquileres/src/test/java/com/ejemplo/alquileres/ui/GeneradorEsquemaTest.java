package com.ejemplo.alquileres.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.ui.GeneradorEsquema.Esquema;
import com.ejemplo.alquileres.ui.GeneradorEsquema.Recinto;
import com.ejemplo.alquileres.ui.GeneradorEsquema.TipoAmbiente;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeneradorEsquemaTest {

    @Test void dosDormitoriosUnBanoGeneraEsquemaCompleto() {
        Esquema esquema = GeneradorEsquema.generar(
                new BigDecimal("65"), 2, 1, false);

        assertFalse(esquema.simple());
        assertEquals(1, contar(esquema, TipoAmbiente.DORMITORIO_PRINCIPAL));
        assertEquals(1, contar(esquema, TipoAmbiente.DORMITORIO));
        assertEquals(1, contar(esquema, TipoAmbiente.BANO));
        assertEquals(1, contar(esquema, TipoAmbiente.SALA_COMEDOR));
        assertEquals(1, contar(esquema, TipoAmbiente.COCINA));
        assertEquals(1, contar(esquema, TipoAmbiente.PASILLO));
        comprobarConsistencia(esquema, 65);
    }

    @Test void seisDormitoriosYCincoBanosSeDibujanCompletos() {
        Esquema esquema = GeneradorEsquema.generar(
                new BigDecimal("180"), 6, 5, true);

        assertEquals(1, contar(esquema, TipoAmbiente.DORMITORIO_PRINCIPAL));
        assertEquals(5, contar(esquema, TipoAmbiente.DORMITORIO));
        assertEquals(5, contar(esquema, TipoAmbiente.BANO));
        comprobarConsistencia(esquema, 180);
    }

    @Test void unLocalSinBanosEsUnUnicoRecintoSimple() {
        Esquema esquema = GeneradorEsquema.generar(
                new BigDecimal("30"), 0, 0, false);

        assertTrue(esquema.simple());
        assertEquals(1, esquema.recintos().size());
        assertEquals(TipoAmbiente.AMBIENTE, esquema.recintos().get(0).tipo());
        assertEquals(30, esquema.areaRecintos(), 0.01);
        comprobarConsistencia(esquema, 30);
    }

    @Test void unLocalConDosBanosLosUbicaEnLaEsquinaSuperiorDerecha() {
        Esquema esquema = GeneradorEsquema.generar(
                new BigDecimal("72"), 0, 2, false);

        assertTrue(esquema.simple());
        assertEquals(2, contar(esquema, TipoAmbiente.BANO));
        double bordeDerecho = esquema.ancho() - 1e-6;
        double bordeSuperior = esquema.alto() - 1e-6;
        for (Recinto recinto : esquema.recintos()) {
            if (recinto.tipo() != TipoAmbiente.BANO) continue;
            assertTrue(recinto.x() + recinto.ancho() <= bordeDerecho + 1e-6);
            assertTrue(recinto.y() + recinto.alto() <= bordeSuperior + 1e-6);
            assertTrue(recinto.y() > esquema.alto() * 0.5);
        }
        comprobarConsistencia(esquema, 72);
    }

    @Test void unAreaNulaUsaElValorPorDefectoYSeMarcaEstimada() {
        Esquema esquema = GeneradorEsquema.generar(null, 2, 1, false);

        assertTrue(esquema.areaEstimada());
        assertEquals(65, esquema.areaUsada(), 0.01);
        assertTrue(esquema.avisos().stream().anyMatch(a -> a.contains("Área estimada")));
        comprobarConsistencia(esquema, 65);
    }

    @Test void losCasosLimiteNoFallanYConservanLasAreas() {
        List<double[]> casos = List.of(
                new double[]{0, 0, 0, 30},
                new double[]{1, 1, 1, 1},
                new double[]{500, 3, 2, 500},
                new double[]{1, 6, 5, 1},
                new double[]{500, 6, 5, 500});
        for (double[] caso : casos) {
            Esquema esquema = GeneradorEsquema.generar(
                    BigDecimal.valueOf(caso[0]), (int) caso[1], (int) caso[2], false);
            comprobarConsistencia(esquema, caso[3]);
        }
    }

    @Test void elResultadoEsDeterminista() {
        assertEquals(GeneradorEsquema.generar(new BigDecimal("88"), 3, 2, true),
                GeneradorEsquema.generar(new BigDecimal("88"), 3, 2, true));
    }

    @Test void losValoresSeTopanAOchoDormitoriosYSeisBanos() {
        Esquema esquema = GeneradorEsquema.generar(
                new BigDecimal("200"), 10, 9, false);

        assertEquals(1, contar(esquema, TipoAmbiente.DORMITORIO_PRINCIPAL));
        assertEquals(7, contar(esquema, TipoAmbiente.DORMITORIO));
        assertEquals(6, contar(esquema, TipoAmbiente.BANO));
        assertTrue(esquema.avisos().stream().anyMatch(a -> a.contains("Dormitorios topados")));
        assertTrue(esquema.avisos().stream().anyMatch(a -> a.contains("Baños topados")));
        comprobarConsistencia(esquema, 200);
    }

    @Test void amobladoSeConservaEnElResultado() {
        assertTrue(GeneradorEsquema.generar(
                new BigDecimal("65"), 2, 1, true).amoblado());
        assertFalse(GeneradorEsquema.generar(
                new BigDecimal("65"), 2, 1, null).amoblado());
    }

    private long contar(Esquema esquema, TipoAmbiente tipo) {
        return esquema.recintos().stream().filter(r -> r.tipo() == tipo).count();
    }

    private void comprobarConsistencia(Esquema esquema, double areaEsperada) {
        assertEquals(areaEsperada, esquema.areaUsada(), 0.01);
        assertEquals(esquema.areaUsada(), esquema.areaRecintos(),
                Math.max(0.01, esquema.areaUsada() * 0.01));
        for (Recinto recinto : esquema.recintos()) {
            assertTrue(recinto.ancho() > 0, "ancho > 0");
            assertTrue(recinto.alto() > 0, "alto > 0");
            assertTrue(recinto.x() >= -1e-6, "x dentro");
            assertTrue(recinto.y() >= -1e-6, "y dentro");
            assertTrue(recinto.x() + recinto.ancho() <= esquema.ancho() + 1e-6,
                    "borde derecho dentro");
            assertTrue(recinto.y() + recinto.alto() <= esquema.alto() + 1e-6,
                    "borde superior dentro");
        }
        for (int i = 0; i < esquema.recintos().size(); i++) {
            for (int j = i + 1; j < esquema.recintos().size(); j++) {
                assertFalse(seSolapan(esquema.recintos().get(i), esquema.recintos().get(j)),
                        "recintos " + i + " y " + j + " se solapan");
            }
        }
        assertTrue(esquema.puerta().x() >= 0 && esquema.puerta().x() <= esquema.ancho());
    }

    private boolean seSolapan(Recinto a, Recinto b) {
        double ancho = Math.min(a.x() + a.ancho(), b.x() + b.ancho())
                - Math.max(a.x(), b.x());
        double alto = Math.min(a.y() + a.alto(), b.y() + b.alto())
                - Math.max(a.y(), b.y());
        return ancho > 1e-6 && alto > 1e-6;
    }
}

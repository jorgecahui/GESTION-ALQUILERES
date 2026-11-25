package com.ejemplo.alquileres.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class JlccPlantaPiso {

    public static final double JLCC_ANCHO_MINIMO = 44;
    public static final double JLCC_ANCHO_MAXIMO = 130;
    public static final double JLCC_ANCHO_POR_DEFECTO = 64;
    public static final double JLCC_ANCHO_POR_M2 = 1.1;
    public static final double JLCC_PROFUNDIDAD_RECINTO = 82;
    public static final double JLCC_ANCHO_PASILLO = 28;
    public static final double JLCC_SEPARACION_RECINTOS = 2;
    public static final double JLCC_MARGEN_PLANTA = 26;
    public static final int JLCC_MAXIMO_UNA_HILERA = 6;

    private JlccPlantaPiso() {}

    public record Recinto(
            BloqueMaqueta bloque, int fila, double x, double z, double ancho, double profundidad) {}

    public record Distribucion(
            int nivel,
            List<Recinto> recintos,
            int filas,
            double ancho,
            double profundidad,
            double pasilloZ,
            double pasilloAncho) {

        public Distribucion {
            recintos = List.copyOf(recintos);
        }

        public double anchoConMargen() {
            return ancho + 2 * JLCC_MARGEN_PLANTA;
        }

        public double profundidadConMargen() {
            return profundidad + 2 * JLCC_MARGEN_PLANTA;
        }
    }

    public static double jlccAnchoPara(BigDecimal jlccAreaM2) {
        if (jlccAreaM2 == null || jlccAreaM2.signum() <= 0) return JLCC_ANCHO_POR_DEFECTO;
        double jlccAncho = jlccAreaM2.doubleValue() * JLCC_ANCHO_POR_M2;
        return Math.max(JLCC_ANCHO_MINIMO, Math.min(JLCC_ANCHO_MAXIMO, jlccAncho));
    }

    public static boolean jlccTieneArea(BloqueMaqueta jlccBloque) {
        return jlccBloque != null && jlccBloque.areaM2() != null && jlccBloque.areaM2().signum() > 0;
    }

    public static Distribucion jlccCalcular(int jlccNivel, List<BloqueMaqueta> jlccBloques) {
        List<BloqueMaqueta> jlccUnidades = new ArrayList<>(jlccBloques == null ? List.of() : jlccBloques);
        jlccUnidades.sort(
                Comparator.comparingInt(BloqueMaqueta::orden).thenComparing(BloqueMaqueta::id));
        if (jlccUnidades.isEmpty()) {
            return new Distribucion(jlccNivel, List.of(), 0, 0, 0, 0, JLCC_ANCHO_PASILLO);
        }

        int jlccFilas = jlccUnidades.size() > JLCC_MAXIMO_UNA_HILERA ? 2 : 1;
        List<List<BloqueMaqueta>> jlccPorFila = new ArrayList<>(jlccFilas);
        if (jlccFilas == 1) {
            jlccPorFila.add(jlccUnidades);
        } else {
            int jlccPrimera = (jlccUnidades.size() + 1) / 2;
            jlccPorFila.add(jlccUnidades.subList(0, jlccPrimera));
            jlccPorFila.add(jlccUnidades.subList(jlccPrimera, jlccUnidades.size()));
        }

        double jlccAnchoMaximo = 0;
        List<Double> jlccAnchosFila = new ArrayList<>(jlccFilas);
        for (List<BloqueMaqueta> jlccFila : jlccPorFila) {
            double jlccAncho = 0;
            for (int i = 0; i < jlccFila.size(); i++) {
                jlccAncho += jlccAnchoPara(jlccFila.get(i).areaM2());
                if (i > 0) jlccAncho += JLCC_SEPARACION_RECINTOS;
            }
            jlccAnchosFila.add(jlccAncho);
            jlccAnchoMaximo = Math.max(jlccAnchoMaximo, jlccAncho);
        }

        double jlccProfundidad =
                jlccFilas == 2
                        ? 2 * JLCC_PROFUNDIDAD_RECINTO + JLCC_ANCHO_PASILLO
                        : JLCC_PROFUNDIDAD_RECINTO + JLCC_ANCHO_PASILLO;
        double jlccZUnaHilera = -JLCC_ANCHO_PASILLO / 2;
        double jlccZDosHileras = JLCC_PROFUNDIDAD_RECINTO / 2 + JLCC_ANCHO_PASILLO / 2;

        List<Recinto> jlccRecintos = new ArrayList<>(jlccUnidades.size());
        for (int jlccFila = 0; jlccFila < jlccFilas; jlccFila++) {
            double z = jlccFilas == 2 ? (jlccFila == 0 ? -jlccZDosHileras : jlccZDosHileras) : jlccZUnaHilera;
            double x = -jlccAnchosFila.get(jlccFila) / 2;
            for (BloqueMaqueta jlccBloque : jlccPorFila.get(jlccFila)) {
                double jlccAncho = jlccAnchoPara(jlccBloque.areaM2());
                jlccRecintos.add(
                        new Recinto(jlccBloque, jlccFila, x + jlccAncho / 2, z, jlccAncho, JLCC_PROFUNDIDAD_RECINTO));
                x += jlccAncho + JLCC_SEPARACION_RECINTOS;
            }
        }

        double jlccPasilloZ = jlccFilas == 2 ? 0 : jlccProfundidad / 2 - JLCC_ANCHO_PASILLO / 2;
        return new Distribucion(
                jlccNivel, jlccRecintos, jlccFilas, jlccAnchoMaximo, jlccProfundidad, jlccPasilloZ, JLCC_ANCHO_PASILLO);
    }
}

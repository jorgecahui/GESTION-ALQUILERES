package com.ejemplo.alquileres.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Distribución pura de las unidades de un piso como recintos en planta.
 *
 * <p>El ancho de cada recinto es proporcional a su área (dentro de un mínimo y
 * un máximo) y la profundidad es constante. Hasta {@link #MAXIMO_UNA_HILERA}
 * unidades se colocan en una hilera con el pasillo al frente; con más unidades
 * se reparten en dos hileras enfrentadas. La clase no usa JavaFX.</p>
 */
public final class PlantaPiso {

    public static final double ANCHO_MINIMO = 44;
    public static final double ANCHO_MAXIMO = 130;
    public static final double ANCHO_POR_DEFECTO = 64;
    public static final double ANCHO_POR_M2 = 1.1;
    public static final double PROFUNDIDAD_RECINTO = 82;
    public static final double ANCHO_PASILLO = 28;
    public static final double SEPARACION_RECINTOS = 2;
    public static final double MARGEN_PLANTA = 26;
    public static final int MAXIMO_UNA_HILERA = 6;

    private PlantaPiso() {
    }

    /** Un recinto rectangular de la planta, centrado en (x, z). */
    public record Recinto(BloqueMaqueta bloque, int fila, double x, double z,
                          double ancho, double profundidad) {
    }

    /** Resultado completo de la distribución de un piso. */
    public record Distribucion(int nivel, List<Recinto> recintos, int filas,
                               double ancho, double profundidad,
                               double pasilloZ, double pasilloAncho) {

        public Distribucion {
            recintos = List.copyOf(recintos);
        }

        public double anchoConMargen() {
            return ancho + 2 * MARGEN_PLANTA;
        }

        public double profundidadConMargen() {
            return profundidad + 2 * MARGEN_PLANTA;
        }
    }

    /** Ancho en unidades de mundo para el área indicada. Área nula o inválida usa el ancho por defecto. */
    public static double anchoPara(BigDecimal areaM2) {
        if (areaM2 == null || areaM2.signum() <= 0) return ANCHO_POR_DEFECTO;
        double ancho = areaM2.doubleValue() * ANCHO_POR_M2;
        return Math.max(ANCHO_MINIMO, Math.min(ANCHO_MAXIMO, ancho));
    }

    /** Indica si la unidad tiene un área válida para rotular los metros cuadrados. */
    public static boolean tieneArea(BloqueMaqueta bloque) {
        return bloque != null && bloque.areaM2() != null && bloque.areaM2().signum() > 0;
    }

    /**
     * Calcula la distribución de un piso. Las unidades se ordenan por orden e id
     * para que el resultado sea determinista.
     */
    public static Distribucion calcular(int nivel, List<BloqueMaqueta> bloques) {
        List<BloqueMaqueta> unidades = new ArrayList<>(
                bloques == null ? List.of() : bloques);
        unidades.sort(Comparator.comparingInt(BloqueMaqueta::orden)
                .thenComparing(BloqueMaqueta::id));
        if (unidades.isEmpty()) {
            return new Distribucion(nivel, List.of(), 0, 0, 0, 0, ANCHO_PASILLO);
        }

        int filas = unidades.size() > MAXIMO_UNA_HILERA ? 2 : 1;
        List<List<BloqueMaqueta>> porFila = new ArrayList<>(filas);
        if (filas == 1) {
            porFila.add(unidades);
        } else {
            int primera = (unidades.size() + 1) / 2;
            porFila.add(unidades.subList(0, primera));
            porFila.add(unidades.subList(primera, unidades.size()));
        }

        double anchoMaximo = 0;
        List<Double> anchosFila = new ArrayList<>(filas);
        for (List<BloqueMaqueta> fila : porFila) {
            double ancho = 0;
            for (int i = 0; i < fila.size(); i++) {
                ancho += anchoPara(fila.get(i).areaM2());
                if (i > 0) ancho += SEPARACION_RECINTOS;
            }
            anchosFila.add(ancho);
            anchoMaximo = Math.max(anchoMaximo, ancho);
        }

        double profundidad = filas == 2
                ? 2 * PROFUNDIDAD_RECINTO + ANCHO_PASILLO
                : PROFUNDIDAD_RECINTO + ANCHO_PASILLO;
        double zUnaHilera = -ANCHO_PASILLO / 2;
        double zDosHileras = PROFUNDIDAD_RECINTO / 2 + ANCHO_PASILLO / 2;

        List<Recinto> recintos = new ArrayList<>(unidades.size());
        for (int fila = 0; fila < filas; fila++) {
            double z = filas == 2 ? (fila == 0 ? -zDosHileras : zDosHileras) : zUnaHilera;
            double x = -anchosFila.get(fila) / 2;
            for (BloqueMaqueta bloque : porFila.get(fila)) {
                double ancho = anchoPara(bloque.areaM2());
                recintos.add(new Recinto(bloque, fila,
                        x + ancho / 2, z, ancho, PROFUNDIDAD_RECINTO));
                x += ancho + SEPARACION_RECINTOS;
            }
        }

        double pasilloZ = filas == 2 ? 0 : profundidad / 2 - ANCHO_PASILLO / 2;
        return new Distribucion(nivel, recintos, filas, anchoMaximo, profundidad,
                pasilloZ, ANCHO_PASILLO);
    }
}

package com.ejemplo.alquileres.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class GeneradorEsquema {

    public static final int MAX_DORMITORIOS = 8;
    public static final int MAX_BANOS = 6;
    public static final double RELACION_ANCHO_ALTO = 1.4;

    private static final double AREA_BANO = 3.0;
    private static final double AREA_BANO_OBJETIVO = 3.5;
    private static final double AREA_BANO_MINIMA = 2.5;
    private static final double AREA_COCINA_OBJETIVO = 7.0;
    private static final double AREA_COCINA_MINIMA = 6.0;
    private static final double AREA_DORMITORIO_OBJETIVO = 11.5;
    private static final double AREA_DORMITORIO_PRINCIPAL_OBJETIVO = 14.0;
    private static final double AREA_DORMITORIO_MINIMA = 7.0;
    private static final double PROPORCION_SOCIAL_CHICA = 0.55;
    private static final double PROPORCION_SOCIAL_GRANDE = 0.50;
    private static final double UMBRAL_AREA_CHICA = 45.0;
    private static final double ANCHO_PUERTA = 0.9;

    private GeneradorEsquema() {}

    public enum TipoAmbiente {
        SALA_COMEDOR("Sala-comedor"),
        COCINA("Cocina"),
        DORMITORIO_PRINCIPAL("Dormitorio principal"),
        DORMITORIO("Dormitorio"),
        BANO("Baño"),
        PASILLO("Pasillo"),
        PASILLO_AUXILIAR("Pasillo"),
        AMBIENTE("Ambiente"),
        AMBIENTE_AUXILIAR("Ambiente");

        private final String etiqueta;

        TipoAmbiente(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    public record Recinto(TipoAmbiente tipo, double x, double y, double ancho, double alto) {
        public double area() {
            return ancho * alto;
        }
    }

    public record Puerta(double x, double y, double ancho) {}

    public record Esquema(
            double ancho,
            double alto,
            double areaUsada,
            boolean simple,
            boolean areaEstimada,
            boolean ajustado,
            boolean amoblado,
            List<Recinto> recintos,
            Puerta puerta,
            List<String> avisos) {
        public Esquema {
            recintos = List.copyOf(recintos);
            avisos = List.copyOf(avisos);
        }

        public double areaRecintos() {
            return recintos.stream().mapToDouble(Recinto::area).sum();
        }
    }

    private record Cuerpo(List<Recinto> recintos, Puerta puerta, boolean ajustado) {}

    public static double areaPorDefecto(int dormitorios) {
        int d = Math.max(0, Math.min(MAX_DORMITORIOS, dormitorios));
        return switch (d) {
            case 0 -> 30;
            case 1 -> 45;
            case 2 -> 65;
            case 3 -> 90;
            default -> 90 + 20 * (d - 3);
        };
    }

    public static Esquema generar(
            BigDecimal areaM2, Integer dormitorios, Integer banos, Boolean amoblado) {
        int dormitoriosSolicitados = dormitorios == null ? 0 : Math.max(0, dormitorios);
        int banosSolicitados = banos == null ? 0 : Math.max(0, banos);
        int d = Math.min(dormitoriosSolicitados, MAX_DORMITORIOS);
        int b = Math.min(banosSolicitados, MAX_BANOS);

        List<String> avisos = new ArrayList<>();
        if (dormitoriosSolicitados > MAX_DORMITORIOS) {
            avisos.add("Dormitorios topados a " + MAX_DORMITORIOS);
        }
        if (banosSolicitados > MAX_BANOS) {
            avisos.add("Baños topados a " + MAX_BANOS);
        }

        boolean areaEstimada = areaM2 == null || areaM2.signum() <= 0;
        double area = areaEstimada ? areaPorDefecto(d) : areaM2.doubleValue();
        if (area < 0.01) area = 0.01;
        if (areaEstimada) avisos.add("Área estimada");

        double ancho = Math.sqrt(area * RELACION_ANCHO_ALTO);
        double alto = area / ancho;
        Cuerpo cuerpo =
                d == 0 ? construirSimple(ancho, alto, b) : construirCompleto(ancho, alto, d, b);
        if (cuerpo.ajustado()) avisos.add("Distribución ajustada al área");

        return new Esquema(
                ancho,
                alto,
                area,
                d == 0,
                areaEstimada,
                cuerpo.ajustado(),
                Boolean.TRUE.equals(amoblado),
                cuerpo.recintos(),
                cuerpo.puerta(),
                avisos);
    }

    private static Cuerpo construirSimple(double ancho, double alto, int banos) {
        List<Recinto> recintos = new ArrayList<>();
        double anchoPuerta = Math.min(ANCHO_PUERTA, ancho * 0.6);
        Puerta puerta = new Puerta(ancho / 2, 0, anchoPuerta);
        if (banos == 0) {
            recintos.add(new Recinto(TipoAmbiente.AMBIENTE, 0, 0, ancho, alto));
            return new Cuerpo(recintos, puerta, false);
        }

        double altoBanos = Math.min(3.0, alto * 0.35);
        double anchoBanosTotal = Math.min(ancho * 0.85, banos * AREA_BANO / altoBanos);
        double anchoBano = anchoBanosTotal / banos;
        double altoPrincipal = alto - altoBanos;
        double anchoPrincipalSuperior = ancho - anchoBanosTotal;

        recintos.add(new Recinto(TipoAmbiente.AMBIENTE, 0, 0, ancho, altoPrincipal));
        if (anchoPrincipalSuperior > 0.0001) {
            recintos.add(
                    new Recinto(
                            TipoAmbiente.AMBIENTE_AUXILIAR,
                            0,
                            altoPrincipal,
                            anchoPrincipalSuperior,
                            altoBanos));
        }
        for (int i = 0; i < banos; i++) {
            recintos.add(
                    new Recinto(
                            TipoAmbiente.BANO,
                            ancho - anchoBanosTotal + i * anchoBano,
                            altoPrincipal,
                            anchoBano,
                            altoBanos));
        }
        boolean ajustado = anchoBano * altoBanos < AREA_BANO_MINIMA;
        return new Cuerpo(recintos, puerta, ajustado);
    }

    private static Cuerpo construirCompleto(double ancho, double alto, int dormitorios, int banos) {
        double proporcionSocial =
                ancho * alto < UMBRAL_AREA_CHICA
                        ? PROPORCION_SOCIAL_CHICA
                        : PROPORCION_SOCIAL_GRANDE;
        double anchoSocial = ancho * proporcionSocial;
        double anchoPasillo = dormitorios >= 2 ? Math.min(1.0, ancho * 0.12) : 0;
        double anchoPrivado = ancho - anchoSocial - anchoPasillo;
        if (anchoPrivado < ancho * 0.15) {
            anchoSocial = ancho * 0.55;
            anchoPasillo = dormitorios >= 2 ? Math.min(1.0, ancho * 0.08) : 0;
            anchoPrivado = ancho - anchoSocial - anchoPasillo;
        }

        List<Recinto> recintos = new ArrayList<>();
        boolean ajustado = false;

        double altoCocina = Math.min(alto * 0.40, AREA_COCINA_OBJETIVO / anchoSocial);
        if (altoCocina <= 0) altoCocina = alto * 0.10;
        double altoSala = alto - altoCocina;
        recintos.add(new Recinto(TipoAmbiente.COCINA, 0, altoSala, anchoSocial, altoCocina));
        recintos.add(new Recinto(TipoAmbiente.SALA_COMEDOR, 0, 0, anchoSocial, altoSala));
        if (anchoSocial * altoCocina < AREA_COCINA_MINIMA) ajustado = true;
        if (anchoSocial * altoSala < ancho * alto * 0.30) ajustado = true;

        if (anchoPasillo > 0) {
            recintos.add(new Recinto(TipoAmbiente.PASILLO, anchoSocial, 0, anchoPasillo, alto));
        }

        double xPrivado = anchoSocial + anchoPasillo;
        double objetivoDormitorios =
                AREA_DORMITORIO_PRINCIPAL_OBJETIVO
                        + Math.max(0, dormitorios - 1) * AREA_DORMITORIO_OBJETIVO;

        double anchoBano = 0;
        double anchoBanosTotal = 0;
        double altoBanos = 0;
        if (banos > 0) {
            double anchoObjetivo = Math.min(2.0, anchoPrivado / banos);
            anchoBanosTotal = Math.min(anchoPrivado, anchoObjetivo * banos);
            anchoBano = anchoBanosTotal / banos;
            altoBanos = Math.min(alto * 0.45, AREA_BANO_OBJETIVO / anchoBano);
            if (altoBanos <= 0) altoBanos = Math.min(alto * 0.2, 1.0);
        }
        double anchoPasilloSuperior = Math.max(0, anchoPrivado - anchoBanosTotal);

        if (banos > 0) {
            double yBanos = alto - altoBanos;
            if (anchoPasilloSuperior > 0.0001) {
                recintos.add(
                        new Recinto(
                                TipoAmbiente.PASILLO_AUXILIAR,
                                xPrivado,
                                yBanos,
                                anchoPasilloSuperior,
                                altoBanos));
            }
            for (int i = 0; i < banos; i++) {
                recintos.add(
                        new Recinto(
                                TipoAmbiente.BANO,
                                xPrivado + anchoPasilloSuperior + i * anchoBano,
                                yBanos,
                                anchoBano,
                                altoBanos));
            }
            if (anchoBano * altoBanos < AREA_BANO_MINIMA) ajustado = true;
        }

        double altoDormitorios = alto - altoBanos;
        if (altoDormitorios <= 0) {
            altoDormitorios = alto;
            altoBanos = 0;
        }
        double altoPrincipal =
                AREA_DORMITORIO_PRINCIPAL_OBJETIVO
                        / Math.max(1, objetivoDormitorios)
                        * altoDormitorios;
        double altoOtros =
                AREA_DORMITORIO_OBJETIVO / Math.max(1, objetivoDormitorios) * altoDormitorios;

        double y = alto - altoBanos;
        for (int i = 0; i < dormitorios; i++) {
            double altura = i == dormitorios - 1 ? y : (i == 0 ? altoPrincipal : altoOtros);
            if (altura <= 0) altura = Math.max(0.0001, y);
            y -= altura;
            TipoAmbiente tipo =
                    i == 0 ? TipoAmbiente.DORMITORIO_PRINCIPAL : TipoAmbiente.DORMITORIO;
            recintos.add(new Recinto(tipo, xPrivado, y, anchoPrivado, altura));
            double area = anchoPrivado * altura;
            if (area < AREA_DORMITORIO_MINIMA) ajustado = true;
        }

        double anchoPuerta = Math.min(ANCHO_PUERTA, anchoSocial * 0.6);
        Puerta puerta = new Puerta(anchoSocial / 2, 0, anchoPuerta);
        return new Cuerpo(recintos, puerta, ajustado);
    }
}

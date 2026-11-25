package com.ejemplo.alquileres.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class JlccGeneradorEsquema {

    public static final int JLCC_MAX_DORMITORIOS = 8;
    public static final int JLCC_MAX_BANOS = 6;
    public static final double JLCC_RELACION_ANCHO_ALTO = 1.4;

    private static final double JLCC_AREA_BANO = 3.0;
    private static final double JLCC_AREA_BANO_OBJETIVO = 3.5;
    private static final double JLCC_AREA_BANO_MINIMA = 2.5;
    private static final double JLCC_AREA_COCINA_OBJETIVO = 7.0;
    private static final double JLCC_AREA_COCINA_MINIMA = 6.0;
    private static final double JLCC_AREA_DORMITORIO_OBJETIVO = 11.5;
    private static final double JLCC_AREA_DORMITORIO_PRINCIPAL_OBJETIVO = 14.0;
    private static final double JLCC_AREA_DORMITORIO_MINIMA = 7.0;
    private static final double JLCC_PROPORCION_SOCIAL_CHICA = 0.55;
    private static final double JLCC_PROPORCION_SOCIAL_GRANDE = 0.50;
    private static final double JLCC_UMBRAL_AREA_CHICA = 45.0;
    private static final double JLCC_ANCHO_PUERTA = 0.9;

    private JlccGeneradorEsquema() {}

    public enum JlccTipoAmbiente {
        SALA_COMEDOR("Sala-comedor"),
        COCINA("Cocina"),
        DORMITORIO_PRINCIPAL("Dormitorio principal"),
        DORMITORIO("Dormitorio"),
        BANO("Baño"),
        PASILLO("Pasillo"),
        PASILLO_AUXILIAR("Pasillo"),
        AMBIENTE("Ambiente"),
        AMBIENTE_AUXILIAR("Ambiente");

        private final String jlccEtiqueta;

        JlccTipoAmbiente(String jlccEtiqueta) {
            this.jlccEtiqueta = jlccEtiqueta;
        }

        public String jlccGetEtiqueta() {
            return jlccEtiqueta;
        }
    }

    public record Recinto(JlccTipoAmbiente tipo, double x, double y, double ancho, double alto) {
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

    public static double jlccAreaPorDefecto(int jlccDormitorios) {
        int d = Math.max(0, Math.min(JLCC_MAX_DORMITORIOS, jlccDormitorios));
        return switch (d) {
            case 0 -> 30;
            case 1 -> 45;
            case 2 -> 65;
            case 3 -> 90;
            default -> 90 + 20 * (d - 3);
        };
    }

    public static Esquema jlccGenerar(
            BigDecimal jlccAreaM2, Integer jlccDormitorios, Integer jlccBanos, Boolean jlccAmoblado) {
        int jlccDormitoriosSolicitados = jlccDormitorios == null ? 0 : Math.max(0, jlccDormitorios);
        int jlccBanosSolicitados = jlccBanos == null ? 0 : Math.max(0, jlccBanos);
        int d = Math.min(jlccDormitoriosSolicitados, JLCC_MAX_DORMITORIOS);
        int b = Math.min(jlccBanosSolicitados, JLCC_MAX_BANOS);

        List<String> jlccAvisos = new ArrayList<>();
        if (jlccDormitoriosSolicitados > JLCC_MAX_DORMITORIOS) {
            jlccAvisos.add("Dormitorios topados a " + JLCC_MAX_DORMITORIOS);
        }
        if (jlccBanosSolicitados > JLCC_MAX_BANOS) {
            jlccAvisos.add("Baños topados a " + JLCC_MAX_BANOS);
        }

        boolean jlccAreaEstimada = jlccAreaM2 == null || jlccAreaM2.signum() <= 0;
        double jlccArea = jlccAreaEstimada ? jlccAreaPorDefecto(d) : jlccAreaM2.doubleValue();
        if (jlccArea < 0.01) jlccArea = 0.01;
        if (jlccAreaEstimada) jlccAvisos.add("Área estimada");

        double jlccAncho = Math.sqrt(jlccArea * JLCC_RELACION_ANCHO_ALTO);
        double jlccAlto = jlccArea / jlccAncho;
        Cuerpo jlccCuerpo =
                d == 0 ? jlccConstruirSimple(jlccAncho, jlccAlto, b) : jlccConstruirCompleto(jlccAncho, jlccAlto, d, b);
        if (jlccCuerpo.ajustado()) jlccAvisos.add("Distribución ajustada al área");

        return new Esquema(
                jlccAncho,
                jlccAlto,
                jlccArea,
                d == 0,
                jlccAreaEstimada,
                jlccCuerpo.ajustado(),
                Boolean.TRUE.equals(jlccAmoblado),
                jlccCuerpo.recintos(),
                jlccCuerpo.puerta(),
                jlccAvisos);
    }

    private static Cuerpo jlccConstruirSimple(double jlccAncho, double jlccAlto, int jlccBanos) {
        List<Recinto> jlccRecintos = new ArrayList<>();
        double jlccAnchoPuerta = Math.min(JLCC_ANCHO_PUERTA, jlccAncho * 0.6);
        Puerta jlccPuerta = new Puerta(jlccAncho / 2, 0, jlccAnchoPuerta);
        if (jlccBanos == 0) {
            jlccRecintos.add(new Recinto(JlccTipoAmbiente.AMBIENTE, 0, 0, jlccAncho, jlccAlto));
            return new Cuerpo(jlccRecintos, jlccPuerta, false);
        }

        double jlccAltoBanos = Math.min(3.0, jlccAlto * 0.35);
        double jlccAnchoBanosTotal = Math.min(jlccAncho * 0.85, jlccBanos * JLCC_AREA_BANO / jlccAltoBanos);
        double jlccAnchoBano = jlccAnchoBanosTotal / jlccBanos;
        double jlccAltoPrincipal = jlccAlto - jlccAltoBanos;
        double jlccAnchoPrincipalSuperior = jlccAncho - jlccAnchoBanosTotal;

        jlccRecintos.add(new Recinto(JlccTipoAmbiente.AMBIENTE, 0, 0, jlccAncho, jlccAltoPrincipal));
        if (jlccAnchoPrincipalSuperior > 0.0001) {
            jlccRecintos.add(
                    new Recinto(
                            JlccTipoAmbiente.AMBIENTE_AUXILIAR,
                            0,
                            jlccAltoPrincipal,
                            jlccAnchoPrincipalSuperior,
                            jlccAltoBanos));
        }
        for (int i = 0; i < jlccBanos; i++) {
            jlccRecintos.add(
                    new Recinto(
                            JlccTipoAmbiente.BANO,
                            jlccAncho - jlccAnchoBanosTotal + i * jlccAnchoBano,
                            jlccAltoPrincipal,
                            jlccAnchoBano,
                            jlccAltoBanos));
        }
        boolean jlccAjustado = jlccAnchoBano * jlccAltoBanos < JLCC_AREA_BANO_MINIMA;
        return new Cuerpo(jlccRecintos, jlccPuerta, jlccAjustado);
    }

    private static Cuerpo jlccConstruirCompleto(double jlccAncho, double jlccAlto, int jlccDormitorios, int jlccBanos) {
        double jlccProporcionSocial =
                jlccAncho * jlccAlto < JLCC_UMBRAL_AREA_CHICA
                        ? JLCC_PROPORCION_SOCIAL_CHICA
                        : JLCC_PROPORCION_SOCIAL_GRANDE;
        double jlccAnchoSocial = jlccAncho * jlccProporcionSocial;
        double jlccAnchoPasillo = jlccDormitorios >= 2 ? Math.min(1.0, jlccAncho * 0.12) : 0;
        double jlccAnchoPrivado = jlccAncho - jlccAnchoSocial - jlccAnchoPasillo;
        if (jlccAnchoPrivado < jlccAncho * 0.15) {
            jlccAnchoSocial = jlccAncho * 0.55;
            jlccAnchoPasillo = jlccDormitorios >= 2 ? Math.min(1.0, jlccAncho * 0.08) : 0;
            jlccAnchoPrivado = jlccAncho - jlccAnchoSocial - jlccAnchoPasillo;
        }

        List<Recinto> jlccRecintos = new ArrayList<>();
        boolean jlccAjustado = false;

        double jlccAltoCocina = Math.min(jlccAlto * 0.40, JLCC_AREA_COCINA_OBJETIVO / jlccAnchoSocial);
        if (jlccAltoCocina <= 0) jlccAltoCocina = jlccAlto * 0.10;
        double jlccAltoSala = jlccAlto - jlccAltoCocina;
        jlccRecintos.add(new Recinto(JlccTipoAmbiente.COCINA, 0, jlccAltoSala, jlccAnchoSocial, jlccAltoCocina));
        jlccRecintos.add(new Recinto(JlccTipoAmbiente.SALA_COMEDOR, 0, 0, jlccAnchoSocial, jlccAltoSala));
        if (jlccAnchoSocial * jlccAltoCocina < JLCC_AREA_COCINA_MINIMA) jlccAjustado = true;
        if (jlccAnchoSocial * jlccAltoSala < jlccAncho * jlccAlto * 0.30) jlccAjustado = true;

        if (jlccAnchoPasillo > 0) {
            jlccRecintos.add(new Recinto(JlccTipoAmbiente.PASILLO, jlccAnchoSocial, 0, jlccAnchoPasillo, jlccAlto));
        }

        double jlccXPrivado = jlccAnchoSocial + jlccAnchoPasillo;
        double jlccObjetivoDormitorios =
                JLCC_AREA_DORMITORIO_PRINCIPAL_OBJETIVO
                        + Math.max(0, jlccDormitorios - 1) * JLCC_AREA_DORMITORIO_OBJETIVO;

        double jlccAnchoBano = 0;
        double jlccAnchoBanosTotal = 0;
        double jlccAltoBanos = 0;
        if (jlccBanos > 0) {
            double jlccAnchoObjetivo = Math.min(2.0, jlccAnchoPrivado / jlccBanos);
            jlccAnchoBanosTotal = Math.min(jlccAnchoPrivado, jlccAnchoObjetivo * jlccBanos);
            jlccAnchoBano = jlccAnchoBanosTotal / jlccBanos;
            jlccAltoBanos = Math.min(jlccAlto * 0.45, JLCC_AREA_BANO_OBJETIVO / jlccAnchoBano);
            if (jlccAltoBanos <= 0) jlccAltoBanos = Math.min(jlccAlto * 0.2, 1.0);
        }
        double jlccAnchoPasilloSuperior = Math.max(0, jlccAnchoPrivado - jlccAnchoBanosTotal);

        if (jlccBanos > 0) {
            double jlccYBanos = jlccAlto - jlccAltoBanos;
            if (jlccAnchoPasilloSuperior > 0.0001) {
                jlccRecintos.add(
                        new Recinto(
                                JlccTipoAmbiente.PASILLO_AUXILIAR,
                                jlccXPrivado,
                                jlccYBanos,
                                jlccAnchoPasilloSuperior,
                                jlccAltoBanos));
            }
            for (int i = 0; i < jlccBanos; i++) {
                jlccRecintos.add(
                        new Recinto(
                                JlccTipoAmbiente.BANO,
                                jlccXPrivado + jlccAnchoPasilloSuperior + i * jlccAnchoBano,
                                jlccYBanos,
                                jlccAnchoBano,
                                jlccAltoBanos));
            }
            if (jlccAnchoBano * jlccAltoBanos < JLCC_AREA_BANO_MINIMA) jlccAjustado = true;
        }

        double jlccAltoDormitorios = jlccAlto - jlccAltoBanos;
        if (jlccAltoDormitorios <= 0) {
            jlccAltoDormitorios = jlccAlto;
            jlccAltoBanos = 0;
        }
        double jlccAltoPrincipal =
                JLCC_AREA_DORMITORIO_PRINCIPAL_OBJETIVO
                        / Math.max(1, jlccObjetivoDormitorios)
                        * jlccAltoDormitorios;
        double jlccAltoOtros =
                JLCC_AREA_DORMITORIO_OBJETIVO / Math.max(1, jlccObjetivoDormitorios) * jlccAltoDormitorios;

        double y = jlccAlto - jlccAltoBanos;
        for (int i = 0; i < jlccDormitorios; i++) {
            double jlccAltura = i == jlccDormitorios - 1 ? y : (i == 0 ? jlccAltoPrincipal : jlccAltoOtros);
            if (jlccAltura <= 0) jlccAltura = Math.max(0.0001, y);
            y -= jlccAltura;
            JlccTipoAmbiente jlccTipo =
                    i == 0 ? JlccTipoAmbiente.DORMITORIO_PRINCIPAL : JlccTipoAmbiente.DORMITORIO;
            jlccRecintos.add(new Recinto(jlccTipo, jlccXPrivado, y, jlccAnchoPrivado, jlccAltura));
            double jlccArea = jlccAnchoPrivado * jlccAltura;
            if (jlccArea < JLCC_AREA_DORMITORIO_MINIMA) jlccAjustado = true;
        }

        double jlccAnchoPuerta = Math.min(JLCC_ANCHO_PUERTA, jlccAnchoSocial * 0.6);
        Puerta jlccPuerta = new Puerta(jlccAnchoSocial / 2, 0, jlccAnchoPuerta);
        return new Cuerpo(jlccRecintos, jlccPuerta, jlccAjustado);
    }
}

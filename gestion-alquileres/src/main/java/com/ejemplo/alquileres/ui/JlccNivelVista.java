package com.ejemplo.alquileres.ui;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum JlccNivelVista {
    EDIFICIO,
    PISO,
    UNIDAD;

    public boolean jlccPuedeIrA(JlccNivelVista jlccDestino) {
        if (jlccDestino == null || jlccDestino == this) return false;
        return switch (this) {
            case EDIFICIO -> jlccDestino == PISO;
            case PISO -> jlccDestino == UNIDAD || jlccDestino == EDIFICIO;
            case UNIDAD -> jlccDestino == PISO || jlccDestino == EDIFICIO;
        };
    }

    public JlccNivelVista jlccVolver() {
        return switch (this) {
            case EDIFICIO -> EDIFICIO;
            case PISO -> EDIFICIO;
            case UNIDAD -> PISO;
        };
    }

    public static Set<JlccNivelVista> jlccDestinosPermitidos(JlccNivelVista jlccOrigen) {
        if (jlccOrigen == null) return Set.of();
        EnumSet<JlccNivelVista> jlccDestinos = EnumSet.noneOf(JlccNivelVista.class);
        for (JlccNivelVista jlccCandidato : values()) {
            if (jlccOrigen.jlccPuedeIrA(jlccCandidato)) jlccDestinos.add(jlccCandidato);
        }
        return Collections.unmodifiableSet(jlccDestinos);
    }
}

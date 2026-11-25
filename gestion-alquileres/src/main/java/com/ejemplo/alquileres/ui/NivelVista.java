package com.ejemplo.alquileres.ui;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum NivelVista {
    EDIFICIO,
    PISO,
    UNIDAD;

    public boolean puedeIrA(NivelVista destino) {
        if (destino == null || destino == this) return false;
        return switch (this) {
            case EDIFICIO -> destino == PISO;
            case PISO -> destino == UNIDAD || destino == EDIFICIO;
            case UNIDAD -> destino == PISO || destino == EDIFICIO;
        };
    }

    public NivelVista volver() {
        return switch (this) {
            case EDIFICIO -> EDIFICIO;
            case PISO -> EDIFICIO;
            case UNIDAD -> PISO;
        };
    }

    public static Set<NivelVista> destinosPermitidos(NivelVista origen) {
        if (origen == null) return Set.of();
        EnumSet<NivelVista> destinos = EnumSet.noneOf(NivelVista.class);
        for (NivelVista candidato : values()) {
            if (origen.puedeIrA(candidato)) destinos.add(candidato);
        }
        return Collections.unmodifiableSet(destinos);
    }
}

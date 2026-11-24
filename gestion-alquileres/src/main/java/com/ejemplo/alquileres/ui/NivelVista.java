package com.ejemplo.alquileres.ui;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Niveles de navegación de la maqueta y transiciones permitidas entre ellos.
 * Es la única definición de la máquina de estados: el controlador solo cambia
 * de nivel después de comprobar {@link #puedeIrA(NivelVista)}.
 */
public enum NivelVista {
    EDIFICIO,
    PISO,
    UNIDAD;

    /**
     * Indica si se puede pasar de este nivel al destino. No se permiten
     * transiciones al mismo nivel ni saltos que se salten un nivel.
     */
    public boolean puedeIrA(NivelVista destino) {
        if (destino == null || destino == this) return false;
        return switch (this) {
            case EDIFICIO -> destino == PISO;
            case PISO -> destino == UNIDAD || destino == EDIFICIO;
            case UNIDAD -> destino == PISO || destino == EDIFICIO;
        };
    }

    /** Nivel al que se vuelve con Volver o Esc. */
    public NivelVista volver() {
        return switch (this) {
            case EDIFICIO -> EDIFICIO;
            case PISO -> EDIFICIO;
            case UNIDAD -> PISO;
        };
    }

    /** Destinos válidos desde el nivel indicado, en orden de declaración. */
    public static Set<NivelVista> destinosPermitidos(NivelVista origen) {
        if (origen == null) return Set.of();
        EnumSet<NivelVista> destinos = EnumSet.noneOf(NivelVista.class);
        for (NivelVista candidato : values()) {
            if (origen.puedeIrA(candidato)) destinos.add(candidato);
        }
        return Collections.unmodifiableSet(destinos);
    }
}

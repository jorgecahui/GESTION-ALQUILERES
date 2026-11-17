package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;

/**
 * Unidad que se dibuja como bloque en la maqueta 3D.
 * El semáforo de morosidad solo tiene sentido cuando el inmueble está alquilado.
 */
public record BloqueMaqueta(Long id, String codigo, String nombre, String tipo,
                            int nivel, int orden, EstadoInmueble estado,
                            Long contratoId, String arrendatario,
                            SemaforoMorosidad semaforo, boolean inconsistencia) {

    public BloqueMaqueta {
        if (estado == null) throw new IllegalArgumentException("El estado del bloque es obligatorio");
        if (estado != EstadoInmueble.ALQUILADO) semaforo = null;
    }

    /** Constructor compatible con los datos de ejemplo del paso 1. */
    public BloqueMaqueta(Long id, String etiqueta, int nivel, int orden,
                         EstadoInmueble estado, SemaforoMorosidad semaforo) {
        this(id, null, etiqueta, null, nivel, orden, estado, null, null, semaforo, false);
    }

    public String etiqueta() {
        return nombre;
    }
}

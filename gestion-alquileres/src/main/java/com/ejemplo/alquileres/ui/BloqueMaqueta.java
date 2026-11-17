package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;

/**
 * Unidad que se dibuja como bloque en la maqueta 3D.
 * El semáforo de morosidad solo tiene sentido cuando el inmueble está alquilado.
 */
public record BloqueMaqueta(Long id, String etiqueta, int nivel, int orden,
                            EstadoInmueble estado, SemaforoMorosidad semaforo) {

    public BloqueMaqueta {
        if (estado == null) throw new IllegalArgumentException("El estado del bloque es obligatorio");
        if (estado != EstadoInmueble.ALQUILADO) semaforo = null;
    }
}

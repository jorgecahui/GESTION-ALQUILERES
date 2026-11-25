package com.ejemplo.alquileres.exception;

public class JlccNegocioException extends RuntimeException {
    public JlccNegocioException(String jlccMensaje) {
        super(jlccMensaje);
    }

    public JlccNegocioException(String jlccMensaje, Throwable jlccCausa) {
        super(jlccMensaje, jlccCausa);
    }
}

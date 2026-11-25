package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.TipoDocumento;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class JlccValidadorDocumento {
    public String jlccNormalizar(TipoDocumento jlccTipo, String jlccNumero) {
        if (jlccTipo == null) throw new JlccNegocioException("Seleccione el tipo de documento");
        String jlccValor =
                jlccNumero == null ? "" : jlccNumero.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        boolean jlccValido =
                switch (jlccTipo) {
                    case DNI -> jlccValor.matches("\\d{8}");
                    case RUC -> jlccValor.matches("(?:10|15|17|20)\\d{9}");
                    case CE -> jlccValor.matches("[A-Z0-9]{9,12}");
                    case PASAPORTE -> jlccValor.matches("[A-Z0-9]{6,12}");
                };
        if (!jlccValido) throw new JlccNegocioException(jlccMensaje(jlccTipo));
        return jlccValor;
    }

    public boolean jlccEsValido(TipoDocumento jlccTipo, String jlccNumero) {
        try {
            jlccNormalizar(jlccTipo, jlccNumero);
            return true;
        } catch (JlccNegocioException e) {
            return false;
        }
    }

    private String jlccMensaje(TipoDocumento jlccTipo) {
        return switch (jlccTipo) {
            case DNI -> "El DNI debe tener 8 dígitos";
            case RUC -> "El RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20";
            case CE -> "El CE debe tener entre 9 y 12 caracteres alfanuméricos";
            case PASAPORTE -> "El pasaporte debe tener entre 6 y 12 caracteres alfanuméricos";
        };
    }
}

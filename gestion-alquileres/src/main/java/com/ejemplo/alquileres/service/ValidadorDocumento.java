package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.TipoDocumento;
import org.springframework.stereotype.Component;
import java.util.Locale;

/** Reglas de identificación peruanas reutilizables por personas y propietarios. */
@Component
public class ValidadorDocumento {
    public String normalizar(TipoDocumento tipo, String numero) {
        if (tipo == null) throw new NegocioException("Seleccione el tipo de documento");
        String valor = numero == null ? "" : numero.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        boolean valido = switch (tipo) {
            case DNI -> valor.matches("\\d{8}");
            case RUC -> valor.matches("(?:10|15|17|20)\\d{9}");
            case CE -> valor.matches("[A-Z0-9]{9,12}");
            case PASAPORTE -> valor.matches("[A-Z0-9]{6,12}");
        };
        if (!valido) throw new NegocioException(mensaje(tipo));
        return valor;
    }
    public boolean esValido(TipoDocumento tipo, String numero) { try { normalizar(tipo, numero); return true; } catch (NegocioException e) { return false; } }
    private String mensaje(TipoDocumento tipo) { return switch (tipo) {
        case DNI -> "El DNI debe tener 8 dígitos";
        case RUC -> "El RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20";
        case CE -> "El CE debe tener entre 9 y 12 caracteres alfanuméricos";
        case PASAPORTE -> "El pasaporte debe tener entre 6 y 12 caracteres alfanuméricos";
    }; }
}

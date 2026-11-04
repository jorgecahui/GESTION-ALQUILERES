package com.ejemplo.alquileres.service;
import com.ejemplo.alquileres.model.TipoDocumento; import org.junit.jupiter.api.*; import static org.junit.jupiter.api.Assertions.*;
class ValidadorDocumentoTest { private final ValidadorDocumento validador=new ValidadorDocumento();
 @Test void aceptaDocumentosValidos(){assertEquals("12345678",validador.normalizar(TipoDocumento.DNI,"12345678"));assertTrue(validador.esValido(TipoDocumento.RUC,"20123456789"));assertTrue(validador.esValido(TipoDocumento.CE,"CE1234567"));assertTrue(validador.esValido(TipoDocumento.PASAPORTE,"AB123456"));}
 @Test void rechazaLongitudesPrefijosYCaracteresInvalidos(){assertFalse(validador.esValido(TipoDocumento.DNI,"1234567"));assertFalse(validador.esValido(TipoDocumento.RUC,"11123456789"));assertFalse(validador.esValido(TipoDocumento.CE,"ABC-123"));assertFalse(validador.esValido(TipoDocumento.PASAPORTE,"A12"));}
}

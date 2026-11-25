package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.model.TipoRecordatorio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

class PlantillasMensajeTest {
    @Test
    void reemplazaMarcadoresYFormateaImportesYFechas() {
        String resultado =
                PlantillasMensaje.aplicar(
                        "Hola {nombre}, debe {monto} con vencimiento {vencimiento}.",
                        Map.of(
                                "nombre",
                                "Ana Pérez",
                                "monto",
                                PlantillasMensaje.soles(new BigDecimal("1250")),
                                "vencimiento",
                                PlantillasMensaje.fecha(LocalDate.of(2026, 1, 5))));

        assertEquals(
                "Hola Ana Pérez, debe S/ 1,250.00 con vencimiento 05 de enero de 2026.", resultado);
        assertTrue(PlantillasMensaje.fecha(LocalDate.of(2026, 1, 5)).contains("enero"));
    }

    @Test
    void eliminaMarcadoresSinDatoYEspaciosSobrantes() {
        String resultado =
                PlantillasMensaje.aplicar(
                        "Hola {nombre}, {sin_dato} gracias.", Map.of("nombre", "Ana"));

        assertEquals("Hola Ana, gracias.", resultado);
        assertFalse(resultado.contains("{"));
    }

    @Test
    void usaPlantillaPersonalizadaYVuelveAlValorPorDefecto() {
        assertEquals(
                "Estimado {nombre}: regularice {monto}.",
                PlantillasMensaje.resolver(
                        TipoRecordatorio.VENCIDO, "Estimado {nombre}: regularice {monto}."));
        assertEquals(
                PlantillasMensaje.PREVENTIVO,
                PlantillasMensaje.resolver(TipoRecordatorio.PREVENTIVO, null));
        assertEquals(
                PlantillasMensaje.PREVENTIVO,
                PlantillasMensaje.resolver(TipoRecordatorio.PREVENTIVO, "  "));
    }

    @Test
    void formateaCeroCuandoElImporteEsNulo() {
        assertEquals("S/ 0.00", PlantillasMensaje.soles(null));
    }
}

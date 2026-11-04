package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.EstadoPago;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.Pago;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.service.ContratoService.EstadoVisual;
import com.ejemplo.alquileres.service.CuentasPorCobrarService.Cuenta;
import com.ejemplo.alquileres.service.CuentasPorCobrarService.Filtro;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReportesExcelTest {
    @Test void reporteDePagosIncluyeResumenPagosYDetalle() throws Exception {
        Contrato contrato = DatosFase6.contrato();
        Pago emitido = DatosFase6.pago(contrato, contrato.getCuotas().get(0), "R001-00000001", "1200.00", EstadoPago.EMITIDO);
        Pago anulado = DatosFase6.pago(contrato, contrato.getCuotas().get(0), "R001-00000002", "500.00", EstadoPago.ANULADO);
        ReportePagosExcelService servicio = new ReportePagosExcelService(DatosFase6.CLOCK);

        byte[] bytes = servicio.generar(List.of(emitido, anulado), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        try (XSSFWorkbook libro = leer(bytes)) {
            Sheet resumen = hoja(libro, "Resumen");
            assertEquals(1, resumen.getRow(4).getCell(1).getNumericCellValue());
            assertEquals(1, resumen.getRow(5).getCell(1).getNumericCellValue());
            assertEquals(1200.00, resumen.getRow(6).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(0.00, resumen.getRow(7).getCell(1).getNumericCellValue(), 0.001);
            assertEquals("R001-00000001", hoja(libro, "Pagos").getRow(1).getCell(0).getStringCellValue());
            assertEquals(2, hoja(libro, "Detalle de conceptos").getLastRowNum());
        }
    }

    @Test void reporteDeCuentasPorCobrarSeparaCuotasYMorosos() throws Exception {
        Cuenta vencida = new Cuenta(1L, 20L, "Ana Pérez", "Departamento 101", "Alquiler",
                LocalDate.of(2026, 2, 1), 9, true, dinero("1200.00"), dinero("10.80"),
                dinero("1210.80"), SemaforoMorosidad.MOROSO);
        Cuenta vigente = new Cuenta(1L, 21L, "Luis Gómez", "Local 2", "Alquiler",
                LocalDate.of(2026, 2, 20), 0, false, dinero("500.00"), dinero("0.00"),
                dinero("500.00"), SemaforoMorosidad.AL_DIA);
        ReporteCuentasPorCobrarExcelService servicio = new ReporteCuentasPorCobrarExcelService(DatosFase6.CLOCK);

        byte[] bytes = servicio.generar(List.of(vencida, vigente), Filtro.TODAS, "Ana");

        try (XSSFWorkbook libro = leer(bytes)) {
            Sheet resumen = hoja(libro, "Resumen");
            assertEquals(2, resumen.getRow(5).getCell(1).getNumericCellValue());
            assertEquals(1, resumen.getRow(6).getCell(1).getNumericCellValue());
            assertEquals(1, resumen.getRow(7).getCell(1).getNumericCellValue());
            assertEquals(1700.00, resumen.getRow(8).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(10.80, resumen.getRow(9).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(1710.80, resumen.getRow(10).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(2, hoja(libro, "Cuotas pendientes").getLastRowNum());
            assertEquals(1, hoja(libro, "Morosos").getLastRowNum());
        }
    }

    @Test void reporteDeContratosCuentaEstadosYPorVencer() throws Exception {
        ContratoService contratos = Mockito.mock(ContratoService.class);
        when(contratos.estadoVisual(any(Contrato.class))).thenAnswer(invocacion -> {
            Contrato contrato = invocacion.getArgument(0);
            return "CTR-000001".equals(contrato.getCodigo()) ? EstadoVisual.VIGENTE : EstadoVisual.POR_VENCER;
        });
        Contrato vigente = DatosFase6.contrato();
        Contrato porVencer = DatosFase6.contrato();
        porVencer.setCodigo("CTR-000002");
        ReporteContratosExcelService servicio = new ReporteContratosExcelService(contratos, DatosFase6.CLOCK);

        byte[] bytes = servicio.generar(List.of(vigente, porVencer), null, false);

        try (XSSFWorkbook libro = leer(bytes)) {
            Sheet resumen = hoja(libro, "Resumen");
            assertEquals(2, resumen.getRow(5).getCell(1).getNumericCellValue());
            assertEquals(1, resumen.getRow(6).getCell(1).getNumericCellValue());
            assertEquals(1, resumen.getRow(7).getCell(1).getNumericCellValue());
            assertEquals(0, resumen.getRow(8).getCell(1).getNumericCellValue());
            assertEquals(1200.00, resumen.getRow(9).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(2, hoja(libro, "Contratos").getLastRowNum());
            assertEquals(1, hoja(libro, "Por vencer").getLastRowNum());
        }
    }

    @Test void reporteDeInmueblesCalculaOcupacionPorTipo() throws Exception {
        Inmueble alquilado = DatosFase6.inmueble();
        Inmueble disponible = DatosFase6.inmueble();
        disponible.setCodigo("INM-0002");
        disponible.setNombre("Departamento 102");
        disponible.setEstado(EstadoInmueble.DISPONIBLE);
        ReporteInmueblesExcelService servicio = new ReporteInmueblesExcelService(DatosFase6.CLOCK);

        byte[] bytes = servicio.generar(List.of(alquilado, disponible));

        try (XSSFWorkbook libro = leer(bytes)) {
            Sheet resumen = hoja(libro, "Resumen");
            assertEquals(2, resumen.getRow(3).getCell(1).getNumericCellValue());
            assertEquals(1, resumen.getRow(4).getCell(1).getNumericCellValue());
            assertEquals(1, resumen.getRow(5).getCell(1).getNumericCellValue());
            assertEquals(50.00, resumen.getRow(8).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(2400.00, resumen.getRow(9).getCell(1).getNumericCellValue(), 0.001);
            Sheet ocupacion = hoja(libro, "Ocupación por tipo");
            assertEquals("Departamento", ocupacion.getRow(1).getCell(0).getStringCellValue());
            assertEquals(2, ocupacion.getRow(1).getCell(1).getNumericCellValue());
            assertEquals(1, ocupacion.getRow(1).getCell(2).getNumericCellValue());
        }
    }

    @Test void reporteDeMantenimientoIncluyeRentabilidad() throws Exception {
        ReporteMantenimientoExcelService servicio = new ReporteMantenimientoExcelService(DatosFase6.CLOCK);

        byte[] bytes = servicio.generar(List.of(DatosFase6.mantenimiento()), DatosFase6.rentabilidad(),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        try (XSSFWorkbook libro = leer(bytes)) {
            Sheet resumen = hoja(libro, "Resumen");
            assertEquals(1, resumen.getRow(4).getCell(1).getNumericCellValue());
            assertEquals(180.00, resumen.getRow(5).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(1230.00, resumen.getRow(6).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(200.00, resumen.getRow(7).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(1030.00, resumen.getRow(8).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(83.74, resumen.getRow(9).getCell(1).getNumericCellValue(), 0.001);
            assertEquals("MNT-000001", hoja(libro, "Mantenimientos").getRow(1).getCell(0).getStringCellValue());
            assertEquals("INM-0001", hoja(libro, "Rentabilidad por inmueble").getRow(1).getCell(0).getStringCellValue());
        }
    }

    private XSSFWorkbook leer(byte[] bytes) throws Exception {
        assertTrue(bytes.length > 100);
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    private Sheet hoja(XSSFWorkbook libro, String nombre) {
        Sheet hoja = libro.getSheet(nombre);
        assertNotNull(hoja, "Falta la hoja " + nombre);
        return hoja;
    }

    private static java.math.BigDecimal dinero(String valor) {
        return new java.math.BigDecimal(valor);
    }
}

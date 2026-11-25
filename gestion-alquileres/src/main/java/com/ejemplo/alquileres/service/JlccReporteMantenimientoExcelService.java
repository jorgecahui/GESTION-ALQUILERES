package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.Mantenimiento;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JlccReporteMantenimientoExcelService {
    private final Clock jlccClock;

    public byte[] jlccGenerar(
            List<Mantenimiento> jlccMantenimientos,
            JlccRentabilidadService.Resumen jlccRentabilidad,
            LocalDate jlccDesde,
            LocalDate jlccHasta) {
        XSSFWorkbook jlccLibro = new XSSFWorkbook();
        try {
            CellStyle jlccMoneda = JlccLibroExcel.jlccMoneda(jlccLibro);
            CellStyle jlccFecha = JlccLibroExcel.jlccFecha(jlccLibro);
            CellStyle jlccPorcentaje = JlccLibroExcel.jlccPorcentaje(jlccLibro);
            jlccResumen(jlccLibro, jlccMantenimientos, jlccRentabilidad, jlccDesde, jlccHasta, jlccMoneda, jlccFecha);
            jlccHojaMantenimientos(jlccLibro, jlccMantenimientos, jlccMoneda, jlccFecha);
            jlccHojaRentabilidad(jlccLibro, jlccRentabilidad, jlccMoneda, jlccPorcentaje);
            return JlccLibroExcel.jlccBytes(jlccLibro);
        } finally {
            jlccCerrar(jlccLibro);
        }
    }

    private void jlccResumen(
            XSSFWorkbook jlccLibro,
            List<Mantenimiento> jlccMantenimientos,
            JlccRentabilidadService.Resumen jlccRentabilidad,
            LocalDate jlccDesde,
            LocalDate jlccHasta,
            CellStyle jlccMoneda,
            CellStyle jlccFecha) {
        Sheet jlccHoja = JlccLibroExcel.jlccHoja(jlccLibro, "Resumen", "Concepto", "Valor");
        BigDecimal jlccCosto =
                jlccMantenimientos.stream()
                        .map(m -> m.getCostoReal() == null ? BigDecimal.ZERO : m.getCostoReal())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 0, "Módulo");
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 1, "Mantenimiento y rentabilidad");
        JlccLibroExcel.jlccTexto(jlccHoja, 2, 0, "Periodo");
        JlccLibroExcel.jlccTexto(
                jlccHoja,
                2,
                1,
                (jlccDesde == null ? "Sin desde" : jlccDesde.toString())
                        + " al "
                        + (jlccHasta == null ? "Sin hasta" : jlccHasta.toString()));
        JlccLibroExcel.jlccTexto(jlccHoja, 3, 0, "Generado");
        JlccLibroExcel.jlccFecha(jlccHoja, 3, 1, LocalDate.now(jlccClock), jlccFecha);
        JlccLibroExcel.jlccTexto(jlccHoja, 4, 0, "Mantenimientos");
        JlccLibroExcel.jlccNumero(jlccHoja, 4, 1, jlccMantenimientos.size());
        JlccLibroExcel.jlccTexto(jlccHoja, 5, 0, "Costo real registrado");
        JlccLibroExcel.jlccNumero(jlccHoja, 5, 1, jlccCosto, jlccMoneda);
        if (jlccRentabilidad != null) {
            JlccLibroExcel.jlccTexto(jlccHoja, 6, 0, "Ingresos cobrados");
            JlccLibroExcel.jlccNumero(jlccHoja, 6, 1, jlccRentabilidad.ingresos(), jlccMoneda);
            JlccLibroExcel.jlccTexto(jlccHoja, 7, 0, "Gastos del propietario");
            JlccLibroExcel.jlccNumero(jlccHoja, 7, 1, jlccRentabilidad.gastos(), jlccMoneda);
            JlccLibroExcel.jlccTexto(jlccHoja, 8, 0, "Neto");
            JlccLibroExcel.jlccNumero(jlccHoja, 8, 1, jlccRentabilidad.neto(), jlccMoneda);
            JlccLibroExcel.jlccTexto(jlccHoja, 9, 0, "Margen");
            JlccLibroExcel.jlccNumero(jlccHoja, 9, 1, jlccRentabilidad.margen(), JlccLibroExcel.jlccPorcentaje(jlccLibro));
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 2);
    }

    private void jlccHojaMantenimientos(
            XSSFWorkbook jlccLibro,
            List<Mantenimiento> jlccMantenimientos,
            CellStyle jlccMoneda,
            CellStyle jlccFecha) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Mantenimientos",
                        "Código",
                        "Inmueble",
                        "Título",
                        "Categoría",
                        "Prioridad",
                        "Estado",
                        "Responsable",
                        "Fecha reporte",
                        "Fecha resolución",
                        "Costo estimado",
                        "Costo real",
                        "Técnico");
        int jlccFila = 1;
        for (Mantenimiento m : jlccMantenimientos) {
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, m.getCodigo());
            JlccLibroExcel.jlccTexto(
                    jlccHoja, jlccFila, 1, m.getInmueble() == null ? "" : m.getInmueble().getNombre());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 2, m.getTitulo());
            JlccLibroExcel.jlccTexto(
                    jlccHoja, jlccFila, 3, m.getCategoria() == null ? "" : m.getCategoria().getEtiqueta());
            JlccLibroExcel.jlccTexto(
                    jlccHoja, jlccFila, 4, m.getPrioridad() == null ? "" : m.getPrioridad().getEtiqueta());
            JlccLibroExcel.jlccTexto(
                    jlccHoja, jlccFila, 5, m.getEstado() == null ? "" : m.getEstado().getEtiqueta());
            JlccLibroExcel.jlccTexto(
                    jlccHoja,
                    jlccFila,
                    6,
                    m.getResponsablePago() == null ? "" : m.getResponsablePago().getEtiqueta());
            JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 7, m.getFechaReporte(), jlccFecha);
            JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 8, m.getFechaResolucion(), jlccFecha);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 9, m.getCostoEstimado(), jlccMoneda);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 10, m.getCostoReal(), jlccMoneda);
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 11, m.getTecnico());
            jlccFila++;
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 12);
    }

    private void jlccHojaRentabilidad(
            XSSFWorkbook jlccLibro,
            JlccRentabilidadService.Resumen jlccRentabilidad,
            CellStyle jlccMoneda,
            CellStyle jlccPorcentaje) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Rentabilidad por inmueble",
                        "Código",
                        "Inmueble",
                        "Capital cobrado",
                        "Mora cobrada",
                        "Ingresos",
                        "Gastos",
                        "Neto",
                        "Margen",
                        "Días ocupado",
                        "Ocupación");
        int jlccFila = 1;
        if (jlccRentabilidad != null) {
            for (JlccRentabilidadService.Fila f : jlccRentabilidad.filas()) {
                JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, f.codigo());
                JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 1, f.inmueble());
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 2, f.capital(), jlccMoneda);
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 3, f.mora(), jlccMoneda);
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 4, f.ingresos(), jlccMoneda);
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 5, f.gastos(), jlccMoneda);
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 6, f.neto(), jlccMoneda);
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 7, f.margen(), jlccPorcentaje);
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 8, f.diasOcupado());
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 9, f.ocupacion(), jlccPorcentaje);
                jlccFila++;
            }
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 10);
    }

    private void jlccCerrar(XSSFWorkbook jlccLibro) {
        try {
            jlccLibro.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cerrar el Excel.", e);
        }
    }
}

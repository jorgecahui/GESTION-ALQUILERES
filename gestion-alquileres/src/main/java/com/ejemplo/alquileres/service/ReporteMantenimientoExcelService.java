package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.Mantenimiento;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** Reporte conjunto de mantenimientos y rentabilidad por inmueble. */
@Service
@RequiredArgsConstructor
public class ReporteMantenimientoExcelService {
    private final Clock clock;

    public byte[] generar(List<Mantenimiento> mantenimientos, RentabilidadService.Resumen rentabilidad,
                          LocalDate desde, LocalDate hasta) {
        XSSFWorkbook libro = new XSSFWorkbook();
        try {
            CellStyle moneda = LibroExcel.moneda(libro);
            CellStyle fecha = LibroExcel.fecha(libro);
            CellStyle porcentaje = LibroExcel.porcentaje(libro);
            resumen(libro, mantenimientos, rentabilidad, desde, hasta, moneda, fecha);
            hojaMantenimientos(libro, mantenimientos, moneda, fecha);
            hojaRentabilidad(libro, rentabilidad, moneda, porcentaje);
            return LibroExcel.bytes(libro);
        } finally {
            cerrar(libro);
        }
    }

    private void resumen(XSSFWorkbook libro, List<Mantenimiento> mantenimientos,
                         RentabilidadService.Resumen rentabilidad, LocalDate desde, LocalDate hasta,
                         CellStyle moneda, CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Resumen", "Concepto", "Valor");
        BigDecimal costo = mantenimientos.stream().map(m -> m.getCostoReal() == null ? BigDecimal.ZERO : m.getCostoReal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LibroExcel.texto(hoja, 1, 0, "Módulo");
        LibroExcel.texto(hoja, 1, 1, "Mantenimiento y rentabilidad");
        LibroExcel.texto(hoja, 2, 0, "Periodo");
        LibroExcel.texto(hoja, 2, 1, (desde == null ? "Sin desde" : desde.toString()) + " al " + (hasta == null ? "Sin hasta" : hasta.toString()));
        LibroExcel.texto(hoja, 3, 0, "Generado");
        LibroExcel.fecha(hoja, 3, 1, LocalDate.now(clock), fecha);
        LibroExcel.texto(hoja, 4, 0, "Mantenimientos");
        LibroExcel.numero(hoja, 4, 1, mantenimientos.size());
        LibroExcel.texto(hoja, 5, 0, "Costo real registrado");
        LibroExcel.numero(hoja, 5, 1, costo, moneda);
        if (rentabilidad != null) {
            LibroExcel.texto(hoja, 6, 0, "Ingresos cobrados");
            LibroExcel.numero(hoja, 6, 1, rentabilidad.ingresos(), moneda);
            LibroExcel.texto(hoja, 7, 0, "Gastos del propietario");
            LibroExcel.numero(hoja, 7, 1, rentabilidad.gastos(), moneda);
            LibroExcel.texto(hoja, 8, 0, "Neto");
            LibroExcel.numero(hoja, 8, 1, rentabilidad.neto(), moneda);
            LibroExcel.texto(hoja, 9, 0, "Margen");
            LibroExcel.numero(hoja, 9, 1, rentabilidad.margen(), LibroExcel.porcentaje(libro));
        }
        LibroExcel.cerrarHoja(hoja, 2);
    }

    private void hojaMantenimientos(XSSFWorkbook libro, List<Mantenimiento> mantenimientos,
                                    CellStyle moneda, CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Mantenimientos", "Código", "Inmueble", "Título", "Categoría",
                "Prioridad", "Estado", "Responsable", "Fecha reporte", "Fecha resolución", "Costo estimado",
                "Costo real", "Técnico");
        int fila = 1;
        for (Mantenimiento m : mantenimientos) {
            LibroExcel.texto(hoja, fila, 0, m.getCodigo());
            LibroExcel.texto(hoja, fila, 1, m.getInmueble() == null ? "" : m.getInmueble().getNombre());
            LibroExcel.texto(hoja, fila, 2, m.getTitulo());
            LibroExcel.texto(hoja, fila, 3, m.getCategoria() == null ? "" : m.getCategoria().name());
            LibroExcel.texto(hoja, fila, 4, m.getPrioridad() == null ? "" : m.getPrioridad().name());
            LibroExcel.texto(hoja, fila, 5, m.getEstado() == null ? "" : m.getEstado().name());
            LibroExcel.texto(hoja, fila, 6, m.getResponsablePago() == null ? "" : m.getResponsablePago().name());
            LibroExcel.fecha(hoja, fila, 7, m.getFechaReporte(), fecha);
            LibroExcel.fecha(hoja, fila, 8, m.getFechaResolucion(), fecha);
            LibroExcel.numero(hoja, fila, 9, m.getCostoEstimado(), moneda);
            LibroExcel.numero(hoja, fila, 10, m.getCostoReal(), moneda);
            LibroExcel.texto(hoja, fila, 11, m.getTecnico());
            fila++;
        }
        LibroExcel.cerrarHoja(hoja, 12);
    }

    private void hojaRentabilidad(XSSFWorkbook libro, RentabilidadService.Resumen rentabilidad,
                                  CellStyle moneda, CellStyle porcentaje) {
        Sheet hoja = LibroExcel.hoja(libro, "Rentabilidad por inmueble", "Código", "Inmueble", "Capital cobrado",
                "Mora cobrada", "Ingresos", "Gastos", "Neto", "Margen", "Días ocupado", "Ocupación");
        int fila = 1;
        if (rentabilidad != null) {
            for (RentabilidadService.Fila f : rentabilidad.filas()) {
                LibroExcel.texto(hoja, fila, 0, f.codigo());
                LibroExcel.texto(hoja, fila, 1, f.inmueble());
                LibroExcel.numero(hoja, fila, 2, f.capital(), moneda);
                LibroExcel.numero(hoja, fila, 3, f.mora(), moneda);
                LibroExcel.numero(hoja, fila, 4, f.ingresos(), moneda);
                LibroExcel.numero(hoja, fila, 5, f.gastos(), moneda);
                LibroExcel.numero(hoja, fila, 6, f.neto(), moneda);
                LibroExcel.numero(hoja, fila, 7, f.margen(), porcentaje);
                LibroExcel.numero(hoja, fila, 8, f.diasOcupado());
                LibroExcel.numero(hoja, fila, 9, f.ocupacion(), porcentaje);
                fila++;
            }
        }
        LibroExcel.cerrarHoja(hoja, 10);
    }

    private void cerrar(XSSFWorkbook libro) {
        try {
            libro.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cerrar el Excel.", e);
        }
    }
}

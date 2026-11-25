package com.ejemplo.alquileres.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

public final class LibroExcel {
    private LibroExcel() {}

    public static byte[] bytes(XSSFWorkbook libro) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel.", e);
        }
    }

    public static CellStyle cabecera(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setBorderBottom(BorderStyle.THIN);
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        return estilo;
    }

    public static CellStyle moneda(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("\"S/\" #,##0.00"));
        return estilo;
    }

    public static CellStyle fecha(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("dd/MM/yyyy"));
        return estilo;
    }

    public static CellStyle porcentaje(XSSFWorkbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("0.00\"%\""));
        return estilo;
    }

    public static Sheet hoja(XSSFWorkbook libro, String nombre, String... columnas) {
        Sheet hoja = libro.createSheet(nombre);
        Row fila = hoja.createRow(0);
        CellStyle estilo = cabecera(libro);
        for (int i = 0; i < columnas.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(columnas[i]);
            celda.setCellStyle(estilo);
        }
        hoja.createFreezePane(0, 1);
        return hoja;
    }

    public static void cerrarHoja(Sheet hoja, int columnas) {
        int ultimaFila = Math.max(1, hoja.getLastRowNum());
        hoja.setAutoFilter(new CellRangeAddress(0, ultimaFila, 0, Math.max(0, columnas - 1)));
        for (int i = 0; i < columnas; i++) hoja.autoSizeColumn(i);
    }

    public static void texto(Sheet hoja, int fila, int columna, String valor) {
        Row row = fila(hoja, fila);
        row.createCell(columna).setCellValue(valor == null ? "" : valor);
    }

    public static void numero(
            Sheet hoja, int fila, int columna, BigDecimal valor, CellStyle estilo) {
        Row row = fila(hoja, fila);
        Cell celda = row.createCell(columna);
        celda.setCellValue(valor == null ? 0d : valor.doubleValue());
        if (estilo != null) celda.setCellStyle(estilo);
    }

    public static void numero(Sheet hoja, int fila, int columna, Number valor) {
        Row row = fila(hoja, fila);
        row.createCell(columna).setCellValue(valor == null ? 0d : valor.doubleValue());
    }

    public static void fecha(Sheet hoja, int fila, int columna, LocalDate valor, CellStyle estilo) {
        Row row = fila(hoja, fila);
        Cell celda = row.createCell(columna);
        if (valor == null) {
            celda.setCellValue("");
            return;
        }
        celda.setCellValue(
                Date.from(valor.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        if (estilo != null) celda.setCellStyle(estilo);
    }

    public static Row fila(Sheet hoja, int indice) {
        Row fila = hoja.getRow(indice);
        return fila == null ? hoja.createRow(indice) : fila;
    }
}

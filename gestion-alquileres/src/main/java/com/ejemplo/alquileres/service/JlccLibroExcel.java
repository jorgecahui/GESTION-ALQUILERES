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

public final class JlccLibroExcel {
    private JlccLibroExcel() {}

    public static byte[] jlccBytes(XSSFWorkbook jlccLibro) {
        try (ByteArrayOutputStream jlccSalida = new ByteArrayOutputStream()) {
            jlccLibro.write(jlccSalida);
            return jlccSalida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel.", e);
        }
    }

    public static CellStyle jlccCabecera(XSSFWorkbook jlccLibro) {
        CellStyle jlccEstilo = jlccLibro.createCellStyle();
        jlccEstilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        jlccEstilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        jlccEstilo.setAlignment(HorizontalAlignment.CENTER);
        jlccEstilo.setVerticalAlignment(VerticalAlignment.CENTER);
        jlccEstilo.setBorderBottom(BorderStyle.THIN);
        Font jlccFuente = jlccLibro.createFont();
        jlccFuente.setBold(true);
        jlccFuente.setColor(IndexedColors.WHITE.getIndex());
        jlccEstilo.setFont(jlccFuente);
        return jlccEstilo;
    }

    public static CellStyle jlccMoneda(XSSFWorkbook jlccLibro) {
        CellStyle jlccEstilo = jlccLibro.createCellStyle();
        jlccEstilo.setDataFormat(jlccLibro.createDataFormat().getFormat("\"S/\" #,##0.00"));
        return jlccEstilo;
    }

    public static CellStyle jlccFecha(XSSFWorkbook jlccLibro) {
        CellStyle jlccEstilo = jlccLibro.createCellStyle();
        jlccEstilo.setDataFormat(jlccLibro.createDataFormat().getFormat("dd/MM/yyyy"));
        return jlccEstilo;
    }

    public static CellStyle jlccPorcentaje(XSSFWorkbook jlccLibro) {
        CellStyle jlccEstilo = jlccLibro.createCellStyle();
        jlccEstilo.setDataFormat(jlccLibro.createDataFormat().getFormat("0.00\"%\""));
        return jlccEstilo;
    }

    public static Sheet jlccHoja(XSSFWorkbook jlccLibro, String jlccNombre, String... jlccColumnas) {
        Sheet jlccHoja = jlccLibro.createSheet(jlccNombre);
        Row jlccFila = jlccHoja.createRow(0);
        CellStyle jlccEstilo = jlccCabecera(jlccLibro);
        for (int i = 0; i < jlccColumnas.length; i++) {
            Cell jlccCelda = jlccFila.createCell(i);
            jlccCelda.setCellValue(jlccColumnas[i]);
            jlccCelda.setCellStyle(jlccEstilo);
        }
        jlccHoja.createFreezePane(0, 1);
        return jlccHoja;
    }

    public static void jlccCerrarHoja(Sheet jlccHoja, int jlccColumnas) {
        int jlccUltimaFila = Math.max(1, jlccHoja.getLastRowNum());
        jlccHoja.setAutoFilter(new CellRangeAddress(0, jlccUltimaFila, 0, Math.max(0, jlccColumnas - 1)));
        for (int i = 0; i < jlccColumnas; i++) jlccHoja.autoSizeColumn(i);
    }

    public static void jlccTexto(Sheet jlccHoja, int jlccFila, int jlccColumna, String jlccValor) {
        Row jlccRow = jlccFila(jlccHoja, jlccFila);
        jlccRow.createCell(jlccColumna).setCellValue(jlccValor == null ? "" : jlccValor);
    }

    public static void jlccNumero(
            Sheet jlccHoja, int jlccFila, int jlccColumna, BigDecimal jlccValor, CellStyle jlccEstilo) {
        Row jlccRow = jlccFila(jlccHoja, jlccFila);
        Cell jlccCelda = jlccRow.createCell(jlccColumna);
        jlccCelda.setCellValue(jlccValor == null ? 0d : jlccValor.doubleValue());
        if (jlccEstilo != null) jlccCelda.setCellStyle(jlccEstilo);
    }

    public static void jlccNumero(Sheet jlccHoja, int jlccFila, int jlccColumna, Number jlccValor) {
        Row jlccRow = jlccFila(jlccHoja, jlccFila);
        jlccRow.createCell(jlccColumna).setCellValue(jlccValor == null ? 0d : jlccValor.doubleValue());
    }

    public static void jlccFecha(Sheet jlccHoja, int jlccFila, int jlccColumna, LocalDate jlccValor, CellStyle jlccEstilo) {
        Row jlccRow = jlccFila(jlccHoja, jlccFila);
        Cell jlccCelda = jlccRow.createCell(jlccColumna);
        if (jlccValor == null) {
            jlccCelda.setCellValue("");
            return;
        }
        jlccCelda.setCellValue(
                Date.from(jlccValor.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        if (jlccEstilo != null) jlccCelda.setCellStyle(jlccEstilo);
    }

    public static Row jlccFila(Sheet jlccHoja, int jlccIndice) {
        Row jlccFila = jlccHoja.getRow(jlccIndice);
        return jlccFila == null ? jlccHoja.createRow(jlccIndice) : jlccFila;
    }
}

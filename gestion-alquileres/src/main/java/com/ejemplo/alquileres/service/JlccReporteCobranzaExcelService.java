package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class JlccReporteCobranzaExcelService {
    public byte[] jlccGenerar(
            JlccReporteCobranzaService.Resumen jlccCobros, JlccDashboardService.Resumen jlccDashboard) {
        XSSFWorkbook jlccLibro = new XSSFWorkbook();
        try {
            CellStyle jlccMoneda = jlccLibro.createCellStyle();
            jlccMoneda.setDataFormat(jlccLibro.createDataFormat().getFormat("\"S/\" #,##0.00"));
            jlccResumen(jlccLibro, jlccCobros, jlccDashboard, jlccMoneda);
            jlccCobros(jlccLibro, jlccCobros.cobros(), jlccMoneda);
            jlccCartera(jlccLibro, jlccDashboard, jlccMoneda);
            jlccVencimientos(jlccLibro, jlccDashboard);
            return JlccLibroExcel.jlccBytes(jlccLibro);
        } finally {
            try {
                jlccLibro.close();
            } catch (java.io.IOException e) {
                throw new IllegalStateException("No se pudo cerrar el Excel.", e);
            }
        }
    }

    private void jlccResumen(
            XSSFWorkbook l,
            JlccReporteCobranzaService.Resumen c,
            JlccDashboardService.Resumen d,
            CellStyle m) {
        Sheet s = l.createSheet("Resumen");
        jlccFila(s, 0, "Indicador", "Valor");
        jlccDato(s, 1, "Periodo", c.desde() + " al " + c.hasta());
        jlccNumero(s, 2, "Total cobrado", c.total(), m);
        jlccNumero(s, 3, "Cartera vencida", d.cartera().total(), m);
        jlccDato(s, 4, "Ocupación", d.ocupacion().alquilados() + " de " + d.ocupacion().total());
    }

    private void jlccCobros(XSSFWorkbook l, List<Pago> jlccPs, CellStyle m) {
        Sheet s = l.createSheet("Cobros");
        jlccFila(
                s,
                0,
                "Recibo",
                "Fecha",
                "Arrendatario",
                "Inmueble",
                "Capital",
                "Mora",
                "Total",
                "Estado");
        int r = 1;
        for (Pago p : jlccPs) {
            Row x = s.createRow(r++);
            x.createCell(0).setCellValue(p.getRecibo());
            x.createCell(1).setCellValue(String.valueOf(p.getFechaPago()));
            x.createCell(2).setCellValue(p.getArrendatarioNombre());
            x.createCell(3).setCellValue(p.getInmuebleNombre());
            for (int i = 4; i < 7; i++) x.createCell(i).setCellStyle(m);
            x.getCell(4).setCellValue(p.getMontoCapital().doubleValue());
            x.getCell(5).setCellValue(p.getMontoMora().doubleValue());
            x.getCell(6).setCellValue(p.getMontoTotal().doubleValue());
            x.createCell(7).setCellValue(p.getEstado().getEtiqueta());
        }
        jlccAncho(s, 8);
    }

    private void jlccCartera(XSSFWorkbook l, JlccDashboardService.Resumen d, CellStyle m) {
        Sheet s = l.createSheet("Cartera vencida");
        jlccFila(s, 0, "Arrendatario", "Inmueble", "Deuda", "Días", "Semáforo");
        int r = 1;
        for (JlccDashboardService.Mora q : d.morosos()) {
            Row x = s.createRow(r++);
            x.createCell(0).setCellValue(q.arrendatario());
            x.createCell(1).setCellValue(q.inmueble());
            x.createCell(2).setCellValue(q.deuda().doubleValue());
            x.getCell(2).setCellStyle(m);
            x.createCell(3).setCellValue(q.dias());
            x.createCell(4).setCellValue(q.semaforo().getEtiqueta());
        }
        jlccAncho(s, 5);
    }

    private void jlccVencimientos(XSSFWorkbook l, JlccDashboardService.Resumen d) {
        Sheet s = l.createSheet("Contratos por vencer");
        jlccFila(s, 0, "Código", "Inmueble", "Arrendatario", "Fecha fin", "Días");
        int r = 1;
        for (JlccDashboardService.Vencimiento v : d.vencimientos()) {
            Row x = s.createRow(r++);
            x.createCell(0).setCellValue(v.codigo());
            x.createCell(1).setCellValue(v.inmueble());
            x.createCell(2).setCellValue(v.arrendatario());
            x.createCell(3).setCellValue(String.valueOf(v.fin()));
            x.createCell(4).setCellValue(v.dias());
        }
        jlccAncho(s, 5);
    }

    private void jlccFila(Sheet s, int r, String... v) {
        Row x = s.createRow(r);
        for (int i = 0; i < v.length; i++) x.createCell(i).setCellValue(v[i]);
    }

    private void jlccDato(Sheet s, int r, String a, String b) {
        jlccFila(s, r, a, b);
    }

    private void jlccNumero(Sheet s, int r, String a, BigDecimal b, CellStyle e) {
        Row x = s.createRow(r);
        x.createCell(0).setCellValue(a);
        x.createCell(1).setCellValue(b.doubleValue());
        x.getCell(1).setCellStyle(e);
    }

    private void jlccAncho(Sheet s, int n) {
        for (int i = 0; i < n; i++) s.autoSizeColumn(i);
    }
}

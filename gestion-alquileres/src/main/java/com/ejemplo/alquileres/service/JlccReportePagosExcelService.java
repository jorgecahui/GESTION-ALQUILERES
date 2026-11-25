package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.EstadoPago;
import com.ejemplo.alquileres.model.Pago;
import com.ejemplo.alquileres.model.PagoDetalle;

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
public class JlccReportePagosExcelService {
    private final Clock jlccClock;

    public byte[] jlccGenerar(List<Pago> jlccPagos, LocalDate jlccDesde, LocalDate jlccHasta) {
        XSSFWorkbook jlccLibro = new XSSFWorkbook();
        try {
            CellStyle jlccMoneda = JlccLibroExcel.jlccMoneda(jlccLibro);
            CellStyle jlccFecha = JlccLibroExcel.jlccFecha(jlccLibro);
            jlccResumen(jlccLibro, jlccPagos, jlccDesde, jlccHasta, jlccMoneda, jlccFecha);
            jlccHojaPagos(jlccLibro, jlccPagos, jlccMoneda, jlccFecha);
            jlccHojaDetalle(jlccLibro, jlccPagos, jlccMoneda, jlccFecha);
            return JlccLibroExcel.jlccBytes(jlccLibro);
        } finally {
            jlccCerrar(jlccLibro);
        }
    }

    private void jlccResumen(
            XSSFWorkbook jlccLibro,
            List<Pago> jlccPagos,
            LocalDate jlccDesde,
            LocalDate jlccHasta,
            CellStyle jlccMoneda,
            CellStyle jlccFecha) {
        Sheet jlccHoja = JlccLibroExcel.jlccHoja(jlccLibro, "Resumen", "Concepto", "Valor");
        long jlccEmitidos = jlccPagos.stream().filter(p -> p.getEstado() == EstadoPago.EMITIDO).count();
        long jlccAnulados = jlccPagos.size() - jlccEmitidos;
        BigDecimal jlccTotal =
                jlccPagos.stream()
                        .filter(p -> p.getEstado() == EstadoPago.EMITIDO)
                        .map(Pago::getMontoTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal jlccMora =
                jlccPagos.stream()
                        .filter(p -> p.getEstado() == EstadoPago.EMITIDO)
                        .map(Pago::getMontoMora)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 0, "Módulo");
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 1, "Pagos");
        JlccLibroExcel.jlccTexto(jlccHoja, 2, 0, "Filtro de fechas");
        JlccLibroExcel.jlccTexto(
                jlccHoja,
                2,
                1,
                (jlccDesde == null ? "Sin desde" : jlccDesde.toString())
                        + " al "
                        + (jlccHasta == null ? "Sin hasta" : jlccHasta.toString()));
        JlccLibroExcel.jlccTexto(jlccHoja, 3, 0, "Generado");
        JlccLibroExcel.jlccFecha(jlccHoja, 3, 1, LocalDate.now(jlccClock), jlccFecha);
        JlccLibroExcel.jlccTexto(jlccHoja, 4, 0, "Recibos emitidos");
        JlccLibroExcel.jlccNumero(jlccHoja, 4, 1, jlccEmitidos);
        JlccLibroExcel.jlccTexto(jlccHoja, 5, 0, "Recibos anulados");
        JlccLibroExcel.jlccNumero(jlccHoja, 5, 1, jlccAnulados);
        JlccLibroExcel.jlccTexto(jlccHoja, 6, 0, "Total cobrado");
        JlccLibroExcel.jlccNumero(jlccHoja, 6, 1, jlccTotal, jlccMoneda);
        JlccLibroExcel.jlccTexto(jlccHoja, 7, 0, "Mora cobrada");
        JlccLibroExcel.jlccNumero(jlccHoja, 7, 1, jlccMora, jlccMoneda);
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 2);
    }

    private void jlccHojaPagos(
            XSSFWorkbook jlccLibro, List<Pago> jlccPagos, CellStyle jlccMoneda, CellStyle jlccFecha) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Pagos",
                        "Recibo",
                        "Fecha",
                        "Arrendatario",
                        "Documento",
                        "Inmueble",
                        "Medio",
                        "Capital",
                        "Mora",
                        "Total",
                        "Estado",
                        "Referencia");
        int jlccFila = 1;
        for (Pago jlccPago : jlccPagos) {
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, jlccPago.getRecibo());
            JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 1, jlccPago.getFechaPago(), jlccFecha);
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 2, jlccPago.getArrendatarioNombre());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 3, jlccPago.getArrendatarioDocumento());
            JlccLibroExcel.jlccTexto(
                    jlccHoja, jlccFila, 4, jlccPago.getInmuebleCodigo() + " · " + jlccPago.getInmuebleNombre());
            JlccLibroExcel.jlccTexto(
                    jlccHoja,
                    jlccFila,
                    5,
                    jlccPago.getMedioPago() == null ? "" : jlccPago.getMedioPago().getEtiqueta());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 6, jlccPago.getMontoCapital(), jlccMoneda);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 7, jlccPago.getMontoMora(), jlccMoneda);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 8, jlccPago.getMontoTotal(), jlccMoneda);
            JlccLibroExcel.jlccTexto(
                    jlccHoja, jlccFila, 9, jlccPago.getEstado() == null ? "" : jlccPago.getEstado().getEtiqueta());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 10, jlccPago.getReferencia());
            jlccFila++;
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 11);
    }

    private void jlccHojaDetalle(
            XSSFWorkbook jlccLibro, List<Pago> jlccPagos, CellStyle jlccMoneda, CellStyle jlccFecha) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Detalle de conceptos",
                        "Recibo",
                        "Item",
                        "Concepto",
                        "Periodo",
                        "Vencimiento",
                        "Capital",
                        "Mora",
                        "Mora condonada",
                        "Estado del recibo");
        int jlccFila = 1;
        for (Pago jlccPago : jlccPagos) {
            for (PagoDetalle jlccDetalle : jlccPago.getDetalles()) {
                JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, jlccPago.getRecibo());
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 1, jlccDetalle.getItem());
                JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 2, jlccDetalle.getConcepto());
                JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 3, jlccDetalle.getPeriodo(), jlccFecha);
                JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 4, jlccDetalle.getFechaVencimiento(), jlccFecha);
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 5, jlccDetalle.getMontoCapital(), jlccMoneda);
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 6, jlccDetalle.getMontoMora(), jlccMoneda);
                JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 7, jlccDetalle.getMontoMoraCondonada(), jlccMoneda);
                JlccLibroExcel.jlccTexto(
                        jlccHoja,
                        jlccFila,
                        8,
                        jlccPago.getEstado() == null ? "" : jlccPago.getEstado().getEtiqueta());
                jlccFila++;
            }
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 9);
    }

    private void jlccCerrar(XSSFWorkbook jlccLibro) {
        try {
            jlccLibro.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cerrar el Excel.", e);
        }
    }
}

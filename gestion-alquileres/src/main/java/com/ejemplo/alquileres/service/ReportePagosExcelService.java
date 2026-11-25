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
public class ReportePagosExcelService {
    private final Clock clock;

    public byte[] generar(List<Pago> pagos, LocalDate desde, LocalDate hasta) {
        XSSFWorkbook libro = new XSSFWorkbook();
        try {
            CellStyle moneda = LibroExcel.moneda(libro);
            CellStyle fecha = LibroExcel.fecha(libro);
            resumen(libro, pagos, desde, hasta, moneda, fecha);
            hojaPagos(libro, pagos, moneda, fecha);
            hojaDetalle(libro, pagos, moneda, fecha);
            return LibroExcel.bytes(libro);
        } finally {
            cerrar(libro);
        }
    }

    private void resumen(
            XSSFWorkbook libro,
            List<Pago> pagos,
            LocalDate desde,
            LocalDate hasta,
            CellStyle moneda,
            CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Resumen", "Concepto", "Valor");
        long emitidos = pagos.stream().filter(p -> p.getEstado() == EstadoPago.EMITIDO).count();
        long anulados = pagos.size() - emitidos;
        BigDecimal total =
                pagos.stream()
                        .filter(p -> p.getEstado() == EstadoPago.EMITIDO)
                        .map(Pago::getMontoTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mora =
                pagos.stream()
                        .filter(p -> p.getEstado() == EstadoPago.EMITIDO)
                        .map(Pago::getMontoMora)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        LibroExcel.texto(hoja, 1, 0, "Módulo");
        LibroExcel.texto(hoja, 1, 1, "Pagos");
        LibroExcel.texto(hoja, 2, 0, "Filtro de fechas");
        LibroExcel.texto(
                hoja,
                2,
                1,
                (desde == null ? "Sin desde" : desde.toString())
                        + " al "
                        + (hasta == null ? "Sin hasta" : hasta.toString()));
        LibroExcel.texto(hoja, 3, 0, "Generado");
        LibroExcel.fecha(hoja, 3, 1, LocalDate.now(clock), fecha);
        LibroExcel.texto(hoja, 4, 0, "Recibos emitidos");
        LibroExcel.numero(hoja, 4, 1, emitidos);
        LibroExcel.texto(hoja, 5, 0, "Recibos anulados");
        LibroExcel.numero(hoja, 5, 1, anulados);
        LibroExcel.texto(hoja, 6, 0, "Total cobrado");
        LibroExcel.numero(hoja, 6, 1, total, moneda);
        LibroExcel.texto(hoja, 7, 0, "Mora cobrada");
        LibroExcel.numero(hoja, 7, 1, mora, moneda);
        LibroExcel.cerrarHoja(hoja, 2);
    }

    private void hojaPagos(
            XSSFWorkbook libro, List<Pago> pagos, CellStyle moneda, CellStyle fecha) {
        Sheet hoja =
                LibroExcel.hoja(
                        libro,
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
        int fila = 1;
        for (Pago pago : pagos) {
            LibroExcel.texto(hoja, fila, 0, pago.getRecibo());
            LibroExcel.fecha(hoja, fila, 1, pago.getFechaPago(), fecha);
            LibroExcel.texto(hoja, fila, 2, pago.getArrendatarioNombre());
            LibroExcel.texto(hoja, fila, 3, pago.getArrendatarioDocumento());
            LibroExcel.texto(
                    hoja, fila, 4, pago.getInmuebleCodigo() + " · " + pago.getInmuebleNombre());
            LibroExcel.texto(
                    hoja,
                    fila,
                    5,
                    pago.getMedioPago() == null ? "" : pago.getMedioPago().getEtiqueta());
            LibroExcel.numero(hoja, fila, 6, pago.getMontoCapital(), moneda);
            LibroExcel.numero(hoja, fila, 7, pago.getMontoMora(), moneda);
            LibroExcel.numero(hoja, fila, 8, pago.getMontoTotal(), moneda);
            LibroExcel.texto(
                    hoja, fila, 9, pago.getEstado() == null ? "" : pago.getEstado().getEtiqueta());
            LibroExcel.texto(hoja, fila, 10, pago.getReferencia());
            fila++;
        }
        LibroExcel.cerrarHoja(hoja, 11);
    }

    private void hojaDetalle(
            XSSFWorkbook libro, List<Pago> pagos, CellStyle moneda, CellStyle fecha) {
        Sheet hoja =
                LibroExcel.hoja(
                        libro,
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
        int fila = 1;
        for (Pago pago : pagos) {
            for (PagoDetalle detalle : pago.getDetalles()) {
                LibroExcel.texto(hoja, fila, 0, pago.getRecibo());
                LibroExcel.numero(hoja, fila, 1, detalle.getItem());
                LibroExcel.texto(hoja, fila, 2, detalle.getConcepto());
                LibroExcel.fecha(hoja, fila, 3, detalle.getPeriodo(), fecha);
                LibroExcel.fecha(hoja, fila, 4, detalle.getFechaVencimiento(), fecha);
                LibroExcel.numero(hoja, fila, 5, detalle.getMontoCapital(), moneda);
                LibroExcel.numero(hoja, fila, 6, detalle.getMontoMora(), moneda);
                LibroExcel.numero(hoja, fila, 7, detalle.getMontoMoraCondonada(), moneda);
                LibroExcel.texto(
                        hoja,
                        fila,
                        8,
                        pago.getEstado() == null ? "" : pago.getEstado().getEtiqueta());
                fila++;
            }
        }
        LibroExcel.cerrarHoja(hoja, 9);
    }

    private void cerrar(XSSFWorkbook libro) {
        try {
            libro.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cerrar el Excel.", e);
        }
    }
}

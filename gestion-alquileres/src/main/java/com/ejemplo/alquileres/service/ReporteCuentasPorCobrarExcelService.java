package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.service.CuentasPorCobrarService.Cuenta;
import com.ejemplo.alquileres.service.CuentasPorCobrarService.Filtro;
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

/** Reporte de cuotas pendientes y morosos. */
@Service
@RequiredArgsConstructor
public class ReporteCuentasPorCobrarExcelService {
    private final Clock clock;

    public byte[] generar(List<Cuenta> cuentas, Filtro filtro, String texto) {
        XSSFWorkbook libro = new XSSFWorkbook();
        try {
            CellStyle moneda = LibroExcel.moneda(libro);
            CellStyle fecha = LibroExcel.fecha(libro);
            resumen(libro, cuentas, filtro, texto, moneda, fecha);
            hojaCuotas(libro, cuentas, moneda, fecha);
            hojaMorosos(libro, cuentas, moneda, fecha);
            return LibroExcel.bytes(libro);
        } finally {
            cerrar(libro);
        }
    }

    private void resumen(XSSFWorkbook libro, List<Cuenta> cuentas, Filtro filtro, String texto,
                         CellStyle moneda, CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Resumen", "Concepto", "Valor");
        BigDecimal total = cuentas.stream().map(Cuenta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldo = cuentas.stream().map(Cuenta::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mora = cuentas.stream().map(Cuenta::moraEstimada).reduce(BigDecimal.ZERO, BigDecimal::add);
        long vencidas = cuentas.stream().filter(Cuenta::vencida).count();
        long morosos = cuentas.stream().filter(c -> c.semaforo() == com.ejemplo.alquileres.model.SemaforoMorosidad.MOROSO).count();
        LibroExcel.texto(hoja, 1, 0, "Módulo");
        LibroExcel.texto(hoja, 1, 1, "Cuentas por cobrar");
        LibroExcel.texto(hoja, 2, 0, "Filtro");
        LibroExcel.texto(hoja, 2, 1, filtro == null ? "TODAS" : filtro.name());
        LibroExcel.texto(hoja, 3, 0, "Búsqueda");
        LibroExcel.texto(hoja, 3, 1, texto == null || texto.isBlank() ? "Sin filtro de texto" : texto);
        LibroExcel.texto(hoja, 4, 0, "Generado");
        LibroExcel.fecha(hoja, 4, 1, LocalDate.now(clock), fecha);
        LibroExcel.texto(hoja, 5, 0, "Cuotas");
        LibroExcel.numero(hoja, 5, 1, cuentas.size());
        LibroExcel.texto(hoja, 6, 0, "Cuotas vencidas");
        LibroExcel.numero(hoja, 6, 1, vencidas);
        LibroExcel.texto(hoja, 7, 0, "Cuotas de morosos");
        LibroExcel.numero(hoja, 7, 1, morosos);
        LibroExcel.texto(hoja, 8, 0, "Saldo capital");
        LibroExcel.numero(hoja, 8, 1, saldo, moneda);
        LibroExcel.texto(hoja, 9, 0, "Mora estimada");
        LibroExcel.numero(hoja, 9, 1, mora, moneda);
        LibroExcel.texto(hoja, 10, 0, "Total por cobrar");
        LibroExcel.numero(hoja, 10, 1, total, moneda);
        LibroExcel.cerrarHoja(hoja, 2);
    }

    private void hojaCuotas(XSSFWorkbook libro, List<Cuenta> cuentas, CellStyle moneda, CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Cuotas pendientes", "Arrendatario", "Inmueble", "Concepto",
                "Vencimiento", "Días de atraso", "Saldo", "Mora estimada", "Total", "Vencida", "Semáforo");
        int fila = 1;
        for (Cuenta cuenta : cuentas) {
            LibroExcel.texto(hoja, fila, 0, cuenta.arrendatario());
            LibroExcel.texto(hoja, fila, 1, cuenta.inmueble());
            LibroExcel.texto(hoja, fila, 2, cuenta.concepto());
            LibroExcel.fecha(hoja, fila, 3, cuenta.vencimiento(), fecha);
            LibroExcel.numero(hoja, fila, 4, cuenta.diasAtraso());
            LibroExcel.numero(hoja, fila, 5, cuenta.saldo(), moneda);
            LibroExcel.numero(hoja, fila, 6, cuenta.moraEstimada(), moneda);
            LibroExcel.numero(hoja, fila, 7, cuenta.total(), moneda);
            LibroExcel.texto(hoja, fila, 8, cuenta.vencida() ? "Sí" : "No");
            LibroExcel.texto(hoja, fila, 9, cuenta.semaforo() == null ? "" : cuenta.semaforo().name());
            fila++;
        }
        LibroExcel.cerrarHoja(hoja, 10);
    }

    private void hojaMorosos(XSSFWorkbook libro, List<Cuenta> cuentas, CellStyle moneda, CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Morosos", "Arrendatario", "Inmueble", "Concepto",
                "Vencimiento", "Días de atraso", "Saldo", "Mora estimada", "Total", "Semáforo");
        int fila = 1;
        for (Cuenta cuenta : cuentas) {
            if (cuenta.semaforo() != com.ejemplo.alquileres.model.SemaforoMorosidad.MOROSO) continue;
            LibroExcel.texto(hoja, fila, 0, cuenta.arrendatario());
            LibroExcel.texto(hoja, fila, 1, cuenta.inmueble());
            LibroExcel.texto(hoja, fila, 2, cuenta.concepto());
            LibroExcel.fecha(hoja, fila, 3, cuenta.vencimiento(), fecha);
            LibroExcel.numero(hoja, fila, 4, cuenta.diasAtraso());
            LibroExcel.numero(hoja, fila, 5, cuenta.saldo(), moneda);
            LibroExcel.numero(hoja, fila, 6, cuenta.moraEstimada(), moneda);
            LibroExcel.numero(hoja, fila, 7, cuenta.total(), moneda);
            LibroExcel.texto(hoja, fila, 8, cuenta.semaforo() == null ? "" : cuenta.semaforo().name());
            fila++;
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

package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.service.JlccCuentasPorCobrarService.Cuenta;
import com.ejemplo.alquileres.service.JlccCuentasPorCobrarService.JlccFiltro;

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
public class JlccReporteCuentasPorCobrarExcelService {
    private final Clock jlccClock;

    public byte[] jlccGenerar(List<Cuenta> jlccCuentas, JlccFiltro jlccFiltro, String jlccTexto) {
        XSSFWorkbook jlccLibro = new XSSFWorkbook();
        try {
            CellStyle jlccMoneda = JlccLibroExcel.jlccMoneda(jlccLibro);
            CellStyle jlccFecha = JlccLibroExcel.jlccFecha(jlccLibro);
            jlccResumen(jlccLibro, jlccCuentas, jlccFiltro, jlccTexto, jlccMoneda, jlccFecha);
            jlccHojaCuotas(jlccLibro, jlccCuentas, jlccMoneda, jlccFecha);
            jlccHojaMorosos(jlccLibro, jlccCuentas, jlccMoneda, jlccFecha);
            return JlccLibroExcel.jlccBytes(jlccLibro);
        } finally {
            jlccCerrar(jlccLibro);
        }
    }

    private void jlccResumen(
            XSSFWorkbook jlccLibro,
            List<Cuenta> jlccCuentas,
            JlccFiltro jlccFiltro,
            String jlccTexto,
            CellStyle jlccMoneda,
            CellStyle jlccFecha) {
        Sheet jlccHoja = JlccLibroExcel.jlccHoja(jlccLibro, "Resumen", "Concepto", "Valor");
        BigDecimal jlccTotal =
                jlccCuentas.stream().map(Cuenta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal jlccSaldo =
                jlccCuentas.stream().map(Cuenta::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal jlccMora =
                jlccCuentas.stream().map(Cuenta::moraEstimada).reduce(BigDecimal.ZERO, BigDecimal::add);
        long jlccVencidas = jlccCuentas.stream().filter(Cuenta::vencida).count();
        long jlccMorosos =
                jlccCuentas.stream()
                        .filter(
                                c ->
                                        c.semaforo()
                                                == com.ejemplo.alquileres.model.SemaforoMorosidad
                                                        .MOROSO)
                        .count();
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 0, "Módulo");
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 1, "Cuentas por cobrar");
        JlccLibroExcel.jlccTexto(jlccHoja, 2, 0, "Filtro");
        JlccLibroExcel.jlccTexto(jlccHoja, 2, 1, jlccFiltro == null ? "Todas" : jlccFiltro.jlccGetEtiqueta());
        JlccLibroExcel.jlccTexto(jlccHoja, 3, 0, "Búsqueda");
        JlccLibroExcel.jlccTexto(
                jlccHoja, 3, 1, jlccTexto == null || jlccTexto.isBlank() ? "Sin filtro de texto" : jlccTexto);
        JlccLibroExcel.jlccTexto(jlccHoja, 4, 0, "Generado");
        JlccLibroExcel.jlccFecha(jlccHoja, 4, 1, LocalDate.now(jlccClock), jlccFecha);
        JlccLibroExcel.jlccTexto(jlccHoja, 5, 0, "Cuotas");
        JlccLibroExcel.jlccNumero(jlccHoja, 5, 1, jlccCuentas.size());
        JlccLibroExcel.jlccTexto(jlccHoja, 6, 0, "Cuotas vencidas");
        JlccLibroExcel.jlccNumero(jlccHoja, 6, 1, jlccVencidas);
        JlccLibroExcel.jlccTexto(jlccHoja, 7, 0, "Cuotas de morosos");
        JlccLibroExcel.jlccNumero(jlccHoja, 7, 1, jlccMorosos);
        JlccLibroExcel.jlccTexto(jlccHoja, 8, 0, "Saldo capital");
        JlccLibroExcel.jlccNumero(jlccHoja, 8, 1, jlccSaldo, jlccMoneda);
        JlccLibroExcel.jlccTexto(jlccHoja, 9, 0, "Mora estimada");
        JlccLibroExcel.jlccNumero(jlccHoja, 9, 1, jlccMora, jlccMoneda);
        JlccLibroExcel.jlccTexto(jlccHoja, 10, 0, "Total por cobrar");
        JlccLibroExcel.jlccNumero(jlccHoja, 10, 1, jlccTotal, jlccMoneda);
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 2);
    }

    private void jlccHojaCuotas(
            XSSFWorkbook jlccLibro, List<Cuenta> jlccCuentas, CellStyle jlccMoneda, CellStyle jlccFecha) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Cuotas pendientes",
                        "Arrendatario",
                        "Inmueble",
                        "Concepto",
                        "Vencimiento",
                        "Días de atraso",
                        "Saldo",
                        "Mora estimada",
                        "Total",
                        "Vencida",
                        "Semáforo");
        int jlccFila = 1;
        for (Cuenta jlccCuenta : jlccCuentas) {
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, jlccCuenta.arrendatario());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 1, jlccCuenta.inmueble());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 2, jlccCuenta.concepto());
            JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 3, jlccCuenta.vencimiento(), jlccFecha);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 4, jlccCuenta.diasAtraso());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 5, jlccCuenta.saldo(), jlccMoneda);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 6, jlccCuenta.moraEstimada(), jlccMoneda);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 7, jlccCuenta.total(), jlccMoneda);
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 8, jlccCuenta.vencida() ? "Sí" : "No");
            JlccLibroExcel.jlccTexto(
                    jlccHoja,
                    jlccFila,
                    9,
                    jlccCuenta.semaforo() == null ? "" : jlccCuenta.semaforo().getEtiqueta());
            jlccFila++;
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 10);
    }

    private void jlccHojaMorosos(
            XSSFWorkbook jlccLibro, List<Cuenta> jlccCuentas, CellStyle jlccMoneda, CellStyle jlccFecha) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Morosos",
                        "Arrendatario",
                        "Inmueble",
                        "Concepto",
                        "Vencimiento",
                        "Días de atraso",
                        "Saldo",
                        "Mora estimada",
                        "Total",
                        "Semáforo");
        int jlccFila = 1;
        for (Cuenta jlccCuenta : jlccCuentas) {
            if (jlccCuenta.semaforo() != com.ejemplo.alquileres.model.SemaforoMorosidad.MOROSO)
                continue;
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, jlccCuenta.arrendatario());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 1, jlccCuenta.inmueble());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 2, jlccCuenta.concepto());
            JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 3, jlccCuenta.vencimiento(), jlccFecha);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 4, jlccCuenta.diasAtraso());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 5, jlccCuenta.saldo(), jlccMoneda);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 6, jlccCuenta.moraEstimada(), jlccMoneda);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 7, jlccCuenta.total(), jlccMoneda);
            JlccLibroExcel.jlccTexto(
                    jlccHoja,
                    jlccFila,
                    8,
                    jlccCuenta.semaforo() == null ? "" : jlccCuenta.semaforo().getEtiqueta());
            jlccFila++;
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

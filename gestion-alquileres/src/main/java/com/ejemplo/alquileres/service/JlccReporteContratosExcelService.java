package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.service.JlccContratoService.JlccEstadoVisual;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JlccReporteContratosExcelService {
    private final JlccContratoService jlccContratos;
    private final Clock jlccClock;

    public byte[] jlccGenerar(List<Contrato> jlccLista, JlccEstadoVisual jlccFiltroEstado, boolean jlccSoloPorVencer) {
        XSSFWorkbook jlccLibro = new XSSFWorkbook();
        try {
            CellStyle jlccMoneda = JlccLibroExcel.jlccMoneda(jlccLibro);
            CellStyle jlccFecha = JlccLibroExcel.jlccFecha(jlccLibro);
            jlccResumen(jlccLibro, jlccLista, jlccFiltroEstado, jlccSoloPorVencer, jlccMoneda, jlccFecha);
            jlccHojaContratos(jlccLibro, jlccLista, jlccMoneda, jlccFecha);
            jlccHojaPorVencer(jlccLibro, jlccLista, jlccMoneda, jlccFecha);
            return JlccLibroExcel.jlccBytes(jlccLibro);
        } finally {
            jlccCerrar(jlccLibro);
        }
    }

    private void jlccResumen(
            XSSFWorkbook jlccLibro,
            List<Contrato> jlccLista,
            JlccEstadoVisual jlccFiltroEstado,
            boolean jlccSoloPorVencer,
            CellStyle jlccMoneda,
            CellStyle jlccFecha) {
        Sheet jlccHoja = JlccLibroExcel.jlccHoja(jlccLibro, "Resumen", "Concepto", "Valor");
        long jlccVigentes =
                jlccLista.stream()
                        .filter(c -> jlccContratos.jlccEstadoVisual(c) == JlccEstadoVisual.VIGENTE)
                        .count();
        long jlccPorVencer =
                jlccLista.stream()
                        .filter(c -> jlccContratos.jlccEstadoVisual(c) == JlccEstadoVisual.POR_VENCER)
                        .count();
        long jlccVencidos =
                jlccLista.stream()
                        .filter(c -> jlccContratos.jlccEstadoVisual(c) == JlccEstadoVisual.VENCIDO)
                        .count();
        BigDecimal jlccRenta =
                jlccLista.stream()
                        .filter(c -> jlccContratos.jlccEstadoVisual(c) == JlccEstadoVisual.VIGENTE)
                        .map(Contrato::getRentaMensual)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 0, "Módulo");
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 1, "Contratos");
        JlccLibroExcel.jlccTexto(jlccHoja, 2, 0, "Filtro de estado");
        JlccLibroExcel.jlccTexto(jlccHoja, 2, 1, jlccFiltroEstado == null ? "TODOS" : jlccFiltroEstado.jlccGetEtiqueta());
        JlccLibroExcel.jlccTexto(jlccHoja, 3, 0, "Solo por vencer");
        JlccLibroExcel.jlccTexto(jlccHoja, 3, 1, jlccSoloPorVencer ? "Sí" : "No");
        JlccLibroExcel.jlccTexto(jlccHoja, 4, 0, "Generado");
        JlccLibroExcel.jlccFecha(jlccHoja, 4, 1, LocalDate.now(jlccClock), jlccFecha);
        JlccLibroExcel.jlccTexto(jlccHoja, 5, 0, "Contratos");
        JlccLibroExcel.jlccNumero(jlccHoja, 5, 1, jlccLista.size());
        JlccLibroExcel.jlccTexto(jlccHoja, 6, 0, "Vigentes");
        JlccLibroExcel.jlccNumero(jlccHoja, 6, 1, jlccVigentes);
        JlccLibroExcel.jlccTexto(jlccHoja, 7, 0, "Por vencer");
        JlccLibroExcel.jlccNumero(jlccHoja, 7, 1, jlccPorVencer);
        JlccLibroExcel.jlccTexto(jlccHoja, 8, 0, "Vencidos");
        JlccLibroExcel.jlccNumero(jlccHoja, 8, 1, jlccVencidos);
        JlccLibroExcel.jlccTexto(jlccHoja, 9, 0, "Renta mensual vigente");
        JlccLibroExcel.jlccNumero(jlccHoja, 9, 1, jlccRenta, jlccMoneda);
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 2);
    }

    private void jlccHojaContratos(
            XSSFWorkbook jlccLibro, List<Contrato> jlccLista, CellStyle jlccMoneda, CellStyle jlccFecha) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Contratos",
                        "Código",
                        "Inmueble",
                        "Arrendatario",
                        "Documento",
                        "Inicio",
                        "Fin",
                        "Día de pago",
                        "Renta",
                        "Incremento %",
                        "Garantía",
                        "Estado",
                        "Estado visual");
        int jlccFila = 1;
        for (Contrato jlccContrato : jlccLista) {
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, jlccContrato.getCodigo());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 1, jlccContrato.getInmueble().getNombre());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 2, jlccContrato.getArrendatario().getNombreCompleto());
            JlccLibroExcel.jlccTexto(
                    jlccHoja,
                    jlccFila,
                    3,
                    jlccContrato.getArrendatario().getTipoDocumento()
                            + " "
                            + jlccContrato.getArrendatario().getNumeroDocumento());
            JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 4, jlccContrato.getFechaInicio(), jlccFecha);
            JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 5, jlccContrato.getFechaFin(), jlccFecha);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 6, jlccContrato.getDiaPago());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 7, jlccContrato.getRentaMensual(), jlccMoneda);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 8, jlccContrato.getIncrementoAnualPct());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 9, jlccContrato.getGarantiaMonto(), jlccMoneda);
            JlccLibroExcel.jlccTexto(
                    jlccHoja,
                    jlccFila,
                    10,
                    jlccContrato.getEstado() == null ? "" : jlccContrato.getEstado().getEtiqueta());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 11, jlccContratos.jlccEstadoVisual(jlccContrato).jlccGetEtiqueta());
            jlccFila++;
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 12);
    }

    private void jlccHojaPorVencer(
            XSSFWorkbook jlccLibro, List<Contrato> jlccLista, CellStyle jlccMoneda, CellStyle jlccFecha) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Por vencer",
                        "Código",
                        "Inmueble",
                        "Arrendatario",
                        "Fin",
                        "Días restantes",
                        "Renta",
                        "Teléfono");
        int jlccFila = 1;
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        for (Contrato jlccContrato : jlccLista) {
            if (jlccContratos.jlccEstadoVisual(jlccContrato) != JlccEstadoVisual.POR_VENCER) continue;
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, jlccContrato.getCodigo());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 1, jlccContrato.getInmueble().getNombre());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 2, jlccContrato.getArrendatario().getNombreCompleto());
            JlccLibroExcel.jlccFecha(jlccHoja, jlccFila, 3, jlccContrato.getFechaFin(), jlccFecha);
            JlccLibroExcel.jlccNumero(
                    jlccHoja,
                    jlccFila,
                    4,
                    Math.max(0, ChronoUnit.DAYS.between(jlccHoy, jlccContrato.getFechaFin())));
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 5, jlccContrato.getRentaMensual(), jlccMoneda);
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 6, jlccContrato.getArrendatario().getTelefono());
            jlccFila++;
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 7);
    }

    private void jlccCerrar(XSSFWorkbook jlccLibro) {
        try {
            jlccLibro.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cerrar el Excel.", e);
        }
    }
}

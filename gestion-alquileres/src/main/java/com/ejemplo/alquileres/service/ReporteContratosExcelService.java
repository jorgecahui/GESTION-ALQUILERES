package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.service.ContratoService.EstadoVisual;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** Reporte de contratos, vigencias y próximos vencimientos. */
@Service
@RequiredArgsConstructor
public class ReporteContratosExcelService {
    private final ContratoService contratos;
    private final Clock clock;

    public byte[] generar(List<Contrato> lista, EstadoVisual filtroEstado, boolean soloPorVencer) {
        XSSFWorkbook libro = new XSSFWorkbook();
        try {
            CellStyle moneda = LibroExcel.moneda(libro);
            CellStyle fecha = LibroExcel.fecha(libro);
            resumen(libro, lista, filtroEstado, soloPorVencer, moneda, fecha);
            hojaContratos(libro, lista, moneda, fecha);
            hojaPorVencer(libro, lista, moneda, fecha);
            return LibroExcel.bytes(libro);
        } finally {
            cerrar(libro);
        }
    }

    private void resumen(XSSFWorkbook libro, List<Contrato> lista, EstadoVisual filtroEstado,
                         boolean soloPorVencer, CellStyle moneda, CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Resumen", "Concepto", "Valor");
        long vigentes = lista.stream().filter(c -> contratos.estadoVisual(c) == EstadoVisual.VIGENTE).count();
        long porVencer = lista.stream().filter(c -> contratos.estadoVisual(c) == EstadoVisual.POR_VENCER).count();
        long vencidos = lista.stream().filter(c -> contratos.estadoVisual(c) == EstadoVisual.VENCIDO).count();
        BigDecimal renta = lista.stream().filter(c -> contratos.estadoVisual(c) == EstadoVisual.VIGENTE)
                .map(Contrato::getRentaMensual).reduce(BigDecimal.ZERO, BigDecimal::add);
        LibroExcel.texto(hoja, 1, 0, "Módulo");
        LibroExcel.texto(hoja, 1, 1, "Contratos");
        LibroExcel.texto(hoja, 2, 0, "Filtro de estado");
        LibroExcel.texto(hoja, 2, 1, filtroEstado == null ? "TODOS" : filtroEstado.getEtiqueta());
        LibroExcel.texto(hoja, 3, 0, "Solo por vencer");
        LibroExcel.texto(hoja, 3, 1, soloPorVencer ? "Sí" : "No");
        LibroExcel.texto(hoja, 4, 0, "Generado");
        LibroExcel.fecha(hoja, 4, 1, LocalDate.now(clock), fecha);
        LibroExcel.texto(hoja, 5, 0, "Contratos");
        LibroExcel.numero(hoja, 5, 1, lista.size());
        LibroExcel.texto(hoja, 6, 0, "Vigentes");
        LibroExcel.numero(hoja, 6, 1, vigentes);
        LibroExcel.texto(hoja, 7, 0, "Por vencer");
        LibroExcel.numero(hoja, 7, 1, porVencer);
        LibroExcel.texto(hoja, 8, 0, "Vencidos");
        LibroExcel.numero(hoja, 8, 1, vencidos);
        LibroExcel.texto(hoja, 9, 0, "Renta mensual vigente");
        LibroExcel.numero(hoja, 9, 1, renta, moneda);
        LibroExcel.cerrarHoja(hoja, 2);
    }

    private void hojaContratos(XSSFWorkbook libro, List<Contrato> lista, CellStyle moneda, CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Contratos", "Código", "Inmueble", "Arrendatario", "Documento",
                "Inicio", "Fin", "Día de pago", "Renta", "Incremento %", "Garantía", "Estado", "Estado visual");
        int fila = 1;
        for (Contrato contrato : lista) {
            LibroExcel.texto(hoja, fila, 0, contrato.getCodigo());
            LibroExcel.texto(hoja, fila, 1, contrato.getInmueble().getNombre());
            LibroExcel.texto(hoja, fila, 2, contrato.getArrendatario().getNombreCompleto());
            LibroExcel.texto(hoja, fila, 3, contrato.getArrendatario().getTipoDocumento() + " " + contrato.getArrendatario().getNumeroDocumento());
            LibroExcel.fecha(hoja, fila, 4, contrato.getFechaInicio(), fecha);
            LibroExcel.fecha(hoja, fila, 5, contrato.getFechaFin(), fecha);
            LibroExcel.numero(hoja, fila, 6, contrato.getDiaPago());
            LibroExcel.numero(hoja, fila, 7, contrato.getRentaMensual(), moneda);
            LibroExcel.numero(hoja, fila, 8, contrato.getIncrementoAnualPct());
            LibroExcel.numero(hoja, fila, 9, contrato.getGarantiaMonto(), moneda);
            LibroExcel.texto(hoja, fila, 10, contrato.getEstado() == null ? "" : contrato.getEstado().getEtiqueta());
            LibroExcel.texto(hoja, fila, 11, contratos.estadoVisual(contrato).getEtiqueta());
            fila++;
        }
        LibroExcel.cerrarHoja(hoja, 12);
    }

    private void hojaPorVencer(XSSFWorkbook libro, List<Contrato> lista, CellStyle moneda, CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Por vencer", "Código", "Inmueble", "Arrendatario",
                "Fin", "Días restantes", "Renta", "Teléfono");
        int fila = 1;
        LocalDate hoy = LocalDate.now(clock);
        for (Contrato contrato : lista) {
            if (contratos.estadoVisual(contrato) != EstadoVisual.POR_VENCER) continue;
            LibroExcel.texto(hoja, fila, 0, contrato.getCodigo());
            LibroExcel.texto(hoja, fila, 1, contrato.getInmueble().getNombre());
            LibroExcel.texto(hoja, fila, 2, contrato.getArrendatario().getNombreCompleto());
            LibroExcel.fecha(hoja, fila, 3, contrato.getFechaFin(), fecha);
            LibroExcel.numero(hoja, fila, 4, Math.max(0, ChronoUnit.DAYS.between(hoy, contrato.getFechaFin())));
            LibroExcel.numero(hoja, fila, 5, contrato.getRentaMensual(), moneda);
            LibroExcel.texto(hoja, fila, 6, contrato.getArrendatario().getTelefono());
            fila++;
        }
        LibroExcel.cerrarHoja(hoja, 7);
    }

    private void cerrar(XSSFWorkbook libro) {
        try {
            libro.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cerrar el Excel.", e);
        }
    }
}

package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.AuditoriaEvento;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class JlccReporteAuditoriaExcelService {
    private static final DateTimeFormatter JLCC_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] jlccGenerar(List<AuditoriaEvento> jlccEventos) {
        XSSFWorkbook jlccLibro = new XSSFWorkbook();
        try {
            Sheet jlccHoja =
                    JlccLibroExcel.jlccHoja(
                            jlccLibro,
                            "Auditoría",
                            "Fecha",
                            "Usuario",
                            "Acción",
                            "Entidad",
                            "ID",
                            "Detalle");
            int jlccFila = 1;
            for (AuditoriaEvento jlccEvento : jlccEventos == null ? List.<AuditoriaEvento>of() : jlccEventos) {
                JlccLibroExcel.jlccTexto(
                        jlccHoja,
                        jlccFila,
                        0,
                        jlccEvento.getFecha() == null ? "" : jlccEvento.getFecha().format(JLCC_FECHA));
                JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 1, jlccEvento.getUsuario());
                JlccLibroExcel.jlccTexto(
                        jlccHoja,
                        jlccFila,
                        2,
                        jlccEvento.getAccion() == null ? "" : jlccEvento.getAccion().getEtiqueta());
                JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 3, jlccEvento.getEntidad());
                JlccLibroExcel.jlccTexto(
                        jlccHoja,
                        jlccFila,
                        4,
                        jlccEvento.getEntidadId() == null ? "" : String.valueOf(jlccEvento.getEntidadId()));
                JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 5, jlccEvento.getDetalle());
                jlccFila++;
            }
            JlccLibroExcel.jlccCerrarHoja(jlccHoja, 6);
            return JlccLibroExcel.jlccBytes(jlccLibro);
        } finally {
            try {
                jlccLibro.close();
            } catch (java.io.IOException e) {
                throw new IllegalStateException("No se pudo cerrar el Excel.", e);
            }
        }
    }
}

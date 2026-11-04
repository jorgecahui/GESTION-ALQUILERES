package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.AuditoriaEvento;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** Exporta la bitácora filtrada a Excel. */
@Service
public class ReporteAuditoriaExcelService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] generar(List<AuditoriaEvento> eventos) {
        XSSFWorkbook libro = new XSSFWorkbook();
        try {
            Sheet hoja = LibroExcel.hoja(libro, "Auditoría", "Fecha", "Usuario", "Acción", "Entidad", "ID", "Detalle");
            int fila = 1;
            for (AuditoriaEvento evento : eventos == null ? List.<AuditoriaEvento>of() : eventos) {
                LibroExcel.texto(hoja, fila, 0, evento.getFecha() == null ? "" : evento.getFecha().format(FECHA));
                LibroExcel.texto(hoja, fila, 1, evento.getUsuario());
                LibroExcel.texto(hoja, fila, 2, evento.getAccion() == null ? "" : evento.getAccion().getEtiqueta());
                LibroExcel.texto(hoja, fila, 3, evento.getEntidad());
                LibroExcel.texto(hoja, fila, 4, evento.getEntidadId() == null ? "" : String.valueOf(evento.getEntidadId()));
                LibroExcel.texto(hoja, fila, 5, evento.getDetalle());
                fila++;
            }
            LibroExcel.cerrarHoja(hoja, 6);
            return LibroExcel.bytes(libro);
        } finally {
            try { libro.close(); } catch (java.io.IOException e) { throw new IllegalStateException("No se pudo cerrar el Excel.", e); }
        }
    }
}

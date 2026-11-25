package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JlccReporteInmueblesExcelService {
    private final Clock jlccClock;

    public byte[] jlccGenerar(List<Inmueble> jlccInmuebles) {
        XSSFWorkbook jlccLibro = new XSSFWorkbook();
        try {
            CellStyle jlccMoneda = JlccLibroExcel.jlccMoneda(jlccLibro);
            CellStyle jlccFecha = JlccLibroExcel.jlccFecha(jlccLibro);
            jlccResumen(jlccLibro, jlccInmuebles, jlccMoneda, jlccFecha);
            jlccHojaInmuebles(jlccLibro, jlccInmuebles, jlccMoneda);
            jlccHojaOcupacion(jlccLibro, jlccInmuebles, jlccMoneda);
            return JlccLibroExcel.jlccBytes(jlccLibro);
        } finally {
            jlccCerrar(jlccLibro);
        }
    }

    private void jlccResumen(
            XSSFWorkbook jlccLibro, List<Inmueble> jlccInmuebles, CellStyle jlccMoneda, CellStyle jlccFecha) {
        Sheet jlccHoja = JlccLibroExcel.jlccHoja(jlccLibro, "Resumen", "Concepto", "Valor");
        long jlccAlquilados = jlccContar(jlccInmuebles, EstadoInmueble.ALQUILADO);
        long jlccDisponibles = jlccContar(jlccInmuebles, EstadoInmueble.DISPONIBLE);
        long jlccMantenimiento = jlccContar(jlccInmuebles, EstadoInmueble.MANTENIMIENTO);
        long jlccInactivos = jlccContar(jlccInmuebles, EstadoInmueble.INACTIVO);
        BigDecimal jlccRenta =
                jlccInmuebles.stream()
                        .map(Inmueble::getRentaReferencial)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 0, "Módulo");
        JlccLibroExcel.jlccTexto(jlccHoja, 1, 1, "Inmuebles y ocupación");
        JlccLibroExcel.jlccTexto(jlccHoja, 2, 0, "Generado");
        JlccLibroExcel.jlccFecha(jlccHoja, 2, 1, LocalDate.now(jlccClock), jlccFecha);
        JlccLibroExcel.jlccTexto(jlccHoja, 3, 0, "Inmuebles");
        JlccLibroExcel.jlccNumero(jlccHoja, 3, 1, jlccInmuebles.size());
        JlccLibroExcel.jlccTexto(jlccHoja, 4, 0, "Alquilados");
        JlccLibroExcel.jlccNumero(jlccHoja, 4, 1, jlccAlquilados);
        JlccLibroExcel.jlccTexto(jlccHoja, 5, 0, "Disponibles");
        JlccLibroExcel.jlccNumero(jlccHoja, 5, 1, jlccDisponibles);
        JlccLibroExcel.jlccTexto(jlccHoja, 6, 0, "En mantenimiento");
        JlccLibroExcel.jlccNumero(jlccHoja, 6, 1, jlccMantenimiento);
        JlccLibroExcel.jlccTexto(jlccHoja, 7, 0, "Inactivos");
        JlccLibroExcel.jlccNumero(jlccHoja, 7, 1, jlccInactivos);
        JlccLibroExcel.jlccTexto(jlccHoja, 8, 0, "Ocupación");
        JlccLibroExcel.jlccNumero(
                jlccHoja, 8, 1, jlccPorcentaje(jlccAlquilados, jlccInmuebles.size()), JlccLibroExcel.jlccPorcentaje(jlccLibro));
        JlccLibroExcel.jlccTexto(jlccHoja, 9, 0, "Renta referencial total");
        JlccLibroExcel.jlccNumero(jlccHoja, 9, 1, jlccRenta, jlccMoneda);
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 2);
    }

    private void jlccHojaInmuebles(XSSFWorkbook jlccLibro, List<Inmueble> jlccInmuebles, CellStyle jlccMoneda) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Inmuebles",
                        "Código",
                        "Nombre",
                        "Tipo",
                        "Propietario",
                        "Dirección",
                        "Distrito",
                        "Área m²",
                        "Dormitorios",
                        "Baños",
                        "Estacionamientos",
                        "Amoblado",
                        "Renta referencial",
                        "Estado");
        int jlccFila = 1;
        for (Inmueble jlccInmueble : jlccInmuebles) {
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, jlccInmueble.getCodigo());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 1, jlccInmueble.getNombre());
            JlccLibroExcel.jlccTexto(
                    jlccHoja,
                    jlccFila,
                    2,
                    jlccInmueble.getTipo() == null ? "Sin tipo" : jlccInmueble.getTipo().getNombre());
            JlccLibroExcel.jlccTexto(
                    jlccHoja,
                    jlccFila,
                    3,
                    jlccInmueble.getPropietario() == null
                            ? "Sin propietario"
                            : jlccInmueble.getPropietario().getNombre());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 4, jlccInmueble.getDireccion());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 5, jlccInmueble.getDistrito());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 6, jlccInmueble.getAreaM2());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 7, jlccInmueble.getDormitorios());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 8, jlccInmueble.getBanos());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 9, jlccInmueble.getEstacionamientos());
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 10, jlccInmueble.isAmoblado() ? "Sí" : "No");
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 11, jlccInmueble.getRentaReferencial(), jlccMoneda);
            JlccLibroExcel.jlccTexto(
                    jlccHoja,
                    jlccFila,
                    12,
                    jlccInmueble.getEstado() == null ? "" : jlccInmueble.getEstado().getEtiqueta());
            jlccFila++;
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 13);
    }

    private void jlccHojaOcupacion(XSSFWorkbook jlccLibro, List<Inmueble> jlccInmuebles, CellStyle jlccMoneda) {
        Sheet jlccHoja =
                JlccLibroExcel.jlccHoja(
                        jlccLibro,
                        "Ocupación por tipo",
                        "Tipo",
                        "Total",
                        "Alquilados",
                        "Disponibles",
                        "Mantenimiento",
                        "Inactivos",
                        "Ocupación",
                        "Renta referencial");
        Map<String, List<Inmueble>> jlccGrupos = new LinkedHashMap<>();
        for (Inmueble jlccInmueble : jlccInmuebles) {
            String jlccTipo = jlccInmueble.getTipo() == null ? "Sin tipo" : jlccInmueble.getTipo().getNombre();
            jlccGrupos.computeIfAbsent(jlccTipo, clave -> new java.util.ArrayList<>()).add(jlccInmueble);
        }
        int jlccFila = 1;
        for (Map.Entry<String, List<Inmueble>> jlccGrupo : jlccGrupos.entrySet()) {
            List<Inmueble> jlccLista = jlccGrupo.getValue();
            long jlccAlquilados = jlccContar(jlccLista, EstadoInmueble.ALQUILADO);
            BigDecimal jlccRenta =
                    jlccLista.stream()
                            .map(Inmueble::getRentaReferencial)
                            .filter(java.util.Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            JlccLibroExcel.jlccTexto(jlccHoja, jlccFila, 0, jlccGrupo.getKey());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 1, jlccLista.size());
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 2, jlccAlquilados);
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 3, jlccContar(jlccLista, EstadoInmueble.DISPONIBLE));
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 4, jlccContar(jlccLista, EstadoInmueble.MANTENIMIENTO));
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 5, jlccContar(jlccLista, EstadoInmueble.INACTIVO));
            JlccLibroExcel.jlccNumero(
                    jlccHoja,
                    jlccFila,
                    6,
                    jlccPorcentaje(jlccAlquilados, jlccLista.size()),
                    JlccLibroExcel.jlccPorcentaje(jlccLibro));
            JlccLibroExcel.jlccNumero(jlccHoja, jlccFila, 7, jlccRenta, jlccMoneda);
            jlccFila++;
        }
        JlccLibroExcel.jlccCerrarHoja(jlccHoja, 8);
    }

    private long jlccContar(List<Inmueble> jlccInmuebles, EstadoInmueble jlccEstado) {
        return jlccInmuebles.stream().filter(i -> i.getEstado() == jlccEstado).count();
    }

    private BigDecimal jlccPorcentaje(long jlccParte, long jlccTotal) {
        return jlccTotal == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(jlccParte)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(jlccTotal), 2, RoundingMode.HALF_UP);
    }

    private void jlccCerrar(XSSFWorkbook jlccLibro) {
        try {
            jlccLibro.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cerrar el Excel.", e);
        }
    }
}

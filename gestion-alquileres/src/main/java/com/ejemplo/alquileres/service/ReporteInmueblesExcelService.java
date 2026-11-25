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
public class ReporteInmueblesExcelService {
    private final Clock clock;

    public byte[] generar(List<Inmueble> inmuebles) {
        XSSFWorkbook libro = new XSSFWorkbook();
        try {
            CellStyle moneda = LibroExcel.moneda(libro);
            CellStyle fecha = LibroExcel.fecha(libro);
            resumen(libro, inmuebles, moneda, fecha);
            hojaInmuebles(libro, inmuebles, moneda);
            hojaOcupacion(libro, inmuebles, moneda);
            return LibroExcel.bytes(libro);
        } finally {
            cerrar(libro);
        }
    }

    private void resumen(
            XSSFWorkbook libro, List<Inmueble> inmuebles, CellStyle moneda, CellStyle fecha) {
        Sheet hoja = LibroExcel.hoja(libro, "Resumen", "Concepto", "Valor");
        long alquilados = contar(inmuebles, EstadoInmueble.ALQUILADO);
        long disponibles = contar(inmuebles, EstadoInmueble.DISPONIBLE);
        long mantenimiento = contar(inmuebles, EstadoInmueble.MANTENIMIENTO);
        long inactivos = contar(inmuebles, EstadoInmueble.INACTIVO);
        BigDecimal renta =
                inmuebles.stream()
                        .map(Inmueble::getRentaReferencial)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        LibroExcel.texto(hoja, 1, 0, "Módulo");
        LibroExcel.texto(hoja, 1, 1, "Inmuebles y ocupación");
        LibroExcel.texto(hoja, 2, 0, "Generado");
        LibroExcel.fecha(hoja, 2, 1, LocalDate.now(clock), fecha);
        LibroExcel.texto(hoja, 3, 0, "Inmuebles");
        LibroExcel.numero(hoja, 3, 1, inmuebles.size());
        LibroExcel.texto(hoja, 4, 0, "Alquilados");
        LibroExcel.numero(hoja, 4, 1, alquilados);
        LibroExcel.texto(hoja, 5, 0, "Disponibles");
        LibroExcel.numero(hoja, 5, 1, disponibles);
        LibroExcel.texto(hoja, 6, 0, "En mantenimiento");
        LibroExcel.numero(hoja, 6, 1, mantenimiento);
        LibroExcel.texto(hoja, 7, 0, "Inactivos");
        LibroExcel.numero(hoja, 7, 1, inactivos);
        LibroExcel.texto(hoja, 8, 0, "Ocupación");
        LibroExcel.numero(
                hoja, 8, 1, porcentaje(alquilados, inmuebles.size()), LibroExcel.porcentaje(libro));
        LibroExcel.texto(hoja, 9, 0, "Renta referencial total");
        LibroExcel.numero(hoja, 9, 1, renta, moneda);
        LibroExcel.cerrarHoja(hoja, 2);
    }

    private void hojaInmuebles(XSSFWorkbook libro, List<Inmueble> inmuebles, CellStyle moneda) {
        Sheet hoja =
                LibroExcel.hoja(
                        libro,
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
        int fila = 1;
        for (Inmueble inmueble : inmuebles) {
            LibroExcel.texto(hoja, fila, 0, inmueble.getCodigo());
            LibroExcel.texto(hoja, fila, 1, inmueble.getNombre());
            LibroExcel.texto(
                    hoja,
                    fila,
                    2,
                    inmueble.getTipo() == null ? "Sin tipo" : inmueble.getTipo().getNombre());
            LibroExcel.texto(
                    hoja,
                    fila,
                    3,
                    inmueble.getPropietario() == null
                            ? "Sin propietario"
                            : inmueble.getPropietario().getNombre());
            LibroExcel.texto(hoja, fila, 4, inmueble.getDireccion());
            LibroExcel.texto(hoja, fila, 5, inmueble.getDistrito());
            LibroExcel.numero(hoja, fila, 6, inmueble.getAreaM2());
            LibroExcel.numero(hoja, fila, 7, inmueble.getDormitorios());
            LibroExcel.numero(hoja, fila, 8, inmueble.getBanos());
            LibroExcel.numero(hoja, fila, 9, inmueble.getEstacionamientos());
            LibroExcel.texto(hoja, fila, 10, inmueble.isAmoblado() ? "Sí" : "No");
            LibroExcel.numero(hoja, fila, 11, inmueble.getRentaReferencial(), moneda);
            LibroExcel.texto(
                    hoja,
                    fila,
                    12,
                    inmueble.getEstado() == null ? "" : inmueble.getEstado().getEtiqueta());
            fila++;
        }
        LibroExcel.cerrarHoja(hoja, 13);
    }

    private void hojaOcupacion(XSSFWorkbook libro, List<Inmueble> inmuebles, CellStyle moneda) {
        Sheet hoja =
                LibroExcel.hoja(
                        libro,
                        "Ocupación por tipo",
                        "Tipo",
                        "Total",
                        "Alquilados",
                        "Disponibles",
                        "Mantenimiento",
                        "Inactivos",
                        "Ocupación",
                        "Renta referencial");
        Map<String, List<Inmueble>> grupos = new LinkedHashMap<>();
        for (Inmueble inmueble : inmuebles) {
            String tipo = inmueble.getTipo() == null ? "Sin tipo" : inmueble.getTipo().getNombre();
            grupos.computeIfAbsent(tipo, clave -> new java.util.ArrayList<>()).add(inmueble);
        }
        int fila = 1;
        for (Map.Entry<String, List<Inmueble>> grupo : grupos.entrySet()) {
            List<Inmueble> lista = grupo.getValue();
            long alquilados = contar(lista, EstadoInmueble.ALQUILADO);
            BigDecimal renta =
                    lista.stream()
                            .map(Inmueble::getRentaReferencial)
                            .filter(java.util.Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            LibroExcel.texto(hoja, fila, 0, grupo.getKey());
            LibroExcel.numero(hoja, fila, 1, lista.size());
            LibroExcel.numero(hoja, fila, 2, alquilados);
            LibroExcel.numero(hoja, fila, 3, contar(lista, EstadoInmueble.DISPONIBLE));
            LibroExcel.numero(hoja, fila, 4, contar(lista, EstadoInmueble.MANTENIMIENTO));
            LibroExcel.numero(hoja, fila, 5, contar(lista, EstadoInmueble.INACTIVO));
            LibroExcel.numero(
                    hoja,
                    fila,
                    6,
                    porcentaje(alquilados, lista.size()),
                    LibroExcel.porcentaje(libro));
            LibroExcel.numero(hoja, fila, 7, renta, moneda);
            fila++;
        }
        LibroExcel.cerrarHoja(hoja, 8);
    }

    private long contar(List<Inmueble> inmuebles, EstadoInmueble estado) {
        return inmuebles.stream().filter(i -> i.getEstado() == estado).count();
    }

    private BigDecimal porcentaje(long parte, long total) {
        return total == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(parte)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private void cerrar(XSSFWorkbook libro) {
        try {
            libro.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cerrar el Excel.", e);
        }
    }
}

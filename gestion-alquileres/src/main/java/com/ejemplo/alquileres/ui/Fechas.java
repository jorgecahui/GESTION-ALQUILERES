package com.ejemplo.alquileres.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Fechas {
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Fechas() {}

    public static String dia(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(DIA);
    }

    public static String diaHora(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(DIA_HORA);
    }
}

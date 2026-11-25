package com.ejemplo.alquileres.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class JlccFechas {
    private static final DateTimeFormatter JLCC_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter JLCC_DIA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private JlccFechas() {}

    public static String jlccDia(LocalDate jlccFecha) {
        return jlccFecha == null ? "" : jlccFecha.format(JLCC_DIA);
    }

    public static String jlccDiaHora(LocalDateTime jlccFecha) {
        return jlccFecha == null ? "" : jlccFecha.format(JLCC_DIA_HORA);
    }
}

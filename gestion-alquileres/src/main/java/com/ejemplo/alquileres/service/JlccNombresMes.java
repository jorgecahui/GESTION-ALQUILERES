package com.ejemplo.alquileres.service;

import java.time.*;
import java.time.format.*;
import java.util.*;

public final class JlccNombresMes {
    private JlccNombresMes() {}

    public static String jlccDe(LocalDate jlccFecha) {
        return jlccFecha.format(DateTimeFormatter.ofPattern("MMMM", new Locale("es", "PE")));
    }
}

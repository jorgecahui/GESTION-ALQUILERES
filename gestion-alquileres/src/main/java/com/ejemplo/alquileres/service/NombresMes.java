package com.ejemplo.alquileres.service;

import java.time.*;
import java.time.format.*;
import java.util.*;

public final class NombresMes {
    private NombresMes() {}

    public static String de(LocalDate fecha) {
        return fecha.format(DateTimeFormatter.ofPattern("MMMM", new Locale("es", "PE")));
    }
}

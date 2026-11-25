package com.ejemplo.alquileres.model;

import java.time.*;
import java.time.temporal.ChronoUnit;

public enum Periodo {
    ULTIMOS_7_DIAS("Últimos 7 días"),
    ULTIMOS_30_DIAS("Últimos 30 días"),
    ESTE_MES("Este mes"),
    MES_ANTERIOR("Mes anterior"),
    ULTIMOS_90_DIAS("Últimos 90 días"),
    ESTE_ANIO("Este año"),
    PERSONALIZADO("Personalizado");
    private final String etiqueta;

    Periodo(String e) {
        etiqueta = e;
    }

    public Rango resolver(Clock c, LocalDate d, LocalDate h) {
        LocalDate hoy = LocalDate.now(c);
        return switch (this) {
            case ULTIMOS_7_DIAS -> new Rango(hoy.minusDays(6), hoy);
            case ULTIMOS_30_DIAS -> new Rango(hoy.minusDays(29), hoy);
            case ESTE_MES -> new Rango(hoy.withDayOfMonth(1), hoy);
            case MES_ANTERIOR -> {
                LocalDate f = hoy.withDayOfMonth(1).minusDays(1);
                yield new Rango(f.withDayOfMonth(1), f);
            }
            case ULTIMOS_90_DIAS -> new Rango(hoy.minusDays(89), hoy);
            case ESTE_ANIO -> new Rango(hoy.withDayOfYear(1), hoy);
            case PERSONALIZADO -> new Rango(d == null ? hoy.minusDays(29) : d, h == null ? hoy : h);
        };
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    public record Rango(LocalDate desde, LocalDate hasta) {
        public Rango {
            if (desde == null || hasta == null || hasta.isBefore(desde))
                throw new IllegalArgumentException("El rango de fechas no es válido.");
        }

        public long dias() {
            return ChronoUnit.DAYS.between(desde, hasta) + 1;
        }

        public Rango anterior() {
            return new Rango(desde.minusDays(dias()), desde.minusDays(1));
        }
    }
}

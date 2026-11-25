package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.*;

import org.springframework.stereotype.Component;

import java.math.*;
import java.time.*;
import java.util.*;

@Component
public class JlccCronogramaGenerador {
    public List<Cuota> jlccGenerar(
            LocalDate jlccInicio, int jlccMeses, int jlccDia, BigDecimal jlccRenta, BigDecimal jlccInc) {
        List<Cuota> r = new ArrayList<>();
        BigDecimal jlccMonto = jlccRenta.setScale(2, RoundingMode.HALF_UP);
        for (int n = 1; n <= jlccMeses; n++) {
            if (n > 1 && (n - 1) % 12 == 0)
                jlccMonto =
                        jlccMonto.multiply(BigDecimal.ONE.add(jlccInc.movePointLeft(2)))
                                .setScale(2, RoundingMode.HALF_UP);
            LocalDate jlccPeriodo = YearMonth.from(jlccInicio).plusMonths(n - 1).atDay(1);
            LocalDate jlccVence = jlccPeriodo.withDayOfMonth(Math.min(jlccDia, jlccPeriodo.lengthOfMonth()));
            if (n == 1 && jlccVence.isBefore(jlccInicio)) jlccVence = jlccInicio;
            r.add(
                    Cuota.builder()
                            .numero(n)
                            .tipo(TipoCuota.RENTA)
                            .periodo(jlccPeriodo)
                            .fechaVencimiento(jlccVence)
                            .concepto("Renta " + JlccNombresMes.jlccDe(jlccPeriodo) + " " + jlccPeriodo.getYear())
                            .montoCuota(jlccMonto)
                            .moraDesde(jlccVence)
                            .estado(EstadoCuota.PENDIENTE)
                            .build());
        }
        return r;
    }
}

package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.TipoMora;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class JlccCalculadoraMora {
    public BigDecimal jlccTotalDevengado(Cuota jlccCuota, Contrato jlccContrato, LocalDate jlccFechaPago) {
        BigDecimal jlccSaldo = jlccSaldoCapital(jlccCuota);
        if (jlccSaldo.signum() <= 0 || !jlccFechaPago.isAfter(jlccCuota.getFechaVencimiento())) {
            return jlccDinero(jlccCuota.getMoraAcumulada());
        }
        long jlccAtraso = ChronoUnit.DAYS.between(jlccCuota.getFechaVencimiento(), jlccFechaPago);
        if (jlccCuota.getMontoPagado().signum() == 0 && jlccAtraso <= jlccContrato.getDiasGracia()) {
            return jlccDinero(jlccCuota.getMoraAcumulada());
        }
        LocalDate jlccDesde =
                jlccCuota.getMoraDesde() == null ? jlccCuota.getFechaVencimiento() : jlccCuota.getMoraDesde();
        long jlccDias = Math.max(0, ChronoUnit.DAYS.between(jlccDesde, jlccFechaPago));
        BigDecimal jlccDiaria =
                jlccContrato.getTipoMora() == TipoMora.PORCENTAJE_DIARIO
                        ? jlccSaldo.multiply(jlccContrato.getValorMora().movePointLeft(2))
                        : jlccContrato.getValorMora();
        return jlccDinero(jlccCuota.getMoraAcumulada().add(jlccDiaria.multiply(BigDecimal.valueOf(jlccDias))));
    }

    public BigDecimal jlccPendiente(Cuota jlccCuota, Contrato jlccContrato, LocalDate jlccFechaPago) {
        return jlccDinero(
                jlccTotalDevengado(jlccCuota, jlccContrato, jlccFechaPago)
                        .subtract(jlccCuota.getMoraCobrada())
                        .subtract(jlccCuota.getMoraCondonada())
                        .max(BigDecimal.ZERO));
    }

    public BigDecimal jlccSaldoCapital(Cuota jlccCuota) {
        return jlccDinero(jlccCuota.getMontoCuota().subtract(jlccCuota.getMontoPagado()).max(BigDecimal.ZERO));
    }

    private BigDecimal jlccDinero(BigDecimal jlccValor) {
        return (jlccValor == null ? BigDecimal.ZERO : jlccValor).setScale(2, RoundingMode.HALF_UP);
    }
}

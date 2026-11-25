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
public class CalculadoraMora {
    public BigDecimal totalDevengado(Cuota cuota, Contrato contrato, LocalDate fechaPago) {
        BigDecimal saldo = saldoCapital(cuota);
        if (saldo.signum() <= 0 || !fechaPago.isAfter(cuota.getFechaVencimiento())) {
            return dinero(cuota.getMoraAcumulada());
        }
        long atraso = ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), fechaPago);
        if (cuota.getMontoPagado().signum() == 0 && atraso <= contrato.getDiasGracia()) {
            return dinero(cuota.getMoraAcumulada());
        }
        LocalDate desde =
                cuota.getMoraDesde() == null ? cuota.getFechaVencimiento() : cuota.getMoraDesde();
        long dias = Math.max(0, ChronoUnit.DAYS.between(desde, fechaPago));
        BigDecimal diaria =
                contrato.getTipoMora() == TipoMora.PORCENTAJE_DIARIO
                        ? saldo.multiply(contrato.getValorMora().movePointLeft(2))
                        : contrato.getValorMora();
        return dinero(cuota.getMoraAcumulada().add(diaria.multiply(BigDecimal.valueOf(dias))));
    }

    public BigDecimal pendiente(Cuota cuota, Contrato contrato, LocalDate fechaPago) {
        return dinero(
                totalDevengado(cuota, contrato, fechaPago)
                        .subtract(cuota.getMoraCobrada())
                        .subtract(cuota.getMoraCondonada())
                        .max(BigDecimal.ZERO));
    }

    public BigDecimal saldoCapital(Cuota cuota) {
        return dinero(cuota.getMontoCuota().subtract(cuota.getMontoPagado()).max(BigDecimal.ZERO));
    }

    private BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}

package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoCuota;
import com.ejemplo.alquileres.model.SemaforoMorosidad;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MorosidadService {
    private final ConfiguracionService configuracion;
    private final Clock clock;

    public List<Cuota> vencidas(Contrato contrato) {
        if (contrato == null || contrato.getCuotas() == null) return List.of();
        LocalDate hoy = LocalDate.now(clock);
        return contrato.getCuotas().stream()
                .filter(
                        c ->
                                (c.getEstado() == EstadoCuota.PENDIENTE
                                                || c.getEstado() == EstadoCuota.PARCIAL)
                                        && c.getFechaVencimiento()
                                                .plusDays(contrato.getDiasGracia())
                                                .isBefore(hoy))
                .toList();
    }

    public SemaforoMorosidad semaforo(Contrato contrato) {
        LocalDate hoy = LocalDate.now(clock);
        List<Cuota> vencidas = vencidas(contrato);
        if (vencidas.isEmpty()) return SemaforoMorosidad.AL_DIA;
        long max =
                vencidas.stream()
                        .mapToLong(c -> ChronoUnit.DAYS.between(c.getFechaVencimiento(), hoy))
                        .max()
                        .orElse(0);
        if (max > configuracion.umbralLeveDias() || vencidas.size() >= configuracion.cuotasRojo()) {
            return SemaforoMorosidad.MOROSO;
        }
        return SemaforoMorosidad.ATRASO_LEVE;
    }
}

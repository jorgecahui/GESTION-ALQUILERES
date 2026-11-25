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
public class JlccMorosidadService {
    private final JlccConfiguracionService jlccConfiguracion;
    private final Clock jlccClock;

    public List<Cuota> jlccVencidas(Contrato jlccContrato) {
        if (jlccContrato == null || jlccContrato.getCuotas() == null) return List.of();
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        return jlccContrato.getCuotas().stream()
                .filter(
                        c ->
                                (c.getEstado() == EstadoCuota.PENDIENTE
                                                || c.getEstado() == EstadoCuota.PARCIAL)
                                        && c.getFechaVencimiento()
                                                .plusDays(jlccContrato.getDiasGracia())
                                                .isBefore(jlccHoy))
                .toList();
    }

    public SemaforoMorosidad jlccSemaforo(Contrato jlccContrato) {
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        List<Cuota> jlccVencidas = jlccVencidas(jlccContrato);
        if (jlccVencidas.isEmpty()) return SemaforoMorosidad.AL_DIA;
        long jlccMax =
                jlccVencidas.stream()
                        .mapToLong(c -> ChronoUnit.DAYS.between(c.getFechaVencimiento(), jlccHoy))
                        .max()
                        .orElse(0);
        if (jlccMax > jlccConfiguracion.jlccUmbralLeveDias() || jlccVencidas.size() >= jlccConfiguracion.jlccCuotasRojo()) {
            return SemaforoMorosidad.MOROSO;
        }
        return SemaforoMorosidad.ATRASO_LEVE;
    }
}

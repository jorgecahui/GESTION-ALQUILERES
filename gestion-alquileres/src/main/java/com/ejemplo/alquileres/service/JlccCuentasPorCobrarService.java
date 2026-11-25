package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.CuotaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JlccCuentasPorCobrarService {
    private final CuotaRepository jlccCuotas;
    private final JlccCalculadoraMora jlccMora;
    private final JlccMorosidadService jlccMorosidad;
    private final Clock jlccClock;

    public enum JlccFiltro {
        TODAS("Todas"),
        VENCIDAS("Vencidas"),
        VENCEN_EN_7_DIAS("Vencen en 7 días"),
        MOROSOS("Morosos");

        private final String jlccEtiqueta;

        JlccFiltro(String jlccEtiqueta) {
            this.jlccEtiqueta = jlccEtiqueta;
        }

        public String jlccGetEtiqueta() {
            return jlccEtiqueta;
        }

        @Override
        public String toString() {
            return jlccEtiqueta;
        }
    }

    public record Cuenta(
            Long contratoId,
            Long cuotaId,
            String arrendatario,
            String inmueble,
            String concepto,
            LocalDate vencimiento,
            long diasAtraso,
            boolean vencida,
            BigDecimal saldo,
            BigDecimal moraEstimada,
            BigDecimal total,
            SemaforoMorosidad semaforo) {}

    @Transactional(readOnly = true)
    public List<Cuenta> jlccListar(JlccFiltro jlccFiltro, String jlccTexto) {
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        String jlccBuscador = jlccTexto == null ? "" : jlccTexto.trim().toLowerCase();
        return jlccCuotas.pendientesConContrato().stream()
                .map(c -> jlccConvertir(c, jlccHoy))
                .filter(c -> jlccCoincide(c, jlccFiltro == null ? JlccFiltro.TODAS : jlccFiltro, jlccHoy))
                .filter(
                        c ->
                                jlccBuscador.isBlank()
                                        || (c.arrendatario()
                                                        + " "
                                                        + c.inmueble()
                                                        + " "
                                                        + c.concepto())
                                                .toLowerCase()
                                                .contains(jlccBuscador))
                .toList();
    }

    private Cuenta jlccConvertir(Cuota jlccCuota, LocalDate jlccHoy) {
        Contrato jlccContrato = jlccCuota.getContrato();
        BigDecimal jlccSaldo = jlccMora.jlccSaldoCapital(jlccCuota),
                jlccEstimada = jlccMora.jlccPendiente(jlccCuota, jlccContrato, jlccHoy);
        long jlccDias = Math.max(0, ChronoUnit.DAYS.between(jlccCuota.getFechaVencimiento(), jlccHoy));
        boolean jlccVencida =
                jlccCuota.getFechaVencimiento().plusDays(jlccContrato.getDiasGracia()).isBefore(jlccHoy);
        return new Cuenta(
                jlccContrato.getId(),
                jlccCuota.getId(),
                jlccContrato.getArrendatario().getNombreCompleto(),
                jlccContrato.getInmueble().getNombre(),
                jlccCuota.getConcepto(),
                jlccCuota.getFechaVencimiento(),
                jlccDias,
                jlccVencida,
                jlccSaldo,
                jlccEstimada,
                jlccSaldo.add(jlccEstimada).setScale(2, RoundingMode.HALF_UP),
                jlccMorosidad.jlccSemaforo(jlccContrato));
    }

    private boolean jlccCoincide(Cuenta jlccCuenta, JlccFiltro jlccFiltro, LocalDate jlccHoy) {
        return switch (jlccFiltro) {
            case TODAS -> true;
            case VENCIDAS -> jlccCuenta.vencida();
            case VENCEN_EN_7_DIAS ->
                    !jlccCuenta.vencimiento().isBefore(jlccHoy)
                            && !jlccCuenta.vencimiento().isAfter(jlccHoy.plusDays(7));
            case MOROSOS -> jlccCuenta.semaforo() == SemaforoMorosidad.MOROSO;
        };
    }
}

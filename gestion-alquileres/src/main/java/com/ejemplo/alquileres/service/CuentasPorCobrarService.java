package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.CuotaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class CuentasPorCobrarService {
    private final CuotaRepository cuotas; private final CalculadoraMora mora; private final MorosidadService morosidad; private final Clock clock;
    public enum Filtro {
        TODAS("Todas"),
        VENCIDAS("Vencidas"),
        VENCEN_EN_7_DIAS("Vencen en 7 días"),
        MOROSOS("Morosos");

        private final String etiqueta;

        Filtro(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
        @Override public String toString() { return etiqueta; }
    }
    public record Cuenta(Long contratoId, Long cuotaId, String arrendatario, String inmueble, String concepto,
                         LocalDate vencimiento, long diasAtraso, boolean vencida, BigDecimal saldo,
                         BigDecimal moraEstimada, BigDecimal total, SemaforoMorosidad semaforo) { }
    @Transactional(readOnly = true)
    public List<Cuenta> listar(Filtro filtro, String texto) {
        LocalDate hoy = LocalDate.now(clock); String buscador = texto == null ? "" : texto.trim().toLowerCase();
        return cuotas.pendientesConContrato().stream().map(c -> convertir(c, hoy)).filter(c -> coincide(c, filtro == null ? Filtro.TODAS : filtro, hoy)).filter(c -> buscador.isBlank() || (c.arrendatario() + " " + c.inmueble() + " " + c.concepto()).toLowerCase().contains(buscador)).toList();
    }
    private Cuenta convertir(Cuota cuota, LocalDate hoy) {
        Contrato contrato = cuota.getContrato(); BigDecimal saldo = mora.saldoCapital(cuota), estimada = mora.pendiente(cuota, contrato, hoy);
        long dias = Math.max(0, ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy));
        boolean vencida = cuota.getFechaVencimiento().plusDays(contrato.getDiasGracia()).isBefore(hoy);
        return new Cuenta(contrato.getId(), cuota.getId(), contrato.getArrendatario().getNombreCompleto(), contrato.getInmueble().getNombre(), cuota.getConcepto(), cuota.getFechaVencimiento(), dias, vencida, saldo, estimada, saldo.add(estimada).setScale(2, RoundingMode.HALF_UP), morosidad.semaforo(contrato));
    }
    private boolean coincide(Cuenta cuenta, Filtro filtro, LocalDate hoy) {
        return switch (filtro) {
            case TODAS -> true;
            case VENCIDAS -> cuenta.vencida();
            case VENCEN_EN_7_DIAS -> !cuenta.vencimiento().isBefore(hoy) && !cuenta.vencimiento().isAfter(hoy.plusDays(7));
            case MOROSOS -> cuenta.semaforo() == SemaforoMorosidad.MOROSO;
        };
    }
}

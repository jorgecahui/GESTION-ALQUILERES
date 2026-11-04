package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ejemplo.alquileres.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CalculadoraMoraTest {
    private final CalculadoraMora calculadora = new CalculadoraMora();
    @Test void respetaGraciaYCalculaPorcentajeDesdeElVencimiento() {
        Cuota cuota = cuota("1000.00"); Contrato contrato = contrato(TipoMora.PORCENTAJE_DIARIO, "0.10");
        assertEquals(new BigDecimal("0.00"), calculadora.pendiente(cuota, contrato, LocalDate.of(2026, 1, 8)));
        assertEquals(new BigDecimal("7.00"), calculadora.pendiente(cuota, contrato, LocalDate.of(2026, 1, 12)));
    }
    @Test void calculaMontoDiarioYNoCobraAntesDelVencimiento() {
        Cuota cuota = cuota("1000.00");
        assertEquals(new BigDecimal("35.00"), calculadora.pendiente(cuota, contrato(TipoMora.MONTO_DIARIO, "5.00"), LocalDate.of(2026, 1, 12)));
        assertEquals(new BigDecimal("0.00"), calculadora.pendiente(cuota, contrato(TipoMora.MONTO_DIARIO, "5.00"), LocalDate.of(2026, 1, 4)));
    }
    @Test void puntoDeControlTrasParcialCalculaSobreSaldo() {
        Cuota cuota = cuota("1000.00"); cuota.setMontoPagado(new BigDecimal("400.00")); cuota.setMoraAcumulada(new BigDecimal("7.00")); cuota.setMoraCobrada(new BigDecimal("7.00")); cuota.setMoraDesde(LocalDate.of(2026, 1, 12));
        assertEquals(new BigDecimal("6.00"), calculadora.pendiente(cuota, contrato(TipoMora.PORCENTAJE_DIARIO, "0.10"), LocalDate.of(2026, 1, 22)));
    }
    @Test void saldoCeroNoDevengaMoraNuevaCuandoLaMoraAcumuladaYaFueCobrada() {
        Cuota cuota = cuota("1000.00"); cuota.setMontoPagado(new BigDecimal("1000.00")); cuota.setMoraAcumulada(new BigDecimal("1.005")); cuota.setMoraCobrada(new BigDecimal("1.01"));
        assertEquals(new BigDecimal("0.00"), calculadora.pendiente(cuota, contrato(TipoMora.MONTO_DIARIO, "1"), LocalDate.of(2026, 2, 1)));
    }
    private Cuota cuota(String monto) { return Cuota.builder().montoCuota(new BigDecimal(monto)).montoPagado(BigDecimal.ZERO).moraAcumulada(BigDecimal.ZERO).moraCobrada(BigDecimal.ZERO).moraCondonada(BigDecimal.ZERO).fechaVencimiento(LocalDate.of(2026, 1, 5)).estado(EstadoCuota.PENDIENTE).build(); }
    private Contrato contrato(TipoMora tipo, String valor) { return Contrato.builder().diasGracia(3).tipoMora(tipo).valorMora(new BigDecimal(valor)).build(); }
}

package com.ejemplo.alquileres.model;

import jakarta.persistence.*;

import lombok.*;

import java.math.*;
import java.time.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pago_id")
    Pago pago;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cuota_id")
    Cuota cuota;

    int item;
    String concepto;
    LocalDate periodo, fechaVencimiento;
    @Column(precision = 10, scale = 2)
    BigDecimal montoCapital, montoMora, montoMoraCondonada, moraAcumuladaAntes;
    String motivoCondonacion;
    LocalDate moraDesdeAntes;

    @Enumerated(EnumType.STRING)
    EstadoCuota estadoCuotaAntes;
}

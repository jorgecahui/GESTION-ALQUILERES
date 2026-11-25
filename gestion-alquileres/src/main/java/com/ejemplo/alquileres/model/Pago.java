package com.ejemplo.alquileres.model;

import jakarta.persistence.*;

import lombok.*;

import java.math.*;
import java.time.*;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_pago_recibo", columnNames = "recibo"),
        indexes = {
            @Index(name = "idx_pago_fecha_pago", columnList = "fechaPago"),
            @Index(name = "idx_pago_contrato", columnList = "contrato_id")
        })
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String serie;
    int numero;

    @Column(nullable = false, unique = true)
    String recibo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contrato_id")
    Contrato contrato;

    LocalDate fechaPago;
    LocalDateTime fechaRegistro;

    @Enumerated(EnumType.STRING)
    MedioPago medioPago;

    String referencia;
    @Column(precision = 10, scale = 2)
    BigDecimal montoTotal, montoMora, montoCapital;

    @Enumerated(EnumType.STRING)
    EstadoPago estado;

    String observacion, motivoAnulacion;
    LocalDateTime fechaAnulacion;
    String arrendatarioNombre, arrendatarioDocumento, inmuebleCodigo, inmuebleNombre;
    @Version Long version;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<PagoDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(PagoDetalle detalle) {
        detalle.setPago(this);
        detalles.add(detalle);
    }
}

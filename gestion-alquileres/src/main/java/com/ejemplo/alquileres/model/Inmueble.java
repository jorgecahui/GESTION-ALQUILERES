package com.ejemplo.alquileres.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "inmueble",
        uniqueConstraints = @UniqueConstraint(name = "uk_inmueble_codigo", columnNames = "codigo"),
        indexes = {
            @Index(name = "idx_inmueble_estado", columnList = "estado"),
            @Index(name = "idx_inmueble_tipo", columnList = "tipo_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inmueble {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 180)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_id")
    private TipoInmueble tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id")
    private Propietario propietario;

    @Column(length = 250)
    private String direccion;

    @Column(length = 250)
    private String referencia;

    @Column(length = 100)
    private String distrito;

    @Column(length = 100)
    private String provincia;

    @Column(length = 100)
    private String departamento;

    @Column(length = 30)
    private String piso;

    @Min(value = -5, message = "El nivel mínimo es -5")
    @Max(value = 100, message = "El nivel máximo es 100")
    private Integer nivel;

    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    private Integer orden;

    @DecimalMin(value = "0.01", message = "El área debe ser mayor que cero")
    @Column(name = "area_m2", precision = 8, scale = 2)
    private BigDecimal areaM2;

    @Min(value = 0, message = "Los dormitorios no pueden ser negativos")
    private Integer dormitorios;

    @Min(value = 0, message = "Los baños no pueden ser negativos")
    private Integer banos;

    @Min(value = 0, message = "Los estacionamientos no pueden ser negativos")
    private Integer estacionamientos;

    @Builder.Default private boolean amoblado = false;

    @DecimalMin(value = "0.00", message = "La renta no puede ser negativa")
    @Column(name = "renta_referencial", precision = 10, scale = 2)
    private BigDecimal rentaReferencial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoInmueble estado = EstadoInmueble.DISPONIBLE;

    @Column(name = "servicios_incluidos", length = 1000)
    private String serviciosIncluidos;

    @Column(length = 1000)
    private String descripcion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Version private Long version;
}

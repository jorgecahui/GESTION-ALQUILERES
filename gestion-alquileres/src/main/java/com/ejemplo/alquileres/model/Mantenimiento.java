package com.ejemplo.alquileres.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(indexes = {@Index(name="idx_mantenimiento_inmueble", columnList="inmueble_id"), @Index(name="idx_mantenimiento_estado", columnList="estado"), @Index(name="idx_mantenimiento_fecha_reporte", columnList="fechaReporte")})
public class Mantenimiento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private String codigo;
 @ManyToOne(optional=false) @JoinColumn(name="inmueble_id") private Inmueble inmueble;
 @ManyToOne @JoinColumn(name="contrato_id") private Contrato contrato;
 @Column(nullable=false) private LocalDate fechaReporte;
 @Column(nullable=false,length=180) private String titulo;
 @Column(length=2000) private String descripcion;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private CategoriaMantenimiento categoria;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private PrioridadMantenimiento prioridad;
 @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private EstadoMantenimiento estado=EstadoMantenimiento.REPORTADO;
 @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private ResponsablePago responsablePago=ResponsablePago.PROPIETARIO;
 private String tecnico,telefonoTecnico;
 @Column(precision=10,scale=2) private BigDecimal costoEstimado,costoReal;
 private LocalDate fechaProgramada,fechaResolucion;
 @ManyToOne @JoinColumn(name="cuota_cargo_id") private Cuota cuotaCargo;
 @Column(length=2000) private String observaciones;
 @Column(nullable=false) private LocalDate fechaRegistro;
 /** Solo el mantenimiento que cambió el estado puede devolverlo a disponible. */
 @Builder.Default private boolean pusoInmuebleEnMantenimiento=false;
}

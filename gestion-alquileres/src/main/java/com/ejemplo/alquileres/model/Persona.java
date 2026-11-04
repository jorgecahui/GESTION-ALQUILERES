package com.ejemplo.alquileres.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name="persona", uniqueConstraints=@UniqueConstraint(name="uk_persona_documento", columnNames={"tipo_documento","numero_documento"}), indexes=@Index(name="idx_persona_documento", columnList="numero_documento"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Persona {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Enumerated(EnumType.STRING) @Column(name="tipo_documento", nullable=false) private TipoDocumento tipoDocumento;
 @NotBlank(message="El documento es obligatorio") @Column(name="numero_documento", nullable=false, length=15) private String numeroDocumento;
 @NotBlank(message="Los nombres son obligatorios") @Column(nullable=false, length=120) private String nombres;
 @Column(length=120) private String apellidos;
 @Email(message="El email no tiene un formato válido") @Column(length=120) private String email;
 @Column(length=30) private String telefono;
 @Column(name="telefono_secundario", length=30) private String telefonoSecundario;
 @Column(length=250) private String direccion;
 @Column(length=120) private String ocupacion;
 @Column(length=1000) private String observaciones;
 @Column(nullable=false) @Builder.Default private boolean activo=true;
 @Column(name="fecha_registro", nullable=false) private LocalDate fechaRegistro;
 public String getNombreCompleto() { return (nombres + " " + (apellidos == null ? "" : apellidos)).trim(); }
}

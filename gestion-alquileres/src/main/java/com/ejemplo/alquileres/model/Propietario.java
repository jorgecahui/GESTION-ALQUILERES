package com.ejemplo.alquileres.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "propietario",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_propietario_documento",
                        columnNames = {"tipo_documento", "numero_documento"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Propietario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El documento es obligatorio")
    @Column(name = "numero_documento", nullable = false, length = 15)
    private String numeroDocumento;

    @NotBlank(message = "El nombre o razón social es obligatorio")
    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Email(message = "El email no tiene un formato válido")
    @Column(length = 120)
    private String email;

    @Column(length = 80)
    private String banco;

    @Column(length = 80)
    private String cuenta;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;
}

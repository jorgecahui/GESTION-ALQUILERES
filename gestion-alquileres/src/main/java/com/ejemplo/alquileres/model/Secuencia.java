package com.ejemplo.alquileres.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "secuencia", uniqueConstraints = @UniqueConstraint(name = "uk_secuencia_clave", columnNames = "clave"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Secuencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 60) private String clave;
    @Column(name = "ultimo_valor", nullable = false) private Long ultimoValor;
}

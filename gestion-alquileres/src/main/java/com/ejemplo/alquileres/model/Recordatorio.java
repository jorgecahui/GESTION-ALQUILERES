package com.ejemplo.alquileres.model;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        indexes = {
            @Index(name = "idx_recordatorio_contrato", columnList = "contrato_id"),
            @Index(name = "idx_recordatorio_fecha", columnList = "fechaEnvio")
        })
public class Recordatorio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contrato_id")
    Contrato contrato;

    @ManyToOne
    @JoinColumn(name = "cuota_id")
    Cuota cuota;

    @Enumerated(EnumType.STRING)
    TipoRecordatorio tipo;

    @Enumerated(EnumType.STRING)
    CanalRecordatorio canal;

    String telefono;

    @Column(length = 3000)
    String mensaje;

    LocalDateTime fechaEnvio;
    String usuario;
}

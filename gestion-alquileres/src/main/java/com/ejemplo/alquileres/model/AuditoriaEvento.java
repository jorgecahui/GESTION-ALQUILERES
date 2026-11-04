package com.ejemplo.alquileres.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Bitácora de operaciones sensibles. Nunca guarda contraseñas ni datos secretos. */
@Entity
@Table(name = "auditoria_evento", indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "fecha"),
        @Index(name = "idx_auditoria_usuario", columnList = "usuario")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditoriaEvento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private LocalDateTime fecha;
    @Column(length = 60) private String usuario;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private AccionAuditoria accion;
    @Column(length = 60) private String entidad;
    @Column(name = "entidad_id") private Long entidadId;
    @Column(length = 500) private String detalle;
}

package com.ejemplo.alquileres.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_username", columnNames = "username"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false, unique = true, length = 60) private String username;
    @Column(nullable = false, length = 150) private String nombre;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RolUsuario rol;
    @Column(nullable = false) @Builder.Default private boolean activo = true;
    @Column(name = "ultimo_acceso") private LocalDateTime ultimoAcceso;
    @Column(name = "intentos_fallidos", nullable = false) @Builder.Default private int intentosFallidos = 0;
    @Column(name = "bloqueado_hasta") private LocalDateTime bloqueadoHasta;

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public boolean esAdmin() { return rol == RolUsuario.ADMIN; }

    public String getRolEtiqueta() { return rol == null ? "" : rol.getEtiqueta(); }
}

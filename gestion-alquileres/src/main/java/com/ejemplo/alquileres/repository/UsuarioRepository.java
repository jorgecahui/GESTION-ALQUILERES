package com.ejemplo.alquileres.repository;

import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    long countByRolAndActivoTrue(RolUsuario rol);
    List<Usuario> findAllByOrderByUsernameAsc();
}

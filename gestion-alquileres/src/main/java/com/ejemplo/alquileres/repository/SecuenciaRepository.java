package com.ejemplo.alquileres.repository;

import com.ejemplo.alquileres.model.Secuencia;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SecuenciaRepository extends JpaRepository<Secuencia, Long> {
    Optional<Secuencia> findByClave(String clave);

    @Modifying
    @Query("update Secuencia s set s.ultimoValor = s.ultimoValor + 1 where s.clave = :clave")
    int incrementar(@Param("clave") String clave);
}

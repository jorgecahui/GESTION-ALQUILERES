package com.ejemplo.alquileres.repository;

import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.AuditoriaEvento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<AuditoriaEvento, Long> {
    @Query(
            "select a from AuditoriaEvento a where (:desde is null or a.fecha >= :desde) and"
                + " (:hasta is null or a.fecha <= :hasta) and (:usuario is null or lower(a.usuario)"
                + " like lower(concat('%', :usuario, '%'))) and (:accion is null or a.accion ="
                + " :accion) order by a.fecha desc, a.id desc")
    List<AuditoriaEvento> buscar(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("usuario") String usuario,
            @Param("accion") AccionAuditoria accion);
}

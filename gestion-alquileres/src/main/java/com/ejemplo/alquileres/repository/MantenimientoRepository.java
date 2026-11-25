package com.ejemplo.alquileres.repository;

import com.ejemplo.alquileres.model.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {
    @Query(
            "select m from Mantenimiento m join fetch m.inmueble i left join fetch m.contrato where"
                + " (:estado is null or m.estado=:estado) and (:prioridad is null or"
                + " m.prioridad=:prioridad) and (:inmuebleId is null or i.id=:inmuebleId) and"
                + " (:categoria is null or m.categoria=:categoria) and (lower(m.codigo) like"
                + " lower(concat('%',:texto,'%')) or lower(m.titulo) like"
                + " lower(concat('%',:texto,'%')) or lower(i.nombre) like"
                + " lower(concat('%',:texto,'%')))")
    Page<Mantenimiento> buscar(
            @Param("texto") String texto,
            @Param("estado") EstadoMantenimiento estado,
            @Param("prioridad") PrioridadMantenimiento prioridad,
            @Param("inmuebleId") Long inmuebleId,
            @Param("categoria") CategoriaMantenimiento categoria,
            Pageable pageable);

    @Query(
            "select m from Mantenimiento m join fetch m.inmueble left join fetch m.contrato left"
                + " join fetch m.cuotaCargo where m.inmueble.id=:id order by m.fechaReporte"
                + " desc,m.id desc")
    List<Mantenimiento> porInmueble(@Param("id") Long id);

    @Query(
            "select m from Mantenimiento m join fetch m.inmueble where m.estado='RESUELTO' and"
                + " m.responsablePago='PROPIETARIO' and m.fechaResolucion between :desde and"
                + " :hasta")
    List<Mantenimiento> gastosPropietario(
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    long countByEstado(EstadoMantenimiento estado);

    long countByEstadoAndPrioridad(EstadoMantenimiento estado, PrioridadMantenimiento prioridad);
}

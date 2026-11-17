package com.ejemplo.alquileres.repository;
import com.ejemplo.alquileres.model.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface InmuebleRepository extends JpaRepository<Inmueble,Long> {
 boolean existsByCodigoIgnoreCase(String codigo); boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
 boolean existsByTipoId(Long tipoId); boolean existsByPropietarioId(Long propietarioId);
 boolean existsByNivelAndOrdenAndEstadoNot(Integer nivel, Integer orden, EstadoInmueble estado);
 boolean existsByNivelAndOrdenAndEstadoNotAndIdNot(Integer nivel, Integer orden, EstadoInmueble estado, Long id);
 @Query("select i from Inmueble i left join fetch i.tipo left join fetch i.propietario where (:estado is null or i.estado=:estado) and (:tipoId is null or i.tipo.id=:tipoId) and (lower(i.codigo) like lower(concat('%',:texto,'%')) or lower(i.nombre) like lower(concat('%',:texto,'%')) or lower(coalesce(i.direccion,'')) like lower(concat('%',:texto,'%')))") Page<Inmueble> buscar(@Param("texto") String texto, @Param("estado") EstadoInmueble estado, @Param("tipoId") Long tipoId, Pageable pageable);
}

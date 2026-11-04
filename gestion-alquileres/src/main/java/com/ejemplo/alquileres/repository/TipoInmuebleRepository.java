package com.ejemplo.alquileres.repository;
import com.ejemplo.alquileres.model.TipoInmueble;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface TipoInmuebleRepository extends JpaRepository<TipoInmueble,Long> {
 boolean existsByCodigoIgnoreCase(String codigo);
 @Query("select t from TipoInmueble t where lower(t.nombre) like lower(concat('%',:texto,'%')) or lower(t.codigo) like lower(concat('%',:texto,'%'))") Page<TipoInmueble> buscar(@Param("texto") String texto, Pageable pageable);
}

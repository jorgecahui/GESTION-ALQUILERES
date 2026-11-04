package com.ejemplo.alquileres.repository;
import com.ejemplo.alquileres.model.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface PropietarioRepository extends JpaRepository<Propietario,Long> {
 boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipo, String numero);
 boolean existsByTipoDocumentoAndNumeroDocumentoAndIdNot(TipoDocumento tipo, String numero, Long id);
 @Query("select p from Propietario p where (:activo is null or p.activo=:activo) and (lower(p.nombre) like lower(concat('%',:texto,'%')) or p.numeroDocumento like concat('%',:texto,'%'))") Page<Propietario> buscar(@Param("texto") String texto, @Param("activo") Boolean activo, Pageable pageable);
}

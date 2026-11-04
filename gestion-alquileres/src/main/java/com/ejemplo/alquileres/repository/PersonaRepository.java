package com.ejemplo.alquileres.repository;
import com.ejemplo.alquileres.model.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface PersonaRepository extends JpaRepository<Persona,Long> {
 boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipo, String numero);
 boolean existsByTipoDocumentoAndNumeroDocumentoAndIdNot(TipoDocumento tipo, String numero, Long id);
 @Query("select p from Persona p where (:tipo is null or p.tipoDocumento=:tipo) and (:activo is null or p.activo=:activo) and (lower(p.nombres) like lower(concat('%',:texto,'%')) or lower(coalesce(p.apellidos,'')) like lower(concat('%',:texto,'%')) or p.numeroDocumento like concat('%',:texto,'%'))") Page<Persona> buscar(@Param("texto") String texto, @Param("tipo") TipoDocumento tipo, @Param("activo") Boolean activo, Pageable pageable);
}

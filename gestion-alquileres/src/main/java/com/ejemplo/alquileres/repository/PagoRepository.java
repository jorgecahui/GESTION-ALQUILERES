package com.ejemplo.alquileres.repository;

import com.ejemplo.alquileres.model.Pago;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.*;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    @Query(
            "select distinct p from Pago p join fetch p.contrato c join fetch c.arrendatario join"
                + " fetch c.inmueble left join fetch p.detalles where p.id=:id")
    Optional<Pago> detalle(@Param("id") Long id);

    @Query(
            "select distinct p from Pago p left join fetch p.detalles where p.contrato.id=:id order"
                + " by p.fechaPago,p.id")
    List<Pago> porContrato(@Param("id") Long id);

    @Query(
            "select distinct p from Pago p join p.detalles d where d.cuota.id=:cuota and"
                + " p.estado='EMITIDO' order by p.fechaRegistro desc,p.id desc")
    List<Pago> activosDeCuota(@Param("cuota") Long cuota);

    @Query(
            "select distinct p from Pago p join fetch p.contrato c join fetch c.arrendatario join"
                + " fetch c.inmueble left join fetch p.detalles where (:desde is null or"
                + " p.fechaPago>=:desde) and (:hasta is null or p.fechaPago<=:hasta) order by"
                + " p.fechaPago desc,p.id desc")
    List<Pago> buscar(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(
            "select p from Pago p join fetch p.contrato c join fetch c.arrendatario join fetch"
                + " c.inmueble where p.fechaPago between :desde and :hasta order by p.fechaPago"
                + " desc,p.id desc")
    List<Pago> listarPeriodo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}

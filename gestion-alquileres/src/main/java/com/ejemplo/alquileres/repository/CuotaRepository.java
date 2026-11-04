package com.ejemplo.alquileres.repository;

import com.ejemplo.alquileres.model.Cuota;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuotaRepository extends JpaRepository<Cuota, Long> {
    boolean existsByContratoIdAndMontoPagadoGreaterThan(Long id, java.math.BigDecimal monto);
    List<Cuota> findByContratoIdOrderByFechaVencimientoAsc(Long contratoId);
    @Query("select c from Cuota c join fetch c.contrato ct join fetch ct.arrendatario join fetch ct.inmueble where c.estado in ('PENDIENTE','PARCIAL') order by c.fechaVencimiento")
    List<Cuota> pendientesConContrato();
    @Modifying @Query("update Cuota c set c.estado='ANULADA' where c.contrato.id=:id and c.fechaVencimiento>:fecha and c.montoPagado=0")
    int anularFuturas(@Param("id") Long id, @Param("fecha") LocalDate fecha);
}

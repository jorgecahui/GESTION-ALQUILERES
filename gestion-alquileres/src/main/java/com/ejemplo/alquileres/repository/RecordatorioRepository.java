package com.ejemplo.alquileres.repository;

import com.ejemplo.alquileres.model.Recordatorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {
    @Query(
            "select r from Recordatorio r where r.cuota.id=:cuota and r.fechaEnvio>=:desde order by"
                + " r.fechaEnvio desc")
    List<Recordatorio> recientes(@Param("cuota") Long cuota, @Param("desde") LocalDateTime desde);

    List<Recordatorio> findByContratoIdOrderByFechaEnvioDesc(Long contratoId);

    @Query("select max(r.fechaEnvio) from Recordatorio r where r.cuota.id=:cuota")
    LocalDateTime ultimoDeCuota(@Param("cuota") Long cuota);

    @Query(
            "select r.cuota.id, max(r.fechaEnvio) from Recordatorio r where r.cuota is not null"
                + " group by r.cuota.id")
    List<Object[]> ultimosPorCuota();
}

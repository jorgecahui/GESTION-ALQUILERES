package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.MantenimientoRepository;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class JlccMantenimientoSeeder {
    private final MantenimientoRepository jlccRepo;
    private final InmuebleRepository jlccInmuebles;
    private final Clock jlccClock;

    public JlccMantenimientoSeeder(MantenimientoRepository r, InmuebleRepository i, Clock c) {
        jlccRepo = r;
        jlccInmuebles = i;
        jlccClock = c;
    }

    public void jlccCargar() {
        if (jlccRepo.count() > 0) return;
        LocalDate jlccHoy = LocalDate.now(jlccClock);
        List<Inmueble> jlccEnMantenimiento =
                jlccInmuebles.findAll().stream()
                        .filter(i -> i.getEstado() == EstadoInmueble.MANTENIMIENTO)
                        .limit(3)
                        .toList();
        String[] jlccTitulos = {"Cambio de grifería", "Revisión eléctrica", "Pintura de interiores"};
        CategoriaMantenimiento[] jlccCategorias = {
            CategoriaMantenimiento.PLOMERIA,
            CategoriaMantenimiento.ELECTRICIDAD,
            CategoriaMantenimiento.PINTURA
        };
        PrioridadMantenimiento[] jlccPrioridades = {
            PrioridadMantenimiento.MEDIA, PrioridadMantenimiento.ALTA, PrioridadMantenimiento.BAJA
        };
        EstadoMantenimiento[] jlccEstados = {
            EstadoMantenimiento.EN_PROCESO,
            EstadoMantenimiento.REPORTADO,
            EstadoMantenimiento.REPORTADO
        };
        for (int i = 0; i < jlccEnMantenimiento.size(); i++) {
            jlccRepo.save(
                    Mantenimiento.builder()
                            .codigo("MNT-DEV-" + String.format("%04d", i + 1))
                            .inmueble(jlccEnMantenimiento.get(i))
                            .fechaReporte(jlccHoy.minusDays(12L - i * 4L))
                            .titulo(jlccTitulos[i])
                            .categoria(jlccCategorias[i])
                            .prioridad(jlccPrioridades[i])
                            .estado(jlccEstados[i])
                            .responsablePago(ResponsablePago.PROPIETARIO)
                            .costoEstimado(
                                    new BigDecimal(
                                            i == 0 ? "180.00" : i == 1 ? "260.00" : "320.00"))
                            .fechaRegistro(jlccHoy.minusDays(12L - i * 4L))
                            .build());
        }
    }
}

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
public class MantenimientoSeeder {
    private final MantenimientoRepository repo;
    private final InmuebleRepository inmuebles;
    private final Clock clock;

    public MantenimientoSeeder(MantenimientoRepository r, InmuebleRepository i, Clock c) {
        repo = r;
        inmuebles = i;
        clock = c;
    }

    public void cargar() {
        if (repo.count() > 0) return;
        LocalDate hoy = LocalDate.now(clock);
        List<Inmueble> enMantenimiento =
                inmuebles.findAll().stream()
                        .filter(i -> i.getEstado() == EstadoInmueble.MANTENIMIENTO)
                        .limit(3)
                        .toList();
        String[] titulos = {"Cambio de grifería", "Revisión eléctrica", "Pintura de interiores"};
        CategoriaMantenimiento[] categorias = {
            CategoriaMantenimiento.PLOMERIA,
            CategoriaMantenimiento.ELECTRICIDAD,
            CategoriaMantenimiento.PINTURA
        };
        PrioridadMantenimiento[] prioridades = {
            PrioridadMantenimiento.MEDIA, PrioridadMantenimiento.ALTA, PrioridadMantenimiento.BAJA
        };
        EstadoMantenimiento[] estados = {
            EstadoMantenimiento.EN_PROCESO,
            EstadoMantenimiento.REPORTADO,
            EstadoMantenimiento.REPORTADO
        };
        for (int i = 0; i < enMantenimiento.size(); i++) {
            repo.save(
                    Mantenimiento.builder()
                            .codigo("MNT-DEV-" + String.format("%04d", i + 1))
                            .inmueble(enMantenimiento.get(i))
                            .fechaReporte(hoy.minusDays(12L - i * 4L))
                            .titulo(titulos[i])
                            .categoria(categorias[i])
                            .prioridad(prioridades[i])
                            .estado(estados[i])
                            .responsablePago(ResponsablePago.PROPIETARIO)
                            .costoEstimado(
                                    new BigDecimal(
                                            i == 0 ? "180.00" : i == 1 ? "260.00" : "320.00"))
                            .fechaRegistro(hoy.minusDays(12L - i * 4L))
                            .build());
        }
    }
}

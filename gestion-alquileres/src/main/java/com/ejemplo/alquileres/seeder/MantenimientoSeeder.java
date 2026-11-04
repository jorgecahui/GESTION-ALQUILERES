package com.ejemplo.alquileres.seeder;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** Incidencias demostrativas, exclusivamente para el perfil dev mediante DatabaseSeeder. */
@Component public class MantenimientoSeeder {
 private final MantenimientoRepository repo; private final InmuebleRepository inmuebles; private final Clock clock;
 public MantenimientoSeeder(MantenimientoRepository r,InmuebleRepository i,Clock c){repo=r;inmuebles=i;clock=c;}
 public void cargar(){if(repo.count()>0)return;LocalDate hoy=LocalDate.now(clock);var lista=inmuebles.findAll();if(lista.isEmpty())return;Inmueble primero=lista.get(0);repo.save(Mantenimiento.builder().codigo("MNT-DEV-0001").inmueble(primero).fechaReporte(hoy.minusDays(12)).titulo("Cambio de grifería").categoria(CategoriaMantenimiento.PLOMERIA).prioridad(PrioridadMantenimiento.MEDIA).estado(EstadoMantenimiento.RESUELTO).responsablePago(ResponsablePago.PROPIETARIO).costoEstimado(new BigDecimal("180.00")).costoReal(new BigDecimal("165.00")).fechaResolucion(hoy.minusDays(8)).fechaRegistro(hoy.minusDays(12)).build());if(lista.size()>1)repo.save(Mantenimiento.builder().codigo("MNT-DEV-0002").inmueble(lista.get(1)).fechaReporte(hoy.minusDays(2)).titulo("Revisión eléctrica").categoria(CategoriaMantenimiento.ELECTRICIDAD).prioridad(PrioridadMantenimiento.ALTA).estado(EstadoMantenimiento.REPORTADO).responsablePago(ResponsablePago.PROPIETARIO).fechaRegistro(hoy.minusDays(2)).build());}
}

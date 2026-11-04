package com.ejemplo.alquileres.service;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.*;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class RentabilidadService {
 private final InmuebleRepository inmuebles; private final ContratoRepository contratos; private final PagoRepository pagos; private final MantenimientoRepository mantenimientos; private final Clock clock;
 public record Fila(Long inmuebleId,String codigo,String inmueble,BigDecimal capital,BigDecimal mora,BigDecimal ingresos,BigDecimal gastos,BigDecimal neto,BigDecimal margen,long diasOcupado,BigDecimal ocupacion){}
 public record Resumen(List<Fila> filas,BigDecimal ingresos,BigDecimal gastos,BigDecimal neto,BigDecimal margen){}
 public Resumen porInmueble(LocalDate desde,LocalDate hasta){if(desde==null||hasta==null||hasta.isBefore(desde))throw new IllegalArgumentException("El período no es válido.");Map<Long,BigDecimal> capital=new HashMap<>(),mora=new HashMap<>(),gastos=new HashMap<>();for(Pago p:pagos.listarPeriodo(desde,hasta))if(p.getEstado()==EstadoPago.EMITIDO){Long id=p.getContrato().getInmueble().getId();capital.merge(id,nvl(p.getMontoCapital()),BigDecimal::add);mora.merge(id,nvl(p.getMontoMora()),BigDecimal::add);}for(Mantenimiento m:mantenimientos.gastosPropietario(desde,hasta))gastos.merge(m.getInmueble().getId(),nvl(m.getCostoReal()),BigDecimal::add);long totalDias=ChronoUnit.DAYS.between(desde,hasta)+1;List<Fila> filas=new ArrayList<>();for(Inmueble i:inmuebles.findAll()){BigDecimal cap=dinero(capital.getOrDefault(i.getId(),BigDecimal.ZERO)),mor=dinero(mora.getOrDefault(i.getId(),BigDecimal.ZERO)),ingreso=dinero(cap.add(mor)),gasto=dinero(gastos.getOrDefault(i.getId(),BigDecimal.ZERO)),neto=dinero(ingreso.subtract(gasto));long ocupado=contratos.findByInmuebleIdOrderByFechaInicioDesc(i.getId()).stream().mapToLong(c->interseccion(c,desde,hasta)).sum();ocupado=Math.min(totalDias,ocupado);BigDecimal margen=ingreso.signum()==0?BigDecimal.ZERO.setScale(2):dinero(neto.multiply(BigDecimal.valueOf(100)).divide(ingreso,2,RoundingMode.HALF_UP));filas.add(new Fila(i.getId(),i.getCodigo(),i.getNombre(),cap,mor,ingreso,gasto,neto,margen,ocupado,porcentaje(ocupado,totalDias)));}filas.sort(Comparator.comparing(Fila::neto).reversed());return totalizar(filas);}
 public Resumen anoActual(Long inmuebleId){LocalDate hoy=LocalDate.now(clock);List<Fila> fila=porInmueble(hoy.withDayOfYear(1),hoy).filas().stream().filter(f->f.inmuebleId().equals(inmuebleId)).toList();return totalizar(fila);}
 private Resumen totalizar(List<Fila> filas){BigDecimal ingreso=filas.stream().map(Fila::ingresos).reduce(BigDecimal.ZERO,BigDecimal::add),gasto=filas.stream().map(Fila::gastos).reduce(BigDecimal.ZERO,BigDecimal::add),neto=ingreso.subtract(gasto);return new Resumen(filas,dinero(ingreso),dinero(gasto),dinero(neto),ingreso.signum()==0?BigDecimal.ZERO.setScale(2):dinero(neto.multiply(BigDecimal.valueOf(100)).divide(ingreso,2,RoundingMode.HALF_UP)));}
 private long interseccion(Contrato c,LocalDate desde,LocalDate hasta){LocalDate fin=c.getFechaTerminoEfectiva()==null?c.getFechaFin():c.getFechaTerminoEfectiva();LocalDate inicio=c.getFechaInicio().isAfter(desde)?c.getFechaInicio():desde;LocalDate ultimo=fin.isBefore(hasta)?fin:hasta;return ultimo.isBefore(inicio)?0:ChronoUnit.DAYS.between(inicio,ultimo)+1;}
 private static BigDecimal nvl(BigDecimal n){return n==null?BigDecimal.ZERO:n;}private static BigDecimal dinero(BigDecimal n){return n.setScale(2,RoundingMode.HALF_UP);}private static BigDecimal porcentaje(long a,long b){return b==0?BigDecimal.ZERO.setScale(2):BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(b),2,RoundingMode.HALF_UP);}
}

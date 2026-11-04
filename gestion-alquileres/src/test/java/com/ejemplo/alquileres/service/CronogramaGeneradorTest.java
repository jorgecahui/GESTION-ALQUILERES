package com.ejemplo.alquileres.service;
import org.junit.jupiter.api.*; import java.math.*; import java.time.*; import static org.junit.jupiter.api.Assertions.*;
class CronogramaGeneradorTest { private final CronogramaGenerador g=new CronogramaGenerador();
 @Test void generaDoceCuotasYAjustaPrimerVencimiento(){var c=g.generar(LocalDate.of(2026,1,31),12,28,new BigDecimal("1000"),BigDecimal.ZERO);assertEquals(12,c.size());assertEquals(LocalDate.of(2026,1,31),c.get(0).getFechaVencimiento());assertEquals(LocalDate.of(2026,2,28),c.get(1).getFechaVencimiento());assertEquals("Renta febrero 2026",c.get(1).getConcepto());}
 @Test void aplicaIncrementoAnualRedondeado(){var c=g.generar(LocalDate.of(2026,1,1),24,10,new BigDecimal("1000.00"),new BigDecimal("5"));assertEquals(new BigDecimal("1000.00"),c.get(11).getMontoCuota());assertEquals(new BigDecimal("1050.00"),c.get(12).getMontoCuota());}
}

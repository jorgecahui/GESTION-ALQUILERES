package com.ejemplo.alquileres.service;
import com.ejemplo.alquileres.exception.NegocioException; import com.ejemplo.alquileres.model.*; import com.ejemplo.alquileres.repository.*; import org.junit.jupiter.api.*; import java.math.BigDecimal; import java.time.*; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
class InmuebleServiceTest {private final InmuebleRepository repo=mock(InmuebleRepository.class);private final SecuenciaService secuencia=mock(SecuenciaService.class);private final InmuebleService service=new InmuebleService(repo,mock(TipoInmuebleRepository.class),mock(PropietarioRepository.class),secuencia,DatosFase6.permisosAdmin(),Clock.systemUTC());
 @Test void generaCodigoYNoPermiteAlquiladoManual(){when(secuencia.siguiente("INMUEBLE")).thenReturn(1L);when(secuencia.formatear("INM-",1L,4)).thenReturn("INM-0001");assertEquals("INM-0001",service.proximoCodigo());Inmueble i=Inmueble.builder().codigo("INM-0001").nombre("Depa").estado(EstadoInmueble.ALQUILADO).build();assertThrows(NegocioException.class,()->service.guardar(i));}
 @Test void noEliminaTipoConInmuebles(){TipoInmuebleRepository tipos=mock(TipoInmuebleRepository.class);InmuebleRepository inmuebles=mock(InmuebleRepository.class);TipoInmuebleService servicio=new TipoInmuebleService(tipos,inmuebles,DatosFase6.permisosAdmin());when(inmuebles.existsByTipoId(4L)).thenReturn(true);TipoInmueble tipo=TipoInmueble.builder().id(4L).activo(true).build();when(tipos.findById(4L)).thenReturn(java.util.Optional.of(tipo));servicio.eliminar(4L);assertFalse(tipo.isActivo());verify(tipos,never()).deleteById(4L);}
 @Test void permiteEditarDatosDeInmuebleAlquilado(){when(repo.findById(1L)).thenReturn(java.util.Optional.of(Inmueble.builder().id(1L).codigo("INM-0001").nombre("Viejo").estado(EstadoInmueble.ALQUILADO).build()));when(repo.existsByCodigoIgnoreCaseAndIdNot("INM-0001",1L)).thenReturn(false);when(repo.save(any())).thenAnswer(inv->inv.getArgument(0));Inmueble editado=Inmueble.builder().id(1L).codigo("INM-0001").nombre("Nuevo").estado(EstadoInmueble.ALQUILADO).build();Inmueble guardado=service.guardar(editado);assertEquals("Nuevo",guardado.getNombre());assertEquals(EstadoInmueble.ALQUILADO,guardado.getEstado());}
 @Test void noCambiaEstadoDeInmuebleAlquiladoManualmente(){when(repo.findById(1L)).thenReturn(java.util.Optional.of(Inmueble.builder().id(1L).codigo("INM-0001").nombre("Viejo").estado(EstadoInmueble.ALQUILADO).build()));Inmueble editado=Inmueble.builder().id(1L).codigo("INM-0001").nombre("Viejo").estado(EstadoInmueble.DISPONIBLE).build();assertThrows(NegocioException.class,()->service.guardar(editado));}
 @Test void editarConservaTodosLosCamposDelInmueble(){
  when(repo.save(any())).thenAnswer(inv->{Inmueble x=inv.getArgument(0);if(x.getId()==null)x.setId(1L);return x;});
  Inmueble nuevo=Inmueble.builder().codigo("INM-0001").nombre("Depto original")
    .direccion("Av. Principal 123").referencia("Frente al parque")
    .distrito("Miraflores").provincia("Lima").departamento("Lima").piso("3")
    .areaM2(new BigDecimal("85.50")).dormitorios(3).banos(2).estacionamientos(1).amoblado(true)
    .rentaReferencial(new BigDecimal("2500.00")).estado(EstadoInmueble.DISPONIBLE)
    .serviciosIncluidos("Agua, luz, internet").descripcion("Vista al parque").build();
  Inmueble creado=service.guardar(nuevo);
  assertEquals(1L,creado.getId());
  assertNotNull(creado.getFechaRegistro());
  LocalDate fechaRegistro=creado.getFechaRegistro();
  when(repo.findById(1L)).thenReturn(java.util.Optional.of(creado));
  when(repo.existsByCodigoIgnoreCaseAndIdNot("INM-0001",1L)).thenReturn(false);

  Inmueble edicion=Inmueble.builder().id(1L).codigo("INM-0001").nombre("Depto actualizado")
    .estado(EstadoInmueble.DISPONIBLE).amoblado(true).build();
  Inmueble actualizado=service.guardar(edicion);

  assertEquals("Depto actualizado",actualizado.getNombre());
  assertEquals("Av. Principal 123",actualizado.getDireccion());
  assertEquals("Frente al parque",actualizado.getReferencia());
  assertEquals("Miraflores",actualizado.getDistrito());
  assertEquals("Lima",actualizado.getProvincia());
  assertEquals("Lima",actualizado.getDepartamento());
  assertEquals("3",actualizado.getPiso());
  assertEquals(new BigDecimal("85.50"),actualizado.getAreaM2());
  assertEquals(3,actualizado.getDormitorios());
  assertEquals(2,actualizado.getBanos());
  assertEquals(1,actualizado.getEstacionamientos());
  assertTrue(actualizado.isAmoblado());
  assertEquals(new BigDecimal("2500.00"),actualizado.getRentaReferencial());
  assertEquals(EstadoInmueble.DISPONIBLE,actualizado.getEstado());
  assertEquals("Agua, luz, internet",actualizado.getServiciosIncluidos());
  assertEquals("Vista al parque",actualizado.getDescripcion());
  assertEquals(fechaRegistro,actualizado.getFechaRegistro());
 }
}

package com.ejemplo.alquileres.service;
import com.ejemplo.alquileres.exception.NegocioException; import com.ejemplo.alquileres.model.*; import com.ejemplo.alquileres.repository.PersonaRepository;
import lombok.RequiredArgsConstructor; import org.springframework.data.domain.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.*;
@Service @RequiredArgsConstructor public class PersonaService {
 private final PersonaRepository repository; private final ValidadorDocumento documentos; private final Permisos permisos; private final Clock clock;
 public Page<Persona> listar(String texto, TipoDocumento tipo, Boolean activo, int pagina, int tamano) { return repository.buscar(texto==null?"":texto.trim(), tipo, activo, PageRequest.of(Math.max(0,pagina),tamano, Sort.by("nombres").ascending())); }
 @Transactional public Persona guardar(Persona entrada) {
  permisos.exigirGestion(); boolean nueva=entrada.getId()==null; if(entrada.getNombres()==null||entrada.getNombres().isBlank())throw new NegocioException("El nombre es obligatorio"); String documento=documentos.normalizar(entrada.getTipoDocumento(), entrada.getNumeroDocumento());
  boolean existe=nueva ? repository.existsByTipoDocumentoAndNumeroDocumento(entrada.getTipoDocumento(), documento) : repository.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(entrada.getTipoDocumento(),documento,entrada.getId());
  if(existe) throw new NegocioException("Ya existe una persona con ese documento");
  Persona p=nueva?new Persona():repository.findById(entrada.getId()).orElseThrow(()->new NegocioException("La persona ya no existe")); copiar(entrada,p); p.setNumeroDocumento(documento); p.setTelefono(normalizarTelefono(p.getTelefono())); p.setTelefonoSecundario(normalizarTelefono(p.getTelefonoSecundario())); if(nueva) p.setFechaRegistro(LocalDate.now(clock)); return repository.save(p);
 }
 @Transactional public void desactivar(Long id){ permisos.exigirGestion(); Persona p=repository.findById(id).orElseThrow(()->new NegocioException("La persona no existe")); p.setActivo(false); repository.save(p); }
 @Transactional public void eliminar(Long id){ permisos.exigirGestion(); repository.delete(repository.findById(id).orElseThrow(()->new NegocioException("La persona no existe"))); }
 private void copiar(Persona a, Persona b){ b.setTipoDocumento(a.getTipoDocumento()); b.setNombres(a.getNombres().trim()); b.setApellidos(vacio(a.getApellidos())); b.setEmail(vacio(a.getEmail())); b.setTelefono(a.getTelefono()); b.setTelefonoSecundario(a.getTelefonoSecundario()); b.setDireccion(vacio(a.getDireccion())); b.setOcupacion(vacio(a.getOcupacion())); b.setObservaciones(vacio(a.getObservaciones())); if(a.getId()==null) b.setActivo(a.isActivo()); }
 static String normalizarTelefono(String valor){ if(valor==null||valor.isBlank())return null; String n=valor.replaceAll("[\\s()-]",""); if(n.startsWith("+51")) n=n.substring(3); else if(n.startsWith("0051"))n=n.substring(4); if(n.matches("9\\d{8}")) return "+51 "+n; if(n.matches("(?:0?1)?\\d{6,8}"))return n; throw new NegocioException("El teléfono debe ser un móvil peruano válido o un fijo"); }
 private static String vacio(String v){return v==null||v.isBlank()?null:v.trim();}
}

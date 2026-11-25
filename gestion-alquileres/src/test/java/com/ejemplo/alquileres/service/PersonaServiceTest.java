package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ejemplo.alquileres.exception.JlccNegocioException;
import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.repository.PersonaRepository;

import org.junit.jupiter.api.*;

import java.time.*;
import java.util.*;

class PersonaServiceTest {
    private final PersonaRepository repo = mock(PersonaRepository.class);
    private final JlccAuditoriaService auditoria = mock(JlccAuditoriaService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final JlccPersonaService service =
            new JlccPersonaService(
                    repo, new JlccValidadorDocumento(), DatosFase6.permisosAdmin(), clock, auditoria);

    private Persona persona() {
        return Persona.builder()
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("12345678")
                .nombres("Ana")
                .activo(true)
                .build();
    }

    @Test
    void altaYEdicionSinCambiarDocumento() {
        when(repo.existsByTipoDocumentoAndNumeroDocumento(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Persona creada = service.jlccGuardar(persona());
        assertEquals(LocalDate.of(2026, 1, 1), creada.getFechaRegistro());
        Persona editada = persona();
        editada.setId(3L);
        when(repo.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(any(), any(), any()))
                .thenReturn(false);
        when(repo.findById(3L)).thenReturn(Optional.of(creada));
        assertDoesNotThrow(() -> service.jlccGuardar(editada));
    }

    @Test
    void rechazaDocumentoDuplicado() {
        when(repo.existsByTipoDocumentoAndNumeroDocumento(any(), any())).thenReturn(true);
        assertThrows(JlccNegocioException.class, () -> service.jlccGuardar(persona()));
    }

    @Test
    void desactiva() {
        Persona p = persona();
        p.setActivo(true);
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        service.jlccDesactivar(1L);
        assertFalse(p.isActivo());
    }

    @Test
    void editarNoReactivaUnaPersonaInactiva() {
        Persona original = persona();
        original.setId(3L);
        original.setActivo(false);
        when(repo.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(any(), any(), any()))
                .thenReturn(false);
        when(repo.findById(3L)).thenReturn(Optional.of(original));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Persona editada = persona();
        editada.setId(3L);
        editada.setActivo(true);
        service.jlccGuardar(editada);
        assertFalse(original.isActivo());
    }

    @Test
    void crearConTodosLosCamposYEditarUnoConservaElResto() {
        when(repo.existsByTipoDocumentoAndNumeroDocumento(any(), any())).thenReturn(false);
        when(repo.save(any()))
                .thenAnswer(
                        i -> {
                            Persona p = i.getArgument(0);
                            if (p.getId() == null) p.setId(3L);
                            return p;
                        });
        Persona nueva =
                Persona.builder()
                        .tipoDocumento(TipoDocumento.DNI)
                        .numeroDocumento("12345678")
                        .nombres("Ana")
                        .apellidos("Pérez")
                        .email("ana@ejemplo.pe")
                        .telefono("987654321")
                        .telefonoSecundario("999888777")
                        .direccion("Av. Principal 123")
                        .ocupacion("Docente")
                        .observaciones("Cliente puntual")
                        .activo(true)
                        .build();
        Persona creada = service.jlccGuardar(nueva);
        LocalDate fechaRegistro = creada.getFechaRegistro();

        when(repo.findById(3L)).thenReturn(Optional.of(creada));
        when(repo.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(any(), any(), any()))
                .thenReturn(false);
        Persona edicion =
                Persona.builder()
                        .id(3L)
                        .tipoDocumento(TipoDocumento.DNI)
                        .numeroDocumento("12345678")
                        .nombres("Ana")
                        .ocupacion("Ingeniera")
                        .build();
        Persona actualizada = service.jlccGuardar(edicion);

        assertEquals("Ingeniera", actualizada.getOcupacion());
        assertEquals("Pérez", actualizada.getApellidos());
        assertEquals("ana@ejemplo.pe", actualizada.getEmail());
        assertEquals("+51 987654321", actualizada.getTelefono());
        assertEquals("+51 999888777", actualizada.getTelefonoSecundario());
        assertEquals("Av. Principal 123", actualizada.getDireccion());
        assertEquals("Cliente puntual", actualizada.getObservaciones());
        assertEquals(fechaRegistro, actualizada.getFechaRegistro());
    }

    @Test
    void textoVacioLimpiaElCampoYConservaElResto() {
        when(repo.existsByTipoDocumentoAndNumeroDocumento(any(), any())).thenReturn(false);
        when(repo.save(any()))
                .thenAnswer(
                        i -> {
                            Persona p = i.getArgument(0);
                            if (p.getId() == null) p.setId(4L);
                            return p;
                        });
        Persona creada =
                service.jlccGuardar(
                        Persona.builder()
                                .tipoDocumento(TipoDocumento.DNI)
                                .numeroDocumento("12345678")
                                .nombres("Ana")
                                .direccion("Av. Principal 123")
                                .ocupacion("Docente")
                                .activo(true)
                                .build());

        when(repo.findById(4L)).thenReturn(Optional.of(creada));
        when(repo.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(any(), any(), any()))
                .thenReturn(false);
        Persona edicion =
                Persona.builder()
                        .id(4L)
                        .tipoDocumento(TipoDocumento.DNI)
                        .numeroDocumento("12345678")
                        .nombres("Ana")
                        .direccion("")
                        .build();
        Persona actualizada = service.jlccGuardar(edicion);

        assertNull(actualizada.getDireccion());
        assertEquals("Docente", actualizada.getOcupacion());
    }

    @Test
    void rechazaTelefonoSecundarioInvalido() {
        Persona p = persona();
        p.setTelefonoSecundario("123");
        when(repo.existsByTipoDocumentoAndNumeroDocumento(any(), any())).thenReturn(false);
        JlccNegocioException error = assertThrows(JlccNegocioException.class, () -> service.jlccGuardar(p));
        assertTrue(error.getMessage().contains("teléfono"));
        verify(repo, never()).save(any());
    }

    @Test
    void desactivaYReactiva() {
        Persona p = persona();
        p.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.jlccDesactivar(1L);
        assertFalse(p.isActivo());

        service.jlccReactivar(1L);
        assertTrue(p.isActivo());
        verify(auditoria, times(2)).jlccRegistrar(any(), any(), any(), any());
    }

    @Test
    void reactivarUnaPersonaYaActivaEsRechazado() {
        Persona p = persona();
        p.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(p));

        JlccNegocioException error = assertThrows(JlccNegocioException.class, () -> service.jlccReactivar(1L));

        assertTrue(error.getMessage().contains("ya está activa"));
        verify(repo, never()).save(any());
    }

    @Test
    void consultaNoPuedeReactivarPersonas() {
        JlccPersonaService soloConsulta =
                new JlccPersonaService(
                        repo,
                        new JlccValidadorDocumento(),
                        DatosFase6.permisosConsulta(),
                        clock,
                        auditoria);

        JlccNegocioException error =
                assertThrows(JlccNegocioException.class, () -> soloConsulta.jlccReactivar(1L));

        assertTrue(error.getMessage().contains("permiso"));
        verify(repo, never()).findById(any());
    }
}

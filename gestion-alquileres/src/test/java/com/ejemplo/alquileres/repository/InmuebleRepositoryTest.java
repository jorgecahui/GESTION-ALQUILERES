package com.ejemplo.alquileres.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.TipoInmueble;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@DataJpaTest(
        properties = {
            "spring.datasource.url=jdbc:sqlite:file:inmueble-repo-test?mode=memory&cache=shared",
            "spring.datasource.driver-class-name=org.sqlite.JDBC",
            "spring.datasource.hikari.maximum-pool-size=1",
            "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InmuebleRepositoryTest {
    private static final Pageable PRIMERA_PAGINA = PageRequest.of(0, 100, Sort.by("codigo"));

    @Autowired private InmuebleRepository inmuebles;
    @Autowired private TipoInmuebleRepository tipos;

    private TipoInmueble departamento;
    private TipoInmueble local;

    @BeforeEach
    void cargarDatos() {
        departamento =
                tipos.save(
                        TipoInmueble.builder()
                                .codigo("DEP")
                                .nombre("Departamento")
                                .activo(true)
                                .build());
        local =
                tipos.save(
                        TipoInmueble.builder().codigo("LOC").nombre("Local").activo(true).build());
        inmuebles.saveAll(
                List.of(
                        inmueble("INM-0001", "Luz Norte", departamento, EstadoInmueble.DISPONIBLE),
                        inmueble("INM-0002", "Luz Sur", departamento, EstadoInmueble.ALQUILADO),
                        inmueble("INM-0003", "Mar Azul", departamento, EstadoInmueble.DISPONIBLE),
                        inmueble("INM-0004", "Luz Este", local, EstadoInmueble.DISPONIBLE)));
        inmuebles.flush();
        tipos.flush();
    }

    @Test
    void filtraPorTipo() {
        Page<Inmueble> pagina = inmuebles.buscar("", null, departamento.getId(), PRIMERA_PAGINA);

        assertEquals(3, pagina.getTotalElements());
        assertTrue(
                pagina.getContent().stream()
                        .allMatch(i -> i.getTipo().getId().equals(departamento.getId())));
    }

    @Test
    void filtraPorTipoYEstado() {
        Page<Inmueble> pagina =
                inmuebles.buscar(
                        "", EstadoInmueble.DISPONIBLE, departamento.getId(), PRIMERA_PAGINA);

        assertEquals(2, pagina.getTotalElements());
        assertTrue(
                pagina.getContent().stream()
                        .allMatch(
                                i ->
                                        i.getTipo().getId().equals(departamento.getId())
                                                && i.getEstado() == EstadoInmueble.DISPONIBLE));
    }

    @Test
    void filtraPorTipoYTexto() {
        Page<Inmueble> pagina = inmuebles.buscar("luz", null, departamento.getId(), PRIMERA_PAGINA);

        Set<String> codigos =
                pagina.getContent().stream().map(Inmueble::getCodigo).collect(Collectors.toSet());
        assertEquals(2, pagina.getTotalElements());
        assertEquals(Set.of("INM-0001", "INM-0002"), codigos);
    }

    private Inmueble inmueble(
            String codigo, String nombre, TipoInmueble tipo, EstadoInmueble estado) {
        return Inmueble.builder()
                .codigo(codigo)
                .nombre(nombre)
                .tipo(tipo)
                .estado(estado)
                .fechaRegistro(LocalDate.of(2026, 1, 1))
                .build();
    }
}

package com.ejemplo.alquileres.seeder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.EstadoContrato;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.repository.ContratoRepository;
import com.ejemplo.alquileres.repository.InmuebleRepository;
import com.ejemplo.alquileres.repository.MantenimientoRepository;
import com.ejemplo.alquileres.service.MorosidadService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "app.seeder.enabled=true",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=WARN"
})
@ActiveProfiles("dev")
class DatosDevSeederTest {
    private static final Path DIRECTORIO = Path.of("target", "test-data-seeder");

    static {
        try {
            Files.createDirectories(DIRECTORIO);
            Files.createDirectories(DIRECTORIO.resolve("logs"));
            Files.deleteIfExists(DIRECTORIO.resolve("alquileres.db"));
            Files.deleteIfExists(DIRECTORIO.resolve("alquileres.db-wal"));
            Files.deleteIfExists(DIRECTORIO.resolve("alquileres.db-shm"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("app.data-dir", DIRECTORIO::toString);
    }

    @Autowired private DatabaseSeeder seeder;
    @Autowired private InmuebleRepository inmuebles;
    @Autowired private ContratoRepository contratos;
    @Autowired private MantenimientoRepository mantenimientos;
    @Autowired private MorosidadService morosidad;

    @Test void siembraElEdificioCompletoYEsIdempotente() {
        seeder.run();
        seeder.run();

        List<Inmueble> todas = inmuebles.findAll();
        assertEquals(20, todas.size());
        assertEquals(20, todas.stream()
                .map(i -> i.getNivel() + "-" + i.getOrden())
                .distinct()
                .count());
        assertTrue(todas.stream().allMatch(i -> i.getNivel() != null && i.getOrden() != null));
        Map<Integer, Long> porNivel = todas.stream()
                .collect(Collectors.groupingBy(Inmueble::getNivel, Collectors.counting()));
        for (int nivel = 1; nivel <= 4; nivel++) {
            assertEquals(5L, porNivel.getOrDefault(nivel, 0L));
        }

        Map<EstadoInmueble, Long> estados = todas.stream()
                .collect(Collectors.groupingBy(Inmueble::getEstado, Collectors.counting()));
        assertEquals(5L, estados.getOrDefault(EstadoInmueble.DISPONIBLE, 0L));
        assertEquals(10L, estados.getOrDefault(EstadoInmueble.ALQUILADO, 0L));
        assertEquals(3L, estados.getOrDefault(EstadoInmueble.MANTENIMIENTO, 0L));
        assertEquals(2L, estados.getOrDefault(EstadoInmueble.INACTIVO, 0L));

        List<Contrato> vigentes = contratos.listarTodos();
        assertEquals(10, vigentes.size());
        assertTrue(vigentes.stream().allMatch(c -> c.getEstado() == EstadoContrato.VIGENTE));
        assertEquals(10, vigentes.stream()
                .map(c -> c.getArrendatario().getId())
                .distinct()
                .count());

        Set<Long> alquilados = todas.stream()
                .filter(i -> i.getEstado() == EstadoInmueble.ALQUILADO)
                .map(Inmueble::getId)
                .collect(Collectors.toSet());
        Set<Long> conContrato = vigentes.stream()
                .map(c -> c.getInmueble().getId())
                .collect(Collectors.toSet());
        assertEquals(alquilados, conContrato);
        assertEquals(3, mantenimientos.count());

        Map<SemaforoMorosidad, Long> reparto = vigentes.stream()
                .collect(Collectors.groupingBy(morosidad::semaforo, Collectors.counting()));
        assertEquals(6L, reparto.getOrDefault(SemaforoMorosidad.AL_DIA, 0L));
        assertEquals(2L, reparto.getOrDefault(SemaforoMorosidad.ATRASO_LEVE, 0L));
        assertEquals(2L, reparto.getOrDefault(SemaforoMorosidad.MOROSO, 0L));

        assertEquals(20, inmuebles.count());
        assertEquals(10, contratos.count());
        assertEquals(3, mantenimientos.count());
    }
}

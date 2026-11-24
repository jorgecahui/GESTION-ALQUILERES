package com.ejemplo.alquileres.seeder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoContrato;
import com.ejemplo.alquileres.model.EstadoCuota;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
    @Autowired private Clock clock;

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
        for (Contrato contrato : vigentes) {
            long vencidas = morosidad.vencidas(contrato).size();
            switch (morosidad.semaforo(contrato)) {
                case AL_DIA -> assertEquals(0, vencidas, contrato.getCodigo());
                case ATRASO_LEVE -> assertEquals(1, vencidas, contrato.getCodigo());
                case MOROSO -> assertEquals(2, vencidas, contrato.getCodigo());
            }
        }

        LocalDate hoy = LocalDate.now(clock);
        Map<String, Long> vencimientos = vigentes.stream()
                .collect(Collectors.groupingBy(
                        contrato -> categoriaVencimiento(contrato, hoy), Collectors.counting()));
        assertEquals(2L, vencimientos.getOrDefault("0-30", 0L));
        assertEquals(2L, vencimientos.getOrDefault("31-90", 0L));
        assertEquals(5L, vencimientos.getOrDefault("MAS-90", 0L));
        assertEquals(1L, vencimientos.getOrDefault("VENCIDO", 0L));

        List<Contrato> lejanos = vigentes.stream()
                .filter(contrato -> diasParaVencer(contrato, hoy) > 90)
                .toList();
        assertEquals(5, lejanos.size());
        assertEquals(5, lejanos.stream().map(Contrato::getFechaFin).distinct().count());

        assertTrue(vigentes.stream().anyMatch(contrato ->
                morosidad.semaforo(contrato) == SemaforoMorosidad.MOROSO
                        && diasParaVencer(contrato, hoy) > 90));
        assertTrue(vigentes.stream().anyMatch(contrato ->
                morosidad.semaforo(contrato) == SemaforoMorosidad.AL_DIA
                        && diasParaVencer(contrato, hoy) >= 0
                        && diasParaVencer(contrato, hoy) <= 30));

        for (Contrato contrato : vigentes) {
            int meses = (int) ChronoUnit.MONTHS.between(contrato.getFechaInicio(),
                    contrato.getFechaFin().plusDays(1));
            List<Cuota> cuotas = contrato.getCuotas().stream()
                    .sorted(Comparator.comparingInt(Cuota::getNumero))
                    .toList();
            assertEquals(meses, cuotas.size(), contrato.getCodigo());
            assertEquals(1, cuotas.get(0).getNumero());
            assertEquals(meses, cuotas.get(meses - 1).getNumero());
            for (var cuota : cuotas) {
                assertTrue(!cuota.getFechaVencimiento().isBefore(contrato.getFechaInicio()));
                assertTrue(!cuota.getFechaVencimiento().isAfter(contrato.getFechaFin()));
            }
        }

        Contrato vencido = vigentes.stream()
                .filter(contrato -> contrato.getFechaFin().isBefore(hoy))
                .findFirst().orElseThrow();
        assertTrue(vencido.getCuotas().stream()
                .allMatch(cuota -> cuota.getFechaVencimiento().isBefore(hoy)));
        assertEquals(2, vencido.getCuotas().stream()
                .filter(cuota -> cuota.getEstado() == EstadoCuota.PENDIENTE).count());

        assertEquals(20, inmuebles.count());
        assertEquals(10, contratos.count());
        assertEquals(3, mantenimientos.count());
    }

    private long diasParaVencer(Contrato contrato, LocalDate hoy) {
        return ChronoUnit.DAYS.between(hoy, contrato.getFechaFin());
    }

    private String categoriaVencimiento(Contrato contrato, LocalDate hoy) {
        long dias = diasParaVencer(contrato, hoy);
        if (dias < 0) return "VENCIDO";
        if (dias <= 30) return "0-30";
        if (dias <= 90) return "31-90";
        return "MAS-90";
    }
}

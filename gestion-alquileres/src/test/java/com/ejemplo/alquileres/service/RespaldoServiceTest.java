package com.ejemplo.alquileres.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.ejemplo.alquileres.config.AppProperties;
import com.ejemplo.alquileres.exception.NegocioException;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

class RespaldoServiceTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-05-01T09:00:00Z"), ZoneOffset.UTC);

    @TempDir Path temp;
    private Path base;
    private SQLiteDataSource dataSource;
    private RespaldoService servicio;

    @BeforeEach
    void preparar() throws Exception {
        base = temp.resolve("alquileres.db");
        dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + base);
        crearEsquema(base, true);
        AppProperties propiedades = new AppProperties();
        propiedades.setDataDir(temp.toString());
        SesionActual sesion = new SesionActual();
        sesion.iniciar(
                Usuario.builder()
                        .id(1L)
                        .username("admin")
                        .rol(RolUsuario.ADMIN)
                        .activo(true)
                        .build());
        servicio =
                new RespaldoService(
                        dataSource,
                        propiedades,
                        new Permisos(sesion),
                        mock(AuditoriaService.class),
                        sesion,
                        mock(ConfiguracionService.class),
                        CLOCK);
    }

    @Test
    void creaRespaldoConVacuumIntoYLoValida() throws Exception {
        Path respaldo = servicio.crearRespaldo(temp);

        assertTrue(Files.isRegularFile(respaldo));
        assertTrue(respaldo.getFileName().toString().startsWith("alquileres-"));
        assertEquals(
                "SQLite format 3\u0000",
                new String(Files.readAllBytes(respaldo), 0, 16, StandardCharsets.US_ASCII));
        servicio.validar(respaldo);
    }

    @Test
    void elRespaldoAutomaticoRotaADiezArchivos() {
        for (int i = 0; i < 12; i++) servicio.respaldoAutomatico();

        assertEquals(10, servicio.listarAutomaticos().size());
    }

    @Test
    void restauraUnRespaldoValidoYConservaLaCopiaDeSeguridad() throws Exception {
        Path respaldo = servicio.crearRespaldo(temp);
        ejecutar("insert into secuencia (id) values (2)");
        assertEquals(2, contar("secuencia"));

        RespaldoService.Restauracion resultado = servicio.restaurar(respaldo);

        assertEquals(1, contar("secuencia"));
        assertTrue(Files.isRegularFile(resultado.respaldoSeguridad()));
        assertEquals(base.toAbsolutePath(), resultado.destino());
    }

    @Test
    void rechazaArchivoQueNoEsSqlite() throws Exception {
        Path malo = temp.resolve("malo.db");
        Files.writeString(malo, "esto no es sqlite");

        assertThrows(NegocioException.class, () -> servicio.validar(malo));
        assertThrows(NegocioException.class, () -> servicio.restaurar(malo));
    }

    @Test
    void rechazaSqliteSinTablasEsperadas() throws Exception {
        Path incompleto = temp.resolve("incompleto.db");
        crearEsquema(incompleto, false);

        NegocioException error =
                assertThrows(NegocioException.class, () -> servicio.validar(incompleto));
        assertTrue(error.getMessage().contains("tablas esperadas"));
    }

    private void crearEsquema(Path archivo, boolean completo) throws Exception {
        try (Connection conexion = java.sql.DriverManager.getConnection("jdbc:sqlite:" + archivo);
                Statement s = conexion.createStatement()) {
            if (completo) {
                for (String tabla :
                        new String[] {
                            "inmueble", "persona", "contrato", "cuota", "pago", "secuencia"
                        }) {
                    s.execute("create table " + tabla + " (id integer primary key)");
                }
                s.execute("insert into secuencia (id) values (1)");
                s.execute(
                        "create table auditoria_evento (id integer primary key autoincrement, fecha"
                            + " timestamp, usuario varchar(60), accion varchar(40), entidad"
                            + " varchar(60), entidad_id integer, detalle varchar(500))");
            } else {
                s.execute("create table secuencia (id integer primary key)");
            }
        }
    }

    private void ejecutar(String sql) throws Exception {
        try (Connection conexion = java.sql.DriverManager.getConnection("jdbc:sqlite:" + base);
                Statement s = conexion.createStatement()) {
            s.execute(sql);
        }
    }

    private int contar(String tabla) throws Exception {
        try (Connection conexion = java.sql.DriverManager.getConnection("jdbc:sqlite:" + base);
                Statement s = conexion.createStatement();
                ResultSet r = s.executeQuery("select count(*) from " + tabla)) {
            r.next();
            return r.getInt(1);
        }
    }
}

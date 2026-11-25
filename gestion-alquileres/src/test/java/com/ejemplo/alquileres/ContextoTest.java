package com.ejemplo.alquileres;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

@SpringBootTest(properties = "app.seeder.enabled=false")
class ContextoTest {
    @Autowired private DataSource dataSource;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        Path directorio = Path.of("target/test-data");
        try {
            Files.createDirectories(directorio);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
        registro.add("app.data-dir", () -> directorio.toString());
    }

    @Test
    void elContextoCargaYSQLiteActivaLlavesForaneas() throws Exception {
        try (Connection conexion = dataSource.getConnection();
                Statement sentencia = conexion.createStatement();
                var resultado = sentencia.executeQuery("PRAGMA foreign_keys")) {
            resultado.next();
            assertEquals(1, resultado.getInt(1));
        }
    }
}

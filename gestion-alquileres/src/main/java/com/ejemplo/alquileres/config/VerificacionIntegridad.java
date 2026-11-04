package com.ejemplo.alquileres.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Ejecuta PRAGMA integrity_check al arrancar y deja el resultado disponible para la UI. */
@Component
@RequiredArgsConstructor
public class VerificacionIntegridad implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(VerificacionIntegridad.class);
    private final DataSource dataSource;
    private volatile boolean correcta = true;
    private volatile String detalle = "ok";

    @Override public void run(ApplicationArguments args) { verificar(); }

    public void verificar() {
        try (Connection conexion = dataSource.getConnection();
             Statement sentencia = conexion.createStatement();
             ResultSet filas = sentencia.executeQuery("pragma integrity_check")) {
            if (filas.next()) {
                detalle = filas.getString(1);
                correcta = "ok".equalsIgnoreCase(detalle);
            }
            if (!correcta) log.warn("La verificación de integridad de SQLite reportó: {}", detalle);
        } catch (Exception e) {
            correcta = false;
            detalle = "No se pudo ejecutar PRAGMA integrity_check.";
            log.warn("No se pudo verificar la integridad de la base de datos.", e);
        }
    }

    public boolean correcta() { return correcta; }
    public String detalle() { return detalle; }
}

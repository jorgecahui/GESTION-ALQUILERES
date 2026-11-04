package com.ejemplo.alquileres.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Crea el directorio SQLite antes de que Hikari abra la conexión. */
public class DirectorioDatosInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override public void initialize(ConfigurableApplicationContext context) {
        try {
            String ruta = context.getEnvironment().getProperty("app.data-dir");
            if (ruta != null) {
                Files.createDirectories(Path.of(ruta));
                Files.createDirectories(Path.of(ruta, "logs"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo preparar el directorio de datos.", e);
        }
    }
}

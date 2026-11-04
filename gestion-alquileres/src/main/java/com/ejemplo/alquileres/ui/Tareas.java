package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.exception.NegocioException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import jakarta.annotation.PreDestroy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Ejecuta trabajo de datos de forma secuencial porque SQLite usa un pool Hikari
 * de una única conexión; las respuestas siempre regresan al hilo JavaFX.
 */
@Component
public class Tareas {
    private final ManejadorErrores manejador;
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(r -> {
        Thread hilo = new Thread(r, "alquileres-datos"); hilo.setDaemon(true); return hilo;
    });

    public Tareas(ManejadorErrores manejador) { this.manejador = manejador; }

    public <T> void ejecutar(Supplier<T> trabajo, Consumer<T> onExito, Consumer<Throwable> onError) {
        ejecutor.submit(() -> {
            try {
                T resultado = trabajo.get();
                Platform.runLater(() -> onExito.accept(resultado));
            } catch (Throwable error) {
                if (!(error instanceof NegocioException) && !(error instanceof OptimisticLockingFailureException)) {
                    manejador.registrar(error);
                }
                Throwable reportado = error instanceof OptimisticLockingFailureException
                        ? new NegocioException("El registro fue modificado por otra operación. Actualice e intente de nuevo.")
                        : error;
                Platform.runLater(() -> onError.accept(reportado));
            }
        });
    }

    @PreDestroy void cerrar() { ejecutor.shutdownNow(); }
}

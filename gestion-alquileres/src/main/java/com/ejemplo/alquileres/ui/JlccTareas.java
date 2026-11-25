package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.exception.JlccNegocioException;

import jakarta.annotation.PreDestroy;

import javafx.application.Platform;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class JlccTareas {
    private final JlccManejadorErrores jlccManejador;
    private final ExecutorService jlccEjecutor =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread jlccHilo = new Thread(r, "alquileres-datos");
                        jlccHilo.setDaemon(true);
                        return jlccHilo;
                    });

    public JlccTareas(JlccManejadorErrores jlccManejador) {
        this.jlccManejador = jlccManejador;
    }

    public <T> void jlccEjecutar(
            Supplier<T> jlccTrabajo, Consumer<T> jlccOnExito, Consumer<Throwable> jlccOnError) {
        jlccEjecutor.submit(
                () -> {
                    try {
                        T jlccResultado = jlccTrabajo.get();
                        Platform.runLater(() -> jlccOnExito.accept(jlccResultado));
                    } catch (Throwable jlccError) {
                        if (!(jlccError instanceof JlccNegocioException)
                                && !(jlccError instanceof OptimisticLockingFailureException)) {
                            jlccManejador.jlccRegistrar(jlccError);
                        }
                        Throwable jlccReportado =
                                jlccError instanceof OptimisticLockingFailureException
                                        ? new JlccNegocioException(
                                                "El registro fue modificado por otra operación."
                                                    + " Actualice e intente de nuevo.")
                                        : jlccError;
                        Platform.runLater(() -> jlccOnError.accept(jlccReportado));
                    }
                });
    }

    @PreDestroy
    void cerrar() {
        jlccEjecutor.shutdownNow();
    }
}

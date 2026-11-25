package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.exception.NegocioException;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

@Component
public class ManejadorErrores {
    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    public void instalarGlobal() {
        Thread.setDefaultUncaughtExceptionHandler(
                (hilo, error) -> {
                    registrar(error);
                    try {
                        Platform.runLater(() -> mostrar(error));
                    } catch (IllegalStateException ignorada) {
                    }
                });
    }

    public void registrar(Throwable error) {
        if (error == null) return;
        log.error("Error no controlado", error);
    }

    public String mensaje(Throwable error) {
        if (error == null) return "Ocurrió un error inesperado.";
        Throwable causa = error.getCause() == null ? error : error.getCause();
        if (causa instanceof OptimisticLockingFailureException) {
            return "El registro fue modificado por otra operación. Actualice e intente de nuevo.";
        }
        if (causa instanceof NegocioException negocio) return negocio.getMessage();
        if (causa.getMessage() != null
                && !causa.getMessage().isBlank()
                && !causa.getClass().getName().startsWith("java.")) {
            return causa.getMessage();
        }
        return "Ocurrió un error inesperado. Puedes copiar los detalles para reportarlo.";
    }

    public void mostrar(Throwable error) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> mostrar(error));
            return;
        }
        String detalle = traza(error);
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error inesperado");
        alerta.setHeaderText("No se pudo completar la operación");
        alerta.setContentText(mensaje(error));
        ButtonType copiar = new ButtonType("Copiar detalles", ButtonBar.ButtonData.OTHER);
        alerta.getButtonTypes().setAll(copiar, ButtonType.CLOSE);
        TextArea area = new TextArea(detalle);
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefRowCount(10);
        alerta.getDialogPane().setExpandableContent(area);
        Optional<ButtonType> resultado = alerta.showAndWait();
        if (resultado.isPresent() && resultado.get() == copiar) {
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString(detalle);
            Clipboard.getSystemClipboard().setContent(contenido);
        }
    }

    private String traza(Throwable error) {
        if (error == null) return "";
        StringWriter texto = new StringWriter();
        error.printStackTrace(new PrintWriter(texto));
        return texto.toString();
    }
}

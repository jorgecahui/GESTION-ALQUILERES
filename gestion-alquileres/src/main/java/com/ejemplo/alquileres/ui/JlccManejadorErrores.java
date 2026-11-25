package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.exception.JlccNegocioException;

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
public class JlccManejadorErrores {
    private static final Logger JLCC_log = LoggerFactory.getLogger(JlccManejadorErrores.class);

    public void jlccInstalarGlobal() {
        Thread.setDefaultUncaughtExceptionHandler(
                (hilo, error) -> {
                    jlccRegistrar(error);
                    try {
                        Platform.runLater(() -> jlccMostrar(error));
                    } catch (IllegalStateException jlccIgnorada) {
                    }
                });
    }

    public void jlccRegistrar(Throwable jlccError) {
        if (jlccError == null) return;
        JLCC_log.error("Error no controlado", jlccError);
    }

    public String jlccMensaje(Throwable jlccError) {
        if (jlccError == null) return "Ocurrió un error inesperado.";
        Throwable jlccCausa = jlccError.getCause() == null ? jlccError : jlccError.getCause();
        if (jlccCausa instanceof OptimisticLockingFailureException) {
            return "El registro fue modificado por otra operación. Actualice e intente de nuevo.";
        }
        if (jlccCausa instanceof JlccNegocioException jlccNegocio) return jlccNegocio.getMessage();
        if (jlccCausa.getMessage() != null
                && !jlccCausa.getMessage().isBlank()
                && !jlccCausa.getClass().getName().startsWith("java.")) {
            return jlccCausa.getMessage();
        }
        return "Ocurrió un error inesperado. Puedes copiar los detalles para reportarlo.";
    }

    public void jlccMostrar(Throwable jlccError) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> jlccMostrar(jlccError));
            return;
        }
        String jlccDetalle = jlccTraza(jlccError);
        Alert jlccAlerta = new Alert(Alert.AlertType.ERROR);
        jlccAlerta.setTitle("Error inesperado");
        jlccAlerta.setHeaderText("No se pudo completar la operación");
        jlccAlerta.setContentText(jlccMensaje(jlccError));
        ButtonType jlccCopiar = new ButtonType("Copiar detalles", ButtonBar.ButtonData.OTHER);
        jlccAlerta.getButtonTypes().setAll(jlccCopiar, ButtonType.CLOSE);
        TextArea jlccArea = new TextArea(jlccDetalle);
        jlccArea.setEditable(false);
        jlccArea.setWrapText(false);
        jlccArea.setPrefRowCount(10);
        jlccAlerta.getDialogPane().setExpandableContent(jlccArea);
        Optional<ButtonType> jlccResultado = jlccAlerta.showAndWait();
        if (jlccResultado.isPresent() && jlccResultado.get() == jlccCopiar) {
            ClipboardContent jlccContenido = new ClipboardContent();
            jlccContenido.putString(jlccDetalle);
            Clipboard.getSystemClipboard().setContent(jlccContenido);
        }
    }

    private String jlccTraza(Throwable jlccError) {
        if (jlccError == null) return "";
        StringWriter jlccTexto = new StringWriter();
        jlccError.printStackTrace(new PrintWriter(jlccTexto));
        return jlccTexto.toString();
    }
}

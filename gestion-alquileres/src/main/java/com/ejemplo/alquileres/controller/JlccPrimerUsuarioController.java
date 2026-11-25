package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.service.JlccAutenticacionService;
import com.ejemplo.alquileres.ui.JlccAplicacionUi;
import com.ejemplo.alquileres.ui.JlccTareas;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.springframework.stereotype.Component;

@Component
public class JlccPrimerUsuarioController {
    private final JlccAutenticacionService jlccAutenticacion;
    private final JlccAplicacionUi jlccAplicacion;
    private final JlccTareas jlccTareas;

    @FXML private TextField username, nombre;
    @FXML private PasswordField password, confirmacion;
    @FXML private Label mensaje;
    @FXML private Button crear;

    public JlccPrimerUsuarioController(
            JlccAutenticacionService jlccAutenticacion, JlccAplicacionUi jlccAplicacion, JlccTareas jlccTareas) {
        this.jlccAutenticacion = jlccAutenticacion;
        this.jlccAplicacion = jlccAplicacion;
        this.jlccTareas = jlccTareas;
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> username.requestFocus());
    }

    @FXML
    private void jlccOnCrear() {
        jlccHabilitar(false);
        String jlccUsuarioTexto = username.getText();
        String jlccNombreTexto = nombre.getText();
        String jlccPasswordTexto = password.getText();
        String jlccConfirmacionTexto = confirmacion.getText();
        jlccTareas.jlccEjecutar(
                () ->
                        jlccAutenticacion.jlccCrearPrimerAdmin(
                                jlccUsuarioTexto, jlccNombreTexto, jlccPasswordTexto, jlccConfirmacionTexto),
                admin -> {
                    mensaje.setText("Administrador creado. Ingresa con tus credenciales.");
                    jlccAplicacion.jlccMostrarLogin();
                },
                error -> {
                    jlccHabilitar(true);
                    mensaje.setText(jlccMensaje(error));
                });
    }

    private void jlccHabilitar(boolean jlccValor) {
        crear.setDisable(!jlccValor);
        username.setDisable(!jlccValor);
        nombre.setDisable(!jlccValor);
        password.setDisable(!jlccValor);
        confirmacion.setDisable(!jlccValor);
    }

    private static String jlccMensaje(Throwable jlccError) {
        Throwable jlccCausa = jlccError.getCause() == null ? jlccError : jlccError.getCause();
        return jlccCausa.getMessage() == null
                ? "No se pudo crear el administrador."
                : jlccCausa.getMessage();
    }
}

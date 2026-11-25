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
public class JlccLoginController {
    private final JlccAutenticacionService jlccAutenticacion;
    private final JlccAplicacionUi jlccAplicacion;
    private final JlccTareas jlccTareas;

    @FXML private TextField usuario;
    @FXML private PasswordField password;
    @FXML private Label mensaje;
    @FXML private Button ingresar;

    public JlccLoginController(
            JlccAutenticacionService jlccAutenticacion, JlccAplicacionUi jlccAplicacion, JlccTareas jlccTareas) {
        this.jlccAutenticacion = jlccAutenticacion;
        this.jlccAplicacion = jlccAplicacion;
        this.jlccTareas = jlccTareas;
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> usuario.requestFocus());
    }

    @FXML
    private void jlccOnIngresar() {
        if (usuario.getText() == null
                || usuario.getText().isBlank()
                || password.getText() == null
                || password.getText().isBlank()) {
            jlccMostrar("Ingresa tu usuario y contraseña.");
            return;
        }
        jlccHabilitar(false);
        String jlccUsuarioTexto = usuario.getText();
        String jlccPasswordTexto = password.getText();
        jlccTareas.jlccEjecutar(
                () -> jlccAutenticacion.jlccLogin(jlccUsuarioTexto, jlccPasswordTexto),
                iniciado -> jlccAplicacion.jlccMostrarPrincipal(),
                error -> {
                    jlccHabilitar(true);
                    password.clear();
                    jlccMostrar(jlccMensaje(error));
                });
    }

    private void jlccMostrar(String jlccTexto) {
        mensaje.setText(
                jlccTexto == null || jlccTexto.isBlank() ? "Usuario o contraseña incorrectos." : jlccTexto);
    }

    private void jlccHabilitar(boolean jlccValor) {
        ingresar.setDisable(!jlccValor);
        usuario.setDisable(!jlccValor);
        password.setDisable(!jlccValor);
    }

    private static String jlccMensaje(Throwable jlccError) {
        Throwable jlccCausa = jlccError.getCause() == null ? jlccError : jlccError.getCause();
        return jlccCausa.getMessage() == null ? "No se pudo iniciar sesión." : jlccCausa.getMessage();
    }
}

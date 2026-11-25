package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.service.AutenticacionService;
import com.ejemplo.alquileres.ui.AplicacionUi;
import com.ejemplo.alquileres.ui.Tareas;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.springframework.stereotype.Component;

@Component
public class LoginController {
    private final AutenticacionService autenticacion;
    private final AplicacionUi aplicacion;
    private final Tareas tareas;

    @FXML private TextField usuario;
    @FXML private PasswordField password;
    @FXML private Label mensaje;
    @FXML private Button ingresar;

    public LoginController(
            AutenticacionService autenticacion, AplicacionUi aplicacion, Tareas tareas) {
        this.autenticacion = autenticacion;
        this.aplicacion = aplicacion;
        this.tareas = tareas;
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> usuario.requestFocus());
    }

    @FXML
    private void onIngresar() {
        if (usuario.getText() == null
                || usuario.getText().isBlank()
                || password.getText() == null
                || password.getText().isBlank()) {
            mostrar("Ingresa tu usuario y contraseña.");
            return;
        }
        habilitar(false);
        String usuarioTexto = usuario.getText();
        String passwordTexto = password.getText();
        tareas.ejecutar(
                () -> autenticacion.login(usuarioTexto, passwordTexto),
                iniciado -> aplicacion.mostrarPrincipal(),
                error -> {
                    habilitar(true);
                    password.clear();
                    mostrar(mensaje(error));
                });
    }

    private void mostrar(String texto) {
        mensaje.setText(
                texto == null || texto.isBlank() ? "Usuario o contraseña incorrectos." : texto);
    }

    private void habilitar(boolean valor) {
        ingresar.setDisable(!valor);
        usuario.setDisable(!valor);
        password.setDisable(!valor);
    }

    private static String mensaje(Throwable error) {
        Throwable causa = error.getCause() == null ? error : error.getCause();
        return causa.getMessage() == null ? "No se pudo iniciar sesión." : causa.getMessage();
    }
}

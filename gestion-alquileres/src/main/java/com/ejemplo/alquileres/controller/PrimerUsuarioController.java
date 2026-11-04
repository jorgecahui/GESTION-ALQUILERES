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
public class PrimerUsuarioController {
    private final AutenticacionService autenticacion;
    private final AplicacionUi aplicacion;
    private final Tareas tareas;

    @FXML private TextField username, nombre;
    @FXML private PasswordField password, confirmacion;
    @FXML private Label mensaje;
    @FXML private Button crear;

    public PrimerUsuarioController(AutenticacionService autenticacion, AplicacionUi aplicacion, Tareas tareas) {
        this.autenticacion = autenticacion;
        this.aplicacion = aplicacion;
        this.tareas = tareas;
    }

    @FXML public void initialize() {
        Platform.runLater(() -> username.requestFocus());
    }

    @FXML private void onCrear() {
        habilitar(false);
        tareas.ejecutar(() -> autenticacion.crearPrimerAdmin(username.getText(), nombre.getText(),
                        password.getText(), confirmacion.getText()),
                admin -> {
                    mensaje.setText("Administrador creado. Ingresa con tus credenciales.");
                    aplicacion.mostrarLogin();
                },
                error -> { habilitar(true); mensaje.setText(mensaje(error)); });
    }

    private void habilitar(boolean valor) {
        crear.setDisable(!valor);
        username.setDisable(!valor);
        nombre.setDisable(!valor);
        password.setDisable(!valor);
        confirmacion.setDisable(!valor);
    }

    private static String mensaje(Throwable error) {
        Throwable causa = error.getCause() == null ? error : error.getCause();
        return causa.getMessage() == null ? "No se pudo crear el administrador." : causa.getMessage();
    }
}

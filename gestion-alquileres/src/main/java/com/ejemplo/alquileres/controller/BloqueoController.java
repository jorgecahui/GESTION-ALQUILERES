package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.service.AutenticacionService;
import com.ejemplo.alquileres.ui.AplicacionUi;
import com.ejemplo.alquileres.ui.Tareas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class BloqueoController {
    private final AutenticacionService autenticacion;
    private final Tareas tareas;
    private Stage ventana;
    private AplicacionUi aplicacion;

    @FXML private Label usuario, rol, mensaje;
    @FXML private PasswordField password;

    public BloqueoController(AutenticacionService autenticacion, Tareas tareas) {
        this.autenticacion = autenticacion;
        this.tareas = tareas;
    }

    @FXML public void initialize() {
        usuario.setText(autenticacion.nombreUsuarioActual());
        rol.setText(autenticacion.rolActual());
    }

    public void configurar(Stage ventana, AplicacionUi aplicacion) {
        this.ventana = ventana;
        this.aplicacion = aplicacion;
    }

    public void enfocar() {
        Platform.runLater(() -> password.requestFocus());
    }

    @FXML private void onDesbloquear() {
        if (password.getText() == null || password.getText().isBlank()) {
            mensaje.setText("Ingresa tu contraseña.");
            return;
        }
        tareas.ejecutar(() -> autenticacion.verificarPassword(password.getText()),
                correcta -> {
                    if (correcta) {
                        ventana.close();
                        aplicacion.desbloquear();
                    } else {
                        mensaje.setText("Contraseña incorrecta.");
                        password.clear();
                        password.requestFocus();
                    }
                },
                error -> mensaje.setText("No se pudo verificar la contraseña."));
    }

    @FXML private void onCerrarSesion() {
        ventana.close();
        aplicacion.cerrarSesion();
    }
}

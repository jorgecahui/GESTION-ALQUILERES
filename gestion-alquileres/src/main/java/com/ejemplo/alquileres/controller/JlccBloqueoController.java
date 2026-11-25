package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.service.JlccAutenticacionService;
import com.ejemplo.alquileres.ui.JlccAplicacionUi;
import com.ejemplo.alquileres.ui.JlccTareas;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import org.springframework.stereotype.Component;

@Component
public class JlccBloqueoController {
    private final JlccAutenticacionService jlccAutenticacion;
    private final JlccTareas jlccTareas;
    private Stage jlccVentana;
    private JlccAplicacionUi jlccAplicacion;

    @FXML private Label usuario, rol, mensaje;
    @FXML private PasswordField password;

    public JlccBloqueoController(JlccAutenticacionService jlccAutenticacion, JlccTareas jlccTareas) {
        this.jlccAutenticacion = jlccAutenticacion;
        this.jlccTareas = jlccTareas;
    }

    @FXML
    public void initialize() {
        usuario.setText(jlccAutenticacion.jlccNombreUsuarioActual());
        rol.setText(jlccAutenticacion.jlccRolActual());
    }

    public void jlccConfigurar(Stage jlccVentana, JlccAplicacionUi jlccAplicacion) {
        this.jlccVentana = jlccVentana;
        this.jlccAplicacion = jlccAplicacion;
    }

    public void jlccEnfocar() {
        Platform.runLater(() -> password.requestFocus());
    }

    @FXML
    private void jlccOnDesbloquear() {
        if (password.getText() == null || password.getText().isBlank()) {
            mensaje.setText("Ingresa tu contraseña.");
            return;
        }
        String jlccPasswordTexto = password.getText();
        jlccTareas.jlccEjecutar(
                () -> jlccAutenticacion.jlccVerificarPassword(jlccPasswordTexto),
                correcta -> {
                    if (correcta) {
                        jlccVentana.close();
                        jlccAplicacion.jlccDesbloquear();
                    } else {
                        mensaje.setText("Contraseña incorrecta.");
                        password.clear();
                        password.requestFocus();
                    }
                },
                error -> mensaje.setText("No se pudo verificar la contraseña."));
    }

    @FXML
    private void jlccOnCerrarSesion() {
        jlccVentana.close();
        jlccAplicacion.jlccCerrarSesion();
    }
}

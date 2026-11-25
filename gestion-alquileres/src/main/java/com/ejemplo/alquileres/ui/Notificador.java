package com.ejemplo.alquileres.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import org.springframework.stereotype.Component;

@Component
public class Notificador {
    public void error(String mensaje) {
        mostrar(Alert.AlertType.ERROR, mensaje);
    }

    public void info(String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, mensaje);
    }

    private void mostrar(Alert.AlertType tipo, String mensaje) {
        Platform.runLater(
                () -> {
                    Alert alerta = new Alert(tipo, mensaje);
                    alerta.setHeaderText(null);
                    alerta.show();
                });
    }
}

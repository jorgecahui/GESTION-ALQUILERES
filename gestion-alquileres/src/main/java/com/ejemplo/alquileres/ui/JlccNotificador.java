package com.ejemplo.alquileres.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import org.springframework.stereotype.Component;

@Component
public class JlccNotificador {
    public void jlccError(String jlccMensaje) {
        jlccMostrar(Alert.AlertType.ERROR, jlccMensaje);
    }

    public void jlccInfo(String jlccMensaje) {
        jlccMostrar(Alert.AlertType.INFORMATION, jlccMensaje);
    }

    private void jlccMostrar(Alert.AlertType jlccTipo, String jlccMensaje) {
        Platform.runLater(
                () -> {
                    Alert jlccAlerta = new Alert(jlccTipo, jlccMensaje);
                    jlccAlerta.setHeaderText(null);
                    jlccAlerta.show();
                });
    }
}

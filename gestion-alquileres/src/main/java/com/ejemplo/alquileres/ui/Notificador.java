package com.ejemplo.alquileres.ui;
import javafx.scene.control.Alert;
import javafx.application.Platform;
import org.springframework.stereotype.Component;
/** Notificaciones sencillas que nunca exponen trazas al usuario. */
@Component public class Notificador {
    public void error(String mensaje) { mostrar(Alert.AlertType.ERROR, mensaje); }
    public void info(String mensaje) { mostrar(Alert.AlertType.INFORMATION, mensaje); }
    private void mostrar(Alert.AlertType tipo, String mensaje) { Platform.runLater(() -> { Alert alerta = new Alert(tipo, mensaje); alerta.setHeaderText(null); alerta.show(); }); }
}

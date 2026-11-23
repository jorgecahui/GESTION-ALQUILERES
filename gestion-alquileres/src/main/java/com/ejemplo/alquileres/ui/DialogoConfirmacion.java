package com.ejemplo.alquileres.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Confirmación reutilizable con el estilo visual de la aplicación. */
public final class DialogoConfirmacion {
    private DialogoConfirmacion() { }

    /**
     * Muestra una confirmación modal. Devuelve {@code true} solo si se pulsa el botón
     * de acción; Cancelar es el botón predeterminado.
     */
    public static boolean mostrar(Window propietario, String titulo, String mensaje, String accion) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (propietario != null) dialogo.initOwner(propietario);
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(null);

        DialogPane panel = dialogo.getDialogPane();
        panel.getStyleClass().add("dialogo");
        var hoja = DialogoConfirmacion.class.getResource("/css/app.css");
        if (hoja != null) panel.getStylesheets().add(hoja.toExternalForm());

        ButtonType confirmar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        panel.getButtonTypes().addAll(cancelar, confirmar);

        Label encabezado = new Label(titulo);
        encabezado.getStyleClass().add("dialogo-titulo");
        Label detalle = new Label(mensaje);
        detalle.setWrapText(true);
        detalle.getStyleClass().add("dialogo-mensaje");
        VBox contenido = new VBox(10, encabezado, detalle);
        contenido.setPrefWidth(430);
        panel.setContent(contenido);

        Button botonCancelar = (Button) panel.lookupButton(cancelar);
        Button botonConfirmar = (Button) panel.lookupButton(confirmar);
        botonCancelar.setDefaultButton(true);
        botonCancelar.setCancelButton(true);
        botonConfirmar.setDefaultButton(false);
        dialogo.setOnShown(evento -> {
            botonCancelar.setDefaultButton(true);
            botonConfirmar.setDefaultButton(false);
            botonCancelar.requestFocus();
        });

        return dialogo.showAndWait().orElse(cancelar) == confirmar;
    }
}

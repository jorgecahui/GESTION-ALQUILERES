package com.ejemplo.alquileres.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class JlccDialogoConfirmacion {
    private JlccDialogoConfirmacion() {}

    public static boolean jlccMostrar(
            Window jlccPropietario, String jlccTitulo, String jlccMensaje, String jlccAccion) {
        Dialog<ButtonType> jlccDialogo = new Dialog<>();
        if (jlccPropietario != null) jlccDialogo.initOwner(jlccPropietario);
        jlccDialogo.setTitle(jlccTitulo);
        jlccDialogo.setHeaderText(null);

        DialogPane jlccPanel = jlccDialogo.getDialogPane();
        jlccPanel.getStyleClass().add("dialogo");
        var jlccHoja = JlccDialogoConfirmacion.class.getResource("/css/app.css");
        if (jlccHoja != null) jlccPanel.getStylesheets().add(jlccHoja.toExternalForm());

        ButtonType jlccConfirmar = new ButtonType(jlccAccion, ButtonBar.ButtonData.OK_DONE);
        ButtonType jlccCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        jlccPanel.getButtonTypes().addAll(jlccCancelar, jlccConfirmar);

        Label jlccEncabezado = new Label(jlccTitulo);
        jlccEncabezado.getStyleClass().add("dialogo-titulo");
        Label jlccDetalle = new Label(jlccMensaje);
        jlccDetalle.setWrapText(true);
        jlccDetalle.getStyleClass().add("dialogo-mensaje");
        VBox jlccContenido = new VBox(10, jlccEncabezado, jlccDetalle);
        jlccContenido.setPrefWidth(430);
        jlccPanel.setContent(jlccContenido);

        Button jlccBotonCancelar = (Button) jlccPanel.lookupButton(jlccCancelar);
        Button jlccBotonConfirmar = (Button) jlccPanel.lookupButton(jlccConfirmar);
        jlccBotonCancelar.setDefaultButton(true);
        jlccBotonCancelar.setCancelButton(true);
        jlccBotonConfirmar.setDefaultButton(false);
        jlccDialogo.setOnShown(
                evento -> {
                    jlccBotonCancelar.setDefaultButton(true);
                    jlccBotonConfirmar.setDefaultButton(false);
                    jlccBotonCancelar.requestFocus();
                });

        return jlccDialogo.showAndWait().orElse(jlccCancelar) == jlccConfirmar;
    }
}

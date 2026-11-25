package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.model.CanalRecordatorio;
import com.ejemplo.alquileres.service.JlccRecordatorioService;
import com.ejemplo.alquileres.service.JlccTelefonoUtil;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

public final class JlccDialogoRecordatorio {
    private JlccDialogoRecordatorio() {}

    public static boolean jlccMostrar(
            Window jlccPropietario,
            JlccRecordatorioService jlccServicio,
            JlccTareas jlccTareas,
            JlccExportador jlccExportador,
            JlccNotificador jlccAviso,
            JlccRecordatorioService.Propuesta jlccPropuesta,
            Runnable jlccAlRegistrar) {
        if (jlccPropuesta == null) {
            jlccAviso.jlccError(
                    "No se pudo preparar el recordatorio: revisa el teléfono del arrendatario.");
            return false;
        }
        boolean jlccValido = jlccPropuesta.telefonoValido();
        Dialog<ButtonType> jlccDialogo = new Dialog<>();
        if (jlccPropietario != null) jlccDialogo.initOwner(jlccPropietario);
        jlccDialogo.setTitle("Recordatorio de cobro");
        jlccDialogo.setHeaderText("Vista previa para " + JlccTelefonoUtil.jlccLegible(jlccPropuesta.telefono()));
        DialogPane jlccPanel = jlccDialogo.getDialogPane();
        jlccPanel.getStyleClass().add("dialogo");
        ButtonType jlccAbrir = new ButtonType("Abrir WhatsApp", ButtonBar.ButtonData.OK_DONE);
        ButtonType jlccCopiar = new ButtonType("Copiar mensaje", ButtonBar.ButtonData.OTHER);
        ButtonType jlccCancelar = ButtonType.CANCEL;
        jlccPanel.getButtonTypes().addAll(jlccAbrir, jlccCopiar, jlccCancelar);
        TextArea jlccMensaje = new TextArea(jlccPropuesta.mensaje());
        jlccMensaje.setWrapText(true);
        jlccMensaje.setPrefRowCount(8);
        jlccMensaje.setPrefColumnCount(48);
        Label jlccAyuda =
                new Label(
                        "Puedes editar el mensaje antes de enviarlo. Quedará registrado en el"
                            + " historial.");
        jlccAyuda.getStyleClass().add("pagina-subtitulo");
        VBox jlccContenido = new VBox(8, new Label("Mensaje"), jlccMensaje, jlccAyuda);
        jlccPanel.setContent(jlccContenido);
        Button jlccBotonAbrir = (Button) jlccPanel.lookupButton(jlccAbrir);
        Button jlccBotonCopiar = (Button) jlccPanel.lookupButton(jlccCopiar);
        if (!jlccValido) {
            jlccBotonAbrir.setDisable(true);
            jlccBotonCopiar.setDisable(true);
            Tooltip.install(jlccBotonAbrir, new Tooltip("Teléfono no válido o ausente"));
            Tooltip.install(jlccBotonCopiar, new Tooltip("Teléfono no válido o ausente"));
        }
        jlccMensaje.textProperty()
                .addListener(
                        (observable, anterior, actual) -> {
                            boolean jlccVacio = actual == null || actual.isBlank();
                            if (jlccValido) {
                                jlccBotonAbrir.setDisable(jlccVacio);
                                jlccBotonCopiar.setDisable(jlccVacio);
                            }
                        });
        Optional<ButtonType> jlccResultado = jlccDialogo.showAndWait();
        if (jlccResultado.isEmpty() || jlccResultado.get() == jlccCancelar) return false;
        String jlccTexto = jlccMensaje.getText();
        CanalRecordatorio jlccCanal;
        if (jlccResultado.get() == jlccAbrir) {
            jlccExportador.jlccAbrirEnlace(jlccServicio.jlccUrlWhatsApp(jlccPropuesta.telefono(), jlccTexto));
            jlccCanal = CanalRecordatorio.WHATSAPP;
        } else {
            ClipboardContent jlccContenidoPortapapeles = new ClipboardContent();
            jlccContenidoPortapapeles.putString(jlccTexto);
            Clipboard.getSystemClipboard().setContent(jlccContenidoPortapapeles);
            jlccCanal = CanalRecordatorio.COPIADO;
        }
        jlccTareas.jlccEjecutar(
                () -> jlccServicio.jlccRegistrar(jlccPropuesta, jlccTexto, jlccCanal),
                registro -> {
                    jlccAviso.jlccInfo(
                            jlccCanal == CanalRecordatorio.WHATSAPP
                                    ? "WhatsApp abierto y recordatorio registrado."
                                    : "Mensaje copiado y recordatorio registrado.");
                    if (jlccAlRegistrar != null) jlccAlRegistrar.run();
                },
                error -> jlccAviso.jlccError(jlccMensajeError(error)));
        return true;
    }

    public static void jlccAsistente(
            Window jlccPropietario,
            JlccRecordatorioService jlccServicio,
            JlccTareas jlccTareas,
            JlccExportador jlccExportador,
            JlccNotificador jlccAviso,
            List<JlccRecordatorioService.Propuesta> jlccPropuestas,
            Runnable jlccAlRegistrar) {
        if (jlccPropuestas == null || jlccPropuestas.isEmpty()) {
            jlccAviso.jlccInfo("No hay cuotas pendientes de recordatorio en este momento.");
            return;
        }
        int jlccEnviados = 0;
        for (JlccRecordatorioService.Propuesta jlccPropuesta : jlccPropuestas) {
            if (!jlccMostrar(jlccPropietario, jlccServicio, jlccTareas, jlccExportador, jlccAviso, jlccPropuesta, jlccAlRegistrar))
                break;
            jlccEnviados++;
        }
        if (jlccEnviados > 0) jlccAviso.jlccInfo("Recordatorios procesados: " + jlccEnviados + ".");
    }

    public static void jlccContactoLibre(
            Window jlccPropietario,
            String jlccNombre,
            String jlccTelefono,
            String jlccMensaje,
            JlccExportador jlccExportador,
            JlccNotificador jlccAviso) {
        if (JlccTelefonoUtil.jlccNormalizarPeru(jlccTelefono) == null) {
            jlccAviso.jlccError("Teléfono no válido o ausente.");
            return;
        }
        Dialog<ButtonType> jlccDialogo = new Dialog<>();
        if (jlccPropietario != null) jlccDialogo.initOwner(jlccPropietario);
        jlccDialogo.setTitle("WhatsApp");
        jlccDialogo.setHeaderText("Contacto con " + (jlccNombre == null ? "" : jlccNombre));
        DialogPane jlccPanel = jlccDialogo.getDialogPane();
        jlccPanel.getStyleClass().add("dialogo");
        ButtonType jlccAbrir = new ButtonType("Abrir WhatsApp", ButtonBar.ButtonData.OK_DONE);
        ButtonType jlccCopiar = new ButtonType("Copiar mensaje", ButtonBar.ButtonData.OTHER);
        jlccPanel.getButtonTypes().addAll(jlccAbrir, jlccCopiar, ButtonType.CANCEL);
        TextArea jlccArea = new TextArea(jlccMensaje);
        jlccArea.setWrapText(true);
        jlccArea.setPrefRowCount(7);
        jlccArea.setPrefColumnCount(48);
        jlccPanel.setContent(new VBox(8, new Label("Mensaje libre"), jlccArea));
        Optional<ButtonType> jlccResultado = jlccDialogo.showAndWait();
        if (jlccResultado.isEmpty() || jlccResultado.get() == ButtonType.CANCEL) return;
        String jlccTexto = jlccArea.getText();
        if (jlccResultado.get() == jlccAbrir) {
            jlccExportador.jlccAbrirEnlace(
                    "https://wa.me/"
                            + JlccTelefonoUtil.jlccNormalizarPeru(jlccTelefono)
                            + "?text="
                            + java.net.URLEncoder.encode(
                                            jlccTexto, java.nio.charset.StandardCharsets.UTF_8)
                                    .replace("+", "%20"));
        } else {
            ClipboardContent jlccContenido = new ClipboardContent();
            jlccContenido.putString(jlccTexto);
            Clipboard.getSystemClipboard().setContent(jlccContenido);
            jlccAviso.jlccInfo("Mensaje copiado.");
        }
    }

    private static String jlccMensajeError(Throwable jlccError) {
        return jlccError.getCause() != null && jlccError.getCause().getMessage() != null
                ? jlccError.getCause().getMessage()
                : jlccError.getMessage();
    }
}

package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.model.CanalRecordatorio;
import com.ejemplo.alquileres.service.RecordatorioService;
import com.ejemplo.alquileres.service.TelefonoUtil;
import java.util.List;
import java.util.Optional;
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

/** Vista previa editable y acciones Copiar / Abrir WhatsApp para los recordatorios. */
public final class DialogoRecordatorio {
    private DialogoRecordatorio() { }

    public static boolean mostrar(Window propietario, RecordatorioService servicio, Tareas tareas,
                                  Exportador exportador, Notificador aviso,
                                  RecordatorioService.Propuesta propuesta, Runnable alRegistrar) {
        if (propuesta == null) {
            aviso.error("No se pudo preparar el recordatorio: revisa el teléfono del arrendatario.");
            return false;
        }
        boolean valido = propuesta.telefonoValido();
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (propietario != null) dialogo.initOwner(propietario);
        dialogo.setTitle("Recordatorio de cobro");
        dialogo.setHeaderText("Vista previa para " + TelefonoUtil.legible(propuesta.telefono()));
        DialogPane panel = dialogo.getDialogPane();
        panel.getStyleClass().add("dialogo");
        ButtonType abrir = new ButtonType("Abrir WhatsApp", ButtonBar.ButtonData.OK_DONE);
        ButtonType copiar = new ButtonType("Copiar mensaje", ButtonBar.ButtonData.OTHER);
        ButtonType cancelar = ButtonType.CANCEL;
        panel.getButtonTypes().addAll(abrir, copiar, cancelar);
        TextArea mensaje = new TextArea(propuesta.mensaje());
        mensaje.setWrapText(true);
        mensaje.setPrefRowCount(8);
        mensaje.setPrefColumnCount(48);
        Label ayuda = new Label("Puedes editar el mensaje antes de enviarlo. Quedará registrado en el historial.");
        ayuda.getStyleClass().add("pagina-subtitulo");
        VBox contenido = new VBox(8, new Label("Mensaje"), mensaje, ayuda);
        panel.setContent(contenido);
        Button botonAbrir = (Button) panel.lookupButton(abrir);
        Button botonCopiar = (Button) panel.lookupButton(copiar);
        if (!valido) {
            botonAbrir.setDisable(true);
            botonCopiar.setDisable(true);
            Tooltip.install(botonAbrir, new Tooltip("Teléfono no válido o ausente"));
            Tooltip.install(botonCopiar, new Tooltip("Teléfono no válido o ausente"));
        }
        mensaje.textProperty().addListener((observable, anterior, actual) -> {
            boolean vacio = actual == null || actual.isBlank();
            if (valido) {
                botonAbrir.setDisable(vacio);
                botonCopiar.setDisable(vacio);
            }
        });
        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isEmpty() || resultado.get() == cancelar) return false;
        String texto = mensaje.getText();
        CanalRecordatorio canal;
        if (resultado.get() == abrir) {
            exportador.abrirEnlace(servicio.urlWhatsApp(propuesta.telefono(), texto));
            canal = CanalRecordatorio.WHATSAPP;
        } else {
            ClipboardContent contenidoPortapapeles = new ClipboardContent();
            contenidoPortapapeles.putString(texto);
            Clipboard.getSystemClipboard().setContent(contenidoPortapapeles);
            canal = CanalRecordatorio.COPIADO;
        }
        tareas.ejecutar(() -> servicio.registrar(propuesta, texto, canal), registro -> {
            aviso.info(canal == CanalRecordatorio.WHATSAPP
                    ? "WhatsApp abierto y recordatorio registrado."
                    : "Mensaje copiado y recordatorio registrado.");
            if (alRegistrar != null) alRegistrar.run();
        }, error -> aviso.error(mensajeError(error)));
        return true;
    }

    /** Recorre las sugerencias una por una; Cancelar detiene el asistente. */
    public static void asistente(Window propietario, RecordatorioService servicio, Tareas tareas,
                                 Exportador exportador, Notificador aviso,
                                 List<RecordatorioService.Propuesta> propuestas, Runnable alRegistrar) {
        if (propuestas == null || propuestas.isEmpty()) {
            aviso.info("No hay cuotas pendientes de recordatorio en este momento.");
            return;
        }
        int enviados = 0;
        for (RecordatorioService.Propuesta propuesta : propuestas) {
            if (!mostrar(propietario, servicio, tareas, exportador, aviso, propuesta, alRegistrar)) break;
            enviados++;
        }
        if (enviados > 0) aviso.info("Recordatorios procesados: " + enviados + ".");
    }

    /** Contacto libre desde Personas: no genera historial porque no pertenece a un contrato. */
    public static void contactoLibre(Window propietario, String nombre, String telefono,
                                     String mensaje, Exportador exportador, Notificador aviso) {
        if (TelefonoUtil.normalizarPeru(telefono) == null) {
            aviso.error("Teléfono no válido o ausente.");
            return;
        }
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (propietario != null) dialogo.initOwner(propietario);
        dialogo.setTitle("WhatsApp");
        dialogo.setHeaderText("Contacto con " + (nombre == null ? "" : nombre));
        DialogPane panel = dialogo.getDialogPane();
        panel.getStyleClass().add("dialogo");
        ButtonType abrir = new ButtonType("Abrir WhatsApp", ButtonBar.ButtonData.OK_DONE);
        ButtonType copiar = new ButtonType("Copiar mensaje", ButtonBar.ButtonData.OTHER);
        panel.getButtonTypes().addAll(abrir, copiar, ButtonType.CANCEL);
        TextArea area = new TextArea(mensaje);
        area.setWrapText(true);
        area.setPrefRowCount(7);
        area.setPrefColumnCount(48);
        panel.setContent(new VBox(8, new Label("Mensaje libre"), area));
        Optional<ButtonType> resultado = dialogo.showAndWait();
        if (resultado.isEmpty() || resultado.get() == ButtonType.CANCEL) return;
        String texto = area.getText();
        if (resultado.get() == abrir) {
            exportador.abrirEnlace("https://wa.me/" + TelefonoUtil.normalizarPeru(telefono)
                    + "?text=" + java.net.URLEncoder.encode(texto, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
        } else {
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString(texto);
            Clipboard.getSystemClipboard().setContent(contenido);
            aviso.info("Mensaje copiado.");
        }
    }

    private static String mensajeError(Throwable error) {
        return error.getCause() != null && error.getCause().getMessage() != null
                ? error.getCause().getMessage() : error.getMessage();
    }
}

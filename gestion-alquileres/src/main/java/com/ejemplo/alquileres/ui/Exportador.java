package com.ejemplo.alquileres.ui;

import javafx.application.HostServices;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

@Component
public class Exportador {
    private HostServices hostServices;

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    public void abrirEnlace(String url) {
        if (hostServices != null) {
            hostServices.showDocument(url);
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo abrir el enlace.", e);
        }
    }

    public File elegirDestino(
            Window propietario, String nombre, String descripcion, String extension) {
        FileChooser selector = new FileChooser();
        selector.setInitialFileName(nombre);
        selector.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(descripcion, "*." + extension));
        return selector.showSaveDialog(propietario);
    }

    public void escribir(File destino, byte[] contenido) {
        try {
            Files.write(destino.toPath(), contenido);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo.", e);
        }
    }

    public void abrirPdf(byte[] contenido, String nombre) {
        try {
            File archivo = File.createTempFile(nombre.replaceAll("[^A-Za-z0-9._-]", "_"), ".pdf");
            Files.write(archivo.toPath(), contenido);
            Desktop.getDesktop().open(archivo);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo abrir el PDF.", e);
        }
    }
}

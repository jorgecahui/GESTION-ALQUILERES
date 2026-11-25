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
public class JlccExportador {
    private HostServices jlccHostServices;

    public void jlccSetHostServices(HostServices jlccHostServices) {
        this.jlccHostServices = jlccHostServices;
    }

    public HostServices jlccGetHostServices() {
        return jlccHostServices;
    }

    public void jlccAbrirEnlace(String jlccUrl) {
        if (jlccHostServices != null) {
            jlccHostServices.showDocument(jlccUrl);
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(jlccUrl));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo abrir el enlace.", e);
        }
    }

    public File jlccElegirDestino(
            Window jlccPropietario, String jlccNombre, String jlccDescripcion, String jlccExtension) {
        FileChooser jlccSelector = new FileChooser();
        jlccSelector.setInitialFileName(jlccNombre);
        jlccSelector.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(jlccDescripcion, "*." + jlccExtension));
        return jlccSelector.showSaveDialog(jlccPropietario);
    }

    public void jlccEscribir(File jlccDestino, byte[] jlccContenido) {
        try {
            Files.write(jlccDestino.toPath(), jlccContenido);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo.", e);
        }
    }

    public void jlccAbrirPdf(byte[] jlccContenido, String jlccNombre) {
        try {
            File jlccArchivo = File.createTempFile(jlccNombre.replaceAll("[^A-Za-z0-9._-]", "_"), ".pdf");
            Files.write(jlccArchivo.toPath(), jlccContenido);
            Desktop.getDesktop().open(jlccArchivo);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo abrir el PDF.", e);
        }
    }
}

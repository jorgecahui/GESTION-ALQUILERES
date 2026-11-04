package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.ui.Navegacion;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Component
public class PlaceholderController {
    @FXML private FontIcon icono;
    @FXML private Label titulo;
    @FXML private Label descripcion;
    @FXML private Label fase;
    public void mostrar(Navegacion.Entrada entrada) {
        icono.setIconCode(entrada.icono());
        titulo.setText(entrada.titulo());
        descripcion.setText(entrada.descripcion());
        fase.setText("Disponible en la Fase " + entrada.fase());
    }
}

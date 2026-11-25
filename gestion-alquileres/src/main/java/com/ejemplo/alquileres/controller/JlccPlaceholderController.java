package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.ui.JlccNavegacion;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Component
public class JlccPlaceholderController {
    @FXML private FontIcon icono;
    @FXML private Label titulo;
    @FXML private Label descripcion;
    @FXML private Label fase;

    public void jlccMostrar(JlccNavegacion.Entrada jlccEntrada) {
        icono.setIconCode(jlccEntrada.icono());
        titulo.setText(jlccEntrada.titulo());
        descripcion.setText(jlccEntrada.descripcion());
        fase.setText("Disponible en la Fase " + jlccEntrada.fase());
    }
}

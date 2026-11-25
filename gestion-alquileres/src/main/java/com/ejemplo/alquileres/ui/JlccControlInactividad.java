package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.service.JlccConfiguracionService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

public final class JlccControlInactividad {
    private final Scene jlccEscena;
    private final JlccConfiguracionService jlccConfiguracion;
    private final Runnable jlccAlBloquear;
    private Timeline jlccReloj;
    private long jlccUltimaActividad = System.nanoTime();
    private boolean jlccBloqueando;

    public JlccControlInactividad(
            Scene jlccEscena, JlccConfiguracionService jlccConfiguracion, Runnable jlccAlBloquear) {
        this.jlccEscena = jlccEscena;
        this.jlccConfiguracion = jlccConfiguracion;
        this.jlccAlBloquear = jlccAlBloquear;
    }

    public void jlccInstalar() {
        jlccEscena.addEventFilter(MouseEvent.ANY, evento -> jlccReiniciar());
        jlccEscena.addEventFilter(KeyEvent.ANY, evento -> jlccReiniciar());
        jlccReloj = new Timeline(new KeyFrame(Duration.seconds(10), evento -> jlccComprobar()));
        jlccReloj.setCycleCount(Timeline.INDEFINITE);
        jlccReloj.play();
    }

    public void jlccReiniciar() {
        jlccUltimaActividad = System.nanoTime();
        jlccBloqueando = false;
    }

    public void jlccDetener() {
        if (jlccReloj != null) jlccReloj.stop();
    }

    private void jlccComprobar() {
        int jlccMinutos = jlccConfiguracion.jlccInactividadMinutos();
        if (jlccMinutos <= 0 || jlccBloqueando) return;
        long jlccTranscurrido = System.nanoTime() - jlccUltimaActividad;
        if (jlccTranscurrido >= jlccMinutos * 60L * 1_000_000_000L) {
            jlccBloqueando = true;
            jlccAlBloquear.run();
        }
    }
}

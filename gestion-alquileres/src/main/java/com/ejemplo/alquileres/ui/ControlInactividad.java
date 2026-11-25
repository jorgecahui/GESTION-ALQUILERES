package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.service.ConfiguracionService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

public final class ControlInactividad {
    private final Scene escena;
    private final ConfiguracionService configuracion;
    private final Runnable alBloquear;
    private Timeline reloj;
    private long ultimaActividad = System.nanoTime();
    private boolean bloqueando;

    public ControlInactividad(
            Scene escena, ConfiguracionService configuracion, Runnable alBloquear) {
        this.escena = escena;
        this.configuracion = configuracion;
        this.alBloquear = alBloquear;
    }

    public void instalar() {
        escena.addEventFilter(MouseEvent.ANY, evento -> reiniciar());
        escena.addEventFilter(KeyEvent.ANY, evento -> reiniciar());
        reloj = new Timeline(new KeyFrame(Duration.seconds(10), evento -> comprobar()));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }

    public void reiniciar() {
        ultimaActividad = System.nanoTime();
        bloqueando = false;
    }

    public void detener() {
        if (reloj != null) reloj.stop();
    }

    private void comprobar() {
        int minutos = configuracion.inactividadMinutos();
        if (minutos <= 0 || bloqueando) return;
        long transcurrido = System.nanoTime() - ultimaActividad;
        if (transcurrido >= minutos * 60L * 1_000_000_000L) {
            bloqueando = true;
            alBloquear.run();
        }
    }
}

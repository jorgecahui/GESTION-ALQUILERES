package com.ejemplo.alquileres.ui;

import org.springframework.stereotype.Component;

import java.util.prefs.Preferences;

@Component
public class JlccPreferenciaTema {
    private final Preferences jlccPrefs = Preferences.userNodeForPackage(JlccPreferenciaTema.class);

    public boolean jlccOscuro() {
        return jlccPrefs.getBoolean("tema.oscuro", false);
    }

    public void jlccOscuro(boolean jlccValor) {
        jlccPrefs.putBoolean("tema.oscuro", jlccValor);
    }

    public boolean jlccMenuContraido() {
        return jlccPrefs.getBoolean("menu.contraido", false);
    }

    public void jlccMenuContraido(boolean jlccValor) {
        jlccPrefs.putBoolean("menu.contraido", jlccValor);
    }
}

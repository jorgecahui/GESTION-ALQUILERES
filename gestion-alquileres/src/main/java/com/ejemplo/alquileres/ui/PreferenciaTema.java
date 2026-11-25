package com.ejemplo.alquileres.ui;

import org.springframework.stereotype.Component;

import java.util.prefs.Preferences;

@Component
public class PreferenciaTema {
    private final Preferences prefs = Preferences.userNodeForPackage(PreferenciaTema.class);

    public boolean oscuro() {
        return prefs.getBoolean("tema.oscuro", false);
    }

    public void oscuro(boolean valor) {
        prefs.putBoolean("tema.oscuro", valor);
    }

    public boolean menuContraido() {
        return prefs.getBoolean("menu.contraido", false);
    }

    public void menuContraido(boolean valor) {
        prefs.putBoolean("menu.contraido", valor);
    }
}

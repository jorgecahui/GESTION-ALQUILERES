package com.ejemplo.alquileres.ui;

import java.util.prefs.Preferences;
import org.springframework.stereotype.Component;
@Component
public class PreferenciaTema {
    private final Preferences prefs = Preferences.userNodeForPackage(PreferenciaTema.class);
    public boolean oscuro() { return prefs.getBoolean("tema.oscuro", false); }
    public void oscuro(boolean valor) { prefs.putBoolean("tema.oscuro", valor); }
    public boolean menuContraido() { return prefs.getBoolean("menu.contraido", false); }
    public void menuContraido(boolean valor) { prefs.putBoolean("menu.contraido", valor); }
}

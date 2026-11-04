package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.service.Permisos;
import java.util.Locale;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Tooltip;

/** Deshabilita controles que el rol actual no puede usar, con una explicación. */
public final class RestriccionesUi {
    private RestriccionesUi() { }

    public static void aplicar(Parent raiz, Permisos permisos) {
        if (raiz == null || permisos == null) return;
        for (Node nodo : raiz.lookupAll(".button")) {
            if (!(nodo instanceof ButtonBase boton) || boton.getText() == null) continue;
            String texto = boton.getText().toLowerCase(Locale.ROOT);
            boolean soloAdmin = contiene(texto, "anular", "finalizar", "rescindir", "condonar",
                    "restablecer", "respald", "restaurar");
            boolean gestion = contiene(texto, "nuevo", "nueva", "editar", "guardar", "eliminar",
                    "desactivar", "registrar", "cobrar", "recordar", "resolver", "iniciar",
                    "generar", "activar", "renovar");
            boolean bloqueado = (soloAdmin && !permisos.esAdmin()) || (gestion && !permisos.puedeGestionar());
            if (bloqueado) {
                boton.setDisable(true);
                boton.setTooltip(new Tooltip(Permisos.SIN_PERMISO));
            }
        }
    }

    private static boolean contiene(String texto, String... palabras) {
        for (String palabra : palabras) if (texto.contains(palabra)) return true;
        return false;
    }
}

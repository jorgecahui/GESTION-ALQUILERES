package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.service.Permisos;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;

public final class RestriccionesUi {
    private RestriccionesUi() {}

    public static void aplicar(Parent raiz, Permisos permisos) {
        aplicar((Node) raiz, permisos);
    }

    public static void aplicar(Node raiz, Permisos permisos) {
        if (raiz == null || permisos == null) return;
        aplicarNodo(raiz, permisos);
        if (!(raiz instanceof Parent padre)) return;

        for (Node hijo : padre.getChildrenUnmodifiable()) aplicar(hijo, permisos);

        if (padre instanceof ScrollPane scroll) aplicar(scroll.getContent(), permisos);
        if (padre instanceof TitledPane titled) aplicar(titled.getContent(), permisos);
        if (padre instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) aplicar(tab.getContent(), permisos);
        }
        if (padre instanceof SplitPane split) {
            for (Node item : split.getItems()) aplicar(item, permisos);
        }
        if (padre instanceof Accordion accordion) {
            for (TitledPane panel : accordion.getPanes()) aplicar(panel, permisos);
        }
    }

    private static void aplicarNodo(Node nodo, Permisos permisos) {
        boolean requiereAdmin = nodo.getStyleClass().contains("requiere-admin");
        boolean requiereGestion = nodo.getStyleClass().contains("requiere-gestion");
        if (!requiereAdmin && !requiereGestion) return;

        boolean bloqueado = requiereAdmin ? !permisos.esAdmin() : !permisos.puedeGestionar();
        if (!bloqueado) return;

        nodo.setDisable(true);
        Tooltip explicacion = new Tooltip(Permisos.SIN_PERMISO);
        if (nodo instanceof Control control) control.setTooltip(explicacion);
        else Tooltip.install(nodo, explicacion);
    }
}

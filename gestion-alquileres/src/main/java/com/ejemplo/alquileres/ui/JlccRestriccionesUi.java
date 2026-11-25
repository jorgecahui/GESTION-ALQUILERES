package com.ejemplo.alquileres.ui;

import com.ejemplo.alquileres.service.JlccPermisos;

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

public final class JlccRestriccionesUi {
    private JlccRestriccionesUi() {}

    public static void jlccAplicar(Parent jlccRaiz, JlccPermisos jlccPermisos) {
        jlccAplicar((Node) jlccRaiz, jlccPermisos);
    }

    public static void jlccAplicar(Node jlccRaiz, JlccPermisos jlccPermisos) {
        if (jlccRaiz == null || jlccPermisos == null) return;
        jlccAplicarNodo(jlccRaiz, jlccPermisos);
        if (!(jlccRaiz instanceof Parent jlccPadre)) return;

        for (Node jlccHijo : jlccPadre.getChildrenUnmodifiable()) jlccAplicar(jlccHijo, jlccPermisos);

        if (jlccPadre instanceof ScrollPane jlccScroll) jlccAplicar(jlccScroll.getContent(), jlccPermisos);
        if (jlccPadre instanceof TitledPane jlccTitled) jlccAplicar(jlccTitled.getContent(), jlccPermisos);
        if (jlccPadre instanceof TabPane jlccTabs) {
            for (Tab jlccTab : jlccTabs.getTabs()) jlccAplicar(jlccTab.getContent(), jlccPermisos);
        }
        if (jlccPadre instanceof SplitPane jlccSplit) {
            for (Node jlccItem : jlccSplit.getItems()) jlccAplicar(jlccItem, jlccPermisos);
        }
        if (jlccPadre instanceof Accordion jlccAccordion) {
            for (TitledPane jlccPanel : jlccAccordion.getPanes()) jlccAplicar(jlccPanel, jlccPermisos);
        }
    }

    private static void jlccAplicarNodo(Node jlccNodo, JlccPermisos jlccPermisos) {
        boolean jlccRequiereAdmin = jlccNodo.getStyleClass().contains("requiere-admin");
        boolean jlccRequiereGestion = jlccNodo.getStyleClass().contains("requiere-gestion");
        if (!jlccRequiereAdmin && !jlccRequiereGestion) return;

        boolean jlccBloqueado = jlccRequiereAdmin ? !jlccPermisos.jlccEsAdmin() : !jlccPermisos.jlccPuedeGestionar();
        if (!jlccBloqueado) return;

        jlccNodo.setDisable(true);
        Tooltip jlccExplicacion = new Tooltip(JlccPermisos.JLCC_SIN_PERMISO);
        if (jlccNodo instanceof Control jlccControl) jlccControl.setTooltip(jlccExplicacion);
        else Tooltip.install(jlccNodo, jlccExplicacion);
    }
}

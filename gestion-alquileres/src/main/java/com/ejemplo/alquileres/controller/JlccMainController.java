package com.ejemplo.alquileres.controller;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.util.Animations;

import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.service.JlccPermisos;
import com.ejemplo.alquileres.service.JlccSesionActual;
import com.ejemplo.alquileres.ui.JlccAplicacionUi;
import com.ejemplo.alquileres.ui.JlccNavegacion;
import com.ejemplo.alquileres.ui.JlccPreferenciaTema;
import com.ejemplo.alquileres.ui.JlccRefrescable;
import com.ejemplo.alquileres.ui.JlccRestriccionesUi;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JlccMainController {
    private static final double JLCC_ANCHO_EXPANDIDO = 248, JLCC_ANCHO_CONTRAIDO = 76;
    private final ApplicationContext jlccContext;
    private final JlccNavegacion jlccNavegacion;
    private final JlccPreferenciaTema jlccPreferencias;
    private final JlccSesionActual jlccSesion;
    private final JlccPermisos jlccPermisos;
    private final JlccAplicacionUi jlccAplicacion;
    private final Map<String, Vista> jlccVistas = new HashMap<>();
    @FXML private StackPane raiz, contenido;
    @FXML private VBox sidebar, marcaTextos, usuarioTextos;
    @FXML private Label usuarioActual, rolActual, inicialUsuario;
    @FXML private ToggleGroup menu;
    @FXML private Region indicador;
    @FXML private Button btnTema, btnContraer, btnSalir;
    @FXML
    private ToggleButton navDashboard,
            navMaqueta,
            navNuevoContrato,
            navContratos,
            navNuevoPago,
            navPagos,
            navCuentasCobrar,
            navMantenimiento,
            navRentabilidad,
            navInmuebles,
            navTiposInmueble,
            navPersonas,
            navPropietarios,
            navConfiguracion;
    private boolean jlccOscuro, jlccContraido;

    private record Vista(Parent nodo, Object controlador) {}

    public JlccMainController(
            ApplicationContext jlccContext,
            JlccNavegacion jlccNavegacion,
            JlccPreferenciaTema jlccPreferencias,
            JlccSesionActual jlccSesion,
            JlccPermisos jlccPermisos,
            JlccAplicacionUi jlccAplicacion) {
        this.jlccContext = jlccContext;
        this.jlccNavegacion = jlccNavegacion;
        this.jlccPreferencias = jlccPreferencias;
        this.jlccSesion = jlccSesion;
        this.jlccPermisos = jlccPermisos;
        this.jlccAplicacion = jlccAplicacion;
    }

    @FXML
    public void initialize() {
        jlccVistas.clear();
        indicador.maxHeightProperty().bind(indicador.prefHeightProperty());
        if (jlccSesion.jlccUsuario() != null) {
            usuarioActual.setText(jlccSesion.jlccUsuario().getNombre());
            rolActual.setText(jlccSesion.jlccRol() == null ? "" : jlccSesion.jlccRol().getEtiqueta());
            String jlccInicial =
                    jlccSesion.jlccUsuario().getNombre() == null || jlccSesion.jlccUsuario().getNombre().isBlank()
                            ? "?"
                            : jlccSesion.jlccUsuario().getNombre().substring(0, 1).toUpperCase();
            inicialUsuario.setText(jlccInicial);
        }
        boolean jlccAdmin = jlccSesion.jlccRol() == RolUsuario.ADMIN;
        navConfiguracion.setVisible(jlccAdmin);
        navConfiguracion.setManaged(jlccAdmin);
        Map<ToggleButton, String> jlccClaves =
                Map.ofEntries(
                        Map.entry(navDashboard, "dashboard"),
                        Map.entry(navMaqueta, "maqueta-3d"),
                        Map.entry(navNuevoContrato, "nuevo-contrato"),
                        Map.entry(navContratos, "contratos"),
                        Map.entry(navNuevoPago, "nuevo-pago"),
                        Map.entry(navPagos, "pagos"),
                        Map.entry(navCuentasCobrar, "cuentas-cobrar"),
                        Map.entry(navMantenimiento, "mantenimiento"),
                        Map.entry(navRentabilidad, "rentabilidad"),
                        Map.entry(navInmuebles, "inmuebles"),
                        Map.entry(navTiposInmueble, "tipos-inmueble"),
                        Map.entry(navPersonas, "personas"),
                        Map.entry(navPropietarios, "propietarios"),
                        Map.entry(navConfiguracion, "configuracion"));
        jlccClaves.forEach(
                (boton, clave) -> {
                    boton.setUserData(clave);
                    boton.setTooltip(new Tooltip(boton.getText()));
                });
        menu.selectedToggleProperty()
                .addListener(
                        (obs, anterior, actual) -> {
                            if (actual == null) {
                                menu.selectToggle(anterior);
                                return;
                            }
                            jlccMostrar((String) actual.getUserData());
                            jlccMoverIndicador(actual);
                        });
        jlccAplicarTema(jlccPreferencias.jlccOscuro());
        jlccContraido = jlccPreferencias.jlccMenuContraido();
        if (jlccContraido) jlccAplicarMenu(true, false);
        menu.selectToggle(navDashboard);
        Platform.runLater(() -> jlccMoverIndicador(navDashboard));
    }

    public void jlccIrA(String jlccClave) {
        jlccBotonesMenu().stream()
                .filter(b -> jlccClave.equals(b.getUserData()))
                .findFirst()
                .ifPresent(menu::selectToggle);
    }

    public void jlccIrANuevoPago(Long jlccContratoId) {
        jlccContext.getBean(JlccPagoFormController.class).jlccPreseleccionarContrato(jlccContratoId);
        jlccIrA("nuevo-pago");
    }

    public void jlccIrANuevoContrato() {
        jlccContext.getBean(JlccContratoFormController.class).jlccPrepararNuevo();
        jlccIrA("nuevo-contrato");
    }

    public void jlccIrARenovarContrato(Long jlccContratoId) {
        jlccContext.getBean(JlccContratoFormController.class).jlccPreseleccionarRenovacion(jlccContratoId);
        jlccIrA("nuevo-contrato");
    }

    private void jlccMostrar(String jlccClave) {
        if ("nuevo-contrato".equals(jlccClave)) {
            JlccContratoFormController jlccFormulario = jlccContext.getBean(JlccContratoFormController.class);
            if (!jlccFormulario.jlccTieneRenovacionPendiente()) jlccFormulario.jlccPrepararNuevo();
        }
        Vista jlccVista = jlccVistas.computeIfAbsent(jlccClave, this::jlccCargar);
        contenido.getChildren().setAll(jlccVista.nodo());
        if (jlccVista.controlador() instanceof JlccRefrescable jlccRefrescable) jlccRefrescable.refrescar();
        Animations.fadeInUp(jlccVista.nodo(), Duration.millis(260)).play();
    }

    private Vista jlccCargar(String jlccClave) {
        try {
            FXMLLoader jlccLoader = new FXMLLoader(getClass().getResource(jlccNavegacion.jlccRecurso(jlccClave)));
            jlccLoader.setControllerFactory(jlccContext::getBean);
            Parent jlccNodo = jlccLoader.load();
            JlccRestriccionesUi.jlccAplicar(jlccNodo, jlccPermisos);
            if (jlccLoader.getController() instanceof JlccPlaceholderController jlccPlaceholder)
                jlccPlaceholder.jlccMostrar(jlccNavegacion.jlccObtener(jlccClave));
            return new Vista(jlccNodo, jlccLoader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + jlccClave, e);
        }
    }

    private void jlccMoverIndicador(Toggle jlccToggle) {
        if (jlccToggle == null) return;
        Node jlccBoton = (Node) jlccToggle;
        double jlccDestino = jlccBoton.getBoundsInParent().getMinY();
        indicador.setPrefHeight(jlccBoton.getBoundsInParent().getHeight());
        new Timeline(
                        new KeyFrame(
                                Duration.millis(240),
                                new KeyValue(
                                        indicador.translateYProperty(),
                                        jlccDestino,
                                        Interpolator.EASE_BOTH)))
                .play();
    }

    @FXML
    private void jlccOnCambiarTema() {
        jlccAplicarTema(!jlccOscuro);
        jlccPreferencias.jlccOscuro(jlccOscuro);
    }

    private void jlccAplicarTema(boolean jlccValor) {
        jlccOscuro = jlccValor;
        Application.setUserAgentStylesheet(
                jlccOscuro
                        ? new PrimerDark().getUserAgentStylesheet()
                        : new PrimerLight().getUserAgentStylesheet());
        if (jlccOscuro) raiz.getStyleClass().add("dark");
        else raiz.getStyleClass().remove("dark");
        ((FontIcon) btnTema.getGraphic()).setIconCode(jlccOscuro ? Feather.SUN : Feather.MOON);
        btnTema.setText(jlccOscuro ? "Modo claro" : "Modo oscuro");
    }

    @FXML
    private void jlccOnContraerMenu() {
        jlccContraido = !jlccContraido;
        jlccAplicarMenu(jlccContraido, true);
        jlccPreferencias.jlccMenuContraido(jlccContraido);
    }

    @FXML
    private void jlccOnCerrarSesion() {
        Alert jlccConfirmacion =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "¿Deseas cerrar la sesión actual?",
                        ButtonType.YES,
                        ButtonType.NO);
        jlccConfirmacion.setHeaderText(null);
        jlccConfirmacion.setTitle("Cerrar sesión");
        jlccConfirmacion
                .showAndWait()
                .filter(opcion -> opcion == ButtonType.YES)
                .ifPresent(opcion -> jlccAplicacion.jlccCerrarSesion());
    }

    private void jlccAplicarMenu(boolean jlccReducir, boolean jlccAnimar) {
        double jlccAncho = jlccReducir ? JLCC_ANCHO_CONTRAIDO : JLCC_ANCHO_EXPANDIDO;
        List<Node> jlccTextos = new ArrayList<>(sidebar.lookupAll(".nav-seccion"));
        jlccTextos.add(marcaTextos);
        jlccTextos.add(usuarioTextos);
        jlccTextos.forEach(
                n -> {
                    n.setVisible(!jlccReducir);
                    n.setManaged(!jlccReducir);
                });
        jlccTodosLosBotones()
                .forEach(
                        b ->
                                b.setContentDisplay(
                                        jlccReducir
                                                ? ContentDisplay.GRAPHIC_ONLY
                                                : ContentDisplay.LEFT));
        ((FontIcon) btnContraer.getGraphic())
                .setIconCode(jlccReducir ? Feather.CHEVRONS_RIGHT : Feather.CHEVRONS_LEFT);
        if (jlccAnimar)
            new Timeline(
                            new KeyFrame(
                                    Duration.millis(260),
                                    new KeyValue(sidebar.prefWidthProperty(), jlccAncho),
                                    new KeyValue(sidebar.minWidthProperty(), jlccAncho)))
                    .play();
        else {
            sidebar.setPrefWidth(jlccAncho);
            sidebar.setMinWidth(jlccAncho);
        }
        Platform.runLater(() -> jlccMoverIndicador(menu.getSelectedToggle()));
    }

    private List<ToggleButton> jlccBotonesMenu() {
        return List.of(
                navDashboard,
                navMaqueta,
                navNuevoContrato,
                navContratos,
                navNuevoPago,
                navPagos,
                navCuentasCobrar,
                navMantenimiento,
                navRentabilidad,
                navInmuebles,
                navTiposInmueble,
                navPersonas,
                navPropietarios,
                navConfiguracion);
    }

    private List<ButtonBase> jlccTodosLosBotones() {
        List<ButtonBase> jlccTodos = new ArrayList<>(jlccBotonesMenu());
        jlccTodos.add(btnTema);
        jlccTodos.add(btnContraer);
        return jlccTodos;
    }
}

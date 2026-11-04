package com.ejemplo.alquileres.controller;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.util.Animations;
import com.ejemplo.alquileres.service.Permisos;
import com.ejemplo.alquileres.service.SesionActual;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.ui.AplicacionUi;
import com.ejemplo.alquileres.ui.Navegacion;
import com.ejemplo.alquileres.ui.PreferenciaTema;
import com.ejemplo.alquileres.ui.RestriccionesUi;
import com.ejemplo.alquileres.ui.Refrescable;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
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
public class MainController {
    private static final double ANCHO_EXPANDIDO = 248, ANCHO_CONTRAIDO = 76;
    private final ApplicationContext context;
    private final Navegacion navegacion;
    private final PreferenciaTema preferencias;
    private final SesionActual sesion;
    private final Permisos permisos;
    private final AplicacionUi aplicacion;
    private final Map<String, Vista> vistas = new HashMap<>();
    @FXML private StackPane raiz, contenido;
    @FXML private VBox sidebar, marcaTextos, usuarioTextos;
    @FXML private Label usuarioActual, rolActual, inicialUsuario;
    @FXML private ToggleGroup menu;
    @FXML private Region indicador;
    @FXML private Button btnTema, btnContraer, btnSalir;
    @FXML private ToggleButton navDashboard, navNuevoContrato, navContratos, navNuevoPago, navPagos,
            navCuentasCobrar, navMantenimiento, navRentabilidad, navInmuebles, navTiposInmueble,
            navPersonas, navPropietarios, navConfiguracion;
    private boolean oscuro, contraido;
    private record Vista(Parent nodo, Object controlador) {}
    public MainController(ApplicationContext context, Navegacion navegacion, PreferenciaTema preferencias,
                          SesionActual sesion, Permisos permisos, AplicacionUi aplicacion) {
        this.context = context; this.navegacion = navegacion; this.preferencias = preferencias;
        this.sesion = sesion; this.permisos = permisos; this.aplicacion = aplicacion;
    }
    @FXML public void initialize() {
        vistas.clear();
        if (sesion.usuario() != null) {
            usuarioActual.setText(sesion.usuario().getNombre());
            rolActual.setText(sesion.rol() == null ? "" : sesion.rol().getEtiqueta());
            String inicial = sesion.usuario().getNombre() == null || sesion.usuario().getNombre().isBlank()
                    ? "?" : sesion.usuario().getNombre().substring(0, 1).toUpperCase();
            inicialUsuario.setText(inicial);
        }
        boolean admin = sesion.rol() == RolUsuario.ADMIN;
        navConfiguracion.setVisible(admin);
        navConfiguracion.setManaged(admin);
        Map<ToggleButton, String> claves = Map.ofEntries(
            Map.entry(navDashboard,"dashboard"), Map.entry(navNuevoContrato,"nuevo-contrato"), Map.entry(navContratos,"contratos"),
            Map.entry(navNuevoPago,"nuevo-pago"), Map.entry(navPagos,"pagos"), Map.entry(navCuentasCobrar,"cuentas-cobrar"),
            Map.entry(navMantenimiento,"mantenimiento"), Map.entry(navRentabilidad,"rentabilidad"), Map.entry(navInmuebles,"inmuebles"),
            Map.entry(navTiposInmueble,"tipos-inmueble"), Map.entry(navPersonas,"personas"), Map.entry(navPropietarios,"propietarios"),
            Map.entry(navConfiguracion,"configuracion"));
        claves.forEach((boton, clave) -> { boton.setUserData(clave); boton.setTooltip(new Tooltip(boton.getText())); });
        menu.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) { menu.selectToggle(anterior); return; }
            mostrar((String) actual.getUserData()); moverIndicador(actual);
        });
        aplicarTema(preferencias.oscuro());
        contraido = preferencias.menuContraido();
        if (contraido) aplicarMenu(true, false);
        menu.selectToggle(navDashboard);
        Platform.runLater(() -> moverIndicador(navDashboard));
    }
    public void irA(String clave) { botonesMenu().stream().filter(b -> clave.equals(b.getUserData())).findFirst().ifPresent(menu::selectToggle); }
    /** Usado desde cuentas por cobrar para conservar el contexto del contrato al abrir el formulario. */
    public void irANuevoPago(Long contratoId) { context.getBean(PagoFormController.class).preseleccionarContrato(contratoId); irA("nuevo-pago"); }
    private void mostrar(String clave) {
        Vista vista = vistas.computeIfAbsent(clave, this::cargar);
        contenido.getChildren().setAll(vista.nodo());
        if (vista.controlador() instanceof Refrescable refrescable) refrescable.refrescar();
        Animations.fadeInUp(vista.nodo(), Duration.millis(260)).play();
    }
    private Vista cargar(String clave) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(navegacion.recurso(clave)));
            loader.setControllerFactory(context::getBean);
            Parent nodo = loader.load();
            RestriccionesUi.aplicar(nodo, permisos);
            if (loader.getController() instanceof PlaceholderController placeholder) placeholder.mostrar(navegacion.obtener(clave));
            return new Vista(nodo, loader.getController());
        } catch (IOException e) { throw new UncheckedIOException("No se pudo cargar la vista " + clave, e); }
    }
    private void moverIndicador(Toggle toggle) {
        if (toggle == null) return;
        Node boton = (Node) toggle; double destino = boton.getBoundsInParent().getMinY();
        indicador.setPrefHeight(boton.getBoundsInParent().getHeight());
        new Timeline(new KeyFrame(Duration.millis(240), new KeyValue(indicador.translateYProperty(), destino, Interpolator.EASE_BOTH))).play();
    }
    @FXML private void onCambiarTema() { aplicarTema(!oscuro); preferencias.oscuro(oscuro); }
    private void aplicarTema(boolean valor) {
        oscuro = valor;
        Application.setUserAgentStylesheet(oscuro ? new PrimerDark().getUserAgentStylesheet() : new PrimerLight().getUserAgentStylesheet());
        if (oscuro) raiz.getStyleClass().add("dark"); else raiz.getStyleClass().remove("dark");
        ((FontIcon) btnTema.getGraphic()).setIconCode(oscuro ? Feather.SUN : Feather.MOON);
        btnTema.setText(oscuro ? "Modo claro" : "Modo oscuro");
    }
    @FXML private void onContraerMenu() { contraido = !contraido; aplicarMenu(contraido, true); preferencias.menuContraido(contraido); }
    @FXML private void onCerrarSesion() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Deseas cerrar la sesión actual?", ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Cerrar sesión");
        confirmacion.showAndWait().filter(opcion -> opcion == ButtonType.YES)
                .ifPresent(opcion -> aplicacion.cerrarSesion());
    }
    private void aplicarMenu(boolean reducir, boolean animar) {
        double ancho = reducir ? ANCHO_CONTRAIDO : ANCHO_EXPANDIDO;
        List<Node> textos = new ArrayList<>(sidebar.lookupAll(".nav-seccion")); textos.add(marcaTextos); textos.add(usuarioTextos);
        textos.forEach(n -> { n.setVisible(!reducir); n.setManaged(!reducir); });
        todosLosBotones().forEach(b -> b.setContentDisplay(reducir ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT));
        ((FontIcon) btnContraer.getGraphic()).setIconCode(reducir ? Feather.CHEVRONS_RIGHT : Feather.CHEVRONS_LEFT);
        if (animar) new Timeline(new KeyFrame(Duration.millis(260), new KeyValue(sidebar.prefWidthProperty(), ancho), new KeyValue(sidebar.minWidthProperty(), ancho))).play();
        else { sidebar.setPrefWidth(ancho); sidebar.setMinWidth(ancho); }
        Platform.runLater(() -> moverIndicador(menu.getSelectedToggle()));
    }
    private List<ToggleButton> botonesMenu() { return List.of(navDashboard,navNuevoContrato,navContratos,navNuevoPago,navPagos,navCuentasCobrar,navMantenimiento,navRentabilidad,navInmuebles,navTiposInmueble,navPersonas,navPropietarios,navConfiguracion); }
    private List<ButtonBase> todosLosBotones() { List<ButtonBase> todos = new ArrayList<>(botonesMenu()); todos.add(btnTema); todos.add(btnContraer); return todos; }
}

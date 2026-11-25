package com.ejemplo.alquileres.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;

import com.ejemplo.alquileres.controller.BloqueoController;
import com.ejemplo.alquileres.service.AutenticacionService;
import com.ejemplo.alquileres.service.ConfiguracionService;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AplicacionUi {
    private final ApplicationContext context;
    private final PreferenciaTema preferencias;
    private final AutenticacionService autenticacion;
    private final ConfiguracionService configuracion;
    private Stage stage;
    private ControlInactividad inactividad;

    public AplicacionUi(
            ApplicationContext context,
            PreferenciaTema preferencias,
            AutenticacionService autenticacion,
            ConfiguracionService configuracion) {
        this.context = context;
        this.preferencias = preferencias;
        this.autenticacion = autenticacion;
        this.configuracion = configuracion;
    }

    public void iniciar(Stage stage, boolean hayUsuarios) {
        this.stage = stage;
        aplicarTema();
        if (hayUsuarios) mostrarLogin();
        else mostrarPrimerUsuario();
    }

    public void mostrarLogin() {
        detenerInactividad();
        cargar(
                "/view/login-view.fxml",
                "Iniciar sesión | Gestión de Alquileres",
                460,
                560,
                420,
                500);
    }

    public void mostrarPrimerUsuario() {
        detenerInactividad();
        cargar(
                "/view/primer-usuario-view.fxml",
                "Primer administrador | Gestión de Alquileres",
                540,
                680,
                470,
                600);
    }

    public void mostrarPrincipal() {
        detenerInactividad();
        Parent raiz =
                cargar(
                        "/view/main-view.fxml",
                        "Panel administrativo | Contratos, Cobranza e Inmuebles",
                        1280,
                        800,
                        1100,
                        700);
        inactividad = new ControlInactividad(raiz.getScene(), configuracion, this::bloquear);
        inactividad.instalar();
    }

    public void cerrarSesion() {
        detenerInactividad();
        autenticacion.cerrarSesion();
        mostrarLogin();
    }

    public void bloquear() {
        if (stage == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/bloqueo-view.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent raiz = loader.load();
            Scene escena = new Scene(raiz, 420, 330);
            escena.getStylesheets()
                    .add(
                            Objects.requireNonNull(getClass().getResource("/css/app.css"))
                                    .toExternalForm());
            if (preferencias.oscuro()) raiz.getStyleClass().add("dark");
            Stage bloqueo = new Stage(StageStyle.UNDECORATED);
            bloqueo.initOwner(stage);
            bloqueo.initModality(Modality.APPLICATION_MODAL);
            bloqueo.setTitle("Sesión bloqueada");
            bloqueo.setScene(escena);
            bloqueo.setResizable(false);
            bloqueo.setOnCloseRequest(evento -> evento.consume());
            BloqueoController controlador = loader.getController();
            controlador.configurar(bloqueo, this);
            bloqueo.show();
            controlador.enfocar();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo mostrar la pantalla de bloqueo.", e);
        }
    }

    public void desbloquear() {
        if (inactividad != null) inactividad.reiniciar();
    }

    public void salir() {
        detenerInactividad();
        if (stage != null) stage.close();
        javafx.application.Platform.exit();
    }

    public void aplicarTema() {
        Application.setUserAgentStylesheet(
                preferencias.oscuro()
                        ? new PrimerDark().getUserAgentStylesheet()
                        : new PrimerLight().getUserAgentStylesheet());
    }

    public Stage stage() {
        return stage;
    }

    private Parent cargar(
            String recurso,
            String titulo,
            double ancho,
            double alto,
            double minimoAncho,
            double minimoAlto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(recurso));
            loader.setControllerFactory(context::getBean);
            Parent raiz = loader.load();
            Scene escena = new Scene(raiz, ancho, alto);
            escena.getStylesheets()
                    .add(
                            Objects.requireNonNull(getClass().getResource("/css/app.css"))
                                    .toExternalForm());
            if (preferencias.oscuro()) raiz.getStyleClass().add("dark");
            stage.setTitle(titulo);
            stage.setScene(escena);
            stage.setMinWidth(minimoAncho);
            stage.setMinHeight(minimoAlto);
            stage.setWidth(ancho);
            stage.setHeight(alto);
            stage.centerOnScreen();
            stage.show();
            return raiz;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar la pantalla " + recurso, e);
        }
    }

    private void detenerInactividad() {
        if (inactividad != null) {
            inactividad.detener();
            inactividad = null;
        }
    }
}

package com.ejemplo.alquileres.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;

import com.ejemplo.alquileres.controller.JlccBloqueoController;
import com.ejemplo.alquileres.service.JlccAutenticacionService;
import com.ejemplo.alquileres.service.JlccConfiguracionService;

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
public class JlccAplicacionUi {
    private final ApplicationContext jlccContext;
    private final JlccPreferenciaTema jlccPreferencias;
    private final JlccAutenticacionService jlccAutenticacion;
    private final JlccConfiguracionService jlccConfiguracion;
    private Stage jlccStage;
    private JlccControlInactividad jlccInactividad;

    public JlccAplicacionUi(
            ApplicationContext jlccContext,
            JlccPreferenciaTema jlccPreferencias,
            JlccAutenticacionService jlccAutenticacion,
            JlccConfiguracionService jlccConfiguracion) {
        this.jlccContext = jlccContext;
        this.jlccPreferencias = jlccPreferencias;
        this.jlccAutenticacion = jlccAutenticacion;
        this.jlccConfiguracion = jlccConfiguracion;
    }

    public void jlccIniciar(Stage jlccStage, boolean jlccHayUsuarios) {
        this.jlccStage = jlccStage;
        jlccAplicarTema();
        if (jlccHayUsuarios) jlccMostrarLogin();
        else jlccMostrarPrimerUsuario();
    }

    public void jlccMostrarLogin() {
        jlccDetenerInactividad();
        jlccCargar(
                "/view/login-view.fxml",
                "Iniciar sesión | Gestión de Alquileres",
                460,
                560,
                420,
                500);
    }

    public void jlccMostrarPrimerUsuario() {
        jlccDetenerInactividad();
        jlccCargar(
                "/view/primer-usuario-view.fxml",
                "Primer administrador | Gestión de Alquileres",
                540,
                680,
                470,
                600);
    }

    public void jlccMostrarPrincipal() {
        jlccDetenerInactividad();
        Parent jlccRaiz =
                jlccCargar(
                        "/view/main-view.fxml",
                        "Panel administrativo | Contratos, Cobranza e Inmuebles",
                        1280,
                        800,
                        1100,
                        700);
        jlccInactividad = new JlccControlInactividad(jlccRaiz.getScene(), jlccConfiguracion, this::jlccBloquear);
        jlccInactividad.jlccInstalar();
    }

    public void jlccCerrarSesion() {
        jlccDetenerInactividad();
        jlccAutenticacion.jlccCerrarSesion();
        jlccMostrarLogin();
    }

    public void jlccBloquear() {
        if (jlccStage == null) return;
        try {
            FXMLLoader jlccLoader = new FXMLLoader(getClass().getResource("/view/bloqueo-view.fxml"));
            jlccLoader.setControllerFactory(jlccContext::getBean);
            Parent jlccRaiz = jlccLoader.load();
            Scene jlccEscena = new Scene(jlccRaiz, 420, 330);
            jlccEscena.getStylesheets()
                    .add(
                            Objects.requireNonNull(getClass().getResource("/css/app.css"))
                                    .toExternalForm());
            if (jlccPreferencias.jlccOscuro()) jlccRaiz.getStyleClass().add("dark");
            Stage jlccBloqueo = new Stage(StageStyle.UNDECORATED);
            jlccBloqueo.initOwner(jlccStage);
            jlccBloqueo.initModality(Modality.APPLICATION_MODAL);
            jlccBloqueo.setTitle("Sesión bloqueada");
            jlccBloqueo.setScene(jlccEscena);
            jlccBloqueo.setResizable(false);
            jlccBloqueo.setOnCloseRequest(evento -> evento.consume());
            JlccBloqueoController jlccControlador = jlccLoader.getController();
            jlccControlador.jlccConfigurar(jlccBloqueo, this);
            jlccBloqueo.show();
            jlccControlador.jlccEnfocar();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo mostrar la pantalla de bloqueo.", e);
        }
    }

    public void jlccDesbloquear() {
        if (jlccInactividad != null) jlccInactividad.jlccReiniciar();
    }

    public void jlccSalir() {
        jlccDetenerInactividad();
        if (jlccStage != null) jlccStage.close();
        javafx.application.Platform.exit();
    }

    public void jlccAplicarTema() {
        Application.setUserAgentStylesheet(
                jlccPreferencias.jlccOscuro()
                        ? new PrimerDark().getUserAgentStylesheet()
                        : new PrimerLight().getUserAgentStylesheet());
    }

    public Stage jlccStage() {
        return jlccStage;
    }

    private Parent jlccCargar(
            String jlccRecurso,
            String jlccTitulo,
            double jlccAncho,
            double jlccAlto,
            double jlccMinimoAncho,
            double jlccMinimoAlto) {
        try {
            FXMLLoader jlccLoader = new FXMLLoader(getClass().getResource(jlccRecurso));
            jlccLoader.setControllerFactory(jlccContext::getBean);
            Parent jlccRaiz = jlccLoader.load();
            Scene jlccEscena = new Scene(jlccRaiz, jlccAncho, jlccAlto);
            jlccEscena.getStylesheets()
                    .add(
                            Objects.requireNonNull(getClass().getResource("/css/app.css"))
                                    .toExternalForm());
            if (jlccPreferencias.jlccOscuro()) jlccRaiz.getStyleClass().add("dark");
            jlccStage.setTitle(jlccTitulo);
            jlccStage.setScene(jlccEscena);
            jlccStage.setMinWidth(jlccMinimoAncho);
            jlccStage.setMinHeight(jlccMinimoAlto);
            jlccStage.setWidth(jlccAncho);
            jlccStage.setHeight(jlccAlto);
            jlccStage.centerOnScreen();
            jlccStage.show();
            return jlccRaiz;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar la pantalla " + jlccRecurso, e);
        }
    }

    private void jlccDetenerInactividad() {
        if (jlccInactividad != null) {
            jlccInactividad.jlccDetener();
            jlccInactividad = null;
        }
    }
}

package com.ejemplo.alquileres;

import com.ejemplo.alquileres.config.DirectorioDatosInitializer;
import com.ejemplo.alquileres.config.VerificacionIntegridad;
import com.ejemplo.alquileres.service.JlccAutenticacionService;
import com.ejemplo.alquileres.service.JlccRespaldoService;
import com.ejemplo.alquileres.ui.JlccAplicacionUi;
import com.ejemplo.alquileres.ui.JlccExportador;
import com.ejemplo.alquileres.ui.JlccManejadorErrores;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(JavaFxApplication.class);
    private ConfigurableApplicationContext context;
    private boolean hayUsuarios;

    @Override
    public void init() {
        context =
                new SpringApplicationBuilder(AlquileresApplication.class)
                        .web(WebApplicationType.NONE)
                        .initializers(new DirectorioDatosInitializer())
                        .run();
        hayUsuarios = context.getBean(JlccAutenticacionService.class).jlccHayUsuarios();
    }

    @Override
    public void start(Stage stage) {
        context.getBean(JlccManejadorErrores.class).jlccInstalarGlobal();
        context.getBean(JlccExportador.class).jlccSetHostServices(getHostServices());
        context.getBean(JlccAplicacionUi.class).jlccIniciar(stage, hayUsuarios);
        VerificacionIntegridad integridad = context.getBean(VerificacionIntegridad.class);
        if (!integridad.correcta()) {
            Alert aviso =
                    new Alert(
                            Alert.AlertType.WARNING,
                            "La verificación de integridad de la base de datos reportó un"
                                + " problema:\n\n"
                                    + integridad.detalle()
                                    + "\n\nSe recomienda crear un respaldo y revisar los datos.",
                            javafx.scene.control.ButtonType.OK);
            aviso.setTitle("Verificación de integridad");
            aviso.setHeaderText("La base de datos requiere atención");
            aviso.show();
        }
    }

    @Override
    public void stop() {
        if (context == null) return;
        try {
            context.getBean(JlccRespaldoService.class).jlccRespaldoAutomatico();
        } catch (Exception e) {
            log.warn("No se pudo crear el respaldo automático al cerrar.", e);
        }
        context.close();
        Platform.exit();
    }
}

package com.ejemplo.alquileres;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.ejemplo.alquileres.controller.ContratoController;
import com.ejemplo.alquileres.controller.ContratoFormController;
import com.ejemplo.alquileres.controller.FichaInmuebleController;
import com.ejemplo.alquileres.controller.InmuebleController;
import com.ejemplo.alquileres.controller.MainController;
import com.ejemplo.alquileres.controller.MaquetaController;
import com.ejemplo.alquileres.controller.MantenimientoController;
import com.ejemplo.alquileres.controller.PagosController;
import com.ejemplo.alquileres.controller.PersonaController;
import com.ejemplo.alquileres.controller.PropietarioController;
import com.ejemplo.alquileres.controller.TipoInmuebleController;
import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.service.ConfiguracionService;
import com.ejemplo.alquileres.service.ContratoPdfService;
import com.ejemplo.alquileres.service.ContratoService;
import com.ejemplo.alquileres.service.CronogramaGenerador;
import com.ejemplo.alquileres.service.EstadoCuentaPdfService;
import com.ejemplo.alquileres.service.InmuebleService;
import com.ejemplo.alquileres.service.MantenimientoService;
import com.ejemplo.alquileres.service.MaquetaService;
import com.ejemplo.alquileres.service.PagoService;
import com.ejemplo.alquileres.service.PersonaService;
import com.ejemplo.alquileres.service.PropietarioService;
import com.ejemplo.alquileres.service.ReciboPdfService;
import com.ejemplo.alquileres.service.RecordatorioService;
import com.ejemplo.alquileres.service.ReporteContratosExcelService;
import com.ejemplo.alquileres.service.ReporteInmueblesExcelService;
import com.ejemplo.alquileres.service.ReporteMantenimientoExcelService;
import com.ejemplo.alquileres.service.ReportePagosExcelService;
import com.ejemplo.alquileres.service.RentabilidadService;
import com.ejemplo.alquileres.service.TipoInmuebleService;
import com.ejemplo.alquileres.ui.Exportador;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Tareas;
import java.time.Clock;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class FxmlSmokeTest {
    @BeforeAll static void iniciar() {
        try { Platform.startup(() -> { }); } catch (IllegalStateException ignorado) { }
    }

    @AfterAll static void cerrar() { Platform.exit(); }

    @Test void cargaFormularioDeContrato() throws Exception {
        ContratoFormController controlador = new ContratoFormController(
                mock(ContratoService.class), mock(InmuebleService.class), mock(PersonaService.class),
                new CronogramaGenerador(), mock(ConfiguracionService.class), mock(Tareas.class),
                mock(Notificador.class), mock(MainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/contrato-form-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaListaDeContratos() throws Exception {
        ContratoController controlador = new ContratoController(
                mock(ContratoService.class), mock(RecordatorioService.class),
                mock(EstadoCuentaPdfService.class), mock(ContratoPdfService.class),
                mock(ReporteContratosExcelService.class), mock(Tareas.class),
                mock(Notificador.class), mock(Exportador.class), mock(MainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/contratos-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaFichaDeInmueble() throws Exception {
        InmuebleController controlador = new InmuebleController(
                mock(InmuebleService.class), mock(TipoInmuebleService.class),
                mock(PropietarioService.class), mock(ReporteInmueblesExcelService.class),
                mock(FichaInmuebleController.class), mock(ContratoService.class),
                mock(Tareas.class), mock(Notificador.class), mock(Exportador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/inmueble-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaListaDePagos() throws Exception {
        PagosController controlador = new PagosController(
                mock(PagoService.class), mock(ReciboPdfService.class), mock(ReportePagosExcelService.class),
                mock(Tareas.class), mock(Notificador.class), mock(Exportador.class), mock(MainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/pagos-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaFormularioDeMantenimiento() throws Exception {
        MantenimientoController controlador = new MantenimientoController(
                mock(MantenimientoService.class), mock(InmuebleService.class),
                mock(ReporteMantenimientoExcelService.class), mock(RentabilidadService.class),
                Clock.systemUTC(), mock(Tareas.class), mock(Notificador.class), mock(Exportador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/mantenimiento-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaMaqueta3d() throws Exception {
        MaquetaController controlador = new MaquetaController(
                mock(MaquetaService.class), mock(Tareas.class), mock(Notificador.class),
                mock(MainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/maqueta-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cadaCargaDeLaMaquetaCreaControladorYSubScenePropios() throws Exception {
        Assumptions.assumeTrue(Platform.isSupported(ConditionalFeature.SCENE3D),
                "La prueba requiere JavaFX 3D");
        try (AnnotationConfigApplicationContext contexto = new AnnotationConfigApplicationContext()) {
            contexto.registerBean(MaquetaService.class, () -> mock(MaquetaService.class));
            contexto.registerBean(Tareas.class, () -> mock(Tareas.class));
            contexto.registerBean(Notificador.class, () -> mock(Notificador.class));
            contexto.registerBean(MainController.class, () -> mock(MainController.class));
            contexto.register(MaquetaController.class);
            contexto.refresh();

            CargaMaqueta primera = enFx(() -> cargarMaqueta(contexto));
            CargaMaqueta segunda = enFx(() -> cargarMaqueta(contexto));

            assertNotSame(primera.controlador(), segunda.controlador());
            assertNotSame(primera.contenedor(), segunda.contenedor());
            assertNotSame(primera.subEscena(), segunda.subEscena());
            assertSame(primera.contenedor(), primera.subEscena().getParent());
            assertSame(segunda.contenedor(), segunda.subEscena().getParent());
        }
    }

    private CargaMaqueta cargarMaqueta(ApplicationContext contexto) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/maqueta-view.fxml"));
        loader.setControllerFactory(contexto::getBean);
        Parent raiz = loader.load();
        StackPane contenedor = buscarContenedor(raiz);
        assertNotNull(contenedor);
        SubScene subEscena = contenedor.getChildren().stream()
                .filter(SubScene.class::isInstance)
                .map(SubScene.class::cast)
                .findFirst().orElse(null);
        assertNotNull(subEscena);
        return new CargaMaqueta((MaquetaController) loader.getController(), contenedor, subEscena);
    }

    private StackPane buscarContenedor(Parent raiz) {
        for (Node nodo : raiz.getChildrenUnmodifiable()) {
            if (nodo instanceof StackPane panel
                    && panel.getStyleClass().contains("maqueta-contenedor")) {
                return panel;
            }
            if (nodo instanceof Parent padre) {
                StackPane encontrado = buscarContenedor(padre);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    private <T> T enFx(Callable<T> accion) throws Exception {
        FutureTask<T> tarea = new FutureTask<>(accion);
        Platform.runLater(tarea);
        return tarea.get(10, TimeUnit.SECONDS);
    }

    private record CargaMaqueta(MaquetaController controlador, StackPane contenedor,
                                SubScene subEscena) {
    }

    @Test void cargaTiposDeInmueble() throws Exception {
        TipoInmuebleController controlador = new TipoInmuebleController(
                mock(TipoInmuebleService.class), mock(Tareas.class), mock(Notificador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/tipo-inmueble-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaPersonas() throws Exception {
        PersonaController controlador = new PersonaController(
                mock(PersonaService.class), mock(Tareas.class), mock(Notificador.class),
                mock(Exportador.class), mock(DatosArrendador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/persona-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test void cargaPropietarios() throws Exception {
        PropietarioController controlador = new PropietarioController(
                mock(PropietarioService.class), mock(Tareas.class), mock(Notificador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/propietario-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }
}

package com.ejemplo.alquileres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.service.Permisos;
import com.ejemplo.alquileres.service.SesionActual;
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
import com.ejemplo.alquileres.ui.RestriccionesUi;
import com.ejemplo.alquileres.ui.Tareas;
import java.time.Clock;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
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

        ComboBox<?> selector = nodos(raiz).stream()
                .filter(ComboBox.class::isInstance)
                .map(ComboBox.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró el selector Ver por"));
        assertEquals(java.util.List.of("Estado", "Mora", "Renta", "Vencimiento"),
                selector.getItems());
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

    @Test void restriccionesDeConsultaYAdmin() throws Exception {
        List<String> vistas = vistasPrincipales();

        for (String fxml : vistas) {
            Parent raiz = cargarConRestricciones(fxml, permisos(RolUsuario.CONSULTA));
            for (Node nodo : nodos(raiz)) {
                if (marcado(nodo, "requiere-gestion") || marcado(nodo, "requiere-admin")) {
                    assertTrue(nodo.isDisabled(), fxml + " / " + texto(nodo));
                }
            }
        }
        for (String fxml : vistas) {
            Parent raiz = cargarConRestricciones(fxml, permisos(RolUsuario.ADMIN));
            for (Node nodo : nodos(raiz)) {
                if (marcado(nodo, "requiere-gestion") || marcado(nodo, "requiere-admin")) {
                    assertFalse(nodo.isDisabled(), fxml + " / " + texto(nodo));
                }
            }
        }

        Parent pagoForm = cargarConRestricciones("/view/pago-form-view.fxml",
                permisos(RolUsuario.CONSULTA));
        assertTrue(boton(pagoForm, "Pagar todo").isDisabled());
        assertTrue(boton(pagoForm, "Aplicar monto").isDisabled());
        assertFalse(boton(pagoForm, "Estado de cuenta").isDisabled());

        Parent inmueble = cargarConRestricciones("/view/inmueble-view.fxml",
                permisos(RolUsuario.CONSULTA));
        assertFalse(boton(inmueble, "Ver ficha").isDisabled());
        assertFalse(boton(inmueble, "Exportar Excel").isDisabled());

        Parent maqueta = cargarConRestricciones("/view/maqueta-view.fxml",
                permisos(RolUsuario.CONSULTA));
        assertFalse(boton(maqueta, "Restablecer vista").isDisabled());

        assertDeshabilitados("/view/pago-form-view.fxml",
                "Pagar todo", "Aplicar monto", "Registrar pago");
        assertDeshabilitados("/view/pagos-view.fxml", "Nuevo pago", "Anular");
        assertDeshabilitados("/view/mantenimiento-view.fxml",
                "Nuevo mantenimiento", "Iniciar", "Resolver", "Cancelar",
                "Generar cargo", "Guardar mantenimiento");
        assertDeshabilitados("/view/persona-view.fxml",
                "Nueva persona", "Editar", "Desactivar", "Reactivar", "Guardar persona");
        assertDeshabilitados("/view/propietario-view.fxml",
                "Nuevo propietario", "Editar", "Eliminar / desactivar", "Reactivar",
                "Guardar propietario");
        assertDeshabilitados("/view/tipo-inmueble-view.fxml",
                "Nuevo tipo", "Editar", "Eliminar / desactivar", "Reactivar", "Guardar tipo");
        assertDeshabilitados("/view/contratos-view.fxml",
                "Nuevo contrato", "Renovar", "Avisar fin", "Finalizar", "Rescindir");
        assertDeshabilitados("/view/contrato-form-view.fxml", "Registrar contrato");
        assertDeshabilitados("/view/inmueble-view.fxml",
                "Nuevo inmueble", "Editar", "Guardar inmueble");
        assertDeshabilitados("/view/cuentas-por-cobrar-view.fxml",
                "Recordar a todos los vencidos", "Recordar", "Cobrar");
        assertDeshabilitados("/view/configuracion-view.fxml",
                "Guardar cambios", "Elegir…", "Quitar", "Restaurar textos por defecto",
                "Crear respaldo ahora", "Restaurar respaldo", "Nuevo usuario", "Editar",
                "Activar / desactivar", "Restablecer contraseña");

        assertHabilitados("/view/pagos-view.fxml", "Filtrar", "Exportar Excel", "Recibo PDF");
        assertHabilitados("/view/contratos-view.fxml", "Estado de cuenta", "Hoja de contrato");
        assertHabilitados("/view/persona-view.fxml", "WhatsApp");
        assertHabilitados("/view/mantenimiento-view.fxml", "Exportar Excel");
        assertHabilitados("/view/inmueble-view.fxml", "Ver ficha", "Exportar Excel");
        assertHabilitados("/view/cuentas-por-cobrar-view.fxml", "Estado de cuenta", "Exportar Excel");
        assertHabilitados("/view/dashboard-view.fxml", "Exportar Excel", "Actualizar");
        assertHabilitados("/view/rentabilidad-view.fxml", "Exportar Excel", "Actualizar");
        assertHabilitados("/view/configuracion-view.fxml", "Exportar Excel", "Buscar", "Limpiar");
        assertHabilitados("/view/pago-form-view.fxml", "Estado de cuenta");
        assertHabilitados("/view/contrato-form-view.fxml", "Cancelar");
        assertHabilitados("/view/maqueta-view.fxml", "Restablecer vista");
    }

    @Test void restriccionesDeOperador() throws Exception {
        Permisos operador = permisos(RolUsuario.OPERADOR);
        for (String fxml : vistasPrincipales()) {
            Parent raiz = cargarConRestricciones(fxml, operador);
            for (Node nodo : nodos(raiz)) {
                if (marcado(nodo, "requiere-admin")) {
                    assertTrue(nodo.isDisabled(), fxml + " / " + texto(nodo));
                }
                if (marcado(nodo, "requiere-gestion")) {
                    assertFalse(nodo.isDisabled(), fxml + " / " + texto(nodo));
                }
            }
        }

        Parent pagos = cargarConRestricciones("/view/pagos-view.fxml", operador);
        assertTrue(boton(pagos, "Anular").isDisabled());
        assertFalse(boton(pagos, "Nuevo pago").isDisabled());
        assertFalse(boton(pagos, "Filtrar").isDisabled());

        Parent contratos = cargarConRestricciones("/view/contratos-view.fxml", operador);
        assertTrue(boton(contratos, "Finalizar").isDisabled());
        assertTrue(boton(contratos, "Rescindir").isDisabled());
        assertFalse(boton(contratos, "Renovar").isDisabled());
        assertFalse(boton(contratos, "Avisar fin").isDisabled());

        Parent pagoForm = cargarConRestricciones("/view/pago-form-view.fxml", operador);
        assertFalse(boton(pagoForm, "Pagar todo").isDisabled());
        assertFalse(boton(pagoForm, "Aplicar monto").isDisabled());
        assertTrue(condonarMora(pagoForm).isDisabled());
        assertTrue(motivoCondonacion(pagoForm).isDisabled());
    }

    private List<String> vistasPrincipales() {
        return List.of(
                "/view/pago-form-view.fxml",
                "/view/pagos-view.fxml",
                "/view/mantenimiento-view.fxml",
                "/view/persona-view.fxml",
                "/view/propietario-view.fxml",
                "/view/tipo-inmueble-view.fxml",
                "/view/contratos-view.fxml",
                "/view/contrato-form-view.fxml",
                "/view/inmueble-view.fxml",
                "/view/cuentas-por-cobrar-view.fxml",
                "/view/dashboard-view.fxml",
                "/view/rentabilidad-view.fxml",
                "/view/configuracion-view.fxml",
                "/view/maqueta-view.fxml");
    }

    private Parent cargarConRestricciones(String fxml, Permisos permisos) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        loader.setControllerFactory(clase -> mock(clase));
        Parent raiz = loader.load();
        RestriccionesUi.aplicar(raiz, permisos);
        return raiz;
    }

    private Permisos permisos(RolUsuario rol) {
        SesionActual sesion = new SesionActual();
        sesion.iniciar(Usuario.builder().id(1L).username("prueba").nombre("Prueba")
                .rol(rol).activo(true).build());
        return new Permisos(sesion);
    }

    private List<Node> nodos(Parent raiz) {
        List<Node> resultado = new ArrayList<>();
        recolectar(raiz, resultado);
        return resultado;
    }

    private void recolectar(Node nodo, List<Node> resultado) {
        if (nodo == null) return;
        resultado.add(nodo);
        if (!(nodo instanceof Parent padre)) return;
        for (Node hijo : padre.getChildrenUnmodifiable()) recolectar(hijo, resultado);
        if (padre instanceof ScrollPane scroll) recolectar(scroll.getContent(), resultado);
        if (padre instanceof TitledPane titled) recolectar(titled.getContent(), resultado);
        if (padre instanceof TabPane tabs) {
            for (Tab tab : tabs.getTabs()) recolectar(tab.getContent(), resultado);
        }
        if (padre instanceof SplitPane split) {
            for (Node item : split.getItems()) recolectar(item, resultado);
        }
        if (padre instanceof Accordion accordion) {
            for (TitledPane panel : accordion.getPanes()) recolectar(panel, resultado);
        }
    }

    private boolean marcado(Node nodo, String clase) {
        return nodo.getStyleClass().contains(clase);
    }

    private String texto(Node nodo) {
        return nodo instanceof Labeled etiquetado && etiquetado.getText() != null
                ? etiquetado.getText() : nodo.getClass().getSimpleName();
    }

    private Button boton(Parent raiz, String texto) {
        return nodos(raiz).stream()
                .filter(nodo -> nodo instanceof Button boton && texto.equals(boton.getText()))
                .map(Button.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró el botón " + texto));
    }

    private Button botonMarcado(Parent raiz, String texto) {
        return nodos(raiz).stream()
                .filter(nodo -> nodo instanceof Button boton && texto.equals(boton.getText())
                        && (marcado(nodo, "requiere-gestion") || marcado(nodo, "requiere-admin")))
                .map(Button.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró marcado el botón " + texto));
    }

    private void assertDeshabilitados(String fxml, String... textos) throws IOException {
        Parent raiz = cargarConRestricciones(fxml, permisos(RolUsuario.CONSULTA));
        for (String texto : textos) {
            Button boton = botonMarcado(raiz, texto);
            assertTrue(boton.isDisabled(), fxml + " / " + texto);
        }
    }

    private void assertHabilitados(String fxml, String... textos) throws IOException {
        Parent raiz = cargarConRestricciones(fxml, permisos(RolUsuario.CONSULTA));
        for (String texto : textos) {
            Button boton = boton(raiz, texto);
            assertFalse(boton.isDisabled(), fxml + " / " + texto);
        }
    }

    private CheckBox condonarMora(Parent raiz) {
        return nodos(raiz).stream()
                .filter(nodo -> nodo instanceof CheckBox check
                        && check.getText() != null && check.getText().contains("Condonar mora"))
                .map(CheckBox.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró Condonar mora"));
    }

    private TextField motivoCondonacion(Parent raiz) {
        return nodos(raiz).stream()
                .filter(nodo -> nodo instanceof TextField campo
                        && "Motivo de condonación".equals(campo.getPromptText()))
                .map(TextField.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró Motivo de condonación"));
    }
}

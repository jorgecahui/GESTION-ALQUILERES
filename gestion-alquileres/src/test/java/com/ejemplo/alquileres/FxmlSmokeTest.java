package com.ejemplo.alquileres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.ejemplo.alquileres.config.DatosArrendador;
import com.ejemplo.alquileres.controller.JlccContratoController;
import com.ejemplo.alquileres.controller.JlccContratoFormController;
import com.ejemplo.alquileres.controller.JlccFichaInmuebleController;
import com.ejemplo.alquileres.controller.JlccInmuebleController;
import com.ejemplo.alquileres.controller.JlccMainController;
import com.ejemplo.alquileres.controller.JlccMantenimientoController;
import com.ejemplo.alquileres.controller.JlccMaquetaController;
import com.ejemplo.alquileres.controller.JlccPagosController;
import com.ejemplo.alquileres.controller.JlccPersonaController;
import com.ejemplo.alquileres.controller.JlccPropietarioController;
import com.ejemplo.alquileres.controller.JlccTipoInmuebleController;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.service.JlccConfiguracionService;
import com.ejemplo.alquileres.service.JlccContratoPdfService;
import com.ejemplo.alquileres.service.JlccContratoService;
import com.ejemplo.alquileres.service.JlccCronogramaGenerador;
import com.ejemplo.alquileres.service.JlccEstadoCuentaPdfService;
import com.ejemplo.alquileres.service.JlccInmuebleService;
import com.ejemplo.alquileres.service.JlccMantenimientoService;
import com.ejemplo.alquileres.service.JlccMaquetaService;
import com.ejemplo.alquileres.service.JlccPagoService;
import com.ejemplo.alquileres.service.JlccPermisos;
import com.ejemplo.alquileres.service.JlccPersonaService;
import com.ejemplo.alquileres.service.JlccPropietarioService;
import com.ejemplo.alquileres.service.JlccReciboPdfService;
import com.ejemplo.alquileres.service.JlccRecordatorioService;
import com.ejemplo.alquileres.service.JlccRentabilidadService;
import com.ejemplo.alquileres.service.JlccReporteContratosExcelService;
import com.ejemplo.alquileres.service.JlccReporteInmueblesExcelService;
import com.ejemplo.alquileres.service.JlccReporteMantenimientoExcelService;
import com.ejemplo.alquileres.service.JlccReportePagosExcelService;
import com.ejemplo.alquileres.service.JlccSesionActual;
import com.ejemplo.alquileres.service.JlccTipoInmuebleService;
import com.ejemplo.alquileres.ui.JlccExportador;
import com.ejemplo.alquileres.ui.JlccNotificador;
import com.ejemplo.alquileres.ui.JlccRestriccionesUi;
import com.ejemplo.alquileres.ui.JlccTareas;

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

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

class FxmlSmokeTest {
    @BeforeAll
    static void iniciar() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignorado) {
        }
    }

    @AfterAll
    static void cerrar() {
        Platform.exit();
    }

    @Test
    void cargaFormularioDeContrato() throws Exception {
        JlccContratoFormController controlador =
                new JlccContratoFormController(
                        mock(JlccContratoService.class),
                        mock(JlccInmuebleService.class),
                        mock(JlccPersonaService.class),
                        new JlccCronogramaGenerador(),
                        mock(JlccConfiguracionService.class),
                        mock(JlccTareas.class),
                        mock(JlccNotificador.class),
                        mock(JlccMainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/contrato-form-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test
    void cargaListaDeContratos() throws Exception {
        JlccContratoController controlador =
                new JlccContratoController(
                        mock(JlccContratoService.class),
                        mock(JlccRecordatorioService.class),
                        mock(JlccEstadoCuentaPdfService.class),
                        mock(JlccContratoPdfService.class),
                        mock(JlccReporteContratosExcelService.class),
                        mock(JlccTareas.class),
                        mock(JlccNotificador.class),
                        mock(JlccExportador.class),
                        mock(JlccMainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/contratos-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test
    void cargaFichaDeInmueble() throws Exception {
        JlccInmuebleController controlador =
                new JlccInmuebleController(
                        mock(JlccInmuebleService.class),
                        mock(JlccTipoInmuebleService.class),
                        mock(JlccPropietarioService.class),
                        mock(JlccReporteInmueblesExcelService.class),
                        mock(JlccFichaInmuebleController.class),
                        mock(JlccContratoService.class),
                        mock(JlccTareas.class),
                        mock(JlccNotificador.class),
                        mock(JlccExportador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/inmueble-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test
    void cargaListaDePagos() throws Exception {
        JlccPagosController controlador =
                new JlccPagosController(
                        mock(JlccPagoService.class),
                        mock(JlccReciboPdfService.class),
                        mock(JlccReportePagosExcelService.class),
                        mock(JlccTareas.class),
                        mock(JlccNotificador.class),
                        mock(JlccExportador.class),
                        mock(JlccMainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/pagos-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test
    void cargaFormularioDeMantenimiento() throws Exception {
        JlccMantenimientoController controlador =
                new JlccMantenimientoController(
                        mock(JlccMantenimientoService.class),
                        mock(JlccInmuebleService.class),
                        mock(JlccReporteMantenimientoExcelService.class),
                        mock(JlccRentabilidadService.class),
                        Clock.systemUTC(),
                        mock(JlccTareas.class),
                        mock(JlccNotificador.class),
                        mock(JlccExportador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/mantenimiento-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test
    void cargaMaqueta3d() throws Exception {
        JlccMaquetaController controlador =
                new JlccMaquetaController(
                        mock(JlccMaquetaService.class),
                        mock(JlccTareas.class),
                        mock(JlccNotificador.class),
                        mock(JlccMainController.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/maqueta-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);

        ComboBox<?> selector =
                nodos(raiz).stream()
                        .filter(ComboBox.class::isInstance)
                        .map(ComboBox.class::cast)
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError("No se encontró el selector Ver por"));
        assertEquals(
                java.util.List.of("Estado", "Mora", "Renta", "Vencimiento"), selector.getItems());
    }

    @Test
    void cadaCargaDeLaMaquetaCreaControladorYSubScenePropios() throws Exception {
        Assumptions.assumeTrue(
                Platform.isSupported(ConditionalFeature.SCENE3D), "La prueba requiere JavaFX 3D");
        try (AnnotationConfigApplicationContext contexto =
                new AnnotationConfigApplicationContext()) {
            contexto.registerBean(JlccMaquetaService.class, () -> mock(JlccMaquetaService.class));
            contexto.registerBean(JlccTareas.class, () -> mock(JlccTareas.class));
            contexto.registerBean(JlccNotificador.class, () -> mock(JlccNotificador.class));
            contexto.registerBean(JlccMainController.class, () -> mock(JlccMainController.class));
            contexto.register(JlccMaquetaController.class);
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
        SubScene subEscena =
                contenedor.getChildren().stream()
                        .filter(SubScene.class::isInstance)
                        .map(SubScene.class::cast)
                        .findFirst()
                        .orElse(null);
        assertNotNull(subEscena);
        return new CargaMaqueta((JlccMaquetaController) loader.getController(), contenedor, subEscena);
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

    private record CargaMaqueta(
            JlccMaquetaController controlador, StackPane contenedor, SubScene subEscena) {}

    @Test
    void cargaTiposDeInmueble() throws Exception {
        JlccTipoInmuebleController controlador =
                new JlccTipoInmuebleController(
                        mock(JlccTipoInmuebleService.class),
                        mock(JlccTareas.class),
                        mock(JlccNotificador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/tipo-inmueble-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test
    void cargaPersonas() throws Exception {
        JlccPersonaController controlador =
                new JlccPersonaController(
                        mock(JlccPersonaService.class),
                        mock(JlccTareas.class),
                        mock(JlccNotificador.class),
                        mock(JlccExportador.class),
                        mock(DatosArrendador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/persona-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test
    void cargaPropietarios() throws Exception {
        JlccPropietarioController controlador =
                new JlccPropietarioController(
                        mock(JlccPropietarioService.class),
                        mock(JlccTareas.class),
                        mock(JlccNotificador.class));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/propietario-view.fxml"));
        loader.setControllerFactory(clase -> controlador);
        Parent raiz = loader.load();
        assertNotNull(raiz);
    }

    @Test
    void restriccionesDeConsultaYAdmin() throws Exception {
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

        Parent pagoForm =
                cargarConRestricciones("/view/pago-form-view.fxml", permisos(RolUsuario.CONSULTA));
        assertTrue(boton(pagoForm, "Pagar todo").isDisabled());
        assertTrue(boton(pagoForm, "Aplicar monto").isDisabled());
        assertFalse(boton(pagoForm, "Estado de cuenta").isDisabled());

        Parent inmueble =
                cargarConRestricciones("/view/inmueble-view.fxml", permisos(RolUsuario.CONSULTA));
        assertFalse(boton(inmueble, "Ver ficha").isDisabled());
        assertFalse(boton(inmueble, "Exportar Excel").isDisabled());

        Parent maqueta =
                cargarConRestricciones("/view/maqueta-view.fxml", permisos(RolUsuario.CONSULTA));
        assertFalse(boton(maqueta, "Restablecer vista").isDisabled());

        assertDeshabilitados(
                "/view/pago-form-view.fxml", "Pagar todo", "Aplicar monto", "Registrar pago");
        assertDeshabilitados("/view/pagos-view.fxml", "Nuevo pago", "Anular");
        assertDeshabilitados(
                "/view/mantenimiento-view.fxml",
                "Nuevo mantenimiento",
                "Iniciar",
                "Resolver",
                "Cancelar",
                "Generar cargo",
                "Guardar mantenimiento");
        assertDeshabilitados(
                "/view/persona-view.fxml",
                "Nueva persona",
                "Editar",
                "Desactivar",
                "Reactivar",
                "Guardar persona");
        assertDeshabilitados(
                "/view/propietario-view.fxml",
                "Nuevo propietario",
                "Editar",
                "Eliminar / desactivar",
                "Reactivar",
                "Guardar propietario");
        assertDeshabilitados(
                "/view/tipo-inmueble-view.fxml",
                "Nuevo tipo",
                "Editar",
                "Eliminar / desactivar",
                "Reactivar",
                "Guardar tipo");
        assertDeshabilitados(
                "/view/contratos-view.fxml",
                "Nuevo contrato",
                "Renovar",
                "Avisar fin",
                "Finalizar",
                "Rescindir");
        assertDeshabilitados("/view/contrato-form-view.fxml", "Registrar contrato");
        assertDeshabilitados(
                "/view/inmueble-view.fxml", "Nuevo inmueble", "Editar", "Guardar inmueble");
        assertDeshabilitados(
                "/view/cuentas-por-cobrar-view.fxml",
                "Recordar a todos los vencidos",
                "Recordar",
                "Cobrar");
        assertDeshabilitados(
                "/view/configuracion-view.fxml",
                "Guardar cambios",
                "Elegir…",
                "Quitar",
                "Restaurar textos por defecto",
                "Crear respaldo ahora",
                "Restaurar respaldo",
                "Nuevo usuario",
                "Editar",
                "Activar / desactivar",
                "Restablecer contraseña");

        assertHabilitados("/view/pagos-view.fxml", "Filtrar", "Exportar Excel", "Recibo PDF");
        assertHabilitados("/view/contratos-view.fxml", "Estado de cuenta", "Hoja de contrato");
        assertHabilitados("/view/persona-view.fxml", "WhatsApp");
        assertHabilitados("/view/mantenimiento-view.fxml", "Exportar Excel");
        assertHabilitados("/view/inmueble-view.fxml", "Ver ficha", "Exportar Excel");
        assertHabilitados(
                "/view/cuentas-por-cobrar-view.fxml", "Estado de cuenta", "Exportar Excel");
        assertHabilitados("/view/dashboard-view.fxml", "Exportar Excel", "Actualizar");
        assertHabilitados("/view/rentabilidad-view.fxml", "Exportar Excel", "Actualizar");
        assertHabilitados("/view/configuracion-view.fxml", "Exportar Excel", "Buscar", "Limpiar");
        assertHabilitados("/view/pago-form-view.fxml", "Estado de cuenta");
        assertHabilitados("/view/contrato-form-view.fxml", "Cancelar");
        assertHabilitados("/view/maqueta-view.fxml", "Restablecer vista");
    }

    @Test
    void restriccionesDeOperador() throws Exception {
        JlccPermisos operador = permisos(RolUsuario.OPERADOR);
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

    private Parent cargarConRestricciones(String fxml, JlccPermisos permisos) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        loader.setControllerFactory(clase -> mock(clase));
        Parent raiz = loader.load();
        JlccRestriccionesUi.jlccAplicar(raiz, permisos);
        return raiz;
    }

    private JlccPermisos permisos(RolUsuario rol) {
        JlccSesionActual sesion = new JlccSesionActual();
        sesion.jlccIniciar(
                Usuario.builder()
                        .id(1L)
                        .username("prueba")
                        .nombre("Prueba")
                        .rol(rol)
                        .activo(true)
                        .build());
        return new JlccPermisos(sesion);
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
                ? etiquetado.getText()
                : nodo.getClass().getSimpleName();
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
                .filter(
                        nodo ->
                                nodo instanceof Button boton
                                        && texto.equals(boton.getText())
                                        && (marcado(nodo, "requiere-gestion")
                                                || marcado(nodo, "requiere-admin")))
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
                .filter(
                        nodo ->
                                nodo instanceof CheckBox check
                                        && check.getText() != null
                                        && check.getText().contains("Condonar mora"))
                .map(CheckBox.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró Condonar mora"));
    }

    private TextField motivoCondonacion(Parent raiz) {
        return nodos(raiz).stream()
                .filter(
                        nodo ->
                                nodo instanceof TextField campo
                                        && "Motivo de condonación".equals(campo.getPromptText()))
                .map(TextField.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró Motivo de condonación"));
    }
}

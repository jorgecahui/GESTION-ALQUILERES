package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.*;
import com.ejemplo.alquileres.ui.*;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class JlccMantenimientoController implements JlccRefrescable {
    private final JlccMantenimientoService jlccService;
    private final JlccInmuebleService jlccInmuebles;
    private final JlccReporteMantenimientoExcelService jlccExcel;
    private final JlccRentabilidadService jlccRentabilidad;
    private final Clock jlccClock;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccExportador jlccExportador;
    private final ObservableList<Mantenimiento> jlccDatos = FXCollections.observableArrayList();
    private List<Mantenimiento> jlccVisibles = List.of();

    @FXML private TextField buscar, titulo, costo;
    @FXML private DatePicker fechaReporte, fechaProgramada;
    @FXML private ComboBox<EstadoMantenimiento> filtroEstado;
    @FXML private ComboBox<PrioridadMantenimiento> filtroPrioridad;
    @FXML private ComboBox<CategoriaMantenimiento> filtroCategoria, categoria;
    @FXML private ComboBox<ResponsablePago> responsable;
    @FXML private ComboBox<Inmueble> inmueble;
    @FXML private TextArea descripcion, observaciones;
    @FXML private VBox drawer;
    @FXML private Region overlay;
    @FXML private HBox campoCosto;
    @FXML private ToggleGroup grupoPrioridad;
    @FXML private ToggleButton prioridadBaja, prioridadMedia, prioridadAlta, prioridadUrgente;
    @FXML private Label errorInmueble, errorTitulo, errorCategoria;
    @FXML private TableView<Mantenimiento> tabla;
    @FXML
    private TableColumn<Mantenimiento, String> colCodigo,
            colInmueble,
            colTitulo,
            colCategoria,
            colPrioridad,
            colEstado,
            colResponsable,
            colFecha;
    @FXML private TableColumn<Mantenimiento, BigDecimal> colCosto;
    @FXML private Label abiertos, urgentes, enProceso, gastoMes;

    public JlccMantenimientoController(
            JlccMantenimientoService s,
            JlccInmuebleService i,
            JlccReporteMantenimientoExcelService e,
            JlccRentabilidadService r,
            Clock c,
            JlccTareas t,
            JlccNotificador a,
            JlccExportador x) {
        jlccService = s;
        jlccInmuebles = i;
        jlccExcel = e;
        jlccRentabilidad = r;
        jlccClock = c;
        jlccTareas = t;
        jlccAviso = a;
        jlccExportador = x;
    }

    @FXML
    public void initialize() {
        filtroEstado.setItems(FXCollections.observableArrayList(EstadoMantenimiento.values()));
        filtroPrioridad.setItems(
                FXCollections.observableArrayList(PrioridadMantenimiento.values()));
        filtroCategoria.setItems(
                FXCollections.observableArrayList(CategoriaMantenimiento.values()));
        categoria.setItems(FXCollections.observableArrayList(CategoriaMantenimiento.values()));
        responsable.setItems(FXCollections.observableArrayList(ResponsablePago.values()));
        inmueble.setConverter(
                new javafx.util.StringConverter<>() {
                    public String toString(Inmueble v) {
                        return v == null ? "" : v.getCodigo() + " · " + v.getNombre();
                    }

                    public Inmueble fromString(String t) {
                        return null;
                    }
                });

        prioridadMedia.setSelected(true);
        grupoPrioridad
                .selectedToggleProperty()
                .addListener(
                        (o, anterior, actual) -> {
                            if (actual == null)
                                grupoPrioridad.selectToggle(
                                        anterior == null ? prioridadMedia : anterior);
                        });

        inmueble.valueProperty().addListener((o, a, b) -> jlccOcultarError(errorInmueble));
        titulo.textProperty().addListener((o, a, b) -> jlccOcultarError(errorTitulo));
        categoria.valueProperty().addListener((o, a, b) -> jlccOcultarError(errorCategoria));
        costo.focusedProperty()
                .addListener(
                        (o, a, b) -> {
                            if (b) campoCosto.getStyleClass().add("focused");
                            else campoCosto.getStyleClass().remove("focused");
                        });

        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colInmueble.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getInmueble().getNombre()));
        colTitulo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitulo()));
        colCategoria.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getCategoria().getEtiqueta()));
        colPrioridad.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getPrioridad().getEtiqueta()));
        colEstado.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getEstado().getEtiqueta()));
        colResponsable.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getResponsablePago().getEtiqueta()));
        colFecha.setCellValueFactory(
                c -> new SimpleStringProperty(JlccFechas.jlccDia(c.getValue().getFechaReporte())));
        colCosto.setCellValueFactory(
                c ->
                        new SimpleObjectProperty<>(
                                c.getValue().getCostoReal() == null
                                        ? c.getValue().getCostoEstimado()
                                        : c.getValue().getCostoReal()));
        colCosto.setCellFactory(x -> jlccMoneda());
        tabla.setItems(jlccDatos);
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        filtroEstado.valueProperty().addListener((o, a, b) -> refrescar());
        filtroPrioridad.valueProperty().addListener((o, a, b) -> refrescar());
        filtroCategoria.valueProperty().addListener((o, a, b) -> refrescar());
    }

    @Override
    public void refrescar() {
        String jlccTexto = buscar.getText();
        EstadoMantenimiento jlccEstadoFiltro = filtroEstado.getValue();
        PrioridadMantenimiento jlccPrioridadFiltro = filtroPrioridad.getValue();
        CategoriaMantenimiento jlccCategoriaFiltro = filtroCategoria.getValue();
        jlccTareas.jlccEjecutar(
                () ->
                        new Object[] {
                            jlccService.jlccListar(
                                    jlccTexto,
                                    jlccEstadoFiltro,
                                    jlccPrioridadFiltro,
                                    null,
                                    jlccCategoriaFiltro,
                                    0,
                                    100),
                            jlccService.jlccResumen()
                        },
                x -> {
                    Page<Mantenimiento> p = (Page<Mantenimiento>) x[0];
                    jlccVisibles = p.getContent();
                    jlccDatos.setAll(jlccVisibles);
                    JlccMantenimientoService.Resumen r = (JlccMantenimientoService.Resumen) x[1];
                    abiertos.setText(String.valueOf(r.abiertos()));
                    urgentes.setText(String.valueOf(r.urgentes()));
                    enProceso.setText(String.valueOf(r.enProceso()));
                    gastoMes.setText(JlccTablas.jlccMoneda(r.gastoMes()));
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    @FXML
    private void jlccOnNuevo() {
        jlccLimpiarErrores();
        titulo.clear();
        descripcion.clear();
        costo.clear();
        observaciones.clear();
        inmueble.setValue(null);
        fechaReporte.setValue(LocalDate.now(jlccClock));
        fechaProgramada.setValue(null);
        jlccSeleccionarPrioridad(PrioridadMantenimiento.MEDIA);
        categoria.setValue(CategoriaMantenimiento.OTROS);
        responsable.setValue(ResponsablePago.PROPIETARIO);
        jlccAbrir();
        jlccTareas.jlccEjecutar(
                () -> jlccInmuebles.jlccListar("", null, null, 0, 200),
                p -> inmueble.setItems(FXCollections.observableArrayList(p.getContent())),
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    @FXML
    private void jlccOnGuardar() {
        jlccLimpiarErrores();
        if (!jlccValidarFormulario()) return;
        try {
            JlccMantenimientoService.Solicitud s =
                    new JlccMantenimientoService.Solicitud(
                            inmueble.getValue() == null ? null : inmueble.getValue().getId(),
                            null,
                            fechaReporte.getValue(),
                            titulo.getText(),
                            descripcion.getText(),
                            categoria.getValue(),
                            jlccPrioridadSeleccionada(),
                            responsable.getValue(),
                            null,
                            null,
                            jlccDecimal(costo.getText()),
                            fechaProgramada.getValue(),
                            observaciones.getText());
            jlccTareas.jlccEjecutar(
                    () -> jlccService.jlccRegistrar(s),
                    x -> {
                        jlccCerrar();
                        jlccAviso.jlccInfo("Mantenimiento registrado.");
                        refrescar();
                    },
                    e -> jlccAviso.jlccError(jlccMsg(e)));
        } catch (NumberFormatException e) {
            jlccAviso.jlccError("El costo debe ser un número válido.");
        }
    }

    private boolean jlccValidarFormulario() {
        boolean jlccValido = true;
        if (inmueble.getValue() == null) {
            jlccMostrarError(errorInmueble, "Selecciona un inmueble");
            jlccValido = false;
        }
        if (titulo.getText() == null || titulo.getText().isBlank()) {
            jlccMostrarError(errorTitulo, "Ingresa un título");
            jlccValido = false;
        }
        if (categoria.getValue() == null) {
            jlccMostrarError(errorCategoria, "Selecciona una categoría");
            jlccValido = false;
        }
        return jlccValido;
    }

    private void jlccMostrarError(Label jlccError, String jlccTexto) {
        if (jlccError == null) return;
        jlccError.setText(jlccTexto);
        jlccError.setVisible(true);
        jlccError.setManaged(true);
    }

    private void jlccOcultarError(Label jlccError) {
        if (jlccError == null) return;
        jlccError.setText("");
        jlccError.setVisible(false);
        jlccError.setManaged(false);
    }

    private void jlccLimpiarErrores() {
        jlccOcultarError(errorInmueble);
        jlccOcultarError(errorTitulo);
        jlccOcultarError(errorCategoria);
    }

    private PrioridadMantenimiento jlccPrioridadSeleccionada() {
        Toggle jlccSeleccion = grupoPrioridad.getSelectedToggle();
        if (jlccSeleccion == prioridadBaja) return PrioridadMantenimiento.BAJA;
        if (jlccSeleccion == prioridadAlta) return PrioridadMantenimiento.ALTA;
        if (jlccSeleccion == prioridadUrgente) return PrioridadMantenimiento.URGENTE;
        return PrioridadMantenimiento.MEDIA;
    }

    private void jlccSeleccionarPrioridad(PrioridadMantenimiento jlccPrioridad) {
        switch (jlccPrioridad == null ? PrioridadMantenimiento.MEDIA : jlccPrioridad) {
            case BAJA -> prioridadBaja.setSelected(true);
            case MEDIA -> prioridadMedia.setSelected(true);
            case ALTA -> prioridadAlta.setSelected(true);
            case URGENTE -> prioridadUrgente.setSelected(true);
        }
    }

    @FXML
    private void jlccOnIniciar() {
        Mantenimiento m = jlccSeleccion();
        if (m == null) return;
        CheckBox c = new CheckBox("Poner el inmueble en mantenimiento");
        new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Iniciar " + m.getCodigo() + "?",
                ButtonType.OK,
                ButtonType.CANCEL) {
            {
                getDialogPane().setContent(c);
            }
        }.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(
                        b -> {
                            boolean jlccPoner = c.isSelected();
                            jlccTareas.jlccEjecutar(
                                    () -> jlccService.jlccIniciar(m.getId(), jlccPoner),
                                    x -> {
                                        jlccAviso.jlccInfo("Mantenimiento iniciado.");
                                        refrescar();
                                    },
                                    e -> jlccAviso.jlccError(jlccMsg(e)));
                        });
    }

    @FXML
    private void jlccOnResolver() {
        Mantenimiento m = jlccSeleccion();
        if (m == null) return;
        DatePicker f = new DatePicker(LocalDate.now(jlccClock));
        TextField v = new TextField();
        v.setPromptText("Costo real");
        VBox jlccBox =
                new VBox(8, new Label("Fecha de resolución"), f, new Label("Costo real (S/)"), v);
        Alert d = new Alert(Alert.AlertType.CONFIRMATION);
        d.setHeaderText("Resolver " + m.getCodigo());
        d.getDialogPane().setContent(jlccBox);
        d.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(
                        b -> {
                            try {
                                LocalDate jlccFechaResolucion = f.getValue();
                                BigDecimal jlccCostoReal = jlccDecimal(v.getText());
                                jlccTareas.jlccEjecutar(
                                        () ->
                                                jlccService.jlccResolver(
                                                        m.getId(), jlccFechaResolucion, jlccCostoReal),
                                        x -> {
                                            jlccAviso.jlccInfo("Mantenimiento resuelto.");
                                            refrescar();
                                        },
                                        e -> jlccAviso.jlccError(jlccMsg(e)));
                            } catch (NumberFormatException e) {
                                jlccAviso.jlccError("El costo debe ser válido.");
                            }
                        });
    }

    @FXML
    private void jlccOnCancelar() {
        Mantenimiento m = jlccSeleccion();
        if (m == null) return;
        if (!JlccDialogoConfirmacion.jlccMostrar(
                jlccVentana(),
                "Cancelar mantenimiento",
                "Se cancelará el mantenimiento «"
                        + m.getCodigo()
                        + "». "
                        + "Se anulará la cuota de cargo si no tiene pagos y el inmueble "
                        + "volverá a su estado anterior.",
                "Cancelar mantenimiento")) {
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccCancelar(m.getId()),
                x -> {
                    jlccAviso.jlccInfo("Mantenimiento cancelado.");
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    @FXML
    private void jlccOnCargo() {
        Mantenimiento m = jlccSeleccion();
        if (m == null) return;
        DatePicker f = new DatePicker(LocalDate.now(jlccClock));
        Alert d = new Alert(Alert.AlertType.CONFIRMATION);
        d.setHeaderText("Vencimiento del cargo");
        d.getDialogPane().setContent(f);
        d.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(
                        b -> {
                            LocalDate jlccVencimiento = f.getValue();
                            jlccTareas.jlccEjecutar(
                                    () -> jlccService.jlccGenerarCargo(m.getId(), jlccVencimiento),
                                    x -> {
                                        jlccAviso.jlccInfo("Cargo generado en Cuentas por cobrar.");
                                        refrescar();
                                    },
                                    e -> jlccAviso.jlccError(jlccMsg(e)));
                        });
    }

    @FXML
    private void jlccOnExportar() {
        java.io.File jlccDestino =
                jlccExportador.jlccElegirDestino(
                        tabla.getScene() == null ? null : tabla.getScene().getWindow(),
                        "reporte-mantenimiento-" + java.time.LocalDate.now() + ".xlsx",
                        "Excel",
                        "xlsx");
        if (jlccDestino == null) return;
        Periodo.Rango r = Periodo.ESTE_ANIO.resolver(jlccClock, null, null);
        jlccTareas.jlccEjecutar(
                () ->
                        jlccExcel.jlccGenerar(
                                jlccVisibles,
                                jlccRentabilidad.jlccPorInmueble(r.desde(), r.hasta()),
                                r.desde(),
                                r.hasta()),
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("Excel generado.");
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    @FXML
    private void jlccOnCerrar() {
        jlccCerrar();
    }

    private void jlccAbrir() {
        drawer.setVisible(true);
        drawer.setManaged(true);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    private void jlccCerrar() {
        drawer.setVisible(false);
        drawer.setManaged(false);
        overlay.setVisible(false);
        overlay.setManaged(false);
        jlccLimpiarErrores();
    }

    private Mantenimiento jlccSeleccion() {
        Mantenimiento m = tabla.getSelectionModel().getSelectedItem();
        if (m == null) jlccAviso.jlccError("Selecciona un mantenimiento.");
        return m;
    }

    private Window jlccVentana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    private TableCell<Mantenimiento, BigDecimal> jlccMoneda() {
        return new TableCell<>() {
            protected void updateItem(BigDecimal v, boolean e) {
                super.updateItem(v, e);
                setText(e || v == null ? "" : JlccTablas.jlccMoneda(v));
            }
        };
    }

    private BigDecimal jlccDecimal(String v) {
        return v == null || v.isBlank() ? null : new BigDecimal(v.trim().replace(',', '.'));
    }

    private String jlccMsg(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }
}

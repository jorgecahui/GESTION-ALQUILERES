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
public class MantenimientoController implements Refrescable {
    private final MantenimientoService service;
    private final InmuebleService inmuebles;
    private final ReporteMantenimientoExcelService excel;
    private final RentabilidadService rentabilidad;
    private final Clock clock;
    private final Tareas tareas;
    private final Notificador aviso;
    private final Exportador exportador;
    private final ObservableList<Mantenimiento> datos = FXCollections.observableArrayList();
    private List<Mantenimiento> visibles = List.of();

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

    public MantenimientoController(
            MantenimientoService s,
            InmuebleService i,
            ReporteMantenimientoExcelService e,
            RentabilidadService r,
            Clock c,
            Tareas t,
            Notificador a,
            Exportador x) {
        service = s;
        inmuebles = i;
        excel = e;
        rentabilidad = r;
        clock = c;
        tareas = t;
        aviso = a;
        exportador = x;
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

        inmueble.valueProperty().addListener((o, a, b) -> ocultarError(errorInmueble));
        titulo.textProperty().addListener((o, a, b) -> ocultarError(errorTitulo));
        categoria.valueProperty().addListener((o, a, b) -> ocultarError(errorCategoria));
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
                c -> new SimpleStringProperty(Fechas.dia(c.getValue().getFechaReporte())));
        colCosto.setCellValueFactory(
                c ->
                        new SimpleObjectProperty<>(
                                c.getValue().getCostoReal() == null
                                        ? c.getValue().getCostoEstimado()
                                        : c.getValue().getCostoReal()));
        colCosto.setCellFactory(x -> moneda());
        tabla.setItems(datos);
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        filtroEstado.valueProperty().addListener((o, a, b) -> refrescar());
        filtroPrioridad.valueProperty().addListener((o, a, b) -> refrescar());
        filtroCategoria.valueProperty().addListener((o, a, b) -> refrescar());
    }

    @Override
    public void refrescar() {
        String texto = buscar.getText();
        EstadoMantenimiento estadoFiltro = filtroEstado.getValue();
        PrioridadMantenimiento prioridadFiltro = filtroPrioridad.getValue();
        CategoriaMantenimiento categoriaFiltro = filtroCategoria.getValue();
        tareas.ejecutar(
                () ->
                        new Object[] {
                            service.listar(
                                    texto,
                                    estadoFiltro,
                                    prioridadFiltro,
                                    null,
                                    categoriaFiltro,
                                    0,
                                    100),
                            service.resumen()
                        },
                x -> {
                    Page<Mantenimiento> p = (Page<Mantenimiento>) x[0];
                    visibles = p.getContent();
                    datos.setAll(visibles);
                    MantenimientoService.Resumen r = (MantenimientoService.Resumen) x[1];
                    abiertos.setText(String.valueOf(r.abiertos()));
                    urgentes.setText(String.valueOf(r.urgentes()));
                    enProceso.setText(String.valueOf(r.enProceso()));
                    gastoMes.setText(Tablas.moneda(r.gastoMes()));
                },
                e -> aviso.error(msg(e)));
    }

    @FXML
    private void onNuevo() {
        limpiarErrores();
        titulo.clear();
        descripcion.clear();
        costo.clear();
        observaciones.clear();
        inmueble.setValue(null);
        fechaReporte.setValue(LocalDate.now(clock));
        fechaProgramada.setValue(null);
        seleccionarPrioridad(PrioridadMantenimiento.MEDIA);
        categoria.setValue(CategoriaMantenimiento.OTROS);
        responsable.setValue(ResponsablePago.PROPIETARIO);
        abrir();
        tareas.ejecutar(
                () -> inmuebles.listar("", null, null, 0, 200),
                p -> inmueble.setItems(FXCollections.observableArrayList(p.getContent())),
                e -> aviso.error(msg(e)));
    }

    @FXML
    private void onGuardar() {
        limpiarErrores();
        if (!validarFormulario()) return;
        try {
            MantenimientoService.Solicitud s =
                    new MantenimientoService.Solicitud(
                            inmueble.getValue() == null ? null : inmueble.getValue().getId(),
                            null,
                            fechaReporte.getValue(),
                            titulo.getText(),
                            descripcion.getText(),
                            categoria.getValue(),
                            prioridadSeleccionada(),
                            responsable.getValue(),
                            null,
                            null,
                            decimal(costo.getText()),
                            fechaProgramada.getValue(),
                            observaciones.getText());
            tareas.ejecutar(
                    () -> service.registrar(s),
                    x -> {
                        cerrar();
                        aviso.info("Mantenimiento registrado.");
                        refrescar();
                    },
                    e -> aviso.error(msg(e)));
        } catch (NumberFormatException e) {
            aviso.error("El costo debe ser un número válido.");
        }
    }

    private boolean validarFormulario() {
        boolean valido = true;
        if (inmueble.getValue() == null) {
            mostrarError(errorInmueble, "Selecciona un inmueble");
            valido = false;
        }
        if (titulo.getText() == null || titulo.getText().isBlank()) {
            mostrarError(errorTitulo, "Ingresa un título");
            valido = false;
        }
        if (categoria.getValue() == null) {
            mostrarError(errorCategoria, "Selecciona una categoría");
            valido = false;
        }
        return valido;
    }

    private void mostrarError(Label error, String texto) {
        if (error == null) return;
        error.setText(texto);
        error.setVisible(true);
        error.setManaged(true);
    }

    private void ocultarError(Label error) {
        if (error == null) return;
        error.setText("");
        error.setVisible(false);
        error.setManaged(false);
    }

    private void limpiarErrores() {
        ocultarError(errorInmueble);
        ocultarError(errorTitulo);
        ocultarError(errorCategoria);
    }

    private PrioridadMantenimiento prioridadSeleccionada() {
        Toggle seleccion = grupoPrioridad.getSelectedToggle();
        if (seleccion == prioridadBaja) return PrioridadMantenimiento.BAJA;
        if (seleccion == prioridadAlta) return PrioridadMantenimiento.ALTA;
        if (seleccion == prioridadUrgente) return PrioridadMantenimiento.URGENTE;
        return PrioridadMantenimiento.MEDIA;
    }

    private void seleccionarPrioridad(PrioridadMantenimiento prioridad) {
        switch (prioridad == null ? PrioridadMantenimiento.MEDIA : prioridad) {
            case BAJA -> prioridadBaja.setSelected(true);
            case MEDIA -> prioridadMedia.setSelected(true);
            case ALTA -> prioridadAlta.setSelected(true);
            case URGENTE -> prioridadUrgente.setSelected(true);
        }
    }

    @FXML
    private void onIniciar() {
        Mantenimiento m = seleccion();
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
                            boolean poner = c.isSelected();
                            tareas.ejecutar(
                                    () -> service.iniciar(m.getId(), poner),
                                    x -> {
                                        aviso.info("Mantenimiento iniciado.");
                                        refrescar();
                                    },
                                    e -> aviso.error(msg(e)));
                        });
    }

    @FXML
    private void onResolver() {
        Mantenimiento m = seleccion();
        if (m == null) return;
        DatePicker f = new DatePicker(LocalDate.now(clock));
        TextField v = new TextField();
        v.setPromptText("Costo real");
        VBox box =
                new VBox(8, new Label("Fecha de resolución"), f, new Label("Costo real (S/)"), v);
        Alert d = new Alert(Alert.AlertType.CONFIRMATION);
        d.setHeaderText("Resolver " + m.getCodigo());
        d.getDialogPane().setContent(box);
        d.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(
                        b -> {
                            try {
                                LocalDate fechaResolucion = f.getValue();
                                BigDecimal costoReal = decimal(v.getText());
                                tareas.ejecutar(
                                        () ->
                                                service.resolver(
                                                        m.getId(), fechaResolucion, costoReal),
                                        x -> {
                                            aviso.info("Mantenimiento resuelto.");
                                            refrescar();
                                        },
                                        e -> aviso.error(msg(e)));
                            } catch (NumberFormatException e) {
                                aviso.error("El costo debe ser válido.");
                            }
                        });
    }

    @FXML
    private void onCancelar() {
        Mantenimiento m = seleccion();
        if (m == null) return;
        if (!DialogoConfirmacion.mostrar(
                ventana(),
                "Cancelar mantenimiento",
                "Se cancelará el mantenimiento «"
                        + m.getCodigo()
                        + "». "
                        + "Se anulará la cuota de cargo si no tiene pagos y el inmueble "
                        + "volverá a su estado anterior.",
                "Cancelar mantenimiento")) {
            return;
        }
        tareas.ejecutar(
                () -> service.cancelar(m.getId()),
                x -> {
                    aviso.info("Mantenimiento cancelado.");
                    refrescar();
                },
                e -> aviso.error(msg(e)));
    }

    @FXML
    private void onCargo() {
        Mantenimiento m = seleccion();
        if (m == null) return;
        DatePicker f = new DatePicker(LocalDate.now(clock));
        Alert d = new Alert(Alert.AlertType.CONFIRMATION);
        d.setHeaderText("Vencimiento del cargo");
        d.getDialogPane().setContent(f);
        d.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(
                        b -> {
                            LocalDate vencimiento = f.getValue();
                            tareas.ejecutar(
                                    () -> service.generarCargo(m.getId(), vencimiento),
                                    x -> {
                                        aviso.info("Cargo generado en Cuentas por cobrar.");
                                        refrescar();
                                    },
                                    e -> aviso.error(msg(e)));
                        });
    }

    @FXML
    private void onExportar() {
        java.io.File destino =
                exportador.elegirDestino(
                        tabla.getScene() == null ? null : tabla.getScene().getWindow(),
                        "reporte-mantenimiento-" + java.time.LocalDate.now() + ".xlsx",
                        "Excel",
                        "xlsx");
        if (destino == null) return;
        Periodo.Rango r = Periodo.ESTE_ANIO.resolver(clock, null, null);
        tareas.ejecutar(
                () ->
                        excel.generar(
                                visibles,
                                rentabilidad.porInmueble(r.desde(), r.hasta()),
                                r.desde(),
                                r.hasta()),
                bytes -> {
                    exportador.escribir(destino, bytes);
                    aviso.info("Excel generado.");
                },
                e -> aviso.error(msg(e)));
    }

    @FXML
    private void onCerrar() {
        cerrar();
    }

    private void abrir() {
        drawer.setVisible(true);
        drawer.setManaged(true);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    private void cerrar() {
        drawer.setVisible(false);
        drawer.setManaged(false);
        overlay.setVisible(false);
        overlay.setManaged(false);
        limpiarErrores();
    }

    private Mantenimiento seleccion() {
        Mantenimiento m = tabla.getSelectionModel().getSelectedItem();
        if (m == null) aviso.error("Selecciona un mantenimiento.");
        return m;
    }

    private Window ventana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    private TableCell<Mantenimiento, BigDecimal> moneda() {
        return new TableCell<>() {
            protected void updateItem(BigDecimal v, boolean e) {
                super.updateItem(v, e);
                setText(e || v == null ? "" : Tablas.moneda(v));
            }
        };
    }

    private BigDecimal decimal(String v) {
        return v == null || v.isBlank() ? null : new BigDecimal(v.trim().replace(',', '.'));
    }

    private String msg(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }
}

package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.Recordatorio;
import com.ejemplo.alquileres.service.JlccContratoPdfService;
import com.ejemplo.alquileres.service.JlccContratoService;
import com.ejemplo.alquileres.service.JlccContratoService.JlccEstadoVisual;
import com.ejemplo.alquileres.service.JlccEstadoCuentaPdfService;
import com.ejemplo.alquileres.service.JlccRecordatorioService;
import com.ejemplo.alquileres.service.JlccReporteContratosExcelService;
import com.ejemplo.alquileres.ui.JlccDialogoRecordatorio;
import com.ejemplo.alquileres.ui.JlccExportador;
import com.ejemplo.alquileres.ui.JlccNotificador;
import com.ejemplo.alquileres.ui.JlccRefrescable;
import com.ejemplo.alquileres.ui.JlccTablas;
import com.ejemplo.alquileres.ui.JlccTareas;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Component
public class JlccContratoController implements JlccRefrescable {
    private static final DateTimeFormatter JLCC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter JLCC_FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yy");

    private final JlccContratoService jlccContratos;
    private final JlccRecordatorioService jlccRecordatorios;
    private final JlccEstadoCuentaPdfService jlccEstadoCuenta;
    private final JlccContratoPdfService jlccHojaContrato;
    private final JlccReporteContratosExcelService jlccExcel;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccExportador jlccExportador;
    private final JlccMainController jlccPrincipal;
    private final ObservableList<Contrato> jlccDatos = FXCollections.observableArrayList();
    private final ObservableList<Cuota> jlccCuotas = FXCollections.observableArrayList();
    private final ObservableList<Recordatorio> jlccEnviados = FXCollections.observableArrayList();
    private List<Contrato> jlccTodos = List.of();
    private List<Contrato> jlccVisibles = List.of();
    private Contrato jlccSeleccionado;

    @FXML private TextField buscar;
    @FXML private ComboBox<JlccEstadoVisual> filtroEstado;
    @FXML private CheckBox porVencer;
    @FXML private Label vigentes, kpiPorVencer, vencidos, rentaMensual;
    @FXML private TableView<Contrato> tabla;
    @FXML
    private TableColumn<Contrato, String> colCodigo, colInmueble, colArrendatario, colVigencia;
    @FXML private TableColumn<Contrato, BigDecimal> colRenta;
    @FXML private TableColumn<Contrato, JlccEstadoVisual> colEstado;
    @FXML private VBox detalle;
    @FXML
    private Label detalleTitulo,
            detalleSubtitulo,
            detalleEstado,
            detalleInmueble,
            detalleVigencia,
            detalleRenta,
            detalleDiaPago,
            detalleGarantia;
    @FXML private TableView<Cuota> tablaCuotas;
    @FXML private TableColumn<Cuota, String> colCuotaConcepto, colCuotaVencimiento, colCuotaEstado;
    @FXML private TableColumn<Cuota, BigDecimal> colCuotaMonto;
    @FXML private TableView<Recordatorio> tablaRecordatorios;
    @FXML private TableColumn<Recordatorio, String> colRecFecha, colRecTipo, colRecMensaje;

    public JlccContratoController(
            JlccContratoService jlccContratos,
            JlccRecordatorioService jlccRecordatorios,
            JlccEstadoCuentaPdfService jlccEstadoCuenta,
            JlccContratoPdfService jlccHojaContrato,
            JlccReporteContratosExcelService jlccExcel,
            JlccTareas jlccTareas,
            JlccNotificador jlccAviso,
            JlccExportador jlccExportador,
            JlccMainController jlccPrincipal) {
        this.jlccContratos = jlccContratos;
        this.jlccRecordatorios = jlccRecordatorios;
        this.jlccEstadoCuenta = jlccEstadoCuenta;
        this.jlccHojaContrato = jlccHojaContrato;
        this.jlccExcel = jlccExcel;
        this.jlccTareas = jlccTareas;
        this.jlccAviso = jlccAviso;
        this.jlccExportador = jlccExportador;
        this.jlccPrincipal = jlccPrincipal;
    }

    @FXML
    public void initialize() {
        filtroEstado.setItems(FXCollections.observableArrayList(JlccEstadoVisual.values()));
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colInmueble.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getInmueble().getNombre()));
        colArrendatario.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getArrendatario().getNombreCompleto()));
        colVigencia.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getFechaInicio().format(JLCC_FECHA_CORTA)
                                        + " al "
                                        + c.getValue().getFechaFin().format(JLCC_FECHA_CORTA)));
        colRenta.setCellValueFactory(
                c -> new SimpleObjectProperty<>(c.getValue().getRentaMensual()));
        colRenta.setCellFactory(
                c ->
                        new TableCell<>() {
                            protected void updateItem(BigDecimal valor, boolean vacio) {
                                super.updateItem(valor, vacio);
                                setText(vacio ? null : JlccTablas.jlccMoneda(valor));
                            }
                        });
        colEstado.setCellValueFactory(
                c -> new SimpleObjectProperty<>(jlccContratos.jlccEstadoVisual(c.getValue())));
        colEstado.setCellFactory(
                c ->
                        new TableCell<>() {
                            protected void updateItem(JlccEstadoVisual estado, boolean vacio) {
                                super.updateItem(estado, vacio);
                                setText(null);
                                setGraphic(vacio || estado == null ? null : jlccChipEstado(estado));
                            }
                        });
        colCuotaConcepto.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getConcepto()));
        colCuotaVencimiento.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getFechaVencimiento().format(JLCC_FECHA)));
        colCuotaEstado.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getEstado().getEtiqueta()));
        colCuotaMonto.setCellValueFactory(
                c -> new SimpleObjectProperty<>(c.getValue().getMontoCuota()));
        colCuotaMonto.setCellFactory(c -> jlccMoneda());
        colRecFecha.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getFechaEnvio() == null
                                        ? "—"
                                        : c.getValue()
                                                .getFechaEnvio()
                                                .format(
                                                        DateTimeFormatter.ofPattern(
                                                                "dd/MM/yy HH:mm"))));
        colRecTipo.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                (c.getValue().getTipo() == null
                                                ? "—"
                                                : c.getValue().getTipo().getEtiqueta())
                                        + (c.getValue().getCanal() == null
                                                ? ""
                                                : " · " + c.getValue().getCanal().getEtiqueta())));
        colRecTipo.setCellFactory(
                c ->
                        new TableCell<>() {
                            protected void updateItem(String valor, boolean vacio) {
                                super.updateItem(valor, vacio);
                                setText(vacio || valor == null ? null : valor);
                                setTooltip(
                                        vacio || valor == null
                                                ? null
                                                : new javafx.scene.control.Tooltip(valor));
                            }
                        });
        colRecMensaje.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMensaje()));
        colRecMensaje.setCellFactory(
                c ->
                        new TableCell<>() {
                            protected void updateItem(String valor, boolean vacio) {
                                super.updateItem(valor, vacio);
                                setText(vacio || valor == null ? null : valor);
                                setTooltip(
                                        vacio || valor == null
                                                ? null
                                                : new javafx.scene.control.Tooltip(valor));
                            }
                        });
        tabla.setItems(jlccDatos);
        tablaCuotas.setItems(jlccCuotas);
        tablaRecordatorios.setItems(jlccEnviados);
        tablaCuotas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaRecordatorios.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        buscar.textProperty().addListener((o, a, b) -> jlccAplicarFiltros());
        filtroEstado.valueProperty().addListener((o, a, b) -> jlccAplicarFiltros());
        porVencer.selectedProperty().addListener((o, a, b) -> jlccAplicarFiltros());
        tabla.getSelectionModel()
                .selectedItemProperty()
                .addListener((o, a, b) -> jlccMostrarDetalle(b));
        tabla.setOnMouseClicked(
                e -> {
                    if (e.getClickCount() == 2)
                        jlccMostrarDetalle(tabla.getSelectionModel().getSelectedItem());
                });
    }

    @Override
    public void refrescar() {
        jlccTareas.jlccEjecutar(
                jlccContratos::jlccListar,
                lista -> {
                    jlccTodos = lista;
                    jlccAplicarFiltros();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccAplicarFiltros() {
        String jlccTexto = buscar.getText() == null ? "" : buscar.getText().trim().toLowerCase();
        JlccEstadoVisual jlccEstado = filtroEstado.getValue();
        jlccVisibles =
                jlccTodos.stream()
                        .filter(
                                c ->
                                        jlccTexto.isBlank()
                                                || (c.getCodigo()
                                                                + " "
                                                                + c.getInmueble().getNombre()
                                                                + " "
                                                                + c.getArrendatario()
                                                                        .getNombreCompleto())
                                                        .toLowerCase()
                                                        .contains(jlccTexto))
                        .filter(c -> jlccEstado == null || jlccContratos.jlccEstadoVisual(c) == jlccEstado)
                        .filter(
                                c ->
                                        !porVencer.isSelected()
                                                || jlccContratos.jlccEstadoVisual(c)
                                                        == JlccEstadoVisual.POR_VENCER)
                        .toList();
        jlccDatos.setAll(jlccVisibles);
        long jlccVigentes =
                jlccTodos.stream()
                        .filter(c -> jlccContratos.jlccEstadoVisual(c) == JlccEstadoVisual.VIGENTE)
                        .count();
        long jlccPorVencer =
                jlccTodos.stream()
                        .filter(c -> jlccContratos.jlccEstadoVisual(c) == JlccEstadoVisual.POR_VENCER)
                        .count();
        long jlccVencidos =
                jlccTodos.stream()
                        .filter(c -> jlccContratos.jlccEstadoVisual(c) == JlccEstadoVisual.VENCIDO)
                        .count();
        BigDecimal jlccRenta =
                jlccTodos.stream()
                        .filter(c -> jlccContratos.jlccEstadoVisual(c) == JlccEstadoVisual.VIGENTE)
                        .map(Contrato::getRentaMensual)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.vigentes.setText(String.valueOf(jlccVigentes));
        this.kpiPorVencer.setText(String.valueOf(jlccPorVencer));
        this.vencidos.setText(String.valueOf(jlccVencidos));
        this.rentaMensual.setText(JlccTablas.jlccMoneda(jlccRenta));
    }

    private void jlccMostrarDetalle(Contrato jlccContrato) {
        jlccSeleccionado = jlccContrato;
        if (jlccContrato == null) {
            detalle.setVisible(false);
            detalle.setManaged(false);
            return;
        }
        detalle.setVisible(true);
        detalle.setManaged(true);
        detalleTitulo.setText(jlccContrato.getCodigo());
        detalleSubtitulo.setText(jlccContrato.getArrendatario().getNombreCompleto());
        detalleInmueble.setText(jlccContrato.getInmueble().getNombre());
        detalleVigencia.setText(
                jlccContrato.getFechaInicio().format(JLCC_FECHA)
                        + " al "
                        + jlccContrato.getFechaFin().format(JLCC_FECHA));
        detalleRenta.setText(JlccTablas.jlccMoneda(jlccContrato.getRentaMensual()));
        detalleDiaPago.setText(String.valueOf(jlccContrato.getDiaPago()));
        detalleGarantia.setText(
                jlccContrato.getGarantiaMonto() == null
                        ? "—"
                        : JlccTablas.jlccMoneda(jlccContrato.getGarantiaMonto()));
        JlccEstadoVisual jlccEstadoVisual = jlccContratos.jlccEstadoVisual(jlccContrato);
        detalleEstado.setText(jlccEstadoVisual.jlccGetEtiqueta());
        detalleEstado.getStyleClass().setAll("chip", jlccEstiloEstado(jlccEstadoVisual));
        jlccCuotas.setAll(
                jlccContrato.getCuotas().stream()
                        .sorted(
                                Comparator.comparing(Cuota::getFechaVencimiento)
                                        .thenComparing(Cuota::getNumero))
                        .toList());
        jlccEnviados.clear();
        jlccTareas.jlccEjecutar(
                () -> jlccRecordatorios.jlccListar(jlccContrato.getId()),
                lista -> jlccEnviados.setAll(lista),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnEstadoCuenta() {
        if (jlccSeleccionado == null) {
            jlccAviso.jlccError("Selecciona un contrato.");
            return;
        }
        jlccGuardarPdf(
                "estado-cuenta-" + jlccSeleccionado.getCodigo(),
                () -> jlccEstadoCuenta.jlccGenerar(jlccSeleccionado.getId()));
    }

    @FXML
    private void jlccOnHojaContrato() {
        if (jlccSeleccionado == null) {
            jlccAviso.jlccError("Selecciona un contrato.");
            return;
        }
        jlccGuardarPdf(
                "contrato-" + jlccSeleccionado.getCodigo(),
                () -> jlccHojaContrato.jlccGenerar(jlccSeleccionado.getId()));
    }

    @FXML
    private void jlccOnAvisarFin() {
        if (jlccSeleccionado == null) {
            jlccAviso.jlccError("Selecciona un contrato.");
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> jlccRecordatorios.jlccPropuestaFinContrato(jlccSeleccionado.getId()),
                propuesta ->
                        JlccDialogoRecordatorio.jlccMostrar(
                                jlccVentana(),
                                jlccRecordatorios,
                                jlccTareas,
                                jlccExportador,
                                jlccAviso,
                                propuesta,
                                () -> jlccMostrarDetalle(jlccSeleccionado)),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnExportar() {
        File jlccDestino =
                jlccExportador.jlccElegirDestino(
                        jlccVentana(),
                        "reporte-contratos-" + java.time.LocalDate.now() + ".xlsx",
                        "Excel",
                        "xlsx");
        if (jlccDestino == null) return;
        JlccEstadoVisual jlccEstado = filtroEstado.getValue();
        boolean jlccSoloPorVencer = porVencer.isSelected();
        jlccTareas.jlccEjecutar(
                () -> jlccExcel.jlccGenerar(jlccVisibles, jlccEstado, jlccSoloPorVencer),
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("Excel generado.");
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnNuevo() {
        jlccPrincipal.jlccIrANuevoContrato();
    }

    @FXML
    private void jlccOnRenovar() {
        if (jlccSeleccionado == null) {
            jlccAviso.jlccError("Selecciona un contrato.");
            return;
        }
        jlccPrincipal.jlccIrARenovarContrato(jlccSeleccionado.getId());
    }

    @FXML
    private void jlccOnFinalizar() {
        jlccTerminar(false);
    }

    @FXML
    private void jlccOnRescindir() {
        jlccTerminar(true);
    }

    private void jlccTerminar(boolean jlccRescindir) {
        if (jlccSeleccionado == null) {
            jlccAviso.jlccError("Selecciona un contrato.");
            return;
        }
        Contrato jlccContrato = jlccSeleccionado;
        DatePicker jlccFecha = new DatePicker(LocalDate.now());
        TextField jlccMotivo = new TextField();
        jlccMotivo.setPromptText(jlccRescindir ? "Motivo obligatorio" : "Motivo (opcional)");
        TextField jlccDevuelta = new TextField(jlccNvl(jlccContrato.getGarantiaMonto()).toPlainString());
        TextField jlccDescontada = new TextField("0.00");
        TextField jlccMotivoGarantia = new TextField();
        jlccMotivoGarantia.setPromptText("Obligatorio si hay descuento");
        GridPane jlccFormulario = new GridPane();
        jlccFormulario.setHgap(12);
        jlccFormulario.setVgap(10);
        jlccFormulario.setPrefWidth(390);
        int jlccFila = 0;
        jlccFila(jlccFormulario, jlccFila++, "Fecha de término", jlccFecha);
        jlccFila(jlccFormulario, jlccFila++, "Motivo", jlccMotivo);
        jlccFila(jlccFormulario, jlccFila++, "Garantía devuelta (S/)", jlccDevuelta);
        jlccFila(jlccFormulario, jlccFila++, "Garantía descontada (S/)", jlccDescontada);
        jlccFila(jlccFormulario, jlccFila, "Motivo del descuento", jlccMotivoGarantia);
        Label jlccAyuda =
                new Label("Las cuotas no pagadas con vencimiento posterior quedarán anuladas.");
        jlccAyuda.getStyleClass().add("campo-ayuda");
        jlccAyuda.setWrapText(true);
        jlccFormulario.add(jlccAyuda, 0, jlccFila + 1, 2, 1);
        Dialog<ButtonType> jlccDialogo = new Dialog<>();
        jlccDialogo.setTitle(jlccRescindir ? "Rescindir contrato" : "Finalizar contrato");
        jlccDialogo.setHeaderText(
                jlccContrato.getCodigo() + " · " + jlccContrato.getArrendatario().getNombreCompleto());
        jlccDialogo.getDialogPane().getStyleClass().add("dialogo");
        jlccDialogo.getDialogPane()
                .getButtonTypes()
                .addAll(
                        new ButtonType(
                                jlccRescindir ? "Rescindir" : "Finalizar",
                                ButtonBar.ButtonData.OK_DONE),
                        ButtonType.CANCEL);
        jlccDialogo.getDialogPane().setContent(jlccFormulario);
        if (tabla.getScene() != null && tabla.getScene().getWindow() != null) {
            jlccDialogo.initOwner(tabla.getScene().getWindow());
        }
        if (jlccDialogo.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        try {
            if (jlccFecha.getValue() == null)
                throw new IllegalArgumentException("Indica la fecha de término.");
            BigDecimal jlccDevueltaFinal = jlccDecimal(jlccDevuelta.getText(), "La garantía devuelta");
            BigDecimal jlccDescontadaFinal = jlccDecimal(jlccDescontada.getText(), "La garantía descontada");
            jlccTareas.jlccEjecutar(
                    () -> {
                        jlccContratos.jlccTerminar(
                                jlccContrato.getId(),
                                jlccFecha.getValue(),
                                jlccMotivo.getText(),
                                jlccRescindir,
                                jlccDevueltaFinal,
                                jlccDescontadaFinal,
                                jlccMotivoGarantia.getText());
                        return null;
                    },
                    ok -> {
                        jlccAviso.jlccInfo(jlccRescindir ? "Contrato rescindido." : "Contrato finalizado.");
                        jlccMostrarDetalle(null);
                        refrescar();
                    },
                    e -> jlccAviso.jlccError(jlccMensaje(e)));
        } catch (RuntimeException e) {
            jlccAviso.jlccError(e.getMessage());
        }
    }

    private void jlccFila(
            GridPane jlccFormulario, int jlccFila, String jlccEtiqueta, javafx.scene.control.Control jlccControl) {
        Label jlccLabel = new Label(jlccEtiqueta);
        jlccLabel.getStyleClass().add("campo-label");
        jlccFormulario.add(jlccLabel, 0, jlccFila);
        jlccFormulario.add(jlccControl, 1, jlccFila);
        if (jlccControl instanceof TextField jlccTexto) jlccTexto.setPrefColumnCount(18);
    }

    private static BigDecimal jlccDecimal(String jlccValor, String jlccCampo) {
        try {
            return jlccValor == null || jlccValor.isBlank()
                    ? BigDecimal.ZERO
                    : new BigDecimal(jlccValor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(jlccCampo + " debe ser un número válido.");
        }
    }

    private static BigDecimal jlccNvl(BigDecimal jlccValor) {
        return jlccValor == null ? BigDecimal.ZERO : jlccValor;
    }

    @FXML
    private void jlccOnCerrarDetalle() {
        tabla.getSelectionModel().clearSelection();
        jlccMostrarDetalle(null);
    }

    private void jlccGuardarPdf(String jlccNombre, java.util.function.Supplier<byte[]> jlccGenerador) {
        File jlccDestino = jlccExportador.jlccElegirDestino(jlccVentana(), jlccNombre + ".pdf", "PDF", "pdf");
        if (jlccDestino == null) return;
        jlccTareas.jlccEjecutar(
                jlccGenerador,
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("PDF generado.");
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private Label jlccChipEstado(JlccEstadoVisual jlccEstado) {
        Label jlccChip = new Label(jlccEstado.jlccGetEtiqueta());
        jlccChip.getStyleClass().addAll("chip", jlccEstiloEstado(jlccEstado));
        return jlccChip;
    }

    private static String jlccEstiloEstado(JlccEstadoVisual jlccEstado) {
        return switch (jlccEstado) {
            case VIGENTE -> "exito";
            case POR_VENCER -> "advertencia";
            case VENCIDO -> "peligro";
            case FINALIZADO, RESCINDIDO -> "neutro";
        };
    }

    private TableCell<Cuota, BigDecimal> jlccMoneda() {
        return new TableCell<>() {
            protected void updateItem(BigDecimal valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : JlccTablas.jlccMoneda(valor));
            }
        };
    }

    private javafx.stage.Window jlccVentana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    private String jlccMensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }
}

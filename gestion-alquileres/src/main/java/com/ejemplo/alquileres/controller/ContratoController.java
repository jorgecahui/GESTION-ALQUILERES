package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.Recordatorio;
import com.ejemplo.alquileres.service.ContratoPdfService;
import com.ejemplo.alquileres.service.ContratoService;
import com.ejemplo.alquileres.service.ContratoService.EstadoVisual;
import com.ejemplo.alquileres.service.EstadoCuentaPdfService;
import com.ejemplo.alquileres.service.RecordatorioService;
import com.ejemplo.alquileres.service.ReporteContratosExcelService;
import com.ejemplo.alquileres.ui.DialogoRecordatorio;
import com.ejemplo.alquileres.ui.Exportador;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Refrescable;
import com.ejemplo.alquileres.ui.Tablas;
import com.ejemplo.alquileres.ui.Tareas;
import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

/** Lista y detalle de contratos con acceso a los documentos y recordatorios de la Fase 6. */
@Component
public class ContratoController implements Refrescable {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContratoService contratos;
    private final RecordatorioService recordatorios;
    private final EstadoCuentaPdfService estadoCuenta;
    private final ContratoPdfService hojaContrato;
    private final ReporteContratosExcelService excel;
    private final Tareas tareas;
    private final Notificador aviso;
    private final Exportador exportador;
    private final MainController principal;
    private final ObservableList<Contrato> datos = FXCollections.observableArrayList();
    private final ObservableList<Cuota> cuotas = FXCollections.observableArrayList();
    private final ObservableList<Recordatorio> enviados = FXCollections.observableArrayList();
    private List<Contrato> todos = List.of();
    private List<Contrato> visibles = List.of();
    private Contrato seleccionado;

    @FXML private TextField buscar;
    @FXML private ComboBox<EstadoVisual> filtroEstado;
    @FXML private CheckBox porVencer;
    @FXML private Label vigentes, kpiPorVencer, vencidos, rentaMensual;
    @FXML private TableView<Contrato> tabla;
    @FXML private TableColumn<Contrato, String> colCodigo, colInmueble, colArrendatario, colVigencia, colEstado;
    @FXML private TableColumn<Contrato, BigDecimal> colRenta;
    @FXML private VBox detalle;
    @FXML private Label detalleTitulo, detalleInfo;
    @FXML private TableView<Cuota> tablaCuotas;
    @FXML private TableColumn<Cuota, String> colCuotaConcepto, colCuotaVencimiento, colCuotaEstado;
    @FXML private TableColumn<Cuota, BigDecimal> colCuotaMonto, colCuotaPagado;
    @FXML private TableView<Recordatorio> tablaRecordatorios;
    @FXML private TableColumn<Recordatorio, String> colRecFecha, colRecTipo, colRecCanal, colRecMensaje;

    public ContratoController(ContratoService contratos, RecordatorioService recordatorios,
                              EstadoCuentaPdfService estadoCuenta, ContratoPdfService hojaContrato,
                              ReporteContratosExcelService excel, Tareas tareas, Notificador aviso,
                              Exportador exportador, MainController principal) {
        this.contratos = contratos; this.recordatorios = recordatorios; this.estadoCuenta = estadoCuenta;
        this.hojaContrato = hojaContrato; this.excel = excel; this.tareas = tareas; this.aviso = aviso;
        this.exportador = exportador; this.principal = principal;
    }

    @FXML public void initialize() {
        filtroEstado.setItems(FXCollections.observableArrayList(EstadoVisual.values()));
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colInmueble.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getInmueble().getNombre()));
        colArrendatario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getArrendatario().getNombreCompleto()));
        colVigencia.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaInicio().format(FECHA) + " al " + c.getValue().getFechaFin().format(FECHA)));
        colRenta.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getRentaMensual()));
        colRenta.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(BigDecimal valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio ? null : Tablas.moneda(valor));
            }
        });
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(contratos.estadoVisual(c.getValue()).name()));
        colCuotaConcepto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConcepto()));
        colCuotaVencimiento.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFechaVencimiento().format(FECHA)));
        colCuotaEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().name()));
        colCuotaMonto.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontoCuota()));
        colCuotaPagado.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontoPagado()));
        colCuotaMonto.setCellFactory(c -> moneda());
        colCuotaPagado.setCellFactory(c -> moneda());
        colRecFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFechaEnvio() == null ? "—" : c.getValue().getFechaEnvio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        colRecTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo() == null ? "—" : c.getValue().getTipo().name()));
        colRecCanal.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCanal() == null ? "—" : c.getValue().getCanal().name()));
        colRecMensaje.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMensaje()));
        tabla.setItems(datos); tablaCuotas.setItems(cuotas); tablaRecordatorios.setItems(enviados);
        buscar.textProperty().addListener((o, a, b) -> aplicarFiltros());
        filtroEstado.valueProperty().addListener((o, a, b) -> aplicarFiltros());
        porVencer.selectedProperty().addListener((o, a, b) -> aplicarFiltros());
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> mostrarDetalle(b));
        tabla.setOnMouseClicked(e -> { if (e.getClickCount() == 2) mostrarDetalle(tabla.getSelectionModel().getSelectedItem()); });
    }

    @Override public void refrescar() {
        tareas.ejecutar(contratos::listar, lista -> { todos = lista; aplicarFiltros(); }, e -> aviso.error(mensaje(e)));
    }

    private void aplicarFiltros() {
        String texto = buscar.getText() == null ? "" : buscar.getText().trim().toLowerCase();
        EstadoVisual estado = filtroEstado.getValue();
        visibles = todos.stream()
                .filter(c -> texto.isBlank() || (c.getCodigo() + " " + c.getInmueble().getNombre() + " "
                        + c.getArrendatario().getNombreCompleto()).toLowerCase().contains(texto))
                .filter(c -> estado == null || contratos.estadoVisual(c) == estado)
                .filter(c -> !porVencer.isSelected() || contratos.estadoVisual(c) == EstadoVisual.POR_VENCER)
                .toList();
        datos.setAll(visibles);
        long vigentes = todos.stream().filter(c -> contratos.estadoVisual(c) == EstadoVisual.VIGENTE).count();
        long porVencer = todos.stream().filter(c -> contratos.estadoVisual(c) == EstadoVisual.POR_VENCER).count();
        long vencidos = todos.stream().filter(c -> contratos.estadoVisual(c) == EstadoVisual.VENCIDO).count();
        BigDecimal renta = todos.stream().filter(c -> contratos.estadoVisual(c) == EstadoVisual.VIGENTE)
                .map(Contrato::getRentaMensual).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.vigentes.setText(String.valueOf(vigentes));
        this.kpiPorVencer.setText(String.valueOf(porVencer));
        this.vencidos.setText(String.valueOf(vencidos));
        this.rentaMensual.setText(Tablas.moneda(renta));
    }

    private void mostrarDetalle(Contrato contrato) {
        seleccionado = contrato;
        if (contrato == null) {
            detalle.setVisible(false);
            detalle.setManaged(false);
            return;
        }
        detalle.setVisible(true);
        detalle.setManaged(true);
        detalleTitulo.setText(contrato.getCodigo() + " · " + contrato.getArrendatario().getNombreCompleto());
        detalleInfo.setText("Inmueble: " + contrato.getInmueble().getNombre()
                + "\nVigencia: " + contrato.getFechaInicio().format(FECHA) + " al " + contrato.getFechaFin().format(FECHA)
                + "\nRenta: " + Tablas.moneda(contrato.getRentaMensual())
                + " · Día de pago: " + contrato.getDiaPago()
                + "\nGarantía: " + Tablas.moneda(contrato.getGarantiaMonto())
                + " · Estado: " + contratos.estadoVisual(contrato).name());
        cuotas.setAll(contrato.getCuotas().stream()
                .sorted(Comparator.comparing(Cuota::getFechaVencimiento).thenComparing(Cuota::getNumero)).toList());
        enviados.clear();
        tareas.ejecutar(() -> recordatorios.listar(contrato.getId()), lista -> enviados.setAll(lista),
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onEstadoCuenta() {
        if (seleccionado == null) { aviso.error("Selecciona un contrato."); return; }
        guardarPdf("estado-cuenta-" + seleccionado.getCodigo(), () -> estadoCuenta.generar(seleccionado.getId()));
    }

    @FXML private void onHojaContrato() {
        if (seleccionado == null) { aviso.error("Selecciona un contrato."); return; }
        guardarPdf("contrato-" + seleccionado.getCodigo(), () -> hojaContrato.generar(seleccionado.getId()));
    }

    @FXML private void onAvisarFin() {
        if (seleccionado == null) { aviso.error("Selecciona un contrato."); return; }
        tareas.ejecutar(() -> recordatorios.propuestaFinContrato(seleccionado.getId()),
                propuesta -> DialogoRecordatorio.mostrar(ventana(), recordatorios, tareas, exportador, aviso,
                        propuesta, () -> mostrarDetalle(seleccionado)),
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onExportar() {
        File destino = exportador.elegirDestino(ventana(), "reporte-contratos-" + java.time.LocalDate.now() + ".xlsx",
                "Excel", "xlsx");
        if (destino == null) return;
        tareas.ejecutar(() -> excel.generar(visibles, filtroEstado.getValue(), porVencer.isSelected()),
                bytes -> { exportador.escribir(destino, bytes); aviso.info("Excel generado."); },
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onNuevo() { principal.irA("nuevo-contrato"); }

    @FXML private void onCerrarDetalle() {
        tabla.getSelectionModel().clearSelection();
        mostrarDetalle(null);
    }

    private void guardarPdf(String nombre, java.util.function.Supplier<byte[]> generador) {
        File destino = exportador.elegirDestino(ventana(), nombre + ".pdf", "PDF", "pdf");
        if (destino == null) return;
        tareas.ejecutar(generador, bytes -> { exportador.escribir(destino, bytes); aviso.info("PDF generado."); },
                e -> aviso.error(mensaje(e)));
    }

    private TableCell<Cuota, BigDecimal> moneda() {
        return new TableCell<>() {
            protected void updateItem(BigDecimal valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : Tablas.moneda(valor));
            }
        };
    }

    private javafx.stage.Window ventana() { return tabla.getScene() == null ? null : tabla.getScene().getWindow(); }

    private String mensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

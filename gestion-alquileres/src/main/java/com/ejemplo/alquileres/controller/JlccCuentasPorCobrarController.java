package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.service.JlccCuentasPorCobrarService;
import com.ejemplo.alquileres.service.JlccCuentasPorCobrarService.Cuenta;
import com.ejemplo.alquileres.service.JlccEstadoCuentaPdfService;
import com.ejemplo.alquileres.service.JlccRecordatorioService;
import com.ejemplo.alquileres.service.JlccReporteCuentasPorCobrarExcelService;
import com.ejemplo.alquileres.ui.JlccDialogoRecordatorio;
import com.ejemplo.alquileres.ui.JlccExportador;
import com.ejemplo.alquileres.ui.JlccFechas;
import com.ejemplo.alquileres.ui.JlccNotificador;
import com.ejemplo.alquileres.ui.JlccRefrescable;
import com.ejemplo.alquileres.ui.JlccSemaforo;
import com.ejemplo.alquileres.ui.JlccTablas;
import com.ejemplo.alquileres.ui.JlccTareas;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JlccCuentasPorCobrarController implements JlccRefrescable {
    private final JlccCuentasPorCobrarService jlccCuentas;
    private final JlccRecordatorioService jlccRecordatorios;
    private final JlccEstadoCuentaPdfService jlccEstadoCuenta;
    private final JlccReporteCuentasPorCobrarExcelService jlccExcel;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccExportador jlccExportador;
    private final ApplicationContext jlccContexto;
    private final ObservableList<Cuenta> jlccDatos = FXCollections.observableArrayList();
    private final Map<Long, LocalDateTime> jlccUltimos = new HashMap<>();

    @FXML private ComboBox<JlccCuentasPorCobrarService.JlccFiltro> filtro;
    @FXML private TextField buscar;
    @FXML private TableView<Cuenta> tabla;
    @FXML
    private TableColumn<Cuenta, String> colArrendatario,
            colInmueble,
            colConcepto,
            colVencimiento,
            colDias,
            colSemaforo,
            colUltimoRecordatorio;
    @FXML private TableColumn<Cuenta, BigDecimal> colSaldo, colMora, colTotal;
    @FXML private Label totalCobrar, totalVencido, porVencer, contratosMorosos;

    public JlccCuentasPorCobrarController(
            JlccCuentasPorCobrarService c,
            JlccRecordatorioService r,
            JlccEstadoCuentaPdfService jlccEc,
            JlccReporteCuentasPorCobrarExcelService e,
            JlccTareas t,
            JlccNotificador a,
            JlccExportador x,
            ApplicationContext jlccContexto) {
        this.jlccCuentas = c;
        this.jlccRecordatorios = r;
        this.jlccEstadoCuenta = jlccEc;
        this.jlccExcel = e;
        this.jlccTareas = t;
        this.jlccAviso = a;
        this.jlccExportador = x;
        this.jlccContexto = jlccContexto;
    }

    @FXML
    public void initialize() {
        filtro.setItems(FXCollections.observableArrayList(JlccCuentasPorCobrarService.JlccFiltro.values()));
        filtro.setValue(JlccCuentasPorCobrarService.JlccFiltro.TODAS);
        colArrendatario.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().arrendatario()));
        colInmueble.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().inmueble()));
        colConcepto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().concepto()));
        colVencimiento.setCellValueFactory(
                c -> new SimpleStringProperty(JlccFechas.jlccDia(c.getValue().vencimiento())));
        colDias.setCellValueFactory(
                c -> new SimpleStringProperty(String.valueOf(c.getValue().diasAtraso())));
        colSemaforo.setCellValueFactory(
                c -> new SimpleStringProperty(JlccSemaforo.jlccTexto(c.getValue().semaforo())));
        colSemaforo.setCellFactory(
                c ->
                        new TableCell<>() {
                            protected void updateItem(String valor, boolean vacio) {
                                super.updateItem(valor, vacio);
                                setText(null);
                                Cuenta jlccCuenta =
                                        getTableRow() == null ? null : getTableRow().getItem();
                                setGraphic(
                                        vacio || jlccCuenta == null
                                                ? null
                                                : JlccSemaforo.jlccCrear(jlccCuenta.semaforo()));
                            }
                        });
        colUltimoRecordatorio.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                jlccRecordatorios.jlccUltimoTexto(jlccUltimos.get(c.getValue().cuotaId()))));
        jlccConfigurar(colSaldo, Cuenta::saldo);
        jlccConfigurar(colMora, Cuenta::moraEstimada);
        jlccConfigurar(colTotal, Cuenta::total);
        tabla.setItems(jlccDatos);
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        filtro.valueProperty().addListener((o, a, b) -> refrescar());
    }

    @Override
    public void refrescar() {
        if (filtro == null) return;
        JlccCuentasPorCobrarService.JlccFiltro jlccFiltroActual = filtro.getValue();
        String jlccTexto = buscar.getText();
        jlccTareas.jlccEjecutar(
                () ->
                        new Object[] {
                            jlccCuentas.jlccListar(jlccFiltroActual, jlccTexto), jlccRecordatorios.jlccUltimosPorCuota()
                        },
                this::jlccMostrar,
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @SuppressWarnings("unchecked")
    private void jlccMostrar(Object[] jlccResultado) {
        List<Cuenta> jlccLista = (List<Cuenta>) jlccResultado[0];
        jlccUltimos.clear();
        jlccUltimos.putAll((Map<Long, LocalDateTime>) jlccResultado[1]);
        jlccDatos.setAll(jlccLista);
        tabla.refresh();
        java.time.LocalDate jlccHoy = java.time.LocalDate.now();
        BigDecimal jlccTotal =
                jlccLista.stream().map(Cuenta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal jlccVencido =
                jlccLista.stream()
                        .filter(Cuenta::vencida)
                        .map(Cuenta::total)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal jlccProximo =
                jlccLista.stream()
                        .filter(
                                c ->
                                        !c.vencimiento().isBefore(jlccHoy)
                                                && !c.vencimiento().isAfter(jlccHoy.plusDays(7)))
                        .map(Cuenta::total)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCobrar.setText(JlccTablas.jlccMoneda(jlccTotal));
        totalVencido.setText(JlccTablas.jlccMoneda(jlccVencido));
        porVencer.setText(JlccTablas.jlccMoneda(jlccProximo));
        contratosMorosos.setText(
                String.valueOf(
                        jlccLista.stream()
                                .filter(
                                        c ->
                                                c.semaforo()
                                                        == com.ejemplo.alquileres.model
                                                                .SemaforoMorosidad.MOROSO)
                                .map(Cuenta::contratoId)
                                .distinct()
                                .count()));
    }

    @FXML
    private void jlccOnCobrar() {
        Cuenta jlccCuenta = tabla.getSelectionModel().getSelectedItem();
        if (jlccCuenta == null) {
            jlccAviso.jlccError("Selecciona una cuota.");
            return;
        }
        jlccContexto.getBean(JlccMainController.class).jlccIrANuevoPago(jlccCuenta.contratoId());
    }

    @FXML
    private void jlccOnEstadoCuenta() {
        Cuenta jlccCuenta = tabla.getSelectionModel().getSelectedItem();
        if (jlccCuenta == null) {
            jlccAviso.jlccError("Selecciona una cuota.");
            return;
        }
        File jlccDestino =
                jlccExportador.jlccElegirDestino(
                        jlccVentana(),
                        "estado-cuenta-contrato-" + jlccCuenta.contratoId() + ".pdf",
                        "PDF",
                        "pdf");
        if (jlccDestino == null) return;
        jlccTareas.jlccEjecutar(
                () -> jlccEstadoCuenta.jlccGenerar(jlccCuenta.contratoId()),
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("Estado de cuenta generado.");
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnRecordar() {
        Cuenta jlccCuenta = tabla.getSelectionModel().getSelectedItem();
        if (jlccCuenta == null) {
            jlccAviso.jlccError("Selecciona una cuota.");
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> jlccRecordatorios.jlccPropuestaPorCuota(jlccCuenta.cuotaId()),
                propuesta ->
                        JlccDialogoRecordatorio.jlccMostrar(
                                jlccVentana(),
                                jlccRecordatorios,
                                jlccTareas,
                                jlccExportador,
                                jlccAviso,
                                propuesta,
                                this::refrescar),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnRecordarTodos() {
        jlccTareas.jlccEjecutar(
                jlccRecordatorios::jlccSugerencias,
                sugerencias -> {
                    var jlccVencidas =
                            sugerencias.stream()
                                    .filter(
                                            p ->
                                                    p.tipo()
                                                            == com.ejemplo.alquileres.model
                                                                    .TipoRecordatorio.VENCIDO)
                                    .toList();
                    JlccDialogoRecordatorio.jlccAsistente(
                            jlccVentana(),
                            jlccRecordatorios,
                            jlccTareas,
                            jlccExportador,
                            jlccAviso,
                            jlccVencidas,
                            this::refrescar);
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnExportar() {
        File jlccDestino =
                jlccExportador.jlccElegirDestino(
                        jlccVentana(),
                        "reporte-cuentas-por-cobrar-" + java.time.LocalDate.now() + ".xlsx",
                        "Excel",
                        "xlsx");
        if (jlccDestino == null) return;
        JlccCuentasPorCobrarService.JlccFiltro jlccFiltroActual = filtro.getValue();
        String jlccTexto = buscar.getText();
        List<Cuenta> jlccExportar = List.copyOf(jlccDatos);
        jlccTareas.jlccEjecutar(
                () -> jlccExcel.jlccGenerar(jlccExportar, jlccFiltroActual, jlccTexto),
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("Excel generado.");
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private javafx.stage.Window jlccVentana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    private <T> void jlccConfigurar(
            TableColumn<Cuenta, BigDecimal> jlccColumna,
            java.util.function.Function<Cuenta, BigDecimal> jlccValor) {
        jlccColumna.setCellValueFactory(x -> new SimpleObjectProperty<>(jlccValor.apply(x.getValue())));
        jlccColumna.setCellFactory(
                x ->
                        new TableCell<>() {
                            protected void updateItem(BigDecimal v, boolean vacio) {
                                super.updateItem(v, vacio);
                                setText(vacio ? null : JlccTablas.jlccMoneda(v));
                            }
                        });
    }

    private String jlccMensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }
}

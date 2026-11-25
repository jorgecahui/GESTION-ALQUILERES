package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.EstadoPago;
import com.ejemplo.alquileres.model.Pago;
import com.ejemplo.alquileres.service.JlccPagoService;
import com.ejemplo.alquileres.service.JlccReciboPdfService;
import com.ejemplo.alquileres.service.JlccReportePagosExcelService;
import com.ejemplo.alquileres.ui.JlccExportador;
import com.ejemplo.alquileres.ui.JlccFechas;
import com.ejemplo.alquileres.ui.JlccNotificador;
import com.ejemplo.alquileres.ui.JlccRefrescable;
import com.ejemplo.alquileres.ui.JlccTablas;
import com.ejemplo.alquileres.ui.JlccTareas;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;

import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

@Component
public class JlccPagosController implements JlccRefrescable {
    private final JlccPagoService jlccPagos;
    private final JlccReciboPdfService jlccRecibos;
    private final JlccReportePagosExcelService jlccExcel;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccExportador jlccExportador;
    private final JlccMainController jlccPrincipal;
    private final ObservableList<Pago> jlccDatos = FXCollections.observableArrayList();
    private List<Pago> jlccVisibles = List.of();

    @FXML private DatePicker desde, hasta;
    @FXML private TableView<Pago> tabla;
    @FXML
    private TableColumn<Pago, String> colRecibo,
            colFecha,
            colArrendatario,
            colInmueble,
            colMedio,
            colEstado;
    @FXML private TableColumn<Pago, BigDecimal> colTotal;
    @FXML private Label totalCobrado, numeroRecibos, moraCobrada, anulados;

    public JlccPagosController(
            JlccPagoService p,
            JlccReciboPdfService r,
            JlccReportePagosExcelService e,
            JlccTareas t,
            JlccNotificador a,
            JlccExportador x,
            JlccMainController jlccPrincipal) {
        jlccPagos = p;
        jlccRecibos = r;
        jlccExcel = e;
        jlccTareas = t;
        jlccAviso = a;
        jlccExportador = x;
        this.jlccPrincipal = jlccPrincipal;
    }

    @FXML
    public void initialize() {
        colRecibo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRecibo()));
        colFecha.setCellValueFactory(
                c -> new SimpleStringProperty(JlccFechas.jlccDia(c.getValue().getFechaPago())));
        colArrendatario.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getArrendatarioNombre()));
        colInmueble.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getInmuebleNombre()));
        colMedio.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getMedioPago().getEtiqueta()));
        colEstado.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getEstado().getEtiqueta()));
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontoTotal()));
        colTotal.setCellFactory(
                c ->
                        new TableCell<>() {
                            protected void updateItem(BigDecimal v, boolean vacio) {
                                super.updateItem(v, vacio);
                                setText(vacio ? null : JlccTablas.jlccMoneda(v));
                            }
                        });
        tabla.setItems(jlccDatos);
    }

    @Override
    public void refrescar() {
        LocalDate jlccInicio = desde.getValue();
        LocalDate jlccFin = hasta.getValue();
        jlccTareas.jlccEjecutar(
                () -> jlccPagos.jlccListar(jlccInicio, jlccFin), this::jlccMostrar, e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnFiltrar() {
        refrescar();
    }

    @FXML
    private void jlccOnNuevo() {
        jlccPrincipal.jlccIrA("nuevo-pago");
    }

    private void jlccMostrar(List<Pago> jlccLista) {
        jlccVisibles = jlccLista;
        jlccDatos.setAll(jlccLista);
        long jlccEmitidos = jlccLista.stream().filter(p -> p.getEstado() == EstadoPago.EMITIDO).count();
        long jlccAnul = jlccLista.size() - jlccEmitidos;
        BigDecimal jlccTotal =
                jlccLista.stream()
                        .filter(p -> p.getEstado() == EstadoPago.EMITIDO)
                        .map(Pago::getMontoTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal jlccMora =
                jlccLista.stream()
                        .filter(p -> p.getEstado() == EstadoPago.EMITIDO)
                        .map(Pago::getMontoMora)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCobrado.setText(JlccTablas.jlccMoneda(jlccTotal));
        numeroRecibos.setText(String.valueOf(jlccEmitidos));
        moraCobrada.setText(JlccTablas.jlccMoneda(jlccMora));
        anulados.setText(String.valueOf(jlccAnul));
    }

    @FXML
    private void jlccOnAnular() {
        Pago p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            jlccAviso.jlccError("Selecciona un pago.");
            return;
        }
        TextInputDialog jlccDialogo = new TextInputDialog();
        jlccDialogo.setHeaderText("Motivo de anulación para " + p.getRecibo());
        jlccDialogo.setContentText("Motivo:");
        jlccDialogo.showAndWait()
                .ifPresent(
                        motivo ->
                                jlccTareas.jlccEjecutar(
                                        () -> {
                                            jlccPagos.jlccAnular(p.getId(), motivo);
                                            return null;
                                        },
                                        x -> {
                                            jlccAviso.jlccInfo("Pago anulado.");
                                            refrescar();
                                        },
                                        e -> jlccAviso.jlccError(jlccMensaje(e))));
    }

    @FXML
    private void jlccOnPdf() {
        Pago p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            jlccAviso.jlccError("Selecciona un pago.");
            return;
        }
        FileChooser jlccSelector = new FileChooser();
        jlccSelector.setInitialFileName(p.getRecibo() + ".pdf");
        jlccSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File jlccDestino = jlccSelector.showSaveDialog(tabla.getScene().getWindow());
        if (jlccDestino != null) {
            jlccTareas.jlccEjecutar(
                    () -> {
                        try {
                            Files.write(jlccDestino.toPath(), jlccRecibos.jlccGenerar(p.getId()));
                            return jlccDestino;
                        } catch (java.io.IOException e) {
                            throw new IllegalStateException("No se pudo guardar el recibo.", e);
                        }
                    },
                    x -> jlccAviso.jlccInfo("Recibo guardado."),
                    e -> jlccAviso.jlccError(jlccMensaje(e)));
        }
    }

    @FXML
    private void jlccOnExportar() {
        File jlccDestino =
                jlccExportador.jlccElegirDestino(
                        tabla.getScene().getWindow(),
                        "reporte-pagos-" + LocalDate.now() + ".xlsx",
                        "Excel",
                        "xlsx");
        if (jlccDestino == null) return;
        LocalDate jlccInicio = desde.getValue();
        LocalDate jlccFin = hasta.getValue();
        jlccTareas.jlccEjecutar(
                () -> jlccExcel.jlccGenerar(jlccVisibles, jlccInicio, jlccFin),
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("Excel generado.");
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private String jlccMensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }
}

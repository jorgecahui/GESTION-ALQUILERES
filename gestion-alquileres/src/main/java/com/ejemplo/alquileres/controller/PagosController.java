package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.EstadoPago;
import com.ejemplo.alquileres.model.Pago;
import com.ejemplo.alquileres.service.PagoService;
import com.ejemplo.alquileres.service.ReciboPdfService;
import com.ejemplo.alquileres.service.ReportePagosExcelService;
import com.ejemplo.alquileres.ui.Exportador;
import com.ejemplo.alquileres.ui.Fechas;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Refrescable;
import com.ejemplo.alquileres.ui.Tablas;
import com.ejemplo.alquileres.ui.Tareas;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
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

@Component
public class PagosController implements Refrescable {
    private final PagoService pagos;
    private final ReciboPdfService recibos;
    private final ReportePagosExcelService excel;
    private final Tareas tareas;
    private final Notificador aviso;
    private final Exportador exportador;
    private final MainController principal;
    private final ObservableList<Pago> datos = FXCollections.observableArrayList();
    private List<Pago> visibles = List.of();

    @FXML private DatePicker desde, hasta;
    @FXML private TableView<Pago> tabla;
    @FXML private TableColumn<Pago, String> colRecibo, colFecha, colArrendatario, colInmueble, colMedio, colEstado;
    @FXML private TableColumn<Pago, BigDecimal> colTotal;
    @FXML private Label totalCobrado, numeroRecibos, moraCobrada, anulados;

    public PagosController(PagoService p, ReciboPdfService r, ReportePagosExcelService e, Tareas t,
                           Notificador a, Exportador x, MainController principal) {
        pagos = p; recibos = r; excel = e; tareas = t; aviso = a; exportador = x; this.principal = principal;
    }

    @FXML public void initialize() {
        colRecibo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRecibo()));
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(Fechas.dia(c.getValue().getFechaPago())));
        colArrendatario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getArrendatarioNombre()));
        colInmueble.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getInmuebleNombre()));
        colMedio.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedioPago().getEtiqueta()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().getEtiqueta()));
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontoTotal()));
        colTotal.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(BigDecimal v, boolean vacio) {
                super.updateItem(v, vacio);
                setText(vacio ? null : Tablas.moneda(v));
            }
        });
        tabla.setItems(datos);
    }

    @Override public void refrescar() {
        tareas.ejecutar(() -> pagos.listar(desde.getValue(), hasta.getValue()), this::mostrar, e -> aviso.error(mensaje(e)));
    }

    @FXML private void onFiltrar() { refrescar(); }

    @FXML private void onNuevo() { principal.irA("nuevo-pago"); }

    private void mostrar(List<Pago> lista) {
        visibles = lista;
        datos.setAll(lista);
        long emitidos = lista.stream().filter(p -> p.getEstado() == EstadoPago.EMITIDO).count();
        long anul = lista.size() - emitidos;
        BigDecimal total = lista.stream().filter(p -> p.getEstado() == EstadoPago.EMITIDO)
                .map(Pago::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mora = lista.stream().filter(p -> p.getEstado() == EstadoPago.EMITIDO)
                .map(Pago::getMontoMora).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCobrado.setText(Tablas.moneda(total));
        numeroRecibos.setText(String.valueOf(emitidos));
        moraCobrada.setText(Tablas.moneda(mora));
        anulados.setText(String.valueOf(anul));
    }

    @FXML private void onAnular() {
        Pago p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) { aviso.error("Selecciona un pago."); return; }
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setHeaderText("Motivo de anulación para " + p.getRecibo());
        dialogo.setContentText("Motivo:");
        dialogo.showAndWait().ifPresent(motivo -> tareas.ejecutar(() -> {
            pagos.anular(p.getId(), motivo);
            return null;
        }, x -> { aviso.info("Pago anulado."); refrescar(); }, e -> aviso.error(mensaje(e))));
    }

    @FXML private void onPdf() {
        Pago p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) { aviso.error("Selecciona un pago."); return; }
        FileChooser selector = new FileChooser();
        selector.setInitialFileName(p.getRecibo() + ".pdf");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = selector.showSaveDialog(tabla.getScene().getWindow());
        if (destino != null) {
            tareas.ejecutar(() -> {
                try {
                    Files.write(destino.toPath(), recibos.generar(p.getId()));
                    return destino;
                } catch (java.io.IOException e) {
                    throw new IllegalStateException("No se pudo guardar el recibo.", e);
                }
            }, x -> aviso.info("Recibo guardado."), e -> aviso.error(mensaje(e)));
        }
    }

    @FXML private void onExportar() {
        File destino = exportador.elegirDestino(tabla.getScene().getWindow(),
                "reporte-pagos-" + LocalDate.now() + ".xlsx", "Excel", "xlsx");
        if (destino == null) return;
        tareas.ejecutar(() -> excel.generar(visibles, desde.getValue(), hasta.getValue()),
                bytes -> { exportador.escribir(destino, bytes); aviso.info("Excel generado."); },
                e -> aviso.error(mensaje(e)));
    }

    private String mensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

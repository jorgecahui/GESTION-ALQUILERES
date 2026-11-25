package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.Periodo;
import com.ejemplo.alquileres.service.JlccMantenimientoService;
import com.ejemplo.alquileres.service.JlccRentabilidadService;
import com.ejemplo.alquileres.service.JlccReporteMantenimientoExcelService;
import com.ejemplo.alquileres.ui.*;

import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;

@Component
public class JlccRentabilidadController implements JlccRefrescable {
    private final JlccRentabilidadService jlccService;
    private final JlccMantenimientoService jlccMantenimientos;
    private final JlccReporteMantenimientoExcelService jlccExcel;
    private final Clock jlccClock;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccExportador jlccExportador;
    private final ObservableList<JlccRentabilidadService.Fila> jlccDatos =
            FXCollections.observableArrayList();
    private JlccRentabilidadService.Resumen jlccActual;
    private Periodo.Rango jlccRango;
    @FXML private ComboBox<Periodo> periodo;
    @FXML private DatePicker desde, hasta;
    @FXML private Label ingresos, gastos, neto, margen;
    @FXML private TableView<JlccRentabilidadService.Fila> tabla;
    @FXML private TableColumn<JlccRentabilidadService.Fila, String> colInmueble;
    @FXML
    private TableColumn<JlccRentabilidadService.Fila, BigDecimal> colIngresos,
            colGastos,
            colNeto,
            colMargen,
            colOcupacion;

    public JlccRentabilidadController(
            JlccRentabilidadService s,
            JlccMantenimientoService m,
            JlccReporteMantenimientoExcelService e,
            Clock c,
            JlccTareas t,
            JlccNotificador a,
            JlccExportador x) {
        jlccService = s;
        jlccMantenimientos = m;
        jlccExcel = e;
        jlccClock = c;
        jlccTareas = t;
        jlccAviso = a;
        jlccExportador = x;
    }

    @FXML
    public void initialize() {
        periodo.setItems(FXCollections.observableArrayList(Periodo.values()));
        periodo.setValue(Periodo.ESTE_ANIO);
        colInmueble.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().codigo() + " · " + c.getValue().inmueble()));
        colIngresos.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().ingresos()));
        colGastos.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().gastos()));
        colNeto.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().neto()));
        colMargen.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().margen()));
        colOcupacion.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().ocupacion()));
        for (TableColumn<JlccRentabilidadService.Fila, BigDecimal> c :
                java.util.List.of(colIngresos, colGastos, colNeto)) c.setCellFactory(x -> jlccMoneda());
        for (TableColumn<JlccRentabilidadService.Fila, BigDecimal> c :
                java.util.List.of(colMargen, colOcupacion)) c.setCellFactory(x -> jlccPorcentaje());
        tabla.setItems(jlccDatos);
        periodo.valueProperty()
                .addListener(
                        (o, a, b) -> {
                            boolean p = b == Periodo.PERSONALIZADO;
                            desde.setVisible(p);
                            desde.setManaged(p);
                            hasta.setVisible(p);
                            hasta.setManaged(p);
                            refrescar();
                        });
        desde.setVisible(false);
        desde.setManaged(false);
        hasta.setVisible(false);
        hasta.setManaged(false);
    }

    public void refrescar() {
        Periodo.Rango r = periodo.getValue().resolver(jlccClock, desde.getValue(), hasta.getValue());
        jlccRango = r;
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccPorInmueble(r.desde(), r.hasta()),
                x -> {
                    jlccActual = x;
                    jlccDatos.setAll(x.filas());
                    ingresos.setText(JlccTablas.jlccMoneda(x.ingresos()));
                    gastos.setText(JlccTablas.jlccMoneda(x.gastos()));
                    neto.setText(JlccTablas.jlccMoneda(x.neto()));
                    margen.setText(x.margen() + " %");
                },
                e -> jlccAviso.jlccError(e.getMessage()));
    }

    @FXML
    private void jlccOnActualizar() {
        refrescar();
    }

    @FXML
    private void jlccOnExportar() {
        if (jlccActual == null || jlccRango == null) {
            jlccAviso.jlccError("Actualiza el reporte antes de exportarlo.");
            return;
        }
        java.io.File jlccDestino =
                jlccExportador.jlccElegirDestino(
                        tabla.getScene() == null ? null : tabla.getScene().getWindow(),
                        "reporte-rentabilidad-" + java.time.LocalDate.now() + ".xlsx",
                        "Excel",
                        "xlsx");
        if (jlccDestino == null) return;
        jlccTareas.jlccEjecutar(
                () ->
                        jlccExcel.jlccGenerar(
                                jlccMantenimientos.jlccGastosPropietario(jlccRango.desde(), jlccRango.hasta()),
                                jlccActual,
                                jlccRango.desde(),
                                jlccRango.hasta()),
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("Excel generado.");
                },
                e -> jlccAviso.jlccError(e.getMessage()));
    }

    private TableCell<JlccRentabilidadService.Fila, BigDecimal> jlccMoneda() {
        return new TableCell<>() {
            protected void updateItem(BigDecimal v, boolean e) {
                super.updateItem(v, e);
                setText(e ? "" : JlccTablas.jlccMoneda(v));
            }
        };
    }

    private TableCell<JlccRentabilidadService.Fila, BigDecimal> jlccPorcentaje() {
        return new TableCell<>() {
            protected void updateItem(BigDecimal v, boolean e) {
                super.updateItem(v, e);
                setText(e || v == null ? "" : v + " %");
            }
        };
    }
}

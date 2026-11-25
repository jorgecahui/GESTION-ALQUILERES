package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.JlccMorosidadService;
import com.ejemplo.alquileres.service.JlccPagoService;
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
import javafx.scene.control.*;
import javafx.util.StringConverter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class JlccPagoFormController implements JlccRefrescable {
    private final JlccPagoService jlccPagos;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccMorosidadService jlccMorosidad;
    private final com.ejemplo.alquileres.service.JlccEstadoCuentaPdfService jlccEstadoCuenta;
    private final com.ejemplo.alquileres.ui.JlccExportador jlccExportador;
    private final ObservableList<JlccPagoService.Imputacion> jlccLineas =
            FXCollections.observableArrayList();
    private Long jlccPreseleccionado;
    @FXML private ComboBox<Contrato> contrato;
    @FXML private DatePicker fecha;
    @FXML private ComboBox<MedioPago> medio;
    @FXML private TextField montoRecibido, referencia, motivoCondonacion;
    @FXML private TextArea observacion;
    @FXML private CheckBox permitirAdelantos, condonarMora;
    @FXML private TableView<JlccPagoService.Imputacion> tabla;
    @FXML private TableColumn<JlccPagoService.Imputacion, String> colConcepto, colVencimiento;
    @FXML private TableColumn<JlccPagoService.Imputacion, BigDecimal> colCapital, colMora, colCondonada;
    @FXML private Label resumenContrato, totalCapital, totalMora, totalCondonada, totalPago;
    @FXML private javafx.scene.layout.HBox semaforoContrato;

    public JlccPagoFormController(
            JlccPagoService jlccPagos,
            JlccTareas jlccTareas,
            JlccNotificador jlccAviso,
            JlccMorosidadService jlccMorosidad,
            com.ejemplo.alquileres.service.JlccEstadoCuentaPdfService jlccEstadoCuenta,
            com.ejemplo.alquileres.ui.JlccExportador jlccExportador) {
        this.jlccPagos = jlccPagos;
        this.jlccTareas = jlccTareas;
        this.jlccAviso = jlccAviso;
        this.jlccMorosidad = jlccMorosidad;
        this.jlccEstadoCuenta = jlccEstadoCuenta;
        this.jlccExportador = jlccExportador;
    }

    @FXML
    public void initialize() {
        fecha.setValue(LocalDate.now());
        medio.setItems(FXCollections.observableArrayList(MedioPago.values()));
        medio.setValue(MedioPago.EFECTIVO);
        contrato.setConverter(
                new StringConverter<>() {
                    public String toString(Contrato c) {
                        return c == null
                                ? ""
                                : c.getCodigo()
                                        + " · "
                                        + c.getArrendatario().getNombreCompleto()
                                        + " · "
                                        + c.getInmueble().getNombre();
                    }

                    public Contrato fromString(String v) {
                        return null;
                    }
                });
        colConcepto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().concepto()));
        colVencimiento.setCellValueFactory(
                c -> new SimpleStringProperty(JlccFechas.jlccDia(c.getValue().vencimiento())));
        jlccConfigurarMoneda(colCapital, JlccPagoService.Imputacion::capital);
        jlccConfigurarMoneda(colMora, JlccPagoService.Imputacion::mora);
        jlccConfigurarMoneda(colCondonada, JlccPagoService.Imputacion::moraCondonada);
        tabla.setItems(jlccLineas);
        contrato.valueProperty()
                .addListener(
                        (o, a, b) -> {
                            jlccActualizarSemaforo(b);
                            jlccCargarImputacionCompleta();
                        });
        fecha.valueProperty().addListener((o, a, b) -> jlccCargarImputacionCompleta());
    }

    public void jlccPreseleccionarContrato(Long jlccContratoId) {
        jlccPreseleccionado = jlccContratoId;
    }

    @Override
    public void refrescar() {
        jlccTareas.jlccEjecutar(
                jlccPagos::jlccContratosCobrables, this::jlccMostrarContratos, e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccMostrarContratos(List<Contrato> jlccContratos) {
        contrato.setItems(FXCollections.observableArrayList(jlccContratos));
        if (jlccPreseleccionado != null) {
            jlccContratos.stream()
                    .filter(c -> c.getId().equals(jlccPreseleccionado))
                    .findFirst()
                    .ifPresent(contrato::setValue);
            jlccPreseleccionado = null;
        }
    }

    @FXML
    private void jlccOnAplicarMonto() {
        try {
            Contrato c = contrato.getValue();
            if (c == null) throw new IllegalArgumentException("Selecciona un contrato.");
            BigDecimal jlccMonto = new BigDecimal(montoRecibido.getText().trim().replace(',', '.'));
            jlccProponer(c, jlccMonto);
        } catch (Exception e) {
            jlccAviso.jlccError(
                    e.getMessage() == null ? "El monto recibido no es válido." : e.getMessage());
        }
    }

    @FXML
    private void jlccOnPagarTodo() {
        jlccCargarImputacionCompleta();
    }

    private void jlccCargarImputacionCompleta() {
        Contrato c = contrato == null ? null : contrato.getValue();
        if (c == null || fecha.getValue() == null) return;
        jlccProponer(c, new BigDecimal("999999999.99"));
    }

    @FXML
    private void jlccOnCambiarCondonacion() {
        if (!jlccLineas.isEmpty()) {
            if (montoRecibido.getText() != null && !montoRecibido.getText().isBlank())
                jlccOnAplicarMonto();
            else jlccCargarImputacionCompleta();
        }
    }

    private void jlccProponer(Contrato jlccContratoSeleccionado, BigDecimal jlccMonto) {
        LocalDate jlccFechaPago = fecha.getValue();
        boolean jlccAdelantos = permitirAdelantos.isSelected();
        boolean jlccCondonar = condonarMora.isSelected();
        String jlccMotivo = motivoCondonacion.getText();
        jlccTareas.jlccEjecutar(
                () ->
                        jlccPagos.jlccProponerImputacion(
                                jlccContratoSeleccionado.getId(),
                                jlccMonto,
                                jlccFechaPago,
                                jlccAdelantos,
                                jlccCondonar,
                                jlccMotivo),
                this::jlccMostrarLineas,
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccMostrarLineas(List<JlccPagoService.Imputacion> jlccResultado) {
        jlccLineas.setAll(jlccResultado);
        BigDecimal
                jlccCapital =
                        jlccResultado.stream()
                                .map(JlccPagoService.Imputacion::capital)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                jlccMora =
                        jlccResultado.stream()
                                .map(JlccPagoService.Imputacion::mora)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                jlccCondonada =
                        jlccResultado.stream()
                                .map(JlccPagoService.Imputacion::moraCondonada)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCapital.setText(JlccTablas.jlccMoneda(jlccCapital));
        totalMora.setText(JlccTablas.jlccMoneda(jlccMora));
        totalCondonada.setText(JlccTablas.jlccMoneda(jlccCondonada));
        totalPago.setText(JlccTablas.jlccMoneda(jlccCapital.add(jlccMora)));
        Contrato c = contrato.getValue();
        resumenContrato.setText(
                c == null
                        ? "Selecciona un contrato."
                        : c.getArrendatario().getNombreCompleto()
                                + " · "
                                + c.getInmueble().getNombre());
    }

    @FXML
    private void jlccOnRegistrar() {
        Contrato c = contrato.getValue();
        if (c == null || jlccLineas.isEmpty()) {
            jlccAviso.jlccError("Selecciona un contrato y al menos una cuota.");
            return;
        }
        List<JlccPagoService.Linea> jlccDetalle =
                jlccLineas.stream()
                        .map(
                                i ->
                                        new JlccPagoService.Linea(
                                                i.cuotaId(),
                                                i.capital(),
                                                i.mora(),
                                                i.moraCondonada(),
                                                i.motivoCondonacion()))
                        .toList();
        JlccPagoService.Solicitud s =
                new JlccPagoService.Solicitud(
                        c.getId(),
                        fecha.getValue(),
                        medio.getValue(),
                        referencia.getText(),
                        observacion.getText(),
                        jlccDetalle,
                        permitirAdelantos.isSelected());
        jlccTareas.jlccEjecutar(
                () -> jlccPagos.jlccRegistrar(s),
                p -> {
                    jlccAviso.jlccInfo("Pago registrado: " + p.getRecibo());
                    jlccLineas.clear();
                    montoRecibido.clear();
                    referencia.clear();
                    observacion.clear();
                    motivoCondonacion.clear();
                    condonarMora.setSelected(false);
                    totalCapital.setText("S/ 0.00");
                    totalMora.setText("S/ 0.00");
                    totalCondonada.setText("S/ 0.00");
                    totalPago.setText("S/ 0.00");
                    jlccPreseleccionado = c.getId();
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnEstadoCuenta() {
        Contrato c = contrato.getValue();
        if (c == null) {
            jlccAviso.jlccError("Selecciona un contrato para generar el estado de cuenta.");
            return;
        }
        java.io.File jlccDestino =
                jlccExportador.jlccElegirDestino(
                        jlccVentana(), "estado-cuenta-" + c.getCodigo() + ".pdf", "PDF", "pdf");
        if (jlccDestino == null) return;
        jlccTareas.jlccEjecutar(
                () -> jlccEstadoCuenta.jlccGenerar(c.getId()),
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("Estado de cuenta generado.");
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccActualizarSemaforo(Contrato c) {
        if (c == null) {
            semaforoContrato.getChildren().clear();
            return;
        }
        semaforoContrato.getChildren().setAll(JlccSemaforo.jlccCrear(jlccMorosidad.jlccSemaforo(c)));
    }

    private javafx.stage.Window jlccVentana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    private <T> void jlccConfigurarMoneda(
            TableColumn<JlccPagoService.Imputacion, BigDecimal> jlccColumna,
            java.util.function.Function<JlccPagoService.Imputacion, BigDecimal> jlccValor) {
        jlccColumna.setCellValueFactory(c -> new SimpleObjectProperty<>(jlccValor.apply(c.getValue())));
        jlccColumna.setCellFactory(
                c ->
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

package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.MorosidadService;
import com.ejemplo.alquileres.service.PagoService;
import com.ejemplo.alquileres.ui.Fechas;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Refrescable;
import com.ejemplo.alquileres.ui.Semaforo;
import com.ejemplo.alquileres.ui.Tablas;
import com.ejemplo.alquileres.ui.Tareas;

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
public class PagoFormController implements Refrescable {
    private final PagoService pagos;
    private final Tareas tareas;
    private final Notificador aviso;
    private final MorosidadService morosidad;
    private final com.ejemplo.alquileres.service.EstadoCuentaPdfService estadoCuenta;
    private final com.ejemplo.alquileres.ui.Exportador exportador;
    private final ObservableList<PagoService.Imputacion> lineas =
            FXCollections.observableArrayList();
    private Long preseleccionado;
    @FXML private ComboBox<Contrato> contrato;
    @FXML private DatePicker fecha;
    @FXML private ComboBox<MedioPago> medio;
    @FXML private TextField montoRecibido, referencia, motivoCondonacion;
    @FXML private TextArea observacion;
    @FXML private CheckBox permitirAdelantos, condonarMora;
    @FXML private TableView<PagoService.Imputacion> tabla;
    @FXML private TableColumn<PagoService.Imputacion, String> colConcepto, colVencimiento;
    @FXML private TableColumn<PagoService.Imputacion, BigDecimal> colCapital, colMora, colCondonada;
    @FXML private Label resumenContrato, totalCapital, totalMora, totalCondonada, totalPago;
    @FXML private javafx.scene.layout.HBox semaforoContrato;

    public PagoFormController(
            PagoService pagos,
            Tareas tareas,
            Notificador aviso,
            MorosidadService morosidad,
            com.ejemplo.alquileres.service.EstadoCuentaPdfService estadoCuenta,
            com.ejemplo.alquileres.ui.Exportador exportador) {
        this.pagos = pagos;
        this.tareas = tareas;
        this.aviso = aviso;
        this.morosidad = morosidad;
        this.estadoCuenta = estadoCuenta;
        this.exportador = exportador;
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
                c -> new SimpleStringProperty(Fechas.dia(c.getValue().vencimiento())));
        configurarMoneda(colCapital, PagoService.Imputacion::capital);
        configurarMoneda(colMora, PagoService.Imputacion::mora);
        configurarMoneda(colCondonada, PagoService.Imputacion::moraCondonada);
        tabla.setItems(lineas);
        contrato.valueProperty()
                .addListener(
                        (o, a, b) -> {
                            actualizarSemaforo(b);
                            cargarImputacionCompleta();
                        });
        fecha.valueProperty().addListener((o, a, b) -> cargarImputacionCompleta());
    }

    public void preseleccionarContrato(Long contratoId) {
        preseleccionado = contratoId;
    }

    @Override
    public void refrescar() {
        tareas.ejecutar(
                pagos::contratosCobrables, this::mostrarContratos, e -> aviso.error(mensaje(e)));
    }

    private void mostrarContratos(List<Contrato> contratos) {
        contrato.setItems(FXCollections.observableArrayList(contratos));
        if (preseleccionado != null) {
            contratos.stream()
                    .filter(c -> c.getId().equals(preseleccionado))
                    .findFirst()
                    .ifPresent(contrato::setValue);
            preseleccionado = null;
        }
    }

    @FXML
    private void onAplicarMonto() {
        try {
            Contrato c = contrato.getValue();
            if (c == null) throw new IllegalArgumentException("Selecciona un contrato.");
            BigDecimal monto = new BigDecimal(montoRecibido.getText().trim().replace(',', '.'));
            proponer(c, monto);
        } catch (Exception e) {
            aviso.error(
                    e.getMessage() == null ? "El monto recibido no es válido." : e.getMessage());
        }
    }

    @FXML
    private void onPagarTodo() {
        cargarImputacionCompleta();
    }

    private void cargarImputacionCompleta() {
        Contrato c = contrato == null ? null : contrato.getValue();
        if (c == null || fecha.getValue() == null) return;
        proponer(c, new BigDecimal("999999999.99"));
    }

    @FXML
    private void onCambiarCondonacion() {
        if (!lineas.isEmpty()) {
            if (montoRecibido.getText() != null && !montoRecibido.getText().isBlank())
                onAplicarMonto();
            else cargarImputacionCompleta();
        }
    }

    private void proponer(Contrato contratoSeleccionado, BigDecimal monto) {
        LocalDate fechaPago = fecha.getValue();
        boolean adelantos = permitirAdelantos.isSelected();
        boolean condonar = condonarMora.isSelected();
        String motivo = motivoCondonacion.getText();
        tareas.ejecutar(
                () ->
                        pagos.proponerImputacion(
                                contratoSeleccionado.getId(),
                                monto,
                                fechaPago,
                                adelantos,
                                condonar,
                                motivo),
                this::mostrarLineas,
                e -> aviso.error(mensaje(e)));
    }

    private void mostrarLineas(List<PagoService.Imputacion> resultado) {
        lineas.setAll(resultado);
        BigDecimal
                capital =
                        resultado.stream()
                                .map(PagoService.Imputacion::capital)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                mora =
                        resultado.stream()
                                .map(PagoService.Imputacion::mora)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                condonada =
                        resultado.stream()
                                .map(PagoService.Imputacion::moraCondonada)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCapital.setText(Tablas.moneda(capital));
        totalMora.setText(Tablas.moneda(mora));
        totalCondonada.setText(Tablas.moneda(condonada));
        totalPago.setText(Tablas.moneda(capital.add(mora)));
        Contrato c = contrato.getValue();
        resumenContrato.setText(
                c == null
                        ? "Selecciona un contrato."
                        : c.getArrendatario().getNombreCompleto()
                                + " · "
                                + c.getInmueble().getNombre());
    }

    @FXML
    private void onRegistrar() {
        Contrato c = contrato.getValue();
        if (c == null || lineas.isEmpty()) {
            aviso.error("Selecciona un contrato y al menos una cuota.");
            return;
        }
        List<PagoService.Linea> detalle =
                lineas.stream()
                        .map(
                                i ->
                                        new PagoService.Linea(
                                                i.cuotaId(),
                                                i.capital(),
                                                i.mora(),
                                                i.moraCondonada(),
                                                i.motivoCondonacion()))
                        .toList();
        PagoService.Solicitud s =
                new PagoService.Solicitud(
                        c.getId(),
                        fecha.getValue(),
                        medio.getValue(),
                        referencia.getText(),
                        observacion.getText(),
                        detalle,
                        permitirAdelantos.isSelected());
        tareas.ejecutar(
                () -> pagos.registrar(s),
                p -> {
                    aviso.info("Pago registrado: " + p.getRecibo());
                    lineas.clear();
                    montoRecibido.clear();
                    referencia.clear();
                    observacion.clear();
                    motivoCondonacion.clear();
                    condonarMora.setSelected(false);
                    totalCapital.setText("S/ 0.00");
                    totalMora.setText("S/ 0.00");
                    totalCondonada.setText("S/ 0.00");
                    totalPago.setText("S/ 0.00");
                    preseleccionado = c.getId();
                    refrescar();
                },
                e -> aviso.error(mensaje(e)));
    }

    @FXML
    private void onEstadoCuenta() {
        Contrato c = contrato.getValue();
        if (c == null) {
            aviso.error("Selecciona un contrato para generar el estado de cuenta.");
            return;
        }
        java.io.File destino =
                exportador.elegirDestino(
                        ventana(), "estado-cuenta-" + c.getCodigo() + ".pdf", "PDF", "pdf");
        if (destino == null) return;
        tareas.ejecutar(
                () -> estadoCuenta.generar(c.getId()),
                bytes -> {
                    exportador.escribir(destino, bytes);
                    aviso.info("Estado de cuenta generado.");
                },
                e -> aviso.error(mensaje(e)));
    }

    private void actualizarSemaforo(Contrato c) {
        if (c == null) {
            semaforoContrato.getChildren().clear();
            return;
        }
        semaforoContrato.getChildren().setAll(Semaforo.crear(morosidad.semaforo(c)));
    }

    private javafx.stage.Window ventana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    private <T> void configurarMoneda(
            TableColumn<PagoService.Imputacion, BigDecimal> columna,
            java.util.function.Function<PagoService.Imputacion, BigDecimal> valor) {
        columna.setCellValueFactory(c -> new SimpleObjectProperty<>(valor.apply(c.getValue())));
        columna.setCellFactory(
                c ->
                        new TableCell<>() {
                            protected void updateItem(BigDecimal v, boolean vacio) {
                                super.updateItem(v, vacio);
                                setText(vacio ? null : Tablas.moneda(v));
                            }
                        });
    }

    private String mensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }
}

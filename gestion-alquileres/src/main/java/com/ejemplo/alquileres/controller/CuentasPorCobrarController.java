package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.service.CuentasPorCobrarService;
import com.ejemplo.alquileres.service.CuentasPorCobrarService.Cuenta;
import com.ejemplo.alquileres.service.EstadoCuentaPdfService;
import com.ejemplo.alquileres.service.RecordatorioService;
import com.ejemplo.alquileres.service.ReporteCuentasPorCobrarExcelService;
import com.ejemplo.alquileres.ui.DialogoRecordatorio;
import com.ejemplo.alquileres.ui.Exportador;
import com.ejemplo.alquileres.ui.Fechas;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Refrescable;
import com.ejemplo.alquileres.ui.Semaforo;
import com.ejemplo.alquileres.ui.Tablas;
import com.ejemplo.alquileres.ui.Tareas;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CuentasPorCobrarController implements Refrescable {
    private final CuentasPorCobrarService cuentas;
    private final RecordatorioService recordatorios;
    private final EstadoCuentaPdfService estadoCuenta;
    private final ReporteCuentasPorCobrarExcelService excel;
    private final Tareas tareas;
    private final Notificador aviso;
    private final Exportador exportador;
    private final ApplicationContext contexto;
    private final ObservableList<Cuenta> datos = FXCollections.observableArrayList();
    private final Map<Long, LocalDateTime> ultimos = new HashMap<>();

    @FXML private ComboBox<CuentasPorCobrarService.Filtro> filtro;
    @FXML private TextField buscar;
    @FXML private TableView<Cuenta> tabla;
    @FXML private TableColumn<Cuenta, String> colArrendatario, colInmueble, colConcepto, colVencimiento,
            colDias, colSemaforo, colUltimoRecordatorio;
    @FXML private TableColumn<Cuenta, BigDecimal> colSaldo, colMora, colTotal;
    @FXML private Label totalCobrar, totalVencido, porVencer, contratosMorosos;

    public CuentasPorCobrarController(CuentasPorCobrarService c, RecordatorioService r,
                                      EstadoCuentaPdfService ec, ReporteCuentasPorCobrarExcelService e,
                                      Tareas t, Notificador a, Exportador x, ApplicationContext contexto) {
        this.cuentas = c; this.recordatorios = r; this.estadoCuenta = ec; this.excel = e;
        this.tareas = t; this.aviso = a; this.exportador = x; this.contexto = contexto;
    }

    @FXML public void initialize() {
        filtro.setItems(FXCollections.observableArrayList(CuentasPorCobrarService.Filtro.values()));
        filtro.setValue(CuentasPorCobrarService.Filtro.TODAS);
        colArrendatario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().arrendatario()));
        colInmueble.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().inmueble()));
        colConcepto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().concepto()));
        colVencimiento.setCellValueFactory(c -> new SimpleStringProperty(Fechas.dia(c.getValue().vencimiento())));
        colDias.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().diasAtraso())));
        colSemaforo.setCellValueFactory(c -> new SimpleStringProperty(Semaforo.texto(c.getValue().semaforo())));
        colSemaforo.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(null);
                Cuenta cuenta = getTableRow() == null ? null : getTableRow().getItem();
                setGraphic(vacio || cuenta == null ? null : Semaforo.crear(cuenta.semaforo()));
            }
        });
        colUltimoRecordatorio.setCellValueFactory(c -> new SimpleStringProperty(
                recordatorios.ultimoTexto(ultimos.get(c.getValue().cuotaId()))));
        configurar(colSaldo, Cuenta::saldo);
        configurar(colMora, Cuenta::moraEstimada);
        configurar(colTotal, Cuenta::total);
        tabla.setItems(datos);
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        filtro.valueProperty().addListener((o, a, b) -> refrescar());
    }

    @Override public void refrescar() {
        if (filtro == null) return;
        CuentasPorCobrarService.Filtro filtroActual = filtro.getValue();
        String texto = buscar.getText();
        tareas.ejecutar(() -> new Object[]{cuentas.listar(filtroActual, texto), recordatorios.ultimosPorCuota()},
                this::mostrar, e -> aviso.error(mensaje(e)));
    }

    @SuppressWarnings("unchecked")
    private void mostrar(Object[] resultado) {
        List<Cuenta> lista = (List<Cuenta>) resultado[0];
        ultimos.clear();
        ultimos.putAll((Map<Long, LocalDateTime>) resultado[1]);
        datos.setAll(lista);
        tabla.refresh();
        java.time.LocalDate hoy = java.time.LocalDate.now();
        BigDecimal total = lista.stream().map(Cuenta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vencido = lista.stream().filter(Cuenta::vencida).map(Cuenta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal proximo = lista.stream().filter(c -> !c.vencimiento().isBefore(hoy) && !c.vencimiento().isAfter(hoy.plusDays(7)))
                .map(Cuenta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCobrar.setText(Tablas.moneda(total));
        totalVencido.setText(Tablas.moneda(vencido));
        porVencer.setText(Tablas.moneda(proximo));
        contratosMorosos.setText(String.valueOf(lista.stream()
                .filter(c -> c.semaforo() == com.ejemplo.alquileres.model.SemaforoMorosidad.MOROSO)
                .map(Cuenta::contratoId).distinct().count()));
    }

    @FXML private void onCobrar() {
        Cuenta cuenta = tabla.getSelectionModel().getSelectedItem();
        if (cuenta == null) { aviso.error("Selecciona una cuota."); return; }
        contexto.getBean(MainController.class).irANuevoPago(cuenta.contratoId());
    }

    @FXML private void onEstadoCuenta() {
        Cuenta cuenta = tabla.getSelectionModel().getSelectedItem();
        if (cuenta == null) { aviso.error("Selecciona una cuota."); return; }
        File destino = exportador.elegirDestino(ventana(), "estado-cuenta-contrato-" + cuenta.contratoId() + ".pdf",
                "PDF", "pdf");
        if (destino == null) return;
        tareas.ejecutar(() -> estadoCuenta.generar(cuenta.contratoId()),
                bytes -> { exportador.escribir(destino, bytes); aviso.info("Estado de cuenta generado."); },
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onRecordar() {
        Cuenta cuenta = tabla.getSelectionModel().getSelectedItem();
        if (cuenta == null) { aviso.error("Selecciona una cuota."); return; }
        tareas.ejecutar(() -> recordatorios.propuestaPorCuota(cuenta.cuotaId()),
                propuesta -> DialogoRecordatorio.mostrar(ventana(), recordatorios, tareas, exportador, aviso,
                        propuesta, this::refrescar),
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onRecordarTodos() {
        tareas.ejecutar(recordatorios::sugerencias, sugerencias -> {
            var vencidas = sugerencias.stream().filter(p -> p.tipo() == com.ejemplo.alquileres.model.TipoRecordatorio.VENCIDO).toList();
            DialogoRecordatorio.asistente(ventana(), recordatorios, tareas, exportador, aviso, vencidas, this::refrescar);
        }, e -> aviso.error(mensaje(e)));
    }

    @FXML private void onExportar() {
        File destino = exportador.elegirDestino(ventana(), "reporte-cuentas-por-cobrar-" + java.time.LocalDate.now() + ".xlsx",
                "Excel", "xlsx");
        if (destino == null) return;
        CuentasPorCobrarService.Filtro filtroActual = filtro.getValue();
        String texto = buscar.getText();
        List<Cuenta> exportar = List.copyOf(datos);
        tareas.ejecutar(() -> excel.generar(exportar, filtroActual, texto),
                bytes -> { exportador.escribir(destino, bytes); aviso.info("Excel generado."); },
                e -> aviso.error(mensaje(e)));
    }

    private javafx.stage.Window ventana() { return tabla.getScene() == null ? null : tabla.getScene().getWindow(); }

    private <T> void configurar(TableColumn<Cuenta, BigDecimal> columna, java.util.function.Function<Cuenta, BigDecimal> valor) {
        columna.setCellValueFactory(x -> new SimpleObjectProperty<>(valor.apply(x.getValue())));
        columna.setCellFactory(x -> new TableCell<>() {
            protected void updateItem(BigDecimal v, boolean vacio) {
                super.updateItem(v, vacio);
                setText(vacio ? null : Tablas.moneda(v));
            }
        });
    }

    private String mensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

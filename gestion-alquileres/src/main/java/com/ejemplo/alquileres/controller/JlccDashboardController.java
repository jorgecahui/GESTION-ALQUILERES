package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.*;
import com.ejemplo.alquileres.ui.*;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.*;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class JlccDashboardController implements JlccRefrescable {
    private final JlccDashboardService jlccDashboard;
    private final JlccReporteCobranzaService jlccReporte;
    private final JlccReporteCobranzaExcelService jlccExcel;
    private final JlccReciboPdfService jlccRecibos;
    private final JlccRecordatorioService jlccRecordatorios;
    private final JlccExportador jlccExportador;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccMainController jlccPrincipal;
    private final Clock jlccClock;
    private final JlccPermisos jlccPermisos;
    @FXML private ComboBox<Periodo> periodo;
    @FXML private DatePicker desde, hasta;
    @FXML
    private Label actualizado,
            totalCobrado,
            variacion,
            detalleCartera,
            cartera,
            ocupacion,
            detalleOcupacion,
            porVencer,
            detalleVencer;
    @FXML private AreaChart<Number, Number> grafico;
    @FXML private NumberAxis ejeX, ejeY;
    @FXML private VBox top, ultimos, morosos, leyenda, vencimientos;
    @FXML private PieChart dona;
    @FXML private ProgressIndicator cargando;
    private List<String> jlccEtiquetas = List.of();

    public JlccDashboardController(
            JlccDashboardService d,
            JlccReporteCobranzaService r,
            JlccReporteCobranzaExcelService e,
            JlccReciboPdfService jlccPdf,
            JlccRecordatorioService jlccRec,
            JlccExportador jlccEx,
            JlccTareas t,
            JlccNotificador n,
            JlccMainController m,
            Clock c,
            JlccPermisos p) {
        jlccDashboard = d;
        jlccReporte = r;
        jlccExcel = e;
        jlccRecibos = jlccPdf;
        jlccRecordatorios = jlccRec;
        jlccExportador = jlccEx;
        jlccTareas = t;
        jlccAviso = n;
        jlccPrincipal = m;
        jlccClock = c;
        jlccPermisos = p;
    }

    @FXML
    public void initialize() {
        periodo.setItems(FXCollections.observableArrayList(Periodo.values()));
        periodo.setValue(Periodo.ULTIMOS_30_DIAS);
        periodo.valueProperty()
                .addListener(
                        (o, a, b) -> {
                            boolean p = b == Periodo.PERSONALIZADO;
                            desde.setVisible(p);
                            hasta.setVisible(p);
                            desde.setManaged(p);
                            hasta.setManaged(p);
                            refrescar();
                        });
        ejeX.setTickLabelFormatter(
                new javafx.util.StringConverter<>() {
                    public String toString(Number n) {
                        int i = n.intValue();
                        return i >= 0 && i < jlccEtiquetas.size() ? jlccEtiquetas.get(i) : "";
                    }

                    public Number fromString(String s) {
                        return 0;
                    }
                });
        ejeY.setTickLabelFormatter(
                new javafx.util.StringConverter<>() {
                    public String toString(Number n) {
                        return "S/ " + String.format(java.util.Locale.US, "%,.0f", n.doubleValue());
                    }

                    public Number fromString(String s) {
                        return 0;
                    }
                });
    }

    @Override
    public void refrescar() {
        if (periodo == null) return;
        cargando.setVisible(true);
        Periodo.Rango r = periodo.getValue().resolver(jlccClock, desde.getValue(), hasta.getValue());
        jlccTareas.jlccEjecutar(
                () -> new Datos(jlccReporte.jlccResumen(r), jlccDashboard.jlccResumen()),
                this::jlccMostrar,
                e -> {
                    cargando.setVisible(false);
                    jlccAviso.jlccError(jlccMensaje(e));
                });
    }

    private void jlccMostrar(Datos d) {
        cargando.setVisible(false);
        JlccReporteCobranzaService.Resumen r = d.cobros;
        JlccDashboardService.Resumen x = d.dashboard;
        totalCobrado.setText(JlccTablas.jlccMoneda(r.total()));
        if (r.variacion() == null) {
            variacion.setText(r.total().signum() > 0 ? "Nuevo" : "▼ 0.0 %");
            variacion
                    .getStyleClass()
                    .setAll("variacion", r.total().signum() > 0 ? "positiva" : "neutra");
        } else {
            boolean jlccSube = r.variacion() >= 0;
            variacion.setText(
                    (jlccSube ? "▲ " : "▼ ")
                            + String.format(
                                    java.util.Locale.US, "%.1f %%", Math.abs(r.variacion())));
            variacion.getStyleClass().setAll("variacion", jlccSube ? "positiva" : "negativa");
        }
        cartera.setText(JlccTablas.jlccMoneda(x.cartera().total()));
        detalleCartera.setText(
                x.cartera().cuotas()
                        + " cuotas · "
                        + x.cartera().arrendatarios()
                        + " arrendatarios · "
                        + String.format(java.util.Locale.US, "%.1f %%", x.cartera().porcentaje()));
        double jlccPct =
                x.ocupacion().total() == 0
                        ? 0
                        : 100d * x.ocupacion().alquilados() / x.ocupacion().total();
        ocupacion.setText(String.format(java.util.Locale.US, "%.0f %%", jlccPct));
        detalleOcupacion.setText(
                x.ocupacion().alquilados() + " de " + x.ocupacion().total() + " alquilados");
        porVencer.setText(String.valueOf(x.vencimientos().size()));
        detalleVencer.setText(
                x.vencimientos().isEmpty()
                        ? "Ninguno en los próximos 60 días"
                        : "Próximo: "
                                + x.vencimientos().get(0).codigo()
                                + " en "
                                + x.vencimientos().get(0).dias()
                                + " días");
        actualizado.setText(
                "Resumen general · actualizado a las "
                        + LocalTime.now(jlccClock).format(DateTimeFormatter.ofPattern("HH:mm")));
        jlccSerie(r);
        jlccRanking(r.top());
        jlccPagos(r.ultimos());
        jlccMorosos(x.morosos());
        jlccDona(x.ocupacion());
        jlccVencimientos(x.vencimientos());
    }

    private void jlccSerie(JlccReporteCobranzaService.Resumen r) {
        jlccEtiquetas =
                r.serie().stream()
                        .map(
                                p ->
                                        p.fecha()
                                                .format(
                                                        DateTimeFormatter.ofPattern(
                                                                r.porMes() ? "MMM yy" : "dd MMM",
                                                                new Locale("es", "PE"))))
                        .toList();
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        double jlccMax = 0;
        for (int i = 0; i < r.serie().size(); i++) {
            double v = r.serie().get(i).total().doubleValue();
            s.getData().add(new XYChart.Data<>(i, v));
            jlccMax = Math.max(jlccMax, v);
        }
        grafico.getData().setAll(s);
        ejeX.setAutoRanging(false);
        ejeX.setLowerBound(0);
        ejeX.setUpperBound(Math.max(1, r.serie().size() - 1));
        ejeX.setTickUnit(Math.max(1, Math.ceil(r.serie().size() / 7d)));
        ejeY.setAutoRanging(false);
        ejeY.setLowerBound(0);
        ejeY.setUpperBound(jlccMax <= 0 ? 100 : Math.ceil(jlccMax / 5) * 5);
        ejeY.setTickUnit(ejeY.getUpperBound() / 5);
    }

    private void jlccRanking(List<JlccReporteCobranzaService.IngresoInmueble> a) {
        top.getChildren().clear();
        if (a.isEmpty()) {
            jlccVacio(top, "Sin cobros en el periodo.");
            return;
        }
        double m = a.get(0).importe().doubleValue();
        for (int i = 0; i < a.size(); i++) {
            JlccReporteCobranzaService.IngresoInmueble p = a.get(i);
            Label n = new Label((i + 1) + ". " + p.nombre());
            Label v = new Label(JlccTablas.jlccMoneda(p.importe()));
            VBox jlccTexto = new VBox(2, n, v);
            ProgressBar b = new ProgressBar(m == 0 ? 0 : p.importe().doubleValue() / m);
            b.setMaxWidth(Double.MAX_VALUE);
            jlccTexto.getChildren().add(b);
            jlccTexto.getStyleClass().add("ranking-fila");
            top.getChildren().add(jlccTexto);
        }
    }

    private void jlccPagos(List<Pago> a) {
        ultimos.getChildren().clear();
        if (a.isEmpty()) {
            jlccVacio(ultimos, "Sin cobros en el periodo.");
            return;
        }
        for (Pago p : a) {
            Button jlccPdf = new Button("PDF", new FontIcon(Feather.FILE_TEXT));
            jlccPdf.setOnAction(e -> jlccPdf(p));
            Label l =
                    new Label(
                            p.getRecibo()
                                    + " · "
                                    + p.getArrendatarioNombre()
                                    + " · "
                                    + JlccTablas.jlccMoneda(p.getMontoTotal()));
            HBox f = new HBox(10, l, jlccPdf);
            f.getStyleClass().add("venta-fila");
            ultimos.getChildren().add(f);
        }
    }

    private void jlccMorosos(List<JlccDashboardService.Mora> a) {
        morosos.getChildren().clear();
        if (a.isEmpty()) {
            jlccVacio(morosos, "Nadie en mora. ¡Buen trabajo!");
            return;
        }
        for (JlccDashboardService.Mora m : a) {
            Label l =
                    new Label(
                            m.arrendatario()
                                    + " · "
                                    + m.inmueble()
                                    + " · "
                                    + JlccTablas.jlccMoneda(m.deuda())
                                    + " · "
                                    + m.dias()
                                    + " días");
            HBox f = new HBox(10, l, JlccSemaforo.jlccCrear(m.semaforo()));
            f.getStyleClass().add("venta-fila");
            morosos.getChildren().add(f);
        }
    }

    private void jlccDona(JlccDashboardService.Ocupacion o) {
        ObservableList<PieChart.Data> d = FXCollections.observableArrayList();
        for (JlccDashboardService.Tipo t : o.porTipo())
            d.add(new PieChart.Data(t.nombre(), t.cantidad()));
        dona.setData(d);
        leyenda.getChildren().clear();
        for (JlccDashboardService.Tipo t : o.porTipo())
            leyenda.getChildren().add(new Label(t.nombre() + " · " + t.cantidad()));
    }

    private void jlccVencimientos(List<JlccDashboardService.Vencimiento> a) {
        vencimientos.getChildren().clear();
        if (a.isEmpty()) {
            jlccVacio(vencimientos, "Sin contratos por vencer.");
            return;
        }
        for (JlccDashboardService.Vencimiento v : a) {
            Button b = new Button("Renovar");
            b.getStyleClass().add("requiere-gestion");
            b.setOnAction(e -> jlccPrincipal.jlccIrARenovarContrato(v.id()));
            Button jlccAvisar = new Button("Avisar");
            jlccAvisar.getStyleClass().add("requiere-gestion");
            jlccAvisar.setOnAction(
                    e ->
                            jlccTareas.jlccEjecutar(
                                    () -> jlccRecordatorios.jlccPropuestaFinContrato(v.id()),
                                    p ->
                                            JlccDialogoRecordatorio.jlccMostrar(
                                                    jlccVentana(),
                                                    jlccRecordatorios,
                                                    jlccTareas,
                                                    jlccExportador,
                                                    jlccAviso,
                                                    p,
                                                    () -> refrescar()),
                                    err -> jlccAviso.jlccError(jlccMensaje(err))));
            HBox f =
                    new HBox(
                            10,
                            new Label(
                                    v.codigo()
                                            + " · "
                                            + v.inmueble()
                                            + " · "
                                            + v.arrendatario()
                                            + " · en "
                                            + v.dias()
                                            + " días"),
                            b,
                            jlccAvisar);
            f.getStyleClass().add("venta-fila");
            JlccRestriccionesUi.jlccAplicar(f, jlccPermisos);
            vencimientos.getChildren().add(f);
        }
    }

    private javafx.stage.Window jlccVentana() {
        return periodo.getScene() == null ? null : periodo.getScene().getWindow();
    }

    @FXML
    private void jlccOnActualizar() {
        refrescar();
    }

    @FXML
    private void jlccOnExportar() {
        Periodo.Rango r = periodo.getValue().resolver(jlccClock, desde.getValue(), hasta.getValue());
        FileChooser f = new FileChooser();
        f.setInitialFileName("dashboard-" + r.desde() + "-" + r.hasta() + ".xlsx");
        f.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        var jlccDestino = f.showSaveDialog(periodo.getScene().getWindow());
        if (jlccDestino == null) return;
        jlccTareas.jlccEjecutar(
                () -> {
                    try {
                        Datos d = new Datos(jlccReporte.jlccResumen(r), jlccDashboard.jlccResumen());
                        Files.write(jlccDestino.toPath(), jlccExcel.jlccGenerar(d.cobros, d.dashboard));
                        return null;
                    } catch (IOException e) {
                        throw new IllegalStateException("No se pudo guardar el Excel.", e);
                    }
                },
                v -> jlccAviso.jlccInfo("Excel generado."),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnVerPagos() {
        jlccPrincipal.jlccIrA("pagos");
    }

    private void jlccPdf(Pago p) {
        jlccTareas.jlccEjecutar(
                () -> jlccRecibos.jlccGenerar(p.getId()),
                b -> {
                    try {
                        File x = File.createTempFile(p.getRecibo(), ".pdf");
                        Files.write(x.toPath(), b);
                        java.awt.Desktop.getDesktop().open(x);
                    } catch (Exception e) {
                        jlccAviso.jlccError("No se pudo abrir el recibo.");
                    }
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccVacio(VBox b, String t) {
        Label l = new Label(t);
        l.getStyleClass().add("texto-vacio");
        b.getChildren().add(l);
    }

    private String jlccMensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }

    private record Datos(
            JlccReporteCobranzaService.Resumen cobros, JlccDashboardService.Resumen dashboard) {}
}

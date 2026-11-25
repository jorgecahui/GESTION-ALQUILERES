package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.GarantiaEstado;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.Persona;
import com.ejemplo.alquileres.model.TipoMora;
import com.ejemplo.alquileres.service.JlccConfiguracionService;
import com.ejemplo.alquileres.service.JlccContratoService;
import com.ejemplo.alquileres.service.JlccCronogramaGenerador;
import com.ejemplo.alquileres.service.JlccInmuebleService;
import com.ejemplo.alquileres.service.JlccPersonaService;
import com.ejemplo.alquileres.ui.JlccFormularios;
import com.ejemplo.alquileres.ui.JlccNotificador;
import com.ejemplo.alquileres.ui.JlccRefrescable;
import com.ejemplo.alquileres.ui.JlccTablas;
import com.ejemplo.alquileres.ui.JlccTareas;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class JlccContratoFormController implements JlccRefrescable {
    private static final DateTimeFormatter JLCC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter JLCC_MES =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "PE"));

    private final JlccContratoService jlccContratos;
    private final JlccInmuebleService jlccInmuebles;
    private final JlccPersonaService jlccPersonas;
    private final JlccCronogramaGenerador jlccGenerador;
    private final JlccConfiguracionService jlccConfiguracion;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccMainController jlccPrincipal;

    private final ObservableList<Cuota> jlccCronograma = FXCollections.observableArrayList();
    private Long jlccRenovacionPendiente;
    private Long jlccContratoAnteriorId;
    private boolean jlccModoRenovacion;
    private TipoMora jlccTipoMoraDefecto;
    private BigDecimal jlccValorMoraDefecto = BigDecimal.ZERO;
    private int jlccDiasGraciaDefecto;

    @FXML private Label titulo, subtitulo, mensaje;
    @FXML private ComboBox<Inmueble> inmueble;
    @FXML private ComboBox<Persona> arrendatario, garante;
    @FXML private DatePicker inicio;
    @FXML private TextField meses, diaPago, renta, incremento, garantia, valorMora, diasGracia;
    @FXML private CheckBox garantiaRecibida;
    @FXML private ComboBox<TipoMora> tipoMora;
    @FXML private TextArea observaciones;
    @FXML private TableView<Cuota> tablaCronograma;
    @FXML private TableColumn<Cuota, String> colPeriodo, colVencimiento;
    @FXML private TableColumn<Cuota, BigDecimal> colMonto;
    @FXML private Label resumenRenta, resumenGarantia, resumenCuotas, resumenTotal, resumenFin;
    @FXML private Button btnRegistrar;

    public JlccContratoFormController(
            JlccContratoService jlccContratos,
            JlccInmuebleService jlccInmuebles,
            JlccPersonaService jlccPersonas,
            JlccCronogramaGenerador jlccGenerador,
            JlccConfiguracionService jlccConfiguracion,
            JlccTareas jlccTareas,
            JlccNotificador jlccAviso,
            JlccMainController jlccPrincipal) {
        this.jlccContratos = jlccContratos;
        this.jlccInmuebles = jlccInmuebles;
        this.jlccPersonas = jlccPersonas;
        this.jlccGenerador = jlccGenerador;
        this.jlccConfiguracion = jlccConfiguracion;
        this.jlccTareas = jlccTareas;
        this.jlccAviso = jlccAviso;
        this.jlccPrincipal = jlccPrincipal;
    }

    @FXML
    public void initialize() {
        tipoMora.setItems(FXCollections.observableArrayList(TipoMora.values()));
        tipoMora.setConverter(
                new StringConverter<>() {
                    public String toString(TipoMora valor) {
                        return valor == null
                                ? ""
                                : (valor == TipoMora.PORCENTAJE_DIARIO
                                        ? "Porcentaje diario"
                                        : "Monto diario");
                    }

                    public TipoMora fromString(String texto) {
                        return null;
                    }
                });
        inmueble.setConverter(
                new StringConverter<>() {
                    public String toString(Inmueble valor) {
                        return valor == null
                                ? ""
                                : valor.getCodigo()
                                        + " · "
                                        + valor.getNombre()
                                        + " · "
                                        + JlccTablas.jlccMoneda(valor.getRentaReferencial());
                    }

                    public Inmueble fromString(String texto) {
                        return null;
                    }
                });
        StringConverter<Persona> jlccPersonasTexto =
                new StringConverter<>() {
                    public String toString(Persona valor) {
                        return valor == null
                                ? ""
                                : valor.getNombreCompleto()
                                        + " · "
                                        + valor.getTipoDocumento()
                                        + " "
                                        + valor.getNumeroDocumento();
                    }

                    public Persona fromString(String texto) {
                        return null;
                    }
                };
        arrendatario.setConverter(jlccPersonasTexto);
        garante.setConverter(jlccPersonasTexto);

        JlccFormularios.jlccSoloDigitos(meses, 3);
        JlccFormularios.jlccSoloDigitos(diaPago, 2);
        JlccFormularios.jlccSoloDigitos(diasGracia, 3);
        JlccFormularios.jlccSoloDecimal(renta, 2);
        JlccFormularios.jlccSoloDecimal(incremento, 2);
        JlccFormularios.jlccSoloDecimal(garantia, 2);
        JlccFormularios.jlccSoloDecimal(valorMora, 4);

        colPeriodo.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getPeriodo() == null
                                        ? ""
                                        : c.getValue().getPeriodo().format(JLCC_MES)));
        colVencimiento.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getFechaVencimiento() == null
                                        ? ""
                                        : c.getValue().getFechaVencimiento().format(JLCC_FECHA)));
        colMonto.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontoCuota()));
        colMonto.setCellFactory(
                c ->
                        new TableCell<>() {
                            protected void updateItem(BigDecimal valor, boolean vacio) {
                                super.updateItem(valor, vacio);
                                setText(vacio || valor == null ? null : JlccTablas.jlccMoneda(valor));
                            }
                        });
        tablaCronograma.setItems(jlccCronograma);

        inicio.valueProperty().addListener((o, a, b) -> jlccActualizarVistaPrevia());
        meses.textProperty().addListener((o, a, b) -> jlccActualizarVistaPrevia());
        diaPago.textProperty().addListener((o, a, b) -> jlccActualizarVistaPrevia());
        renta.textProperty().addListener((o, a, b) -> jlccActualizarVistaPrevia());
        incremento.textProperty().addListener((o, a, b) -> jlccActualizarVistaPrevia());
        garantia.textProperty().addListener((o, a, b) -> jlccActualizarResumenGarantia());
        garantiaRecibida.selectedProperty().addListener((o, a, b) -> jlccActualizarResumenGarantia());
        inmueble.valueProperty()
                .addListener(
                        (o, a, b) -> {
                            if (b != null
                                    && (renta.getText() == null || renta.getText().isBlank())
                                    && b.getRentaReferencial() != null) {
                                renta.setText(b.getRentaReferencial().toPlainString());
                            }
                            jlccActualizarVistaPrevia();
                        });
    }

    public void jlccPreseleccionarRenovacion(Long jlccContratoId) {
        jlccRenovacionPendiente = jlccContratoId;
    }

    public boolean jlccTieneRenovacionPendiente() {
        return jlccRenovacionPendiente != null;
    }

    public void jlccPrepararNuevo() {
        if (titulo == null || jlccRenovacionPendiente != null) return;
        jlccLimpiar();
    }

    @Override
    public void refrescar() {
        Long jlccRenovar = jlccRenovacionPendiente;
        jlccRenovacionPendiente = null;
        jlccTareas.jlccEjecutar(
                () ->
                        new Carga(
                                jlccInmuebles
                                        .jlccListar("", EstadoInmueble.DISPONIBLE, null, 0, 300)
                                        .getContent(),
                                jlccPersonas.jlccListar("", null, true, 0, 300).getContent(),
                                jlccRenovar == null ? null : jlccContratos.jlccObtener(jlccRenovar),
                                jlccConfiguracion.jlccMoraTipo(),
                                jlccConfiguracion.jlccMoraValor(),
                                jlccConfiguracion.jlccMoraDiasGracia()),
                datos -> jlccAplicar(datos, jlccRenovar != null),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccAplicar(Carga jlccDatos, boolean jlccRenovar) {
        jlccTipoMoraDefecto = jlccDatos.tipoMora();
        jlccValorMoraDefecto = jlccDatos.valorMora() == null ? BigDecimal.ZERO : jlccDatos.valorMora();
        jlccDiasGraciaDefecto = jlccDatos.gracia();
        inmueble.setItems(FXCollections.observableArrayList(jlccDatos.inmuebles()));
        arrendatario.setItems(FXCollections.observableArrayList(jlccDatos.personas()));
        garante.setItems(FXCollections.observableArrayList(jlccDatos.personas()));
        if (jlccRenovar && jlccDatos.renovacion() != null) {
            jlccCargarRenovacion(jlccDatos.renovacion());
        } else {
            if (jlccModoRenovacion) jlccLimpiar();
            if (inicio.getValue() == null) inicio.setValue(LocalDate.now());
            if (meses.getText() == null || meses.getText().isBlank()) meses.setText("12");
            if (diaPago.getText() == null || diaPago.getText().isBlank()) {
                diaPago.setText(String.valueOf(Math.min(LocalDate.now().getDayOfMonth(), 28)));
            }
            if (incremento.getText() == null || incremento.getText().isBlank())
                incremento.setText("0");
            if (garantia.getText() == null || garantia.getText().isBlank()) garantia.setText("0");
            if (tipoMora.getValue() == null) tipoMora.setValue(jlccTipoMoraDefecto);
            if (valorMora.getText() == null || valorMora.getText().isBlank()) {
                valorMora.setText(jlccValorMoraDefecto.toPlainString());
            }
            if (diasGracia.getText() == null || diasGracia.getText().isBlank()) {
                diasGracia.setText(String.valueOf(jlccDiasGraciaDefecto));
            }
            jlccActualizarVistaPrevia();
        }
    }

    private void jlccCargarRenovacion(Contrato jlccAnterior) {
        jlccModoRenovacion = true;
        jlccContratoAnteriorId = jlccAnterior.getId();
        titulo.setText("Renovar contrato");
        subtitulo.setText(
                "Renovación de "
                        + jlccAnterior.getCodigo()
                        + " · "
                        + jlccAnterior.getArrendatario().getNombreCompleto());
        btnRegistrar.setText("Renovar contrato");
        inmueble.setDisable(true);
        arrendatario.setDisable(true);
        inicio.setDisable(true);
        garantia.setDisable(true);
        garantiaRecibida.setDisable(true);

        List<Inmueble> jlccDisponibles = new ArrayList<>(inmueble.getItems());
        if (jlccDisponibles.stream().noneMatch(i -> i.getId().equals(jlccAnterior.getInmueble().getId()))) {
            jlccDisponibles.add(jlccAnterior.getInmueble());
        }
        inmueble.setItems(FXCollections.observableArrayList(jlccDisponibles));
        inmueble.setValue(jlccAnterior.getInmueble());
        arrendatario.setValue(jlccAnterior.getArrendatario());
        garante.setValue(jlccAnterior.getGarante());
        inicio.setValue(jlccAnterior.getFechaFin().plusDays(1));
        long jlccDuracion =
                Math.max(
                        1,
                        ChronoUnit.MONTHS.between(
                                jlccAnterior.getFechaInicio(), jlccAnterior.getFechaFin().plusDays(1)));
        meses.setText(String.valueOf(jlccDuracion));
        diaPago.setText(String.valueOf(jlccAnterior.getDiaPago()));
        BigDecimal jlccInc = jlccNvl(jlccAnterior.getIncrementoAnualPct());
        incremento.setText(jlccInc.toPlainString());
        BigDecimal jlccRentaPropuesta =
                jlccNvl(jlccAnterior.getRentaMensual())
                        .multiply(BigDecimal.ONE.add(jlccInc.movePointLeft(2)))
                        .setScale(2, RoundingMode.HALF_UP);
        renta.setText(jlccRentaPropuesta.toPlainString());
        garantia.setText(jlccNvl(jlccAnterior.getGarantiaMonto()).toPlainString());
        garantiaRecibida.setSelected(jlccAnterior.getGarantiaEstado() == GarantiaEstado.RECIBIDA);
        tipoMora.setValue(
                jlccAnterior.getTipoMora() == null ? jlccTipoMoraDefecto : jlccAnterior.getTipoMora());
        valorMora.setText(jlccNvl(jlccAnterior.getValorMora()).toPlainString());
        diasGracia.setText(String.valueOf(jlccAnterior.getDiasGracia()));
        observaciones.setText(
                jlccAnterior.getObservaciones() == null ? "" : jlccAnterior.getObservaciones());
        jlccActualizarVistaPrevia();
    }

    private void jlccLimpiar() {
        jlccModoRenovacion = false;
        jlccContratoAnteriorId = null;
        titulo.setText("Nuevo contrato");
        subtitulo.setText("Registra el inmueble, las partes y las condiciones del alquiler.");
        btnRegistrar.setText("Registrar contrato");
        inmueble.setDisable(false);
        arrendatario.setDisable(false);
        inicio.setDisable(false);
        garantia.setDisable(false);
        garantiaRecibida.setDisable(false);

        inmueble.setValue(null);
        arrendatario.setValue(null);
        garante.setValue(null);
        inicio.setValue(LocalDate.now());
        meses.setText("12");
        diaPago.setText(String.valueOf(Math.min(LocalDate.now().getDayOfMonth(), 28)));
        renta.clear();
        incremento.setText("0");
        garantia.setText("0");
        garantiaRecibida.setSelected(false);
        tipoMora.setValue(jlccTipoMoraDefecto);
        valorMora.setText(jlccValorMoraDefecto.toPlainString());
        diasGracia.setText(String.valueOf(jlccDiasGraciaDefecto));
        observaciones.clear();
        mensaje.setText("");
        jlccCronograma.clear();
        resumenRenta.setText("—");
        resumenGarantia.setText("—");
        resumenCuotas.setText("—");
        resumenTotal.setText("—");
        resumenFin.setText("—");
    }

    private void jlccActualizarVistaPrevia() {
        try {
            LocalDate jlccFecha = inicio.getValue();
            if (jlccFecha == null) {
                jlccLimpiarResumen();
                return;
            }
            int jlccCantidad = jlccEntero(meses.getText(), "La duración");
            int jlccDia = jlccEntero(diaPago.getText(), "El día de pago");
            BigDecimal jlccMonto = jlccDecimal(renta.getText());
            if (jlccCantidad < 1
                    || jlccCantidad > 120
                    || jlccDia < 1
                    || jlccDia > 28
                    || jlccMonto == null
                    || jlccMonto.signum() <= 0) {
                jlccLimpiarResumen();
                return;
            }
            BigDecimal jlccInc = jlccDecimal(incremento.getText());
            List<Cuota> jlccLista =
                    jlccGenerador.jlccGenerar(
                            jlccFecha, jlccCantidad, jlccDia, jlccMonto, jlccInc == null ? BigDecimal.ZERO : jlccInc);
            jlccCronograma.setAll(jlccLista);
            BigDecimal jlccTotal =
                    jlccLista.stream()
                            .map(Cuota::getMontoCuota)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            resumenRenta.setText(JlccTablas.jlccMoneda(jlccMonto));
            resumenCuotas.setText(jlccCantidad + " cuotas");
            resumenTotal.setText(JlccTablas.jlccMoneda(jlccTotal));
            resumenFin.setText(jlccFecha.plusMonths(jlccCantidad).minusDays(1).format(JLCC_FECHA));
            jlccActualizarResumenGarantia();
        } catch (RuntimeException e) {
            jlccLimpiarResumen();
        }
    }

    private void jlccActualizarResumenGarantia() {
        BigDecimal jlccMonto = jlccDecimal(garantia.getText());
        if (jlccMonto == null || jlccMonto.signum() <= 0) {
            resumenGarantia.setText("Sin garantía");
        } else {
            resumenGarantia.setText(
                    JlccTablas.jlccMoneda(jlccMonto)
                            + (garantiaRecibida.isSelected() ? " · recibida" : " · pendiente"));
        }
    }

    private void jlccLimpiarResumen() {
        jlccCronograma.clear();
        resumenRenta.setText("—");
        resumenGarantia.setText("—");
        resumenCuotas.setText("—");
        resumenTotal.setText("—");
        resumenFin.setText("—");
    }

    @FXML
    private void jlccOnRegistrar() {
        try {
            JlccContratoService.Solicitud jlccSolicitud = jlccLeerSolicitud();
            mensaje.setText("");
            btnRegistrar.setDisable(true);
            if (jlccModoRenovacion && jlccContratoAnteriorId != null) {
                Long jlccAnterior = jlccContratoAnteriorId;
                jlccTareas.jlccEjecutar(
                        () -> jlccContratos.jlccRenovar(jlccAnterior, jlccSolicitud),
                        creado -> {
                            btnRegistrar.setDisable(false);
                            jlccAviso.jlccInfo("Contrato renovado: " + creado.getCodigo());
                            jlccLimpiar();
                            jlccPrincipal.jlccIrA("contratos");
                        },
                        e -> {
                            btnRegistrar.setDisable(false);
                            jlccAviso.jlccError(jlccMensaje(e));
                        });
            } else {
                jlccTareas.jlccEjecutar(
                        () -> jlccContratos.jlccCrear(jlccSolicitud),
                        creado -> {
                            btnRegistrar.setDisable(false);
                            jlccAviso.jlccInfo("Contrato registrado: " + creado.getCodigo());
                            jlccLimpiar();
                            jlccPrincipal.jlccIrA("contratos");
                        },
                        e -> {
                            btnRegistrar.setDisable(false);
                            jlccAviso.jlccError(jlccMensaje(e));
                        });
            }
        } catch (RuntimeException e) {
            mensaje.setText(e.getMessage());
            jlccAviso.jlccError(e.getMessage() == null ? "Revisa los datos del contrato." : e.getMessage());
        }
    }

    private JlccContratoService.Solicitud jlccLeerSolicitud() {
        if (inmueble.getValue() == null)
            throw new IllegalArgumentException("Selecciona un inmueble disponible.");
        if (arrendatario.getValue() == null)
            throw new IllegalArgumentException("Selecciona un arrendatario.");
        if (inicio.getValue() == null)
            throw new IllegalArgumentException("Indica la fecha de inicio.");
        int jlccCantidad = jlccEntero(meses.getText(), "La duración");
        int jlccDia = jlccEntero(diaPago.getText(), "El día de pago");
        BigDecimal jlccMonto = jlccDecimal(renta.getText());
        if (jlccMonto == null || jlccMonto.signum() <= 0)
            throw new IllegalArgumentException("La renta debe ser mayor que cero.");
        BigDecimal jlccGarantiaMonto = jlccDecimal(garantia.getText());
        if (jlccGarantiaMonto == null) jlccGarantiaMonto = BigDecimal.ZERO;
        BigDecimal jlccInc = jlccDecimal(incremento.getText());
        BigDecimal jlccMora = jlccDecimal(valorMora.getText());
        int jlccGracia = jlccEntero(diasGracia.getText(), "Los días de gracia");
        Persona jlccGaranteSeleccionado = garante.getValue();
        if (jlccGaranteSeleccionado != null
                && jlccGaranteSeleccionado.getId().equals(arrendatario.getValue().getId())) {
            throw new IllegalArgumentException("El garante debe ser distinto del arrendatario.");
        }
        return new JlccContratoService.Solicitud(
                inmueble.getValue().getId(),
                arrendatario.getValue().getId(),
                jlccGaranteSeleccionado == null ? null : jlccGaranteSeleccionado.getId(),
                inicio.getValue(),
                jlccCantidad,
                jlccDia,
                jlccMonto,
                jlccInc == null ? BigDecimal.ZERO : jlccInc,
                jlccGarantiaMonto,
                garantiaRecibida.isSelected(),
                jlccGracia,
                tipoMora.getValue(),
                jlccMora == null ? BigDecimal.ZERO : jlccMora,
                observaciones.getText());
    }

    @FXML
    private void jlccOnCancelar() {
        jlccLimpiar();
        jlccPrincipal.jlccIrA("contratos");
    }

    private static int jlccEntero(String jlccValor, String jlccCampo) {
        try {
            return Integer.parseInt(jlccValor == null ? "" : jlccValor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(jlccCampo + " debe ser un número entero.");
        }
    }

    private static BigDecimal jlccDecimal(String jlccValor) {
        if (jlccValor == null || jlccValor.isBlank()) return null;
        try {
            return new BigDecimal(jlccValor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El importe debe ser un número válido.");
        }
    }

    private static BigDecimal jlccNvl(BigDecimal jlccValor) {
        return jlccValor == null ? BigDecimal.ZERO : jlccValor;
    }

    private static String jlccMensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }

    private record Carga(
            List<Inmueble> inmuebles,
            List<Persona> personas,
            Contrato renovacion,
            TipoMora tipoMora,
            BigDecimal valorMora,
            int gracia) {}
}

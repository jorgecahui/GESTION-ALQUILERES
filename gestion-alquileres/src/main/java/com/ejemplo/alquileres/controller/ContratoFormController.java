package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.Contrato;
import com.ejemplo.alquileres.model.Cuota;
import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.GarantiaEstado;
import com.ejemplo.alquileres.model.Inmueble;
import com.ejemplo.alquileres.model.Persona;
import com.ejemplo.alquileres.model.TipoMora;
import com.ejemplo.alquileres.service.ConfiguracionService;
import com.ejemplo.alquileres.service.ContratoService;
import com.ejemplo.alquileres.service.CronogramaGenerador;
import com.ejemplo.alquileres.service.InmuebleService;
import com.ejemplo.alquileres.service.PersonaService;
import com.ejemplo.alquileres.ui.Formularios;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Refrescable;
import com.ejemplo.alquileres.ui.Tablas;
import com.ejemplo.alquileres.ui.Tareas;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

/** Alta y renovación de contratos con vista previa del cronograma. */
@Component
public class ContratoFormController implements Refrescable {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "PE"));

    private final ContratoService contratos;
    private final InmuebleService inmuebles;
    private final PersonaService personas;
    private final CronogramaGenerador generador;
    private final ConfiguracionService configuracion;
    private final Tareas tareas;
    private final Notificador aviso;
    private final MainController principal;

    private final ObservableList<Cuota> cronograma = FXCollections.observableArrayList();
    private Long renovacionPendiente;
    private Long contratoAnteriorId;
    private boolean modoRenovacion;
    private TipoMora tipoMoraDefecto;
    private BigDecimal valorMoraDefecto = BigDecimal.ZERO;
    private int diasGraciaDefecto;

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

    public ContratoFormController(ContratoService contratos, InmuebleService inmuebles, PersonaService personas,
                                  CronogramaGenerador generador, ConfiguracionService configuracion,
                                  Tareas tareas, Notificador aviso, MainController principal) {
        this.contratos = contratos;
        this.inmuebles = inmuebles;
        this.personas = personas;
        this.generador = generador;
        this.configuracion = configuracion;
        this.tareas = tareas;
        this.aviso = aviso;
        this.principal = principal;
    }

    @FXML public void initialize() {
        tipoMora.setItems(FXCollections.observableArrayList(TipoMora.values()));
        tipoMora.setConverter(new StringConverter<>() {
            public String toString(TipoMora valor) {
                return valor == null ? "" : (valor == TipoMora.PORCENTAJE_DIARIO ? "Porcentaje diario" : "Monto diario");
            }
            public TipoMora fromString(String texto) { return null; }
        });
        inmueble.setConverter(new StringConverter<>() {
            public String toString(Inmueble valor) {
                return valor == null ? "" : valor.getCodigo() + " · " + valor.getNombre()
                        + " · " + Tablas.moneda(valor.getRentaReferencial());
            }
            public Inmueble fromString(String texto) { return null; }
        });
        StringConverter<Persona> personasTexto = new StringConverter<>() {
            public String toString(Persona valor) {
                return valor == null ? "" : valor.getNombreCompleto() + " · "
                        + valor.getTipoDocumento() + " " + valor.getNumeroDocumento();
            }
            public Persona fromString(String texto) { return null; }
        };
        arrendatario.setConverter(personasTexto);
        garante.setConverter(personasTexto);

        Formularios.soloDigitos(meses, 3);
        Formularios.soloDigitos(diaPago, 2);
        Formularios.soloDigitos(diasGracia, 3);
        Formularios.soloDecimal(renta, 2);
        Formularios.soloDecimal(incremento, 2);
        Formularios.soloDecimal(garantia, 2);
        Formularios.soloDecimal(valorMora, 4);

        colPeriodo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPeriodo() == null ? "" : c.getValue().getPeriodo().format(MES)));
        colVencimiento.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaVencimiento() == null ? "" : c.getValue().getFechaVencimiento().format(FECHA)));
        colMonto.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getMontoCuota()));
        colMonto.setCellFactory(c -> new TableCell<>() {
            protected void updateItem(BigDecimal valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : Tablas.moneda(valor));
            }
        });
        tablaCronograma.setItems(cronograma);

        inicio.valueProperty().addListener((o, a, b) -> actualizarVistaPrevia());
        meses.textProperty().addListener((o, a, b) -> actualizarVistaPrevia());
        diaPago.textProperty().addListener((o, a, b) -> actualizarVistaPrevia());
        renta.textProperty().addListener((o, a, b) -> actualizarVistaPrevia());
        incremento.textProperty().addListener((o, a, b) -> actualizarVistaPrevia());
        garantia.textProperty().addListener((o, a, b) -> actualizarResumenGarantia());
        garantiaRecibida.selectedProperty().addListener((o, a, b) -> actualizarResumenGarantia());
        inmueble.valueProperty().addListener((o, a, b) -> {
            if (b != null && (renta.getText() == null || renta.getText().isBlank())
                    && b.getRentaReferencial() != null) {
                renta.setText(b.getRentaReferencial().toPlainString());
            }
            actualizarVistaPrevia();
        });
    }

    public void preseleccionarRenovacion(Long contratoId) { renovacionPendiente = contratoId; }

    public boolean tieneRenovacionPendiente() { return renovacionPendiente != null; }

    /** Deja el formulario listo para un contrato nuevo, incluso si antes se abrió una renovación. */
    public void prepararNuevo() {
        if (titulo == null || renovacionPendiente != null) return;
        limpiar();
    }

    @Override public void refrescar() {
        Long renovar = renovacionPendiente;
        renovacionPendiente = null;
        tareas.ejecutar(() -> new Carga(
                        inmuebles.listar("", EstadoInmueble.DISPONIBLE, null, 0, 300).getContent(),
                        personas.listar("", null, true, 0, 300).getContent(),
                        renovar == null ? null : contratos.obtener(renovar),
                        configuracion.moraTipo(), configuracion.moraValor(), configuracion.moraDiasGracia()),
                datos -> aplicar(datos, renovar != null), e -> aviso.error(mensaje(e)));
    }

    private void aplicar(Carga datos, boolean renovar) {
        tipoMoraDefecto = datos.tipoMora();
        valorMoraDefecto = datos.valorMora() == null ? BigDecimal.ZERO : datos.valorMora();
        diasGraciaDefecto = datos.gracia();
        inmueble.setItems(FXCollections.observableArrayList(datos.inmuebles()));
        arrendatario.setItems(FXCollections.observableArrayList(datos.personas()));
        garante.setItems(FXCollections.observableArrayList(datos.personas()));
        if (renovar && datos.renovacion() != null) {
            cargarRenovacion(datos.renovacion());
        } else {
            if (modoRenovacion) limpiar();
            if (inicio.getValue() == null) inicio.setValue(LocalDate.now());
            if (meses.getText() == null || meses.getText().isBlank()) meses.setText("12");
            if (diaPago.getText() == null || diaPago.getText().isBlank()) {
                diaPago.setText(String.valueOf(Math.min(LocalDate.now().getDayOfMonth(), 28)));
            }
            if (incremento.getText() == null || incremento.getText().isBlank()) incremento.setText("0");
            if (garantia.getText() == null || garantia.getText().isBlank()) garantia.setText("0");
            if (tipoMora.getValue() == null) tipoMora.setValue(tipoMoraDefecto);
            if (valorMora.getText() == null || valorMora.getText().isBlank()) {
                valorMora.setText(valorMoraDefecto.toPlainString());
            }
            if (diasGracia.getText() == null || diasGracia.getText().isBlank()) {
                diasGracia.setText(String.valueOf(diasGraciaDefecto));
            }
            actualizarVistaPrevia();
        }
    }

    private void cargarRenovacion(Contrato anterior) {
        modoRenovacion = true;
        contratoAnteriorId = anterior.getId();
        titulo.setText("Renovar contrato");
        subtitulo.setText("Renovación de " + anterior.getCodigo() + " · " + anterior.getArrendatario().getNombreCompleto());
        btnRegistrar.setText("Renovar contrato");
        inmueble.setDisable(true);
        arrendatario.setDisable(true);
        inicio.setDisable(true);
        garantia.setDisable(true);
        garantiaRecibida.setDisable(true);

        List<Inmueble> disponibles = new ArrayList<>(inmueble.getItems());
        if (disponibles.stream().noneMatch(i -> i.getId().equals(anterior.getInmueble().getId()))) {
            disponibles.add(anterior.getInmueble());
        }
        inmueble.setItems(FXCollections.observableArrayList(disponibles));
        inmueble.setValue(anterior.getInmueble());
        arrendatario.setValue(anterior.getArrendatario());
        garante.setValue(anterior.getGarante());
        inicio.setValue(anterior.getFechaFin().plusDays(1));
        long duracion = Math.max(1, ChronoUnit.MONTHS.between(anterior.getFechaInicio(), anterior.getFechaFin().plusDays(1)));
        meses.setText(String.valueOf(duracion));
        diaPago.setText(String.valueOf(anterior.getDiaPago()));
        BigDecimal inc = nvl(anterior.getIncrementoAnualPct());
        incremento.setText(inc.toPlainString());
        BigDecimal rentaPropuesta = nvl(anterior.getRentaMensual())
                .multiply(BigDecimal.ONE.add(inc.movePointLeft(2))).setScale(2, RoundingMode.HALF_UP);
        renta.setText(rentaPropuesta.toPlainString());
        garantia.setText(nvl(anterior.getGarantiaMonto()).toPlainString());
        garantiaRecibida.setSelected(anterior.getGarantiaEstado() == GarantiaEstado.RECIBIDA);
        tipoMora.setValue(anterior.getTipoMora() == null ? tipoMoraDefecto : anterior.getTipoMora());
        valorMora.setText(nvl(anterior.getValorMora()).toPlainString());
        diasGracia.setText(String.valueOf(anterior.getDiasGracia()));
        observaciones.setText(anterior.getObservaciones() == null ? "" : anterior.getObservaciones());
        actualizarVistaPrevia();
    }

    private void limpiar() {
        modoRenovacion = false;
        contratoAnteriorId = null;
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
        tipoMora.setValue(tipoMoraDefecto);
        valorMora.setText(valorMoraDefecto.toPlainString());
        diasGracia.setText(String.valueOf(diasGraciaDefecto));
        observaciones.clear();
        mensaje.setText("");
        cronograma.clear();
        resumenRenta.setText("—");
        resumenGarantia.setText("—");
        resumenCuotas.setText("—");
        resumenTotal.setText("—");
        resumenFin.setText("—");
    }

    private void actualizarVistaPrevia() {
        try {
            LocalDate fecha = inicio.getValue();
            if (fecha == null) { limpiarResumen(); return; }
            int cantidad = entero(meses.getText(), "La duración");
            int dia = entero(diaPago.getText(), "El día de pago");
            BigDecimal monto = decimal(renta.getText());
            if (cantidad < 1 || cantidad > 120 || dia < 1 || dia > 28
                    || monto == null || monto.signum() <= 0) {
                limpiarResumen();
                return;
            }
            BigDecimal inc = decimal(incremento.getText());
            List<Cuota> lista = generador.generar(fecha, cantidad, dia, monto,
                    inc == null ? BigDecimal.ZERO : inc);
            cronograma.setAll(lista);
            BigDecimal total = lista.stream().map(Cuota::getMontoCuota)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resumenRenta.setText(Tablas.moneda(monto));
            resumenCuotas.setText(cantidad + " cuotas");
            resumenTotal.setText(Tablas.moneda(total));
            resumenFin.setText(fecha.plusMonths(cantidad).minusDays(1).format(FECHA));
            actualizarResumenGarantia();
        } catch (RuntimeException e) {
            limpiarResumen();
        }
    }

    private void actualizarResumenGarantia() {
        BigDecimal monto = decimal(garantia.getText());
        if (monto == null || monto.signum() <= 0) {
            resumenGarantia.setText("Sin garantía");
        } else {
            resumenGarantia.setText(Tablas.moneda(monto) + (garantiaRecibida.isSelected()
                    ? " · recibida" : " · pendiente"));
        }
    }

    private void limpiarResumen() {
        cronograma.clear();
        resumenRenta.setText("—");
        resumenGarantia.setText("—");
        resumenCuotas.setText("—");
        resumenTotal.setText("—");
        resumenFin.setText("—");
    }

    @FXML private void onRegistrar() {
        try {
            ContratoService.Solicitud solicitud = leerSolicitud();
            mensaje.setText("");
            btnRegistrar.setDisable(true);
            if (modoRenovacion && contratoAnteriorId != null) {
                Long anterior = contratoAnteriorId;
                tareas.ejecutar(() -> contratos.renovar(anterior, solicitud),
                        creado -> {
                            btnRegistrar.setDisable(false);
                            aviso.info("Contrato renovado: " + creado.getCodigo());
                            limpiar();
                            principal.irA("contratos");
                        },
                        e -> { btnRegistrar.setDisable(false); aviso.error(mensaje(e)); });
            } else {
                tareas.ejecutar(() -> contratos.crear(solicitud),
                        creado -> {
                            btnRegistrar.setDisable(false);
                            aviso.info("Contrato registrado: " + creado.getCodigo());
                            limpiar();
                            principal.irA("contratos");
                        },
                        e -> { btnRegistrar.setDisable(false); aviso.error(mensaje(e)); });
            }
        } catch (RuntimeException e) {
            mensaje.setText(e.getMessage());
            aviso.error(e.getMessage() == null ? "Revisa los datos del contrato." : e.getMessage());
        }
    }

    private ContratoService.Solicitud leerSolicitud() {
        if (inmueble.getValue() == null) throw new IllegalArgumentException("Selecciona un inmueble disponible.");
        if (arrendatario.getValue() == null) throw new IllegalArgumentException("Selecciona un arrendatario.");
        if (inicio.getValue() == null) throw new IllegalArgumentException("Indica la fecha de inicio.");
        int cantidad = entero(meses.getText(), "La duración");
        int dia = entero(diaPago.getText(), "El día de pago");
        BigDecimal monto = decimal(renta.getText());
        if (monto == null || monto.signum() <= 0) throw new IllegalArgumentException("La renta debe ser mayor que cero.");
        BigDecimal garantiaMonto = decimal(garantia.getText());
        if (garantiaMonto == null) garantiaMonto = BigDecimal.ZERO;
        BigDecimal inc = decimal(incremento.getText());
        BigDecimal mora = decimal(valorMora.getText());
        int gracia = entero(diasGracia.getText(), "Los días de gracia");
        Persona garanteSeleccionado = garante.getValue();
        if (garanteSeleccionado != null && garanteSeleccionado.getId().equals(arrendatario.getValue().getId())) {
            throw new IllegalArgumentException("El garante debe ser distinto del arrendatario.");
        }
        return new ContratoService.Solicitud(
                inmueble.getValue().getId(), arrendatario.getValue().getId(),
                garanteSeleccionado == null ? null : garanteSeleccionado.getId(),
                inicio.getValue(), cantidad, dia, monto,
                inc == null ? BigDecimal.ZERO : inc, garantiaMonto, garantiaRecibida.isSelected(),
                gracia, tipoMora.getValue(), mora == null ? BigDecimal.ZERO : mora,
                observaciones.getText());
    }

    @FXML private void onCancelar() {
        limpiar();
        principal.irA("contratos");
    }

    private static int entero(String valor, String campo) {
        try {
            return Integer.parseInt(valor == null ? "" : valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " debe ser un número entero.");
        }
    }

    private static BigDecimal decimal(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return new BigDecimal(valor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El importe debe ser un número válido.");
        }
    }

    private static BigDecimal nvl(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }

    private static String mensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getMessage();
    }

    private record Carga(List<Inmueble> inmuebles, List<Persona> personas, Contrato renovacion,
                         TipoMora tipoMora, BigDecimal valorMora, int gracia) { }
}

package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.*;
import com.ejemplo.alquileres.ui.*;
import java.math.BigDecimal;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class InmuebleController implements Refrescable {
    private final InmuebleService service;
    private final TipoInmuebleService tipos;
    private final PropietarioService propietarios;
    private final ReporteInmueblesExcelService excel;
    private final FichaInmuebleController fichaController;
    private final ContratoService contratos;
    private final Tareas tareas;
    private final Notificador aviso;
    private final Exportador exportador;
    private final ObservableList<Inmueble> datos = FXCollections.observableArrayList();
    private List<Inmueble> visibles = List.of();
    private Long id;

    @FXML private TextField buscar, codigo, nombre, direccion, referencia, distrito, provincia,
            departamento, piso, dormitorios, banos, estacionamientos, renta, area;
    @FXML private TextArea serviciosIncluidos, descripcion;
    @FXML private CheckBox amoblado;
    @FXML private ComboBox<EstadoInmueble> filtroEstado, estado;
    @FXML private ComboBox<TipoInmueble> tipo;
    @FXML private ComboBox<Propietario> propietario;
    @FXML private TableView<Inmueble> tabla;
    @FXML private TableColumn<Inmueble, String> colCodigo, colNombre, colTipo, colPropietario, colEstado;
    @FXML private TableColumn<Inmueble, BigDecimal> colRenta;
    @FXML private VBox drawer, ficha;
    @FXML private Region overlay;
    @FXML private HBox campoRenta;
    @FXML private Label titulo, fichaTitulo, fichaDatos, fichaFinanzas;
    @FXML private Label errorCodigo, errorNombre;
    @FXML private TableView<Contrato> fichaContratos;
    @FXML private TableColumn<Contrato, String> colFichaContratoCodigo, colFichaContratoArrendatario,
            colFichaContratoVigencia, colFichaContratoEstado;
    @FXML private TableColumn<Contrato, BigDecimal> colFichaContratoRenta;
    @FXML private TableView<Mantenimiento> fichaMantenimientos;
    @FXML private TableColumn<Mantenimiento, String> colFichaMantCodigo, colFichaMantTitulo,
            colFichaMantEstado, colFichaMantPrioridad;
    @FXML private TableColumn<Mantenimiento, BigDecimal> colFichaMantCosto;

    public InmuebleController(InmuebleService s, TipoInmuebleService ti, PropietarioService pr,
                              ReporteInmueblesExcelService e, FichaInmuebleController fichaController,
                              ContratoService contratos, Tareas t, Notificador a, Exportador x) {
        service = s;
        tipos = ti;
        propietarios = pr;
        excel = e;
        this.fichaController = fichaController;
        this.contratos = contratos;
        tareas = t;
        aviso = a;
        exportador = x;
    }

    @FXML public void initialize() {
        limitar(referencia, 250);
        limitar(distrito, 100);
        limitar(provincia, 100);
        limitar(departamento, 100);
        limitar(piso, 30);
        limitar(serviciosIncluidos, 1000);
        limitar(descripcion, 1000);
        Formularios.soloDigitos(dormitorios, 3);
        Formularios.soloDigitos(banos, 3);
        Formularios.soloDigitos(estacionamientos, 3);
        filtroEstado.setItems(FXCollections.observableArrayList(EstadoInmueble.values()));
        estadosEditables();
        tipo.setConverter(new StringConverter<>() {
            public String toString(TipoInmueble valor) {
                return valor == null ? "" : valor.getNombre() + " · " + valor.getCodigo();
            }
            public TipoInmueble fromString(String texto) { return null; }
        });
        propietario.setConverter(new StringConverter<>() {
            public String toString(Propietario valor) {
                return valor == null ? "" : valor.getNombre() + " · "
                        + valor.getTipoDocumento() + " " + valor.getNumeroDocumento();
            }
            public Propietario fromString(String texto) { return null; }
        });
        codigo.textProperty().addListener((o, a, b) -> ocultarError(errorCodigo));
        nombre.textProperty().addListener((o, a, b) -> ocultarError(errorNombre));
        renta.focusedProperty().addListener((o, a, b) -> {
            if (b) campoRenta.getStyleClass().add("focused");
            else campoRenta.getStyleClass().remove("focused");
        });
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNombre() + " · " + n(c.getValue().getDireccion())));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTipo() == null ? "Sin tipo" : c.getValue().getTipo().getNombre()));
        colPropietario.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPropietario() == null ? "Sin propietario" : c.getValue().getPropietario().getNombre()));
        colRenta.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getRentaReferencial()));
        colRenta.setCellFactory(x -> moneda());
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().getEtiqueta()));
        tabla.setItems(datos);
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        filtroEstado.valueProperty().addListener((o, a, b) -> refrescar());
        tabla.setOnMouseClicked(e -> { if (e.getClickCount() == 2) verFicha(tabla.getSelectionModel().getSelectedItem()); });

        colFichaContratoCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colFichaContratoArrendatario.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getArrendatario().getNombreCompleto()));
        colFichaContratoVigencia.setCellValueFactory(c -> new SimpleStringProperty(
                Fechas.dia(c.getValue().getFechaInicio()) + " al " + Fechas.dia(c.getValue().getFechaFin())));
        colFichaContratoEstado.setCellValueFactory(c -> new SimpleStringProperty(
                contratos.estadoVisual(c.getValue()).getEtiqueta()));
        colFichaContratoRenta.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getRentaMensual()));
        colFichaContratoRenta.setCellFactory(x -> moneda());

        colFichaMantCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colFichaMantTitulo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitulo()));
        colFichaMantEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().getEtiqueta()));
        colFichaMantPrioridad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrioridad().getEtiqueta()));
        colFichaMantCosto.setCellValueFactory(c -> new SimpleObjectProperty<>(
                c.getValue().getCostoReal() == null ? c.getValue().getCostoEstimado() : c.getValue().getCostoReal()));
        colFichaMantCosto.setCellFactory(x -> moneda());
        fichaContratos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        fichaMantenimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @Override public void refrescar() {
        String texto = buscar.getText();
        EstadoInmueble filtro = filtroEstado.getValue();
        tareas.ejecutar(() -> service.listar(texto, filtro, null, 0, 100),
                this::mostrar, e -> aviso.error(msg(e)));
    }

    private void mostrar(Page<Inmueble> p) {
        visibles = p.getContent();
        datos.setAll(visibles);
    }

    @FXML private void onExportar() {
        java.io.File destino = exportador.elegirDestino(ficha.getScene() == null ? null : ficha.getScene().getWindow(),
                "reporte-inmuebles-" + java.time.LocalDate.now() + ".xlsx", "Excel", "xlsx");
        if (destino == null) return;
        tareas.ejecutar(() -> excel.generar(visibles),
                bytes -> { exportador.escribir(destino, bytes); aviso.info("Excel generado."); },
                e -> aviso.error(msg(e)));
    }

    @FXML private void onNuevo() {
        id = null;
        limpiarErrores();
        codigo.clear();
        nombre.clear();
        direccion.clear();
        referencia.clear();
        distrito.clear();
        provincia.clear();
        departamento.clear();
        piso.clear();
        dormitorios.clear();
        banos.clear();
        estacionamientos.clear();
        amoblado.setSelected(false);
        serviciosIncluidos.clear();
        descripcion.clear();
        renta.clear();
        area.clear();
        tipo.setValue(null);
        propietario.setValue(null);
        estadosEditables();
        estado.setValue(EstadoInmueble.DISPONIBLE);
        titulo.setText("Nuevo inmueble");
        abrir();
        tareas.ejecutar(service::proximoCodigo, x -> codigo.setText(x), e -> aviso.error(msg(e)));
        cargarOpciones();
    }

    @FXML private void onEditar() { editar(tabla.getSelectionModel().getSelectedItem()); }

    private void editar(Inmueble i) {
        if (i == null) return;
        limpiarErrores();
        id = i.getId();
        codigo.setText(i.getCodigo());
        nombre.setText(i.getNombre());
        direccion.setText(texto(i.getDireccion()));
        referencia.setText(texto(i.getReferencia()));
        distrito.setText(texto(i.getDistrito()));
        provincia.setText(texto(i.getProvincia()));
        departamento.setText(texto(i.getDepartamento()));
        piso.setText(texto(i.getPiso()));
        dormitorios.setText(cantidad(i.getDormitorios()));
        banos.setText(cantidad(i.getBanos()));
        estacionamientos.setText(cantidad(i.getEstacionamientos()));
        amoblado.setSelected(i.isAmoblado());
        serviciosIncluidos.setText(texto(i.getServiciosIncluidos()));
        descripcion.setText(texto(i.getDescripcion()));
        renta.setText(i.getRentaReferencial() == null ? "" : i.getRentaReferencial().toPlainString());
        area.setText(i.getAreaM2() == null ? "" : i.getAreaM2().toPlainString());
        if (i.getEstado() == EstadoInmueble.ALQUILADO) {
            estado.setItems(FXCollections.observableArrayList(EstadoInmueble.values()));
            estado.setValue(EstadoInmueble.ALQUILADO);
            estado.setDisable(true);
        } else {
            estadosEditables();
            estado.setValue(i.getEstado());
        }
        titulo.setText("Editar inmueble");
        cargarOpciones();
        tareas.ejecutar(() -> new Object[]{i.getTipo(), i.getPropietario()}, x -> {
            tipo.setValue((TipoInmueble) x[0]);
            propietario.setValue((Propietario) x[1]);
            abrir();
        }, e -> aviso.error(msg(e)));
    }

    private void estadosEditables() {
        estado.setItems(FXCollections.observableArrayList(
                EstadoInmueble.DISPONIBLE, EstadoInmueble.MANTENIMIENTO, EstadoInmueble.INACTIVO));
        estado.setDisable(false);
    }

    private void cargarOpciones() {
        tareas.ejecutar(() -> List.of(tipos.activos(), propietarios.activos()), x -> {
            tipo.setItems(FXCollections.observableArrayList((List<TipoInmueble>) x.get(0)));
            propietario.setItems(FXCollections.observableArrayList((List<Propietario>) x.get(1)));
        }, e -> aviso.error(msg(e)));
    }

    @FXML private void onGuardar() {
        limpiarErrores();
        if (!validarFormulario()) return;
        try {
            Inmueble i = Inmueble.builder().id(id).codigo(codigo.getText()).nombre(nombre.getText())
                    .direccion(direccion.getText()).referencia(referencia.getText())
                    .distrito(distrito.getText()).provincia(provincia.getText())
                    .departamento(departamento.getText()).piso(piso.getText())
                    .dormitorios(entero(dormitorios.getText())).banos(entero(banos.getText()))
                    .estacionamientos(entero(estacionamientos.getText())).amoblado(amoblado.isSelected())
                    .serviciosIncluidos(serviciosIncluidos.getText()).descripcion(descripcion.getText())
                    .tipo(tipo.getValue()).propietario(propietario.getValue())
                    .areaM2(decimal(area.getText())).rentaReferencial(decimal(renta.getText()))
                    .estado(estado.getValue()).build();
            tareas.ejecutar(() -> service.guardar(i), x -> {
                cerrar();
                aviso.info("Inmueble guardado.");
                refrescar();
            }, e -> aviso.error(msg(e)));
        } catch (NumberFormatException e) {
            aviso.error("Área y renta deben ser números válidos.");
        }
    }

    private boolean validarFormulario() {
        boolean valido = true;
        if (codigo.getText() == null || codigo.getText().isBlank()) {
            mostrarError(errorCodigo, "El código es obligatorio");
            valido = false;
        }
        if (nombre.getText() == null || nombre.getText().isBlank()) {
            mostrarError(errorNombre, "Ingresa un nombre o alias");
            valido = false;
        }
        return valido;
    }

    private void mostrarError(Label error, String texto) {
        if (error == null) return;
        error.setText(texto);
        error.setVisible(true);
        error.setManaged(true);
    }

    private void ocultarError(Label error) {
        if (error == null) return;
        error.setText("");
        error.setVisible(false);
        error.setManaged(false);
    }

    private void limpiarErrores() {
        ocultarError(errorCodigo);
        ocultarError(errorNombre);
    }

    @FXML private void onFicha() { verFicha(tabla.getSelectionModel().getSelectedItem()); }

    private void verFicha(Inmueble i) {
        if (i == null) return;
        drawer.setVisible(false);
        drawer.setManaged(false);
        fichaTitulo.setText(i.getCodigo() + " · " + i.getNombre());
        fichaDatos.setText("Cargando ficha…");
        fichaContratos.setItems(FXCollections.observableArrayList());
        fichaMantenimientos.setItems(FXCollections.observableArrayList());
        fichaFinanzas.setText("");
        ficha.setVisible(true);
        ficha.setManaged(true);
        overlay.setVisible(true);
        overlay.setManaged(true);
        tareas.ejecutar(() -> fichaController.cargar(i), this::mostrarFicha, e -> aviso.error(msg(e)));
    }

    private void mostrarFicha(FichaInmuebleController.Ficha f) {
        Inmueble i = f.inmueble();
        fichaTitulo.setText(i.getCodigo() + " · " + i.getNombre());
        fichaDatos.setText(
                "Código: " + i.getCodigo()
                        + "\nNombre: " + n(i.getNombre())
                        + "\nTipo: " + (i.getTipo() == null ? "—" : i.getTipo().getNombre())
                        + "\nPropietario: " + (i.getPropietario() == null ? "—" : i.getPropietario().getNombre())
                        + "\nDirección: " + n(i.getDireccion())
                        + "\nReferencia: " + n(i.getReferencia())
                        + "\nDistrito: " + n(i.getDistrito())
                        + "\nProvincia: " + n(i.getProvincia())
                        + "\nDepartamento: " + n(i.getDepartamento())
                        + "\nPiso: " + n(i.getPiso())
                        + "\nÁrea: " + (i.getAreaM2() == null ? "—" : i.getAreaM2().toPlainString() + " m²")
                        + "\nDormitorios: " + numero(i.getDormitorios())
                        + "\nBaños: " + numero(i.getBanos())
                        + "\nEstacionamientos: " + numero(i.getEstacionamientos())
                        + "\nAmoblado: " + (i.isAmoblado() ? "Sí" : "No")
                        + "\nServicios incluidos: " + n(i.getServiciosIncluidos())
                        + "\nDescripción: " + n(i.getDescripcion())
                        + "\nRenta referencial: " + Tablas.moneda(i.getRentaReferencial())
                        + "\nEstado: " + i.getEstado().getEtiqueta()
                        + "\nFecha de registro: " + n(Fechas.dia(i.getFechaRegistro())));
        fichaContratos.setItems(FXCollections.observableArrayList(f.contratos()));
        fichaMantenimientos.setItems(FXCollections.observableArrayList(f.mantenimientos()));
        RentabilidadService.Resumen finanzas = f.finanzas();
        fichaFinanzas.setText("Año actual\n\nIngresos: " + Tablas.moneda(finanzas.ingresos())
                + "\nGastos: " + Tablas.moneda(finanzas.gastos())
                + "\nNeto: " + Tablas.moneda(finanzas.neto())
                + "\nMargen: " + finanzas.margen().toPlainString() + " %");
    }

    @FXML private void onCerrar() { cerrar(); }

    @FXML private void onCerrarFicha() { cerrar(); }

    private void abrir() {
        ficha.setVisible(false);
        ficha.setManaged(false);
        drawer.setVisible(true);
        drawer.setManaged(true);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    private void cerrar() {
        drawer.setVisible(false);
        drawer.setManaged(false);
        ficha.setVisible(false);
        ficha.setManaged(false);
        overlay.setVisible(false);
        overlay.setManaged(false);
        limpiarErrores();
    }

    private <T> TableCell<T, BigDecimal> moneda() {
        return new TableCell<>() {
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : Tablas.moneda(v));
            }
        };
    }

    private void limitar(TextInputControl campo, int maximo) {
        campo.setTextFormatter(new TextFormatter<>(c ->
                c.getControlNewText().length() <= maximo ? c : null));
    }

    private BigDecimal decimal(String v) {
        return v == null || v.isBlank() ? null : new BigDecimal(v.trim().replace(',', '.'));
    }

    private int entero(String v) {
        return v == null || v.isBlank() ? 0 : Integer.parseInt(v.trim());
    }

    private String texto(String v) { return v == null ? "" : v; }

    private String cantidad(Integer v) { return v == null ? "" : String.valueOf(v); }

    private String numero(Integer v) { return v == null ? "—" : String.valueOf(v); }

    private String n(String v) { return v == null || v.isBlank() ? "—" : v; }
    private String msg(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

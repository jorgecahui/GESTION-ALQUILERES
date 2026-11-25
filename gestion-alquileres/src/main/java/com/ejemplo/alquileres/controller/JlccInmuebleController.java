package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.*;
import com.ejemplo.alquileres.ui.*;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JlccInmuebleController implements JlccRefrescable {
    private static final TipoFiltro JLCC_TODOS_LOS_TIPOS = new TipoFiltro(null, "Todos los tipos");

    private final JlccInmuebleService jlccService;
    private final JlccTipoInmuebleService jlccTipos;
    private final JlccPropietarioService jlccPropietarios;
    private final JlccReporteInmueblesExcelService jlccExcel;
    private final JlccFichaInmuebleController jlccFichaController;
    private final JlccContratoService jlccContratos;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccExportador jlccExportador;
    private final ObservableList<Inmueble> jlccDatos = FXCollections.observableArrayList();
    private List<Inmueble> jlccVisibles = List.of();
    private Long jlccId;
    private boolean jlccActualizandoTipos;

    @FXML
    private TextField buscar,
            codigo,
            nombre,
            direccion,
            referencia,
            distrito,
            provincia,
            departamento,
            piso,
            nivel,
            orden,
            dormitorios,
            banos,
            estacionamientos,
            renta,
            area;
    @FXML private TextArea serviciosIncluidos, descripcion;
    @FXML private CheckBox amoblado;
    @FXML private ComboBox<EstadoInmueble> filtroEstado, estado;
    @FXML private ComboBox<TipoInmueble> tipo;
    @FXML private ComboBox<TipoFiltro> filtroTipo;
    @FXML private ComboBox<Propietario> propietario;
    @FXML private TableView<Inmueble> tabla;
    @FXML
    private TableColumn<Inmueble, String> colCodigo, colNombre, colTipo, colPropietario, colEstado;
    @FXML private TableColumn<Inmueble, BigDecimal> colRenta;
    @FXML private VBox drawer, ficha;
    @FXML private Region overlay;
    @FXML private HBox campoRenta;
    @FXML private Label titulo, fichaTitulo, fichaDatos, fichaFinanzas;
    @FXML private Label errorCodigo, errorNombre;
    @FXML private TableView<Contrato> fichaContratos;
    @FXML
    private TableColumn<Contrato, String> colFichaContratoCodigo,
            colFichaContratoArrendatario,
            colFichaContratoVigencia,
            colFichaContratoEstado;
    @FXML private TableColumn<Contrato, BigDecimal> colFichaContratoRenta;
    @FXML private TableView<Mantenimiento> fichaMantenimientos;
    @FXML
    private TableColumn<Mantenimiento, String> colFichaMantCodigo,
            colFichaMantTitulo,
            colFichaMantEstado,
            colFichaMantPrioridad;
    @FXML private TableColumn<Mantenimiento, BigDecimal> colFichaMantCosto;

    public JlccInmuebleController(
            JlccInmuebleService s,
            JlccTipoInmuebleService jlccTi,
            JlccPropietarioService jlccPr,
            JlccReporteInmueblesExcelService e,
            JlccFichaInmuebleController jlccFichaController,
            JlccContratoService jlccContratos,
            JlccTareas t,
            JlccNotificador a,
            JlccExportador x) {
        jlccService = s;
        jlccTipos = jlccTi;
        jlccPropietarios = jlccPr;
        jlccExcel = e;
        this.jlccFichaController = jlccFichaController;
        this.jlccContratos = jlccContratos;
        jlccTareas = t;
        jlccAviso = a;
        jlccExportador = x;
    }

    @FXML
    public void initialize() {
        jlccLimitar(referencia, 250);
        jlccLimitar(distrito, 100);
        jlccLimitar(provincia, 100);
        jlccLimitar(departamento, 100);
        jlccLimitar(piso, 30);
        jlccLimitarNivel(nivel);
        JlccFormularios.jlccSoloDigitos(orden, 3);
        jlccLimitar(serviciosIncluidos, 1000);
        jlccLimitar(descripcion, 1000);
        JlccFormularios.jlccSoloDigitos(dormitorios, 3);
        JlccFormularios.jlccSoloDigitos(banos, 3);
        JlccFormularios.jlccSoloDigitos(estacionamientos, 3);
        filtroEstado.setItems(FXCollections.observableArrayList(EstadoInmueble.values()));
        jlccEstadosEditables();
        filtroTipo.setConverter(
                new StringConverter<>() {
                    public String toString(TipoFiltro valor) {
                        return valor == null ? JLCC_TODOS_LOS_TIPOS.nombre() : valor.nombre();
                    }

                    public TipoFiltro fromString(String texto) {
                        return null;
                    }
                });
        filtroTipo.setItems(FXCollections.observableArrayList(JLCC_TODOS_LOS_TIPOS));
        filtroTipo.setValue(JLCC_TODOS_LOS_TIPOS);
        tipo.setConverter(
                new StringConverter<>() {
                    public String toString(TipoInmueble valor) {
                        return valor == null ? "" : valor.getNombre() + " · " + valor.getCodigo();
                    }

                    public TipoInmueble fromString(String texto) {
                        return null;
                    }
                });
        propietario.setConverter(
                new StringConverter<>() {
                    public String toString(Propietario valor) {
                        return valor == null
                                ? ""
                                : valor.getNombre()
                                        + " · "
                                        + valor.getTipoDocumento()
                                        + " "
                                        + valor.getNumeroDocumento();
                    }

                    public Propietario fromString(String texto) {
                        return null;
                    }
                });
        codigo.textProperty().addListener((o, a, b) -> jlccOcultarError(errorCodigo));
        nombre.textProperty().addListener((o, a, b) -> jlccOcultarError(errorNombre));
        renta.focusedProperty()
                .addListener(
                        (o, a, b) -> {
                            if (b) campoRenta.getStyleClass().add("focused");
                            else campoRenta.getStyleClass().remove("focused");
                        });
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colNombre.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getNombre() + " · " + jlccN(c.getValue().getDireccion())));
        colTipo.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getTipo() == null
                                        ? "Sin tipo"
                                        : c.getValue().getTipo().getNombre()));
        colPropietario.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getPropietario() == null
                                        ? "Sin propietario"
                                        : c.getValue().getPropietario().getNombre()));
        colRenta.setCellValueFactory(
                c -> new SimpleObjectProperty<>(c.getValue().getRentaReferencial()));
        colRenta.setCellFactory(x -> jlccMoneda());
        colEstado.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getEstado().getEtiqueta()));
        tabla.setItems(jlccDatos);
        buscar.textProperty().addListener((o, a, b) -> jlccRefrescarLista());
        filtroEstado.valueProperty().addListener((o, a, b) -> jlccRefrescarLista());
        filtroTipo
                .valueProperty()
                .addListener(
                        (o, a, b) -> {
                            if (!jlccActualizandoTipos) jlccRefrescarLista();
                        });
        tabla.setOnMouseClicked(
                e -> {
                    if (e.getClickCount() == 2)
                        jlccVerFicha(tabla.getSelectionModel().getSelectedItem());
                });

        colFichaContratoCodigo.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colFichaContratoArrendatario.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getArrendatario().getNombreCompleto()));
        colFichaContratoVigencia.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                JlccFechas.jlccDia(c.getValue().getFechaInicio())
                                        + " al "
                                        + JlccFechas.jlccDia(c.getValue().getFechaFin())));
        colFichaContratoEstado.setCellValueFactory(
                c -> new SimpleStringProperty(jlccContratos.jlccEstadoVisual(c.getValue()).jlccGetEtiqueta()));
        colFichaContratoRenta.setCellValueFactory(
                c -> new SimpleObjectProperty<>(c.getValue().getRentaMensual()));
        colFichaContratoRenta.setCellFactory(x -> jlccMoneda());

        colFichaMantCodigo.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colFichaMantTitulo.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getTitulo()));
        colFichaMantEstado.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getEstado().getEtiqueta()));
        colFichaMantPrioridad.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getPrioridad().getEtiqueta()));
        colFichaMantCosto.setCellValueFactory(
                c ->
                        new SimpleObjectProperty<>(
                                c.getValue().getCostoReal() == null
                                        ? c.getValue().getCostoEstimado()
                                        : c.getValue().getCostoReal()));
        colFichaMantCosto.setCellFactory(x -> jlccMoneda());
        fichaContratos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        fichaMantenimientos.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @Override
    public void refrescar() {
        jlccCargarTiposFiltro();
    }

    private void jlccRefrescarLista() {
        if (buscar == null || filtroEstado == null || filtroTipo == null) return;
        String jlccTexto = buscar.getText();
        EstadoInmueble jlccFiltro = filtroEstado.getValue();
        TipoFiltro jlccElegido = filtroTipo.getValue();
        Long jlccTipoId = jlccElegido == null ? null : jlccElegido.id();
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccListar(jlccTexto, jlccFiltro, jlccTipoId, 0, 100),
                this::jlccMostrar,
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private void jlccCargarTiposFiltro() {
        TipoFiltro jlccActual = filtroTipo == null ? null : filtroTipo.getValue();
        Long jlccSeleccionado = jlccActual == null ? null : jlccActual.id();
        jlccTareas.jlccEjecutar(
                jlccTipos::jlccActivos,
                lista -> {
                    List<TipoFiltro> jlccOpciones = new ArrayList<>();
                    jlccOpciones.add(JLCC_TODOS_LOS_TIPOS);
                    for (TipoInmueble jlccTipo : lista) {
                        jlccOpciones.add(new TipoFiltro(jlccTipo.getId(), jlccTipo.getNombre()));
                    }
                    TipoFiltro jlccElegido =
                            jlccOpciones.stream()
                                    .filter(
                                            opcion ->
                                                    opcion.id() == null
                                                            ? jlccSeleccionado == null
                                                            : opcion.id().equals(jlccSeleccionado))
                                    .findFirst()
                                    .orElse(JLCC_TODOS_LOS_TIPOS);
                    jlccActualizandoTipos = true;
                    try {
                        filtroTipo.setItems(FXCollections.observableArrayList(jlccOpciones));
                        filtroTipo.setValue(jlccElegido);
                    } finally {
                        jlccActualizandoTipos = false;
                    }
                    jlccRefrescarLista();
                },
                e -> {
                    jlccAviso.jlccError(jlccMsg(e));
                    jlccRefrescarLista();
                });
    }

    private void jlccMostrar(Page<Inmueble> p) {
        jlccVisibles = p.getContent();
        jlccDatos.setAll(jlccVisibles);
    }

    @FXML
    private void jlccOnExportar() {
        java.io.File jlccDestino =
                jlccExportador.jlccElegirDestino(
                        ficha.getScene() == null ? null : ficha.getScene().getWindow(),
                        "reporte-inmuebles-" + java.time.LocalDate.now() + ".xlsx",
                        "Excel",
                        "xlsx");
        if (jlccDestino == null) return;
        jlccTareas.jlccEjecutar(
                () -> jlccExcel.jlccGenerar(jlccVisibles),
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("Excel generado.");
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    @FXML
    private void jlccOnNuevo() {
        jlccId = null;
        jlccLimpiarErrores();
        codigo.clear();
        nombre.clear();
        direccion.clear();
        referencia.clear();
        distrito.clear();
        provincia.clear();
        departamento.clear();
        piso.clear();
        nivel.clear();
        orden.clear();
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
        jlccEstadosEditables();
        estado.setValue(EstadoInmueble.DISPONIBLE);
        titulo.setText("Nuevo inmueble");
        jlccAbrir();
        jlccTareas.jlccEjecutar(jlccService::jlccProximoCodigo, x -> codigo.setText(x), e -> jlccAviso.jlccError(jlccMsg(e)));
        jlccCargarOpciones();
    }

    @FXML
    private void jlccOnEditar() {
        jlccEditar(tabla.getSelectionModel().getSelectedItem());
    }

    private void jlccEditar(Inmueble i) {
        if (i == null) return;
        jlccLimpiarErrores();
        jlccId = i.getId();
        codigo.setText(i.getCodigo());
        nombre.setText(i.getNombre());
        direccion.setText(jlccTexto(i.getDireccion()));
        referencia.setText(jlccTexto(i.getReferencia()));
        distrito.setText(jlccTexto(i.getDistrito()));
        provincia.setText(jlccTexto(i.getProvincia()));
        departamento.setText(jlccTexto(i.getDepartamento()));
        piso.setText(jlccTexto(i.getPiso()));
        nivel.setText(jlccCantidad(i.getNivel()));
        orden.setText(jlccCantidad(i.getOrden()));
        dormitorios.setText(jlccCantidad(i.getDormitorios()));
        banos.setText(jlccCantidad(i.getBanos()));
        estacionamientos.setText(jlccCantidad(i.getEstacionamientos()));
        amoblado.setSelected(i.isAmoblado());
        serviciosIncluidos.setText(jlccTexto(i.getServiciosIncluidos()));
        descripcion.setText(jlccTexto(i.getDescripcion()));
        renta.setText(
                i.getRentaReferencial() == null ? "" : i.getRentaReferencial().toPlainString());
        area.setText(i.getAreaM2() == null ? "" : i.getAreaM2().toPlainString());
        if (i.getEstado() == EstadoInmueble.ALQUILADO) {
            estado.setItems(FXCollections.observableArrayList(EstadoInmueble.values()));
            estado.setValue(EstadoInmueble.ALQUILADO);
            estado.setDisable(true);
        } else {
            jlccEstadosEditables();
            estado.setValue(i.getEstado());
        }
        titulo.setText("Editar inmueble");
        jlccCargarOpciones();
        jlccTareas.jlccEjecutar(
                () -> new Object[] {i.getTipo(), i.getPropietario()},
                x -> {
                    tipo.setValue((TipoInmueble) x[0]);
                    propietario.setValue((Propietario) x[1]);
                    jlccAbrir();
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private void jlccEstadosEditables() {
        estado.setItems(
                FXCollections.observableArrayList(
                        EstadoInmueble.DISPONIBLE,
                        EstadoInmueble.MANTENIMIENTO,
                        EstadoInmueble.INACTIVO));
        estado.setDisable(false);
    }

    private void jlccCargarOpciones() {
        jlccTareas.jlccEjecutar(
                () -> List.of(jlccTipos.jlccActivos(), jlccPropietarios.jlccActivos()),
                x -> {
                    tipo.setItems(FXCollections.observableArrayList((List<TipoInmueble>) x.get(0)));
                    propietario.setItems(
                            FXCollections.observableArrayList((List<Propietario>) x.get(1)));
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    @FXML
    private void jlccOnGuardar() {
        jlccLimpiarErrores();
        if (!jlccValidarFormulario()) return;
        try {
            Inmueble i =
                    Inmueble.builder()
                            .id(jlccId)
                            .codigo(codigo.getText())
                            .nombre(nombre.getText())
                            .direccion(direccion.getText())
                            .referencia(referencia.getText())
                            .distrito(distrito.getText())
                            .provincia(provincia.getText())
                            .departamento(departamento.getText())
                            .piso(piso.getText())
                            .nivel(jlccEnteroNulo(nivel.getText()))
                            .orden(jlccEnteroNulo(orden.getText()))
                            .dormitorios(jlccEntero(dormitorios.getText()))
                            .banos(jlccEntero(banos.getText()))
                            .estacionamientos(jlccEntero(estacionamientos.getText()))
                            .amoblado(amoblado.isSelected())
                            .serviciosIncluidos(serviciosIncluidos.getText())
                            .descripcion(descripcion.getText())
                            .tipo(tipo.getValue())
                            .propietario(propietario.getValue())
                            .areaM2(jlccDecimal(area.getText()))
                            .rentaReferencial(jlccDecimal(renta.getText()))
                            .estado(estado.getValue())
                            .build();
            jlccTareas.jlccEjecutar(
                    () -> jlccService.jlccGuardar(i),
                    x -> {
                        jlccCerrar();
                        jlccAviso.jlccInfo("Inmueble guardado.");
                        refrescar();
                    },
                    e -> jlccAviso.jlccError(jlccMsg(e)));
        } catch (NumberFormatException e) {
            jlccAviso.jlccError("Área y renta deben ser números válidos.");
        }
    }

    private boolean jlccValidarFormulario() {
        boolean jlccValido = true;
        if (codigo.getText() == null || codigo.getText().isBlank()) {
            jlccMostrarError(errorCodigo, "El código es obligatorio");
            jlccValido = false;
        }
        if (nombre.getText() == null || nombre.getText().isBlank()) {
            jlccMostrarError(errorNombre, "Ingresa un nombre o alias");
            jlccValido = false;
        }
        return jlccValido;
    }

    private void jlccMostrarError(Label jlccError, String jlccTexto) {
        if (jlccError == null) return;
        jlccError.setText(jlccTexto);
        jlccError.setVisible(true);
        jlccError.setManaged(true);
    }

    private void jlccOcultarError(Label jlccError) {
        if (jlccError == null) return;
        jlccError.setText("");
        jlccError.setVisible(false);
        jlccError.setManaged(false);
    }

    private void jlccLimpiarErrores() {
        jlccOcultarError(errorCodigo);
        jlccOcultarError(errorNombre);
    }

    @FXML
    private void jlccOnFicha() {
        jlccVerFicha(tabla.getSelectionModel().getSelectedItem());
    }

    private void jlccVerFicha(Inmueble i) {
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
        jlccTareas.jlccEjecutar(
                () -> jlccFichaController.jlccCargar(i), this::jlccMostrarFicha, e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private void jlccMostrarFicha(JlccFichaInmuebleController.Ficha f) {
        Inmueble i = f.inmueble();
        fichaTitulo.setText(i.getCodigo() + " · " + i.getNombre());
        fichaDatos.setText(
                "Código: "
                        + i.getCodigo()
                        + "\nNombre: "
                        + jlccN(i.getNombre())
                        + "\nTipo: "
                        + (i.getTipo() == null ? "—" : i.getTipo().getNombre())
                        + "\nPropietario: "
                        + (i.getPropietario() == null ? "—" : i.getPropietario().getNombre())
                        + "\nDirección: "
                        + jlccN(i.getDireccion())
                        + "\nReferencia: "
                        + jlccN(i.getReferencia())
                        + "\nDistrito: "
                        + jlccN(i.getDistrito())
                        + "\nProvincia: "
                        + jlccN(i.getProvincia())
                        + "\nDepartamento: "
                        + jlccN(i.getDepartamento())
                        + "\nPiso: "
                        + jlccN(i.getPiso())
                        + "\nNivel: "
                        + jlccNumero(i.getNivel())
                        + "\nOrden: "
                        + jlccNumero(i.getOrden())
                        + "\nÁrea: "
                        + (i.getAreaM2() == null ? "—" : i.getAreaM2().toPlainString() + " m²")
                        + "\nDormitorios: "
                        + jlccNumero(i.getDormitorios())
                        + "\nBaños: "
                        + jlccNumero(i.getBanos())
                        + "\nEstacionamientos: "
                        + jlccNumero(i.getEstacionamientos())
                        + "\nAmoblado: "
                        + (i.isAmoblado() ? "Sí" : "No")
                        + "\nServicios incluidos: "
                        + jlccN(i.getServiciosIncluidos())
                        + "\nDescripción: "
                        + jlccN(i.getDescripcion())
                        + "\nRenta referencial: "
                        + JlccTablas.jlccMoneda(i.getRentaReferencial())
                        + "\nEstado: "
                        + i.getEstado().getEtiqueta()
                        + "\nFecha de registro: "
                        + jlccN(JlccFechas.jlccDia(i.getFechaRegistro())));
        fichaContratos.setItems(FXCollections.observableArrayList(f.contratos()));
        fichaMantenimientos.setItems(FXCollections.observableArrayList(f.mantenimientos()));
        JlccRentabilidadService.Resumen jlccFinanzas = f.finanzas();
        fichaFinanzas.setText(
                "Año actual\n\nIngresos: "
                        + JlccTablas.jlccMoneda(jlccFinanzas.ingresos())
                        + "\nGastos: "
                        + JlccTablas.jlccMoneda(jlccFinanzas.gastos())
                        + "\nNeto: "
                        + JlccTablas.jlccMoneda(jlccFinanzas.neto())
                        + "\nMargen: "
                        + jlccFinanzas.margen().toPlainString()
                        + " %");
    }

    @FXML
    private void jlccOnCerrar() {
        jlccCerrar();
    }

    @FXML
    private void jlccOnCerrarFicha() {
        jlccCerrar();
    }

    private void jlccAbrir() {
        ficha.setVisible(false);
        ficha.setManaged(false);
        drawer.setVisible(true);
        drawer.setManaged(true);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    private void jlccCerrar() {
        drawer.setVisible(false);
        drawer.setManaged(false);
        ficha.setVisible(false);
        ficha.setManaged(false);
        overlay.setVisible(false);
        overlay.setManaged(false);
        jlccLimpiarErrores();
    }

    private <T> TableCell<T, BigDecimal> jlccMoneda() {
        return new TableCell<>() {
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : JlccTablas.jlccMoneda(v));
            }
        };
    }

    private void jlccLimitar(TextInputControl jlccCampo, int jlccMaximo) {
        jlccCampo.setTextFormatter(
                new TextFormatter<>(c -> c.getControlNewText().length() <= jlccMaximo ? c : null));
    }

    private void jlccLimitarNivel(TextField jlccCampo) {
        jlccCampo.setTextFormatter(
                new TextFormatter<>(c -> c.getControlNewText().matches("-?\\d{0,3}") ? c : null));
    }

    private BigDecimal jlccDecimal(String v) {
        return v == null || v.isBlank() ? null : new BigDecimal(v.trim().replace(',', '.'));
    }

    private int jlccEntero(String v) {
        return v == null || v.isBlank() ? 0 : Integer.parseInt(v.trim());
    }

    private Integer jlccEnteroNulo(String v) {
        return v == null || v.isBlank() || "-".equals(v.trim()) ? null : Integer.parseInt(v.trim());
    }

    private String jlccTexto(String v) {
        return v == null ? "" : v;
    }

    private String jlccCantidad(Integer v) {
        return v == null ? "" : String.valueOf(v);
    }

    private String jlccNumero(Integer v) {
        return v == null ? "—" : String.valueOf(v);
    }

    private String jlccN(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private String jlccMsg(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }

    private record TipoFiltro(Long id, String nombre) {}
}

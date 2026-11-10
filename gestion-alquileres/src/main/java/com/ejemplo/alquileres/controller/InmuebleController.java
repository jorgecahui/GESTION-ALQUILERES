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
import javafx.scene.layout.VBox;
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

    @FXML private TextField buscar, codigo, nombre, direccion, renta, area;
    @FXML private ComboBox<EstadoInmueble> filtroEstado, estado;
    @FXML private ComboBox<TipoInmueble> tipo;
    @FXML private ComboBox<Propietario> propietario;
    @FXML private TableView<Inmueble> tabla;
    @FXML private TableColumn<Inmueble, String> colCodigo, colNombre, colTipo, colPropietario, colEstado;
    @FXML private TableColumn<Inmueble, BigDecimal> colRenta;
    @FXML private VBox drawer, ficha;
    @FXML private Label titulo, fichaTitulo, fichaDatos, fichaFinanzas;
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
        filtroEstado.setItems(FXCollections.observableArrayList(EstadoInmueble.values()));
        estado.setItems(FXCollections.observableArrayList(
                EstadoInmueble.DISPONIBLE, EstadoInmueble.MANTENIMIENTO, EstadoInmueble.INACTIVO));
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNombre() + "\n" + n(c.getValue().getDireccion())));
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
        tareas.ejecutar(() -> service.listar(buscar.getText(), filtroEstado.getValue(), null, 0, 100),
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
        nombre.clear();
        direccion.clear();
        renta.clear();
        area.clear();
        estado.setValue(EstadoInmueble.DISPONIBLE);
        titulo.setText("Nuevo inmueble");
        tareas.ejecutar(service::proximoCodigo, x -> { codigo.setText(x); abrir(); }, e -> aviso.error(msg(e)));
        cargarOpciones();
    }

    @FXML private void onEditar() { editar(tabla.getSelectionModel().getSelectedItem()); }

    private void editar(Inmueble i) {
        if (i == null) return;
        id = i.getId();
        codigo.setText(i.getCodigo());
        nombre.setText(i.getNombre());
        direccion.setText(n(i.getDireccion()));
        renta.setText(i.getRentaReferencial() == null ? "" : i.getRentaReferencial().toPlainString());
        area.setText(i.getAreaM2() == null ? "" : i.getAreaM2().toPlainString());
        estado.setValue(i.getEstado() == EstadoInmueble.ALQUILADO ? EstadoInmueble.DISPONIBLE : i.getEstado());
        titulo.setText("Editar inmueble");
        cargarOpciones();
        tareas.ejecutar(() -> new Object[]{i.getTipo(), i.getPropietario()}, x -> {
            tipo.setValue((TipoInmueble) x[0]);
            propietario.setValue((Propietario) x[1]);
            abrir();
        }, e -> aviso.error(msg(e)));
    }

    private void cargarOpciones() {
        tareas.ejecutar(() -> List.of(tipos.activos(), propietarios.activos()), x -> {
            tipo.setItems(FXCollections.observableArrayList((List<TipoInmueble>) x.get(0)));
            propietario.setItems(FXCollections.observableArrayList((List<Propietario>) x.get(1)));
        }, e -> aviso.error(msg(e)));
    }

    @FXML private void onGuardar() {
        try {
            Inmueble i = Inmueble.builder().id(id).codigo(codigo.getText()).nombre(nombre.getText())
                    .direccion(direccion.getText()).tipo(tipo.getValue()).propietario(propietario.getValue())
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

    @FXML private void onFicha() { verFicha(tabla.getSelectionModel().getSelectedItem()); }

    private void verFicha(Inmueble i) {
        if (i == null) return;
        fichaTitulo.setText(i.getCodigo() + " · " + i.getNombre());
        fichaDatos.setText("Cargando ficha…");
        fichaContratos.setItems(FXCollections.observableArrayList());
        fichaMantenimientos.setItems(FXCollections.observableArrayList());
        fichaFinanzas.setText("");
        ficha.setVisible(true);
        ficha.setManaged(true);
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
                        + "\nDistrito: " + n(i.getDistrito())
                        + "\nÁrea: " + (i.getAreaM2() == null ? "—" : i.getAreaM2().toPlainString() + " m²")
                        + "\nDormitorios: " + i.getDormitorios()
                        + "\nBaños: " + i.getBanos()
                        + "\nEstacionamientos: " + i.getEstacionamientos()
                        + "\nAmoblado: " + (i.isAmoblado() ? "Sí" : "No")
                        + "\nRenta referencial: " + Tablas.moneda(i.getRentaReferencial())
                        + "\nEstado: " + i.getEstado().getEtiqueta());
        fichaContratos.setItems(FXCollections.observableArrayList(f.contratos()));
        fichaMantenimientos.setItems(FXCollections.observableArrayList(f.mantenimientos()));
        RentabilidadService.Resumen finanzas = f.finanzas();
        fichaFinanzas.setText("Año actual\n\nIngresos: " + Tablas.moneda(finanzas.ingresos())
                + "\nGastos: " + Tablas.moneda(finanzas.gastos())
                + "\nNeto: " + Tablas.moneda(finanzas.neto())
                + "\nMargen: " + finanzas.margen().toPlainString() + " %");
    }

    @FXML private void onCerrar() { cerrar(); }

    @FXML private void onCerrarFicha() {
        ficha.setVisible(false);
        ficha.setManaged(false);
    }

    private void abrir() {
        drawer.setVisible(true);
        drawer.setManaged(true);
    }

    private void cerrar() {
        drawer.setVisible(false);
        drawer.setManaged(false);
    }

    private <T> TableCell<T, BigDecimal> moneda() {
        return new TableCell<>() {
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : Tablas.moneda(v));
            }
        };
    }

    private BigDecimal decimal(String v) {
        return v == null || v.isBlank() ? null : new BigDecimal(v.trim().replace(',', '.'));
    }

    private String n(String v) { return v == null || v.isBlank() ? "—" : v; }
    private String msg(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

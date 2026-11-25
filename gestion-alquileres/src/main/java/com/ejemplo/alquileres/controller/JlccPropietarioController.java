package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.JlccPropietarioService;
import com.ejemplo.alquileres.ui.*;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class JlccPropietarioController implements JlccRefrescable {
    private final JlccPropietarioService jlccService;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final ObservableList<Propietario> jlccDatos = FXCollections.observableArrayList();
    private Long jlccId;
    private boolean jlccActivo = true;

    @FXML private TextField buscar, documento, nombre, telefono, email, banco, cuenta;
    @FXML private ComboBox<TipoDocumento> tipo;
    @FXML private CheckBox activos;
    @FXML private Button btnEliminarDesactivar, btnReactivar;
    @FXML private TableView<Propietario> tabla;
    @FXML
    private TableColumn<Propietario, String> colNombre,
            colDocumento,
            colTelefono,
            colEmail,
            colEstado;
    @FXML private VBox drawer;
    @FXML private Region overlay;
    @FXML private Label titulo, errorTipo, errorDocumento, errorNombre;

    public JlccPropietarioController(JlccPropietarioService s, JlccTareas t, JlccNotificador a) {
        jlccService = s;
        jlccTareas = t;
        jlccAviso = a;
    }

    @FXML
    public void initialize() {
        tipo.setItems(FXCollections.observableArrayList(TipoDocumento.values()));
        tipo.setValue(TipoDocumento.DNI);
        activos.setSelected(true);
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colDocumento.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getTipoDocumento()
                                        + " "
                                        + c.getValue().getNumeroDocumento()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefono()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEstado.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        tabla.setItems(jlccDatos);
        tabla.getSelectionModel()
                .selectedItemProperty()
                .addListener((o, a, b) -> jlccActualizarAcciones(b));
        jlccActualizarAcciones(null);
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        activos.selectedProperty().addListener((o, a, b) -> refrescar());
        tabla.setOnMouseClicked(
                e -> {
                    if (e.getClickCount() == 2) jlccEditar(tabla.getSelectionModel().getSelectedItem());
                });
        tipo.valueProperty().addListener((o, a, b) -> jlccOcultarError(errorTipo));
        documento.textProperty().addListener((o, a, b) -> jlccOcultarError(errorDocumento));
        nombre.textProperty().addListener((o, a, b) -> jlccOcultarError(errorNombre));
    }

    @Override
    public void refrescar() {
        String jlccTexto = buscar.getText();
        Boolean jlccSoloActivos = activos.isSelected() ? true : null;
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccListar(jlccTexto, jlccSoloActivos, 0, 100),
                this::jlccMostrar,
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private void jlccMostrar(Page<Propietario> p) {
        jlccDatos.setAll(p.getContent());
    }

    @FXML
    private void jlccOnNuevo() {
        jlccId = null;
        jlccActivo = true;
        jlccLimpiarErrores();
        documento.clear();
        nombre.clear();
        telefono.clear();
        email.clear();
        banco.clear();
        cuenta.clear();
        tipo.setValue(TipoDocumento.DNI);
        titulo.setText("Nuevo propietario");
        jlccAbrir();
    }

    @FXML
    private void jlccOnEditar() {
        Propietario p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            jlccAviso.jlccError("Selecciona un propietario.");
            return;
        }
        jlccEditar(p);
    }

    private void jlccEditar(Propietario p) {
        if (p == null) return;
        jlccLimpiarErrores();
        jlccId = p.getId();
        jlccActivo = p.isActivo();
        tipo.setValue(p.getTipoDocumento());
        documento.setText(p.getNumeroDocumento());
        nombre.setText(p.getNombre());
        telefono.setText(jlccN(p.getTelefono()));
        email.setText(jlccN(p.getEmail()));
        banco.setText(jlccN(p.getBanco()));
        cuenta.setText(jlccN(p.getCuenta()));
        titulo.setText("Editar propietario");
        jlccAbrir();
    }

    @FXML
    private void jlccOnGuardar() {
        jlccLimpiarErrores();
        if (!jlccValidarFormulario()) return;
        Propietario p =
                Propietario.builder()
                        .id(jlccId)
                        .tipoDocumento(tipo.getValue())
                        .numeroDocumento(documento.getText())
                        .nombre(nombre.getText())
                        .telefono(telefono.getText())
                        .email(email.getText())
                        .banco(banco.getText())
                        .cuenta(cuenta.getText())
                        .activo(jlccActivo)
                        .build();
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccGuardar(p),
                x -> {
                    jlccCerrar();
                    jlccAviso.jlccInfo("Propietario guardado.");
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private boolean jlccValidarFormulario() {
        boolean jlccValido = true;
        if (tipo.getValue() == null) {
            jlccMostrarError(errorTipo, "Selecciona el tipo de documento");
            jlccValido = false;
        }
        if (documento.getText() == null || documento.getText().isBlank()) {
            jlccMostrarError(errorDocumento, "Ingresa el número de documento");
            jlccValido = false;
        }
        if (nombre.getText() == null || nombre.getText().isBlank()) {
            jlccMostrarError(errorNombre, "Ingresa el nombre o razón social");
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
        jlccOcultarError(errorTipo);
        jlccOcultarError(errorDocumento);
        jlccOcultarError(errorNombre);
    }

    @FXML
    private void jlccOnEliminar() {
        Propietario p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            jlccAviso.jlccError("Selecciona un propietario.");
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccTieneReferencias(p.getId()),
                referencias -> jlccConfirmarAccion(p, referencias),
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private void jlccConfirmarAccion(Propietario p, boolean jlccReferencias) {
        String jlccTitulo = jlccReferencias ? "Desactivar" : "Eliminar definitivamente";
        String jlccMensaje =
                jlccReferencias
                        ? "«"
                                + p.getNombre()
                                + "» está en uso, por lo que se desactivará "
                                + "y podrás reactivarlo después."
                        : "Se eliminará «"
                                + p.getNombre()
                                + "» de forma permanente. "
                                + "Esta acción no se puede deshacer.";
        if (!JlccDialogoConfirmacion.jlccMostrar(
                jlccVentana(), jlccTitulo, jlccMensaje, jlccReferencias ? "Desactivar" : "Eliminar")) {
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> {
                    jlccService.jlccEliminar(p.getId());
                    return null;
                },
                x -> {
                    jlccAviso.jlccInfo(jlccReferencias ? "Propietario desactivado." : "Propietario eliminado.");
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private Window jlccVentana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    @FXML
    private void jlccOnReactivar() {
        Propietario p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            jlccAviso.jlccError("Selecciona un propietario.");
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> {
                    jlccService.jlccReactivar(p.getId());
                    return null;
                },
                x -> {
                    jlccAviso.jlccInfo("Propietario reactivado.");
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private void jlccActualizarAcciones(Propietario p) {
        jlccMostrar(btnEliminarDesactivar, p != null && p.isActivo());
        jlccMostrar(btnReactivar, p != null && !p.isActivo());
    }

    private void jlccMostrar(Button jlccBoton, boolean jlccVisible) {
        if (jlccBoton == null) return;
        jlccBoton.setVisible(jlccVisible);
        jlccBoton.setManaged(jlccVisible);
    }

    @FXML
    private void jlccOnCerrar() {
        jlccCerrar();
    }

    private void jlccAbrir() {
        drawer.setVisible(true);
        drawer.setManaged(true);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    private void jlccCerrar() {
        drawer.setVisible(false);
        drawer.setManaged(false);
        overlay.setVisible(false);
        overlay.setManaged(false);
        jlccLimpiarErrores();
    }

    private String jlccN(String v) {
        return v == null ? "" : v;
    }

    private String jlccMsg(Throwable e) {
        return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

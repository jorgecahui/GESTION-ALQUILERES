package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.TipoInmueble;
import com.ejemplo.alquileres.service.JlccTipoInmuebleService;
import com.ejemplo.alquileres.ui.JlccDialogoConfirmacion;
import com.ejemplo.alquileres.ui.JlccNotificador;
import com.ejemplo.alquileres.ui.JlccRefrescable;
import com.ejemplo.alquileres.ui.JlccTareas;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class JlccTipoInmuebleController implements JlccRefrescable {
    private final JlccTipoInmuebleService jlccService;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final ObservableList<TipoInmueble> jlccDatos = FXCollections.observableArrayList();
    private Long jlccId;
    private boolean jlccActivo = true;

    @FXML private TextField buscar, codigo, nombre;
    @FXML private TextArea descripcion;
    @FXML private Button btnEliminarDesactivar, btnReactivar;
    @FXML private TableView<TipoInmueble> tabla;
    @FXML private TableColumn<TipoInmueble, String> colCodigo, colNombre, colEstado;
    @FXML private VBox drawer;
    @FXML private Region overlay;
    @FXML private Label titulo, errorCodigo, errorNombre;

    public JlccTipoInmuebleController(JlccTipoInmuebleService s, JlccTareas t, JlccNotificador a) {
        jlccService = s;
        jlccTareas = t;
        jlccAviso = a;
    }

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colEstado.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        tabla.setItems(jlccDatos);
        tabla.getSelectionModel()
                .selectedItemProperty()
                .addListener((o, a, b) -> jlccActualizarAcciones(b));
        jlccActualizarAcciones(null);
        tabla.setOnMouseClicked(
                e -> {
                    if (e.getClickCount() == 2) jlccEditar(tabla.getSelectionModel().getSelectedItem());
                });
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        codigo.textProperty().addListener((o, a, b) -> jlccOcultarError(errorCodigo));
        nombre.textProperty().addListener((o, a, b) -> jlccOcultarError(errorNombre));
    }

    @Override
    public void refrescar() {
        String jlccTexto = buscar.getText();
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccListar(jlccTexto, 0, 100), this::jlccMostrar, e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccMostrar(Page<TipoInmueble> p) {
        jlccDatos.setAll(p.getContent());
    }

    @FXML
    private void jlccOnNuevo() {
        jlccId = null;
        jlccActivo = true;
        jlccLimpiarErrores();
        codigo.clear();
        nombre.clear();
        descripcion.clear();
        titulo.setText("Nuevo tipo de inmueble");
        jlccAbrir();
    }

    @FXML
    private void jlccOnEditar() {
        TipoInmueble t = tabla.getSelectionModel().getSelectedItem();
        if (t == null) {
            jlccAviso.jlccError("Selecciona un tipo de inmueble.");
            return;
        }
        jlccEditar(t);
    }

    private void jlccEditar(TipoInmueble t) {
        if (t == null) return;
        jlccLimpiarErrores();
        jlccId = t.getId();
        jlccActivo = t.isActivo();
        codigo.setText(t.getCodigo());
        nombre.setText(t.getNombre());
        descripcion.setText(t.getDescripcion() == null ? "" : t.getDescripcion());
        titulo.setText("Editar tipo de inmueble");
        jlccAbrir();
    }

    @FXML
    private void jlccOnGuardar() {
        jlccLimpiarErrores();
        if (!jlccValidarFormulario()) return;
        TipoInmueble t =
                TipoInmueble.builder()
                        .id(jlccId)
                        .codigo(codigo.getText())
                        .nombre(nombre.getText())
                        .descripcion(descripcion.getText())
                        .activo(jlccActivo)
                        .build();
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccGuardar(t),
                x -> {
                    jlccCerrar();
                    jlccAviso.jlccInfo("Tipo de inmueble guardado.");
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private boolean jlccValidarFormulario() {
        boolean jlccValido = true;
        if (codigo.getText() == null || codigo.getText().isBlank()) {
            jlccMostrarError(errorCodigo, "El código es obligatorio");
            jlccValido = false;
        }
        if (nombre.getText() == null || nombre.getText().isBlank()) {
            jlccMostrarError(errorNombre, "Ingresa un nombre");
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
    private void jlccOnEliminar() {
        TipoInmueble t = tabla.getSelectionModel().getSelectedItem();
        if (t == null) {
            jlccAviso.jlccError("Selecciona un tipo de inmueble.");
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccTieneReferencias(t.getId()),
                referencias -> jlccConfirmarAccion(t, referencias),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccConfirmarAccion(TipoInmueble t, boolean jlccReferencias) {
        String jlccTitulo = jlccReferencias ? "Desactivar" : "Eliminar definitivamente";
        String jlccMensaje =
                jlccReferencias
                        ? "«"
                                + t.getNombre()
                                + "» está en uso, por lo que se desactivará "
                                + "y podrás reactivarlo después."
                        : "Se eliminará «"
                                + t.getNombre()
                                + "» de forma permanente. "
                                + "Esta acción no se puede deshacer.";
        if (!JlccDialogoConfirmacion.jlccMostrar(
                jlccVentana(), jlccTitulo, jlccMensaje, jlccReferencias ? "Desactivar" : "Eliminar")) {
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> {
                    jlccService.jlccEliminar(t.getId());
                    return null;
                },
                x -> {
                    jlccAviso.jlccInfo(
                            jlccReferencias
                                    ? "Tipo de inmueble desactivado."
                                    : "Tipo de inmueble eliminado.");
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private Window jlccVentana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    @FXML
    private void jlccOnReactivar() {
        TipoInmueble t = tabla.getSelectionModel().getSelectedItem();
        if (t == null) {
            jlccAviso.jlccError("Selecciona un tipo de inmueble.");
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> {
                    jlccService.jlccReactivar(t.getId());
                    return null;
                },
                x -> {
                    jlccAviso.jlccInfo("Tipo de inmueble reactivado.");
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccActualizarAcciones(TipoInmueble t) {
        jlccMostrar(btnEliminarDesactivar, t != null && t.isActivo());
        jlccMostrar(btnReactivar, t != null && !t.isActivo());
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

    private String jlccMensaje(Throwable e) {
        return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

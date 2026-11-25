package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.TipoInmueble;
import com.ejemplo.alquileres.service.TipoInmuebleService;
import com.ejemplo.alquileres.ui.DialogoConfirmacion;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Refrescable;
import com.ejemplo.alquileres.ui.Tareas;

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
public class TipoInmuebleController implements Refrescable {
    private final TipoInmuebleService service;
    private final Tareas tareas;
    private final Notificador aviso;
    private final ObservableList<TipoInmueble> datos = FXCollections.observableArrayList();
    private Long id;
    private boolean activo = true;

    @FXML private TextField buscar, codigo, nombre;
    @FXML private TextArea descripcion;
    @FXML private Button btnEliminarDesactivar, btnReactivar;
    @FXML private TableView<TipoInmueble> tabla;
    @FXML private TableColumn<TipoInmueble, String> colCodigo, colNombre, colEstado;
    @FXML private VBox drawer;
    @FXML private Region overlay;
    @FXML private Label titulo, errorCodigo, errorNombre;

    public TipoInmuebleController(TipoInmuebleService s, Tareas t, Notificador a) {
        service = s;
        tareas = t;
        aviso = a;
    }

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colEstado.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        tabla.setItems(datos);
        tabla.getSelectionModel()
                .selectedItemProperty()
                .addListener((o, a, b) -> actualizarAcciones(b));
        actualizarAcciones(null);
        tabla.setOnMouseClicked(
                e -> {
                    if (e.getClickCount() == 2) editar(tabla.getSelectionModel().getSelectedItem());
                });
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        codigo.textProperty().addListener((o, a, b) -> ocultarError(errorCodigo));
        nombre.textProperty().addListener((o, a, b) -> ocultarError(errorNombre));
    }

    @Override
    public void refrescar() {
        String texto = buscar.getText();
        tareas.ejecutar(
                () -> service.listar(texto, 0, 100), this::mostrar, e -> aviso.error(mensaje(e)));
    }

    private void mostrar(Page<TipoInmueble> p) {
        datos.setAll(p.getContent());
    }

    @FXML
    private void onNuevo() {
        id = null;
        activo = true;
        limpiarErrores();
        codigo.clear();
        nombre.clear();
        descripcion.clear();
        titulo.setText("Nuevo tipo de inmueble");
        abrir();
    }

    @FXML
    private void onEditar() {
        TipoInmueble t = tabla.getSelectionModel().getSelectedItem();
        if (t == null) {
            aviso.error("Selecciona un tipo de inmueble.");
            return;
        }
        editar(t);
    }

    private void editar(TipoInmueble t) {
        if (t == null) return;
        limpiarErrores();
        id = t.getId();
        activo = t.isActivo();
        codigo.setText(t.getCodigo());
        nombre.setText(t.getNombre());
        descripcion.setText(t.getDescripcion() == null ? "" : t.getDescripcion());
        titulo.setText("Editar tipo de inmueble");
        abrir();
    }

    @FXML
    private void onGuardar() {
        limpiarErrores();
        if (!validarFormulario()) return;
        TipoInmueble t =
                TipoInmueble.builder()
                        .id(id)
                        .codigo(codigo.getText())
                        .nombre(nombre.getText())
                        .descripcion(descripcion.getText())
                        .activo(activo)
                        .build();
        tareas.ejecutar(
                () -> service.guardar(t),
                x -> {
                    cerrar();
                    aviso.info("Tipo de inmueble guardado.");
                    refrescar();
                },
                e -> aviso.error(mensaje(e)));
    }

    private boolean validarFormulario() {
        boolean valido = true;
        if (codigo.getText() == null || codigo.getText().isBlank()) {
            mostrarError(errorCodigo, "El código es obligatorio");
            valido = false;
        }
        if (nombre.getText() == null || nombre.getText().isBlank()) {
            mostrarError(errorNombre, "Ingresa un nombre");
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

    @FXML
    private void onEliminar() {
        TipoInmueble t = tabla.getSelectionModel().getSelectedItem();
        if (t == null) {
            aviso.error("Selecciona un tipo de inmueble.");
            return;
        }
        tareas.ejecutar(
                () -> service.tieneReferencias(t.getId()),
                referencias -> confirmarAccion(t, referencias),
                e -> aviso.error(mensaje(e)));
    }

    private void confirmarAccion(TipoInmueble t, boolean referencias) {
        String titulo = referencias ? "Desactivar" : "Eliminar definitivamente";
        String mensaje =
                referencias
                        ? "«"
                                + t.getNombre()
                                + "» está en uso, por lo que se desactivará "
                                + "y podrás reactivarlo después."
                        : "Se eliminará «"
                                + t.getNombre()
                                + "» de forma permanente. "
                                + "Esta acción no se puede deshacer.";
        if (!DialogoConfirmacion.mostrar(
                ventana(), titulo, mensaje, referencias ? "Desactivar" : "Eliminar")) {
            return;
        }
        tareas.ejecutar(
                () -> {
                    service.eliminar(t.getId());
                    return null;
                },
                x -> {
                    aviso.info(
                            referencias
                                    ? "Tipo de inmueble desactivado."
                                    : "Tipo de inmueble eliminado.");
                    refrescar();
                },
                e -> aviso.error(mensaje(e)));
    }

    private Window ventana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    @FXML
    private void onReactivar() {
        TipoInmueble t = tabla.getSelectionModel().getSelectedItem();
        if (t == null) {
            aviso.error("Selecciona un tipo de inmueble.");
            return;
        }
        tareas.ejecutar(
                () -> {
                    service.reactivar(t.getId());
                    return null;
                },
                x -> {
                    aviso.info("Tipo de inmueble reactivado.");
                    refrescar();
                },
                e -> aviso.error(mensaje(e)));
    }

    private void actualizarAcciones(TipoInmueble t) {
        mostrar(btnEliminarDesactivar, t != null && t.isActivo());
        mostrar(btnReactivar, t != null && !t.isActivo());
    }

    private void mostrar(Button boton, boolean visible) {
        if (boton == null) return;
        boton.setVisible(visible);
        boton.setManaged(visible);
    }

    @FXML
    private void onCerrar() {
        cerrar();
    }

    private void abrir() {
        drawer.setVisible(true);
        drawer.setManaged(true);
        overlay.setVisible(true);
        overlay.setManaged(true);
    }

    private void cerrar() {
        drawer.setVisible(false);
        drawer.setManaged(false);
        overlay.setVisible(false);
        overlay.setManaged(false);
        limpiarErrores();
    }

    private String mensaje(Throwable e) {
        return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

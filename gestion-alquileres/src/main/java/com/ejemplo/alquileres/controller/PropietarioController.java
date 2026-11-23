package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.PropietarioService;
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
public class PropietarioController implements Refrescable {
    private final PropietarioService service;
    private final Tareas tareas;
    private final Notificador aviso;
    private final ObservableList<Propietario> datos = FXCollections.observableArrayList();
    private Long id;
    private boolean activo = true;

    @FXML private TextField buscar, documento, nombre, telefono, email, banco, cuenta;
    @FXML private ComboBox<TipoDocumento> tipo;
    @FXML private CheckBox activos;
    @FXML private Button btnEliminarDesactivar, btnReactivar;
    @FXML private TableView<Propietario> tabla;
    @FXML private TableColumn<Propietario, String> colNombre, colDocumento, colTelefono, colEmail, colEstado;
    @FXML private VBox drawer;
    @FXML private Region overlay;
    @FXML private Label titulo, errorTipo, errorDocumento, errorNombre;

    public PropietarioController(PropietarioService s, Tareas t, Notificador a) {
        service = s;
        tareas = t;
        aviso = a;
    }

    @FXML public void initialize() {
        tipo.setItems(FXCollections.observableArrayList(TipoDocumento.values()));
        tipo.setValue(TipoDocumento.DNI);
        activos.setSelected(true);
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colDocumento.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTipoDocumento() + " " + c.getValue().getNumeroDocumento()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefono()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        tabla.setItems(datos);
        tabla.getSelectionModel().selectedItemProperty().addListener(
                (o, a, b) -> actualizarAcciones(b));
        actualizarAcciones(null);
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        activos.selectedProperty().addListener((o, a, b) -> refrescar());
        tabla.setOnMouseClicked(e -> { if (e.getClickCount() == 2) editar(tabla.getSelectionModel().getSelectedItem()); });
        tipo.valueProperty().addListener((o, a, b) -> ocultarError(errorTipo));
        documento.textProperty().addListener((o, a, b) -> ocultarError(errorDocumento));
        nombre.textProperty().addListener((o, a, b) -> ocultarError(errorNombre));
    }

    @Override public void refrescar() {
        String texto = buscar.getText();
        Boolean soloActivos = activos.isSelected() ? true : null;
        tareas.ejecutar(() -> service.listar(texto, soloActivos, 0, 100),
                this::mostrar, e -> aviso.error(msg(e)));
    }

    private void mostrar(Page<Propietario> p) { datos.setAll(p.getContent()); }

    @FXML private void onNuevo() {
        id = null;
        activo = true;
        limpiarErrores();
        documento.clear();
        nombre.clear();
        telefono.clear();
        email.clear();
        banco.clear();
        cuenta.clear();
        tipo.setValue(TipoDocumento.DNI);
        titulo.setText("Nuevo propietario");
        abrir();
    }

    @FXML private void onEditar() {
        Propietario p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            aviso.error("Selecciona un propietario.");
            return;
        }
        editar(p);
    }

    private void editar(Propietario p) {
        if (p == null) return;
        limpiarErrores();
        id = p.getId();
        activo = p.isActivo();
        tipo.setValue(p.getTipoDocumento());
        documento.setText(p.getNumeroDocumento());
        nombre.setText(p.getNombre());
        telefono.setText(n(p.getTelefono()));
        email.setText(n(p.getEmail()));
        banco.setText(n(p.getBanco()));
        cuenta.setText(n(p.getCuenta()));
        titulo.setText("Editar propietario");
        abrir();
    }

    @FXML private void onGuardar() {
        limpiarErrores();
        if (!validarFormulario()) return;
        Propietario p = Propietario.builder().id(id).tipoDocumento(tipo.getValue())
                .numeroDocumento(documento.getText()).nombre(nombre.getText())
                .telefono(telefono.getText()).email(email.getText()).banco(banco.getText())
                .cuenta(cuenta.getText()).activo(activo).build();
        tareas.ejecutar(() -> service.guardar(p), x -> {
            cerrar();
            aviso.info("Propietario guardado.");
            refrescar();
        }, e -> aviso.error(msg(e)));
    }

    private boolean validarFormulario() {
        boolean valido = true;
        if (tipo.getValue() == null) {
            mostrarError(errorTipo, "Selecciona el tipo de documento");
            valido = false;
        }
        if (documento.getText() == null || documento.getText().isBlank()) {
            mostrarError(errorDocumento, "Ingresa el número de documento");
            valido = false;
        }
        if (nombre.getText() == null || nombre.getText().isBlank()) {
            mostrarError(errorNombre, "Ingresa el nombre o razón social");
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
        ocultarError(errorTipo);
        ocultarError(errorDocumento);
        ocultarError(errorNombre);
    }

    @FXML private void onEliminar() {
        Propietario p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            aviso.error("Selecciona un propietario.");
            return;
        }
        tareas.ejecutar(() -> service.tieneReferencias(p.getId()),
                referencias -> confirmarAccion(p, referencias),
                e -> aviso.error(msg(e)));
    }

    private void confirmarAccion(Propietario p, boolean referencias) {
        String titulo = referencias ? "Desactivar" : "Eliminar definitivamente";
        String mensaje = referencias
                ? "«" + p.getNombre() + "» está en uso, por lo que se desactivará "
                        + "y podrás reactivarlo después."
                : "Se eliminará «" + p.getNombre() + "» de forma permanente. "
                        + "Esta acción no se puede deshacer.";
        if (!DialogoConfirmacion.mostrar(ventana(), titulo, mensaje,
                referencias ? "Desactivar" : "Eliminar")) {
            return;
        }
        tareas.ejecutar(() -> { service.eliminar(p.getId()); return null; },
                x -> {
                    aviso.info(referencias ? "Propietario desactivado." : "Propietario eliminado.");
                    refrescar();
                },
                e -> aviso.error(msg(e)));
    }

    private Window ventana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    @FXML private void onReactivar() {
        Propietario p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            aviso.error("Selecciona un propietario.");
            return;
        }
        tareas.ejecutar(() -> { service.reactivar(p.getId()); return null; },
                x -> { aviso.info("Propietario reactivado."); refrescar(); },
                e -> aviso.error(msg(e)));
    }

    private void actualizarAcciones(Propietario p) {
        mostrar(btnEliminarDesactivar, p != null && p.isActivo());
        mostrar(btnReactivar, p != null && !p.isActivo());
    }

    private void mostrar(Button boton, boolean visible) {
        if (boton == null) return;
        boton.setVisible(visible);
        boton.setManaged(visible);
    }

    @FXML private void onCerrar() { cerrar(); }

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

    private String n(String v) { return v == null ? "" : v; }

    private String msg(Throwable e) {
        return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

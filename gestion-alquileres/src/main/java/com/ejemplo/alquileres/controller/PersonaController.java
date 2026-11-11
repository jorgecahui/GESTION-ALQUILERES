package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.PersonaService;
import com.ejemplo.alquileres.ui.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PersonaController implements Refrescable {
    private final PersonaService service;
    private final Tareas tareas;
    private final Notificador aviso;
    private final Exportador exportador;
    private final com.ejemplo.alquileres.config.DatosArrendador arrendador;
    private final ObservableList<Persona> datos = FXCollections.observableArrayList();
    private Long id;
    private boolean activo = true;

    @FXML private TextField buscar, documento, nombres, apellidos, email, telefono;
    @FXML private ComboBox<TipoDocumento> tipo;
    @FXML private CheckBox activos;
    @FXML private TableView<Persona> tabla;
    @FXML private TableColumn<Persona, String> colNombre, colDocumento, colTelefono, colEmail, colEstado;
    @FXML private VBox drawer;
    @FXML private Region overlay;
    @FXML private Label titulo, errorTipo, errorDocumento, errorNombres;

    public PersonaController(PersonaService s, Tareas t, Notificador a, Exportador x,
                             com.ejemplo.alquileres.config.DatosArrendador d) {
        service = s;
        tareas = t;
        aviso = a;
        exportador = x;
        arrendador = d;
    }

    @FXML public void initialize() {
        tipo.setItems(FXCollections.observableArrayList(TipoDocumento.values()));
        tipo.setValue(TipoDocumento.DNI);
        activos.setSelected(true);
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreCompleto()));
        colDocumento.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTipoDocumento() + " " + c.getValue().getNumeroDocumento()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefono()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        tabla.setItems(datos);
        buscar.textProperty().addListener((o, a, b) -> refrescar());
        activos.selectedProperty().addListener((o, a, b) -> refrescar());
        tabla.setOnMouseClicked(e -> { if (e.getClickCount() == 2) editar(tabla.getSelectionModel().getSelectedItem()); });
        tipo.valueProperty().addListener((o, a, b) -> ocultarError(errorTipo));
        documento.textProperty().addListener((o, a, b) -> ocultarError(errorDocumento));
        nombres.textProperty().addListener((o, a, b) -> ocultarError(errorNombres));
    }

    @Override public void refrescar() {
        String texto = buscar.getText();
        Boolean soloActivos = activos.isSelected() ? true : null;
        tareas.ejecutar(() -> service.listar(texto, null, soloActivos, 0, 100),
                this::mostrar, e -> aviso.error(msg(e)));
    }

    private void mostrar(Page<Persona> p) { datos.setAll(p.getContent()); }

    @FXML private void onNuevo() {
        id = null;
        activo = true;
        limpiarErrores();
        documento.clear();
        nombres.clear();
        apellidos.clear();
        email.clear();
        telefono.clear();
        tipo.setValue(TipoDocumento.DNI);
        titulo.setText("Nueva persona");
        abrir();
    }

    @FXML private void onEditar() {
        Persona p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            aviso.error("Selecciona una persona.");
            return;
        }
        editar(p);
    }

    private void editar(Persona p) {
        if (p == null) return;
        limpiarErrores();
        id = p.getId();
        activo = p.isActivo();
        tipo.setValue(p.getTipoDocumento());
        documento.setText(p.getNumeroDocumento());
        nombres.setText(p.getNombres());
        apellidos.setText(n(p.getApellidos()));
        email.setText(n(p.getEmail()));
        telefono.setText(n(p.getTelefono()));
        titulo.setText("Editar persona");
        abrir();
    }

    @FXML private void onGuardar() {
        limpiarErrores();
        if (!validarFormulario()) return;
        Persona p = Persona.builder().id(id).tipoDocumento(tipo.getValue())
                .numeroDocumento(documento.getText()).nombres(nombres.getText())
                .apellidos(apellidos.getText()).email(email.getText()).telefono(telefono.getText())
                .activo(activo).build();
        tareas.ejecutar(() -> service.guardar(p), x -> {
            cerrar();
            aviso.info("Persona guardada.");
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
        if (nombres.getText() == null || nombres.getText().isBlank()) {
            mostrarError(errorNombres, "Ingresa el nombre o razón social");
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
        ocultarError(errorNombres);
    }

    @FXML private void onDesactivar() {
        Persona p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            aviso.error("Selecciona una persona.");
            return;
        }
        tareas.ejecutar(() -> { service.desactivar(p.getId()); return null; },
                x -> { aviso.info("Persona desactivada."); refrescar(); },
                e -> aviso.error(msg(e)));
    }

    @FXML private void onWhatsApp() {
        Persona p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            aviso.error("Selecciona una persona.");
            return;
        }
        String mensaje = "Hola " + p.getNombreCompleto() + ", le escribimos de " + arrendador.razonSocial()
                + " para coordinar un tema relacionado con su alquiler. Quedamos atentos a su respuesta.";
        DialogoRecordatorio.contactoLibre(tabla.getScene() == null ? null : tabla.getScene().getWindow(),
                p.getNombreCompleto(), p.getTelefono(), mensaje, exportador, aviso);
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

package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.*;
import com.ejemplo.alquileres.service.JlccPersonaService;
import com.ejemplo.alquileres.service.JlccTelefonoUtil;
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
public class JlccPersonaController implements JlccRefrescable {
    private final JlccPersonaService jlccService;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccExportador jlccExportador;
    private final com.ejemplo.alquileres.config.DatosArrendador jlccArrendador;
    private final ObservableList<Persona> jlccDatos = FXCollections.observableArrayList();
    private Long jlccId;
    private boolean jlccActivo = true;

    @FXML
    private TextField buscar,
            documento,
            nombres,
            apellidos,
            email,
            telefono,
            telefonoSecundario,
            direccion,
            ocupacion;
    @FXML private TextArea observaciones;
    @FXML private ComboBox<TipoDocumento> tipo;
    @FXML private CheckBox activos;
    @FXML private Button btnDesactivar, btnReactivar;
    @FXML private TableView<Persona> tabla;
    @FXML
    private TableColumn<Persona, String> colNombre, colDocumento, colTelefono, colEmail, colEstado;
    @FXML private VBox drawer;
    @FXML private Region overlay;
    @FXML
    private Label titulo,
            errorTipo,
            errorDocumento,
            errorNombres,
            errorTelefono,
            errorTelefonoSecundario;

    public JlccPersonaController(
            JlccPersonaService s,
            JlccTareas t,
            JlccNotificador a,
            JlccExportador x,
            com.ejemplo.alquileres.config.DatosArrendador d) {
        jlccService = s;
        jlccTareas = t;
        jlccAviso = a;
        jlccExportador = x;
        jlccArrendador = d;
    }

    @FXML
    public void initialize() {
        tipo.setItems(FXCollections.observableArrayList(TipoDocumento.values()));
        tipo.setValue(TipoDocumento.DNI);
        activos.setSelected(true);
        jlccLimitar(documento, 15);
        jlccLimitar(nombres, 120);
        jlccLimitar(apellidos, 120);
        jlccLimitar(email, 120);
        jlccLimitar(telefono, 30);
        jlccLimitar(telefonoSecundario, 30);
        jlccLimitar(direccion, 250);
        jlccLimitar(ocupacion, 120);
        jlccLimitar(observaciones, 1000);
        colNombre.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getNombreCompleto()));
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
        nombres.textProperty().addListener((o, a, b) -> jlccOcultarError(errorNombres));
        telefono.textProperty().addListener((o, a, b) -> jlccOcultarError(errorTelefono));
        telefonoSecundario
                .textProperty()
                .addListener((o, a, b) -> jlccOcultarError(errorTelefonoSecundario));
    }

    @Override
    public void refrescar() {
        String jlccTexto = buscar.getText();
        Boolean jlccSoloActivos = activos.isSelected() ? true : null;
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccListar(jlccTexto, null, jlccSoloActivos, 0, 100),
                this::jlccMostrar,
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private void jlccMostrar(Page<Persona> p) {
        jlccDatos.setAll(p.getContent());
    }

    @FXML
    private void jlccOnNuevo() {
        jlccId = null;
        jlccActivo = true;
        jlccLimpiarErrores();
        documento.clear();
        nombres.clear();
        apellidos.clear();
        email.clear();
        telefono.clear();
        telefonoSecundario.clear();
        direccion.clear();
        ocupacion.clear();
        observaciones.clear();
        tipo.setValue(TipoDocumento.DNI);
        titulo.setText("Nueva persona");
        jlccAbrir();
    }

    @FXML
    private void jlccOnEditar() {
        Persona p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            jlccAviso.jlccError("Selecciona una persona.");
            return;
        }
        jlccEditar(p);
    }

    private void jlccEditar(Persona p) {
        if (p == null) return;
        jlccLimpiarErrores();
        jlccId = p.getId();
        jlccActivo = p.isActivo();
        tipo.setValue(p.getTipoDocumento());
        documento.setText(p.getNumeroDocumento());
        nombres.setText(p.getNombres());
        apellidos.setText(jlccN(p.getApellidos()));
        email.setText(jlccN(p.getEmail()));
        telefono.setText(jlccN(p.getTelefono()));
        telefonoSecundario.setText(jlccN(p.getTelefonoSecundario()));
        direccion.setText(jlccN(p.getDireccion()));
        ocupacion.setText(jlccN(p.getOcupacion()));
        observaciones.setText(jlccN(p.getObservaciones()));
        titulo.setText("Editar persona");
        jlccAbrir();
    }

    @FXML
    private void jlccOnGuardar() {
        jlccLimpiarErrores();
        if (!jlccValidarFormulario()) return;
        Persona p =
                Persona.builder()
                        .id(jlccId)
                        .tipoDocumento(tipo.getValue())
                        .numeroDocumento(documento.getText())
                        .nombres(nombres.getText())
                        .apellidos(apellidos.getText())
                        .email(email.getText())
                        .telefono(telefono.getText())
                        .telefonoSecundario(telefonoSecundario.getText())
                        .direccion(direccion.getText())
                        .ocupacion(ocupacion.getText())
                        .observaciones(observaciones.getText())
                        .activo(jlccActivo)
                        .build();
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccGuardar(p),
                x -> {
                    jlccCerrar();
                    jlccAviso.jlccInfo("Persona guardada.");
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
        if (nombres.getText() == null || nombres.getText().isBlank()) {
            jlccMostrarError(errorNombres, "Ingresa el nombre o razón social");
            jlccValido = false;
        }
        if (!jlccTelefonoValido(telefono.getText())) {
            jlccMostrarError(errorTelefono, "El teléfono no es válido");
            jlccValido = false;
        }
        if (!jlccTelefonoValido(telefonoSecundario.getText())) {
            jlccMostrarError(errorTelefonoSecundario, "El teléfono secundario no es válido");
            jlccValido = false;
        }
        return jlccValido;
    }

    private boolean jlccTelefonoValido(String jlccValor) {
        if (jlccValor == null || jlccValor.isBlank()) return true;
        if (JlccTelefonoUtil.jlccNormalizarPeru(jlccValor) != null) return true;
        String jlccLimpio = jlccValor.replaceAll("[\\s().-]", "");
        return jlccLimpio.matches("(?:0?1)?\\d{6,8}");
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
        jlccOcultarError(errorNombres);
        jlccOcultarError(errorTelefono);
        jlccOcultarError(errorTelefonoSecundario);
    }

    @FXML
    private void jlccOnDesactivar() {
        Persona p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            jlccAviso.jlccError("Selecciona una persona.");
            return;
        }
        if (!JlccDialogoConfirmacion.jlccMostrar(
                jlccVentana(),
                "Desactivar persona",
                "Se desactivará «" + p.getNombreCompleto() + "». Podrás reactivarla después.",
                "Desactivar")) {
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> {
                    jlccService.jlccDesactivar(p.getId());
                    return null;
                },
                x -> {
                    jlccAviso.jlccInfo("Persona desactivada.");
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    @FXML
    private void jlccOnReactivar() {
        Persona p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            jlccAviso.jlccError("Selecciona una persona.");
            return;
        }
        jlccTareas.jlccEjecutar(
                () -> {
                    jlccService.jlccReactivar(p.getId());
                    return null;
                },
                x -> {
                    jlccAviso.jlccInfo("Persona reactivada.");
                    refrescar();
                },
                e -> jlccAviso.jlccError(jlccMsg(e)));
    }

    private void jlccActualizarAcciones(Persona p) {
        jlccMostrar(btnDesactivar, p != null && p.isActivo());
        jlccMostrar(btnReactivar, p != null && !p.isActivo());
    }

    private void jlccMostrar(Button jlccBoton, boolean jlccVisible) {
        if (jlccBoton == null) return;
        jlccBoton.setVisible(jlccVisible);
        jlccBoton.setManaged(jlccVisible);
    }

    @FXML
    private void jlccOnWhatsApp() {
        Persona p = tabla.getSelectionModel().getSelectedItem();
        if (p == null) {
            jlccAviso.jlccError("Selecciona una persona.");
            return;
        }
        String jlccMensaje =
                "Hola "
                        + p.getNombreCompleto()
                        + ", le escribimos de "
                        + jlccArrendador.razonSocial()
                        + " para coordinar un tema relacionado con su alquiler. Quedamos atentos a"
                        + " su respuesta.";
        JlccDialogoRecordatorio.jlccContactoLibre(
                tabla.getScene() == null ? null : tabla.getScene().getWindow(),
                p.getNombreCompleto(),
                p.getTelefono(),
                jlccMensaje,
                jlccExportador,
                jlccAviso);
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

    private Window jlccVentana() {
        return tabla.getScene() == null ? null : tabla.getScene().getWindow();
    }

    private void jlccLimitar(TextInputControl jlccCampo, int jlccMaximo) {
        jlccCampo.setTextFormatter(
                new TextFormatter<>(c -> c.getControlNewText().length() <= jlccMaximo ? c : null));
    }

    private String jlccMsg(Throwable e) {
        return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.AuditoriaEvento;
import com.ejemplo.alquileres.model.Configuracion;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.TipoMora;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.service.JlccAuditoriaService;
import com.ejemplo.alquileres.service.JlccConfiguracionService;
import com.ejemplo.alquileres.service.JlccPermisos;
import com.ejemplo.alquileres.service.JlccPlantillasMensaje;
import com.ejemplo.alquileres.service.JlccReporteAuditoriaExcelService;
import com.ejemplo.alquileres.service.JlccRespaldoService;
import com.ejemplo.alquileres.service.JlccUsuarioService;
import com.ejemplo.alquileres.ui.JlccAplicacionUi;
import com.ejemplo.alquileres.ui.JlccExportador;
import com.ejemplo.alquileres.ui.JlccFormularios;
import com.ejemplo.alquileres.ui.JlccNotificador;
import com.ejemplo.alquileres.ui.JlccRefrescable;
import com.ejemplo.alquileres.ui.JlccTareas;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class JlccConfiguracionController implements JlccRefrescable {
    private final JlccConfiguracionService jlccConfiguracion;
    private final JlccUsuarioService jlccUsuariosServicio;
    private final JlccAuditoriaService jlccAuditoria;
    private final JlccReporteAuditoriaExcelService jlccReporteAuditoria;
    private final JlccRespaldoService jlccRespaldos;
    private final JlccAplicacionUi jlccAplicacion;
    private final JlccPermisos jlccPermisos;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccAviso;
    private final JlccExportador jlccExportador;

    @FXML
    private TextField razonSocial,
            tipoDocumento,
            documento,
            direccion,
            telefono,
            email,
            logoArchivo;
    @FXML private ImageView logoVista;
    @FXML private TextField reciboSerie;
    @FXML private TextArea reciboLeyenda;
    @FXML private ComboBox<TipoMora> moraTipo;
    @FXML private TextField moraValor, moraDiasGracia;
    @FXML private TextField diasVencimiento, diasRecordatorio;
    @FXML private TextField umbralLeve, cuotasRojo;
    @FXML private TextArea plantillaPreventivo, plantillaVencido, plantillaFinContrato;
    @FXML private TextField inactividad;
    @FXML private Label estado;
    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML
    private TableColumn<Usuario, String> colUsuario,
            colNombreUsuario,
            colRolUsuario,
            colEstadoUsuario,
            colUltimoAcceso;
    @FXML private TableView<AuditoriaEvento> tablaAuditoria;
    @FXML
    private TableColumn<AuditoriaEvento, String> colAudFecha,
            colAudUsuario,
            colAudAccion,
            colAudEntidad,
            colAudDetalle;
    @FXML private DatePicker audDesde, audHasta;
    @FXML private TextField audUsuario;
    @FXML private ComboBox<AccionAuditoria> audAccion;
    @FXML private TableView<JlccRespaldoService.Respaldo> tablaRespaldos;
    @FXML
    private TableColumn<JlccRespaldoService.Respaldo, String> colRespNombre,
            colRespFecha,
            colRespTamano;
    @FXML private Label rutaDatos;

    private final ObservableList<Usuario> jlccListaUsuarios = FXCollections.observableArrayList();
    private final ObservableList<AuditoriaEvento> jlccListaAuditoria =
            FXCollections.observableArrayList();
    private final ObservableList<JlccRespaldoService.Respaldo> jlccListaRespaldos =
            FXCollections.observableArrayList();
    private static final DateTimeFormatter JLCC_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long jlccVersion;

    public JlccConfiguracionController(
            JlccConfiguracionService jlccConfiguracion,
            JlccUsuarioService jlccUsuariosServicio,
            JlccAuditoriaService jlccAuditoria,
            JlccReporteAuditoriaExcelService jlccReporteAuditoria,
            JlccRespaldoService jlccRespaldos,
            JlccAplicacionUi jlccAplicacion,
            JlccPermisos jlccPermisos,
            JlccTareas jlccTareas,
            JlccNotificador jlccAviso,
            JlccExportador jlccExportador) {
        this.jlccConfiguracion = jlccConfiguracion;
        this.jlccUsuariosServicio = jlccUsuariosServicio;
        this.jlccAuditoria = jlccAuditoria;
        this.jlccReporteAuditoria = jlccReporteAuditoria;
        this.jlccRespaldos = jlccRespaldos;
        this.jlccAplicacion = jlccAplicacion;
        this.jlccPermisos = jlccPermisos;
        this.jlccTareas = jlccTareas;
        this.jlccAviso = jlccAviso;
        this.jlccExportador = jlccExportador;
    }

    @FXML
    public void initialize() {
        moraTipo.setItems(FXCollections.observableArrayList(TipoMora.values()));
        audAccion.setItems(FXCollections.observableArrayList(AccionAuditoria.values()));
        JlccFormularios.jlccSoloDigitos(moraDiasGracia, 3);
        JlccFormularios.jlccSoloDigitos(diasVencimiento, 4);
        JlccFormularios.jlccSoloDigitos(diasRecordatorio, 4);
        JlccFormularios.jlccSoloDigitos(umbralLeve, 4);
        JlccFormularios.jlccSoloDigitos(cuotasRojo, 3);
        JlccFormularios.jlccSoloDigitos(inactividad, 4);
        JlccFormularios.jlccSoloDecimal(moraValor, 4);
        jlccPrepararTablaUsuarios();
        jlccPrepararTablaAuditoria();
        jlccPrepararTablaRespaldos();
        rutaDatos.setText(jlccConfiguracion.jlccRutaDatos().toString());
        refrescar();
    }

    @Override
    public void refrescar() {
        jlccTareas.jlccEjecutar(jlccConfiguracion::jlccActual, this::jlccCargar, e -> jlccAviso.jlccError(jlccMensaje(e)));
        if (jlccPermisos.jlccEsAdmin()) {
            jlccCargarUsuarios();
            jlccCargarAuditoria();
            jlccCargarRespaldos();
        }
    }

    private void jlccPrepararTablaUsuarios() {
        colUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colNombreUsuario.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getNombre()));
        colRolUsuario.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getRolEtiqueta()));
        colEstadoUsuario.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colUltimoAcceso.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getUltimoAcceso() == null
                                        ? "—"
                                        : c.getValue().getUltimoAcceso().format(JLCC_FECHA_HORA)));
        tablaUsuarios.setItems(jlccListaUsuarios);
    }

    private void jlccPrepararTablaAuditoria() {
        colAudFecha.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getFecha() == null
                                        ? ""
                                        : c.getValue().getFecha().format(JLCC_FECHA_HORA)));
        colAudUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsuario()));
        colAudAccion.setCellValueFactory(
                c ->
                        new SimpleStringProperty(
                                c.getValue().getAccion() == null
                                        ? ""
                                        : c.getValue().getAccion().getEtiqueta()));
        colAudEntidad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEntidad()));
        colAudDetalle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetalle()));
        tablaAuditoria.setItems(jlccListaAuditoria);
    }

    private void jlccPrepararTablaRespaldos() {
        colRespNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        colRespFecha.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().fecha().format(JLCC_FECHA_HORA)));
        colRespTamano.setCellValueFactory(
                c -> new SimpleStringProperty(jlccTamano(c.getValue().bytes())));
        tablaRespaldos.setItems(jlccListaRespaldos);
    }

    private void jlccCargarRespaldos() {
        jlccTareas.jlccEjecutar(
                jlccRespaldos::jlccListarAutomaticos,
                lista -> jlccListaRespaldos.setAll(lista),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnCrearRespaldo() {
        DirectoryChooser jlccSelector = new DirectoryChooser();
        jlccSelector.setTitle("Carpeta para el respaldo");
        File jlccCarpeta = jlccSelector.showDialog(jlccVentana());
        if (jlccCarpeta == null) return;
        jlccTareas.jlccEjecutar(
                () -> jlccRespaldos.jlccCrearRespaldo(jlccCarpeta.toPath()),
                ruta -> {
                    jlccAviso.jlccInfo("Respaldo creado: " + ruta.getFileName());
                    jlccCargarRespaldos();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnRestaurarRespaldo() {
        FileChooser jlccSelector = new FileChooser();
        jlccSelector.setTitle("Elegir respaldo para restaurar");
        jlccSelector.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Base de datos SQLite", "*.db"));
        File jlccInicial = jlccRespaldos.jlccRutaRespaldos().toFile();
        if (jlccInicial.isDirectory()) jlccSelector.setInitialDirectory(jlccInicial);
        File jlccArchivo = jlccSelector.showOpenDialog(jlccVentana());
        if (jlccArchivo == null) return;
        javafx.scene.control.Alert jlccConfirmacion =
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION,
                        "Se sustituirá la base de datos actual por el respaldo elegido.\n"
                            + "Antes se guardará una copia de seguridad. La aplicación se cerrará"
                            + " al terminar.\n\n"
                            + "¿Deseas continuar?",
                        ButtonType.YES,
                        ButtonType.NO);
        jlccConfirmacion.setHeaderText("Restaurar respaldo");
        jlccConfirmacion.setTitle("Confirmar restauración");
        if (jlccConfirmacion.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        jlccTareas.jlccEjecutar(
                () -> jlccRespaldos.jlccRestaurar(jlccArchivo.toPath()),
                resultado -> {
                    javafx.scene.control.Alert jlccFin =
                            new javafx.scene.control.Alert(
                                    javafx.scene.control.Alert.AlertType.INFORMATION,
                                    "Respaldo restaurado correctamente.\n\n"
                                        + "La aplicación se cerrará. Vuelve a abrirla para trabajar"
                                        + " con los datos restaurados.",
                                    ButtonType.OK);
                    jlccFin.setHeaderText("Restauración completada");
                    jlccFin.setTitle("Respaldo restaurado");
                    jlccFin.showAndWait();
                    jlccAplicacion.jlccSalir();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private static String jlccTamano(long jlccBytes) {
        if (jlccBytes < 1024) return jlccBytes + " B";
        if (jlccBytes < 1024 * 1024) return String.format("%.1f KB", jlccBytes / 1024.0);
        return String.format("%.1f MB", jlccBytes / (1024.0 * 1024.0));
    }

    private void jlccCargarUsuarios() {
        jlccTareas.jlccEjecutar(
                jlccUsuariosServicio::jlccListar,
                lista -> jlccListaUsuarios.setAll(lista),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private void jlccCargarAuditoria() {
        LocalDateTime jlccDesde =
                audDesde.getValue() == null ? null : audDesde.getValue().atStartOfDay();
        LocalDateTime jlccHasta =
                audHasta.getValue() == null ? null : audHasta.getValue().atTime(23, 59, 59);
        String jlccUsuario = audUsuario.getText();
        AccionAuditoria jlccAccion = audAccion.getValue();
        jlccTareas.jlccEjecutar(
                () -> jlccAuditoria.jlccListar(jlccDesde, jlccHasta, jlccUsuario, jlccAccion),
                lista -> jlccListaAuditoria.setAll(lista),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnBuscarAuditoria() {
        jlccCargarAuditoria();
    }

    @FXML
    private void jlccOnLimpiarAuditoria() {
        audDesde.setValue(null);
        audHasta.setValue(null);
        audUsuario.clear();
        audAccion.setValue(null);
        jlccCargarAuditoria();
    }

    @FXML
    private void jlccOnExportarAuditoria() {
        if (jlccListaAuditoria.isEmpty()) {
            jlccAviso.jlccInfo("No hay eventos para exportar.");
            return;
        }
        File jlccDestino =
                jlccExportador.jlccElegirDestino(
                        jlccVentana(),
                        "auditoria-" + java.time.LocalDate.now() + ".xlsx",
                        "Excel",
                        "xlsx");
        if (jlccDestino == null) return;
        jlccTareas.jlccEjecutar(
                () -> jlccReporteAuditoria.jlccGenerar(List.copyOf(jlccListaAuditoria)),
                bytes -> {
                    jlccExportador.jlccEscribir(jlccDestino, bytes);
                    jlccAviso.jlccInfo("Auditoría exportada.");
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnNuevoUsuario() {
        TextField jlccUsuario = new TextField();
        TextField jlccNombre = new TextField();
        PasswordField jlccPassword = new PasswordField();
        PasswordField jlccConfirmacion = new PasswordField();
        ComboBox<RolUsuario> jlccRol =
                new ComboBox<>(FXCollections.observableArrayList(RolUsuario.values()));
        jlccRol.setValue(RolUsuario.OPERADOR);
        CheckBox jlccActivo = new CheckBox("Activo");
        jlccActivo.setSelected(true);
        GridPane jlccFormulario = jlccFormulario();
        jlccFila(jlccFormulario, 0, "Usuario", jlccUsuario);
        jlccFila(jlccFormulario, 1, "Nombre completo", jlccNombre);
        jlccFila(jlccFormulario, 2, "Contraseña", jlccPassword);
        jlccFila(jlccFormulario, 3, "Confirmar contraseña", jlccConfirmacion);
        jlccFila(jlccFormulario, 4, "Rol", jlccRol);
        jlccFormulario.add(jlccActivo, 1, 5);
        Dialog<ButtonType> jlccDialogo = jlccDialogo("Nuevo usuario");
        jlccDialogo.getDialogPane().setContent(jlccFormulario);
        if (jlccDialogo.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        String jlccUsuarioTexto = jlccUsuario.getText();
        String jlccNombreTexto = jlccNombre.getText();
        String jlccPasswordTexto = jlccPassword.getText();
        String jlccConfirmacionTexto = jlccConfirmacion.getText();
        RolUsuario jlccRolSeleccionado = jlccRol.getValue();
        boolean jlccActivoSeleccionado = jlccActivo.isSelected();
        jlccTareas.jlccEjecutar(
                () ->
                        jlccUsuariosServicio.jlccCrear(
                                jlccUsuarioTexto,
                                jlccNombreTexto,
                                jlccPasswordTexto,
                                jlccConfirmacionTexto,
                                jlccRolSeleccionado,
                                jlccActivoSeleccionado),
                creado -> {
                    jlccAviso.jlccInfo("Usuario creado.");
                    jlccCargarUsuarios();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnEditarUsuario() {
        Usuario jlccSeleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (jlccSeleccionado == null) {
            jlccAviso.jlccInfo("Selecciona un usuario.");
            return;
        }
        TextField jlccUsuario = new TextField(jlccSeleccionado.getUsername());
        jlccUsuario.setDisable(true);
        TextField jlccNombre = new TextField(jlccSeleccionado.getNombre());
        ComboBox<RolUsuario> jlccRol =
                new ComboBox<>(FXCollections.observableArrayList(RolUsuario.values()));
        jlccRol.setValue(jlccSeleccionado.getRol());
        CheckBox jlccActivo = new CheckBox("Activo");
        jlccActivo.setSelected(jlccSeleccionado.isActivo());
        GridPane jlccFormulario = jlccFormulario();
        jlccFila(jlccFormulario, 0, "Usuario", jlccUsuario);
        jlccFila(jlccFormulario, 1, "Nombre completo", jlccNombre);
        jlccFila(jlccFormulario, 2, "Rol", jlccRol);
        jlccFormulario.add(jlccActivo, 1, 3);
        Dialog<ButtonType> jlccDialogo = jlccDialogo("Editar usuario");
        jlccDialogo.getDialogPane().setContent(jlccFormulario);
        if (jlccDialogo.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        String jlccNombreTexto = jlccNombre.getText();
        RolUsuario jlccRolSeleccionado = jlccRol.getValue();
        boolean jlccActivoSeleccionado = jlccActivo.isSelected();
        jlccTareas.jlccEjecutar(
                () ->
                        jlccUsuariosServicio.jlccActualizar(
                                jlccSeleccionado.getId(),
                                jlccNombreTexto,
                                jlccRolSeleccionado,
                                jlccActivoSeleccionado),
                actualizado -> {
                    jlccAviso.jlccInfo("Usuario actualizado.");
                    jlccCargarUsuarios();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnDesactivarUsuario() {
        Usuario jlccSeleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (jlccSeleccionado == null) {
            jlccAviso.jlccInfo("Selecciona un usuario.");
            return;
        }
        jlccTareas.jlccEjecutar(
                () ->
                        jlccUsuariosServicio.jlccActualizar(
                                jlccSeleccionado.getId(),
                                jlccSeleccionado.getNombre(),
                                jlccSeleccionado.getRol(),
                                !jlccSeleccionado.isActivo()),
                actualizado -> {
                    jlccAviso.jlccInfo(
                            actualizado.isActivo() ? "Usuario activado." : "Usuario desactivado.");
                    jlccCargarUsuarios();
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnRestablecerPassword() {
        Usuario jlccSeleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (jlccSeleccionado == null) {
            jlccAviso.jlccInfo("Selecciona un usuario.");
            return;
        }
        PasswordField jlccPassword = new PasswordField();
        PasswordField jlccConfirmacion = new PasswordField();
        GridPane jlccFormulario = jlccFormulario();
        jlccFila(jlccFormulario, 0, "Nueva contraseña", jlccPassword);
        jlccFila(jlccFormulario, 1, "Confirmar contraseña", jlccConfirmacion);
        Dialog<ButtonType> jlccDialogo =
                jlccDialogo("Restablecer contraseña de " + jlccSeleccionado.getUsername());
        jlccDialogo.getDialogPane().setContent(jlccFormulario);
        if (jlccDialogo.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        String jlccPasswordTexto = jlccPassword.getText();
        String jlccConfirmacionTexto = jlccConfirmacion.getText();
        jlccTareas.jlccEjecutar(
                () -> {
                    jlccUsuariosServicio.jlccRestablecerPassword(
                            jlccSeleccionado.getId(), jlccPasswordTexto, jlccConfirmacionTexto);
                    return null;
                },
                ignorado -> jlccAviso.jlccInfo("Contraseña restablecida."),
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    private GridPane jlccFormulario() {
        GridPane jlccFormulario = new GridPane();
        jlccFormulario.setHgap(12);
        jlccFormulario.setVgap(10);
        jlccFormulario.setPrefWidth(360);
        return jlccFormulario;
    }

    private void jlccFila(
            GridPane jlccFormulario, int jlccFila, String jlccEtiqueta, javafx.scene.control.Control jlccControl) {
        Label jlccLabel = new Label(jlccEtiqueta);
        jlccLabel.getStyleClass().add("campo-label");
        jlccFormulario.add(jlccLabel, 0, jlccFila);
        jlccFormulario.add(jlccControl, 1, jlccFila);
        if (jlccControl instanceof TextField jlccTexto) jlccTexto.setPrefColumnCount(20);
    }

    private Dialog<ButtonType> jlccDialogo(String jlccTitulo) {
        Dialog<ButtonType> jlccDialogo = new Dialog<>();
        jlccDialogo.setTitle(jlccTitulo);
        jlccDialogo.setHeaderText(null);
        jlccDialogo.getDialogPane().getStyleClass().add("dialogo");
        jlccDialogo.getDialogPane()
                .getButtonTypes()
                .addAll(new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        if (jlccVentana() != null) jlccDialogo.initOwner(jlccVentana());
        return jlccDialogo;
    }

    private void jlccCargar(Configuracion c) {
        jlccVersion = c.getVersion();
        razonSocial.setText(jlccTexto(c.getArrendadorRazonSocial()));
        tipoDocumento.setText(jlccTexto(c.getArrendadorTipoDocumento()));
        documento.setText(jlccTexto(c.getArrendadorDocumento()));
        direccion.setText(jlccTexto(c.getArrendadorDireccion()));
        telefono.setText(jlccTexto(c.getArrendadorTelefono()));
        email.setText(jlccTexto(c.getArrendadorEmail()));
        logoArchivo.setText(jlccTexto(c.getLogoArchivo()));
        jlccActualizarVistaLogo(c.getLogoArchivo());
        reciboSerie.setText(jlccTexto(c.getReciboSerie()));
        reciboLeyenda.setText(jlccTexto(c.getReciboLeyenda()));
        moraTipo.setValue(c.getMoraTipo());
        moraValor.setText(c.getMoraValor() == null ? "0" : c.getMoraValor().toPlainString());
        moraDiasGracia.setText(String.valueOf(c.getMoraDiasGracia()));
        diasVencimiento.setText(String.valueOf(c.getAlertasDiasVencimiento()));
        diasRecordatorio.setText(String.valueOf(c.getAlertaRecordatorioDias()));
        umbralLeve.setText(String.valueOf(c.getSemaforoUmbralLeveDias()));
        cuotasRojo.setText(String.valueOf(c.getSemaforoCuotasRojo()));
        plantillaPreventivo.setText(jlccTexto(c.getPlantillaPreventivo()));
        plantillaVencido.setText(jlccTexto(c.getPlantillaVencido()));
        plantillaFinContrato.setText(jlccTexto(c.getPlantillaFinContrato()));
        inactividad.setText(String.valueOf(c.getInactividadMinutos()));
        estado.setText("Configuración cargada.");
    }

    @FXML
    private void jlccOnGuardar() {
        try {
            Configuracion jlccDatos = jlccLeerFormulario();
            jlccTareas.jlccEjecutar(
                    () -> jlccConfiguracion.jlccGuardar(jlccDatos),
                    guardada -> {
                        jlccVersion = guardada.getVersion();
                        estado.setText("Guardado correctamente.");
                        jlccAviso.jlccInfo("Configuración guardada.");
                    },
                    e -> {
                        estado.setText("No se pudo guardar.");
                        jlccAviso.jlccError(jlccMensaje(e));
                    });
        } catch (RuntimeException e) {
            estado.setText("No se pudo guardar.");
            jlccAviso.jlccError(e.getMessage() == null ? "Revisa los valores ingresados." : e.getMessage());
        }
    }

    private Configuracion jlccLeerFormulario() {
        Configuracion c =
                Configuracion.builder()
                        .id(Configuracion.FILA_UNICA)
                        .arrendadorRazonSocial(razonSocial.getText())
                        .arrendadorTipoDocumento(tipoDocumento.getText())
                        .arrendadorDocumento(documento.getText())
                        .arrendadorDireccion(direccion.getText())
                        .arrendadorTelefono(telefono.getText())
                        .arrendadorEmail(email.getText())
                        .logoArchivo(
                                jlccVacio(logoArchivo.getText()) ? null : logoArchivo.getText().trim())
                        .reciboSerie(reciboSerie.getText())
                        .reciboLeyenda(reciboLeyenda.getText())
                        .moraTipo(moraTipo.getValue())
                        .moraValor(jlccDecimal(moraValor.getText(), "El valor de la mora"))
                        .moraDiasGracia(jlccEntero(moraDiasGracia.getText(), "Los días de gracia"))
                        .alertasDiasVencimiento(
                                jlccEntero(diasVencimiento.getText(), "Los días para por vencer"))
                        .alertaRecordatorioDias(
                                jlccEntero(diasRecordatorio.getText(), "Los días del recordatorio"))
                        .semaforoUmbralLeveDias(
                                jlccEntero(umbralLeve.getText(), "El umbral de atraso leve"))
                        .semaforoCuotasRojo(jlccEntero(cuotasRojo.getText(), "Las cuotas para rojo"))
                        .plantillaPreventivo(plantillaPreventivo.getText())
                        .plantillaVencido(plantillaVencido.getText())
                        .plantillaFinContrato(plantillaFinContrato.getText())
                        .inactividadMinutos(
                                jlccEntero(inactividad.getText(), "Los minutos de inactividad"))
                        .version(jlccVersion)
                        .build();
        return c;
    }

    @FXML
    private void jlccOnElegirLogo() {
        FileChooser jlccSelector = new FileChooser();
        jlccSelector.setTitle("Elegir logo del arrendador");
        jlccSelector.getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "Imágenes PNG o JPG", "*.png", "*.jpg", "*.jpeg"));
        File jlccArchivo = jlccSelector.showOpenDialog(jlccVentana());
        if (jlccArchivo == null) return;
        jlccTareas.jlccEjecutar(
                () -> jlccConfiguracion.jlccCopiarLogo(jlccArchivo.toPath()),
                nombre -> {
                    logoArchivo.setText(nombre);
                    jlccActualizarVistaLogo(nombre);
                    jlccAviso.jlccInfo("Logo listo. Guarda la configuración para aplicarlo.");
                },
                e -> jlccAviso.jlccError(jlccMensaje(e)));
    }

    @FXML
    private void jlccOnQuitarLogo() {
        logoArchivo.clear();
        logoVista.setImage(null);
        logoVista.setVisible(false);
        logoVista.setManaged(false);
    }

    @FXML
    private void jlccOnRestaurarPlantillas() {
        plantillaPreventivo.setText(JlccPlantillasMensaje.JLCC_PREVENTIVO);
        plantillaVencido.setText(JlccPlantillasMensaje.JLCC_VENCIDO);
        plantillaFinContrato.setText(JlccPlantillasMensaje.JLCC_FIN_CONTRATO);
        jlccAviso.jlccInfo("Plantillas restauradas. Revisa y guarda los cambios.");
    }

    private void jlccActualizarVistaLogo(String jlccArchivo) {
        Path jlccRuta =
                jlccArchivo == null || jlccArchivo.isBlank()
                        ? null
                        : jlccConfiguracion.jlccRutaDatos().resolve(jlccArchivo);
        if (jlccRuta != null && java.nio.file.Files.isRegularFile(jlccRuta)) {
            logoVista.setImage(new Image(jlccRuta.toUri().toString(), 160, 70, true, true));
            logoVista.setVisible(true);
            logoVista.setManaged(true);
        } else {
            logoVista.setImage(null);
            logoVista.setVisible(false);
            logoVista.setManaged(false);
        }
    }

    private Window jlccVentana() {
        return razonSocial.getScene() == null ? null : razonSocial.getScene().getWindow();
    }

    private static String jlccTexto(String jlccValor) {
        return jlccValor == null ? "" : jlccValor;
    }

    private static boolean jlccVacio(String jlccValor) {
        return jlccValor == null || jlccValor.isBlank();
    }

    private static int jlccEntero(String jlccValor, String jlccCampo) {
        try {
            return Integer.parseInt(jlccValor == null ? "" : jlccValor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(jlccCampo + " debe ser un número entero.");
        }
    }

    private static java.math.BigDecimal jlccDecimal(String jlccValor, String jlccCampo) {
        try {
            return new java.math.BigDecimal((jlccValor == null ? "" : jlccValor.trim()).replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(jlccCampo + " debe ser un número válido.");
        }
    }

    private static String jlccMensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }
}

package com.ejemplo.alquileres.controller;

import com.ejemplo.alquileres.model.AccionAuditoria;
import com.ejemplo.alquileres.model.AuditoriaEvento;
import com.ejemplo.alquileres.model.Configuracion;
import com.ejemplo.alquileres.model.RolUsuario;
import com.ejemplo.alquileres.model.TipoMora;
import com.ejemplo.alquileres.model.Usuario;
import com.ejemplo.alquileres.service.AuditoriaService;
import com.ejemplo.alquileres.service.ConfiguracionService;
import com.ejemplo.alquileres.service.Permisos;
import com.ejemplo.alquileres.service.PlantillasMensaje;
import com.ejemplo.alquileres.service.ReporteAuditoriaExcelService;
import com.ejemplo.alquileres.service.RespaldoService;
import com.ejemplo.alquileres.service.UsuarioService;
import com.ejemplo.alquileres.ui.AplicacionUi;
import com.ejemplo.alquileres.ui.Exportador;
import com.ejemplo.alquileres.ui.Formularios;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Refrescable;
import com.ejemplo.alquileres.ui.Tareas;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

/** Parámetros editables de la aplicación, agrupados en pestañas. */
@Component
public class ConfiguracionController implements Refrescable {
    private final ConfiguracionService configuracion;
    private final UsuarioService usuariosServicio;
    private final AuditoriaService auditoria;
    private final ReporteAuditoriaExcelService reporteAuditoria;
    private final RespaldoService respaldos;
    private final AplicacionUi aplicacion;
    private final Permisos permisos;
    private final Tareas tareas;
    private final Notificador aviso;
    private final Exportador exportador;

    @FXML private TextField razonSocial, tipoDocumento, documento, direccion, telefono, email, logoArchivo;
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
    @FXML private TableColumn<Usuario, String> colUsuario, colNombreUsuario, colRolUsuario, colEstadoUsuario, colUltimoAcceso;
    @FXML private TableView<AuditoriaEvento> tablaAuditoria;
    @FXML private TableColumn<AuditoriaEvento, String> colAudFecha, colAudUsuario, colAudAccion, colAudEntidad, colAudDetalle;
    @FXML private DatePicker audDesde, audHasta;
    @FXML private TextField audUsuario;
    @FXML private ComboBox<AccionAuditoria> audAccion;
    @FXML private TableView<RespaldoService.Respaldo> tablaRespaldos;
    @FXML private TableColumn<RespaldoService.Respaldo, String> colRespNombre, colRespFecha, colRespTamano;
    @FXML private Label rutaDatos;

    private final ObservableList<Usuario> listaUsuarios = FXCollections.observableArrayList();
    private final ObservableList<AuditoriaEvento> listaAuditoria = FXCollections.observableArrayList();
    private final ObservableList<RespaldoService.Respaldo> listaRespaldos = FXCollections.observableArrayList();
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Long version;

    public ConfiguracionController(ConfiguracionService configuracion, UsuarioService usuariosServicio,
                                   AuditoriaService auditoria, ReporteAuditoriaExcelService reporteAuditoria,
                                   RespaldoService respaldos, AplicacionUi aplicacion, Permisos permisos,
                                   Tareas tareas, Notificador aviso, Exportador exportador) {
        this.configuracion = configuracion;
        this.usuariosServicio = usuariosServicio;
        this.auditoria = auditoria;
        this.reporteAuditoria = reporteAuditoria;
        this.respaldos = respaldos;
        this.aplicacion = aplicacion;
        this.permisos = permisos;
        this.tareas = tareas;
        this.aviso = aviso;
        this.exportador = exportador;
    }

    @FXML public void initialize() {
        moraTipo.setItems(FXCollections.observableArrayList(TipoMora.values()));
        audAccion.setItems(FXCollections.observableArrayList(AccionAuditoria.values()));
        Formularios.soloDigitos(moraDiasGracia, 3);
        Formularios.soloDigitos(diasVencimiento, 4);
        Formularios.soloDigitos(diasRecordatorio, 4);
        Formularios.soloDigitos(umbralLeve, 4);
        Formularios.soloDigitos(cuotasRojo, 3);
        Formularios.soloDigitos(inactividad, 4);
        Formularios.soloDecimal(moraValor, 4);
        prepararTablaUsuarios();
        prepararTablaAuditoria();
        prepararTablaRespaldos();
        rutaDatos.setText(configuracion.rutaDatos().toString());
        refrescar();
    }

    @Override public void refrescar() {
        tareas.ejecutar(configuracion::actual, this::cargar, e -> aviso.error(mensaje(e)));
        if (permisos.esAdmin()) {
            cargarUsuarios();
            cargarAuditoria();
            cargarRespaldos();
        }
    }

    private void prepararTablaUsuarios() {
        colUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        colNombreUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colRolUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRolEtiqueta()));
        colEstadoUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colUltimoAcceso.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUltimoAcceso() == null
                ? "—" : c.getValue().getUltimoAcceso().format(FECHA_HORA)));
        tablaUsuarios.setItems(listaUsuarios);
    }

    private void prepararTablaAuditoria() {
        colAudFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha() == null
                ? "" : c.getValue().getFecha().format(FECHA_HORA)));
        colAudUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsuario()));
        colAudAccion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAccion() == null
                ? "" : c.getValue().getAccion().getEtiqueta()));
        colAudEntidad.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEntidad()));
        colAudDetalle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDetalle()));
        tablaAuditoria.setItems(listaAuditoria);
    }

    private void prepararTablaRespaldos() {
        colRespNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        colRespFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fecha().format(FECHA_HORA)));
        colRespTamano.setCellValueFactory(c -> new SimpleStringProperty(tamano(c.getValue().bytes())));
        tablaRespaldos.setItems(listaRespaldos);
    }

    private void cargarRespaldos() {
        tareas.ejecutar(respaldos::listarAutomaticos, lista -> listaRespaldos.setAll(lista), e -> aviso.error(mensaje(e)));
    }

    @FXML private void onCrearRespaldo() {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Carpeta para el respaldo");
        File carpeta = selector.showDialog(ventana());
        if (carpeta == null) return;
        tareas.ejecutar(() -> respaldos.crearRespaldo(carpeta.toPath()),
                ruta -> { aviso.info("Respaldo creado: " + ruta.getFileName()); cargarRespaldos(); },
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onRestaurarRespaldo() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir respaldo para restaurar");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos SQLite", "*.db"));
        File inicial = respaldos.rutaRespaldos().toFile();
        if (inicial.isDirectory()) selector.setInitialDirectory(inicial);
        File archivo = selector.showOpenDialog(ventana());
        if (archivo == null) return;
        javafx.scene.control.Alert confirmacion = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "Se sustituirá la base de datos actual por el respaldo elegido.\n"
                        + "Antes se guardará una copia de seguridad. La aplicación se cerrará al terminar.\n\n"
                        + "¿Deseas continuar?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText("Restaurar respaldo");
        confirmacion.setTitle("Confirmar restauración");
        if (confirmacion.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        tareas.ejecutar(() -> respaldos.restaurar(archivo.toPath()),
                resultado -> {
                    javafx.scene.control.Alert fin = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.INFORMATION,
                            "Respaldo restaurado correctamente.\n\nLa aplicación se cerrará. "
                                    + "Vuelve a abrirla para trabajar con los datos restaurados.",
                            ButtonType.OK);
                    fin.setHeaderText("Restauración completada");
                    fin.setTitle("Respaldo restaurado");
                    fin.showAndWait();
                    aplicacion.salir();
                },
                e -> aviso.error(mensaje(e)));
    }

    private static String tamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void cargarUsuarios() {
        tareas.ejecutar(usuariosServicio::listar, lista -> listaUsuarios.setAll(lista), e -> aviso.error(mensaje(e)));
    }

    private void cargarAuditoria() {
        LocalDateTime desde = audDesde.getValue() == null ? null : audDesde.getValue().atStartOfDay();
        LocalDateTime hasta = audHasta.getValue() == null ? null : audHasta.getValue().atTime(23, 59, 59);
        tareas.ejecutar(() -> auditoria.listar(desde, hasta, audUsuario.getText(), audAccion.getValue()),
                lista -> listaAuditoria.setAll(lista), e -> aviso.error(mensaje(e)));
    }

    @FXML private void onBuscarAuditoria() { cargarAuditoria(); }

    @FXML private void onLimpiarAuditoria() {
        audDesde.setValue(null);
        audHasta.setValue(null);
        audUsuario.clear();
        audAccion.setValue(null);
        cargarAuditoria();
    }

    @FXML private void onExportarAuditoria() {
        if (listaAuditoria.isEmpty()) { aviso.info("No hay eventos para exportar."); return; }
        File destino = exportador.elegirDestino(ventana(), "auditoria-" + java.time.LocalDate.now() + ".xlsx", "Excel", "xlsx");
        if (destino == null) return;
        tareas.ejecutar(() -> reporteAuditoria.generar(List.copyOf(listaAuditoria)),
                bytes -> { exportador.escribir(destino, bytes); aviso.info("Auditoría exportada."); },
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onNuevoUsuario() {
        TextField usuario = new TextField();
        TextField nombre = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirmacion = new PasswordField();
        ComboBox<RolUsuario> rol = new ComboBox<>(FXCollections.observableArrayList(RolUsuario.values()));
        rol.setValue(RolUsuario.OPERADOR);
        CheckBox activo = new CheckBox("Activo");
        activo.setSelected(true);
        GridPane formulario = formulario();
        fila(formulario, 0, "Usuario", usuario);
        fila(formulario, 1, "Nombre completo", nombre);
        fila(formulario, 2, "Contraseña", password);
        fila(formulario, 3, "Confirmar contraseña", confirmacion);
        fila(formulario, 4, "Rol", rol);
        formulario.add(activo, 1, 5);
        Dialog<ButtonType> dialogo = dialogo("Nuevo usuario");
        dialogo.getDialogPane().setContent(formulario);
        if (dialogo.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        tareas.ejecutar(() -> usuariosServicio.crear(usuario.getText(), nombre.getText(), password.getText(),
                        confirmacion.getText(), rol.getValue(), activo.isSelected()),
                creado -> { aviso.info("Usuario creado."); cargarUsuarios(); },
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onEditarUsuario() {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) { aviso.info("Selecciona un usuario."); return; }
        TextField usuario = new TextField(seleccionado.getUsername());
        usuario.setDisable(true);
        TextField nombre = new TextField(seleccionado.getNombre());
        ComboBox<RolUsuario> rol = new ComboBox<>(FXCollections.observableArrayList(RolUsuario.values()));
        rol.setValue(seleccionado.getRol());
        CheckBox activo = new CheckBox("Activo");
        activo.setSelected(seleccionado.isActivo());
        GridPane formulario = formulario();
        fila(formulario, 0, "Usuario", usuario);
        fila(formulario, 1, "Nombre completo", nombre);
        fila(formulario, 2, "Rol", rol);
        formulario.add(activo, 1, 3);
        Dialog<ButtonType> dialogo = dialogo("Editar usuario");
        dialogo.getDialogPane().setContent(formulario);
        if (dialogo.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        tareas.ejecutar(() -> usuariosServicio.actualizar(seleccionado.getId(), nombre.getText(), rol.getValue(), activo.isSelected()),
                actualizado -> { aviso.info("Usuario actualizado."); cargarUsuarios(); },
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onDesactivarUsuario() {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) { aviso.info("Selecciona un usuario."); return; }
        tareas.ejecutar(() -> usuariosServicio.actualizar(seleccionado.getId(), seleccionado.getNombre(),
                        seleccionado.getRol(), !seleccionado.isActivo()),
                actualizado -> { aviso.info(actualizado.isActivo() ? "Usuario activado." : "Usuario desactivado."); cargarUsuarios(); },
                e -> aviso.error(mensaje(e)));
    }

    @FXML private void onRestablecerPassword() {
        Usuario seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) { aviso.info("Selecciona un usuario."); return; }
        PasswordField password = new PasswordField();
        PasswordField confirmacion = new PasswordField();
        GridPane formulario = formulario();
        fila(formulario, 0, "Nueva contraseña", password);
        fila(formulario, 1, "Confirmar contraseña", confirmacion);
        Dialog<ButtonType> dialogo = dialogo("Restablecer contraseña de " + seleccionado.getUsername());
        dialogo.getDialogPane().setContent(formulario);
        if (dialogo.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        tareas.ejecutar(() -> { usuariosServicio.restablecerPassword(seleccionado.getId(), password.getText(), confirmacion.getText()); return null; },
                ignorado -> aviso.info("Contraseña restablecida."),
                e -> aviso.error(mensaje(e)));
    }

    private GridPane formulario() {
        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(10);
        formulario.setPrefWidth(360);
        return formulario;
    }

    private void fila(GridPane formulario, int fila, String etiqueta, javafx.scene.control.Control control) {
        Label label = new Label(etiqueta);
        label.getStyleClass().add("campo-label");
        formulario.add(label, 0, fila);
        formulario.add(control, 1, fila);
        if (control instanceof TextField texto) texto.setPrefColumnCount(20);
    }

    private Dialog<ButtonType> dialogo(String titulo) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().getStyleClass().add("dialogo");
        dialogo.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        if (ventana() != null) dialogo.initOwner(ventana());
        return dialogo;
    }

    private void cargar(Configuracion c) {
        version = c.getVersion();
        razonSocial.setText(texto(c.getArrendadorRazonSocial()));
        tipoDocumento.setText(texto(c.getArrendadorTipoDocumento()));
        documento.setText(texto(c.getArrendadorDocumento()));
        direccion.setText(texto(c.getArrendadorDireccion()));
        telefono.setText(texto(c.getArrendadorTelefono()));
        email.setText(texto(c.getArrendadorEmail()));
        logoArchivo.setText(texto(c.getLogoArchivo()));
        actualizarVistaLogo(c.getLogoArchivo());
        reciboSerie.setText(texto(c.getReciboSerie()));
        reciboLeyenda.setText(texto(c.getReciboLeyenda()));
        moraTipo.setValue(c.getMoraTipo());
        moraValor.setText(c.getMoraValor() == null ? "0" : c.getMoraValor().toPlainString());
        moraDiasGracia.setText(String.valueOf(c.getMoraDiasGracia()));
        diasVencimiento.setText(String.valueOf(c.getAlertasDiasVencimiento()));
        diasRecordatorio.setText(String.valueOf(c.getAlertaRecordatorioDias()));
        umbralLeve.setText(String.valueOf(c.getSemaforoUmbralLeveDias()));
        cuotasRojo.setText(String.valueOf(c.getSemaforoCuotasRojo()));
        plantillaPreventivo.setText(texto(c.getPlantillaPreventivo()));
        plantillaVencido.setText(texto(c.getPlantillaVencido()));
        plantillaFinContrato.setText(texto(c.getPlantillaFinContrato()));
        inactividad.setText(String.valueOf(c.getInactividadMinutos()));
        estado.setText("Configuración cargada.");
    }

    @FXML private void onGuardar() {
        tareas.ejecutar(() -> configuracion.guardar(leerFormulario()), guardada -> {
            version = guardada.getVersion();
            estado.setText("Guardado correctamente.");
            aviso.info("Configuración guardada.");
        }, e -> {
            estado.setText("No se pudo guardar.");
            aviso.error(mensaje(e));
        });
    }

    private Configuracion leerFormulario() {
        Configuracion c = Configuracion.builder()
                .id(Configuracion.FILA_UNICA)
                .arrendadorRazonSocial(razonSocial.getText())
                .arrendadorTipoDocumento(tipoDocumento.getText())
                .arrendadorDocumento(documento.getText())
                .arrendadorDireccion(direccion.getText())
                .arrendadorTelefono(telefono.getText())
                .arrendadorEmail(email.getText())
                .logoArchivo(vacio(logoArchivo.getText()) ? null : logoArchivo.getText().trim())
                .reciboSerie(reciboSerie.getText())
                .reciboLeyenda(reciboLeyenda.getText())
                .moraTipo(moraTipo.getValue())
                .moraValor(decimal(moraValor.getText(), "El valor de la mora"))
                .moraDiasGracia(entero(moraDiasGracia.getText(), "Los días de gracia"))
                .alertasDiasVencimiento(entero(diasVencimiento.getText(), "Los días para por vencer"))
                .alertaRecordatorioDias(entero(diasRecordatorio.getText(), "Los días del recordatorio"))
                .semaforoUmbralLeveDias(entero(umbralLeve.getText(), "El umbral de atraso leve"))
                .semaforoCuotasRojo(entero(cuotasRojo.getText(), "Las cuotas para rojo"))
                .plantillaPreventivo(plantillaPreventivo.getText())
                .plantillaVencido(plantillaVencido.getText())
                .plantillaFinContrato(plantillaFinContrato.getText())
                .inactividadMinutos(entero(inactividad.getText(), "Los minutos de inactividad"))
                .version(version)
                .build();
        return c;
    }

    @FXML private void onElegirLogo() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir logo del arrendador");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG o JPG", "*.png", "*.jpg", "*.jpeg"));
        File archivo = selector.showOpenDialog(ventana());
        if (archivo == null) return;
        tareas.ejecutar(() -> configuracion.copiarLogo(archivo.toPath()), nombre -> {
            logoArchivo.setText(nombre);
            actualizarVistaLogo(nombre);
            aviso.info("Logo listo. Guarda la configuración para aplicarlo.");
        }, e -> aviso.error(mensaje(e)));
    }

    @FXML private void onQuitarLogo() {
        logoArchivo.clear();
        logoVista.setImage(null);
        logoVista.setVisible(false);
        logoVista.setManaged(false);
    }

    @FXML private void onRestaurarPlantillas() {
        plantillaPreventivo.setText(PlantillasMensaje.PREVENTIVO);
        plantillaVencido.setText(PlantillasMensaje.VENCIDO);
        plantillaFinContrato.setText(PlantillasMensaje.FIN_CONTRATO);
        aviso.info("Plantillas restauradas. Revisa y guarda los cambios.");
    }

    private void actualizarVistaLogo(String archivo) {
        Path ruta = archivo == null || archivo.isBlank() ? null : configuracion.rutaDatos().resolve(archivo);
        if (ruta != null && java.nio.file.Files.isRegularFile(ruta)) {
            logoVista.setImage(new Image(ruta.toUri().toString(), 160, 70, true, true));
            logoVista.setVisible(true);
            logoVista.setManaged(true);
        } else {
            logoVista.setImage(null);
            logoVista.setVisible(false);
            logoVista.setManaged(false);
        }
    }

    private Window ventana() { return razonSocial.getScene() == null ? null : razonSocial.getScene().getWindow(); }
    private static String texto(String valor) { return valor == null ? "" : valor; }
    private static boolean vacio(String valor) { return valor == null || valor.isBlank(); }

    private static int entero(String valor, String campo) {
        try {
            return Integer.parseInt(valor == null ? "" : valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " debe ser un número entero.");
        }
    }

    private static java.math.BigDecimal decimal(String valor, String campo) {
        try {
            return new java.math.BigDecimal((valor == null ? "" : valor.trim()).replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " debe ser un número válido.");
        }
    }

    private static String mensaje(Throwable e) {
        return e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage() : e.getMessage();
    }
}

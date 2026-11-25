package com.ejemplo.alquileres.controller;

import static com.ejemplo.alquileres.model.EstadoInmueble.ALQUILADO;
import static com.ejemplo.alquileres.model.EstadoInmueble.DISPONIBLE;
import static com.ejemplo.alquileres.model.EstadoInmueble.INACTIVO;
import static com.ejemplo.alquileres.model.EstadoInmueble.MANTENIMIENTO;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.service.JlccMaquetaService;
import com.ejemplo.alquileres.ui.BloqueMaqueta;
import com.ejemplo.alquileres.ui.JlccFechas;
import com.ejemplo.alquileres.ui.JlccGeneradorEsquema;
import com.ejemplo.alquileres.ui.JlccNivelVista;
import com.ejemplo.alquileres.ui.JlccNotificador;
import com.ejemplo.alquileres.ui.JlccPlantaPiso;
import com.ejemplo.alquileres.ui.JlccRefrescable;
import com.ejemplo.alquileres.ui.JlccSemaforo;
import com.ejemplo.alquileres.ui.JlccTablas;
import com.ejemplo.alquileres.ui.JlccTareas;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.AmbientLight;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.stage.Window;
import javafx.util.Duration;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope("prototype")
public class JlccMaquetaController implements JlccRefrescable {

    private static final Logger JLCC_log = LoggerFactory.getLogger(JlccMaquetaController.class);

    private static final double JLCC_ANCHO_UNIDAD = 62;
    private static final double JLCC_ALTO_UNIDAD = 48;
    private static final double JLCC_PROFUNDIDAD_UNIDAD = 64;
    private static final int JLCC_MAX_UNIDADES_POR_FILA = 8;
    private static final double JLCC_SEPARACION_UNIDAD = 10;
    private static final double JLCC_SEPARACION_FILA = 38;
    private static final double JLCC_SEPARACION_PISO = 42;
    private static final double JLCC_PASO_X = JLCC_ANCHO_UNIDAD + JLCC_SEPARACION_UNIDAD;
    private static final double JLCC_PASO_Y = JLCC_ALTO_UNIDAD + JLCC_SEPARACION_PISO;
    private static final double JLCC_PASO_Z = JLCC_PROFUNDIDAD_UNIDAD + JLCC_SEPARACION_FILA;

    private static final double JLCC_LOSA_ALTO = 4;
    private static final double JLCC_LOSA_MARGEN_X = 8;
    private static final double JLCC_LOSA_MARGEN_Z = 8;
    private static final double JLCC_BASE_ALTO = 8;
    private static final double JLCC_TECHO_ALTO = 6;
    private static final double JLCC_TECHO_ELEVACION = 22;
    private static final double JLCC_COLUMNA_ANCHO = 6;
    private static final double JLCC_COLUMNA_MARGEN = 18;
    private static final double JLCC_MARGEN_SUELO_X = 70;
    private static final double JLCC_MARGEN_SUELO_Z = 60;
    private static final double JLCC_SEPARACION_ETIQUETA_PANTALLA = 12;
    private static final double JLCC_RADIO_MARCA = 11.5;
    private static final double JLCC_ALTO_MARCA = 5;

    private static final double JLCC_ANGULO_Y_INICIAL = -38;
    private static final double JLCC_ANGULO_X_INICIAL = 34;
    private static final double JLCC_ANGULO_X_MINIMO = -8;
    private static final double JLCC_ANGULO_X_MAXIMO = 65;
    private static final double JLCC_ANGULO_X_PISO = 72;
    private static final double JLCC_ANGULO_Y_PISO = 0;
    private static final double JLCC_ANGULO_X_ESQUEMA = 80;
    private static final double JLCC_ANGULO_Y_ESQUEMA = 0;

    private static final double JLCC_ALTO_PARED_PLANTA = 7;
    private static final double JLCC_GROSOR_PARED_PLANTA = 1.6;
    private static final double JLCC_ALTO_PLACA_PLANTA = 2;
    private static final double JLCC_ALTO_CONTORNO_PLANTA = 10;
    private static final double JLCC_RADIO_MARCA_PLANTA = 7;
    private static final double JLCC_ALTO_MARCA_PLANTA = 1.6;

    private static final double JLCC_ESCALA_ESQUEMA = 10;
    private static final double JLCC_ALTO_PARED_ESQUEMA = 2.6 * JLCC_ESCALA_ESQUEMA;
    private static final double JLCC_GROSOR_PARED_ESQUEMA = 0.15 * JLCC_ESCALA_ESQUEMA;
    private static final double JLCC_ALTO_PLACA_ESQUEMA = 2;
    private static final double JLCC_ANCHO_PANEL_UNIDAD = 440;
    private static final double JLCC_ANCHO_PANEL_UNIDAD_MINIMO = 360;
    private static final double JLCC_MARGEN_PANEL_UNIDAD = 56;
    private static final Duration JLCC_DURACION_FUNDIDO = Duration.millis(160);

    private static final double JLCC_RELLENO_ENCUADRE = 0.88;
    private static final double JLCC_FACTOR_ZOOM_CERCA = 0.55;
    private static final double JLCC_FACTOR_ZOOM_LEJOS = 2.4;
    private static final Duration JLCC_DURACION_TRANSICION = Duration.millis(450);
    private static final double JLCC_ESCALA_OCULTA = 0;
    private static final double JLCC_ESCALA_VISIBLE = 1;

    private static final Color JLCC_COLOR_DISPONIBLE = Color.web("#2f9e6e");
    private static final Color JLCC_COLOR_ALQUILADO = Color.web("#3d7fc4");
    private static final Color JLCC_COLOR_MANTENIMIENTO = Color.web("#e08a2e");
    private static final Color JLCC_COLOR_INACTIVO = Color.web("#8a94a6");
    private static final Color JLCC_COLOR_ESTRUCTURA = Color.web("#8a96a8");
    private static final Color JLCC_COLOR_PASILLO = Color.web("#6f7c8e");
    private static final Color JLCC_COLOR_LOSA = Color.web("#95a2b4");
    private static final Color JLCC_COLOR_ESQUEMA_SALA = Color.web("#8fbfae");
    private static final Color JLCC_COLOR_ESQUEMA_COCINA = Color.web("#d8b579");
    private static final Color JLCC_COLOR_ESQUEMA_DORMITORIO_PRINCIPAL = Color.web("#89a9cf");
    private static final Color JLCC_COLOR_ESQUEMA_DORMITORIO = Color.web("#b3c9e2");
    private static final Color JLCC_COLOR_ESQUEMA_BANO = Color.web("#8fc3c8");
    private static final Color JLCC_COLOR_ESQUEMA_PASILLO = Color.web("#b9bec8");
    private static final Color JLCC_COLOR_ESQUEMA_AMBIENTE = Color.web("#b4a9cf");
    private static final Color JLCC_COLOR_LINEA_CLARA = Color.web("#243247");
    private static final Color JLCC_COLOR_LINEA_OSCURA = Color.web("#e5e9f0");
    private static final Color JLCC_COLOR_PARED_ESQUEMA = Color.web("#c7ced8");
    private static final Color JLCC_COLOR_MORA_AL_DIA = Color.web("#2f9e6e");
    private static final Color JLCC_COLOR_MORA_ATRASO = Color.web("#ffc107");
    private static final Color JLCC_COLOR_MORA_MOROSO = Color.web("#e53935");
    private static final Color JLCC_COLOR_SIN_DATO = Color.web("#b8c1cd");
    private static final Color JLCC_COLOR_VENCIDO = Color.web("#8e1b1b");
    private static final Color JLCC_COLOR_RENTA_INICIO = Color.web("#cfe2f7");
    private static final Color JLCC_COLOR_RENTA_FIN = Color.web("#1f5f9e");
    private static final Color JLCC_COLOR_SELECCION = Color.web("#f8fafc");
    private static final Color JLCC_COLOR_HOVER = Color.web("#9fd3ff");
    private static final Color JLCC_COLOR_SUELO_CLARO_CENTRO = Color.web("#d7dee8");
    private static final Color JLCC_COLOR_SUELO_CLARO_BORDE = Color.web("#9eacbe");
    private static final Color JLCC_COLOR_SUELO_OSCURO_CENTRO = Color.web("#566273");
    private static final Color JLCC_COLOR_SUELO_OSCURO_BORDE = Color.web("#303945");

    private static final List<Color> JLCC_COLORES_RENTA = jlccCrearPaletaRenta();

    private static final double JLCC_ALFA_ATENUADO = 0.12;
    private static final Pattern JLCC_NUMERO_UNIDAD = Pattern.compile("(\\d{1,4})\\s*$");
    private static final String JLCC_PREFERENCIA_MODO = "maqueta.modo";

    @FXML private StackPane contenedor3d;
    @FXML private Pane capaEtiquetas;
    @FXML private Canvas lienzoPlano;
    @FXML private Region veloVista;
    @FXML private Label etiquetaHover, ayuda, avisoUbicacion, resumen;
    @FXML private Label migaNavegacion, avisoEsquema, avisosUnidad;
    @FXML private Button btnRestablecer, btnVolverEdificio;
    @FXML private ComboBox<String> selectorModo;
    @FXML private HBox leyenda, barraNavegacion, alternadorVista;
    @FXML private VBox panelUnidad, cuerpoPanelUnidad;
    @FXML private ToggleButton btnPlano, btnTresD;

    private final JlccMaquetaService jlccService;
    private final JlccTareas jlccTareas;
    private final JlccNotificador jlccNotificador;
    private final JlccMainController jlccPrincipal;

    private final Map<EstadoInmueble, PhongMaterial> jlccMateriales =
            new EnumMap<>(EstadoInmueble.class);
    private final Map<EstadoInmueble, PhongMaterial> jlccMaterialesAtenuados =
            new EnumMap<>(EstadoInmueble.class);
    private final Map<Color, PhongMaterial> jlccMaterialesMora = new HashMap<>();
    private final Map<Color, PhongMaterial> jlccMaterialesMoraAtenuados = new HashMap<>();
    private final Map<BloqueMaqueta.CategoriaMora, PhongMaterial> jlccMaterialesCategoriaMora =
            new EnumMap<>(BloqueMaqueta.CategoriaMora.class);
    private final Map<BloqueMaqueta.CategoriaMora, PhongMaterial> jlccMaterialesCategoriaMoraAtenuados =
            new EnumMap<>(BloqueMaqueta.CategoriaMora.class);
    private final Map<BloqueMaqueta.CategoriaVencimiento, PhongMaterial> jlccMaterialesVencimiento =
            new EnumMap<>(BloqueMaqueta.CategoriaVencimiento.class);
    private final Map<BloqueMaqueta.CategoriaVencimiento, PhongMaterial>
            jlccMaterialesVencimientoAtenuados =
                    new EnumMap<>(BloqueMaqueta.CategoriaVencimiento.class);
    private final List<PhongMaterial> jlccMaterialesRenta = new ArrayList<>();
    private final List<PhongMaterial> jlccMaterialesRentaAtenuados = new ArrayList<>();
    private final Map<JlccGeneradorEsquema.JlccTipoAmbiente, PhongMaterial> jlccMaterialesAmbiente =
            new EnumMap<>(JlccGeneradorEsquema.JlccTipoAmbiente.class);
    private final Map<Node, BloqueMaqueta> jlccMapaBloques = new HashMap<>();
    private final Map<BloqueMaqueta, Node> jlccNodosPorBloque = new LinkedHashMap<>();
    private final Map<BloqueMaqueta, Box> jlccCajasPorBloque = new HashMap<>();
    private final Map<BloqueMaqueta, Cylinder> jlccMarcasPorBloque = new HashMap<>();
    private final Map<Integer, Group> jlccGruposPiso = new LinkedHashMap<>();
    private final Map<Integer, Group> jlccGruposBloques = new LinkedHashMap<>();
    private final Map<Integer, Group> jlccGruposPlanta = new LinkedHashMap<>();
    private final Map<Integer, Group> jlccGruposRecintos = new LinkedHashMap<>();
    private final Map<BloqueMaqueta, Node> jlccNodosPlantaPorBloque = new LinkedHashMap<>();
    private final Map<BloqueMaqueta, Box> jlccCajasPlantaPorBloque = new HashMap<>();
    private final Map<BloqueMaqueta, Cylinder> jlccMarcasPlantaPorBloque = new HashMap<>();
    private final Map<BloqueMaqueta, JlccPlantaPiso.Recinto> jlccRecintosPlantaPorBloque = new HashMap<>();
    private final Map<Integer, JlccPlantaPiso.Distribucion> jlccDistribucionesPlanta =
            new LinkedHashMap<>();
    private final List<Box> jlccBasesPlanta = new ArrayList<>();
    private final Map<Node, Integer> jlccMapaPisos = new HashMap<>();
    private final List<EtiquetaPiso> jlccEtiquetasPiso = new ArrayList<>();
    private final List<EtiquetaUnidad> jlccEtiquetasUnidad = new ArrayList<>();
    private final List<EtiquetaAmbiente> jlccEtiquetasAmbiente = new ArrayList<>();
    private final Preferences jlccPreferencias =
            Preferences.userNodeForPackage(JlccMaquetaController.class);
    private final EventHandler<KeyEvent> jlccManejadorTecla = this::jlccAlPresionarTecla;

    private Group jlccMundo;
    private Group jlccEstructuraGeneral;
    private SubScene jlccSubEscena;
    private PerspectiveCamera jlccCamara;
    private Rotate jlccRotacionX, jlccRotacionY;
    private PhongMaterial jlccMaterialEstructura,
            jlccMaterialSeleccion,
            jlccMaterialHover,
            jlccMaterialLosa,
            jlccMaterialPasillo,
            jlccMaterialSueloClaro,
            jlccMaterialSueloOscuro,
            jlccMaterialParedEsquema,
            jlccMaterialSinDato,
            jlccMaterialSinDatoAtenuado;
    private Box jlccSuelo;
    private Geometria jlccGeometria;
    private List<Point3D> jlccPuntosEncuadre = List.of();
    private Box jlccContornoSeleccion;
    private Box jlccContornoHover;
    private Box jlccContornoSeleccionPlanta;
    private Box jlccContornoHoverPlanta;

    private JlccNivelVista jlccNivelVista = JlccNivelVista.EDIFICIO;
    private Integer jlccNivelEnfocado;
    private Timeline jlccTransicion;
    private Scene jlccEscenaTeclado;
    private double jlccAnguloXEdificio = JLCC_ANGULO_X_INICIAL;
    private double jlccAnguloYEdificio = JLCC_ANGULO_Y_INICIAL;

    private Group jlccGrupoUnidad;
    private JlccGeneradorEsquema.Esquema jlccEsquemaActual;
    private BloqueMaqueta jlccBloqueUnidad;
    private JlccMaquetaService.DetalleMaqueta jlccDetalleUnidad;
    private boolean jlccVistaPlano;
    private double jlccZoomPlano = 1;
    private Timeline jlccTransicionVista;
    private double jlccAnguloYPisoAntesUnidad = JLCC_ANGULO_Y_PISO;

    private double jlccCentroZAuto;
    private double jlccDistanciaAuto;

    private double jlccXPresionado, jlccYPresionado, jlccXInicialClic, jlccYInicialClic;
    private boolean jlccArrastrando;
    private boolean jlccEscenaIntentada;
    private BloqueMaqueta jlccBloqueHover;
    private Node jlccNodoHover;
    private VBox jlccMensajeEscena;

    private final List<ItemLeyenda> jlccItemsLeyenda = new ArrayList<>();
    private List<BloqueMaqueta> jlccBloquesActuales = List.of();
    private JlccModoVista jlccModoVista = JlccModoVista.ESTADO;
    private BigDecimal jlccRentaMinima;
    private BigDecimal jlccRentaMaxima;
    private String jlccFiltroClave;
    private boolean jlccFiltroActivo;
    private boolean jlccFiltroFijo;

    public JlccMaquetaController(
            JlccMaquetaService jlccService,
            JlccTareas jlccTareas,
            JlccNotificador jlccNotificador,
            JlccMainController jlccPrincipal) {
        this.jlccService = jlccService;
        this.jlccTareas = jlccTareas;
        this.jlccNotificador = jlccNotificador;
        this.jlccPrincipal = jlccPrincipal;
    }

    @FXML
    public void initialize() {
        jlccModoVista =
                JlccModoVista.jlccDesdePreferencia(
                        jlccPreferencias.get(JLCC_PREFERENCIA_MODO, JlccModoVista.ESTADO.name()));
        jlccConfigurarSelectorModo();
        jlccConfigurarNavegacionTeclado();
        jlccActualizarBarraNavegacion();
        if (Platform.isFxApplicationThread()) jlccIniciarMaqueta();
        else Platform.runLater(this::jlccIniciarMaqueta);
        jlccConfigurarLeyenda();
    }

    @Override
    public void refrescar() {
        if (Platform.isFxApplicationThread()) {
            jlccReiniciarNavegacion();
            jlccMostrarCargandoEscena();
        } else {
            Platform.runLater(
                    () -> {
                        jlccReiniciarNavegacion();
                        jlccMostrarCargandoEscena();
                    });
        }
        jlccTareas.jlccEjecutar(jlccService::jlccCargar, this::jlccAplicarResultado, this::jlccMostrarError);
    }

    private void jlccIniciarMaqueta() {
        if (jlccEscenaIntentada) return;
        jlccEscenaIntentada = true;
        if (contenedor3d == null) {
            jlccEscenaIntentada = false;
            return;
        }
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            jlccMostrarAvisoSin3d();
            return;
        }

        jlccRotacionX = new Rotate(JLCC_ANGULO_X_INICIAL, Rotate.X_AXIS);
        jlccRotacionY = new Rotate(JLCC_ANGULO_Y_INICIAL, Rotate.Y_AXIS);
        jlccMundo = new Group();
        jlccMundo.getTransforms().addAll(jlccRotacionX, jlccRotacionY);

        Group jlccRaiz3d = new Group(jlccMundo);
        jlccPrepararMateriales();
        jlccContornoSeleccion = new Box(JLCC_ANCHO_UNIDAD + 3, JLCC_ALTO_UNIDAD + 3, JLCC_PROFUNDIDAD_UNIDAD + 3);
        jlccContornoSeleccion.setDrawMode(DrawMode.LINE);
        jlccContornoSeleccion.setMaterial(jlccMaterialSeleccion);
        jlccContornoSeleccion.setMouseTransparent(true);
        jlccContornoHover = new Box(JLCC_ANCHO_UNIDAD + 3, JLCC_ALTO_UNIDAD + 3, JLCC_PROFUNDIDAD_UNIDAD + 3);
        jlccContornoHover.setDrawMode(DrawMode.LINE);
        jlccContornoHover.setMaterial(jlccMaterialHover);
        jlccContornoHover.setMouseTransparent(true);
        jlccContornoSeleccionPlanta = new Box(1, JLCC_ALTO_CONTORNO_PLANTA, 1);
        jlccContornoSeleccionPlanta.setDrawMode(DrawMode.LINE);
        jlccContornoSeleccionPlanta.setMaterial(jlccMaterialSeleccion);
        jlccContornoSeleccionPlanta.setMouseTransparent(true);
        jlccContornoSeleccionPlanta.setTranslateY(-(JLCC_ALTO_CONTORNO_PLANTA / 2 - 1));
        jlccContornoHoverPlanta = new Box(1, JLCC_ALTO_CONTORNO_PLANTA, 1);
        jlccContornoHoverPlanta.setDrawMode(DrawMode.LINE);
        jlccContornoHoverPlanta.setMaterial(jlccMaterialHover);
        jlccContornoHoverPlanta.setMouseTransparent(true);
        jlccContornoHoverPlanta.setTranslateY(-(JLCC_ALTO_CONTORNO_PLANTA / 2 - 1));
        jlccAgregarIluminacion(jlccRaiz3d);
        jlccConfigurarCamara(jlccRaiz3d);
        if (lienzoPlano != null) {
            lienzoPlano.setManaged(false);
            lienzoPlano.widthProperty().bind(contenedor3d.widthProperty());
            lienzoPlano.heightProperty().bind(contenedor3d.heightProperty());
        }
        jlccConfigurarTemaSuelo();
        jlccConfigurarInteraccion();

        contenedor3d
                .widthProperty()
                .addListener((observable, anterior, actual) -> jlccActualizarEncuadreYEtiquetas());
        contenedor3d
                .heightProperty()
                .addListener((observable, anterior, actual) -> jlccActualizarEncuadreYEtiquetas());
        Platform.runLater(this::jlccActualizarEncuadreYEtiquetas);
    }

    private void jlccAplicarResultado(JlccMaquetaService.ResultadoMaqueta jlccResultado) {
        if (jlccResultado == null) return;
        if (jlccMundo == null) {
            jlccIniciarMaqueta();
            if (jlccMundo == null) return;
        }

        jlccBloquesActuales = List.copyOf(jlccResultado.bloques());
        jlccActualizarAvisoUbicacion(jlccResultado.sinUbicacion());
        jlccActualizarResumen(jlccResultado.bloques());
        if (jlccResultado.bloques().isEmpty()) {
            jlccLimpiarEscena();
            jlccMostrarMensajeEscena(
                    "Sin inmuebles ubicados",
                    "No hay unidades con nivel y orden para dibujar. "
                            + "Asigna una ubicación desde el formulario de inmuebles.");
            return;
        }

        jlccOcultarMensajeEscena();
        try {
            jlccConstruirEscena(jlccResultado.bloques());
        } catch (RuntimeException jlccError) {
            JLCC_log.error("No se pudo construir la escena 3D de la maqueta", jlccError);
            jlccLimpiarEscena();
            jlccMostrarMensajeEscena(
                    "No se pudo dibujar la maqueta",
                    "Ocurrió un error al construir la vista 3D. Intenta nuevamente.");
            jlccNotificador.jlccError("No se pudo dibujar la maqueta 3D. Intenta nuevamente.");
        }
    }

    private void jlccMostrarError(Throwable jlccError) {
        String jlccMensaje = "No se pudieron cargar los datos de la maqueta. Intenta nuevamente.";
        jlccNotificador.jlccError(jlccMensaje);
        if (contenedor3d != null && Platform.isSupported(ConditionalFeature.SCENE3D)) {
            if (jlccMundo == null) jlccIniciarMaqueta();
            jlccMostrarMensajeEscena("No se pudo actualizar la maqueta", jlccMensaje);
        }
    }

    private void jlccConfigurarCamara(Group jlccRaiz3d) {
        jlccSubEscena = new SubScene(jlccRaiz3d, 1, 1, true, SceneAntialiasing.BALANCED);
        jlccSubEscena.setManaged(false);
        jlccSubEscena.setFill(Color.TRANSPARENT);
        jlccSubEscena.widthProperty().bind(contenedor3d.widthProperty());
        jlccSubEscena.heightProperty().bind(contenedor3d.heightProperty());

        jlccCamara = new PerspectiveCamera(false);
        jlccCamara.setFieldOfView(36);
        jlccCamara.setNearClip(0.1);
        jlccCamara.setFarClip(6000);
        jlccCamara.setTranslateZ(-1200);
        jlccSubEscena.setCamera(jlccCamara);

        Rectangle jlccRecorte = new Rectangle();
        jlccRecorte.widthProperty().bind(contenedor3d.widthProperty());
        jlccRecorte.heightProperty().bind(contenedor3d.heightProperty());
        jlccRecorte.setArcWidth(32);
        jlccRecorte.setArcHeight(32);
        contenedor3d.setClip(jlccRecorte);

        contenedor3d.getChildren().add(0, jlccSubEscena);
    }

    private void jlccConfigurarTemaSuelo() {
        contenedor3d
                .sceneProperty()
                .addListener(
                        (observable, anterior, escena) -> {
                            if (escena == null) return;
                            Parent jlccRaiz = escena.getRoot();
                            jlccRaiz.getStyleClass()
                                    .addListener(
                                            (ListChangeListener<String>)
                                                    cambio -> jlccActualizarMaterialSuelo());
                            jlccActualizarMaterialSuelo();
                        });
    }

    private void jlccActualizarMaterialSuelo() {
        PhongMaterial jlccMaterial = jlccEsTemaOscuro() ? jlccMaterialSueloOscuro : jlccMaterialSueloClaro;
        if (jlccSuelo != null) jlccSuelo.setMaterial(jlccMaterial);
        for (Box jlccBase : jlccBasesPlanta) jlccBase.setMaterial(jlccMaterial);
        if (jlccNivelVista == JlccNivelVista.UNIDAD && jlccVistaPlano) jlccDibujarPlano();
    }

    private boolean jlccEsTemaOscuro() {
        return contenedor3d != null
                && contenedor3d.getScene() != null
                && contenedor3d.getScene().getRoot().getStyleClass().contains("dark");
    }

    private void jlccAgregarIluminacion(Group jlccRaiz3d) {
        AmbientLight jlccAmbiente = new AmbientLight(Color.rgb(58, 61, 68));

        PointLight jlccPrincipal = new PointLight(Color.rgb(178, 184, 194));
        jlccPrincipal.setTranslateX(-350);
        jlccPrincipal.setTranslateY(-450);
        jlccPrincipal.setTranslateZ(-800);

        PointLight jlccRelleno = new PointLight(Color.rgb(78, 86, 100));
        jlccRelleno.setTranslateX(450);
        jlccRelleno.setTranslateY(250);
        jlccRelleno.setTranslateZ(-500);

        jlccRaiz3d.getChildren().addAll(jlccAmbiente, jlccPrincipal, jlccRelleno);
    }

    private void jlccPrepararMateriales() {
        jlccMateriales.put(DISPONIBLE, jlccMaterial(JLCC_COLOR_DISPONIBLE));
        jlccMateriales.put(ALQUILADO, jlccMaterial(JLCC_COLOR_ALQUILADO));
        jlccMateriales.put(MANTENIMIENTO, jlccMaterial(JLCC_COLOR_MANTENIMIENTO));
        jlccMateriales.put(INACTIVO, jlccMaterial(JLCC_COLOR_INACTIVO));
        jlccMaterialesAtenuados.put(DISPONIBLE, jlccMaterialAtenuado(JLCC_COLOR_DISPONIBLE));
        jlccMaterialesAtenuados.put(ALQUILADO, jlccMaterialAtenuado(JLCC_COLOR_ALQUILADO));
        jlccMaterialesAtenuados.put(MANTENIMIENTO, jlccMaterialAtenuado(JLCC_COLOR_MANTENIMIENTO));
        jlccMaterialesAtenuados.put(INACTIVO, jlccMaterialAtenuado(JLCC_COLOR_INACTIVO));
        jlccMaterialesMora.put(JLCC_COLOR_MORA_MOROSO, jlccMaterial(JLCC_COLOR_MORA_MOROSO));
        jlccMaterialesMora.put(JLCC_COLOR_MORA_ATRASO, jlccMaterial(JLCC_COLOR_MORA_ATRASO));
        jlccMaterialesMoraAtenuados.put(JLCC_COLOR_MORA_MOROSO, jlccMaterialAtenuado(JLCC_COLOR_MORA_MOROSO));
        jlccMaterialesMoraAtenuados.put(JLCC_COLOR_MORA_ATRASO, jlccMaterialAtenuado(JLCC_COLOR_MORA_ATRASO));

        jlccMaterialesCategoriaMora.put(
                BloqueMaqueta.CategoriaMora.AL_DIA, jlccMaterial(JLCC_COLOR_MORA_AL_DIA));
        jlccMaterialesCategoriaMora.put(
                BloqueMaqueta.CategoriaMora.ATRASO_LEVE, jlccMaterial(JLCC_COLOR_MORA_ATRASO));
        jlccMaterialesCategoriaMora.put(
                BloqueMaqueta.CategoriaMora.MOROSO, jlccMaterial(JLCC_COLOR_MORA_MOROSO));
        jlccMaterialesCategoriaMora.put(BloqueMaqueta.CategoriaMora.SIN_MORA, jlccMaterial(JLCC_COLOR_SIN_DATO));
        jlccMaterialesCategoriaMoraAtenuados.put(
                BloqueMaqueta.CategoriaMora.AL_DIA, jlccMaterialAtenuado(JLCC_COLOR_MORA_AL_DIA));
        jlccMaterialesCategoriaMoraAtenuados.put(
                BloqueMaqueta.CategoriaMora.ATRASO_LEVE, jlccMaterialAtenuado(JLCC_COLOR_MORA_ATRASO));
        jlccMaterialesCategoriaMoraAtenuados.put(
                BloqueMaqueta.CategoriaMora.MOROSO, jlccMaterialAtenuado(JLCC_COLOR_MORA_MOROSO));
        jlccMaterialesCategoriaMoraAtenuados.put(
                BloqueMaqueta.CategoriaMora.SIN_MORA, jlccMaterialAtenuado(JLCC_COLOR_SIN_DATO));

        jlccMaterialesVencimiento.put(
                BloqueMaqueta.CategoriaVencimiento.VENCIDO, jlccMaterial(JLCC_COLOR_VENCIDO));
        jlccMaterialesVencimiento.put(
                BloqueMaqueta.CategoriaVencimiento.HASTA_30_DIAS, jlccMaterial(JLCC_COLOR_MORA_MOROSO));
        jlccMaterialesVencimiento.put(
                BloqueMaqueta.CategoriaVencimiento.DE_31_A_90_DIAS, jlccMaterial(JLCC_COLOR_MANTENIMIENTO));
        jlccMaterialesVencimiento.put(
                BloqueMaqueta.CategoriaVencimiento.MAS_DE_90_DIAS, jlccMaterial(JLCC_COLOR_DISPONIBLE));
        jlccMaterialesVencimiento.put(
                BloqueMaqueta.CategoriaVencimiento.SIN_CONTRATO, jlccMaterial(JLCC_COLOR_SIN_DATO));
        jlccMaterialesVencimientoAtenuados.put(
                BloqueMaqueta.CategoriaVencimiento.VENCIDO, jlccMaterialAtenuado(JLCC_COLOR_VENCIDO));
        jlccMaterialesVencimientoAtenuados.put(
                BloqueMaqueta.CategoriaVencimiento.HASTA_30_DIAS,
                jlccMaterialAtenuado(JLCC_COLOR_MORA_MOROSO));
        jlccMaterialesVencimientoAtenuados.put(
                BloqueMaqueta.CategoriaVencimiento.DE_31_A_90_DIAS,
                jlccMaterialAtenuado(JLCC_COLOR_MANTENIMIENTO));
        jlccMaterialesVencimientoAtenuados.put(
                BloqueMaqueta.CategoriaVencimiento.MAS_DE_90_DIAS,
                jlccMaterialAtenuado(JLCC_COLOR_DISPONIBLE));
        jlccMaterialesVencimientoAtenuados.put(
                BloqueMaqueta.CategoriaVencimiento.SIN_CONTRATO, jlccMaterialAtenuado(JLCC_COLOR_SIN_DATO));

        for (Color jlccColor : JLCC_COLORES_RENTA) {
            jlccMaterialesRenta.add(jlccMaterial(jlccColor));
            jlccMaterialesRentaAtenuados.add(jlccMaterialAtenuado(jlccColor));
        }
        jlccMaterialSinDato = jlccMaterial(JLCC_COLOR_SIN_DATO);
        jlccMaterialSinDatoAtenuado = jlccMaterialAtenuado(JLCC_COLOR_SIN_DATO);

        jlccMaterialEstructura = jlccMaterial(JLCC_COLOR_ESTRUCTURA);
        jlccMaterialPasillo = jlccMaterial(JLCC_COLOR_PASILLO);
        jlccMaterialLosa = jlccMaterial(JLCC_COLOR_LOSA);
        jlccMaterialParedEsquema = jlccMaterialParedEsquema(JLCC_COLOR_PARED_ESQUEMA);
        jlccMaterialSeleccion = jlccMaterial(JLCC_COLOR_SELECCION);
        jlccMaterialHover = jlccMaterial(JLCC_COLOR_HOVER);
        jlccMaterialSueloClaro = jlccMaterialSuelo(JLCC_COLOR_SUELO_CLARO_CENTRO, JLCC_COLOR_SUELO_CLARO_BORDE);
        jlccMaterialSueloOscuro = jlccMaterialSuelo(JLCC_COLOR_SUELO_OSCURO_CENTRO, JLCC_COLOR_SUELO_OSCURO_BORDE);

        jlccMaterialesAmbiente.put(
                JlccGeneradorEsquema.JlccTipoAmbiente.SALA_COMEDOR, jlccMaterial(JLCC_COLOR_ESQUEMA_SALA));
        jlccMaterialesAmbiente.put(
                JlccGeneradorEsquema.JlccTipoAmbiente.COCINA, jlccMaterial(JLCC_COLOR_ESQUEMA_COCINA));
        jlccMaterialesAmbiente.put(
                JlccGeneradorEsquema.JlccTipoAmbiente.DORMITORIO_PRINCIPAL,
                jlccMaterial(JLCC_COLOR_ESQUEMA_DORMITORIO_PRINCIPAL));
        jlccMaterialesAmbiente.put(
                JlccGeneradorEsquema.JlccTipoAmbiente.DORMITORIO, jlccMaterial(JLCC_COLOR_ESQUEMA_DORMITORIO));
        jlccMaterialesAmbiente.put(JlccGeneradorEsquema.JlccTipoAmbiente.BANO, jlccMaterial(JLCC_COLOR_ESQUEMA_BANO));
        jlccMaterialesAmbiente.put(
                JlccGeneradorEsquema.JlccTipoAmbiente.PASILLO, jlccMaterial(JLCC_COLOR_ESQUEMA_PASILLO));
        jlccMaterialesAmbiente.put(
                JlccGeneradorEsquema.JlccTipoAmbiente.PASILLO_AUXILIAR, jlccMaterial(JLCC_COLOR_ESQUEMA_PASILLO));
        jlccMaterialesAmbiente.put(
                JlccGeneradorEsquema.JlccTipoAmbiente.AMBIENTE, jlccMaterial(JLCC_COLOR_ESQUEMA_AMBIENTE));
        jlccMaterialesAmbiente.put(
                JlccGeneradorEsquema.JlccTipoAmbiente.AMBIENTE_AUXILIAR, jlccMaterial(JLCC_COLOR_ESQUEMA_AMBIENTE));
    }

    private static List<Color> jlccCrearPaletaRenta() {
        List<Color> jlccColores = new ArrayList<>(JlccMaquetaService.JLCC_TONOS_RENTA);
        for (int i = 0; i < JlccMaquetaService.JLCC_TONOS_RENTA; i++) {
            double jlccAvance = i / (double) (JlccMaquetaService.JLCC_TONOS_RENTA - 1);
            jlccColores.add(JLCC_COLOR_RENTA_INICIO.interpolate(JLCC_COLOR_RENTA_FIN, jlccAvance));
        }
        return List.copyOf(jlccColores);
    }

    private PhongMaterial jlccMaterial(Color jlccColor) {
        PhongMaterial jlccMaterial = new PhongMaterial(jlccColor);
        jlccMaterial.setSpecularColor(Color.rgb(178, 184, 194));
        jlccMaterial.setSpecularPower(18);
        return jlccMaterial;
    }

    private PhongMaterial jlccMaterialParedEsquema(Color jlccColor) {
        PhongMaterial jlccMaterial = jlccMaterial(jlccColor);
        WritableImage jlccEmision = new WritableImage(1, 1);
        jlccEmision.getPixelWriter().setColor(0, 0, Color.rgb(44, 48, 56));
        jlccMaterial.setSelfIlluminationMap(jlccEmision);
        return jlccMaterial;
    }

    private PhongMaterial jlccMaterialAtenuado(Color jlccColor) {
        PhongMaterial jlccMaterial =
                new PhongMaterial(
                        new Color(
                                jlccColor.getRed(), jlccColor.getGreen(), jlccColor.getBlue(), JLCC_ALFA_ATENUADO));
        jlccMaterial.setSpecularColor(Color.TRANSPARENT);
        jlccMaterial.setSpecularPower(0);
        return jlccMaterial;
    }

    private PhongMaterial jlccMaterialSuelo(Color jlccCentro, Color jlccBorde) {
        WritableImage jlccTextura = new WritableImage(96, 96);
        PixelWriter jlccPixel = jlccTextura.getPixelWriter();
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 96; x++) {
                double jlccDx = (x - 47.5) / 47.5;
                double jlccDy = (y - 47.5) / 47.5;
                double jlccDistancia = Math.min(1, Math.hypot(jlccDx, jlccDy));
                jlccPixel.setColor(x, y, jlccCentro.interpolate(jlccBorde, jlccDistancia));
            }
        }
        PhongMaterial jlccMaterial = new PhongMaterial(Color.WHITE);
        jlccMaterial.setDiffuseMap(jlccTextura);
        jlccMaterial.setSpecularColor(Color.rgb(86, 92, 102));
        jlccMaterial.setSpecularPower(8);
        return jlccMaterial;
    }

    private void jlccConstruirEscena(List<BloqueMaqueta> jlccBloques) {
        List<BloqueMaqueta> jlccLista = jlccBloques == null ? List.of() : jlccBloques;
        jlccActualizarHover(null);
        jlccLimpiarSeleccion();
        jlccDetenerTransicion();
        jlccLimpiarUnidad();
        jlccNivelVista = JlccNivelVista.EDIFICIO;
        jlccNivelEnfocado = null;
        jlccGeometria = jlccCalcularGeometria(jlccLista);
        jlccPuntosEncuadre = jlccCalcularPuntosEncuadre();

        jlccMundo.getChildren().clear();
        jlccMapaBloques.clear();
        jlccMapaPisos.clear();
        jlccGruposPiso.clear();
        jlccGruposBloques.clear();
        jlccGruposPlanta.clear();
        jlccGruposRecintos.clear();
        jlccNodosPlantaPorBloque.clear();
        jlccCajasPlantaPorBloque.clear();
        jlccMarcasPlantaPorBloque.clear();
        jlccRecintosPlantaPorBloque.clear();
        jlccDistribucionesPlanta.clear();
        jlccBasesPlanta.clear();
        jlccNodosPorBloque.clear();
        jlccCajasPorBloque.clear();
        jlccMarcasPorBloque.clear();
        jlccBloqueHover = null;
        jlccNodoHover = null;
        jlccSuelo = null;
        jlccCalcularRentas(jlccLista);

        jlccCrearEtiquetasPiso();
        jlccEstructuraGeneral = jlccCrearEstructura();
        jlccMundo.getChildren().add(jlccEstructuraGeneral);
        for (PisoGeometria jlccPiso : jlccGeometria.pisos().values()) {
            Group jlccBloqueGrupo = new Group();
            for (FilaPiso jlccFila : jlccPiso.filas()) {
                for (BloqueMaqueta jlccBloque : jlccFila.bloques()) {
                    jlccBloqueGrupo.getChildren().add(jlccCrearBloque(jlccBloque));
                }
            }
            Group jlccPisoGrupo = new Group(jlccCrearLosa(jlccPiso), jlccBloqueGrupo);
            jlccGruposPiso.put(jlccPiso.nivel(), jlccPisoGrupo);
            jlccGruposBloques.put(jlccPiso.nivel(), jlccBloqueGrupo);
            jlccMundo.getChildren().add(jlccPisoGrupo);

            List<BloqueMaqueta> jlccUnidades = new ArrayList<>();
            for (FilaPiso jlccFila : jlccPiso.filas()) jlccUnidades.addAll(jlccFila.bloques());
            JlccPlantaPiso.Distribucion jlccDistribucion = JlccPlantaPiso.jlccCalcular(jlccPiso.nivel(), jlccUnidades);
            jlccDistribucionesPlanta.put(jlccPiso.nivel(), jlccDistribucion);
            for (JlccPlantaPiso.Recinto jlccRecinto : jlccDistribucion.recintos()) {
                jlccRecintosPlantaPorBloque.put(jlccRecinto.bloque(), jlccRecinto);
            }
            Group jlccPlanta = jlccCrearPlanta(jlccPiso, jlccDistribucion);
            jlccGruposPlanta.put(jlccPiso.nivel(), jlccPlanta);
            jlccMundo.getChildren().add(jlccPlanta);
        }
        jlccCrearEtiquetasUnidad(jlccLista);
        jlccAplicarFiltroMateriales();
        jlccConfigurarLeyenda();
        jlccActualizarMaterialSuelo();
        jlccActualizarBarraNavegacion();
        jlccActualizarResumen(jlccBloquesActuales);

        Platform.runLater(this::jlccActualizarEncuadreYEtiquetas);
    }

    private void jlccCalcularRentas(List<BloqueMaqueta> jlccBloques) {
        jlccRentaMinima = null;
        jlccRentaMaxima = null;
        for (BloqueMaqueta jlccBloque : jlccBloques) {
            BigDecimal jlccRenta = jlccBloque.rentaReferencial();
            if (jlccRenta == null) continue;
            if (jlccRentaMinima == null || jlccRenta.compareTo(jlccRentaMinima) < 0) jlccRentaMinima = jlccRenta;
            if (jlccRentaMaxima == null || jlccRenta.compareTo(jlccRentaMaxima) > 0) jlccRentaMaxima = jlccRenta;
        }
    }

    static Geometria jlccCalcularGeometria(List<BloqueMaqueta> jlccBloques) {
        Map<Integer, List<BloqueMaqueta>> jlccPorNivel = new TreeMap<>();
        for (BloqueMaqueta jlccBloque : jlccBloques) {
            jlccPorNivel.computeIfAbsent(jlccBloque.nivel(), nivel -> new ArrayList<>()).add(jlccBloque);
        }
        jlccPorNivel.values()
                .forEach(
                        lista ->
                                lista.sort(
                                        Comparator.comparingInt(BloqueMaqueta::orden)
                                                .thenComparing(BloqueMaqueta::id)));

        List<Integer> jlccNiveles = List.copyOf(jlccPorNivel.keySet());
        Map<Integer, List<List<BloqueMaqueta>>> jlccFilasPorNivel = new LinkedHashMap<>();
        int jlccColumnasMax = 1;
        int jlccFilasMax = 1;
        for (Map.Entry<Integer, List<BloqueMaqueta>> jlccEntrada : jlccPorNivel.entrySet()) {
            List<List<BloqueMaqueta>> jlccFilas = jlccRepartirFilas(jlccEntrada.getValue());
            jlccFilasPorNivel.put(jlccEntrada.getKey(), jlccFilas);
            jlccColumnasMax =
                    Math.max(jlccColumnasMax, jlccFilas.stream().mapToInt(List::size).max().orElse(1));
            jlccFilasMax = Math.max(jlccFilasMax, jlccFilas.size());
        }

        double jlccAncho = jlccAnchoColumnas(jlccColumnasMax);
        double jlccProfundidad =
                jlccFilasMax * JLCC_PROFUNDIDAD_UNIDAD + Math.max(0, jlccFilasMax - 1) * JLCC_SEPARACION_FILA;
        int jlccCantidadNiveles = Math.max(1, jlccNiveles.size());
        double jlccAlto =
                jlccCantidadNiveles * JLCC_ALTO_UNIDAD + Math.max(0, jlccCantidadNiveles - 1) * JLCC_SEPARACION_PISO;

        double jlccAnchoBase = jlccAncho + 2 * JLCC_MARGEN_SUELO_X;
        double jlccProfundidadBase = jlccProfundidad + 2 * JLCC_MARGEN_SUELO_Z;
        double jlccAnchoTecho = jlccAncho + 2 * (JLCC_COLUMNA_MARGEN + JLCC_COLUMNA_ANCHO);
        double jlccProfundidadTecho = jlccProfundidad + 2 * (JLCC_COLUMNA_MARGEN + JLCC_COLUMNA_ANCHO);

        double jlccLimiteSuperior = -jlccAlto / 2 - JLCC_TECHO_ELEVACION - JLCC_TECHO_ALTO;
        double jlccLimiteInferior = jlccAlto / 2 + JLCC_BASE_ALTO;
        double jlccDesplazamientoY = -(jlccLimiteSuperior + jlccLimiteInferior) / 2;

        Map<Integer, Double> jlccYPorNivel = new HashMap<>();
        for (int i = 0; i < jlccNiveles.size(); i++) {
            double y = jlccAlto / 2 - JLCC_ALTO_UNIDAD / 2 - i * JLCC_PASO_Y + jlccDesplazamientoY;
            jlccYPorNivel.put(jlccNiveles.get(i), y);
        }

        Map<Integer, PisoGeometria> jlccPisos = new LinkedHashMap<>();
        Map<Long, Point3D> jlccPosiciones = new HashMap<>();
        for (Map.Entry<Integer, List<List<BloqueMaqueta>>> jlccEntrada : jlccFilasPorNivel.entrySet()) {
            int jlccNivel = jlccEntrada.getKey();
            List<List<BloqueMaqueta>> jlccFilas = jlccEntrada.getValue();
            double y = jlccYPorNivel.get(jlccNivel);
            int jlccColumnasPiso = jlccFilas.stream().mapToInt(List::size).max().orElse(1);
            double jlccAnchoPiso = jlccAnchoColumnas(jlccColumnasPiso);
            double jlccZInicio = -(jlccFilas.size() - 1) * JLCC_PASO_Z / 2.0;
            List<FilaPiso> jlccFilasGeometria = new ArrayList<>();
            for (int jlccFila = 0; jlccFila < jlccFilas.size(); jlccFila++) {
                List<BloqueMaqueta> jlccUnidades = jlccFilas.get(jlccFila);
                double z = jlccZInicio + jlccFila * JLCC_PASO_Z;
                double jlccXInicio = -(jlccUnidades.size() - 1) * JLCC_PASO_X / 2.0;
                for (int i = 0; i < jlccUnidades.size(); i++) {
                    double x = jlccXInicio + i * JLCC_PASO_X;
                    jlccPosiciones.put(jlccUnidades.get(i).id(), new Point3D(x, y, z));
                }
                jlccFilasGeometria.add(new FilaPiso(List.copyOf(jlccUnidades), z));
            }
            double jlccProfundidadPiso =
                    jlccFilas.size() * JLCC_PROFUNDIDAD_UNIDAD
                            + Math.max(0, jlccFilas.size() - 1) * JLCC_SEPARACION_FILA;
            jlccPisos.put(
                    jlccNivel,
                    new PisoGeometria(
                            jlccNivel, y, jlccAnchoPiso, jlccProfundidadPiso, List.copyOf(jlccFilasGeometria)));
        }

        return new Geometria(
                jlccAncho,
                jlccProfundidad,
                jlccAlto,
                jlccAnchoBase,
                jlccProfundidadBase,
                jlccAnchoTecho,
                jlccProfundidadTecho,
                jlccDesplazamientoY,
                jlccPisos,
                jlccPosiciones);
    }

    private static List<List<BloqueMaqueta>> jlccRepartirFilas(List<BloqueMaqueta> jlccUnidades) {
        int jlccCantidadFilas =
                Math.max(1, (jlccUnidades.size() + JLCC_MAX_UNIDADES_POR_FILA - 1) / JLCC_MAX_UNIDADES_POR_FILA);
        int jlccBase = jlccUnidades.size() / jlccCantidadFilas;
        int jlccResto = jlccUnidades.size() % jlccCantidadFilas;
        List<List<BloqueMaqueta>> jlccFilas = new ArrayList<>(jlccCantidadFilas);
        int jlccInicio = 0;
        for (int jlccFila = 0; jlccFila < jlccCantidadFilas; jlccFila++) {
            int jlccCantidad = jlccBase + (jlccFila < jlccResto ? 1 : 0);
            jlccFilas.add(List.copyOf(jlccUnidades.subList(jlccInicio, jlccInicio + jlccCantidad)));
            jlccInicio += jlccCantidad;
        }
        return jlccFilas;
    }

    private static double jlccAnchoColumnas(int jlccColumnas) {
        return jlccColumnas * JLCC_ANCHO_UNIDAD + Math.max(0, jlccColumnas - 1) * JLCC_SEPARACION_UNIDAD;
    }

    private List<Point3D> jlccCalcularPuntosEncuadre() {
        List<Point3D> jlccPuntos = new ArrayList<>();
        jlccAgregarPuntosCaja(
                jlccPuntos,
                0,
                jlccYBase(),
                0,
                jlccGeometria.anchoBase(),
                JLCC_BASE_ALTO,
                jlccGeometria.profundidadBase());
        double jlccDzTecho = jlccGeometria.profundidadTecho() / 2 - JLCC_COLUMNA_ANCHO / 2;
        double jlccDxTecho = jlccGeometria.anchoTecho() / 2 - JLCC_COLUMNA_ANCHO / 2;
        jlccAgregarPuntosCaja(
                jlccPuntos, 0, jlccYTecho(), -jlccDzTecho, jlccGeometria.anchoTecho(), JLCC_TECHO_ALTO, JLCC_COLUMNA_ANCHO);
        jlccAgregarPuntosCaja(
                jlccPuntos, 0, jlccYTecho(), jlccDzTecho, jlccGeometria.anchoTecho(), JLCC_TECHO_ALTO, JLCC_COLUMNA_ANCHO);
        jlccAgregarPuntosCaja(
                jlccPuntos,
                -jlccDxTecho,
                jlccYTecho(),
                0,
                JLCC_COLUMNA_ANCHO,
                JLCC_TECHO_ALTO,
                jlccGeometria.profundidadTecho());
        jlccAgregarPuntosCaja(
                jlccPuntos,
                jlccDxTecho,
                jlccYTecho(),
                0,
                JLCC_COLUMNA_ANCHO,
                JLCC_TECHO_ALTO,
                jlccGeometria.profundidadTecho());
        for (double jlccDx : new double[] {-jlccXColumna(), jlccXColumna()}) {
            for (double jlccDz : new double[] {-jlccZColumna(), jlccZColumna()}) {
                jlccAgregarPuntosCaja(
                        jlccPuntos, jlccDx, jlccYColumnas(), jlccDz, JLCC_COLUMNA_ANCHO, jlccAltoColumna(), JLCC_COLUMNA_ANCHO);
            }
        }
        for (Point3D jlccPosicion : jlccGeometria.posiciones().values()) {
            jlccAgregarPuntosCaja(
                    jlccPuntos,
                    jlccPosicion.getX(),
                    jlccPosicion.getY(),
                    jlccPosicion.getZ(),
                    JLCC_ANCHO_UNIDAD,
                    JLCC_ALTO_UNIDAD,
                    JLCC_PROFUNDIDAD_UNIDAD);
        }
        return jlccPuntos;
    }

    private static void jlccAgregarPuntosCaja(
            List<Point3D> jlccPuntos,
            double jlccCentroX,
            double jlccCentroY,
            double jlccCentroZ,
            double jlccAncho,
            double jlccAlto,
            double jlccProfundidad) {
        double jlccMitadX = jlccAncho / 2;
        double jlccMitadY = jlccAlto / 2;
        double jlccMitadZ = jlccProfundidad / 2;
        for (double jlccDx : new double[] {-jlccMitadX, jlccMitadX}) {
            for (double jlccDy : new double[] {-jlccMitadY, jlccMitadY}) {
                for (double jlccDz : new double[] {-jlccMitadZ, jlccMitadZ}) {
                    jlccPuntos.add(new Point3D(jlccCentroX + jlccDx, jlccCentroY + jlccDy, jlccCentroZ + jlccDz));
                }
            }
        }
    }

    private double jlccYBase() {
        return jlccGeometria.alto() / 2 + JLCC_BASE_ALTO / 2 + jlccGeometria.desplazamientoY();
    }

    private double jlccYTecho() {
        return -jlccGeometria.alto() / 2
                - JLCC_TECHO_ELEVACION
                - JLCC_TECHO_ALTO / 2
                + jlccGeometria.desplazamientoY();
    }

    private double jlccYColumnas() {
        return -(JLCC_TECHO_ELEVACION + JLCC_TECHO_ALTO) / 2 + jlccGeometria.desplazamientoY();
    }

    private double jlccXColumna() {
        return jlccGeometria.ancho() / 2 + JLCC_COLUMNA_MARGEN + JLCC_COLUMNA_ANCHO / 2;
    }

    private double jlccZColumna() {
        return jlccGeometria.profundidad() / 2 + JLCC_COLUMNA_MARGEN + JLCC_COLUMNA_ANCHO / 2;
    }

    private double jlccAltoColumna() {
        return jlccGeometria.alto() + JLCC_TECHO_ELEVACION + JLCC_TECHO_ALTO;
    }

    private Group jlccCrearEstructura() {
        Group jlccEstructura = new Group(jlccCrearBase(), jlccCrearTecho());
        jlccEstructura.getChildren().add(jlccCrearColumnas());
        return jlccEstructura;
    }

    private Node jlccCrearBase() {
        jlccSuelo = new Box(jlccGeometria.anchoBase(), JLCC_BASE_ALTO, jlccGeometria.profundidadBase());
        jlccSuelo.setMaterial(jlccEsTemaOscuro() ? jlccMaterialSueloOscuro : jlccMaterialSueloClaro);
        jlccSuelo.setTranslateY(jlccYBase());
        return jlccSuelo;
    }

    private Node jlccCrearLosa(PisoGeometria jlccPiso) {
        Box jlccLosa =
                new Box(
                        jlccPiso.ancho() + 2 * JLCC_LOSA_MARGEN_X,
                        JLCC_LOSA_ALTO,
                        jlccPiso.profundidad() + 2 * JLCC_LOSA_MARGEN_Z);
        jlccLosa.setMaterial(jlccMaterialLosa);
        jlccLosa.setTranslateY(jlccPiso.y() + JLCC_ALTO_UNIDAD / 2 + JLCC_LOSA_ALTO / 2);
        jlccLosa.setCursor(Cursor.HAND);
        jlccMapaPisos.put(jlccLosa, jlccPiso.nivel());
        return jlccLosa;
    }

    private Node jlccCrearTecho() {
        Group jlccTecho = new Group();
        double jlccDz = jlccGeometria.profundidadTecho() / 2 - JLCC_COLUMNA_ANCHO / 2;
        double jlccDx = jlccGeometria.anchoTecho() / 2 - JLCC_COLUMNA_ANCHO / 2;
        jlccTecho.getChildren()
                .addAll(
                        jlccViga(jlccGeometria.anchoTecho(), JLCC_TECHO_ALTO, JLCC_COLUMNA_ANCHO, 0, jlccYTecho(), -jlccDz),
                        jlccViga(jlccGeometria.anchoTecho(), JLCC_TECHO_ALTO, JLCC_COLUMNA_ANCHO, 0, jlccYTecho(), jlccDz),
                        jlccViga(
                                JLCC_COLUMNA_ANCHO,
                                JLCC_TECHO_ALTO,
                                jlccGeometria.profundidadTecho(),
                                -jlccDx,
                                jlccYTecho(),
                                0),
                        jlccViga(
                                JLCC_COLUMNA_ANCHO,
                                JLCC_TECHO_ALTO,
                                jlccGeometria.profundidadTecho(),
                                jlccDx,
                                jlccYTecho(),
                                0));
        return jlccTecho;
    }

    private Box jlccViga(double jlccAncho, double jlccAlto, double jlccProfundidad, double x, double y, double z) {
        Box jlccViga = new Box(jlccAncho, jlccAlto, jlccProfundidad);
        jlccViga.setMaterial(jlccMaterialEstructura);
        jlccViga.setTranslateX(x);
        jlccViga.setTranslateY(y);
        jlccViga.setTranslateZ(z);
        return jlccViga;
    }

    private Node jlccCrearColumnas() {
        Group jlccColumnas = new Group();
        for (double jlccDx : new double[] {-jlccXColumna(), jlccXColumna()}) {
            for (double jlccDz : new double[] {-jlccZColumna(), jlccZColumna()}) {
                Box jlccColumna = new Box(JLCC_COLUMNA_ANCHO, jlccAltoColumna(), JLCC_COLUMNA_ANCHO);
                jlccColumna.setMaterial(jlccMaterialEstructura);
                jlccColumna.setTranslateX(jlccDx);
                jlccColumna.setTranslateY(jlccYColumnas());
                jlccColumna.setTranslateZ(jlccDz);
                jlccColumnas.getChildren().add(jlccColumna);
            }
        }
        return jlccColumnas;
    }

    private void jlccCrearEtiquetasPiso() {
        capaEtiquetas.getChildren().clear();
        jlccEtiquetasPiso.clear();
        for (PisoGeometria jlccPiso : jlccGeometria.pisos().values()) {
            Label jlccEtiqueta = new Label(jlccEtiquetaNivel(jlccPiso.nivel()));
            jlccEtiqueta.getStyleClass().add("maqueta-piso-etiqueta");
            jlccEtiqueta.setMouseTransparent(false);
            jlccEtiqueta.setCursor(Cursor.HAND);
            jlccEtiqueta.setVisible(false);
            int jlccNivel = jlccPiso.nivel();
            jlccEtiqueta.setOnMouseClicked(
                    evento -> {
                        if (evento.getButton() != MouseButton.PRIMARY) return;
                        jlccEntrarPiso(jlccNivel);
                        evento.consume();
                    });
            capaEtiquetas.getChildren().add(jlccEtiqueta);

            Point3D jlccAncla = new Point3D(-jlccXColumna(), jlccPiso.y(), jlccPiso.filas().get(0).z());
            jlccEtiquetasPiso.add(new EtiquetaPiso(jlccNivel, jlccEtiqueta, jlccAncla));
        }
    }

    private void jlccCrearEtiquetasUnidad(List<BloqueMaqueta> jlccBloques) {
        jlccEtiquetasUnidad.clear();
        for (BloqueMaqueta jlccBloque : jlccBloques) {
            Point3D jlccPosicion = jlccGeometria.posiciones().get(jlccBloque.id());
            if (jlccPosicion == null) continue;
            Label jlccEtiqueta = new Label(jlccNumeroUnidad(jlccBloque));
            jlccEtiqueta.getStyleClass().add("maqueta-unidad-etiqueta");
            jlccEtiqueta.setMouseTransparent(true);
            jlccEtiqueta.setVisible(false);
            capaEtiquetas.getChildren().add(jlccEtiqueta);
            Point3D jlccAncla =
                    new Point3D(
                            jlccPosicion.getX(),
                            jlccPosicion.getY() - JLCC_ALTO_UNIDAD / 2,
                            jlccPosicion.getZ() - JLCC_PROFUNDIDAD_UNIDAD / 2);
            JlccPlantaPiso.Recinto jlccRecinto = jlccRecintosPlantaPorBloque.get(jlccBloque);
            PisoGeometria jlccPiso = jlccGeometria.pisos().get(jlccBloque.nivel());
            Point3D jlccAnclaPlanta =
                    jlccRecinto == null || jlccPiso == null
                            ? jlccAncla
                            : new Point3D(
                                    jlccRecinto.x(), jlccPiso.y() - JLCC_ALTO_CONTORNO_PLANTA, jlccRecinto.z());
            jlccEtiquetasUnidad.add(new EtiquetaUnidad(jlccBloque, jlccEtiqueta, jlccAncla, jlccAnclaPlanta));
        }
    }

    static String jlccNumeroUnidad(BloqueMaqueta jlccBloque) {
        String jlccTexto =
                jlccBloque.nombre() == null || jlccBloque.nombre().isBlank()
                        ? jlccBloque.codigo()
                        : jlccBloque.nombre();
        if (jlccTexto != null) {
            Matcher jlccMatcher = JLCC_NUMERO_UNIDAD.matcher(jlccTexto);
            if (jlccMatcher.find()) return jlccMatcher.group(1);
        }
        return jlccBloque.nivel() + "-" + jlccBloque.orden();
    }

    private Node jlccCrearBloque(BloqueMaqueta jlccBloque) {
        Point3D jlccPosicion = jlccGeometria.posiciones().get(jlccBloque.id());
        if (jlccPosicion == null) return new Group();

        Box jlccCaja = new Box(JLCC_ANCHO_UNIDAD, JLCC_ALTO_UNIDAD, JLCC_PROFUNDIDAD_UNIDAD);
        jlccCaja.setMaterial(jlccMaterialPara(jlccBloque.estado()));
        jlccCaja.setTranslateX(jlccPosicion.getX());
        jlccCaja.setTranslateY(jlccPosicion.getY());
        jlccCaja.setTranslateZ(jlccPosicion.getZ());
        jlccCajasPorBloque.put(jlccBloque, jlccCaja);

        Group jlccGrupo = new Group(jlccCaja);

        if (jlccBloque.estado() == ALQUILADO) {
            Color jlccColorMora = jlccColorMora(jlccBloque.semaforo());
            if (jlccColorMora != null) {
                Cylinder jlccMarca = new Cylinder(JLCC_RADIO_MARCA, JLCC_ALTO_MARCA, 24);
                jlccMarca.setMaterial(jlccMaterialMora(jlccColorMora));
                jlccMarca.setTranslateX(jlccPosicion.getX());
                jlccMarca.setTranslateY(jlccPosicion.getY() - JLCC_ALTO_UNIDAD / 2 + JLCC_RADIO_MARCA + 4);
                jlccMarca.setTranslateZ(jlccPosicion.getZ() - JLCC_PROFUNDIDAD_UNIDAD / 2 - JLCC_ALTO_MARCA / 2);
                jlccMarca.setRotationAxis(Rotate.X_AXIS);
                jlccMarca.setRotate(90);
                jlccGrupo.getChildren().add(jlccMarca);
                jlccMarcasPorBloque.put(jlccBloque, jlccMarca);
                jlccMapaBloques.put(jlccMarca, jlccBloque);
            }
        }

        jlccMapaBloques.put(jlccGrupo, jlccBloque);
        jlccMapaBloques.put(jlccCaja, jlccBloque);
        jlccNodosPorBloque.put(jlccBloque, jlccGrupo);
        return jlccGrupo;
    }

    private Group jlccCrearPlanta(PisoGeometria jlccPiso, JlccPlantaPiso.Distribucion jlccDistribucion) {
        Group jlccRecintos = new Group();
        for (JlccPlantaPiso.Recinto jlccRecinto : jlccDistribucion.recintos()) {
            jlccRecintos.getChildren().add(jlccCrearRecintoPlanta(jlccPiso, jlccRecinto));
        }
        jlccGruposRecintos.put(jlccPiso.nivel(), jlccRecintos);

        Box jlccBase =
                new Box(
                        jlccDistribucion.anchoConMargen(),
                        JLCC_ALTO_PLACA_PLANTA,
                        jlccDistribucion.profundidadConMargen());
        jlccBase.setMaterial(jlccEsTemaOscuro() ? jlccMaterialSueloOscuro : jlccMaterialSueloClaro);
        jlccBase.setTranslateY(jlccPiso.y() + JLCC_ALTO_PLACA_PLANTA / 2 + 1);
        jlccBasesPlanta.add(jlccBase);

        Box jlccPasillo = new Box(jlccDistribucion.ancho(), 1.2, jlccDistribucion.pasilloAncho());
        jlccPasillo.setMaterial(jlccMaterialPasillo);
        jlccPasillo.setTranslateY(jlccPiso.y() + 0.4);
        jlccPasillo.setTranslateZ(jlccDistribucion.pasilloZ());

        Group jlccPlanta = new Group(jlccBase, jlccPasillo, jlccRecintos);
        jlccPlanta.setVisible(false);
        return jlccPlanta;
    }

    private Group jlccCrearRecintoPlanta(PisoGeometria jlccPiso, JlccPlantaPiso.Recinto jlccRecinto) {
        BloqueMaqueta jlccBloque = jlccRecinto.bloque();
        double jlccAncho = jlccRecinto.ancho();
        double jlccProfundidad = jlccRecinto.profundidad();
        double jlccMitadGrosor = JLCC_GROSOR_PARED_PLANTA / 2;
        double jlccYPared = -(JLCC_ALTO_PLACA_PLANTA / 2 + JLCC_ALTO_PARED_PLANTA / 2);

        Box jlccPlaca = new Box(jlccAncho, JLCC_ALTO_PLACA_PLANTA, jlccProfundidad);
        jlccPlaca.setMaterial(jlccMaterialPara(jlccBloque.estado()));
        jlccCajasPlantaPorBloque.put(jlccBloque, jlccPlaca);

        Box jlccParedFrente = jlccParedPlanta(jlccAncho, jlccYPared, jlccProfundidad / 2 - jlccMitadGrosor);
        Box jlccParedFondo = jlccParedPlanta(jlccAncho, jlccYPared, -jlccProfundidad / 2 + jlccMitadGrosor);
        Box jlccParedIzquierda = new Box(JLCC_GROSOR_PARED_PLANTA, JLCC_ALTO_PARED_PLANTA, jlccProfundidad);
        jlccParedIzquierda.setMaterial(jlccMaterialEstructura);
        jlccParedIzquierda.setTranslateX(-jlccAncho / 2 + jlccMitadGrosor);
        jlccParedIzquierda.setTranslateY(jlccYPared);
        Box jlccParedDerecha = new Box(JLCC_GROSOR_PARED_PLANTA, JLCC_ALTO_PARED_PLANTA, jlccProfundidad);
        jlccParedDerecha.setMaterial(jlccMaterialEstructura);
        jlccParedDerecha.setTranslateX(jlccAncho / 2 - jlccMitadGrosor);
        jlccParedDerecha.setTranslateY(jlccYPared);

        Group jlccGrupo = new Group(jlccPlaca, jlccParedFrente, jlccParedFondo, jlccParedIzquierda, jlccParedDerecha);

        if (jlccBloque.estado() == ALQUILADO) {
            Color jlccColorMora = jlccColorMora(jlccBloque.semaforo());
            if (jlccColorMora != null) {
                Cylinder jlccMarca = new Cylinder(JLCC_RADIO_MARCA_PLANTA, JLCC_ALTO_MARCA_PLANTA, 20);
                jlccMarca.setMaterial(jlccMaterialMora(jlccColorMora));
                jlccMarca.setTranslateY(-(JLCC_ALTO_PLACA_PLANTA / 2 + JLCC_ALTO_MARCA_PLANTA / 2));
                jlccGrupo.getChildren().add(jlccMarca);
                jlccMarcasPlantaPorBloque.put(jlccBloque, jlccMarca);
            }
        }

        jlccGrupo.setTranslateX(jlccRecinto.x());
        jlccGrupo.setTranslateY(jlccPiso.y());
        jlccGrupo.setTranslateZ(jlccRecinto.z());

        jlccMapaBloques.put(jlccGrupo, jlccBloque);
        for (Node jlccNodo : jlccGrupo.getChildren()) jlccMapaBloques.put(jlccNodo, jlccBloque);
        jlccNodosPlantaPorBloque.put(jlccBloque, jlccGrupo);
        return jlccGrupo;
    }

    private Box jlccParedPlanta(double jlccAncho, double y, double z) {
        Box jlccPared = new Box(jlccAncho, JLCC_ALTO_PARED_PLANTA, JLCC_GROSOR_PARED_PLANTA);
        jlccPared.setMaterial(jlccMaterialEstructura);
        jlccPared.setTranslateY(y);
        jlccPared.setTranslateZ(z);
        return jlccPared;
    }

    private void jlccCrearGrupoUnidad(BloqueMaqueta jlccBloque, JlccGeneradorEsquema.Esquema jlccEsquema) {
        jlccLimpiarGrupoUnidad();
        Group jlccGrupo = new Group();
        double jlccCentroX = jlccEsquema.ancho() / 2;
        double jlccCentroY = jlccEsquema.alto() / 2;
        double jlccYPared = -(JLCC_ALTO_PLACA_ESQUEMA / 2 + JLCC_ALTO_PARED_ESQUEMA / 2);
        double jlccYEtiqueta = -JLCC_ALTO_PARED_ESQUEMA - 6;

        for (JlccGeneradorEsquema.Recinto jlccRecinto : jlccEsquema.recintos()) {
            if (jlccRecinto.ancho() <= 0 || jlccRecinto.alto() <= 0) continue;
            double jlccAncho = jlccRecinto.ancho() * JLCC_ESCALA_ESQUEMA;
            double jlccProfundidad = jlccRecinto.alto() * JLCC_ESCALA_ESQUEMA;
            double x = (jlccRecinto.x() + jlccRecinto.ancho() / 2 - jlccCentroX) * JLCC_ESCALA_ESQUEMA;
            double z = (jlccRecinto.y() + jlccRecinto.alto() / 2 - jlccCentroY) * JLCC_ESCALA_ESQUEMA;

            Box jlccPlaca = new Box(jlccAncho, JLCC_ALTO_PLACA_ESQUEMA, jlccProfundidad);
            jlccPlaca.setMaterial(jlccMaterialAmbiente(jlccRecinto.tipo()));
            jlccPlaca.setTranslateX(x);
            jlccPlaca.setTranslateZ(z);
            jlccGrupo.getChildren().add(jlccPlaca);
            jlccGrupo.getChildren().addAll(jlccParedesEsquema(jlccAncho, jlccProfundidad, x, z, jlccYPared));

            Label jlccEtiqueta = new Label(jlccTextoAmbiente(jlccBloque, jlccRecinto));
            jlccEtiqueta.getStyleClass().add("maqueta-ambiente-etiqueta");
            jlccEtiqueta.setMouseTransparent(true);
            jlccEtiqueta.setVisible(false);
            capaEtiquetas.getChildren().add(jlccEtiqueta);
            jlccEtiquetasAmbiente.add(new EtiquetaAmbiente(jlccEtiqueta, new Point3D(x, jlccYEtiqueta, z)));
        }

        jlccGrupoUnidad = jlccGrupo;
        jlccGrupo.setVisible(true);
        jlccMundo.getChildren().add(jlccGrupo);
    }

    private List<Box> jlccParedesEsquema(
            double jlccAncho, double jlccProfundidad, double x, double z, double y) {
        double jlccGrosor = JLCC_GROSOR_PARED_ESQUEMA;
        Box jlccFrente = new Box(jlccAncho, JLCC_ALTO_PARED_ESQUEMA, jlccGrosor);
        jlccFrente.setTranslateX(x);
        jlccFrente.setTranslateY(y);
        jlccFrente.setTranslateZ(z + jlccProfundidad / 2 - jlccGrosor / 2);

        Box jlccFondo = new Box(jlccAncho, JLCC_ALTO_PARED_ESQUEMA, jlccGrosor);
        jlccFondo.setTranslateX(x);
        jlccFondo.setTranslateY(y);
        jlccFondo.setTranslateZ(z - jlccProfundidad / 2 + jlccGrosor / 2);

        Box jlccDerecha = new Box(jlccGrosor, JLCC_ALTO_PARED_ESQUEMA, jlccProfundidad);
        jlccDerecha.setTranslateX(x + jlccAncho / 2 - jlccGrosor / 2);
        jlccDerecha.setTranslateY(y);
        jlccDerecha.setTranslateZ(z);

        Box jlccIzquierda = new Box(jlccGrosor, JLCC_ALTO_PARED_ESQUEMA, jlccProfundidad);
        jlccIzquierda.setTranslateX(x - jlccAncho / 2 + jlccGrosor / 2);
        jlccIzquierda.setTranslateY(y);
        jlccIzquierda.setTranslateZ(z);

        for (Box jlccPared : List.of(jlccFrente, jlccFondo, jlccDerecha, jlccIzquierda)) {
            jlccPared.setMaterial(jlccMaterialParedEsquema);
        }
        return List.of(jlccFrente, jlccFondo, jlccDerecha, jlccIzquierda);
    }

    private PhongMaterial jlccMaterialAmbiente(JlccGeneradorEsquema.JlccTipoAmbiente jlccTipo) {
        PhongMaterial jlccMaterial = jlccMaterialesAmbiente.get(jlccTipo);
        return jlccMaterial == null ? jlccMaterialSinDato : jlccMaterial;
    }

    private String jlccTextoAmbiente(BloqueMaqueta jlccBloque, JlccGeneradorEsquema.Recinto jlccRecinto) {
        return jlccNombreAmbiente(jlccBloque, jlccRecinto.tipo()) + " · " + jlccFormatearArea(jlccRecinto.area());
    }

    private String jlccNombreAmbiente(BloqueMaqueta jlccBloque, JlccGeneradorEsquema.JlccTipoAmbiente jlccTipo) {
        if (jlccTipo == JlccGeneradorEsquema.JlccTipoAmbiente.AMBIENTE
                || jlccTipo == JlccGeneradorEsquema.JlccTipoAmbiente.AMBIENTE_AUXILIAR) {
            String jlccTipoInmueble = jlccBloque == null ? null : jlccBloque.tipo();
            return jlccTipoInmueble == null || jlccTipoInmueble.isBlank() ? "Ambiente" : jlccTipoInmueble;
        }
        return jlccTipo.jlccGetEtiqueta();
    }

    private String jlccFormatearArea(double jlccArea) {
        return String.format(Locale.US, "%.1f m²", jlccArea);
    }

    private void jlccMostrarPanelUnidad(BloqueMaqueta jlccBloque) {
        if (panelUnidad != null) {
            panelUnidad.setVisible(true);
            panelUnidad.setManaged(true);
        }
        if (avisosUnidad != null) {
            boolean jlccHayAvisos = jlccEsquemaActual != null && !jlccEsquemaActual.avisos().isEmpty();
            avisosUnidad.setText(jlccHayAvisos ? String.join(" · ", jlccEsquemaActual.avisos()) : "");
            avisosUnidad.setVisible(jlccHayAvisos);
            avisosUnidad.setManaged(jlccHayAvisos);
        }
        jlccActualizarBotonesVista();
        jlccCargarDetallePanel(jlccBloque);
    }

    private void jlccCargarDetallePanel(BloqueMaqueta jlccBloque) {
        if (cuerpoPanelUnidad == null) return;
        jlccMostrarCargandoDetalle(cuerpoPanelUnidad);
        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccDetalle(jlccBloque.id()),
                detalle -> {
                    if (!jlccEsUnidadActual(jlccBloque)) return;
                    if (detalle == null) {
                        jlccMostrarErrorPanel();
                        return;
                    }
                    jlccDetalleUnidad = detalle;
                    cuerpoPanelUnidad.getChildren().setAll(jlccCrearContenidoDetalle(detalle, true));
                    cuerpoPanelUnidad.requestLayout();
                },
                error -> {
                    if (!jlccEsUnidadActual(jlccBloque)) return;
                    jlccNotificador.jlccError("No se pudo cargar el detalle de la unidad.");
                    jlccMostrarErrorPanel();
                });
    }

    private void jlccMostrarErrorPanel() {
        if (cuerpoPanelUnidad == null) return;
        Label jlccMensaje = new Label("No se pudo cargar el detalle. Intenta nuevamente.");
        jlccMensaje.getStyleClass().add("campo-error");
        jlccMensaje.setWrapText(true);
        cuerpoPanelUnidad.getChildren().setAll(jlccMensaje);
    }

    private boolean jlccEsUnidadActual(BloqueMaqueta jlccBloque) {
        return jlccNivelVista == JlccNivelVista.UNIDAD
                && jlccBloqueUnidad != null
                && jlccBloque != null
                && jlccBloqueUnidad.id().equals(jlccBloque.id());
    }

    @FXML
    private void jlccOnVerEnInmueblesUnidad() {
        if (jlccPrincipal != null) jlccPrincipal.jlccIrA("inmuebles");
    }

    @FXML
    private void jlccOnVistaPlano() {
        if (jlccNivelVista == JlccNivelVista.UNIDAD && !jlccTransicionActiva()) {
            jlccCambiarVistaUnidad(true);
        }
    }

    @FXML
    private void jlccOnVistaTresD() {
        if (jlccNivelVista == JlccNivelVista.UNIDAD && !jlccTransicionActiva()) {
            jlccCambiarVistaUnidad(false);
        }
    }

    private void jlccCambiarVistaUnidad(boolean jlccPlano) {
        if (jlccNivelVista != JlccNivelVista.UNIDAD
                || jlccPlano == jlccVistaPlano
                || jlccTransicionVista != null
                || jlccTransicionActiva()) {
            return;
        }
        if (veloVista == null) {
            jlccVistaPlano = jlccPlano;
            jlccAplicarVistaUnidad();
            return;
        }

        veloVista.setVisible(true);
        veloVista.setOpacity(0);
        Timeline jlccSubida =
                new Timeline(
                        new KeyFrame(
                                JLCC_DURACION_FUNDIDO,
                                new KeyValue(
                                        veloVista.opacityProperty(), 1, Interpolator.EASE_BOTH)));
        jlccSubida.setOnFinished(
                evento -> {
                    if (jlccTransicionVista != jlccSubida) return;
                    jlccVistaPlano = jlccPlano;
                    jlccAplicarVistaUnidad();
                    Timeline jlccBajada =
                            new Timeline(
                                    new KeyFrame(
                                            JLCC_DURACION_FUNDIDO,
                                            new KeyValue(
                                                    veloVista.opacityProperty(),
                                                    0,
                                                    Interpolator.EASE_BOTH)));
                    jlccBajada.setOnFinished(
                            fin -> {
                                if (jlccTransicionVista != jlccBajada) return;
                                jlccTransicionVista = null;
                                veloVista.setVisible(false);
                            });
                    jlccTransicionVista = jlccBajada;
                    jlccBajada.play();
                });
        jlccTransicionVista = jlccSubida;
        jlccSubida.play();
    }

    private void jlccAplicarVistaUnidad() {
        if (jlccNivelVista != JlccNivelVista.UNIDAD) return;
        if (jlccGrupoUnidad != null) jlccGrupoUnidad.setVisible(!jlccVistaPlano);
        if (lienzoPlano != null) {
            lienzoPlano.setVisible(jlccVistaPlano);
            if (jlccVistaPlano) jlccDibujarPlano();
            else jlccLimpiarLienzo();
        }
        if (jlccVistaPlano) jlccOcultarEtiquetasAmbiente();
        else jlccActualizarEtiquetasAmbiente();
        jlccActualizarBotonesVista();
        jlccActualizarTextoAyuda();
    }

    private void jlccActualizarBotonesVista() {
        if (btnPlano != null) btnPlano.setSelected(jlccVistaPlano);
        if (btnTresD != null) btnTresD.setSelected(!jlccVistaPlano);
    }

    private void jlccDetenerTransicionVista() {
        if (jlccTransicionVista != null) {
            jlccTransicionVista.stop();
            jlccTransicionVista = null;
        }
        if (veloVista != null) {
            veloVista.setOpacity(0);
            veloVista.setVisible(false);
        }
    }

    private void jlccDibujarPlano() {
        if (lienzoPlano == null) return;
        GraphicsContext jlccGc = lienzoPlano.getGraphicsContext2D();
        double jlccAncho = lienzoPlano.getWidth();
        double jlccAlto = lienzoPlano.getHeight();
        jlccGc.clearRect(0, 0, jlccAncho, jlccAlto);
        if (jlccNivelVista != JlccNivelVista.UNIDAD
                || !jlccVistaPlano
                || jlccEsquemaActual == null
                || jlccAncho <= 0
                || jlccAlto <= 0) {
            return;
        }

        double jlccAnchoDisponible = jlccAnchoEncuadreDisponible();
        double jlccMargenLateral = 42;
        double jlccMargenSuperior = 78;
        double jlccMargenInferior = 40;
        double jlccDisponibleAncho = Math.max(80, jlccAnchoDisponible - 2 * jlccMargenLateral);
        double jlccDisponibleAlto = Math.max(80, jlccAlto - jlccMargenSuperior - jlccMargenInferior);
        double jlccEscala =
                Math.min(
                                jlccDisponibleAncho / jlccEsquemaActual.ancho(),
                                jlccDisponibleAlto / jlccEsquemaActual.alto())
                        * jlccZoomPlano;
        if (!Double.isFinite(jlccEscala) || jlccEscala <= 0) return;

        double jlccOx = (jlccAnchoDisponible - jlccEsquemaActual.ancho() * jlccEscala) / 2;
        double jlccOy = jlccMargenSuperior + (jlccDisponibleAlto - jlccEsquemaActual.alto() * jlccEscala) / 2;
        boolean jlccOscuro = jlccEsTemaOscuro();
        Color jlccLinea = jlccOscuro ? JLCC_COLOR_LINEA_OSCURA : JLCC_COLOR_LINEA_CLARA;

        jlccGc.save();
        jlccGc.beginPath();
        jlccGc.rect(0, 0, jlccAnchoDisponible, jlccAlto);
        jlccGc.clip();

        for (JlccGeneradorEsquema.Recinto jlccRecinto : jlccEsquemaActual.recintos()) {
            double x = jlccOx + jlccRecinto.x() * jlccEscala;
            double y = jlccOy + (jlccEsquemaActual.alto() - jlccRecinto.y() - jlccRecinto.alto()) * jlccEscala;
            double w = jlccRecinto.ancho() * jlccEscala;
            double h = jlccRecinto.alto() * jlccEscala;
            if (w <= 0 || h <= 0) continue;

            Color jlccBase = jlccColorAmbiente(jlccRecinto.tipo());
            jlccGc.setFill(jlccBase.deriveColor(0, 1, 1, jlccOscuro ? 0.26 : 0.32));
            jlccGc.fillRect(x, y, w, h);
            jlccGc.setStroke(jlccLinea);
            jlccGc.setLineWidth(2.2);
            jlccGc.strokeRect(x, y, w, h);

            if (w < 42 || h < 22) continue;
            double jlccCentroX = x + w / 2;
            double jlccCentroY = y + h / 2;
            jlccGc.setTextAlign(TextAlignment.CENTER);
            jlccGc.setTextBaseline(VPos.CENTER);
            jlccGc.setFill(jlccLinea);
            double jlccTamano = Math.max(8, Math.min(13, jlccEscala * 0.26));
            jlccGc.setFont(Font.font("System", FontWeight.SEMI_BOLD, jlccTamano));
            jlccGc.fillText(
                    jlccNombreAmbiente(jlccBloqueUnidad, jlccRecinto.tipo()), jlccCentroX, jlccCentroY - jlccTamano * 0.55);
            jlccGc.setFont(Font.font("System", FontWeight.NORMAL, Math.max(8, jlccTamano - 2)));
            jlccGc.fillText(jlccFormatearArea(jlccRecinto.area()), jlccCentroX, jlccCentroY + jlccTamano * 0.6);
        }

        jlccGc.setStroke(jlccLinea);
        jlccGc.setLineWidth(3);
        jlccGc.strokeRect(jlccOx, jlccOy, jlccEsquemaActual.ancho() * jlccEscala, jlccEsquemaActual.alto() * jlccEscala);

        JlccGeneradorEsquema.Puerta jlccPuerta = jlccEsquemaActual.puerta();
        if (jlccPuerta != null) {
            double jlccPx = jlccOx + jlccPuerta.x() * jlccEscala;
            double jlccPy = jlccOy + jlccEsquemaActual.alto() * jlccEscala;
            double jlccPw = Math.max(18, jlccPuerta.ancho() * jlccEscala);
            jlccGc.clearRect(jlccPx - jlccPw / 2 - 1, jlccPy - 2, jlccPw + 2, 5);
            jlccGc.setStroke(jlccLinea);
            jlccGc.setLineWidth(2);
            jlccGc.strokeLine(jlccPx - jlccPw / 2, jlccPy, jlccPx - jlccPw / 2, jlccPy - jlccPw);
            jlccGc.strokeArc(jlccPx - jlccPw / 2, jlccPy - jlccPw, jlccPw * 2, jlccPw * 2, 0, 90, ArcType.OPEN);
        }

        jlccGc.setTextAlign(TextAlignment.LEFT);
        jlccGc.setTextBaseline(VPos.BASELINE);
        jlccGc.setFill(jlccLinea);
        jlccGc.setFont(Font.font("System", FontWeight.NORMAL, 10.5));
        String jlccMedida =
                String.format(
                        Locale.US, "%.1f × %.1f m", jlccEsquemaActual.ancho(), jlccEsquemaActual.alto());
        if (jlccEsquemaActual.areaEstimada()) jlccMedida += " · área estimada";
        if (jlccEsquemaActual.ajustado()) jlccMedida += " · distribución ajustada";
        jlccGc.fillText(jlccMedida, jlccOx, Math.max(12, jlccOy - 12));
        jlccGc.restore();
    }

    private void jlccLimpiarLienzo() {
        if (lienzoPlano == null) return;
        GraphicsContext jlccGc = lienzoPlano.getGraphicsContext2D();
        jlccGc.clearRect(0, 0, lienzoPlano.getWidth(), lienzoPlano.getHeight());
    }

    private Color jlccColorAmbiente(JlccGeneradorEsquema.JlccTipoAmbiente jlccTipo) {
        return switch (jlccTipo) {
            case SALA_COMEDOR -> JLCC_COLOR_ESQUEMA_SALA;
            case COCINA -> JLCC_COLOR_ESQUEMA_COCINA;
            case DORMITORIO_PRINCIPAL -> JLCC_COLOR_ESQUEMA_DORMITORIO_PRINCIPAL;
            case DORMITORIO -> JLCC_COLOR_ESQUEMA_DORMITORIO;
            case BANO -> JLCC_COLOR_ESQUEMA_BANO;
            case PASILLO -> JLCC_COLOR_ESQUEMA_PASILLO;
            case PASILLO_AUXILIAR -> JLCC_COLOR_ESQUEMA_PASILLO;
            case AMBIENTE, AMBIENTE_AUXILIAR -> JLCC_COLOR_ESQUEMA_AMBIENTE;
        };
    }

    private PhongMaterial jlccMaterialPara(EstadoInmueble jlccEstado) {
        return jlccMateriales.get(jlccEstado);
    }

    private PhongMaterial jlccMaterialAtenuadoPara(EstadoInmueble jlccEstado) {
        return jlccMaterialesAtenuados.get(jlccEstado);
    }

    private PhongMaterial jlccMaterialMora(Color jlccColor) {
        return jlccMaterialesMora.get(jlccColor);
    }

    private PhongMaterial jlccMaterialMoraAtenuado(Color jlccColor) {
        return jlccMaterialesMoraAtenuados.get(jlccColor);
    }

    private Color jlccColorMora(SemaforoMorosidad jlccSemaforo) {
        if (jlccSemaforo == null) return null;
        return switch (jlccSemaforo) {
            case MOROSO -> JLCC_COLOR_MORA_MOROSO;
            case ATRASO_LEVE -> JLCC_COLOR_MORA_ATRASO;
            case AL_DIA -> null;
        };
    }

    private void jlccSeleccionar(BloqueMaqueta jlccBloque) {
        jlccLimpiarSeleccion();
        if (jlccBloque == null || jlccGeometria == null) return;
        if (jlccNivelVista == JlccNivelVista.PISO) {
            Node jlccNodo = jlccNodosPlantaPorBloque.get(jlccBloque);
            JlccPlantaPiso.Recinto jlccRecinto = jlccRecintosPlantaPorBloque.get(jlccBloque);
            if (!(jlccNodo instanceof Group jlccGrupo)
                    || jlccRecinto == null
                    || jlccContornoSeleccionPlanta == null) {
                return;
            }
            jlccAjustarContornoPlanta(jlccContornoSeleccionPlanta, jlccRecinto);
            jlccGrupo.getChildren().add(jlccContornoSeleccionPlanta);
            return;
        }
        if (jlccContornoSeleccion == null) return;
        Node jlccNodo = jlccNodosPorBloque.get(jlccBloque);
        Point3D jlccPosicion = jlccGeometria.posiciones().get(jlccBloque.id());
        if (!(jlccNodo instanceof Group jlccGrupo) || jlccPosicion == null) return;
        jlccContornoSeleccion.setTranslateX(jlccPosicion.getX());
        jlccContornoSeleccion.setTranslateY(jlccPosicion.getY());
        jlccContornoSeleccion.setTranslateZ(jlccPosicion.getZ());
        jlccGrupo.getChildren().add(jlccContornoSeleccion);
    }

    private void jlccLimpiarSeleccion() {
        if (jlccContornoSeleccion != null && jlccContornoSeleccion.getParent() instanceof Group jlccGrupo) {
            jlccGrupo.getChildren().remove(jlccContornoSeleccion);
        }
        if (jlccContornoSeleccionPlanta != null
                && jlccContornoSeleccionPlanta.getParent() instanceof Group jlccGrupo) {
            jlccGrupo.getChildren().remove(jlccContornoSeleccionPlanta);
        }
    }

    private void jlccAbrirDetalle(BloqueMaqueta jlccBloque) {
        if (jlccBloque == null) return;
        jlccSeleccionar(jlccBloque);

        Dialog<ButtonType> jlccDialogo = new Dialog<>();
        jlccDialogo.setTitle("Detalle del inmueble");
        jlccDialogo.setHeaderText(null);
        Window jlccVentana = jlccVentana();
        if (jlccVentana != null) jlccDialogo.initOwner(jlccVentana);

        DialogPane jlccPanel = jlccDialogo.getDialogPane();
        jlccPanel.getStyleClass().add("dialogo");
        jlccPanel.setPrefWidth(700);
        var jlccHoja = getClass().getResource("/css/app.css");
        if (jlccHoja != null) jlccPanel.getStylesheets().add(jlccHoja.toExternalForm());

        ButtonType jlccCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType jlccVer = new ButtonType("Ver en Inmuebles", ButtonBar.ButtonData.OTHER);
        ButtonType jlccVerUnidad = new ButtonType("Ver unidad", ButtonBar.ButtonData.OTHER);
        if (jlccNivelVista == JlccNivelVista.PISO) {
            jlccPanel.getButtonTypes().addAll(jlccCerrar, jlccVerUnidad, jlccVer);
            Node jlccBotonVerUnidad = jlccPanel.lookupButton(jlccVerUnidad);
            if (jlccBotonVerUnidad != null) jlccBotonVerUnidad.setDisable(true);
        } else {
            jlccPanel.getButtonTypes().addAll(jlccCerrar, jlccVer);
        }

        VBox jlccCuerpo = new VBox(8);
        jlccCuerpo.setPadding(new Insets(0, 4, 0, 4));
        ScrollPane jlccScroll = new ScrollPane(jlccCuerpo);
        jlccScroll.setFitToWidth(true);
        jlccScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        jlccScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        jlccScroll.setPrefViewportHeight(520);
        jlccScroll.getStyleClass().add("pagina-scroll");
        jlccPanel.setContent(jlccScroll);
        jlccMostrarCargandoDetalle(jlccCuerpo);

        boolean[] jlccActivo = {true};
        jlccDialogo.setOnHidden(
                evento -> {
                    jlccActivo[0] = false;
                    jlccLimpiarSeleccion();
                });

        jlccTareas.jlccEjecutar(
                () -> jlccService.jlccDetalle(jlccBloque.id()),
                detalle -> {
                    if (!jlccActivo[0]) return;
                    jlccCuerpo.getChildren().setAll(jlccCrearContenidoDetalle(detalle));
                    jlccCuerpo.requestLayout();
                },
                error -> {
                    if (!jlccActivo[0]) return;
                    jlccNotificador.jlccError("No se pudo cargar el detalle del inmueble.");
                    Label jlccMensaje = new Label("No se pudo cargar el detalle. Intenta nuevamente.");
                    jlccMensaje.getStyleClass().add("campo-error");
                    jlccMensaje.setWrapText(true);
                    jlccCuerpo.getChildren().setAll(jlccMensaje);
                });

        var jlccResultado = jlccDialogo.showAndWait();
        if (jlccResultado.isPresent() && jlccResultado.get() == jlccVer && jlccPrincipal != null) {
            jlccPrincipal.jlccIrA("inmuebles");
        }
    }

    private void jlccMostrarCargandoDetalle(VBox jlccCuerpo) {
        ProgressIndicator jlccIndicador = new ProgressIndicator();
        jlccIndicador.setPrefSize(34, 34);
        jlccIndicador.setMaxSize(34, 34);
        Label jlccTexto = new Label("Cargando detalle…");
        jlccTexto.getStyleClass().add("pagina-subtitulo");
        VBox jlccCaja = new VBox(10, jlccIndicador, jlccTexto);
        jlccCaja.setAlignment(Pos.CENTER);
        jlccCaja.setPrefHeight(180);
        jlccCuerpo.getChildren().setAll(jlccCaja);
    }

    private Node jlccCrearContenidoDetalle(JlccMaquetaService.DetalleMaqueta jlccDetalle) {
        return jlccCrearContenidoDetalle(jlccDetalle, false);
    }

    private Node jlccCrearContenidoDetalle(JlccMaquetaService.DetalleMaqueta jlccDetalle, boolean jlccCompacto) {
        VBox jlccContenido = new VBox(8);
        Label jlccTitulo = new Label(jlccDetalle.codigo() + " · " + jlccDetalle.nombre());
        jlccTitulo.getStyleClass().add("dialogo-titulo");
        jlccTitulo.setWrapText(true);
        jlccContenido.getChildren().addAll(jlccTitulo, new Separator());
        if (jlccModoVista != JlccModoVista.ESTADO) {
            Label jlccDatoModo =
                    new Label("Vista " + jlccModoVista.jlccEtiqueta() + " · " + jlccDescripcionModo(jlccDetalle));
            jlccDatoModo.getStyleClass().add("maqueta-detalle-modo");
            jlccDatoModo.setWrapText(true);
            jlccContenido.getChildren().add(jlccDatoModo);
        }

        Label jlccSeccionInmueble = new Label("INMUEBLE");
        jlccSeccionInmueble.getStyleClass().add("seccion-form");
        GridPane jlccDatos = jlccGridDetalle(jlccCompacto);
        jlccAgregarPar(jlccDatos, 0, 0, "Tipo", jlccValor(jlccDetalle.tipo()));
        jlccAgregarPar(jlccDatos, 0, 1, "Nivel / orden", jlccNivelOrden(jlccDetalle));
        jlccAgregarFilaCompleta(jlccDatos, 1, "Dirección", jlccValor(jlccDetalle.direccion()));
        jlccAgregarPar(jlccDatos, 2, 0, "Distrito", jlccValor(jlccDetalle.distrito()));
        jlccAgregarPar(
                jlccDatos,
                2,
                1,
                "Área",
                jlccDetalle.areaM2() == null ? "—" : jlccDetalle.areaM2().toPlainString() + " m²");
        jlccAgregarPar(jlccDatos, 3, 0, "Dormitorios", jlccNumero(jlccDetalle.dormitorios()));
        jlccAgregarPar(jlccDatos, 3, 1, "Baños", jlccNumero(jlccDetalle.banos()));
        jlccAgregarPar(jlccDatos, 4, 0, "Renta referencial", jlccMoneda(jlccDetalle.rentaReferencial()));
        jlccAgregarPar(jlccDatos, 4, 1, "Estado", jlccDetalle.estado().getEtiqueta());
        jlccContenido.getChildren().addAll(jlccSeccionInmueble, jlccDatos);

        Label jlccSeccionContrato = new Label("CONTRATO VIGENTE");
        jlccSeccionContrato.getStyleClass().add("seccion-form");
        jlccContenido.getChildren().add(jlccSeccionContrato);
        JlccMaquetaService.ContratoDetalle jlccContrato = jlccDetalle.contrato();
        if (jlccContrato == null) {
            Label jlccSinContrato = new Label("Sin contrato vigente");
            jlccSinContrato.getStyleClass().add("campo-ayuda");
            jlccSinContrato.setWrapText(true);
            jlccContenido.getChildren().add(jlccSinContrato);
            if (jlccDetalle.inconsistencia()) {
                Label jlccAviso =
                        new Label(
                                "Inconsistencia: el inmueble figura como alquilado "
                                        + "pero no tiene un contrato vigente.");
                jlccAviso.getStyleClass().add("campo-error");
                jlccAviso.setWrapText(true);
                jlccContenido.getChildren().add(jlccAviso);
            }
        } else {
            GridPane jlccDatosContrato = jlccGridDetalle(jlccCompacto);
            jlccAgregarPar(jlccDatosContrato, 0, 0, "Código", jlccValor(jlccContrato.codigo()));
            jlccAgregarPar(jlccDatosContrato, 0, 1, "Renta mensual", jlccMoneda(jlccContrato.rentaMensual()));
            jlccAgregarFilaCompleta(jlccDatosContrato, 1, "Arrendatario", jlccValor(jlccContrato.arrendatario()));
            jlccAgregarPar(jlccDatosContrato, 2, 0, "Documento", jlccValor(jlccContrato.documento()));
            HBox jlccSemaforo = new HBox(8, JlccSemaforo.jlccCrear(jlccContrato.semaforo()));
            jlccSemaforo.setAlignment(Pos.CENTER_LEFT);
            jlccAgregarNodo(jlccDatosContrato, 2, 1, "Semáforo", jlccSemaforo);
            jlccAgregarPar(jlccDatosContrato, 3, 0, "Fecha de inicio", JlccFechas.jlccDia(jlccContrato.fechaInicio()));
            jlccAgregarPar(jlccDatosContrato, 3, 1, "Fecha de fin", JlccFechas.jlccDia(jlccContrato.fechaFin()));
            jlccContenido.getChildren().add(jlccDatosContrato);

            HBox jlccResumen =
                    new HBox(
                            jlccCompacto ? 16 : 30,
                            jlccDatoResumen(
                                    "Cuotas vencidas", String.valueOf(jlccContrato.cuotasVencidas())),
                            jlccDatoResumen(
                                    "Monto total pendiente", jlccMoneda(jlccContrato.montoPendiente())));
            jlccResumen.setAlignment(Pos.CENTER_LEFT);
            jlccResumen.getStyleClass().add("maqueta-resumen-cuotas");
            jlccContenido.getChildren().add(jlccResumen);
        }
        return jlccContenido;
    }

    private GridPane jlccGridDetalle(boolean jlccCompacto) {
        GridPane jlccGrid = new GridPane();
        jlccGrid.setHgap(jlccCompacto ? 10 : 16);
        jlccGrid.setVgap(7);
        double jlccAnchoEtiqueta = jlccCompacto ? 78 : 105;
        ColumnConstraints jlccEtiqueta1 = new ColumnConstraints();
        jlccEtiqueta1.setMinWidth(jlccCompacto ? 72 : 100);
        jlccEtiqueta1.setPrefWidth(jlccAnchoEtiqueta);
        ColumnConstraints jlccValor1 = new ColumnConstraints();
        jlccValor1.setHgrow(Priority.ALWAYS);
        ColumnConstraints jlccEtiqueta2 = new ColumnConstraints();
        jlccEtiqueta2.setMinWidth(jlccCompacto ? 72 : 100);
        jlccEtiqueta2.setPrefWidth(jlccAnchoEtiqueta);
        ColumnConstraints jlccValor2 = new ColumnConstraints();
        jlccValor2.setHgrow(Priority.ALWAYS);
        jlccGrid.getColumnConstraints().addAll(jlccEtiqueta1, jlccValor1, jlccEtiqueta2, jlccValor2);
        return jlccGrid;
    }

    private void jlccAgregarPar(GridPane jlccGrid, int jlccFila, int jlccPar, String jlccEtiqueta, String jlccValor) {
        Label jlccContenido = new Label(jlccValor);
        jlccContenido.setWrapText(true);
        jlccContenido.setMaxWidth(Double.MAX_VALUE);
        jlccAgregarNodo(jlccGrid, jlccFila, jlccPar, jlccEtiqueta, jlccContenido);
    }

    private void jlccAgregarFilaCompleta(GridPane jlccGrid, int jlccFila, String jlccEtiqueta, String jlccValor) {
        Label jlccNombre = new Label(jlccEtiqueta);
        jlccNombre.getStyleClass().add("campo-label");
        jlccNombre.setWrapText(true);
        jlccNombre.setMaxWidth(Double.MAX_VALUE);
        Label jlccContenido = new Label(jlccValor);
        jlccContenido.setWrapText(true);
        jlccContenido.setMaxWidth(Double.MAX_VALUE);
        jlccGrid.add(jlccNombre, 0, jlccFila);
        jlccGrid.add(jlccContenido, 1, jlccFila, 3, 1);
    }

    private void jlccAgregarNodo(GridPane jlccGrid, int jlccFila, int jlccPar, String jlccEtiqueta, Node jlccValor) {
        Label jlccNombre = new Label(jlccEtiqueta);
        jlccNombre.getStyleClass().add("campo-label");
        jlccNombre.setWrapText(true);
        jlccNombre.setMaxWidth(Double.MAX_VALUE);
        int jlccColumna = jlccPar * 2;
        jlccGrid.add(jlccNombre, jlccColumna, jlccFila);
        jlccGrid.add(jlccValor, jlccColumna + 1, jlccFila);
    }

    private VBox jlccDatoResumen(String jlccEtiqueta, String jlccValor) {
        Label jlccNombre = new Label(jlccEtiqueta);
        jlccNombre.getStyleClass().add("maqueta-resumen-etiqueta");
        Label jlccContenido = new Label(jlccValor);
        jlccContenido.getStyleClass().add("maqueta-resumen-valor");
        return new VBox(2, jlccNombre, jlccContenido);
    }

    private String jlccNivelOrden(JlccMaquetaService.DetalleMaqueta jlccDetalle) {
        String jlccNivel = jlccDetalle.nivel() == null ? "—" : jlccEtiquetaNivel(jlccDetalle.nivel());
        String jlccOrden = jlccDetalle.orden() == null ? "—" : String.valueOf(jlccDetalle.orden());
        return jlccNivel + " · Orden " + jlccOrden;
    }

    private String jlccValor(String jlccValor) {
        return jlccValor == null || jlccValor.isBlank() ? "—" : jlccValor;
    }

    private String jlccNumero(Integer jlccValor) {
        return jlccValor == null ? "—" : String.valueOf(jlccValor);
    }

    private String jlccMoneda(BigDecimal jlccValor) {
        return jlccValor == null ? "—" : JlccTablas.jlccMoneda(jlccValor);
    }

    private Window jlccVentana() {
        return contenedor3d == null || contenedor3d.getScene() == null
                ? null
                : contenedor3d.getScene().getWindow();
    }

    private void jlccConfigurarInteraccion() {
        jlccSubEscena.setOnMouseEntered(evento -> jlccSubEscena.setCursor(Cursor.OPEN_HAND));
        jlccSubEscena.setOnMousePressed(this::jlccAlPresionar);
        jlccSubEscena.setOnMouseDragged(this::jlccAlArrastrar);
        jlccSubEscena.setOnMouseReleased(this::jlccAlSoltar);
        jlccSubEscena.setOnMouseMoved(this::jlccAlMover);
        jlccSubEscena.setOnMouseExited(this::jlccAlSalir);
        jlccSubEscena.setOnScroll(this::jlccAlDesplazar);
    }

    private void jlccAlPresionar(MouseEvent jlccEvento) {
        if (jlccTransicionActiva()) return;
        if (jlccEvento.getButton() != MouseButton.PRIMARY) return;
        jlccXInicialClic = jlccEvento.getSceneX();
        jlccYInicialClic = jlccEvento.getSceneY();
        jlccXPresionado = jlccEvento.getSceneX();
        jlccYPresionado = jlccEvento.getSceneY();
        jlccArrastrando = true;
        jlccSubEscena.setCursor(Cursor.CLOSED_HAND);
    }

    private void jlccAlArrastrar(MouseEvent jlccEvento) {
        if (!jlccArrastrando || jlccTransicionActiva()) return;
        double jlccDx = jlccEvento.getSceneX() - jlccXPresionado;
        double jlccDy = jlccEvento.getSceneY() - jlccYPresionado;
        jlccXPresionado = jlccEvento.getSceneX();
        jlccYPresionado = jlccEvento.getSceneY();

        jlccRotacionY.setAngle(Math.IEEEremainder(jlccRotacionY.getAngle() + jlccDx * 0.3, 360));
        if (jlccNivelVista == JlccNivelVista.EDIFICIO) {
            jlccRotacionX.setAngle(
                    jlccLimitar(jlccRotacionX.getAngle() + jlccDy * 0.22, JLCC_ANGULO_X_MINIMO, JLCC_ANGULO_X_MAXIMO));
        }
        jlccActualizarEtiquetasPiso();
        jlccActualizarEtiquetasUnidad();
        jlccActualizarEtiquetasAmbiente();
    }

    private void jlccAlSoltar(MouseEvent jlccEvento) {
        boolean jlccFuePresion = jlccArrastrando;
        jlccArrastrando = false;
        if (jlccSubEscena != null) jlccSubEscena.setCursor(Cursor.OPEN_HAND);
        if (!jlccFuePresion || jlccEvento.getButton() != MouseButton.PRIMARY) return;
        if (jlccTransicionActiva()) return;
        double jlccDx = jlccEvento.getSceneX() - jlccXInicialClic;
        double jlccDy = jlccEvento.getSceneY() - jlccYInicialClic;
        if (Math.hypot(jlccDx, jlccDy) > 5) return;
        Node jlccSeleccionado = jlccEvento.getPickResult().getIntersectedNode();
        BloqueMaqueta jlccBloque = jlccMapaBloques.get(jlccSeleccionado);
        if (jlccBloque != null) {
            if (jlccNivelVista == JlccNivelVista.PISO) jlccEntrarUnidad(jlccBloque);
            else if (jlccNivelVista == JlccNivelVista.EDIFICIO) jlccAbrirDetalle(jlccBloque);
            return;
        }
        Integer jlccPiso = jlccMapaPisos.get(jlccSeleccionado);
        if (jlccPiso != null) jlccEntrarPiso(jlccPiso);
    }

    private void jlccAlMover(MouseEvent jlccEvento) {
        if (jlccArrastrando || jlccTransicionActiva()) return;
        jlccActualizarHover(jlccMapaBloques.get(jlccEvento.getPickResult().getIntersectedNode()));
    }

    private void jlccAlSalir(MouseEvent jlccEvento) {
        jlccArrastrando = false;
        jlccActualizarHover(null);
        jlccSubEscena.setCursor(Cursor.DEFAULT);
    }

    private void jlccAlDesplazar(ScrollEvent jlccEvento) {
        if (jlccTransicionActiva()) return;
        if (jlccNivelVista == JlccNivelVista.UNIDAD && jlccVistaPlano) {
            jlccZoomPlano = jlccLimitar(jlccZoomPlano * (1 + jlccEvento.getDeltaY() * 0.0015), 0.6, 2.0);
            jlccDibujarPlano();
            jlccEvento.consume();
            return;
        }
        if (jlccCamara == null || jlccSubEscena == null || jlccDistanciaAuto <= 0) return;
        double jlccAltoVista = jlccSubEscena.getHeight();
        if (jlccAltoVista <= 0) return;

        double jlccTanV = Math.tan(Math.toRadians(jlccCamara.getFieldOfView() / 2));
        double jlccFoco = jlccAltoVista / (2 * jlccTanV);
        double jlccDistancia = jlccCentroZAuto - (jlccCamara.getTranslateZ() - jlccFoco);
        jlccDistancia =
                jlccLimitar(
                        jlccDistancia - jlccEvento.getDeltaY() * 1.1,
                        jlccDistanciaAuto * JLCC_FACTOR_ZOOM_CERCA,
                        jlccDistanciaAuto * JLCC_FACTOR_ZOOM_LEJOS);
        jlccCamara.setTranslateZ(jlccCentroZAuto - jlccDistancia + jlccFoco);
        jlccActualizarEtiquetasPiso();
        jlccActualizarEtiquetasUnidad();
        jlccActualizarEtiquetasAmbiente();
        jlccEvento.consume();
    }

    private void jlccActualizarEncuadreYEtiquetas() {
        if (jlccTransicionActiva()) return;
        if (jlccNivelVista == JlccNivelVista.UNIDAD) jlccAjustarPanelUnidad();
        jlccEncuadrarAutomatico();
        jlccActualizarEtiquetasPiso();
        jlccActualizarEtiquetasUnidad();
        jlccActualizarEtiquetasAmbiente();
        if (jlccNivelVista == JlccNivelVista.UNIDAD && jlccVistaPlano) jlccDibujarPlano();
    }

    private void jlccEncuadrarAutomatico() {
        if (jlccMundo == null || jlccCamara == null || jlccSubEscena == null) return;
        double jlccAnchoVista = jlccSubEscena.getWidth();
        double jlccAltoVista = jlccSubEscena.getHeight();
        if (jlccAnchoVista < 60 || jlccAltoVista < 60) return;

        List<Point3D> jlccPuntos =
                jlccNivelVista == JlccNivelVista.UNIDAD && jlccEsquemaActual != null
                        ? jlccPuntosEsquema(jlccEsquemaActual)
                        : jlccPuntosEncuadreActuales();
        double jlccAnchoDisponible = jlccAnchoEncuadreDisponible();
        Encuadre jlccEncuadre =
                jlccCalcularEncuadre(
                        jlccProyectarAlPadre(jlccPuntos),
                        jlccAnchoDisponible,
                        jlccAltoVista,
                        jlccCamara.getFieldOfView(),
                        jlccCamara.getNearClip(),
                        JLCC_RELLENO_ENCUADRE);
        if (jlccEncuadre == null) return;
        jlccAplicarEncuadre(jlccEncuadre, jlccAltoVista);
        jlccActualizarEtiquetasPiso();
    }

    private double jlccAnchoEncuadreDisponible() {
        double jlccAncho = jlccSubEscena == null ? 0 : jlccSubEscena.getWidth();
        if (jlccNivelVista == JlccNivelVista.UNIDAD && panelUnidad != null && panelUnidad.isVisible()) {
            return Math.max(220, jlccAncho - jlccAnchoPanelUnidadEfectivo() - JLCC_MARGEN_PANEL_UNIDAD);
        }
        return jlccAncho;
    }

    private void jlccAjustarPanelUnidad() {
        if (panelUnidad == null || jlccSubEscena == null) return;
        double jlccAnchoEscena = jlccSubEscena.getWidth();
        if (jlccAnchoEscena <= 0) return;
        double jlccAncho =
                Math.max(
                        JLCC_ANCHO_PANEL_UNIDAD_MINIMO,
                        Math.min(JLCC_ANCHO_PANEL_UNIDAD, jlccAnchoEscena * 0.42));
        panelUnidad.setPrefWidth(jlccAncho);
        if (jlccNivelVista == JlccNivelVista.UNIDAD) jlccDesplazarEscenaUnidad(true);
    }

    private double jlccAnchoPanelUnidadEfectivo() {
        if (panelUnidad == null) return JLCC_ANCHO_PANEL_UNIDAD;
        double jlccAncho = panelUnidad.getPrefWidth();
        if (jlccAncho <= 0) jlccAncho = panelUnidad.getWidth();
        if (jlccAncho <= 0) jlccAncho = JLCC_ANCHO_PANEL_UNIDAD;
        return Math.max(JLCC_ANCHO_PANEL_UNIDAD_MINIMO, Math.min(JLCC_ANCHO_PANEL_UNIDAD, jlccAncho));
    }

    private List<Point3D> jlccPuntosEncuadreActuales() {
        if (jlccNivelVista == JlccNivelVista.PISO && jlccNivelEnfocado != null && jlccGeometria != null) {
            PisoGeometria jlccPiso = jlccGeometria.pisos().get(jlccNivelEnfocado);
            JlccPlantaPiso.Distribucion jlccPlanta = jlccDistribucionesPlanta.get(jlccNivelEnfocado);
            if (jlccPiso != null && jlccPlanta != null) return jlccPuntosPlanta(jlccPiso, jlccPlanta);
        }
        return jlccPuntosEncuadre;
    }

    private static List<Point3D> jlccPuntosPlanta(PisoGeometria jlccPiso, JlccPlantaPiso.Distribucion jlccPlanta) {
        List<Point3D> jlccPuntos = new ArrayList<>();
        double jlccAlto = JLCC_ALTO_CONTORNO_PLANTA + JLCC_ALTO_PLACA_PLANTA + 3;
        jlccAgregarPuntosCaja(
                jlccPuntos,
                0,
                jlccPiso.y() - 3.5,
                0,
                jlccPlanta.anchoConMargen(),
                jlccAlto,
                jlccPlanta.profundidadConMargen());
        return jlccPuntos;
    }

    private static List<Point3D> jlccPuntosEsquema(JlccGeneradorEsquema.Esquema jlccEsquema) {
        List<Point3D> jlccPuntos = new ArrayList<>();
        double jlccAncho = jlccEsquema.ancho() * JLCC_ESCALA_ESQUEMA + 40;
        double jlccProfundidad = jlccEsquema.alto() * JLCC_ESCALA_ESQUEMA + 40;
        double jlccAlto = JLCC_ALTO_PARED_ESQUEMA + 18;
        jlccAgregarPuntosCaja(jlccPuntos, 0, -JLCC_ALTO_PARED_ESQUEMA / 2, 0, jlccAncho, jlccAlto, jlccProfundidad);
        return jlccPuntos;
    }

    private List<Point3D> jlccProyectarAlPadre(List<Point3D> jlccPuntos) {
        List<Point3D> jlccPadres = new ArrayList<>(jlccPuntos.size());
        for (Point3D jlccPunto : jlccPuntos) jlccPadres.add(jlccMundo.localToParent(jlccPunto));
        return jlccPadres;
    }

    private List<Point3D> jlccProyectarConRotacion(
            List<Point3D> jlccPuntos, double jlccAnguloX, double jlccAnguloY) {
        Group jlccTemporal = new Group();
        jlccTemporal.getTransforms()
                .addAll(new Rotate(jlccAnguloX, Rotate.X_AXIS), new Rotate(jlccAnguloY, Rotate.Y_AXIS));
        List<Point3D> jlccPadres = new ArrayList<>(jlccPuntos.size());
        for (Point3D jlccPunto : jlccPuntos) jlccPadres.add(jlccTemporal.localToParent(jlccPunto));
        return jlccPadres;
    }

    private void jlccAplicarEncuadre(Encuadre jlccEncuadre, double jlccAltoVista) {
        double jlccAnchoVista = jlccSubEscena.getWidth();
        double jlccFoco = jlccAltoVista / (2 * Math.tan(Math.toRadians(jlccCamara.getFieldOfView() / 2)));
        jlccCamara.setTranslateX(jlccEncuadre.ojoX() - jlccAnchoVista / 2);
        jlccCamara.setTranslateY(jlccEncuadre.ojoY() - jlccAltoVista / 2);
        jlccCamara.setTranslateZ(jlccEncuadre.centroZ() - jlccEncuadre.distancia() + jlccFoco);
        jlccAplicarReferenciaAutomatica(jlccEncuadre);
    }

    private void jlccAplicarReferenciaAutomatica(Encuadre jlccEncuadre) {
        if (jlccEncuadre == null) return;
        jlccCentroZAuto = jlccEncuadre.centroZ();
        jlccDistanciaAuto = jlccEncuadre.distancia();
        jlccCamara.setFarClip(Math.max(6000, jlccEncuadre.distancia() * 6));
    }

    static Encuadre jlccCalcularEncuadre(
            List<Point3D> jlccPuntos,
            double jlccAnchoVista,
            double jlccAltoVista,
            double jlccFovGrados,
            double jlccNearClip,
            double jlccRelleno) {
        if (jlccPuntos == null || jlccPuntos.isEmpty() || jlccAnchoVista <= 0 || jlccAltoVista <= 0) {
            return null;
        }

        double jlccMinX = Double.POSITIVE_INFINITY, jlccMaxX = Double.NEGATIVE_INFINITY;
        double jlccMinY = Double.POSITIVE_INFINITY, jlccMaxY = Double.NEGATIVE_INFINITY;
        double jlccMinZ = Double.POSITIVE_INFINITY, jlccMaxZ = Double.NEGATIVE_INFINITY;
        for (Point3D jlccPunto : jlccPuntos) {
            jlccMinX = Math.min(jlccMinX, jlccPunto.getX());
            jlccMaxX = Math.max(jlccMaxX, jlccPunto.getX());
            jlccMinY = Math.min(jlccMinY, jlccPunto.getY());
            jlccMaxY = Math.max(jlccMaxY, jlccPunto.getY());
            jlccMinZ = Math.min(jlccMinZ, jlccPunto.getZ());
            jlccMaxZ = Math.max(jlccMaxZ, jlccPunto.getZ());
        }

        double jlccCentroX = (jlccMinX + jlccMaxX) / 2;
        double jlccCentroY = (jlccMinY + jlccMaxY) / 2;
        double jlccCentroZ = (jlccMinZ + jlccMaxZ) / 2;
        double jlccTanV = Math.tan(Math.toRadians(jlccFovGrados / 2));
        double jlccAspecto = jlccAnchoVista / jlccAltoVista;

        double jlccOjoX = jlccCentroX;
        double jlccOjoY = jlccCentroY;
        double jlccDistancia = Math.max(500, Math.max(jlccMaxX - jlccMinX, Math.max(jlccMaxY - jlccMinY, jlccMaxZ - jlccMinZ)));
        for (int jlccIteracion = 0; jlccIteracion < 8; jlccIteracion++) {
            double jlccMinNdcX = Double.POSITIVE_INFINITY, jlccMaxNdcX = Double.NEGATIVE_INFINITY;
            double jlccMinNdcY = Double.POSITIVE_INFINITY, jlccMaxNdcY = Double.NEGATIVE_INFINITY;
            double jlccProfundidadMinima = Double.POSITIVE_INFINITY;
            for (Point3D jlccPunto : jlccPuntos) {
                double jlccProfundidad = jlccDistancia + jlccPunto.getZ() - jlccCentroZ;
                jlccProfundidadMinima = Math.min(jlccProfundidadMinima, jlccProfundidad);
                if (jlccProfundidad <= jlccNearClip) continue;
                double jlccNdcX = (jlccPunto.getX() - jlccOjoX) / (jlccProfundidad * jlccTanV * jlccAspecto);
                double jlccNdcY = (jlccPunto.getY() - jlccOjoY) / (jlccProfundidad * jlccTanV);
                jlccMinNdcX = Math.min(jlccMinNdcX, jlccNdcX);
                jlccMaxNdcX = Math.max(jlccMaxNdcX, jlccNdcX);
                jlccMinNdcY = Math.min(jlccMinNdcY, jlccNdcY);
                jlccMaxNdcY = Math.max(jlccMaxNdcY, jlccNdcY);
            }

            if (jlccProfundidadMinima <= jlccNearClip
                    || !Double.isFinite(jlccMinNdcX)
                    || !Double.isFinite(jlccMinNdcY)) {
                jlccDistancia *= 1.5;
                continue;
            }

            double jlccRangoX = jlccMaxNdcX - jlccMinNdcX;
            double jlccRangoY = jlccMaxNdcY - jlccMinNdcY;
            double jlccEscala = Math.max(jlccRangoX, jlccRangoY) / (2 * jlccRelleno);
            if (!Double.isFinite(jlccEscala) || jlccEscala <= 0) jlccEscala = 1;

            jlccOjoX += ((jlccMinNdcX + jlccMaxNdcX) / 2) * jlccDistancia * jlccTanV * jlccAspecto;
            jlccOjoY += ((jlccMinNdcY + jlccMaxNdcY) / 2) * jlccDistancia * jlccTanV;
            jlccDistancia *= jlccEscala;
        }

        if (!Double.isFinite(jlccOjoX)
                || !Double.isFinite(jlccOjoY)
                || !Double.isFinite(jlccDistancia)
                || jlccDistancia <= 0) {
            return null;
        }
        return new Encuadre(jlccOjoX, jlccOjoY, jlccCentroZ, Math.max(250, jlccDistancia));
    }

    static List<Point3D> jlccPuntosPiso(Geometria jlccGeometria, int jlccNivel) {
        if (jlccGeometria == null) return List.of();
        PisoGeometria jlccPiso = jlccGeometria.pisos().get(jlccNivel);
        if (jlccPiso == null) return List.of();

        List<Point3D> jlccPuntos = new ArrayList<>();
        jlccAgregarPuntosCaja(
                jlccPuntos,
                0,
                jlccPiso.y() + JLCC_ALTO_UNIDAD / 2 + JLCC_LOSA_ALTO / 2,
                0,
                jlccPiso.ancho() + 2 * JLCC_LOSA_MARGEN_X,
                JLCC_LOSA_ALTO,
                jlccPiso.profundidad() + 2 * JLCC_LOSA_MARGEN_Z);
        for (FilaPiso jlccFila : jlccPiso.filas()) {
            for (BloqueMaqueta jlccBloque : jlccFila.bloques()) {
                Point3D jlccPosicion = jlccGeometria.posiciones().get(jlccBloque.id());
                if (jlccPosicion == null) continue;
                jlccAgregarPuntosCaja(
                        jlccPuntos,
                        jlccPosicion.getX(),
                        jlccPosicion.getY(),
                        jlccPosicion.getZ(),
                        JLCC_ANCHO_UNIDAD,
                        JLCC_ALTO_UNIDAD,
                        JLCC_PROFUNDIDAD_UNIDAD);
            }
        }
        return List.copyOf(jlccPuntos);
    }

    private void jlccActualizarEtiquetasPiso() {
        if (capaEtiquetas == null || jlccSubEscena == null || jlccCamara == null) return;
        double jlccAnchoVista = jlccSubEscena.getWidth();
        double jlccAltoVista = jlccSubEscena.getHeight();
        if (jlccAnchoVista <= 0 || jlccAltoVista <= 0) return;

        double jlccTanV = Math.tan(Math.toRadians(jlccCamara.getFieldOfView() / 2));
        double jlccAspecto = jlccAnchoVista / jlccAltoVista;
        double jlccFoco = jlccAltoVista / (2 * jlccTanV);
        Proyeccion jlccProyeccion =
                new Proyeccion(
                        jlccCamara.getTranslateX() + jlccAnchoVista / 2,
                        jlccCamara.getTranslateY() + jlccAltoVista / 2,
                        jlccCamara.getTranslateZ() - jlccFoco,
                        jlccTanV,
                        jlccAspecto,
                        jlccAnchoVista,
                        jlccAltoVista);

        for (EtiquetaPiso jlccEtiqueta : jlccEtiquetasPiso) {
            boolean jlccDentroDelAmbito = jlccNivelVista == JlccNivelVista.EDIFICIO;
            Point2D jlccPantalla = jlccDentroDelAmbito ? jlccProyectar(jlccEtiqueta.ancla(), jlccProyeccion) : null;
            Label jlccNodo = jlccEtiqueta.nodo();
            if (jlccPantalla == null) {
                jlccNodo.setVisible(false);
                continue;
            }

            jlccNodo.setVisible(true);
            jlccNodo.autosize();
            double x = jlccPantalla.getX() - jlccNodo.getWidth() - JLCC_SEPARACION_ETIQUETA_PANTALLA;
            double y = jlccPantalla.getY() - jlccNodo.getHeight() / 2;
            jlccNodo.relocate(
                    jlccLimitar(x, 8, Math.max(8, jlccAnchoVista - jlccNodo.getWidth() - 8)),
                    jlccLimitar(y, 8, Math.max(8, jlccAltoVista - jlccNodo.getHeight() - 8)));
        }
    }

    private boolean jlccEnAmbito(int jlccNivel) {
        if (jlccNivelVista == JlccNivelVista.EDIFICIO) return true;
        return jlccNivelVista == JlccNivelVista.PISO && jlccNivelEnfocado != null && jlccNivel == jlccNivelEnfocado;
    }

    private void jlccOcultarEtiquetasUnidad() {
        for (EtiquetaUnidad jlccEtiqueta : jlccEtiquetasUnidad) jlccEtiqueta.nodo().setVisible(false);
    }

    private Point2D jlccProyectar(Point3D jlccAncla, Proyeccion jlccProyeccion) {
        Point3D jlccPunto = jlccMundo.localToParent(jlccAncla);
        double jlccProfundidad = jlccPunto.getZ() - jlccProyeccion.ojoZ();
        if (jlccProfundidad <= 0.001) return null;
        double jlccNdcX =
                (jlccPunto.getX() - jlccProyeccion.ojoX())
                        / (jlccProfundidad * jlccProyeccion.tanV() * jlccProyeccion.aspecto());
        double jlccNdcY = (jlccPunto.getY() - jlccProyeccion.ojoY()) / (jlccProfundidad * jlccProyeccion.tanV());
        return new Point2D((jlccNdcX + 1) * jlccProyeccion.ancho() / 2, (jlccNdcY + 1) * jlccProyeccion.alto() / 2);
    }

    private void jlccActualizarEtiquetasUnidad() {
        if (capaEtiquetas == null
                || jlccSubEscena == null
                || jlccCamara == null
                || jlccEtiquetasUnidad.isEmpty()) {
            return;
        }
        double jlccAnchoVista = jlccSubEscena.getWidth();
        double jlccAltoVista = jlccSubEscena.getHeight();
        if (jlccAnchoVista <= 0 || jlccAltoVista <= 0) return;

        double jlccTanV = Math.tan(Math.toRadians(jlccCamara.getFieldOfView() / 2));
        double jlccAspecto = jlccAnchoVista / jlccAltoVista;
        double jlccFoco = jlccAltoVista / (2 * jlccTanV);
        Proyeccion jlccProyeccion =
                new Proyeccion(
                        jlccCamara.getTranslateX() + jlccAnchoVista / 2,
                        jlccCamara.getTranslateY() + jlccAltoVista / 2,
                        jlccCamara.getTranslateZ() - jlccFoco,
                        jlccTanV,
                        jlccAspecto,
                        jlccAnchoVista,
                        jlccAltoVista);

        double jlccDistancia = jlccCentroZAuto - (jlccCamara.getTranslateZ() - jlccFoco);
        double jlccFactor = jlccDistanciaAuto > 0 ? jlccDistancia / jlccDistanciaAuto : 1;
        long jlccEtiquetasEnAmbito =
                jlccEtiquetasUnidad.stream()
                        .filter(etiqueta -> jlccEnAmbito(etiqueta.bloque().nivel()))
                        .count();
        boolean jlccMostrar = jlccFactor <= 1.35 && (jlccEtiquetasEnAmbito <= 40 || jlccFactor < 0.9);
        for (EtiquetaUnidad jlccEtiqueta : jlccEtiquetasUnidad) {
            BloqueMaqueta jlccBloque = jlccEtiqueta.bloque();
            Point3D jlccAncla =
                    jlccNivelVista == JlccNivelVista.PISO
                            ? jlccEtiqueta.anclaPlanta()
                            : jlccEtiqueta.anclaEdificio();
            Point2D jlccPantalla =
                    jlccMostrar && jlccEnAmbito(jlccBloque.nivel()) ? jlccProyectar(jlccAncla, jlccProyeccion) : null;
            Label jlccNodo = jlccEtiqueta.nodo();
            if (jlccPantalla == null) {
                jlccNodo.setVisible(false);
                continue;
            }

            String jlccTexto = jlccTextoEtiquetaUnidad(jlccBloque);
            if (!jlccTexto.equals(jlccNodo.getText())) jlccNodo.setText(jlccTexto);
            jlccNodo.setVisible(true);
            jlccNodo.autosize();
            double x = jlccPantalla.getX() - jlccNodo.getWidth() / 2;
            double y = jlccPantalla.getY() - jlccNodo.getHeight() - 4;
            jlccNodo.relocate(
                    jlccLimitar(x, 4, Math.max(4, jlccAnchoVista - jlccNodo.getWidth() - 4)),
                    jlccLimitar(y, 4, Math.max(4, jlccAltoVista - jlccNodo.getHeight() - 4)));
        }
    }

    private String jlccTextoEtiquetaUnidad(BloqueMaqueta jlccBloque) {
        String jlccNumero = jlccNumeroUnidad(jlccBloque);
        if (jlccNivelVista != JlccNivelVista.PISO || !JlccPlantaPiso.jlccTieneArea(jlccBloque)) return jlccNumero;
        String jlccArea = jlccBloque.areaM2().stripTrailingZeros().toPlainString();
        return jlccNumero + " · " + jlccArea + " m²";
    }

    private void jlccActualizarEtiquetasAmbiente() {
        if (capaEtiquetas == null
                || jlccSubEscena == null
                || jlccCamara == null
                || jlccEtiquetasAmbiente.isEmpty()) {
            return;
        }
        if (jlccNivelVista != JlccNivelVista.UNIDAD || jlccVistaPlano) {
            jlccOcultarEtiquetasAmbiente();
            return;
        }
        double jlccAnchoVista = jlccSubEscena.getWidth();
        double jlccAltoVista = jlccSubEscena.getHeight();
        if (jlccAnchoVista <= 0 || jlccAltoVista <= 0) return;

        double jlccTanV = Math.tan(Math.toRadians(jlccCamara.getFieldOfView() / 2));
        double jlccAspecto = jlccAnchoVista / jlccAltoVista;
        double jlccFoco = jlccAltoVista / (2 * jlccTanV);
        Proyeccion jlccProyeccion =
                new Proyeccion(
                        jlccCamara.getTranslateX() + jlccAnchoVista / 2,
                        jlccCamara.getTranslateY() + jlccAltoVista / 2,
                        jlccCamara.getTranslateZ() - jlccFoco,
                        jlccTanV,
                        jlccAspecto,
                        jlccAnchoVista,
                        jlccAltoVista);
        double jlccDistancia = jlccCentroZAuto - (jlccCamara.getTranslateZ() - jlccFoco);
        double jlccFactor = jlccDistanciaAuto > 0 ? jlccDistancia / jlccDistanciaAuto : 1;
        boolean jlccMostrar = jlccFactor <= 1.5;

        for (EtiquetaAmbiente jlccEtiqueta : jlccEtiquetasAmbiente) {
            Point2D jlccPantalla = jlccMostrar ? jlccProyectar(jlccEtiqueta.ancla(), jlccProyeccion) : null;
            Label jlccNodo = jlccEtiqueta.nodo();
            if (jlccPantalla == null) {
                jlccNodo.setVisible(false);
                continue;
            }
            jlccNodo.setVisible(true);
            jlccNodo.autosize();
            double x = jlccPantalla.getX() - jlccNodo.getWidth() / 2;
            double y = jlccPantalla.getY() - jlccNodo.getHeight() - 6;
            jlccNodo.relocate(
                    jlccLimitar(x, 4, Math.max(4, jlccAnchoVista - jlccNodo.getWidth() - 4)),
                    jlccLimitar(y, 4, Math.max(4, jlccAltoVista - jlccNodo.getHeight() - 4)));
        }
    }

    private void jlccOcultarEtiquetasAmbiente() {
        for (EtiquetaAmbiente jlccEtiqueta : jlccEtiquetasAmbiente) jlccEtiqueta.nodo().setVisible(false);
    }

    private void jlccActualizarHover(BloqueMaqueta jlccBloque) {
        if (jlccBloque == jlccBloqueHover) return;

        if (jlccNodoHover != null) {
            if (jlccNodoHover instanceof Group jlccGrupo) {
                jlccGrupo.getChildren().remove(jlccContornoHover);
                jlccGrupo.getChildren().remove(jlccContornoHoverPlanta);
            }
            jlccEscalar(jlccNodoHover, 1);
        }
        jlccBloqueHover = jlccBloque;
        jlccNodoHover = null;

        if (jlccNivelVista == JlccNivelVista.UNIDAD) {
            jlccBloqueHover = null;
            etiquetaHover.setVisible(false);
            return;
        }

        if (jlccBloque == null) {
            etiquetaHover.setVisible(false);
            return;
        }

        if (jlccNivelVista == JlccNivelVista.PISO) {
            jlccNodoHover = jlccNodosPlantaPorBloque.get(jlccBloque);
            JlccPlantaPiso.Recinto jlccRecinto = jlccRecintosPlantaPorBloque.get(jlccBloque);
            if (jlccNodoHover instanceof Group jlccGrupo
                    && jlccRecinto != null
                    && jlccContornoHoverPlanta != null) {
                jlccAjustarContornoPlanta(jlccContornoHoverPlanta, jlccRecinto);
                jlccGrupo.getChildren().add(jlccContornoHoverPlanta);
            }
        } else {
            jlccNodoHover = jlccNodosPorBloque.get(jlccBloque);
            Point3D jlccPosicion = jlccGeometria == null ? null : jlccGeometria.posiciones().get(jlccBloque.id());
            if (jlccNodoHover instanceof Group jlccGrupo) {
                jlccEscalar(jlccNodoHover, 1.045);
                if (jlccContornoHover != null && jlccPosicion != null) {
                    jlccContornoHover.setTranslateX(jlccPosicion.getX());
                    jlccContornoHover.setTranslateY(jlccPosicion.getY());
                    jlccContornoHover.setTranslateZ(jlccPosicion.getZ());
                    jlccGrupo.getChildren().add(jlccContornoHover);
                }
            }
        }

        if (jlccNodoHover == null) {
            etiquetaHover.setVisible(false);
            return;
        }

        etiquetaHover.setText(jlccDescripcionHover(jlccBloque));
        etiquetaHover.setVisible(true);
    }

    private void jlccAjustarContornoPlanta(Box jlccContorno, JlccPlantaPiso.Recinto jlccRecinto) {
        jlccContorno.setWidth(jlccRecinto.ancho() + 2);
        jlccContorno.setHeight(JLCC_ALTO_CONTORNO_PLANTA);
        jlccContorno.setDepth(jlccRecinto.profundidad() + 2);
        jlccContorno.setTranslateY(-(JLCC_ALTO_CONTORNO_PLANTA / 2 - 1));
    }

    private void jlccEscalar(Node jlccNodo, double jlccFactor) {
        jlccNodo.setScaleX(jlccFactor);
        jlccNodo.setScaleY(jlccFactor);
        jlccNodo.setScaleZ(jlccFactor);
    }

    private void jlccConfigurarNavegacionTeclado() {
        if (contenedor3d == null) return;
        jlccEnlazarTeclado(contenedor3d.getScene());
        contenedor3d
                .sceneProperty()
                .addListener((observable, anterior, actual) -> jlccEnlazarTeclado(actual));
    }

    private void jlccEnlazarTeclado(Scene jlccEscena) {
        if (jlccEscenaTeclado == jlccEscena) return;
        if (jlccEscenaTeclado != null) {
            jlccEscenaTeclado.removeEventHandler(KeyEvent.KEY_PRESSED, jlccManejadorTecla);
        }
        jlccEscenaTeclado = jlccEscena;
        if (jlccEscenaTeclado != null) {
            jlccEscenaTeclado.addEventHandler(KeyEvent.KEY_PRESSED, jlccManejadorTecla);
        } else {
            jlccDetenerTransicion();
        }
    }

    private void jlccAlPresionarTecla(KeyEvent jlccEvento) {
        if (jlccEvento.getCode() != KeyCode.ESCAPE) return;
        if (jlccNivelVista == JlccNivelVista.EDIFICIO) return;
        if (selectorModo != null && selectorModo.isShowing()) return;
        jlccEvento.consume();
        if (jlccTransicionActiva()) return;
        jlccVolverUnNivel();
    }

    private void jlccVolverUnNivel() {
        if (jlccNivelVista == JlccNivelVista.EDIFICIO) return;
        JlccNivelVista jlccDestino = jlccNivelVista.jlccVolver();
        if (!jlccNivelVista.jlccPuedeIrA(jlccDestino)) return;
        if (jlccDestino == JlccNivelVista.PISO) jlccVolverAlPiso();
        else if (jlccDestino == JlccNivelVista.EDIFICIO) jlccVolverAlEdificio();
    }

    @FXML
    private void jlccOnVolverNavegacion() {
        if (jlccNivelVista == JlccNivelVista.EDIFICIO || jlccTransicionActiva()) return;
        if (jlccNivelVista == JlccNivelVista.UNIDAD) jlccVolverAlPiso();
        else jlccVolverAlEdificio();
    }

    private void jlccEntrarPiso(int jlccNivel) {
        if (jlccMundo == null || jlccGeometria == null || jlccCamara == null) return;
        if (jlccTransicionActiva() || jlccNivelVista != JlccNivelVista.EDIFICIO) return;
        if (!jlccNivelVista.jlccPuedeIrA(JlccNivelVista.PISO)) return;
        if (!jlccGeometria.pisos().containsKey(jlccNivel)
                || !jlccGruposPiso.containsKey(jlccNivel)
                || !jlccGruposPlanta.containsKey(jlccNivel)
                || !jlccDistribucionesPlanta.containsKey(jlccNivel)) {
            return;
        }

        jlccAnguloXEdificio = jlccRotacionX.getAngle();
        jlccAnguloYEdificio = jlccRotacionY.getAngle();
        jlccOcultarMensajeEscena();
        jlccNivelVista = JlccNivelVista.PISO;
        jlccNivelEnfocado = jlccNivel;
        jlccLimpiarSeleccion();
        jlccActualizarHover(null);
        jlccActualizarBarraNavegacion();
        jlccActualizarResumen(jlccBloquesActuales);
        jlccActualizarEtiquetasPiso();
        jlccOcultarEtiquetasUnidad();

        jlccGruposPiso.get(jlccNivel).setVisible(false);
        jlccGruposPlanta.get(jlccNivel).setVisible(true);

        List<KeyValue> jlccValores = new ArrayList<>();
        for (Map.Entry<Integer, Group> jlccEntrada : jlccGruposPiso.entrySet()) {
            if (jlccEntrada.getKey() != jlccNivel) {
                jlccValores.add(
                        new KeyValue(
                                jlccEntrada.getValue().scaleYProperty(),
                                JLCC_ESCALA_OCULTA,
                                Interpolator.EASE_BOTH));
            }
        }
        if (jlccEstructuraGeneral != null) {
            jlccValores.add(
                    new KeyValue(
                            jlccEstructuraGeneral.scaleYProperty(),
                            JLCC_ESCALA_OCULTA,
                            Interpolator.EASE_BOTH));
        }
        jlccValores.add(new KeyValue(jlccRotacionX.angleProperty(), JLCC_ANGULO_X_PISO, Interpolator.EASE_BOTH));
        jlccValores.add(new KeyValue(jlccRotacionY.angleProperty(), JLCC_ANGULO_Y_PISO, Interpolator.EASE_BOTH));

        PisoGeometria jlccPiso = jlccGeometria.pisos().get(jlccNivel);
        JlccPlantaPiso.Distribucion jlccPlanta = jlccDistribucionesPlanta.get(jlccNivel);
        Encuadre jlccEncuadre =
                jlccEncuadreDePuntos(jlccPuntosPlanta(jlccPiso, jlccPlanta), JLCC_ANGULO_X_PISO, JLCC_ANGULO_Y_PISO);
        jlccValores.addAll(jlccValoresCamara(jlccEncuadre));

        jlccIniciarTransicion(
                jlccValores,
                () -> {
                    for (Map.Entry<Integer, Group> jlccEntrada : jlccGruposPiso.entrySet()) {
                        if (jlccEntrada.getKey() != jlccNivel) jlccEntrada.getValue().setVisible(false);
                    }
                    if (jlccEstructuraGeneral != null) jlccEstructuraGeneral.setVisible(false);
                    jlccRotacionX.setAngle(JLCC_ANGULO_X_PISO);
                    jlccRotacionY.setAngle(JLCC_ANGULO_Y_PISO);
                    jlccAplicarReferenciaAutomatica(jlccEncuadre);
                    jlccActualizarEtiquetasPiso();
                    jlccActualizarEtiquetasUnidad();
                });
    }

    private void jlccEntrarUnidad(BloqueMaqueta jlccBloque) {
        if (jlccMundo == null || jlccCamara == null || jlccNivelVista != JlccNivelVista.PISO) return;
        if (jlccTransicionActiva() || jlccBloque == null || jlccNivelEnfocado == null) return;
        if (!jlccNivelVista.jlccPuedeIrA(JlccNivelVista.UNIDAD)) return;
        if (jlccBloque.nivel() != jlccNivelEnfocado) return;

        jlccAnguloYPisoAntesUnidad = jlccRotacionY.getAngle();
        jlccNivelVista = JlccNivelVista.UNIDAD;
        jlccBloqueUnidad = jlccBloque;
        jlccDetenerTransicionVista();
        jlccOcultarMensajeEscena();
        jlccLimpiarSeleccion();
        jlccActualizarHover(null);

        for (Group jlccPlanta : jlccGruposPlanta.values()) jlccPlanta.setVisible(false);
        for (Group jlccPiso : jlccGruposPiso.values()) jlccPiso.setVisible(false);
        if (jlccEstructuraGeneral != null) jlccEstructuraGeneral.setVisible(false);

        try {
            jlccEsquemaActual =
                    JlccGeneradorEsquema.jlccGenerar(
                            jlccBloque.areaM2(),
                            jlccBloque.dormitorios(),
                            jlccBloque.banos(),
                            jlccBloque.amoblado());
            jlccCrearGrupoUnidad(jlccBloque, jlccEsquemaActual);
            jlccVistaPlano = false;
            jlccZoomPlano = 1;
            jlccAplicarVistaUnidad();
            jlccMostrarPanelUnidad(jlccBloque);
            jlccAjustarPanelUnidad();
            jlccDesplazarEscenaUnidad(true);
            jlccActualizarBarraNavegacion();
            jlccActualizarResumen(jlccBloquesActuales);
            jlccActualizarEtiquetasPiso();
            jlccOcultarEtiquetasUnidad();
            jlccOcultarEtiquetasAmbiente();

            double jlccAnchoDisponible = jlccAnchoEncuadreDisponible();
            Encuadre jlccEncuadre =
                    jlccEncuadreDePuntos(
                            jlccPuntosEsquema(jlccEsquemaActual),
                            JLCC_ANGULO_X_ESQUEMA,
                            JLCC_ANGULO_Y_ESQUEMA,
                            jlccAnchoDisponible);
            List<KeyValue> jlccValores = new ArrayList<>();
            jlccValores.add(
                    new KeyValue(
                            jlccRotacionX.angleProperty(), JLCC_ANGULO_X_ESQUEMA, Interpolator.EASE_BOTH));
            jlccValores.add(
                    new KeyValue(
                            jlccRotacionY.angleProperty(), JLCC_ANGULO_Y_ESQUEMA, Interpolator.EASE_BOTH));
            jlccValores.addAll(jlccValoresCamara(jlccEncuadre));

            jlccIniciarTransicion(
                    jlccValores,
                    () -> {
                        jlccRotacionX.setAngle(JLCC_ANGULO_X_ESQUEMA);
                        jlccRotacionY.setAngle(JLCC_ANGULO_Y_ESQUEMA);
                        jlccAplicarReferenciaAutomatica(jlccEncuadre);
                        jlccActualizarEtiquetasAmbiente();
                        if (jlccVistaPlano) jlccDibujarPlano();
                    });
        } catch (RuntimeException jlccError) {
            JLCC_log.error("No se pudo dibujar el esquema de la unidad {}", jlccBloque.id(), jlccError);
            jlccNotificador.jlccError("No se pudo dibujar el esquema de la unidad.");
            jlccLimpiarUnidad();
            jlccNivelVista = JlccNivelVista.PISO;
            if (jlccNivelEnfocado != null && jlccGruposPlanta.containsKey(jlccNivelEnfocado)) {
                jlccGruposPlanta.get(jlccNivelEnfocado).setVisible(true);
            }
            for (Group jlccPiso : jlccGruposPiso.values()) jlccPiso.setVisible(false);
            if (jlccEstructuraGeneral != null) jlccEstructuraGeneral.setVisible(false);
            jlccActualizarBarraNavegacion();
            jlccActualizarResumen(jlccBloquesActuales);
            jlccActualizarEtiquetasPiso();
            jlccOcultarEtiquetasUnidad();
            jlccMostrarMensajeEscena(
                    "No se pudo dibujar la unidad",
                    "El esquema referencial no está disponible. Vuelve a intentarlo o elige otra"
                        + " unidad.");
        }
    }

    private void jlccVolverAlPiso() {
        if (jlccMundo == null || jlccTransicionActiva() || jlccNivelVista != JlccNivelVista.UNIDAD) return;
        Integer jlccPiso = jlccNivelEnfocado;
        if (jlccPiso == null || !jlccGruposPlanta.containsKey(jlccPiso)) return;

        jlccDetenerTransicionVista();
        jlccOcultarMensajeEscena();
        jlccLimpiarUnidad();
        jlccNivelVista = JlccNivelVista.PISO;
        for (Group jlccPisoGrupo : jlccGruposPiso.values()) jlccPisoGrupo.setVisible(false);
        jlccGruposPlanta.get(jlccPiso).setVisible(true);
        if (jlccEstructuraGeneral != null) jlccEstructuraGeneral.setVisible(false);

        jlccActualizarBarraNavegacion();
        jlccActualizarResumen(jlccBloquesActuales);
        jlccActualizarEtiquetasPiso();
        jlccOcultarEtiquetasUnidad();

        PisoGeometria jlccPisoGeometria = jlccGeometria == null ? null : jlccGeometria.pisos().get(jlccPiso);
        JlccPlantaPiso.Distribucion jlccDistribucion = jlccDistribucionesPlanta.get(jlccPiso);
        if (jlccPisoGeometria == null || jlccDistribucion == null) return;
        Encuadre jlccEncuadre =
                jlccEncuadreDePuntos(
                        jlccPuntosPlanta(jlccPisoGeometria, jlccDistribucion),
                        JLCC_ANGULO_X_PISO,
                        jlccAnguloYPisoAntesUnidad);
        List<KeyValue> jlccValores = new ArrayList<>();
        jlccValores.add(new KeyValue(jlccRotacionX.angleProperty(), JLCC_ANGULO_X_PISO, Interpolator.EASE_BOTH));
        jlccValores.add(
                new KeyValue(
                        jlccRotacionY.angleProperty(), jlccAnguloYPisoAntesUnidad, Interpolator.EASE_BOTH));
        jlccValores.addAll(jlccValoresCamara(jlccEncuadre));

        jlccIniciarTransicion(
                jlccValores,
                () -> {
                    jlccRotacionX.setAngle(JLCC_ANGULO_X_PISO);
                    jlccRotacionY.setAngle(jlccAnguloYPisoAntesUnidad);
                    jlccAplicarReferenciaAutomatica(jlccEncuadre);
                    jlccActualizarEtiquetasPiso();
                    jlccActualizarEtiquetasUnidad();
                });
    }

    private void jlccLimpiarUnidad() {
        jlccDetenerTransicionVista();
        jlccLimpiarGrupoUnidad();
        jlccDesplazarEscenaUnidad(false);
        jlccBloqueUnidad = null;
        jlccEsquemaActual = null;
        jlccDetalleUnidad = null;
        jlccVistaPlano = false;
        jlccZoomPlano = 1;
        if (lienzoPlano != null) {
            lienzoPlano.setVisible(false);
            jlccLimpiarLienzo();
        }
        if (panelUnidad != null) {
            panelUnidad.setVisible(false);
            panelUnidad.setManaged(false);
        }
        if (cuerpoPanelUnidad != null) cuerpoPanelUnidad.getChildren().clear();
        if (alternadorVista != null) {
            alternadorVista.setVisible(false);
            alternadorVista.setManaged(false);
        }
        if (avisoEsquema != null) {
            avisoEsquema.setVisible(false);
            avisoEsquema.setManaged(false);
        }
        if (avisosUnidad != null) {
            avisosUnidad.setText("");
            avisosUnidad.setVisible(false);
            avisosUnidad.setManaged(false);
        }
        if (btnPlano != null) btnPlano.setSelected(false);
        if (btnTresD != null) btnTresD.setSelected(false);
    }

    private void jlccLimpiarGrupoUnidad() {
        if (jlccGrupoUnidad != null && jlccMundo != null) jlccMundo.getChildren().remove(jlccGrupoUnidad);
        jlccGrupoUnidad = null;
        if (capaEtiquetas != null) {
            for (EtiquetaAmbiente jlccEtiqueta : jlccEtiquetasAmbiente) {
                capaEtiquetas.getChildren().remove(jlccEtiqueta.nodo());
            }
        }
        jlccEtiquetasAmbiente.clear();
    }

    private void jlccDesplazarEscenaUnidad(boolean jlccActiva) {
        double jlccDesplazamiento =
                jlccActiva ? -(jlccAnchoPanelUnidadEfectivo() + JLCC_MARGEN_PANEL_UNIDAD) / 2 : 0;
        if (jlccSubEscena != null) jlccSubEscena.setTranslateX(jlccDesplazamiento);
        if (capaEtiquetas != null) capaEtiquetas.setTranslateX(jlccDesplazamiento);
    }

    private void jlccVolverAlEdificio() {
        if (jlccMundo == null || jlccTransicionActiva()) return;
        if (jlccNivelVista == JlccNivelVista.EDIFICIO) return;
        if (!jlccNivelVista.jlccPuedeIrA(JlccNivelVista.EDIFICIO)) return;
        if (jlccNivelVista == JlccNivelVista.UNIDAD) jlccLimpiarUnidad();
        jlccOcultarMensajeEscena();

        Integer jlccPisoSaliente = jlccNivelEnfocado;
        jlccNivelVista = JlccNivelVista.EDIFICIO;
        jlccNivelEnfocado = null;
        jlccLimpiarSeleccion();
        jlccActualizarHover(null);
        jlccActualizarBarraNavegacion();
        jlccActualizarResumen(jlccBloquesActuales);
        jlccOcultarEtiquetasUnidad();

        for (Group jlccPlanta : jlccGruposPlanta.values()) jlccPlanta.setVisible(false);
        for (Group jlccGrupo : jlccGruposPiso.values()) jlccGrupo.setVisible(true);
        if (jlccPisoSaliente != null && jlccGruposPiso.containsKey(jlccPisoSaliente)) {
            jlccGruposPiso.get(jlccPisoSaliente).setVisible(true);
            jlccGruposPiso.get(jlccPisoSaliente).setScaleY(JLCC_ESCALA_VISIBLE);
        }
        if (jlccEstructuraGeneral != null) {
            jlccEstructuraGeneral.setVisible(true);
            jlccEstructuraGeneral.setScaleY(JLCC_ESCALA_OCULTA);
        }

        List<KeyValue> jlccValores = new ArrayList<>();
        for (Map.Entry<Integer, Group> jlccEntrada : jlccGruposPiso.entrySet()) {
            if (jlccPisoSaliente != null && jlccEntrada.getKey().equals(jlccPisoSaliente)) continue;
            jlccValores.add(
                    new KeyValue(
                            jlccEntrada.getValue().scaleYProperty(),
                            JLCC_ESCALA_VISIBLE,
                            Interpolator.EASE_BOTH));
        }
        if (jlccEstructuraGeneral != null) {
            jlccValores.add(
                    new KeyValue(
                            jlccEstructuraGeneral.scaleYProperty(),
                            JLCC_ESCALA_VISIBLE,
                            Interpolator.EASE_BOTH));
        }
        jlccValores.add(
                new KeyValue(jlccRotacionX.angleProperty(), jlccAnguloXEdificio, Interpolator.EASE_BOTH));
        jlccValores.add(
                new KeyValue(jlccRotacionY.angleProperty(), jlccAnguloYEdificio, Interpolator.EASE_BOTH));
        Encuadre jlccEncuadre = jlccEncuadreDePuntos(jlccPuntosEncuadre, jlccAnguloXEdificio, jlccAnguloYEdificio);
        jlccValores.addAll(jlccValoresCamara(jlccEncuadre));

        jlccIniciarTransicion(
                jlccValores,
                () -> {
                    jlccRotacionX.setAngle(jlccAnguloXEdificio);
                    jlccRotacionY.setAngle(jlccAnguloYEdificio);
                    jlccAplicarReferenciaAutomatica(jlccEncuadre);
                    jlccActualizarEtiquetasPiso();
                    jlccActualizarEtiquetasUnidad();
                });
    }

    private void jlccReiniciarNavegacion() {
        boolean jlccEstabaFueraDelEdificio = jlccNivelVista != JlccNivelVista.EDIFICIO;
        jlccDetenerTransicion();
        jlccOcultarMensajeEscena();
        jlccLimpiarUnidad();
        if (jlccEstabaFueraDelEdificio && jlccRotacionX != null && jlccRotacionY != null) {
            jlccRotacionX.setAngle(jlccAnguloXEdificio);
            jlccRotacionY.setAngle(jlccAnguloYEdificio);
        }
        jlccNivelVista = JlccNivelVista.EDIFICIO;
        jlccNivelEnfocado = null;
        jlccRestaurarPisos();
        jlccActualizarBarraNavegacion();
        jlccActualizarEtiquetasPiso();
        jlccActualizarEtiquetasUnidad();
    }

    private void jlccRestaurarPisos() {
        for (Group jlccPlanta : jlccGruposPlanta.values()) jlccPlanta.setVisible(false);
        for (Group jlccGrupo : jlccGruposPiso.values()) {
            jlccGrupo.setVisible(true);
            jlccGrupo.setScaleY(JLCC_ESCALA_VISIBLE);
        }
        if (jlccEstructuraGeneral != null) {
            jlccEstructuraGeneral.setVisible(true);
            jlccEstructuraGeneral.setScaleY(JLCC_ESCALA_VISIBLE);
        }
        jlccArrastrando = false;
    }

    private void jlccActualizarBarraNavegacion() {
        if (barraNavegacion == null) return;
        boolean jlccVisible = jlccNivelVista != JlccNivelVista.EDIFICIO && jlccNivelEnfocado != null;
        barraNavegacion.setVisible(jlccVisible);
        barraNavegacion.setManaged(jlccVisible);
        if (alternadorVista != null) {
            boolean jlccEnUnidad = jlccVisible && jlccNivelVista == JlccNivelVista.UNIDAD && jlccBloqueUnidad != null;
            alternadorVista.setVisible(jlccEnUnidad);
            alternadorVista.setManaged(jlccEnUnidad);
        }
        if (!jlccVisible) {
            if (avisoEsquema != null) {
                avisoEsquema.setVisible(false);
                avisoEsquema.setManaged(false);
            }
            jlccActualizarTextoAyuda();
            return;
        }
        String jlccNivel = jlccEtiquetaNivel(jlccNivelEnfocado);
        if (jlccNivelVista == JlccNivelVista.UNIDAD && jlccBloqueUnidad != null) {
            if (migaNavegacion != null) {
                migaNavegacion.setText(
                        "Edificio › " + jlccNivel + " › Unidad " + jlccNumeroUnidad(jlccBloqueUnidad));
            }
            if (btnVolverEdificio != null) btnVolverEdificio.setText("Volver al piso");
            if (avisoEsquema != null) {
                avisoEsquema.setVisible(true);
                avisoEsquema.setManaged(true);
            }
            jlccActualizarBotonesVista();
        } else {
            if (migaNavegacion != null) {
                migaNavegacion.setText("Edificio › " + jlccNivel);
            }
            if (btnVolverEdificio != null) {
                btnVolverEdificio.setText("Volver al edificio");
            }
            if (avisoEsquema != null) {
                avisoEsquema.setVisible(false);
                avisoEsquema.setManaged(false);
            }
        }
        jlccActualizarTextoAyuda();
    }

    private void jlccActualizarTextoAyuda() {
        if (ayuda == null) return;
        ayuda.setText(
                switch (jlccNivelVista) {
                    case EDIFICIO -> "Arrastra para rotar · Usa la rueda para acercar";
                    case PISO -> "Arrastra para girar la planta · Usa la rueda para acercar";
                    case UNIDAD ->
                            jlccVistaPlano
                                    ? "Vista de plano · Rueda para acercar · Cambia a 3D para girar"
                                    : "Arrastra para girar el esquema · Usa la rueda para acercar";
                });
    }

    private boolean jlccTransicionActiva() {
        return jlccTransicion != null;
    }

    private void jlccDetenerTransicion() {
        if (jlccTransicion == null) return;
        Timeline jlccActiva = jlccTransicion;
        jlccTransicion = null;
        jlccActiva.stop();
    }

    private void jlccIniciarTransicion(List<KeyValue> jlccValores, Runnable jlccAlTerminar) {
        jlccDetenerTransicion();
        if (jlccValores.isEmpty()) {
            jlccAlTerminar.run();
            return;
        }
        Timeline jlccNueva =
                new Timeline(new KeyFrame(JLCC_DURACION_TRANSICION, jlccValores.toArray(new KeyValue[0])));
        jlccTransicion = jlccNueva;
        jlccNueva.setOnFinished(
                evento -> {
                    if (jlccTransicion != jlccNueva) return;
                    jlccTransicion = null;
                    jlccAlTerminar.run();
                });
        jlccNueva.play();
    }

    private Encuadre jlccEncuadreDePuntos(List<Point3D> jlccPuntos, double jlccAnguloX, double jlccAnguloY) {
        return jlccEncuadreDePuntos(
                jlccPuntos, jlccAnguloX, jlccAnguloY, jlccSubEscena == null ? 0 : jlccSubEscena.getWidth());
    }

    private Encuadre jlccEncuadreDePuntos(
            List<Point3D> jlccPuntos, double jlccAnguloX, double jlccAnguloY, double jlccAnchoDisponible) {
        if (jlccCamara == null || jlccSubEscena == null || jlccPuntos.isEmpty()) return null;
        double jlccAltoVista = jlccSubEscena.getHeight();
        if (jlccAnchoDisponible < 60 || jlccAltoVista < 60) return null;
        return jlccCalcularEncuadre(
                jlccProyectarConRotacion(jlccPuntos, jlccAnguloX, jlccAnguloY),
                jlccAnchoDisponible,
                jlccAltoVista,
                jlccCamara.getFieldOfView(),
                jlccCamara.getNearClip(),
                JLCC_RELLENO_ENCUADRE);
    }

    private List<KeyValue> jlccValoresCamara(Encuadre jlccEncuadre) {
        if (jlccEncuadre == null || jlccCamara == null || jlccSubEscena == null) return List.of();
        double jlccAnchoVista = jlccSubEscena.getWidth();
        double jlccAltoVista = jlccSubEscena.getHeight();
        double jlccFoco = jlccAltoVista / (2 * Math.tan(Math.toRadians(jlccCamara.getFieldOfView() / 2)));
        return List.of(
                new KeyValue(
                        jlccCamara.translateXProperty(),
                        jlccEncuadre.ojoX() - jlccAnchoVista / 2,
                        Interpolator.EASE_BOTH),
                new KeyValue(
                        jlccCamara.translateYProperty(),
                        jlccEncuadre.ojoY() - jlccAltoVista / 2,
                        Interpolator.EASE_BOTH),
                new KeyValue(
                        jlccCamara.translateZProperty(),
                        jlccEncuadre.centroZ() - jlccEncuadre.distancia() + jlccFoco,
                        Interpolator.EASE_BOTH));
    }

    @FXML
    private void jlccOnRestablecerVista() {
        if (jlccRotacionX == null || jlccRotacionY == null || jlccCamara == null) return;
        jlccDetenerTransicion();
        jlccOcultarMensajeEscena();
        jlccLimpiarUnidad();
        jlccNivelVista = JlccNivelVista.EDIFICIO;
        jlccNivelEnfocado = null;
        jlccRestaurarPisos();
        jlccActualizarBarraNavegacion();
        jlccActualizarResumen(jlccBloquesActuales);
        jlccAnguloXEdificio = JLCC_ANGULO_X_INICIAL;
        jlccAnguloYEdificio = JLCC_ANGULO_Y_INICIAL;
        jlccRotacionX.setAngle(JLCC_ANGULO_X_INICIAL);
        jlccRotacionY.setAngle(JLCC_ANGULO_Y_INICIAL);
        jlccEncuadrarAutomatico();
        jlccActualizarEtiquetasPiso();
        jlccActualizarEtiquetasUnidad();
        jlccActualizarHover(null);
        jlccLimpiarSeleccion();
    }

    @FXML
    private void jlccOnCambiarModo() {
        if (selectorModo == null) return;
        JlccModoVista jlccNuevo = JlccModoVista.jlccDesdeEtiqueta(selectorModo.getValue());
        if (jlccNuevo == jlccModoVista) return;
        jlccModoVista = jlccNuevo;
        jlccPreferencias.put(JLCC_PREFERENCIA_MODO, jlccNuevo.name());
        jlccFiltroClave = null;
        jlccFiltroActivo = false;
        jlccFiltroFijo = false;
        jlccActualizarHover(null);
        jlccAplicarFiltroMateriales();
        jlccActualizarResumen(jlccBloquesActuales);
        jlccConfigurarLeyenda();
        if (jlccNivelVista == JlccNivelVista.UNIDAD && jlccDetalleUnidad != null && cuerpoPanelUnidad != null) {
            cuerpoPanelUnidad.getChildren().setAll(jlccCrearContenidoDetalle(jlccDetalleUnidad, true));
        }
    }

    private void jlccConfigurarSelectorModo() {
        if (selectorModo == null) return;
        selectorModo.getItems().setAll(JlccModoVista.jlccEtiquetas());
        selectorModo.setValue(jlccModoVista.jlccEtiqueta());
    }

    private void jlccConfigurarLeyenda() {
        jlccItemsLeyenda.clear();
        if (leyenda == null) return;
        leyenda.getChildren().clear();
        switch (jlccModoVista) {
            case ESTADO -> jlccConfigurarLeyendaEstado();
            case MORA -> jlccConfigurarLeyendaMora();
            case RENTA -> jlccConfigurarLeyendaRenta();
            case VENCIMIENTO -> jlccConfigurarLeyendaVencimiento();
        }
        jlccActualizarEstiloLeyenda();
    }

    private void jlccConfigurarLeyendaEstado() {
        leyenda.getChildren().add(jlccTituloLeyenda("Estados"));
        leyenda.getChildren()
                .addAll(
                        jlccItemLeyenda(
                                "Disponible", "maqueta-punto-disponible", jlccClaveEstado(DISPONIBLE)),
                        jlccItemLeyenda("Alquilado", "maqueta-punto-alquilado", jlccClaveEstado(ALQUILADO)),
                        jlccItemLeyenda(
                                "Mantenimiento",
                                "maqueta-punto-mantenimiento",
                                jlccClaveEstado(MANTENIMIENTO)),
                        jlccItemLeyenda("Inactivo", "maqueta-punto-inactivo", jlccClaveEstado(INACTIVO)));
        Region jlccEspacio = new Region();
        HBox.setHgrow(jlccEspacio, Priority.ALWAYS);
        leyenda.getChildren().add(jlccEspacio);
        leyenda.getChildren().add(jlccTituloLeyenda("Mora"));
        leyenda.getChildren()
                .addAll(
                        jlccItemLeyenda(
                                "Atraso leve",
                                "maqueta-punto-atraso",
                                jlccClaveMora(BloqueMaqueta.CategoriaMora.ATRASO_LEVE)),
                        jlccItemLeyenda(
                                "Moroso",
                                "maqueta-punto-moroso",
                                jlccClaveMora(BloqueMaqueta.CategoriaMora.MOROSO)));
    }

    private void jlccConfigurarLeyendaMora() {
        leyenda.getChildren().add(jlccTituloLeyenda("Mora"));
        for (BloqueMaqueta.CategoriaMora jlccCategoria : BloqueMaqueta.CategoriaMora.values()) {
            leyenda.getChildren()
                    .add(
                            jlccItemLeyenda(
                                    jlccCategoria.getEtiqueta(),
                                    jlccClasePuntoMora(jlccCategoria),
                                    jlccClaveMora(jlccCategoria)));
        }
    }

    private void jlccConfigurarLeyendaRenta() {
        leyenda.getChildren().add(jlccTituloLeyenda("Renta (S/)"));
        if (jlccRentaMinima == null || jlccRentaMaxima == null) {
            Label jlccVacio = new Label("Sin rentas registradas");
            jlccVacio.getStyleClass().add("maqueta-leyenda-etiqueta");
            jlccVacio.setMinHeight(21);
            jlccVacio.setPrefHeight(21);
            leyenda.getChildren().add(jlccVacio);
            return;
        }
        if (jlccRentaMinima.compareTo(jlccRentaMaxima) == 0) {
            int jlccTono =
                    JlccMaquetaService.jlccTonoRenta(
                            jlccRentaMinima, jlccRentaMinima, jlccRentaMaxima, JlccMaquetaService.JLCC_TONOS_RENTA);
            leyenda.getChildren().add(jlccItemRentaUnica(jlccRentaMinima, jlccTono));
            return;
        }

        HBox jlccEscala = new HBox();
        jlccEscala.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < JLCC_COLORES_RENTA.size(); i++) {
            Region jlccTono = new Region();
            jlccTono.getStyleClass().add("maqueta-renta-tono");
            jlccTono.setStyle("-fx-background-color: " + jlccColorHex(JLCC_COLORES_RENTA.get(i)) + ";");
            StackPane jlccCelda = new StackPane(jlccTono);
            jlccCelda.getStyleClass().addAll("maqueta-leyenda-item", "maqueta-renta-celda");
            jlccRegistrarItemLeyenda(jlccCelda, jlccClaveRenta(i));
            jlccEscala.getChildren().add(jlccCelda);
        }
        Label jlccEtiquetas =
                new Label(
                        "Mín. "
                                + JlccTablas.jlccMoneda(jlccRentaMinima)
                                + "  ·  Máx. "
                                + JlccTablas.jlccMoneda(jlccRentaMaxima));
        jlccEtiquetas.getStyleClass().add("maqueta-leyenda-etiqueta");
        leyenda.getChildren().addAll(jlccEscala, jlccEtiquetas);
        if (jlccHayBloquesSinRenta()) {
            leyenda.getChildren()
                    .add(jlccItemLeyenda("Sin renta", "maqueta-punto-sin-renta", jlccClaveRenta(null)));
        }
    }

    private void jlccConfigurarLeyendaVencimiento() {
        leyenda.getChildren().add(jlccTituloLeyenda("Vencimiento"));
        for (BloqueMaqueta.CategoriaVencimiento jlccCategoria :
                BloqueMaqueta.CategoriaVencimiento.values()) {
            leyenda.getChildren()
                    .add(
                            jlccItemLeyenda(
                                    jlccCategoria.getEtiqueta(),
                                    jlccClasePuntoVencimiento(jlccCategoria),
                                    jlccClaveVencimiento(jlccCategoria)));
        }
    }

    private Label jlccTituloLeyenda(String jlccTexto) {
        Label jlccTitulo = new Label(jlccTexto);
        jlccTitulo.getStyleClass().add("maqueta-leyenda-titulo");
        return jlccTitulo;
    }

    private HBox jlccItemLeyenda(String jlccTexto, String jlccClasePunto, String jlccClave) {
        Region jlccPunto = new Region();
        jlccPunto.getStyleClass().addAll("maqueta-punto", jlccClasePunto);
        Label jlccEtiqueta = new Label(jlccTexto);
        jlccEtiqueta.getStyleClass().add("maqueta-leyenda-etiqueta");
        HBox jlccItem = new HBox(6, jlccPunto, jlccEtiqueta);
        jlccItem.setAlignment(Pos.CENTER_LEFT);
        jlccItem.getStyleClass().add("maqueta-leyenda-item");
        jlccRegistrarItemLeyenda(jlccItem, jlccClave);
        return jlccItem;
    }

    private HBox jlccItemRentaUnica(BigDecimal jlccRenta, int jlccTono) {
        int jlccIndice = jlccLimitarIndiceRenta(jlccTono);
        Region jlccMuestra = new Region();
        jlccMuestra.getStyleClass().add("maqueta-renta-tono");
        jlccMuestra.setStyle("-fx-background-color: " + jlccColorHex(JLCC_COLORES_RENTA.get(jlccIndice)) + ";");
        Label jlccEtiqueta = new Label(JlccTablas.jlccMoneda(jlccRenta));
        jlccEtiqueta.getStyleClass().add("maqueta-leyenda-etiqueta");
        HBox jlccItem = new HBox(6, jlccMuestra, jlccEtiqueta);
        jlccItem.setAlignment(Pos.CENTER_LEFT);
        jlccItem.getStyleClass().add("maqueta-leyenda-item");
        jlccRegistrarItemLeyenda(jlccItem, jlccClaveRenta(jlccIndice));
        return jlccItem;
    }

    private void jlccRegistrarItemLeyenda(Node jlccItem, String jlccClave) {
        jlccItemsLeyenda.add(new ItemLeyenda(jlccItem, jlccClave));
        jlccItem.setOnMouseEntered(
                evento -> {
                    if (!jlccFiltroFijo) jlccAplicarFiltro(jlccClave, false);
                });
        jlccItem.setOnMouseExited(
                evento -> {
                    if (!jlccFiltroFijo) jlccAplicarFiltro(null, false);
                });
        jlccItem.setOnMouseClicked(
                evento -> {
                    boolean jlccMismoFiltroFijo =
                            jlccFiltroFijo && jlccFiltroActivo && jlccClave.equals(jlccFiltroClave);
                    if (jlccMismoFiltroFijo) jlccAplicarFiltro(null, false);
                    else jlccAplicarFiltro(jlccClave, true);
                    evento.consume();
                });
    }

    private void jlccAplicarFiltro(String jlccClave, boolean jlccFijo) {
        jlccFiltroClave = jlccClave;
        jlccFiltroActivo = jlccClave != null;
        jlccFiltroFijo = jlccFijo && jlccClave != null;
        jlccAplicarFiltroMateriales();
        jlccActualizarEstiloLeyenda();
    }

    private void jlccAplicarFiltroMateriales() {
        Set<BloqueMaqueta> jlccAtenuados = new HashSet<>();
        for (BloqueMaqueta jlccBloque : jlccNodosPorBloque.keySet()) {
            boolean jlccCoincide = !jlccFiltroActivo || jlccCoincideFiltro(jlccBloque);
            jlccAplicarMateriales(jlccBloque, jlccCoincide);
            if (jlccFiltroActivo && !jlccCoincide) jlccAtenuados.add(jlccBloque);
        }
        if (jlccFiltroActivo) jlccOrdenarBloques(jlccAtenuados);
    }

    private boolean jlccCoincideFiltro(BloqueMaqueta jlccBloque) {
        if (jlccFiltroClave == null) return true;
        if (jlccModoVista == JlccModoVista.ESTADO && jlccFiltroClave.startsWith("MORA:")) {
            return jlccFiltroClave.equals(jlccClaveMora(jlccBloque.categoriaMora()));
        }
        return jlccFiltroClave.equals(jlccClaveCategoria(jlccBloque));
    }

    private String jlccClaveCategoria(BloqueMaqueta jlccBloque) {
        return switch (jlccModoVista) {
            case ESTADO -> jlccClaveEstado(jlccBloque.estado());
            case MORA -> jlccClaveMora(jlccBloque.categoriaMora());
            case RENTA -> jlccClaveRenta(jlccTonoRenta(jlccBloque));
            case VENCIMIENTO -> jlccClaveVencimiento(jlccBloque.categoriaVencimiento());
        };
    }

    private void jlccAplicarMateriales(BloqueMaqueta jlccBloque, boolean jlccCoincide) {
        PhongMaterial jlccMaterial = jlccMaterialParaModo(jlccBloque, jlccCoincide);
        Box jlccCaja = jlccCajasPorBloque.get(jlccBloque);
        if (jlccCaja != null && jlccMaterial != null) jlccCaja.setMaterial(jlccMaterial);
        Box jlccPlaca = jlccCajasPlantaPorBloque.get(jlccBloque);
        if (jlccPlaca != null && jlccMaterial != null) jlccPlaca.setMaterial(jlccMaterial);

        Cylinder jlccMarca = jlccMarcasPorBloque.get(jlccBloque);
        Cylinder jlccMarcaPlanta = jlccMarcasPlantaPorBloque.get(jlccBloque);
        boolean jlccMostrarMarca = jlccModoVista == JlccModoVista.ESTADO;
        if (jlccMarca != null) jlccMarca.setVisible(jlccMostrarMarca);
        if (jlccMarcaPlanta != null) jlccMarcaPlanta.setVisible(jlccMostrarMarca);
        Color jlccColor = jlccColorMora(jlccBloque.semaforo());
        if (jlccColor == null) return;
        PhongMaterial jlccMaterialMarca = jlccCoincide ? jlccMaterialMora(jlccColor) : jlccMaterialMoraAtenuado(jlccColor);
        if (jlccMaterialMarca == null) return;
        if (jlccMarca != null) jlccMarca.setMaterial(jlccMaterialMarca);
        if (jlccMarcaPlanta != null) jlccMarcaPlanta.setMaterial(jlccMaterialMarca);
    }

    private PhongMaterial jlccMaterialParaModo(BloqueMaqueta jlccBloque, boolean jlccCoincide) {
        return switch (jlccModoVista) {
            case ESTADO ->
                    jlccCoincide
                            ? jlccMaterialPara(jlccBloque.estado())
                            : jlccMaterialAtenuadoPara(jlccBloque.estado());
            case MORA -> {
                BloqueMaqueta.CategoriaMora jlccCategoria = jlccBloque.categoriaMora();
                yield jlccCoincide
                        ? jlccMaterialesCategoriaMora.get(jlccCategoria)
                        : jlccMaterialesCategoriaMoraAtenuados.get(jlccCategoria);
            }
            case RENTA -> {
                Integer jlccTono = jlccTonoRenta(jlccBloque);
                if (jlccTono == null) {
                    yield jlccCoincide ? jlccMaterialSinDato : jlccMaterialSinDatoAtenuado;
                }
                yield jlccCoincide ? jlccMaterialesRenta.get(jlccTono) : jlccMaterialesRentaAtenuados.get(jlccTono);
            }
            case VENCIMIENTO -> {
                BloqueMaqueta.CategoriaVencimiento jlccCategoria = jlccBloque.categoriaVencimiento();
                yield jlccCoincide
                        ? jlccMaterialesVencimiento.get(jlccCategoria)
                        : jlccMaterialesVencimientoAtenuados.get(jlccCategoria);
            }
        };
    }

    private Integer jlccTonoRenta(BloqueMaqueta jlccBloque) {
        if (jlccBloque.rentaReferencial() == null) return null;
        int jlccTono =
                JlccMaquetaService.jlccTonoRenta(
                        jlccBloque.rentaReferencial(),
                        jlccRentaMinima,
                        jlccRentaMaxima,
                        JlccMaquetaService.JLCC_TONOS_RENTA);
        return jlccTono < 0 ? null : jlccTono;
    }

    private boolean jlccHayBloquesSinRenta() {
        return jlccBloquesActuales.stream().anyMatch(bloque -> bloque.rentaReferencial() == null);
    }

    private static String jlccClaveEstado(EstadoInmueble jlccEstado) {
        return "ESTADO:" + jlccEstado.name();
    }

    private static String jlccClaveMora(BloqueMaqueta.CategoriaMora jlccCategoria) {
        return "MORA:" + jlccCategoria.name();
    }

    private static String jlccClaveVencimiento(BloqueMaqueta.CategoriaVencimiento jlccCategoria) {
        return "VENCIMIENTO:" + jlccCategoria.name();
    }

    private static String jlccClaveRenta(Integer jlccTono) {
        return jlccTono == null ? "RENTA:SIN" : "RENTA:" + jlccTono;
    }

    private static String jlccClasePuntoMora(BloqueMaqueta.CategoriaMora jlccCategoria) {
        return switch (jlccCategoria) {
            case AL_DIA -> "maqueta-punto-al-dia";
            case ATRASO_LEVE -> "maqueta-punto-atraso";
            case MOROSO -> "maqueta-punto-moroso";
            case SIN_MORA -> "maqueta-punto-sin-mora";
        };
    }

    private static String jlccClasePuntoVencimiento(BloqueMaqueta.CategoriaVencimiento jlccCategoria) {
        return switch (jlccCategoria) {
            case VENCIDO -> "maqueta-punto-vencido";
            case HASTA_30_DIAS -> "maqueta-punto-por-vencer";
            case DE_31_A_90_DIAS -> "maqueta-punto-entre-31-y-90";
            case MAS_DE_90_DIAS -> "maqueta-punto-mas-de-90";
            case SIN_CONTRATO -> "maqueta-punto-sin-contrato";
        };
    }

    private static int jlccLimitarIndiceRenta(int jlccTono) {
        return Math.max(0, Math.min(JLCC_COLORES_RENTA.size() - 1, jlccTono));
    }

    private static String jlccColorHex(Color jlccColor) {
        return String.format(
                "#%02X%02X%02X",
                (int) Math.round(jlccColor.getRed() * 255),
                (int) Math.round(jlccColor.getGreen() * 255),
                (int) Math.round(jlccColor.getBlue() * 255));
    }

    private void jlccOrdenarBloques(Set<BloqueMaqueta> jlccAtenuados) {
        if (jlccAtenuados.isEmpty()) return;
        Set<Node> jlccNodosAtenuados = new HashSet<>();
        for (BloqueMaqueta jlccBloque : jlccAtenuados) {
            Node jlccBloque3d = jlccNodosPorBloque.get(jlccBloque);
            if (jlccBloque3d != null) jlccNodosAtenuados.add(jlccBloque3d);
            Node jlccRecinto = jlccNodosPlantaPorBloque.get(jlccBloque);
            if (jlccRecinto != null) jlccNodosAtenuados.add(jlccRecinto);
        }
        for (Group jlccGrupo : jlccGruposBloques.values()) jlccReordenarAtenuados(jlccGrupo, jlccNodosAtenuados);
        for (Group jlccGrupo : jlccGruposRecintos.values()) jlccReordenarAtenuados(jlccGrupo, jlccNodosAtenuados);
    }

    private void jlccReordenarAtenuados(Group jlccGrupo, Set<Node> jlccAtenuados) {
        List<Node> jlccActuales = new ArrayList<>(jlccGrupo.getChildren());
        List<Node> jlccReordenados = new ArrayList<>(jlccActuales.size());
        for (Node jlccNodo : jlccActuales) {
            if (!jlccAtenuados.contains(jlccNodo)) jlccReordenados.add(jlccNodo);
        }
        for (Node jlccNodo : jlccActuales) {
            if (jlccAtenuados.contains(jlccNodo)) jlccReordenados.add(jlccNodo);
        }
        if (!jlccReordenados.equals(jlccActuales)) jlccGrupo.getChildren().setAll(jlccReordenados);
    }

    private void jlccActualizarEstiloLeyenda() {
        for (ItemLeyenda jlccItem : jlccItemsLeyenda) {
            boolean jlccActivo = jlccFiltroActivo && jlccItem.clave().equals(jlccFiltroClave);
            jlccItem.nodo().getStyleClass().remove("filtro-activo");
            if (jlccActivo) jlccItem.nodo().getStyleClass().add("filtro-activo");
        }
    }

    private void jlccActualizarResumen(List<BloqueMaqueta> jlccBloques) {
        if (resumen == null) return;
        if (jlccNivelVista == JlccNivelVista.UNIDAD && jlccBloqueUnidad != null) {
            StringBuilder jlccTexto = new StringBuilder("Unidad ").append(jlccNumeroUnidad(jlccBloqueUnidad));
            if (jlccBloqueUnidad.tipo() != null && !jlccBloqueUnidad.tipo().isBlank()) {
                jlccTexto.append(" · ").append(jlccBloqueUnidad.tipo());
            }
            if (JlccPlantaPiso.jlccTieneArea(jlccBloqueUnidad)) {
                jlccTexto.append(" · ")
                        .append(jlccBloqueUnidad.areaM2().stripTrailingZeros().toPlainString())
                        .append(" m²");
            }
            if (jlccEsquemaActual != null && jlccEsquemaActual.areaEstimada()) {
                jlccTexto.append(" · área estimada");
            }
            if (jlccEsquemaActual != null && jlccEsquemaActual.amoblado()) {
                jlccTexto.append(" · amoblado");
            }
            if (jlccEsquemaActual != null && jlccEsquemaActual.ajustado()) {
                jlccTexto.append(" · distribución ajustada");
            }
            resumen.setText(jlccTexto.toString());
            return;
        }
        if (jlccBloques == null || jlccBloques.isEmpty()) {
            resumen.setText("Sin inmuebles ubicados");
            return;
        }
        List<BloqueMaqueta> jlccAmbito = jlccBloquesEnAmbito(jlccBloques);
        String jlccPrefijo =
                jlccNivelVista == JlccNivelVista.PISO && jlccNivelEnfocado != null
                        ? jlccEtiquetaNivel(jlccNivelEnfocado) + " · "
                        : "";
        resumen.setText(
                jlccPrefijo
                        + switch (jlccModoVista) {
                            case ESTADO -> jlccResumenEstado(jlccAmbito);
                            case MORA -> jlccResumenMora(jlccAmbito);
                            case RENTA -> jlccResumenRenta(jlccAmbito);
                            case VENCIMIENTO -> jlccResumenVencimiento(jlccAmbito);
                        });
    }

    private List<BloqueMaqueta> jlccBloquesEnAmbito(List<BloqueMaqueta> jlccBloques) {
        if (jlccBloques == null) return List.of();
        if (jlccNivelVista == JlccNivelVista.PISO && jlccNivelEnfocado != null) {
            return jlccBloques.stream().filter(bloque -> bloque.nivel() == jlccNivelEnfocado).toList();
        }
        return jlccBloques;
    }

    private String jlccResumenEstado(List<BloqueMaqueta> jlccBloques) {
        long jlccDisponibles = jlccBloques.stream().filter(b -> b.estado() == DISPONIBLE).count();
        long jlccAlquilados = jlccBloques.stream().filter(b -> b.estado() == ALQUILADO).count();
        long jlccMantenimiento = jlccBloques.stream().filter(b -> b.estado() == MANTENIMIENTO).count();
        long jlccInactivos = jlccBloques.stream().filter(b -> b.estado() == INACTIVO).count();
        long jlccMorosos =
                jlccBloques.stream().filter(b -> b.semaforo() == SemaforoMorosidad.MOROSO).count();
        long jlccAtraso =
                jlccBloques.stream().filter(b -> b.semaforo() == SemaforoMorosidad.ATRASO_LEVE).count();
        return jlccContar(jlccBloques.size(), "inmueble", "inmuebles")
                + " · "
                + jlccContar(jlccDisponibles, "disponible", "disponibles")
                + " · "
                + jlccContar(jlccAlquilados, "alquilado", "alquilados")
                + " · "
                + jlccMantenimiento
                + " en mantenimiento"
                + " · "
                + jlccContar(jlccInactivos, "inactivo", "inactivos")
                + " · "
                + jlccContar(jlccMorosos, "moroso", "morosos")
                + " · "
                + jlccContar(jlccAtraso, "con atraso leve", "con atraso leve");
    }

    private String jlccResumenMora(List<BloqueMaqueta> jlccBloques) {
        long jlccAlDia = jlccContarCategoriaMora(jlccBloques, BloqueMaqueta.CategoriaMora.AL_DIA);
        long jlccAtraso = jlccContarCategoriaMora(jlccBloques, BloqueMaqueta.CategoriaMora.ATRASO_LEVE);
        long jlccMorosos = jlccContarCategoriaMora(jlccBloques, BloqueMaqueta.CategoriaMora.MOROSO);
        long jlccSinMora = jlccContarCategoriaMora(jlccBloques, BloqueMaqueta.CategoriaMora.SIN_MORA);
        return jlccContar(jlccBloques.size(), "inmueble", "inmuebles")
                + " · "
                + jlccContar(jlccAlDia, "al día", "al día")
                + " · "
                + jlccContar(jlccAtraso, "con atraso leve", "con atraso leve")
                + " · "
                + jlccContar(jlccMorosos, "moroso", "morosos")
                + " · "
                + jlccContar(jlccSinMora, "sin mora", "sin mora");
    }

    private String jlccResumenRenta(List<BloqueMaqueta> jlccBloques) {
        List<BigDecimal> jlccRentas =
                jlccBloques.stream()
                        .map(BloqueMaqueta::rentaReferencial)
                        .filter(renta -> renta != null)
                        .toList();
        String jlccBase = jlccContar(jlccBloques.size(), "inmueble", "inmuebles");
        if (jlccRentas.isEmpty()) return jlccBase + " · Sin rentas registradas";

        BigDecimal jlccMinima = jlccRentas.get(0);
        BigDecimal jlccMaxima = jlccRentas.get(0);
        BigDecimal jlccSuma = BigDecimal.ZERO;
        for (BigDecimal jlccRenta : jlccRentas) {
            if (jlccRenta.compareTo(jlccMinima) < 0) jlccMinima = jlccRenta;
            if (jlccRenta.compareTo(jlccMaxima) > 0) jlccMaxima = jlccRenta;
            jlccSuma = jlccSuma.add(jlccRenta);
        }
        BigDecimal jlccPromedio =
                jlccSuma.divide(BigDecimal.valueOf(jlccRentas.size()), 2, RoundingMode.HALF_UP);
        long jlccSinRenta = jlccBloques.size() - jlccRentas.size();
        String jlccTexto =
                jlccBase
                        + " · "
                        + jlccContar(jlccRentas.size(), "con renta", "con renta")
                        + " · Mín. "
                        + JlccTablas.jlccMoneda(jlccMinima)
                        + " · Máx. "
                        + JlccTablas.jlccMoneda(jlccMaxima)
                        + " · Prom. "
                        + JlccTablas.jlccMoneda(jlccPromedio);
        if (jlccSinRenta > 0) jlccTexto += " · " + jlccContar(jlccSinRenta, "sin renta", "sin renta");
        return jlccTexto;
    }

    private String jlccResumenVencimiento(List<BloqueMaqueta> jlccBloques) {
        long jlccVencidos =
                jlccContarCategoriaVencimiento(jlccBloques, BloqueMaqueta.CategoriaVencimiento.VENCIDO);
        long jlccHasta30 =
                jlccContarCategoriaVencimiento(
                        jlccBloques, BloqueMaqueta.CategoriaVencimiento.HASTA_30_DIAS);
        long jlccEntre31y90 =
                jlccContarCategoriaVencimiento(
                        jlccBloques, BloqueMaqueta.CategoriaVencimiento.DE_31_A_90_DIAS);
        long jlccMasDe90 =
                jlccContarCategoriaVencimiento(
                        jlccBloques, BloqueMaqueta.CategoriaVencimiento.MAS_DE_90_DIAS);
        long jlccSinContrato =
                jlccContarCategoriaVencimiento(
                        jlccBloques, BloqueMaqueta.CategoriaVencimiento.SIN_CONTRATO);
        return jlccContar(jlccBloques.size(), "inmueble", "inmuebles")
                + " · "
                + jlccContar(jlccVencidos, "vencido", "vencidos")
                + " · "
                + jlccContar(jlccHasta30, "vence en 30 días o menos", "vencen en 30 días o menos")
                + " · "
                + jlccContar(jlccEntre31y90, "entre 31 y 90 días", "entre 31 y 90 días")
                + " · "
                + jlccContar(jlccMasDe90, "con más de 90 días", "con más de 90 días")
                + " · "
                + jlccContar(jlccSinContrato, "sin contrato", "sin contrato");
    }

    private long jlccContarCategoriaMora(
            List<BloqueMaqueta> jlccBloques, BloqueMaqueta.CategoriaMora jlccCategoria) {
        return jlccBloques.stream().filter(b -> b.categoriaMora() == jlccCategoria).count();
    }

    private long jlccContarCategoriaVencimiento(
            List<BloqueMaqueta> jlccBloques, BloqueMaqueta.CategoriaVencimiento jlccCategoria) {
        return jlccBloques.stream().filter(b -> b.categoriaVencimiento() == jlccCategoria).count();
    }

    private String jlccContar(long jlccCantidad, String jlccSingular, String jlccPlural) {
        return jlccCantidad + " " + (jlccCantidad == 1 ? jlccSingular : jlccPlural);
    }

    private void jlccMostrarCargandoEscena() {
        if (contenedor3d == null || !Platform.isSupported(ConditionalFeature.SCENE3D)) return;
        jlccOcultarMensajeEscena();
        ProgressIndicator jlccIndicador = new ProgressIndicator();
        jlccIndicador.setPrefSize(34, 34);
        jlccIndicador.setMaxSize(34, 34);
        Label jlccTexto = new Label("Cargando maqueta…");
        jlccTexto.getStyleClass().add("maqueta-aviso-detalle");
        VBox jlccAviso = new VBox(10, jlccIndicador, jlccTexto);
        jlccAviso.setAlignment(Pos.CENTER);
        jlccAviso.setMaxWidth(520);
        jlccAviso.setMouseTransparent(true);
        jlccAviso.getStyleClass().add("maqueta-aviso");
        jlccMensajeEscena = jlccAviso;
        contenedor3d.getChildren().add(jlccAviso);
    }

    private void jlccLimpiarEscena() {
        jlccActualizarHover(null);
        jlccLimpiarSeleccion();
        jlccDetenerTransicion();
        jlccLimpiarUnidad();
        jlccNivelVista = JlccNivelVista.EDIFICIO;
        jlccNivelEnfocado = null;
        if (jlccMundo != null) jlccMundo.getChildren().clear();
        if (capaEtiquetas != null) capaEtiquetas.getChildren().clear();
        jlccMapaBloques.clear();
        jlccMapaPisos.clear();
        jlccGruposPiso.clear();
        jlccGruposBloques.clear();
        jlccGruposPlanta.clear();
        jlccGruposRecintos.clear();
        jlccNodosPlantaPorBloque.clear();
        jlccCajasPlantaPorBloque.clear();
        jlccMarcasPlantaPorBloque.clear();
        jlccRecintosPlantaPorBloque.clear();
        jlccDistribucionesPlanta.clear();
        jlccBasesPlanta.clear();
        jlccNodosPorBloque.clear();
        jlccCajasPorBloque.clear();
        jlccMarcasPorBloque.clear();
        jlccEtiquetasPiso.clear();
        jlccEtiquetasUnidad.clear();
        jlccGeometria = null;
        jlccPuntosEncuadre = List.of();
        jlccRentaMinima = null;
        jlccRentaMaxima = null;
        jlccBloqueHover = null;
        jlccNodoHover = null;
        jlccEstructuraGeneral = null;
        jlccSuelo = null;
        if (etiquetaHover != null) etiquetaHover.setVisible(false);
        jlccActualizarBarraNavegacion();
        jlccConfigurarLeyenda();
    }

    private void jlccActualizarAvisoUbicacion(int jlccSinUbicacion) {
        if (avisoUbicacion == null) return;
        boolean jlccVisible = jlccSinUbicacion > 0;
        if (jlccVisible) {
            avisoUbicacion.setText(
                    jlccSinUbicacion == 1
                            ? "1 inmueble sin ubicación no se muestra"
                            : jlccSinUbicacion + " inmuebles sin ubicación no se muestran");
        }
        avisoUbicacion.setVisible(jlccVisible);
        avisoUbicacion.setManaged(jlccVisible);
    }

    private void jlccMostrarMensajeEscena(String jlccTituloTexto, String jlccDetalleTexto) {
        if (contenedor3d == null) return;
        jlccOcultarMensajeEscena();

        FontIcon jlccIcono = new FontIcon(Feather.ALERT_TRIANGLE);
        jlccIcono.getStyleClass().add("maqueta-aviso-icono");

        Label jlccTitulo = new Label(jlccTituloTexto);
        jlccTitulo.getStyleClass().add("maqueta-aviso-titulo");

        Label jlccDetalle = new Label(jlccDetalleTexto);
        jlccDetalle.getStyleClass().add("maqueta-aviso-detalle");
        jlccDetalle.setWrapText(true);
        jlccDetalle.setMaxWidth(460);

        VBox jlccAviso = new VBox(8, jlccIcono, jlccTitulo, jlccDetalle);
        jlccAviso.setAlignment(Pos.CENTER);
        jlccAviso.setMaxWidth(520);
        jlccAviso.setMouseTransparent(true);
        jlccAviso.getStyleClass().add("maqueta-aviso");
        jlccMensajeEscena = jlccAviso;
        contenedor3d.getChildren().add(jlccAviso);
    }

    private void jlccOcultarMensajeEscena() {
        if (jlccMensajeEscena == null) return;
        contenedor3d.getChildren().remove(jlccMensajeEscena);
        jlccMensajeEscena = null;
    }

    private String jlccEtiquetaNivel(int jlccNivel) {
        if (jlccNivel == 0) return "Planta baja";
        if (jlccNivel < 0) return "Sótano " + Math.abs(jlccNivel);
        return "Piso " + jlccNivel;
    }

    private void jlccMostrarAvisoSin3d() {
        if (ayuda != null) {
            ayuda.setVisible(false);
            ayuda.setManaged(false);
        }

        FontIcon jlccIcono = new FontIcon(Feather.ALERT_TRIANGLE);
        jlccIcono.getStyleClass().add("maqueta-aviso-icono");

        Label jlccTitulo = new Label("La vista 3D no está disponible");
        jlccTitulo.getStyleClass().add("maqueta-aviso-titulo");

        Label jlccDetalle =
                new Label(
                        "Esta computadora no tiene habilitados los gráficos 3D de JavaFX. "
                                + "Puedes seguir usando el resto del sistema normalmente.");
        jlccDetalle.getStyleClass().add("maqueta-aviso-detalle");
        jlccDetalle.setWrapText(true);
        jlccDetalle.setMaxWidth(460);

        VBox jlccAviso = new VBox(8, jlccIcono, jlccTitulo, jlccDetalle);
        jlccAviso.setAlignment(Pos.CENTER);
        jlccAviso.setMaxWidth(520);
        jlccAviso.getStyleClass().add("maqueta-aviso");
        contenedor3d.getChildren().add(jlccAviso);
    }

    private String jlccDescripcionHover(BloqueMaqueta jlccBloque) {
        StringBuilder jlccTexto = new StringBuilder();
        if (jlccBloque.codigo() != null && !jlccBloque.codigo().isBlank()) {
            jlccTexto.append(jlccBloque.codigo()).append(" · ");
        }
        jlccTexto.append(jlccBloque.nombre() == null ? "Sin nombre" : jlccBloque.nombre())
                .append(" · ")
                .append(jlccBloque.tipo() == null ? "Sin tipo" : jlccBloque.tipo())
                .append(" · ")
                .append(jlccEtiquetaNivel(jlccBloque.nivel()))
                .append(" · ")
                .append(jlccBloque.estado().getEtiqueta());
        if (JlccPlantaPiso.jlccTieneArea(jlccBloque)) {
            jlccTexto.append(" · ")
                    .append(jlccBloque.areaM2().stripTrailingZeros().toPlainString())
                    .append(" m²");
        }
        if (jlccBloque.arrendatario() != null) jlccTexto.append(" · ").append(jlccBloque.arrendatario());
        switch (jlccModoVista) {
            case ESTADO -> {
                if (jlccBloque.semaforo() != null) {
                    jlccTexto.append(" · ").append(jlccBloque.semaforo().getEtiqueta());
                }
                if (jlccBloque.inconsistencia()) jlccTexto.append(" · Sin contrato vigente");
            }
            case MORA -> jlccTexto.append(" · Mora: ").append(jlccTextoMora(jlccBloque));
            case RENTA -> jlccTexto.append(" · ").append(jlccTextoRenta(jlccBloque.rentaReferencial()));
            case VENCIMIENTO ->
                    jlccTexto.append(" · ").append(jlccTextoVencimiento(jlccBloque.diasParaVencer()));
        }
        return jlccTexto.toString();
    }

    private String jlccTextoMora(BloqueMaqueta jlccBloque) {
        if (jlccBloque.estado() != ALQUILADO) return "Sin mora";
        if (jlccBloque.semaforo() == null) return "Sin contrato vigente";
        return jlccBloque.semaforo().getEtiqueta();
    }

    private String jlccDescripcionModo(JlccMaquetaService.DetalleMaqueta jlccDetalle) {
        return switch (jlccModoVista) {
            case ESTADO -> jlccDetalle.estado().getEtiqueta();
            case MORA -> {
                if (jlccDetalle.estado() != ALQUILADO) yield "Sin mora";
                if (jlccDetalle.contrato() == null) yield "Sin contrato vigente";
                yield jlccDetalle.contrato().semaforo().getEtiqueta();
            }
            case RENTA ->
                    jlccDetalle.rentaReferencial() == null
                            ? "Sin renta registrada"
                            : JlccTablas.jlccMoneda(jlccDetalle.rentaReferencial());
            case VENCIMIENTO ->
                    jlccDetalle.contrato() == null
                            ? "Sin contrato vigente"
                            : jlccTextoVencimiento(jlccDetalle.contrato().diasParaVencer());
        };
    }

    private String jlccTextoRenta(BigDecimal jlccRenta) {
        return jlccRenta == null ? "Sin renta registrada" : "Renta " + JlccTablas.jlccMoneda(jlccRenta);
    }

    private String jlccTextoVencimiento(Long jlccDias) {
        if (jlccDias == null) return "Sin contrato vigente";
        if (jlccDias < 0) {
            long jlccCantidad = -jlccDias;
            return "Venció hace " + jlccCantidad + (jlccCantidad == 1 ? " día" : " días");
        }
        if (jlccDias == 0) return "Vence hoy";
        return "Vence en " + jlccDias + (jlccDias == 1 ? " día" : " días");
    }

    enum JlccModoVista {
        ESTADO("Estado"),
        MORA("Mora"),
        RENTA("Renta"),
        VENCIMIENTO("Vencimiento");

        private final String jlccEtiqueta;

        JlccModoVista(String jlccEtiqueta) {
            this.jlccEtiqueta = jlccEtiqueta;
        }

        String jlccEtiqueta() {
            return jlccEtiqueta;
        }

        static List<String> jlccEtiquetas() {
            List<String> jlccEtiquetas = new ArrayList<>();
            for (JlccModoVista jlccModo : values()) jlccEtiquetas.add(jlccModo.jlccEtiqueta);
            return List.copyOf(jlccEtiquetas);
        }

        static JlccModoVista jlccDesdeEtiqueta(String jlccEtiqueta) {
            for (JlccModoVista jlccModo : values()) {
                if (jlccModo.jlccEtiqueta.equals(jlccEtiqueta)) return jlccModo;
            }
            return ESTADO;
        }

        static JlccModoVista jlccDesdePreferencia(String jlccValor) {
            if (jlccValor == null || jlccValor.isBlank()) return ESTADO;
            try {
                return valueOf(jlccValor.trim());
            } catch (IllegalArgumentException jlccError) {
                return ESTADO;
            }
        }
    }

    private double jlccLimitar(double jlccValor, double jlccMinimo, double jlccMaximo) {
        return Math.max(jlccMinimo, Math.min(jlccMaximo, jlccValor));
    }

    record FilaPiso(List<BloqueMaqueta> bloques, double z) {}

    record PisoGeometria(
            int nivel, double y, double ancho, double profundidad, List<FilaPiso> filas) {}

    record Geometria(
            double ancho,
            double profundidad,
            double alto,
            double anchoBase,
            double profundidadBase,
            double anchoTecho,
            double profundidadTecho,
            double desplazamientoY,
            Map<Integer, PisoGeometria> pisos,
            Map<Long, Point3D> posiciones) {}

    record Encuadre(double ojoX, double ojoY, double centroZ, double distancia) {}

    private record EtiquetaPiso(int nivel, Label nodo, Point3D ancla) {}

    private record EtiquetaUnidad(
            BloqueMaqueta bloque, Label nodo, Point3D anclaEdificio, Point3D anclaPlanta) {}

    private record EtiquetaAmbiente(Label nodo, Point3D ancla) {}

    private record ItemLeyenda(Node nodo, String clave) {}

    private record Proyeccion(
            double ojoX,
            double ojoY,
            double ojoZ,
            double tanV,
            double aspecto,
            double ancho,
            double alto) {}
}

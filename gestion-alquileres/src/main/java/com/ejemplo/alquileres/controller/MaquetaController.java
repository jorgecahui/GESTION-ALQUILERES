package com.ejemplo.alquileres.controller;

import static com.ejemplo.alquileres.model.EstadoInmueble.ALQUILADO;
import static com.ejemplo.alquileres.model.EstadoInmueble.DISPONIBLE;
import static com.ejemplo.alquileres.model.EstadoInmueble.INACTIVO;
import static com.ejemplo.alquileres.model.EstadoInmueble.MANTENIMIENTO;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.service.MaquetaService;
import com.ejemplo.alquileres.ui.BloqueMaqueta;
import com.ejemplo.alquileres.ui.Fechas;
import com.ejemplo.alquileres.ui.NivelVista;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Refrescable;
import com.ejemplo.alquileres.ui.Semaforo;
import com.ejemplo.alquileres.ui.Tablas;
import com.ejemplo.alquileres.ui.Tareas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.Window;
import javafx.util.Duration;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Maqueta 3D del edificio de alquileres. Los bloques se cargan desde
 * {@link MaquetaService} fuera del hilo de JavaFX y se convierten en nodos en el hilo FX.
 */
@Component
@Scope("prototype")
public class MaquetaController implements Refrescable {

    private static final Logger log = LoggerFactory.getLogger(MaquetaController.class);

    private static final double ANCHO_UNIDAD = 62;
    private static final double ALTO_UNIDAD = 48;
    private static final double PROFUNDIDAD_UNIDAD = 64;
    private static final int MAX_UNIDADES_POR_FILA = 8;
    private static final double SEPARACION_UNIDAD = 10;
    private static final double SEPARACION_FILA = 38;
    private static final double SEPARACION_PISO = 42;
    private static final double PASO_X = ANCHO_UNIDAD + SEPARACION_UNIDAD;
    private static final double PASO_Y = ALTO_UNIDAD + SEPARACION_PISO;
    private static final double PASO_Z = PROFUNDIDAD_UNIDAD + SEPARACION_FILA;

    private static final double LOSA_ALTO = 4;
    private static final double LOSA_MARGEN_X = 8;
    private static final double LOSA_MARGEN_Z = 8;
    private static final double BASE_ALTO = 8;
    private static final double TECHO_ALTO = 6;
    private static final double TECHO_ELEVACION = 22;
    private static final double COLUMNA_ANCHO = 6;
    private static final double COLUMNA_MARGEN = 18;
    private static final double MARGEN_SUELO_X = 70;
    private static final double MARGEN_SUELO_Z = 60;
    private static final double SEPARACION_ETIQUETA_PANTALLA = 12;
    private static final double RADIO_MARCA = 11.5;
    private static final double ALTO_MARCA = 5;

    private static final double ANGULO_Y_INICIAL = -38;
    private static final double ANGULO_X_INICIAL = 34;
    private static final double ANGULO_X_MINIMO = -8;
    private static final double ANGULO_X_MAXIMO = 65;

    private static final double RELLENO_ENCUADRE = 0.88;
    private static final double FACTOR_ZOOM_CERCA = 0.55;
    private static final double FACTOR_ZOOM_LEJOS = 2.4;
    private static final Duration DURACION_TRANSICION = Duration.millis(450);
    private static final double ESCALA_OCULTA = 0;
    private static final double ESCALA_VISIBLE = 1;

    private static final Color COLOR_DISPONIBLE = Color.web("#2f9e6e");
    private static final Color COLOR_ALQUILADO = Color.web("#3d7fc4");
    private static final Color COLOR_MANTENIMIENTO = Color.web("#e08a2e");
    private static final Color COLOR_INACTIVO = Color.web("#8a94a6");
    private static final Color COLOR_ESTRUCTURA = Color.web("#8a96a8");
    private static final Color COLOR_LOSA = Color.web("#95a2b4");
    private static final Color COLOR_MORA_AL_DIA = Color.web("#2f9e6e");
    private static final Color COLOR_MORA_ATRASO = Color.web("#ffc107");
    private static final Color COLOR_MORA_MOROSO = Color.web("#e53935");
    private static final Color COLOR_SIN_DATO = Color.web("#b8c1cd");
    private static final Color COLOR_VENCIDO = Color.web("#8e1b1b");
    private static final Color COLOR_RENTA_INICIO = Color.web("#cfe2f7");
    private static final Color COLOR_RENTA_FIN = Color.web("#1f5f9e");
    private static final Color COLOR_SELECCION = Color.web("#f8fafc");
    private static final Color COLOR_HOVER = Color.web("#9fd3ff");
    private static final Color COLOR_SUELO_CLARO_CENTRO = Color.web("#d7dee8");
    private static final Color COLOR_SUELO_CLARO_BORDE = Color.web("#9eacbe");
    private static final Color COLOR_SUELO_OSCURO_CENTRO = Color.web("#566273");
    private static final Color COLOR_SUELO_OSCURO_BORDE = Color.web("#303945");

    private static final List<Color> COLORES_RENTA = crearPaletaRenta();

    private static final double ALFA_ATENUADO = 0.12;
    private static final Pattern NUMERO_UNIDAD = Pattern.compile("(\\d{1,4})\\s*$");
    private static final String PREFERENCIA_MODO = "maqueta.modo";

    @FXML private StackPane contenedor3d;
    @FXML private Pane capaEtiquetas;
    @FXML private Label etiquetaHover, ayuda, avisoUbicacion, resumen;
    @FXML private Label migaNavegacion;
    @FXML private Button btnRestablecer, btnVolverEdificio;
    @FXML private ComboBox<String> selectorModo;
    @FXML private HBox leyenda, barraNavegacion;

    private final MaquetaService service;
    private final Tareas tareas;
    private final Notificador notificador;
    private final MainController principal;

    private final Map<EstadoInmueble, PhongMaterial> materiales = new EnumMap<>(EstadoInmueble.class);
    private final Map<EstadoInmueble, PhongMaterial> materialesAtenuados =
            new EnumMap<>(EstadoInmueble.class);
    private final Map<Color, PhongMaterial> materialesMora = new HashMap<>();
    private final Map<Color, PhongMaterial> materialesMoraAtenuados = new HashMap<>();
    private final Map<BloqueMaqueta.CategoriaMora, PhongMaterial> materialesCategoriaMora =
            new EnumMap<>(BloqueMaqueta.CategoriaMora.class);
    private final Map<BloqueMaqueta.CategoriaMora, PhongMaterial> materialesCategoriaMoraAtenuados =
            new EnumMap<>(BloqueMaqueta.CategoriaMora.class);
    private final Map<BloqueMaqueta.CategoriaVencimiento, PhongMaterial> materialesVencimiento =
            new EnumMap<>(BloqueMaqueta.CategoriaVencimiento.class);
    private final Map<BloqueMaqueta.CategoriaVencimiento, PhongMaterial> materialesVencimientoAtenuados =
            new EnumMap<>(BloqueMaqueta.CategoriaVencimiento.class);
    private final List<PhongMaterial> materialesRenta = new ArrayList<>();
    private final List<PhongMaterial> materialesRentaAtenuados = new ArrayList<>();
    private final Map<Node, BloqueMaqueta> mapaBloques = new HashMap<>();
    private final Map<BloqueMaqueta, Node> nodosPorBloque = new LinkedHashMap<>();
    private final Map<BloqueMaqueta, Box> cajasPorBloque = new HashMap<>();
    private final Map<BloqueMaqueta, Cylinder> marcasPorBloque = new HashMap<>();
    private final Map<Integer, Group> gruposPiso = new LinkedHashMap<>();
    private final Map<Integer, Group> gruposBloques = new LinkedHashMap<>();
    private final Map<Node, Integer> mapaPisos = new HashMap<>();
    private final List<EtiquetaPiso> etiquetasPiso = new ArrayList<>();
    private final List<EtiquetaUnidad> etiquetasUnidad = new ArrayList<>();
    private final Preferences preferencias = Preferences.userNodeForPackage(MaquetaController.class);
    private final EventHandler<KeyEvent> manejadorTecla = this::alPresionarTecla;

    private Group mundo;
    private Group estructuraGeneral;
    private SubScene subEscena;
    private PerspectiveCamera camara;
    private Rotate rotacionX, rotacionY;
    private PhongMaterial materialEstructura, materialSeleccion, materialHover,
            materialLosa, materialSueloClaro, materialSueloOscuro,
            materialSinDato, materialSinDatoAtenuado;
    private Box suelo;
    private Geometria geometria;
    private List<Point3D> puntosEncuadre = List.of();
    private Box contornoSeleccion;
    private Box contornoHover;

    private NivelVista nivelVista = NivelVista.EDIFICIO;
    private Integer nivelEnfocado;
    private Timeline transicion;
    private Scene escenaTeclado;

    private double centroZAuto;
    private double distanciaAuto;

    private double xPresionado, yPresionado, xInicialClic, yInicialClic;
    private boolean arrastrando;
    private boolean escenaIntentada;
    private BloqueMaqueta bloqueHover;
    private Node nodoHover;
    private VBox mensajeEscena;

    private final List<ItemLeyenda> itemsLeyenda = new ArrayList<>();
    private List<BloqueMaqueta> bloquesActuales = List.of();
    private ModoVista modoVista = ModoVista.ESTADO;
    private BigDecimal rentaMinima;
    private BigDecimal rentaMaxima;
    private String filtroClave;
    private boolean filtroActivo;
    private boolean filtroFijo;

    public MaquetaController(MaquetaService service, Tareas tareas, Notificador notificador,
                             MainController principal) {
        this.service = service;
        this.tareas = tareas;
        this.notificador = notificador;
        this.principal = principal;
    }

    @FXML
    public void initialize() {
        modoVista = ModoVista.desdePreferencia(
                preferencias.get(PREFERENCIA_MODO, ModoVista.ESTADO.name()));
        configurarSelectorModo();
        configurarNavegacionTeclado();
        actualizarBarraNavegacion();
        if (Platform.isFxApplicationThread()) iniciarMaqueta();
        else Platform.runLater(this::iniciarMaqueta);
        configurarLeyenda();
    }

    @Override
    public void refrescar() {
        if (Platform.isFxApplicationThread()) {
            reiniciarNavegacion();
            mostrarCargandoEscena();
        } else {
            Platform.runLater(() -> {
                reiniciarNavegacion();
                mostrarCargandoEscena();
            });
        }
        tareas.ejecutar(service::cargar, this::aplicarResultado, this::mostrarError);
    }

    private void iniciarMaqueta() {
        if (escenaIntentada) return;
        escenaIntentada = true;
        if (contenedor3d == null) {
            escenaIntentada = false;
            return;
        }
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            mostrarAvisoSin3d();
            return;
        }

        rotacionX = new Rotate(ANGULO_X_INICIAL, Rotate.X_AXIS);
        rotacionY = new Rotate(ANGULO_Y_INICIAL, Rotate.Y_AXIS);
        mundo = new Group();
        mundo.getTransforms().addAll(rotacionX, rotacionY);

        Group raiz3d = new Group(mundo);
        prepararMateriales();
        contornoSeleccion = new Box(ANCHO_UNIDAD + 3, ALTO_UNIDAD + 3, PROFUNDIDAD_UNIDAD + 3);
        contornoSeleccion.setDrawMode(DrawMode.LINE);
        contornoSeleccion.setMaterial(materialSeleccion);
        contornoSeleccion.setMouseTransparent(true);
        contornoHover = new Box(ANCHO_UNIDAD + 3, ALTO_UNIDAD + 3, PROFUNDIDAD_UNIDAD + 3);
        contornoHover.setDrawMode(DrawMode.LINE);
        contornoHover.setMaterial(materialHover);
        contornoHover.setMouseTransparent(true);
        agregarIluminacion(raiz3d);
        configurarCamara(raiz3d);
        configurarTemaSuelo();
        configurarInteraccion();

        contenedor3d.widthProperty().addListener((observable, anterior, actual) -> actualizarEncuadreYEtiquetas());
        contenedor3d.heightProperty().addListener((observable, anterior, actual) -> actualizarEncuadreYEtiquetas());
        Platform.runLater(this::actualizarEncuadreYEtiquetas);
    }

    private void aplicarResultado(MaquetaService.ResultadoMaqueta resultado) {
        if (resultado == null) return;
        if (mundo == null) {
            iniciarMaqueta();
            if (mundo == null) return;
        }

        bloquesActuales = List.copyOf(resultado.bloques());
        actualizarAvisoUbicacion(resultado.sinUbicacion());
        actualizarResumen(resultado.bloques());
        if (resultado.bloques().isEmpty()) {
            limpiarEscena();
            mostrarMensajeEscena("Sin inmuebles ubicados",
                    "No hay unidades con nivel y orden para dibujar. "
                            + "Asigna una ubicación desde el formulario de inmuebles.");
            return;
        }

        ocultarMensajeEscena();
        try {
            construirEscena(resultado.bloques());
        } catch (RuntimeException error) {
            log.error("No se pudo construir la escena 3D de la maqueta", error);
            limpiarEscena();
            mostrarMensajeEscena("No se pudo dibujar la maqueta",
                    "Ocurrió un error al construir la vista 3D. Intenta nuevamente.");
            notificador.error("No se pudo dibujar la maqueta 3D. Intenta nuevamente.");
        }
    }

    private void mostrarError(Throwable error) {
        String mensaje = "No se pudieron cargar los datos de la maqueta. Intenta nuevamente.";
        notificador.error(mensaje);
        if (contenedor3d != null && Platform.isSupported(ConditionalFeature.SCENE3D)) {
            if (mundo == null) iniciarMaqueta();
            mostrarMensajeEscena("No se pudo actualizar la maqueta", mensaje);
        }
    }

    private void configurarCamara(Group raiz3d) {
        subEscena = new SubScene(raiz3d, 1, 1, true, SceneAntialiasing.BALANCED);
        // Sin managed, el tamaño enlazado del SubScene no retroalimenta el cálculo
        // de tamaño del StackPane (eso hacía crecer la vista en cada pulso).
        subEscena.setManaged(false);
        subEscena.setFill(Color.TRANSPARENT);
        subEscena.widthProperty().bind(contenedor3d.widthProperty());
        subEscena.heightProperty().bind(contenedor3d.heightProperty());

        camara = new PerspectiveCamera(false);
        camara.setFieldOfView(36);
        camara.setNearClip(0.1);
        camara.setFarClip(6000);
        camara.setTranslateZ(-1200);
        subEscena.setCamera(camara);

        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(contenedor3d.widthProperty());
        recorte.heightProperty().bind(contenedor3d.heightProperty());
        recorte.setArcWidth(32);
        recorte.setArcHeight(32);
        contenedor3d.setClip(recorte);

        contenedor3d.getChildren().add(0, subEscena);
    }

    private void configurarTemaSuelo() {
        contenedor3d.sceneProperty().addListener((observable, anterior, escena) -> {
            if (escena == null) return;
            Parent raiz = escena.getRoot();
            raiz.getStyleClass().addListener(
                    (ListChangeListener<String>) cambio -> actualizarMaterialSuelo());
            actualizarMaterialSuelo();
        });
    }

    private void actualizarMaterialSuelo() {
        if (suelo == null) return;
        suelo.setMaterial(esTemaOscuro() ? materialSueloOscuro : materialSueloClaro);
    }

    private boolean esTemaOscuro() {
        return contenedor3d != null && contenedor3d.getScene() != null
                && contenedor3d.getScene().getRoot().getStyleClass().contains("dark");
    }

    private void agregarIluminacion(Group raiz3d) {
        AmbientLight ambiente = new AmbientLight(Color.rgb(58, 61, 68));

        PointLight principal = new PointLight(Color.rgb(178, 184, 194));
        principal.setTranslateX(-350);
        principal.setTranslateY(-450);
        principal.setTranslateZ(-800);

        PointLight relleno = new PointLight(Color.rgb(78, 86, 100));
        relleno.setTranslateX(450);
        relleno.setTranslateY(250);
        relleno.setTranslateZ(-500);

        raiz3d.getChildren().addAll(ambiente, principal, relleno);
    }

    private void prepararMateriales() {
        materiales.put(DISPONIBLE, material(COLOR_DISPONIBLE));
        materiales.put(ALQUILADO, material(COLOR_ALQUILADO));
        materiales.put(MANTENIMIENTO, material(COLOR_MANTENIMIENTO));
        materiales.put(INACTIVO, material(COLOR_INACTIVO));
        materialesAtenuados.put(DISPONIBLE, materialAtenuado(COLOR_DISPONIBLE));
        materialesAtenuados.put(ALQUILADO, materialAtenuado(COLOR_ALQUILADO));
        materialesAtenuados.put(MANTENIMIENTO, materialAtenuado(COLOR_MANTENIMIENTO));
        materialesAtenuados.put(INACTIVO, materialAtenuado(COLOR_INACTIVO));
        materialesMora.put(COLOR_MORA_MOROSO, material(COLOR_MORA_MOROSO));
        materialesMora.put(COLOR_MORA_ATRASO, material(COLOR_MORA_ATRASO));
        materialesMoraAtenuados.put(COLOR_MORA_MOROSO, materialAtenuado(COLOR_MORA_MOROSO));
        materialesMoraAtenuados.put(COLOR_MORA_ATRASO, materialAtenuado(COLOR_MORA_ATRASO));

        materialesCategoriaMora.put(BloqueMaqueta.CategoriaMora.AL_DIA,
                material(COLOR_MORA_AL_DIA));
        materialesCategoriaMora.put(BloqueMaqueta.CategoriaMora.ATRASO_LEVE,
                material(COLOR_MORA_ATRASO));
        materialesCategoriaMora.put(BloqueMaqueta.CategoriaMora.MOROSO,
                material(COLOR_MORA_MOROSO));
        materialesCategoriaMora.put(BloqueMaqueta.CategoriaMora.SIN_MORA,
                material(COLOR_SIN_DATO));
        materialesCategoriaMoraAtenuados.put(BloqueMaqueta.CategoriaMora.AL_DIA,
                materialAtenuado(COLOR_MORA_AL_DIA));
        materialesCategoriaMoraAtenuados.put(BloqueMaqueta.CategoriaMora.ATRASO_LEVE,
                materialAtenuado(COLOR_MORA_ATRASO));
        materialesCategoriaMoraAtenuados.put(BloqueMaqueta.CategoriaMora.MOROSO,
                materialAtenuado(COLOR_MORA_MOROSO));
        materialesCategoriaMoraAtenuados.put(BloqueMaqueta.CategoriaMora.SIN_MORA,
                materialAtenuado(COLOR_SIN_DATO));

        materialesVencimiento.put(BloqueMaqueta.CategoriaVencimiento.VENCIDO,
                material(COLOR_VENCIDO));
        materialesVencimiento.put(BloqueMaqueta.CategoriaVencimiento.HASTA_30_DIAS,
                material(COLOR_MORA_MOROSO));
        materialesVencimiento.put(BloqueMaqueta.CategoriaVencimiento.DE_31_A_90_DIAS,
                material(COLOR_MANTENIMIENTO));
        materialesVencimiento.put(BloqueMaqueta.CategoriaVencimiento.MAS_DE_90_DIAS,
                material(COLOR_DISPONIBLE));
        materialesVencimiento.put(BloqueMaqueta.CategoriaVencimiento.SIN_CONTRATO,
                material(COLOR_SIN_DATO));
        materialesVencimientoAtenuados.put(BloqueMaqueta.CategoriaVencimiento.VENCIDO,
                materialAtenuado(COLOR_VENCIDO));
        materialesVencimientoAtenuados.put(BloqueMaqueta.CategoriaVencimiento.HASTA_30_DIAS,
                materialAtenuado(COLOR_MORA_MOROSO));
        materialesVencimientoAtenuados.put(BloqueMaqueta.CategoriaVencimiento.DE_31_A_90_DIAS,
                materialAtenuado(COLOR_MANTENIMIENTO));
        materialesVencimientoAtenuados.put(BloqueMaqueta.CategoriaVencimiento.MAS_DE_90_DIAS,
                materialAtenuado(COLOR_DISPONIBLE));
        materialesVencimientoAtenuados.put(BloqueMaqueta.CategoriaVencimiento.SIN_CONTRATO,
                materialAtenuado(COLOR_SIN_DATO));

        for (Color color : COLORES_RENTA) {
            materialesRenta.add(material(color));
            materialesRentaAtenuados.add(materialAtenuado(color));
        }
        materialSinDato = material(COLOR_SIN_DATO);
        materialSinDatoAtenuado = materialAtenuado(COLOR_SIN_DATO);

        materialEstructura = material(COLOR_ESTRUCTURA);
        materialLosa = material(COLOR_LOSA);
        materialSeleccion = material(COLOR_SELECCION);
        materialHover = material(COLOR_HOVER);
        materialSueloClaro = materialSuelo(COLOR_SUELO_CLARO_CENTRO, COLOR_SUELO_CLARO_BORDE);
        materialSueloOscuro = materialSuelo(COLOR_SUELO_OSCURO_CENTRO, COLOR_SUELO_OSCURO_BORDE);
    }

    private static List<Color> crearPaletaRenta() {
        List<Color> colores = new ArrayList<>(MaquetaService.TONOS_RENTA);
        for (int i = 0; i < MaquetaService.TONOS_RENTA; i++) {
            double avance = i / (double) (MaquetaService.TONOS_RENTA - 1);
            colores.add(COLOR_RENTA_INICIO.interpolate(COLOR_RENTA_FIN, avance));
        }
        return List.copyOf(colores);
    }

    private PhongMaterial material(Color color) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(Color.rgb(178, 184, 194));
        material.setSpecularPower(18);
        return material;
    }

    private PhongMaterial materialAtenuado(Color color) {
        PhongMaterial material = new PhongMaterial(new Color(color.getRed(), color.getGreen(),
                color.getBlue(), ALFA_ATENUADO));
        material.setSpecularColor(Color.TRANSPARENT);
        material.setSpecularPower(0);
        return material;
    }

    private PhongMaterial materialSuelo(Color centro, Color borde) {
        WritableImage textura = new WritableImage(96, 96);
        PixelWriter pixel = textura.getPixelWriter();
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 96; x++) {
                double dx = (x - 47.5) / 47.5;
                double dy = (y - 47.5) / 47.5;
                double distancia = Math.min(1, Math.hypot(dx, dy));
                pixel.setColor(x, y, centro.interpolate(borde, distancia));
            }
        }
        PhongMaterial material = new PhongMaterial(Color.WHITE);
        material.setDiffuseMap(textura);
        material.setSpecularColor(Color.rgb(86, 92, 102));
        material.setSpecularPower(8);
        return material;
    }

    private void construirEscena(List<BloqueMaqueta> bloques) {
        List<BloqueMaqueta> lista = bloques == null ? List.of() : bloques;
        actualizarHover(null);
        limpiarSeleccion();
        detenerTransicion();
        nivelVista = NivelVista.EDIFICIO;
        nivelEnfocado = null;
        geometria = calcularGeometria(lista);
        puntosEncuadre = calcularPuntosEncuadre();

        mundo.getChildren().clear();
        mapaBloques.clear();
        mapaPisos.clear();
        gruposPiso.clear();
        gruposBloques.clear();
        nodosPorBloque.clear();
        cajasPorBloque.clear();
        marcasPorBloque.clear();
        bloqueHover = null;
        nodoHover = null;
        suelo = null;
        calcularRentas(lista);

        crearEtiquetasPiso();
        estructuraGeneral = crearEstructura();
        mundo.getChildren().add(estructuraGeneral);
        for (PisoGeometria piso : geometria.pisos().values()) {
            Group bloqueGrupo = new Group();
            for (FilaPiso fila : piso.filas()) {
                for (BloqueMaqueta bloque : fila.bloques()) {
                    bloqueGrupo.getChildren().add(crearBloque(bloque));
                }
            }
            Group pisoGrupo = new Group(crearLosa(piso), bloqueGrupo);
            gruposPiso.put(piso.nivel(), pisoGrupo);
            gruposBloques.put(piso.nivel(), bloqueGrupo);
            mundo.getChildren().add(pisoGrupo);
        }
        crearEtiquetasUnidad(lista);
        aplicarFiltroMateriales();
        configurarLeyenda();
        actualizarMaterialSuelo();
        actualizarBarraNavegacion();
        actualizarResumen(bloquesActuales);

        Platform.runLater(this::actualizarEncuadreYEtiquetas);
    }

    private void calcularRentas(List<BloqueMaqueta> bloques) {
        rentaMinima = null;
        rentaMaxima = null;
        for (BloqueMaqueta bloque : bloques) {
            BigDecimal renta = bloque.rentaReferencial();
            if (renta == null) continue;
            if (rentaMinima == null || renta.compareTo(rentaMinima) < 0) rentaMinima = renta;
            if (rentaMaxima == null || renta.compareTo(rentaMaxima) > 0) rentaMaxima = renta;
        }
    }

    static Geometria calcularGeometria(List<BloqueMaqueta> bloques) {
        Map<Integer, List<BloqueMaqueta>> porNivel = new TreeMap<>();
        for (BloqueMaqueta bloque : bloques) {
            porNivel.computeIfAbsent(bloque.nivel(), nivel -> new ArrayList<>()).add(bloque);
        }
        porNivel.values().forEach(lista ->
                lista.sort(Comparator.comparingInt(BloqueMaqueta::orden).thenComparing(BloqueMaqueta::id)));

        List<Integer> niveles = List.copyOf(porNivel.keySet());
        Map<Integer, List<List<BloqueMaqueta>>> filasPorNivel = new LinkedHashMap<>();
        int columnasMax = 1;
        int filasMax = 1;
        for (Map.Entry<Integer, List<BloqueMaqueta>> entrada : porNivel.entrySet()) {
            List<List<BloqueMaqueta>> filas = repartirFilas(entrada.getValue());
            filasPorNivel.put(entrada.getKey(), filas);
            columnasMax = Math.max(columnasMax, filas.stream().mapToInt(List::size).max().orElse(1));
            filasMax = Math.max(filasMax, filas.size());
        }

        double ancho = anchoColumnas(columnasMax);
        double profundidad = filasMax * PROFUNDIDAD_UNIDAD
                + Math.max(0, filasMax - 1) * SEPARACION_FILA;
        int cantidadNiveles = Math.max(1, niveles.size());
        double alto = cantidadNiveles * ALTO_UNIDAD + Math.max(0, cantidadNiveles - 1) * SEPARACION_PISO;

        double anchoBase = ancho + 2 * MARGEN_SUELO_X;
        double profundidadBase = profundidad + 2 * MARGEN_SUELO_Z;
        double anchoTecho = ancho + 2 * (COLUMNA_MARGEN + COLUMNA_ANCHO);
        double profundidadTecho = profundidad + 2 * (COLUMNA_MARGEN + COLUMNA_ANCHO);

        double limiteSuperior = -alto / 2 - TECHO_ELEVACION - TECHO_ALTO;
        double limiteInferior = alto / 2 + BASE_ALTO;
        double desplazamientoY = -(limiteSuperior + limiteInferior) / 2;

        Map<Integer, Double> yPorNivel = new HashMap<>();
        for (int i = 0; i < niveles.size(); i++) {
            double y = alto / 2 - ALTO_UNIDAD / 2 - i * PASO_Y + desplazamientoY;
            yPorNivel.put(niveles.get(i), y);
        }

        Map<Integer, PisoGeometria> pisos = new LinkedHashMap<>();
        Map<Long, Point3D> posiciones = new HashMap<>();
        for (Map.Entry<Integer, List<List<BloqueMaqueta>>> entrada : filasPorNivel.entrySet()) {
            int nivel = entrada.getKey();
            List<List<BloqueMaqueta>> filas = entrada.getValue();
            double y = yPorNivel.get(nivel);
            int columnasPiso = filas.stream().mapToInt(List::size).max().orElse(1);
            double anchoPiso = anchoColumnas(columnasPiso);
            double zInicio = -(filas.size() - 1) * PASO_Z / 2.0;
            List<FilaPiso> filasGeometria = new ArrayList<>();
            for (int fila = 0; fila < filas.size(); fila++) {
                List<BloqueMaqueta> unidades = filas.get(fila);
                double z = zInicio + fila * PASO_Z;
                double xInicio = -(unidades.size() - 1) * PASO_X / 2.0;
                for (int i = 0; i < unidades.size(); i++) {
                    double x = xInicio + i * PASO_X;
                    posiciones.put(unidades.get(i).id(), new Point3D(x, y, z));
                }
                filasGeometria.add(new FilaPiso(List.copyOf(unidades), z));
            }
            double profundidadPiso = filas.size() * PROFUNDIDAD_UNIDAD
                    + Math.max(0, filas.size() - 1) * SEPARACION_FILA;
            pisos.put(nivel, new PisoGeometria(nivel, y, anchoPiso, profundidadPiso,
                    List.copyOf(filasGeometria)));
        }

        return new Geometria(ancho, profundidad, alto, anchoBase, profundidadBase,
                anchoTecho, profundidadTecho, desplazamientoY, pisos, posiciones);
    }

    private static List<List<BloqueMaqueta>> repartirFilas(List<BloqueMaqueta> unidades) {
        int cantidadFilas = Math.max(1,
                (unidades.size() + MAX_UNIDADES_POR_FILA - 1) / MAX_UNIDADES_POR_FILA);
        int base = unidades.size() / cantidadFilas;
        int resto = unidades.size() % cantidadFilas;
        List<List<BloqueMaqueta>> filas = new ArrayList<>(cantidadFilas);
        int inicio = 0;
        for (int fila = 0; fila < cantidadFilas; fila++) {
            int cantidad = base + (fila < resto ? 1 : 0);
            filas.add(List.copyOf(unidades.subList(inicio, inicio + cantidad)));
            inicio += cantidad;
        }
        return filas;
    }

    private static double anchoColumnas(int columnas) {
        return columnas * ANCHO_UNIDAD + Math.max(0, columnas - 1) * SEPARACION_UNIDAD;
    }

    private List<Point3D> calcularPuntosEncuadre() {
        List<Point3D> puntos = new ArrayList<>();
        agregarPuntosCaja(puntos, 0, yBase(), 0, geometria.anchoBase(), BASE_ALTO, geometria.profundidadBase());
        double dzTecho = geometria.profundidadTecho() / 2 - COLUMNA_ANCHO / 2;
        double dxTecho = geometria.anchoTecho() / 2 - COLUMNA_ANCHO / 2;
        agregarPuntosCaja(puntos, 0, yTecho(), -dzTecho,
                geometria.anchoTecho(), TECHO_ALTO, COLUMNA_ANCHO);
        agregarPuntosCaja(puntos, 0, yTecho(), dzTecho,
                geometria.anchoTecho(), TECHO_ALTO, COLUMNA_ANCHO);
        agregarPuntosCaja(puntos, -dxTecho, yTecho(), 0,
                COLUMNA_ANCHO, TECHO_ALTO, geometria.profundidadTecho());
        agregarPuntosCaja(puntos, dxTecho, yTecho(), 0,
                COLUMNA_ANCHO, TECHO_ALTO, geometria.profundidadTecho());
        for (double dx : new double[]{-xColumna(), xColumna()}) {
            for (double dz : new double[]{-zColumna(), zColumna()}) {
                agregarPuntosCaja(puntos, dx, yColumnas(), dz,
                        COLUMNA_ANCHO, altoColumna(), COLUMNA_ANCHO);
            }
        }
        for (Point3D posicion : geometria.posiciones().values()) {
            agregarPuntosCaja(puntos, posicion.getX(), posicion.getY(), posicion.getZ(),
                    ANCHO_UNIDAD, ALTO_UNIDAD, PROFUNDIDAD_UNIDAD);
        }
        return puntos;
    }

    private static void agregarPuntosCaja(List<Point3D> puntos, double centroX, double centroY,
                                          double centroZ, double ancho, double alto,
                                          double profundidad) {
        double mitadX = ancho / 2;
        double mitadY = alto / 2;
        double mitadZ = profundidad / 2;
        for (double dx : new double[]{-mitadX, mitadX}) {
            for (double dy : new double[]{-mitadY, mitadY}) {
                for (double dz : new double[]{-mitadZ, mitadZ}) {
                    puntos.add(new Point3D(centroX + dx, centroY + dy, centroZ + dz));
                }
            }
        }
    }

    private double yBase() {
        return geometria.alto() / 2 + BASE_ALTO / 2 + geometria.desplazamientoY();
    }

    private double yTecho() {
        return -geometria.alto() / 2 - TECHO_ELEVACION - TECHO_ALTO / 2 + geometria.desplazamientoY();
    }

    private double yColumnas() {
        return -(TECHO_ELEVACION + TECHO_ALTO) / 2 + geometria.desplazamientoY();
    }

    private double xColumna() {
        return geometria.ancho() / 2 + COLUMNA_MARGEN + COLUMNA_ANCHO / 2;
    }

    private double zColumna() {
        return geometria.profundidad() / 2 + COLUMNA_MARGEN + COLUMNA_ANCHO / 2;
    }

    private double altoColumna() {
        return geometria.alto() + TECHO_ELEVACION + TECHO_ALTO;
    }

    private Group crearEstructura() {
        Group estructura = new Group(crearBase(), crearTecho());
        estructura.getChildren().add(crearColumnas());
        return estructura;
    }

    private Node crearBase() {
        suelo = new Box(geometria.anchoBase(), BASE_ALTO, geometria.profundidadBase());
        suelo.setMaterial(esTemaOscuro() ? materialSueloOscuro : materialSueloClaro);
        suelo.setTranslateY(yBase());
        return suelo;
    }

    private Node crearLosa(PisoGeometria piso) {
        Box losa = new Box(
                piso.ancho() + 2 * LOSA_MARGEN_X,
                LOSA_ALTO,
                piso.profundidad() + 2 * LOSA_MARGEN_Z);
        losa.setMaterial(materialLosa);
        losa.setTranslateY(piso.y() + ALTO_UNIDAD / 2 + LOSA_ALTO / 2);
        losa.setCursor(Cursor.HAND);
        mapaPisos.put(losa, piso.nivel());
        return losa;
    }

    private Node crearTecho() {
        Group techo = new Group();
        double dz = geometria.profundidadTecho() / 2 - COLUMNA_ANCHO / 2;
        double dx = geometria.anchoTecho() / 2 - COLUMNA_ANCHO / 2;
        techo.getChildren().addAll(
                viga(geometria.anchoTecho(), TECHO_ALTO, COLUMNA_ANCHO, 0, yTecho(), -dz),
                viga(geometria.anchoTecho(), TECHO_ALTO, COLUMNA_ANCHO, 0, yTecho(), dz),
                viga(COLUMNA_ANCHO, TECHO_ALTO, geometria.profundidadTecho(), -dx, yTecho(), 0),
                viga(COLUMNA_ANCHO, TECHO_ALTO, geometria.profundidadTecho(), dx, yTecho(), 0));
        return techo;
    }

    private Box viga(double ancho, double alto, double profundidad,
                     double x, double y, double z) {
        Box viga = new Box(ancho, alto, profundidad);
        viga.setMaterial(materialEstructura);
        viga.setTranslateX(x);
        viga.setTranslateY(y);
        viga.setTranslateZ(z);
        return viga;
    }

    private Node crearColumnas() {
        Group columnas = new Group();
        for (double dx : new double[]{-xColumna(), xColumna()}) {
            for (double dz : new double[]{-zColumna(), zColumna()}) {
                Box columna = new Box(COLUMNA_ANCHO, altoColumna(), COLUMNA_ANCHO);
                columna.setMaterial(materialEstructura);
                columna.setTranslateX(dx);
                columna.setTranslateY(yColumnas());
                columna.setTranslateZ(dz);
                columnas.getChildren().add(columna);
            }
        }
        return columnas;
    }

    private void crearEtiquetasPiso() {
        capaEtiquetas.getChildren().clear();
        etiquetasPiso.clear();
        for (PisoGeometria piso : geometria.pisos().values()) {
            Label etiqueta = new Label(etiquetaNivel(piso.nivel()));
            etiqueta.getStyleClass().add("maqueta-piso-etiqueta");
            etiqueta.setMouseTransparent(false);
            etiqueta.setCursor(Cursor.HAND);
            etiqueta.setVisible(false);
            int nivel = piso.nivel();
            etiqueta.setOnMouseClicked(evento -> {
                if (evento.getButton() != MouseButton.PRIMARY) return;
                entrarPiso(nivel);
                evento.consume();
            });
            capaEtiquetas.getChildren().add(etiqueta);

            Point3D ancla = new Point3D(
                    -xColumna(),
                    piso.y(),
                    piso.filas().get(0).z());
            etiquetasPiso.add(new EtiquetaPiso(nivel, etiqueta, ancla));
        }
    }

    private void crearEtiquetasUnidad(List<BloqueMaqueta> bloques) {
        etiquetasUnidad.clear();
        for (BloqueMaqueta bloque : bloques) {
            Point3D posicion = geometria.posiciones().get(bloque.id());
            if (posicion == null) continue;
            Label etiqueta = new Label(numeroUnidad(bloque));
            etiqueta.getStyleClass().add("maqueta-unidad-etiqueta");
            etiqueta.setMouseTransparent(true);
            etiqueta.setVisible(false);
            capaEtiquetas.getChildren().add(etiqueta);
            Point3D ancla = new Point3D(
                    posicion.getX(),
                    posicion.getY() - ALTO_UNIDAD / 2,
                    posicion.getZ() - PROFUNDIDAD_UNIDAD / 2);
            etiquetasUnidad.add(new EtiquetaUnidad(bloque.nivel(), etiqueta, ancla));
        }
    }

    static String numeroUnidad(BloqueMaqueta bloque) {
        String texto = bloque.nombre() == null || bloque.nombre().isBlank()
                ? bloque.codigo() : bloque.nombre();
        if (texto != null) {
            Matcher matcher = NUMERO_UNIDAD.matcher(texto);
            if (matcher.find()) return matcher.group(1);
        }
        return bloque.nivel() + "-" + bloque.orden();
    }

    private Node crearBloque(BloqueMaqueta bloque) {
        Point3D posicion = geometria.posiciones().get(bloque.id());
        if (posicion == null) return new Group();

        Box caja = new Box(ANCHO_UNIDAD, ALTO_UNIDAD, PROFUNDIDAD_UNIDAD);
        caja.setMaterial(materialPara(bloque.estado()));
        caja.setTranslateX(posicion.getX());
        caja.setTranslateY(posicion.getY());
        caja.setTranslateZ(posicion.getZ());
        cajasPorBloque.put(bloque, caja);

        Group grupo = new Group(caja);

        if (bloque.estado() == ALQUILADO) {
            Color colorMora = colorMora(bloque.semaforo());
            if (colorMora != null) {
                Cylinder marca = new Cylinder(RADIO_MARCA, ALTO_MARCA, 24);
                marca.setMaterial(materialMora(colorMora));
                marca.setTranslateX(posicion.getX());
                marca.setTranslateY(posicion.getY() - ALTO_UNIDAD / 2 + RADIO_MARCA + 4);
                marca.setTranslateZ(posicion.getZ() - PROFUNDIDAD_UNIDAD / 2 - ALTO_MARCA / 2);
                marca.setRotationAxis(Rotate.X_AXIS);
                marca.setRotate(90);
                grupo.getChildren().add(marca);
                marcasPorBloque.put(bloque, marca);
                mapaBloques.put(marca, bloque);
            }
        }

        mapaBloques.put(grupo, bloque);
        mapaBloques.put(caja, bloque);
        nodosPorBloque.put(bloque, grupo);
        return grupo;
    }

    private PhongMaterial materialPara(EstadoInmueble estado) {
        return materiales.get(estado);
    }

    private PhongMaterial materialAtenuadoPara(EstadoInmueble estado) {
        return materialesAtenuados.get(estado);
    }

    private PhongMaterial materialMora(Color color) {
        return materialesMora.get(color);
    }

    private PhongMaterial materialMoraAtenuado(Color color) {
        return materialesMoraAtenuados.get(color);
    }

    private Color colorMora(SemaforoMorosidad semaforo) {
        if (semaforo == null) return null;
        return switch (semaforo) {
            case MOROSO -> COLOR_MORA_MOROSO;
            case ATRASO_LEVE -> COLOR_MORA_ATRASO;
            case AL_DIA -> null;
        };
    }

    private void seleccionar(BloqueMaqueta bloque) {
        limpiarSeleccion();
        if (bloque == null || contornoSeleccion == null || geometria == null) return;
        Node nodo = nodosPorBloque.get(bloque);
        Point3D posicion = geometria.posiciones().get(bloque.id());
        if (!(nodo instanceof Group grupo) || posicion == null) return;
        contornoSeleccion.setTranslateX(posicion.getX());
        contornoSeleccion.setTranslateY(posicion.getY());
        contornoSeleccion.setTranslateZ(posicion.getZ());
        grupo.getChildren().add(contornoSeleccion);
    }

    private void limpiarSeleccion() {
        if (contornoSeleccion != null && contornoSeleccion.getParent() instanceof Group grupo) {
            grupo.getChildren().remove(contornoSeleccion);
        }
    }

    private void abrirDetalle(BloqueMaqueta bloque) {
        if (bloque == null) return;
        seleccionar(bloque);

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Detalle del inmueble");
        dialogo.setHeaderText(null);
        Window ventana = ventana();
        if (ventana != null) dialogo.initOwner(ventana);

        DialogPane panel = dialogo.getDialogPane();
        panel.getStyleClass().add("dialogo");
        panel.setPrefWidth(700);
        var hoja = getClass().getResource("/css/app.css");
        if (hoja != null) panel.getStylesheets().add(hoja.toExternalForm());

        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType ver = new ButtonType("Ver en Inmuebles", ButtonBar.ButtonData.OTHER);
        panel.getButtonTypes().addAll(cerrar, ver);

        VBox cuerpo = new VBox(8);
        cuerpo.setPadding(new Insets(0, 4, 0, 4));
        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefViewportHeight(520);
        scroll.getStyleClass().add("pagina-scroll");
        panel.setContent(scroll);
        mostrarCargandoDetalle(cuerpo);

        boolean[] activo = {true};
        dialogo.setOnHidden(evento -> {
            activo[0] = false;
            limpiarSeleccion();
        });

        tareas.ejecutar(() -> service.detalle(bloque.id()), detalle -> {
            if (!activo[0]) return;
            cuerpo.getChildren().setAll(crearContenidoDetalle(detalle));
            cuerpo.requestLayout();
        }, error -> {
            if (!activo[0]) return;
            notificador.error("No se pudo cargar el detalle del inmueble.");
            Label mensaje = new Label("No se pudo cargar el detalle. Intenta nuevamente.");
            mensaje.getStyleClass().add("campo-error");
            mensaje.setWrapText(true);
            cuerpo.getChildren().setAll(mensaje);
        });

        var resultado = dialogo.showAndWait();
        if (resultado.isPresent() && resultado.get() == ver && principal != null) {
            principal.irA("inmuebles");
        }
    }

    private void mostrarCargandoDetalle(VBox cuerpo) {
        ProgressIndicator indicador = new ProgressIndicator();
        indicador.setPrefSize(34, 34);
        indicador.setMaxSize(34, 34);
        Label texto = new Label("Cargando detalle…");
        texto.getStyleClass().add("pagina-subtitulo");
        VBox caja = new VBox(10, indicador, texto);
        caja.setAlignment(Pos.CENTER);
        caja.setPrefHeight(180);
        cuerpo.getChildren().setAll(caja);
    }

    private Node crearContenidoDetalle(MaquetaService.DetalleMaqueta detalle) {
        VBox contenido = new VBox(8);
        Label titulo = new Label(detalle.codigo() + " · " + detalle.nombre());
        titulo.getStyleClass().add("dialogo-titulo");
        titulo.setWrapText(true);
        contenido.getChildren().addAll(titulo, new Separator());
        if (modoVista != ModoVista.ESTADO) {
            Label datoModo = new Label("Vista " + modoVista.etiqueta()
                    + " · " + descripcionModo(detalle));
            datoModo.getStyleClass().add("maqueta-detalle-modo");
            datoModo.setWrapText(true);
            contenido.getChildren().add(datoModo);
        }

        Label seccionInmueble = new Label("INMUEBLE");
        seccionInmueble.getStyleClass().add("seccion-form");
        GridPane datos = gridDetalle();
        agregarPar(datos, 0, 0, "Tipo", valor(detalle.tipo()));
        agregarPar(datos, 0, 1, "Nivel / orden", nivelOrden(detalle));
        agregarFilaCompleta(datos, 1, "Dirección", valor(detalle.direccion()));
        agregarPar(datos, 2, 0, "Distrito", valor(detalle.distrito()));
        agregarPar(datos, 2, 1, "Área", detalle.areaM2() == null
                ? "—" : detalle.areaM2().toPlainString() + " m²");
        agregarPar(datos, 3, 0, "Dormitorios", numero(detalle.dormitorios()));
        agregarPar(datos, 3, 1, "Baños", numero(detalle.banos()));
        agregarPar(datos, 4, 0, "Renta referencial", moneda(detalle.rentaReferencial()));
        agregarPar(datos, 4, 1, "Estado", detalle.estado().getEtiqueta());
        contenido.getChildren().addAll(seccionInmueble, datos);

        Label seccionContrato = new Label("CONTRATO VIGENTE");
        seccionContrato.getStyleClass().add("seccion-form");
        contenido.getChildren().add(seccionContrato);
        MaquetaService.ContratoDetalle contrato = detalle.contrato();
        if (contrato == null) {
            Label sinContrato = new Label("Sin contrato vigente");
            sinContrato.getStyleClass().add("campo-ayuda");
            sinContrato.setWrapText(true);
            contenido.getChildren().add(sinContrato);
            if (detalle.inconsistencia()) {
                Label aviso = new Label("Inconsistencia: el inmueble figura como alquilado "
                        + "pero no tiene un contrato vigente.");
                aviso.getStyleClass().add("campo-error");
                aviso.setWrapText(true);
                contenido.getChildren().add(aviso);
            }
        } else {
            GridPane datosContrato = gridDetalle();
            agregarPar(datosContrato, 0, 0, "Código", valor(contrato.codigo()));
            agregarPar(datosContrato, 0, 1, "Renta mensual", moneda(contrato.rentaMensual()));
            agregarFilaCompleta(datosContrato, 1, "Arrendatario", valor(contrato.arrendatario()));
            agregarPar(datosContrato, 2, 0, "Documento", valor(contrato.documento()));
            HBox semaforo = new HBox(8, Semaforo.crear(contrato.semaforo()));
            semaforo.setAlignment(Pos.CENTER_LEFT);
            agregarNodo(datosContrato, 2, 1, "Semáforo", semaforo);
            agregarPar(datosContrato, 3, 0, "Fecha de inicio", Fechas.dia(contrato.fechaInicio()));
            agregarPar(datosContrato, 3, 1, "Fecha de fin", Fechas.dia(contrato.fechaFin()));
            contenido.getChildren().add(datosContrato);

            HBox resumen = new HBox(30,
                    datoResumen("Cuotas vencidas", String.valueOf(contrato.cuotasVencidas())),
                    datoResumen("Monto total pendiente", moneda(contrato.montoPendiente())));
            resumen.setAlignment(Pos.CENTER_LEFT);
            resumen.getStyleClass().add("maqueta-resumen-cuotas");
            contenido.getChildren().add(resumen);
        }
        return contenido;
    }

    private GridPane gridDetalle() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(7);
        ColumnConstraints etiqueta1 = new ColumnConstraints();
        etiqueta1.setMinWidth(100);
        etiqueta1.setPrefWidth(105);
        ColumnConstraints valor1 = new ColumnConstraints();
        valor1.setHgrow(Priority.ALWAYS);
        ColumnConstraints etiqueta2 = new ColumnConstraints();
        etiqueta2.setMinWidth(100);
        etiqueta2.setPrefWidth(105);
        ColumnConstraints valor2 = new ColumnConstraints();
        valor2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(etiqueta1, valor1, etiqueta2, valor2);
        return grid;
    }

    private void agregarPar(GridPane grid, int fila, int par, String etiqueta, String valor) {
        Label contenido = new Label(valor);
        contenido.setWrapText(true);
        contenido.setMaxWidth(Double.MAX_VALUE);
        agregarNodo(grid, fila, par, etiqueta, contenido);
    }

    private void agregarFilaCompleta(GridPane grid, int fila, String etiqueta, String valor) {
        Label nombre = new Label(etiqueta);
        nombre.getStyleClass().add("campo-label");
        Label contenido = new Label(valor);
        contenido.setWrapText(true);
        contenido.setMaxWidth(Double.MAX_VALUE);
        grid.add(nombre, 0, fila);
        grid.add(contenido, 1, fila, 3, 1);
    }

    private void agregarNodo(GridPane grid, int fila, int par, String etiqueta, Node valor) {
        Label nombre = new Label(etiqueta);
        nombre.getStyleClass().add("campo-label");
        int columna = par * 2;
        grid.add(nombre, columna, fila);
        grid.add(valor, columna + 1, fila);
    }

    private VBox datoResumen(String etiqueta, String valor) {
        Label nombre = new Label(etiqueta);
        nombre.getStyleClass().add("maqueta-resumen-etiqueta");
        Label contenido = new Label(valor);
        contenido.getStyleClass().add("maqueta-resumen-valor");
        return new VBox(2, nombre, contenido);
    }

    private String nivelOrden(MaquetaService.DetalleMaqueta detalle) {
        String nivel = detalle.nivel() == null ? "—" : etiquetaNivel(detalle.nivel());
        String orden = detalle.orden() == null ? "—" : String.valueOf(detalle.orden());
        return nivel + " · Orden " + orden;
    }

    private String valor(String valor) {
        return valor == null || valor.isBlank() ? "—" : valor;
    }

    private String numero(Integer valor) {
        return valor == null ? "—" : String.valueOf(valor);
    }

    private String moneda(BigDecimal valor) {
        return valor == null ? "—" : Tablas.moneda(valor);
    }

    private Window ventana() {
        return contenedor3d == null || contenedor3d.getScene() == null
                ? null : contenedor3d.getScene().getWindow();
    }

    private void configurarInteraccion() {
        subEscena.setOnMouseEntered(evento -> subEscena.setCursor(Cursor.OPEN_HAND));
        subEscena.setOnMousePressed(this::alPresionar);
        subEscena.setOnMouseDragged(this::alArrastrar);
        subEscena.setOnMouseReleased(this::alSoltar);
        subEscena.setOnMouseMoved(this::alMover);
        subEscena.setOnMouseExited(this::alSalir);
        subEscena.setOnScroll(this::alDesplazar);
    }

    private void alPresionar(MouseEvent evento) {
        if (transicionActiva()) return;
        if (evento.getButton() != MouseButton.PRIMARY) return;
        xInicialClic = evento.getSceneX();
        yInicialClic = evento.getSceneY();
        xPresionado = evento.getSceneX();
        yPresionado = evento.getSceneY();
        arrastrando = true;
        subEscena.setCursor(Cursor.CLOSED_HAND);
    }

    private void alArrastrar(MouseEvent evento) {
        if (!arrastrando || transicionActiva()) return;
        double dx = evento.getSceneX() - xPresionado;
        double dy = evento.getSceneY() - yPresionado;
        xPresionado = evento.getSceneX();
        yPresionado = evento.getSceneY();

        rotacionY.setAngle(Math.IEEEremainder(rotacionY.getAngle() + dx * 0.3, 360));
        rotacionX.setAngle(limitar(rotacionX.getAngle() + dy * 0.22, ANGULO_X_MINIMO, ANGULO_X_MAXIMO));
        actualizarEtiquetasPiso();
        actualizarEtiquetasUnidad();
    }

    private void alSoltar(MouseEvent evento) {
        boolean fuePresion = arrastrando;
        arrastrando = false;
        if (subEscena != null) subEscena.setCursor(Cursor.OPEN_HAND);
        if (!fuePresion || evento.getButton() != MouseButton.PRIMARY) return;
        if (transicionActiva()) return;
        double dx = evento.getSceneX() - xInicialClic;
        double dy = evento.getSceneY() - yInicialClic;
        if (Math.hypot(dx, dy) > 5) return;
        Node seleccionado = evento.getPickResult().getIntersectedNode();
        BloqueMaqueta bloque = mapaBloques.get(seleccionado);
        if (bloque != null) {
            abrirDetalle(bloque);
            return;
        }
        Integer piso = mapaPisos.get(seleccionado);
        if (piso != null) entrarPiso(piso);
    }

    private void alMover(MouseEvent evento) {
        if (arrastrando || transicionActiva()) return;
        actualizarHover(mapaBloques.get(evento.getPickResult().getIntersectedNode()));
    }

    private void alSalir(MouseEvent evento) {
        arrastrando = false;
        actualizarHover(null);
        subEscena.setCursor(Cursor.DEFAULT);
    }

    private void alDesplazar(ScrollEvent evento) {
        if (transicionActiva()) return;
        if (camara == null || subEscena == null || distanciaAuto <= 0) return;
        double altoVista = subEscena.getHeight();
        if (altoVista <= 0) return;

        double tanV = Math.tan(Math.toRadians(camara.getFieldOfView() / 2));
        double foco = altoVista / (2 * tanV);
        double distancia = centroZAuto - (camara.getTranslateZ() - foco);
        distancia = limitar(distancia - evento.getDeltaY() * 1.1,
                distanciaAuto * FACTOR_ZOOM_CERCA, distanciaAuto * FACTOR_ZOOM_LEJOS);
        camara.setTranslateZ(centroZAuto - distancia + foco);
        actualizarEtiquetasPiso();
        actualizarEtiquetasUnidad();
        evento.consume();
    }

    private void actualizarEncuadreYEtiquetas() {
        if (transicionActiva()) return;
        encuadrarAutomatico();
        actualizarEtiquetasPiso();
        actualizarEtiquetasUnidad();
    }

    private void encuadrarAutomatico() {
        if (mundo == null || camara == null || subEscena == null) return;
        double anchoVista = subEscena.getWidth();
        double altoVista = subEscena.getHeight();
        if (anchoVista < 60 || altoVista < 60) return;

        Encuadre encuadre = calcularEncuadre(proyectarAlPadre(puntosEncuadreActuales()),
                anchoVista, altoVista, camara.getFieldOfView(), camara.getNearClip(),
                RELLENO_ENCUADRE);
        if (encuadre == null) return;
        aplicarEncuadre(encuadre, anchoVista, altoVista);
        actualizarEtiquetasPiso();
    }

    /**
     * Encuadre del ámbito visible: el piso enfocado o, en su defecto, todo el edificio.
     */
    private List<Point3D> puntosEncuadreActuales() {
        if (nivelVista == NivelVista.PISO && nivelEnfocado != null && geometria != null) {
            return puntosPiso(geometria, nivelEnfocado);
        }
        return puntosEncuadre;
    }

    private List<Point3D> proyectarAlPadre(List<Point3D> puntos) {
        List<Point3D> padres = new ArrayList<>(puntos.size());
        for (Point3D punto : puntos) padres.add(mundo.localToParent(punto));
        return padres;
    }

    private void aplicarEncuadre(Encuadre encuadre, double anchoVista, double altoVista) {
        double foco = altoVista / (2 * Math.tan(Math.toRadians(camara.getFieldOfView() / 2)));
        camara.setTranslateX(encuadre.ojoX() - anchoVista / 2);
        camara.setTranslateY(encuadre.ojoY() - altoVista / 2);
        camara.setTranslateZ(encuadre.centroZ() - encuadre.distancia() + foco);
        aplicarReferenciaAutomatica(encuadre);
    }

    private void aplicarReferenciaAutomatica(Encuadre encuadre) {
        if (encuadre == null) return;
        centroZAuto = encuadre.centroZ();
        distanciaAuto = encuadre.distancia();
        camara.setFarClip(Math.max(6000, encuadre.distancia() * 6));
    }

    /**
     * Calcula el encuadre de un conjunto de puntos ya transformados por la rotación
     * del mundo. Es lógica pura: no toca la cámara ni nodos, por lo que se prueba
     * sin JavaFX.
     */
    static Encuadre calcularEncuadre(List<Point3D> puntos, double anchoVista, double altoVista,
                                     double fovGrados, double nearClip, double relleno) {
        if (puntos == null || puntos.isEmpty() || anchoVista <= 0 || altoVista <= 0) {
            return null;
        }

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Point3D punto : puntos) {
            minX = Math.min(minX, punto.getX());
            maxX = Math.max(maxX, punto.getX());
            minY = Math.min(minY, punto.getY());
            maxY = Math.max(maxY, punto.getY());
            minZ = Math.min(minZ, punto.getZ());
            maxZ = Math.max(maxZ, punto.getZ());
        }

        double centroX = (minX + maxX) / 2;
        double centroY = (minY + maxY) / 2;
        double centroZ = (minZ + maxZ) / 2;
        double tanV = Math.tan(Math.toRadians(fovGrados / 2));
        double aspecto = anchoVista / altoVista;

        double ojoX = centroX;
        double ojoY = centroY;
        double distancia = Math.max(500,
                Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)));
        for (int iteracion = 0; iteracion < 8; iteracion++) {
            double minNdcX = Double.POSITIVE_INFINITY, maxNdcX = Double.NEGATIVE_INFINITY;
            double minNdcY = Double.POSITIVE_INFINITY, maxNdcY = Double.NEGATIVE_INFINITY;
            double profundidadMinima = Double.POSITIVE_INFINITY;
            for (Point3D punto : puntos) {
                double profundidad = distancia + punto.getZ() - centroZ;
                profundidadMinima = Math.min(profundidadMinima, profundidad);
                if (profundidad <= nearClip) continue;
                double ndcX = (punto.getX() - ojoX) / (profundidad * tanV * aspecto);
                double ndcY = (punto.getY() - ojoY) / (profundidad * tanV);
                minNdcX = Math.min(minNdcX, ndcX);
                maxNdcX = Math.max(maxNdcX, ndcX);
                minNdcY = Math.min(minNdcY, ndcY);
                maxNdcY = Math.max(maxNdcY, ndcY);
            }

            if (profundidadMinima <= nearClip
                    || !Double.isFinite(minNdcX) || !Double.isFinite(minNdcY)) {
                distancia *= 1.5;
                continue;
            }

            double rangoX = maxNdcX - minNdcX;
            double rangoY = maxNdcY - minNdcY;
            double escala = Math.max(rangoX, rangoY) / (2 * relleno);
            if (!Double.isFinite(escala) || escala <= 0) escala = 1;

            ojoX += ((minNdcX + maxNdcX) / 2) * distancia * tanV * aspecto;
            ojoY += ((minNdcY + maxNdcY) / 2) * distancia * tanV;
            distancia *= escala;
        }

        if (!Double.isFinite(ojoX) || !Double.isFinite(ojoY)
                || !Double.isFinite(distancia) || distancia <= 0) {
            return null;
        }
        return new Encuadre(ojoX, ojoY, centroZ, Math.max(250, distancia));
    }

    /**
     * Puntos del piso enfocado: su losa y todos sus bloques, para encuadrarlo
     * con la misma lógica que el edificio completo.
     */
    static List<Point3D> puntosPiso(Geometria geometria, int nivel) {
        if (geometria == null) return List.of();
        PisoGeometria piso = geometria.pisos().get(nivel);
        if (piso == null) return List.of();

        List<Point3D> puntos = new ArrayList<>();
        agregarPuntosCaja(puntos, 0,
                piso.y() + ALTO_UNIDAD / 2 + LOSA_ALTO / 2,
                0,
                piso.ancho() + 2 * LOSA_MARGEN_X, LOSA_ALTO,
                piso.profundidad() + 2 * LOSA_MARGEN_Z);
        for (FilaPiso fila : piso.filas()) {
            for (BloqueMaqueta bloque : fila.bloques()) {
                Point3D posicion = geometria.posiciones().get(bloque.id());
                if (posicion == null) continue;
                agregarPuntosCaja(puntos, posicion.getX(), posicion.getY(), posicion.getZ(),
                        ANCHO_UNIDAD, ALTO_UNIDAD, PROFUNDIDAD_UNIDAD);
            }
        }
        return List.copyOf(puntos);
    }

    private void actualizarEtiquetasPiso() {
        if (capaEtiquetas == null || subEscena == null || camara == null) return;
        double anchoVista = subEscena.getWidth();
        double altoVista = subEscena.getHeight();
        if (anchoVista <= 0 || altoVista <= 0) return;

        double tanV = Math.tan(Math.toRadians(camara.getFieldOfView() / 2));
        double aspecto = anchoVista / altoVista;
        double foco = altoVista / (2 * tanV);
        Proyeccion proyeccion = new Proyeccion(
                camara.getTranslateX() + anchoVista / 2,
                camara.getTranslateY() + altoVista / 2,
                camara.getTranslateZ() - foco,
                tanV, aspecto, anchoVista, altoVista);

        for (EtiquetaPiso etiqueta : etiquetasPiso) {
            boolean dentroDelAmbito = nivelVista == NivelVista.EDIFICIO;
            Point2D pantalla = dentroDelAmbito ? proyectar(etiqueta.ancla(), proyeccion) : null;
            Label nodo = etiqueta.nodo();
            if (pantalla == null) {
                nodo.setVisible(false);
                continue;
            }

            nodo.setVisible(true);
            nodo.autosize();
            double x = pantalla.getX() - nodo.getWidth() - SEPARACION_ETIQUETA_PANTALLA;
            double y = pantalla.getY() - nodo.getHeight() / 2;
            nodo.relocate(
                    limitar(x, 8, Math.max(8, anchoVista - nodo.getWidth() - 8)),
                    limitar(y, 8, Math.max(8, altoVista - nodo.getHeight() - 8)));
        }
    }

    private boolean enAmbito(int nivel) {
        if (nivelVista == NivelVista.EDIFICIO) return true;
        return nivelVista == NivelVista.PISO && nivelEnfocado != null && nivel == nivelEnfocado;
    }

    /** Evita que las etiquetas queden flotando mientras corre una transición. */
    private void ocultarEtiquetasUnidad() {
        for (EtiquetaUnidad etiqueta : etiquetasUnidad) etiqueta.nodo().setVisible(false);
    }

    private Point2D proyectar(Point3D ancla, Proyeccion proyeccion) {
        Point3D punto = mundo.localToParent(ancla);
        double profundidad = punto.getZ() - proyeccion.ojoZ();
        if (profundidad <= 0.001) return null;
        double ndcX = (punto.getX() - proyeccion.ojoX())
                / (profundidad * proyeccion.tanV() * proyeccion.aspecto());
        double ndcY = (punto.getY() - proyeccion.ojoY())
                / (profundidad * proyeccion.tanV());
        return new Point2D(
                (ndcX + 1) * proyeccion.ancho() / 2,
                (ndcY + 1) * proyeccion.alto() / 2);
    }

    private void actualizarEtiquetasUnidad() {
        if (capaEtiquetas == null || subEscena == null || camara == null
                || etiquetasUnidad.isEmpty()) {
            return;
        }
        double anchoVista = subEscena.getWidth();
        double altoVista = subEscena.getHeight();
        if (anchoVista <= 0 || altoVista <= 0) return;

        double tanV = Math.tan(Math.toRadians(camara.getFieldOfView() / 2));
        double aspecto = anchoVista / altoVista;
        double foco = altoVista / (2 * tanV);
        Proyeccion proyeccion = new Proyeccion(
                camara.getTranslateX() + anchoVista / 2,
                camara.getTranslateY() + altoVista / 2,
                camara.getTranslateZ() - foco,
                tanV, aspecto, anchoVista, altoVista);

        double distancia = centroZAuto - (camara.getTranslateZ() - foco);
        double factor = distanciaAuto > 0 ? distancia / distanciaAuto : 1;
        long etiquetasEnAmbito = etiquetasUnidad.stream()
                .filter(etiqueta -> enAmbito(etiqueta.nivel()))
                .count();
        boolean mostrar = factor <= 1.35
                && (etiquetasEnAmbito <= 40 || factor < 0.9);
        for (EtiquetaUnidad etiqueta : etiquetasUnidad) {
            Point2D pantalla = mostrar && enAmbito(etiqueta.nivel())
                    ? proyectar(etiqueta.ancla(), proyeccion) : null;
            Label nodo = etiqueta.nodo();
            if (pantalla == null) {
                nodo.setVisible(false);
                continue;
            }

            nodo.setVisible(true);
            nodo.autosize();
            double x = pantalla.getX() - nodo.getWidth() / 2;
            double y = pantalla.getY() - nodo.getHeight() - 4;
            nodo.relocate(
                    limitar(x, 4, Math.max(4, anchoVista - nodo.getWidth() - 4)),
                    limitar(y, 4, Math.max(4, altoVista - nodo.getHeight() - 4)));
        }
    }

    private void actualizarHover(BloqueMaqueta bloque) {
        if (bloque == bloqueHover) return;

        if (nodoHover != null) {
            if (nodoHover instanceof Group grupo) grupo.getChildren().remove(contornoHover);
            escalar(nodoHover, 1);
        }
        bloqueHover = bloque;
        nodoHover = bloque == null ? null : nodosPorBloque.get(bloque);

        if (nodoHover == null) {
            etiquetaHover.setVisible(false);
            return;
        }

        escalar(nodoHover, 1.045);
        if (contornoHover != null && nodoHover instanceof Group grupo) {
            grupo.getChildren().add(contornoHover);
        }
        etiquetaHover.setText(descripcionHover(bloque));
        etiquetaHover.setVisible(true);
    }

    private void escalar(Node nodo, double factor) {
        nodo.setScaleX(factor);
        nodo.setScaleY(factor);
        nodo.setScaleZ(factor);
    }

    // ---------------------------------------------------------- Navegación

    private void configurarNavegacionTeclado() {
        if (contenedor3d == null) return;
        enlazarTeclado(contenedor3d.getScene());
        contenedor3d.sceneProperty().addListener(
                (observable, anterior, actual) -> enlazarTeclado(actual));
    }

    private void enlazarTeclado(Scene escena) {
        if (escenaTeclado == escena) return;
        if (escenaTeclado != null) {
            escenaTeclado.removeEventHandler(KeyEvent.KEY_PRESSED, manejadorTecla);
        }
        escenaTeclado = escena;
        if (escenaTeclado != null) {
            escenaTeclado.addEventHandler(KeyEvent.KEY_PRESSED, manejadorTecla);
        } else {
            detenerTransicion();
        }
    }

    private void alPresionarTecla(KeyEvent evento) {
        if (evento.getCode() != KeyCode.ESCAPE) return;
        if (nivelVista == NivelVista.EDIFICIO) return;
        if (selectorModo != null && selectorModo.isShowing()) return;
        evento.consume();
        if (transicionActiva()) return;
        volverUnNivel();
    }

    private void volverUnNivel() {
        if (nivelVista == NivelVista.EDIFICIO) return;
        NivelVista destino = nivelVista.volver();
        if (!nivelVista.puedeIrA(destino)) return;
        if (destino == NivelVista.EDIFICIO) volverAlEdificio();
    }

    @FXML
    private void onVolverAlEdificio() {
        if (nivelVista == NivelVista.EDIFICIO || transicionActiva()) return;
        volverAlEdificio();
    }

    /**
     * Entra al piso indicado. Los demás pisos y la estructura se ocultan
     * escalando Y hacia 0 y, recién al terminar, con setVisible(false): no se
     * anima opacity de nodos 3D porque produce artefactos de orden de dibujo.
     */
    private void entrarPiso(int nivel) {
        if (mundo == null || geometria == null || camara == null) return;
        if (transicionActiva() || nivelVista != NivelVista.EDIFICIO) return;
        if (!nivelVista.puedeIrA(NivelVista.PISO)) return;
        if (!geometria.pisos().containsKey(nivel) || !gruposPiso.containsKey(nivel)) return;

        nivelVista = NivelVista.PISO;
        nivelEnfocado = nivel;
        actualizarBarraNavegacion();
        actualizarResumen(bloquesActuales);
        actualizarEtiquetasPiso();
        ocultarEtiquetasUnidad();
        actualizarHover(null);

        List<KeyValue> valores = new ArrayList<>();
        for (Map.Entry<Integer, Group> entrada : gruposPiso.entrySet()) {
            if (entrada.getKey() != nivel) {
                valores.add(new KeyValue(entrada.getValue().scaleYProperty(),
                        ESCALA_OCULTA, Interpolator.EASE_BOTH));
            }
        }
        if (estructuraGeneral != null) {
            valores.add(new KeyValue(estructuraGeneral.scaleYProperty(),
                    ESCALA_OCULTA, Interpolator.EASE_BOTH));
        }
        Encuadre encuadre = encuadreDePuntos(puntosPiso(geometria, nivel));
        valores.addAll(valoresCamara(encuadre));

        iniciarTransicion(valores, () -> {
            for (Map.Entry<Integer, Group> entrada : gruposPiso.entrySet()) {
                if (entrada.getKey() != nivel) entrada.getValue().setVisible(false);
            }
            if (estructuraGeneral != null) estructuraGeneral.setVisible(false);
            aplicarReferenciaAutomatica(encuadre);
            actualizarEtiquetasPiso();
            actualizarEtiquetasUnidad();
        });
    }

    private void volverAlEdificio() {
        if (mundo == null || transicionActiva()) return;
        if (nivelVista == NivelVista.EDIFICIO) return;
        if (!nivelVista.puedeIrA(NivelVista.EDIFICIO)) return;

        nivelVista = NivelVista.EDIFICIO;
        nivelEnfocado = null;
        actualizarBarraNavegacion();
        actualizarResumen(bloquesActuales);
        actualizarHover(null);
        ocultarEtiquetasUnidad();

        List<KeyValue> valores = new ArrayList<>();
        for (Group grupo : gruposPiso.values()) {
            grupo.setVisible(true);
            valores.add(new KeyValue(grupo.scaleYProperty(),
                    ESCALA_VISIBLE, Interpolator.EASE_BOTH));
        }
        if (estructuraGeneral != null) {
            estructuraGeneral.setVisible(true);
            valores.add(new KeyValue(estructuraGeneral.scaleYProperty(),
                    ESCALA_VISIBLE, Interpolator.EASE_BOTH));
        }
        Encuadre encuadre = encuadreDePuntos(puntosEncuadre);
        valores.addAll(valoresCamara(encuadre));

        iniciarTransicion(valores, () -> {
            aplicarReferenciaAutomatica(encuadre);
            actualizarEtiquetasPiso();
            actualizarEtiquetasUnidad();
        });
    }

    /** Cancela la navegación en curso y deja la escena completa en EDIFICIO. */
    private void reiniciarNavegacion() {
        detenerTransicion();
        nivelVista = NivelVista.EDIFICIO;
        nivelEnfocado = null;
        restaurarPisos();
        actualizarBarraNavegacion();
        actualizarEtiquetasPiso();
        actualizarEtiquetasUnidad();
    }

    private void restaurarPisos() {
        for (Group grupo : gruposPiso.values()) {
            grupo.setVisible(true);
            grupo.setScaleY(ESCALA_VISIBLE);
        }
        if (estructuraGeneral != null) {
            estructuraGeneral.setVisible(true);
            estructuraGeneral.setScaleY(ESCALA_VISIBLE);
        }
        arrastrando = false;
    }

    private void actualizarBarraNavegacion() {
        if (barraNavegacion == null) return;
        boolean visible = nivelVista != NivelVista.EDIFICIO && nivelEnfocado != null;
        barraNavegacion.setVisible(visible);
        barraNavegacion.setManaged(visible);
        if (visible && migaNavegacion != null) {
            migaNavegacion.setText("Edificio › " + etiquetaNivel(nivelEnfocado));
        }
    }

    private boolean transicionActiva() {
        return transicion != null;
    }

    private void detenerTransicion() {
        if (transicion == null) return;
        Timeline activa = transicion;
        transicion = null;
        activa.stop();
    }

    private void iniciarTransicion(List<KeyValue> valores, Runnable alTerminar) {
        detenerTransicion();
        if (valores.isEmpty()) {
            alTerminar.run();
            return;
        }
        Timeline nueva = new Timeline(new KeyFrame(DURACION_TRANSICION,
                valores.toArray(new KeyValue[0])));
        transicion = nueva;
        nueva.setOnFinished(evento -> {
            if (transicion != nueva) return;
            transicion = null;
            alTerminar.run();
        });
        nueva.play();
    }

    private Encuadre encuadreDePuntos(List<Point3D> puntos) {
        if (mundo == null || camara == null || subEscena == null || puntos.isEmpty()) return null;
        double anchoVista = subEscena.getWidth();
        double altoVista = subEscena.getHeight();
        if (anchoVista < 60 || altoVista < 60) return null;
        return calcularEncuadre(proyectarAlPadre(puntos), anchoVista, altoVista,
                camara.getFieldOfView(), camara.getNearClip(), RELLENO_ENCUADRE);
    }

    private List<KeyValue> valoresCamara(Encuadre encuadre) {
        if (encuadre == null || camara == null || subEscena == null) return List.of();
        double anchoVista = subEscena.getWidth();
        double altoVista = subEscena.getHeight();
        double foco = altoVista / (2 * Math.tan(Math.toRadians(camara.getFieldOfView() / 2)));
        return List.of(
                new KeyValue(camara.translateXProperty(),
                        encuadre.ojoX() - anchoVista / 2, Interpolator.EASE_BOTH),
                new KeyValue(camara.translateYProperty(),
                        encuadre.ojoY() - altoVista / 2, Interpolator.EASE_BOTH),
                new KeyValue(camara.translateZProperty(),
                        encuadre.centroZ() - encuadre.distancia() + foco, Interpolator.EASE_BOTH));
    }

    @FXML
    private void onRestablecerVista() {
        if (rotacionX == null || rotacionY == null || camara == null) return;
        detenerTransicion();
        nivelVista = NivelVista.EDIFICIO;
        nivelEnfocado = null;
        restaurarPisos();
        actualizarBarraNavegacion();
        actualizarResumen(bloquesActuales);
        rotacionX.setAngle(ANGULO_X_INICIAL);
        rotacionY.setAngle(ANGULO_Y_INICIAL);
        encuadrarAutomatico();
        actualizarEtiquetasPiso();
        actualizarEtiquetasUnidad();
        actualizarHover(null);
    }

    @FXML
    private void onCambiarModo() {
        if (selectorModo == null) return;
        ModoVista nuevo = ModoVista.desdeEtiqueta(selectorModo.getValue());
        if (nuevo == modoVista) return;
        modoVista = nuevo;
        preferencias.put(PREFERENCIA_MODO, nuevo.name());
        filtroClave = null;
        filtroActivo = false;
        filtroFijo = false;
        actualizarHover(null);
        aplicarFiltroMateriales();
        actualizarResumen(bloquesActuales);
        configurarLeyenda();
    }

    private void configurarSelectorModo() {
        if (selectorModo == null) return;
        selectorModo.getItems().setAll(ModoVista.etiquetas());
        selectorModo.setValue(modoVista.etiqueta());
    }

    private void configurarLeyenda() {
        itemsLeyenda.clear();
        if (leyenda == null) return;
        leyenda.getChildren().clear();
        switch (modoVista) {
            case ESTADO -> configurarLeyendaEstado();
            case MORA -> configurarLeyendaMora();
            case RENTA -> configurarLeyendaRenta();
            case VENCIMIENTO -> configurarLeyendaVencimiento();
        }
        actualizarEstiloLeyenda();
    }

    private void configurarLeyendaEstado() {
        leyenda.getChildren().add(tituloLeyenda("Estados"));
        leyenda.getChildren().addAll(
                itemLeyenda("Disponible", "maqueta-punto-disponible", claveEstado(DISPONIBLE)),
                itemLeyenda("Alquilado", "maqueta-punto-alquilado", claveEstado(ALQUILADO)),
                itemLeyenda("Mantenimiento", "maqueta-punto-mantenimiento",
                        claveEstado(MANTENIMIENTO)),
                itemLeyenda("Inactivo", "maqueta-punto-inactivo", claveEstado(INACTIVO)));
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        leyenda.getChildren().add(espacio);
        leyenda.getChildren().add(tituloLeyenda("Mora"));
        leyenda.getChildren().addAll(
                itemLeyenda("Atraso leve", "maqueta-punto-atraso",
                        claveMora(BloqueMaqueta.CategoriaMora.ATRASO_LEVE)),
                itemLeyenda("Moroso", "maqueta-punto-moroso",
                        claveMora(BloqueMaqueta.CategoriaMora.MOROSO)));
    }

    private void configurarLeyendaMora() {
        leyenda.getChildren().add(tituloLeyenda("Mora"));
        for (BloqueMaqueta.CategoriaMora categoria : BloqueMaqueta.CategoriaMora.values()) {
            leyenda.getChildren().add(itemLeyenda(categoria.getEtiqueta(),
                    clasePuntoMora(categoria), claveMora(categoria)));
        }
    }

    private void configurarLeyendaRenta() {
        leyenda.getChildren().add(tituloLeyenda("Renta (S/)"));
        if (rentaMinima == null || rentaMaxima == null) {
            Label vacio = new Label("Sin rentas registradas");
            vacio.getStyleClass().add("maqueta-leyenda-etiqueta");
            vacio.setMinHeight(21);
            vacio.setPrefHeight(21);
            leyenda.getChildren().add(vacio);
            return;
        }
        if (rentaMinima.compareTo(rentaMaxima) == 0) {
            int tono = MaquetaService.tonoRenta(rentaMinima, rentaMinima, rentaMaxima,
                    MaquetaService.TONOS_RENTA);
            leyenda.getChildren().add(itemRentaUnica(rentaMinima, tono));
            return;
        }

        HBox escala = new HBox();
        escala.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < COLORES_RENTA.size(); i++) {
            Region tono = new Region();
            tono.getStyleClass().add("maqueta-renta-tono");
            tono.setStyle("-fx-background-color: " + colorHex(COLORES_RENTA.get(i)) + ";");
            StackPane celda = new StackPane(tono);
            celda.getStyleClass().addAll("maqueta-leyenda-item", "maqueta-renta-celda");
            registrarItemLeyenda(celda, claveRenta(i));
            escala.getChildren().add(celda);
        }
        Label etiquetas = new Label("Mín. " + Tablas.moneda(rentaMinima)
                + "  ·  Máx. " + Tablas.moneda(rentaMaxima));
        etiquetas.getStyleClass().add("maqueta-leyenda-etiqueta");
        leyenda.getChildren().addAll(escala, etiquetas);
        if (hayBloquesSinRenta()) {
            leyenda.getChildren().add(itemLeyenda("Sin renta", "maqueta-punto-sin-renta",
                    claveRenta(null)));
        }
    }

    private void configurarLeyendaVencimiento() {
        leyenda.getChildren().add(tituloLeyenda("Vencimiento"));
        for (BloqueMaqueta.CategoriaVencimiento categoria
                : BloqueMaqueta.CategoriaVencimiento.values()) {
            leyenda.getChildren().add(itemLeyenda(categoria.getEtiqueta(),
                    clasePuntoVencimiento(categoria), claveVencimiento(categoria)));
        }
    }

    private Label tituloLeyenda(String texto) {
        Label titulo = new Label(texto);
        titulo.getStyleClass().add("maqueta-leyenda-titulo");
        return titulo;
    }

    private HBox itemLeyenda(String texto, String clasePunto, String clave) {
        Region punto = new Region();
        punto.getStyleClass().addAll("maqueta-punto", clasePunto);
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("maqueta-leyenda-etiqueta");
        HBox item = new HBox(6, punto, etiqueta);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("maqueta-leyenda-item");
        registrarItemLeyenda(item, clave);
        return item;
    }

    private HBox itemRentaUnica(BigDecimal renta, int tono) {
        int indice = limitarIndiceRenta(tono);
        Region muestra = new Region();
        muestra.getStyleClass().add("maqueta-renta-tono");
        muestra.setStyle("-fx-background-color: " + colorHex(COLORES_RENTA.get(indice)) + ";");
        Label etiqueta = new Label(Tablas.moneda(renta));
        etiqueta.getStyleClass().add("maqueta-leyenda-etiqueta");
        HBox item = new HBox(6, muestra, etiqueta);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("maqueta-leyenda-item");
        registrarItemLeyenda(item, claveRenta(indice));
        return item;
    }

    private void registrarItemLeyenda(Node item, String clave) {
        itemsLeyenda.add(new ItemLeyenda(item, clave));
        item.setOnMouseEntered(evento -> {
            if (!filtroFijo) aplicarFiltro(clave, false);
        });
        item.setOnMouseExited(evento -> {
            if (!filtroFijo) aplicarFiltro(null, false);
        });
        item.setOnMouseClicked(evento -> {
            boolean mismoFiltroFijo = filtroFijo && filtroActivo && clave.equals(filtroClave);
            if (mismoFiltroFijo) aplicarFiltro(null, false);
            else aplicarFiltro(clave, true);
            evento.consume();
        });
    }

    private void aplicarFiltro(String clave, boolean fijo) {
        filtroClave = clave;
        filtroActivo = clave != null;
        filtroFijo = fijo && clave != null;
        aplicarFiltroMateriales();
        actualizarEstiloLeyenda();
    }

    /**
     * Atenúa los bloques que no coinciden con el filtro cambiando su material por
     * la versión precalculada con alfa. No se toca la opacidad del nodo: los bloques
     * siguen participando del picking, del hover y del clic.
     */
    private void aplicarFiltroMateriales() {
        List<Node> atenuados = new ArrayList<>();
        for (Map.Entry<BloqueMaqueta, Node> entrada : nodosPorBloque.entrySet()) {
            BloqueMaqueta bloque = entrada.getKey();
            boolean coincide = !filtroActivo || coincideFiltro(bloque);
            aplicarMateriales(bloque, coincide);
            if (filtroActivo && !coincide) atenuados.add(entrada.getValue());
        }
        if (filtroActivo) ordenarBloques(atenuados);
    }

    private boolean coincideFiltro(BloqueMaqueta bloque) {
        if (filtroClave == null) return true;
        if (modoVista == ModoVista.ESTADO && filtroClave.startsWith("MORA:")) {
            return filtroClave.equals(claveMora(bloque.categoriaMora()));
        }
        return filtroClave.equals(claveCategoria(bloque));
    }

    private String claveCategoria(BloqueMaqueta bloque) {
        return switch (modoVista) {
            case ESTADO -> claveEstado(bloque.estado());
            case MORA -> claveMora(bloque.categoriaMora());
            case RENTA -> claveRenta(tonoRenta(bloque));
            case VENCIMIENTO -> claveVencimiento(bloque.categoriaVencimiento());
        };
    }

    private void aplicarMateriales(BloqueMaqueta bloque, boolean coincide) {
        Box caja = cajasPorBloque.get(bloque);
        if (caja != null) {
            PhongMaterial material = materialParaModo(bloque, coincide);
            if (material != null) caja.setMaterial(material);
        }

        Cylinder marca = marcasPorBloque.get(bloque);
        if (marca == null) return;
        marca.setVisible(modoVista == ModoVista.ESTADO);
        Color color = colorMora(bloque.semaforo());
        if (color == null) return;
        PhongMaterial material = coincide
                ? materialMora(color)
                : materialMoraAtenuado(color);
        if (material != null) marca.setMaterial(material);
    }

    private PhongMaterial materialParaModo(BloqueMaqueta bloque, boolean coincide) {
        return switch (modoVista) {
            case ESTADO -> coincide
                    ? materialPara(bloque.estado())
                    : materialAtenuadoPara(bloque.estado());
            case MORA -> {
                BloqueMaqueta.CategoriaMora categoria = bloque.categoriaMora();
                yield coincide
                        ? materialesCategoriaMora.get(categoria)
                        : materialesCategoriaMoraAtenuados.get(categoria);
            }
            case RENTA -> {
                Integer tono = tonoRenta(bloque);
                if (tono == null) {
                    yield coincide ? materialSinDato : materialSinDatoAtenuado;
                }
                yield coincide
                        ? materialesRenta.get(tono)
                        : materialesRentaAtenuados.get(tono);
            }
            case VENCIMIENTO -> {
                BloqueMaqueta.CategoriaVencimiento categoria = bloque.categoriaVencimiento();
                yield coincide
                        ? materialesVencimiento.get(categoria)
                        : materialesVencimientoAtenuados.get(categoria);
            }
        };
    }

    private Integer tonoRenta(BloqueMaqueta bloque) {
        if (bloque.rentaReferencial() == null) return null;
        int tono = MaquetaService.tonoRenta(bloque.rentaReferencial(), rentaMinima, rentaMaxima,
                MaquetaService.TONOS_RENTA);
        return tono < 0 ? null : tono;
    }

    private boolean hayBloquesSinRenta() {
        return bloquesActuales.stream().anyMatch(bloque -> bloque.rentaReferencial() == null);
    }

    private static String claveEstado(EstadoInmueble estado) {
        return "ESTADO:" + estado.name();
    }

    private static String claveMora(BloqueMaqueta.CategoriaMora categoria) {
        return "MORA:" + categoria.name();
    }

    private static String claveVencimiento(BloqueMaqueta.CategoriaVencimiento categoria) {
        return "VENCIMIENTO:" + categoria.name();
    }

    private static String claveRenta(Integer tono) {
        return tono == null ? "RENTA:SIN" : "RENTA:" + tono;
    }

    private static String clasePuntoMora(BloqueMaqueta.CategoriaMora categoria) {
        return switch (categoria) {
            case AL_DIA -> "maqueta-punto-al-dia";
            case ATRASO_LEVE -> "maqueta-punto-atraso";
            case MOROSO -> "maqueta-punto-moroso";
            case SIN_MORA -> "maqueta-punto-sin-mora";
        };
    }

    private static String clasePuntoVencimiento(BloqueMaqueta.CategoriaVencimiento categoria) {
        return switch (categoria) {
            case VENCIDO -> "maqueta-punto-vencido";
            case HASTA_30_DIAS -> "maqueta-punto-por-vencer";
            case DE_31_A_90_DIAS -> "maqueta-punto-entre-31-y-90";
            case MAS_DE_90_DIAS -> "maqueta-punto-mas-de-90";
            case SIN_CONTRATO -> "maqueta-punto-sin-contrato";
        };
    }

    private static int limitarIndiceRenta(int tono) {
        return Math.max(0, Math.min(COLORES_RENTA.size() - 1, tono));
    }

    private static String colorHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    /**
     * Los materiales atenuados son transparentes; dentro de cada piso se dibujan
     * después de los opacos para reducir los artefactos de orden del búfer de
     * profundidad, sin sacar los bloques de su grupo de piso.
     */
    private void ordenarBloques(List<Node> atenuados) {
        if (gruposBloques.isEmpty() || atenuados.isEmpty()) return;
        for (Group grupo : gruposBloques.values()) {
            List<Node> actuales = new ArrayList<>(grupo.getChildren());
            List<Node> reordenados = new ArrayList<>(actuales.size());
            for (Node nodo : actuales) {
                if (!atenuados.contains(nodo)) reordenados.add(nodo);
            }
            for (Node nodo : actuales) {
                if (atenuados.contains(nodo)) reordenados.add(nodo);
            }
            if (!reordenados.equals(actuales)) grupo.getChildren().setAll(reordenados);
        }
    }

    private void actualizarEstiloLeyenda() {
        for (ItemLeyenda item : itemsLeyenda) {
            boolean activo = filtroActivo && item.clave().equals(filtroClave);
            item.nodo().getStyleClass().remove("filtro-activo");
            if (activo) item.nodo().getStyleClass().add("filtro-activo");
        }
    }

    private void actualizarResumen(List<BloqueMaqueta> bloques) {
        if (resumen == null) return;
        if (bloques == null || bloques.isEmpty()) {
            resumen.setText("Sin inmuebles ubicados");
            return;
        }
        List<BloqueMaqueta> ambito = bloquesEnAmbito(bloques);
        String prefijo = nivelVista == NivelVista.PISO && nivelEnfocado != null
                ? etiquetaNivel(nivelEnfocado) + " · " : "";
        resumen.setText(prefijo + switch (modoVista) {
            case ESTADO -> resumenEstado(ambito);
            case MORA -> resumenMora(ambito);
            case RENTA -> resumenRenta(ambito);
            case VENCIMIENTO -> resumenVencimiento(ambito);
        });
    }

    private List<BloqueMaqueta> bloquesEnAmbito(List<BloqueMaqueta> bloques) {
        if (bloques == null) return List.of();
        if (nivelVista == NivelVista.PISO && nivelEnfocado != null) {
            return bloques.stream().filter(bloque -> bloque.nivel() == nivelEnfocado).toList();
        }
        return bloques;
    }

    private String resumenEstado(List<BloqueMaqueta> bloques) {
        long disponibles = bloques.stream().filter(b -> b.estado() == DISPONIBLE).count();
        long alquilados = bloques.stream().filter(b -> b.estado() == ALQUILADO).count();
        long mantenimiento = bloques.stream().filter(b -> b.estado() == MANTENIMIENTO).count();
        long inactivos = bloques.stream().filter(b -> b.estado() == INACTIVO).count();
        long morosos = bloques.stream()
                .filter(b -> b.semaforo() == SemaforoMorosidad.MOROSO).count();
        long atraso = bloques.stream()
                .filter(b -> b.semaforo() == SemaforoMorosidad.ATRASO_LEVE).count();
        return contar(bloques.size(), "inmueble", "inmuebles")
                + " · " + contar(disponibles, "disponible", "disponibles")
                + " · " + contar(alquilados, "alquilado", "alquilados")
                + " · " + mantenimiento + " en mantenimiento"
                + " · " + contar(inactivos, "inactivo", "inactivos")
                + " · " + contar(morosos, "moroso", "morosos")
                + " · " + contar(atraso, "con atraso leve", "con atraso leve");
    }

    private String resumenMora(List<BloqueMaqueta> bloques) {
        long alDia = contarCategoriaMora(bloques, BloqueMaqueta.CategoriaMora.AL_DIA);
        long atraso = contarCategoriaMora(bloques, BloqueMaqueta.CategoriaMora.ATRASO_LEVE);
        long morosos = contarCategoriaMora(bloques, BloqueMaqueta.CategoriaMora.MOROSO);
        long sinMora = contarCategoriaMora(bloques, BloqueMaqueta.CategoriaMora.SIN_MORA);
        return contar(bloques.size(), "inmueble", "inmuebles")
                + " · " + contar(alDia, "al día", "al día")
                + " · " + contar(atraso, "con atraso leve", "con atraso leve")
                + " · " + contar(morosos, "moroso", "morosos")
                + " · " + contar(sinMora, "sin mora", "sin mora");
    }

    private String resumenRenta(List<BloqueMaqueta> bloques) {
        List<BigDecimal> rentas = bloques.stream()
                .map(BloqueMaqueta::rentaReferencial)
                .filter(renta -> renta != null)
                .toList();
        String base = contar(bloques.size(), "inmueble", "inmuebles");
        if (rentas.isEmpty()) return base + " · Sin rentas registradas";

        BigDecimal minima = rentas.get(0);
        BigDecimal maxima = rentas.get(0);
        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal renta : rentas) {
            if (renta.compareTo(minima) < 0) minima = renta;
            if (renta.compareTo(maxima) > 0) maxima = renta;
            suma = suma.add(renta);
        }
        BigDecimal promedio = suma.divide(BigDecimal.valueOf(rentas.size()), 2,
                RoundingMode.HALF_UP);
        long sinRenta = bloques.size() - rentas.size();
        String texto = base + " · " + contar(rentas.size(), "con renta", "con renta")
                + " · Mín. " + Tablas.moneda(minima)
                + " · Máx. " + Tablas.moneda(maxima)
                + " · Prom. " + Tablas.moneda(promedio);
        if (sinRenta > 0) texto += " · " + contar(sinRenta, "sin renta", "sin renta");
        return texto;
    }

    private String resumenVencimiento(List<BloqueMaqueta> bloques) {
        long vencidos = contarCategoriaVencimiento(bloques,
                BloqueMaqueta.CategoriaVencimiento.VENCIDO);
        long hasta30 = contarCategoriaVencimiento(bloques,
                BloqueMaqueta.CategoriaVencimiento.HASTA_30_DIAS);
        long entre31y90 = contarCategoriaVencimiento(bloques,
                BloqueMaqueta.CategoriaVencimiento.DE_31_A_90_DIAS);
        long masDe90 = contarCategoriaVencimiento(bloques,
                BloqueMaqueta.CategoriaVencimiento.MAS_DE_90_DIAS);
        long sinContrato = contarCategoriaVencimiento(bloques,
                BloqueMaqueta.CategoriaVencimiento.SIN_CONTRATO);
        return contar(bloques.size(), "inmueble", "inmuebles")
                + " · " + contar(vencidos, "vencido", "vencidos")
                + " · " + contar(hasta30, "vence en 30 días o menos",
                        "vencen en 30 días o menos")
                + " · " + contar(entre31y90, "entre 31 y 90 días", "entre 31 y 90 días")
                + " · " + contar(masDe90, "con más de 90 días", "con más de 90 días")
                + " · " + contar(sinContrato, "sin contrato", "sin contrato");
    }

    private long contarCategoriaMora(List<BloqueMaqueta> bloques,
                                     BloqueMaqueta.CategoriaMora categoria) {
        return bloques.stream().filter(b -> b.categoriaMora() == categoria).count();
    }

    private long contarCategoriaVencimiento(List<BloqueMaqueta> bloques,
                                            BloqueMaqueta.CategoriaVencimiento categoria) {
        return bloques.stream().filter(b -> b.categoriaVencimiento() == categoria).count();
    }

    private String contar(long cantidad, String singular, String plural) {
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }

    private void mostrarCargandoEscena() {
        if (contenedor3d == null || !Platform.isSupported(ConditionalFeature.SCENE3D)) return;
        ocultarMensajeEscena();
        ProgressIndicator indicador = new ProgressIndicator();
        indicador.setPrefSize(34, 34);
        indicador.setMaxSize(34, 34);
        Label texto = new Label("Cargando maqueta…");
        texto.getStyleClass().add("maqueta-aviso-detalle");
        VBox aviso = new VBox(10, indicador, texto);
        aviso.setAlignment(Pos.CENTER);
        aviso.setMaxWidth(520);
        aviso.setMouseTransparent(true);
        aviso.getStyleClass().add("maqueta-aviso");
        mensajeEscena = aviso;
        contenedor3d.getChildren().add(aviso);
    }

    private void limpiarEscena() {
        actualizarHover(null);
        limpiarSeleccion();
        detenerTransicion();
        nivelVista = NivelVista.EDIFICIO;
        nivelEnfocado = null;
        if (mundo != null) mundo.getChildren().clear();
        if (capaEtiquetas != null) capaEtiquetas.getChildren().clear();
        mapaBloques.clear();
        mapaPisos.clear();
        gruposPiso.clear();
        gruposBloques.clear();
        nodosPorBloque.clear();
        cajasPorBloque.clear();
        marcasPorBloque.clear();
        etiquetasPiso.clear();
        etiquetasUnidad.clear();
        geometria = null;
        puntosEncuadre = List.of();
        rentaMinima = null;
        rentaMaxima = null;
        bloqueHover = null;
        nodoHover = null;
        estructuraGeneral = null;
        suelo = null;
        if (etiquetaHover != null) etiquetaHover.setVisible(false);
        actualizarBarraNavegacion();
        configurarLeyenda();
    }

    private void actualizarAvisoUbicacion(int sinUbicacion) {
        if (avisoUbicacion == null) return;
        boolean visible = sinUbicacion > 0;
        if (visible) {
            avisoUbicacion.setText(sinUbicacion == 1
                    ? "1 inmueble sin ubicación no se muestra"
                    : sinUbicacion + " inmuebles sin ubicación no se muestran");
        }
        avisoUbicacion.setVisible(visible);
        avisoUbicacion.setManaged(visible);
    }

    private void mostrarMensajeEscena(String tituloTexto, String detalleTexto) {
        if (contenedor3d == null) return;
        ocultarMensajeEscena();

        FontIcon icono = new FontIcon(Feather.ALERT_TRIANGLE);
        icono.getStyleClass().add("maqueta-aviso-icono");

        Label titulo = new Label(tituloTexto);
        titulo.getStyleClass().add("maqueta-aviso-titulo");

        Label detalle = new Label(detalleTexto);
        detalle.getStyleClass().add("maqueta-aviso-detalle");
        detalle.setWrapText(true);
        detalle.setMaxWidth(460);

        VBox aviso = new VBox(8, icono, titulo, detalle);
        aviso.setAlignment(Pos.CENTER);
        aviso.setMaxWidth(520);
        aviso.setMouseTransparent(true);
        aviso.getStyleClass().add("maqueta-aviso");
        mensajeEscena = aviso;
        contenedor3d.getChildren().add(aviso);
    }

    private void ocultarMensajeEscena() {
        if (mensajeEscena == null) return;
        contenedor3d.getChildren().remove(mensajeEscena);
        mensajeEscena = null;
    }

    private String etiquetaNivel(int nivel) {
        if (nivel == 0) return "Planta baja";
        if (nivel < 0) return "Sótano " + Math.abs(nivel);
        return "Piso " + nivel;
    }

    private void mostrarAvisoSin3d() {
        if (ayuda != null) {
            ayuda.setVisible(false);
            ayuda.setManaged(false);
        }

        FontIcon icono = new FontIcon(Feather.ALERT_TRIANGLE);
        icono.getStyleClass().add("maqueta-aviso-icono");

        Label titulo = new Label("La vista 3D no está disponible");
        titulo.getStyleClass().add("maqueta-aviso-titulo");

        Label detalle = new Label("Esta computadora no tiene habilitados los gráficos 3D de JavaFX. "
                + "Puedes seguir usando el resto del sistema normalmente.");
        detalle.getStyleClass().add("maqueta-aviso-detalle");
        detalle.setWrapText(true);
        detalle.setMaxWidth(460);

        VBox aviso = new VBox(8, icono, titulo, detalle);
        aviso.setAlignment(Pos.CENTER);
        aviso.setMaxWidth(520);
        aviso.getStyleClass().add("maqueta-aviso");
        contenedor3d.getChildren().add(aviso);
    }

    private String descripcionHover(BloqueMaqueta bloque) {
        StringBuilder texto = new StringBuilder();
        if (bloque.codigo() != null && !bloque.codigo().isBlank()) {
            texto.append(bloque.codigo()).append(" · ");
        }
        texto.append(bloque.nombre() == null ? "Sin nombre" : bloque.nombre())
                .append(" · ").append(bloque.tipo() == null ? "Sin tipo" : bloque.tipo())
                .append(" · ").append(etiquetaNivel(bloque.nivel()))
                .append(" · ").append(bloque.estado().getEtiqueta());
        if (bloque.arrendatario() != null) texto.append(" · ").append(bloque.arrendatario());
        switch (modoVista) {
            case ESTADO -> {
                if (bloque.semaforo() != null) {
                    texto.append(" · ").append(bloque.semaforo().getEtiqueta());
                }
                if (bloque.inconsistencia()) texto.append(" · Sin contrato vigente");
            }
            case MORA -> texto.append(" · Mora: ").append(textoMora(bloque));
            case RENTA -> texto.append(" · ").append(textoRenta(bloque.rentaReferencial()));
            case VENCIMIENTO -> texto.append(" · ")
                    .append(textoVencimiento(bloque.diasParaVencer()));
        }
        return texto.toString();
    }

    private String textoMora(BloqueMaqueta bloque) {
        if (bloque.estado() != ALQUILADO) return "Sin mora";
        if (bloque.semaforo() == null) return "Sin contrato vigente";
        return bloque.semaforo().getEtiqueta();
    }

    private String descripcionModo(MaquetaService.DetalleMaqueta detalle) {
        return switch (modoVista) {
            case ESTADO -> detalle.estado().getEtiqueta();
            case MORA -> {
                if (detalle.estado() != ALQUILADO) yield "Sin mora";
                if (detalle.contrato() == null) yield "Sin contrato vigente";
                yield detalle.contrato().semaforo().getEtiqueta();
            }
            case RENTA -> detalle.rentaReferencial() == null
                    ? "Sin renta registrada" : Tablas.moneda(detalle.rentaReferencial());
            case VENCIMIENTO -> detalle.contrato() == null
                    ? "Sin contrato vigente"
                    : textoVencimiento(detalle.contrato().diasParaVencer());
        };
    }

    private String textoRenta(BigDecimal renta) {
        return renta == null ? "Sin renta registrada" : "Renta " + Tablas.moneda(renta);
    }

    private String textoVencimiento(Long dias) {
        if (dias == null) return "Sin contrato vigente";
        if (dias < 0) {
            long cantidad = -dias;
            return "Venció hace " + cantidad + (cantidad == 1 ? " día" : " días");
        }
        if (dias == 0) return "Vence hoy";
        return "Vence en " + dias + (dias == 1 ? " día" : " días");
    }

    enum ModoVista {
        ESTADO("Estado"),
        MORA("Mora"),
        RENTA("Renta"),
        VENCIMIENTO("Vencimiento");

        private final String etiqueta;

        ModoVista(String etiqueta) { this.etiqueta = etiqueta; }

        String etiqueta() { return etiqueta; }

        static List<String> etiquetas() {
            List<String> etiquetas = new ArrayList<>();
            for (ModoVista modo : values()) etiquetas.add(modo.etiqueta);
            return List.copyOf(etiquetas);
        }

        static ModoVista desdeEtiqueta(String etiqueta) {
            for (ModoVista modo : values()) {
                if (modo.etiqueta.equals(etiqueta)) return modo;
            }
            return ESTADO;
        }

        static ModoVista desdePreferencia(String valor) {
            if (valor == null || valor.isBlank()) return ESTADO;
            try {
                return valueOf(valor.trim());
            } catch (IllegalArgumentException error) {
                return ESTADO;
            }
        }
    }

    private double limitar(double valor, double minimo, double maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    record FilaPiso(List<BloqueMaqueta> bloques, double z) {
    }

    record PisoGeometria(int nivel, double y, double ancho, double profundidad,
                         List<FilaPiso> filas) {
    }

    record Geometria(double ancho, double profundidad, double alto,
                     double anchoBase, double profundidadBase,
                     double anchoTecho, double profundidadTecho,
                     double desplazamientoY, Map<Integer, PisoGeometria> pisos,
                     Map<Long, Point3D> posiciones) {
    }

    record Encuadre(double ojoX, double ojoY, double centroZ, double distancia) {
    }

    private record EtiquetaPiso(int nivel, Label nodo, Point3D ancla) {
    }

    private record EtiquetaUnidad(int nivel, Label nodo, Point3D ancla) {
    }

    private record ItemLeyenda(Node nodo, String clave) {
    }

    private record Proyeccion(double ojoX, double ojoY, double ojoZ, double tanV,
                              double aspecto, double ancho, double alto) {
    }

}

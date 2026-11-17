package com.ejemplo.alquileres.controller;

import static com.ejemplo.alquileres.model.EstadoInmueble.ALQUILADO;
import static com.ejemplo.alquileres.model.EstadoInmueble.DISPONIBLE;
import static com.ejemplo.alquileres.model.EstadoInmueble.INACTIVO;
import static com.ejemplo.alquileres.model.EstadoInmueble.MANTENIMIENTO;

import com.ejemplo.alquileres.model.EstadoInmueble;
import com.ejemplo.alquileres.model.SemaforoMorosidad;
import com.ejemplo.alquileres.service.MaquetaService;
import com.ejemplo.alquileres.ui.BloqueMaqueta;
import com.ejemplo.alquileres.ui.Notificador;
import com.ejemplo.alquileres.ui.Refrescable;
import com.ejemplo.alquileres.ui.Tareas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

/**
 * Maqueta 3D del edificio de alquileres. Los bloques se cargan desde
 * {@link MaquetaService} fuera del hilo de JavaFX y se convierten en nodos en el hilo FX.
 */
@Component
public class MaquetaController implements Refrescable {

    private static final double ANCHO_UNIDAD = 62;
    private static final double ALTO_UNIDAD = 48;
    private static final double PROFUNDIDAD_UNIDAD = 64;
    private static final double SEPARACION_UNIDAD = 10;
    private static final double SEPARACION_FILA = 38;
    private static final double SEPARACION_PISO = 30;
    private static final double PASO_X = ANCHO_UNIDAD + SEPARACION_UNIDAD;
    private static final double PASO_Y = ALTO_UNIDAD + SEPARACION_PISO;
    private static final double Z_FILA_FRONTAL = -(PROFUNDIDAD_UNIDAD + SEPARACION_FILA) / 2;
    private static final double Z_FILA_TRASERA = (PROFUNDIDAD_UNIDAD + SEPARACION_FILA) / 2;

    private static final double BASE_ALTO = 12;
    private static final double TECHO_ALTO = 8;
    private static final double TECHO_ELEVACION = 14;
    private static final double COLUMNA_ANCHO = 8;
    private static final double COLUMNA_MARGEN = 38;
    private static final double MARGEN_BASE_X = 60;
    private static final double MARGEN_BASE_Z = 55;
    private static final double SEPARACION_ETIQUETA_PANTALLA = 140;
    private static final double RADIO_MARCA = 11.5;
    private static final double ALTO_MARCA = 5;

    private static final double ANGULO_Y_INICIAL = -38;
    private static final double ANGULO_X_INICIAL = 34;
    private static final double ANGULO_X_MINIMO = -8;
    private static final double ANGULO_X_MAXIMO = 65;

    private static final double RELLENO_ENCUADRE = 0.76;
    private static final double FACTOR_ZOOM_CERCA = 0.55;
    private static final double FACTOR_ZOOM_LEJOS = 2.4;

    private static final Color COLOR_DISPONIBLE = Color.web("#2f9e6e");
    private static final Color COLOR_ALQUILADO = Color.web("#3d7fc4");
    private static final Color COLOR_MANTENIMIENTO = Color.web("#e08a2e");
    private static final Color COLOR_INACTIVO = Color.web("#8a94a6");
    private static final Color COLOR_BASE = Color.web("#788393");
    private static final Color COLOR_ESTRUCTURA = Color.web("#6c7688");
    private static final Color COLOR_MORA_MOROSO = Color.web("#e53935");
    private static final Color COLOR_MORA_ATRASO = Color.web("#ffc107");

    @FXML private StackPane contenedor3d;
    @FXML private Pane capaEtiquetas;
    @FXML private Label etiquetaHover, ayuda, avisoUbicacion;
    @FXML private Button btnRestablecer;

    private final MaquetaService service;
    private final Tareas tareas;
    private final Notificador notificador;

    private final Map<EstadoInmueble, PhongMaterial> materiales = new EnumMap<>(EstadoInmueble.class);
    private final Map<Color, PhongMaterial> materialesMora = new HashMap<>();
    private final Map<Node, BloqueMaqueta> mapaBloques = new HashMap<>();
    private final Map<BloqueMaqueta, Node> nodosPorBloque = new HashMap<>();
    private final Map<BloqueMaqueta, String> textosHover = new HashMap<>();
    private final List<EtiquetaPiso> etiquetasPiso = new ArrayList<>();

    private Group mundo;
    private SubScene subEscena;
    private PerspectiveCamera camara;
    private Rotate rotacionX, rotacionY;
    private PhongMaterial materialBase, materialEstructura;
    private Geometria geometria;
    private List<Point3D> puntosEncuadre = List.of();

    private double centroZAuto;
    private double distanciaAuto;

    private double xPresionado, yPresionado;
    private boolean arrastrando;
    private boolean escenaIntentada;
    private BloqueMaqueta bloqueHover;
    private Node nodoHover;
    private VBox mensajeEscena;

    public MaquetaController(MaquetaService service, Tareas tareas, Notificador notificador) {
        this.service = service;
        this.tareas = tareas;
        this.notificador = notificador;
    }

    @FXML
    public void initialize() {
        if (Platform.isFxApplicationThread()) iniciarMaqueta();
        else Platform.runLater(this::iniciarMaqueta);

        // RestriccionesUi desactiva los botones con la palabra "restablecer"; este control
        // solo devuelve la cámara a su posición y no requiere permisos de administrador.
        Platform.runLater(() -> {
            if (btnRestablecer == null) return;
            btnRestablecer.setDisable(false);
            btnRestablecer.setTooltip(null);
        });
    }

    @Override
    public void refrescar() {
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
        agregarIluminacion(raiz3d);
        configurarCamara(raiz3d);
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

        actualizarAvisoUbicacion(resultado.sinUbicacion());
        if (resultado.bloques().isEmpty()) {
            limpiarEscena();
            mostrarMensajeEscena("Sin inmuebles ubicados",
                    "No hay unidades con nivel y orden para dibujar. "
                            + "Asigna una ubicación desde el formulario de inmuebles.");
            return;
        }

        ocultarMensajeEscena();
        construirEscena(resultado.bloques());
    }

    private void mostrarError(Throwable error) {
        String mensaje = "No se pudieron cargar los datos de la maqueta. Intenta nuevamente.";
        notificador.error(mensaje);
        if (contenedor3d != null && mundo != null) {
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

    private void agregarIluminacion(Group raiz3d) {
        AmbientLight ambiente = new AmbientLight(Color.rgb(80, 80, 86));

        PointLight principal = new PointLight(Color.rgb(160, 160, 165));
        principal.setTranslateX(-350);
        principal.setTranslateY(-450);
        principal.setTranslateZ(-800);

        PointLight relleno = new PointLight(Color.rgb(60, 68, 84));
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
        materialBase = material(COLOR_BASE);
        materialEstructura = material(COLOR_ESTRUCTURA);
    }

    private PhongMaterial material(Color color) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(Color.rgb(210, 214, 220));
        material.setSpecularPower(24);
        return material;
    }

    private void construirEscena(List<BloqueMaqueta> bloques) {
        List<BloqueMaqueta> lista = bloques == null ? List.of() : bloques;
        geometria = calcularGeometria(lista);
        puntosEncuadre = calcularPuntosEncuadre();

        mundo.getChildren().clear();
        mapaBloques.clear();
        nodosPorBloque.clear();
        textosHover.clear();
        bloqueHover = null;
        nodoHover = null;

        crearEtiquetasPiso();
        mundo.getChildren().add(crearEstructura());
        for (BloqueMaqueta bloque : lista) mundo.getChildren().add(crearBloque(bloque));

        Platform.runLater(this::actualizarEncuadreYEtiquetas);
    }

    private Geometria calcularGeometria(List<BloqueMaqueta> bloques) {
        Map<Integer, List<BloqueMaqueta>> porNivel = new TreeMap<>();
        for (BloqueMaqueta bloque : bloques) {
            porNivel.computeIfAbsent(bloque.nivel(), nivel -> new ArrayList<>()).add(bloque);
        }
        porNivel.values().forEach(lista ->
                lista.sort(Comparator.comparingInt(BloqueMaqueta::orden).thenComparing(BloqueMaqueta::id)));

        List<Integer> niveles = List.copyOf(porNivel.keySet());
        int columnas = porNivel.values().stream()
                .mapToInt(lista -> (lista.size() + 1) / 2)
                .max().orElse(1);
        double ancho = columnas * ANCHO_UNIDAD + Math.max(0, columnas - 1) * SEPARACION_UNIDAD;
        double profundidad = 2 * PROFUNDIDAD_UNIDAD + SEPARACION_FILA;
        int cantidadNiveles = Math.max(1, niveles.size());
        double alto = cantidadNiveles * ALTO_UNIDAD + Math.max(0, cantidadNiveles - 1) * SEPARACION_PISO;

        double anchoBase = ancho + 2 * MARGEN_BASE_X;
        double profundidadBase = profundidad + 2 * MARGEN_BASE_Z;
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

        Map<Long, Point3D> posiciones = new HashMap<>();
        double anchoFilas = Math.max(0, (columnas - 1) * PASO_X);
        for (Map.Entry<Integer, List<BloqueMaqueta>> entrada : porNivel.entrySet()) {
            List<BloqueMaqueta> lista = entrada.getValue();
            int filaFrontal = (lista.size() + 1) / 2;
            for (int i = 0; i < lista.size(); i++) {
                boolean esFrente = i < filaFrontal;
                int indice = esFrente ? i : i - filaFrontal;
                int cantidadFila = esFrente ? filaFrontal : lista.size() - filaFrontal;
                double x = cantidadFila <= 1
                        ? 0 : -anchoFilas / 2 + indice * anchoFilas / (cantidadFila - 1);
                double z = esFrente ? Z_FILA_FRONTAL : Z_FILA_TRASERA;
                posiciones.put(lista.get(i).id(), new Point3D(x, yPorNivel.get(entrada.getKey()), z));
            }
        }

        return new Geometria(ancho, profundidad, alto, anchoBase, profundidadBase,
                anchoTecho, profundidadTecho, desplazamientoY, niveles, yPorNivel, posiciones);
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

    private void agregarPuntosCaja(List<Point3D> puntos, double centroX, double centroY, double centroZ,
                                   double ancho, double alto, double profundidad) {
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

    private Node crearEstructura() {
        Group estructura = new Group(crearBase(), crearTecho());
        estructura.getChildren().add(crearColumnas());
        return estructura;
    }

    private Node crearBase() {
        Box base = new Box(geometria.anchoBase(), BASE_ALTO, geometria.profundidadBase());
        base.setMaterial(materialBase);
        base.setTranslateY(yBase());
        return base;
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
        for (Integer nivel : geometria.niveles()) {
            Label etiqueta = new Label(etiquetaNivel(nivel));
            etiqueta.getStyleClass().add("maqueta-piso-etiqueta");
            etiqueta.setMouseTransparent(true);
            etiqueta.setVisible(false);
            capaEtiquetas.getChildren().add(etiqueta);

            Point3D ancla = new Point3D(
                    -geometria.ancho() / 2,
                    geometria.yPorNivel().get(nivel),
                    Z_FILA_FRONTAL);
            etiquetasPiso.add(new EtiquetaPiso(etiqueta, ancla));
        }
    }

    private Node crearBloque(BloqueMaqueta bloque) {
        Point3D posicion = geometria.posiciones().get(bloque.id());
        if (posicion == null) return new Group();

        Box caja = new Box(ANCHO_UNIDAD, ALTO_UNIDAD, PROFUNDIDAD_UNIDAD);
        caja.setMaterial(materialPara(bloque.estado()));
        caja.setTranslateX(posicion.getX());
        caja.setTranslateY(posicion.getY());
        caja.setTranslateZ(posicion.getZ());

        Group grupo = new Group(caja);

        if (bloque.estado() == ALQUILADO) {
            Color colorMora = colorMora(bloque.semaforo());
            if (colorMora != null) {
                Cylinder marca = new Cylinder(RADIO_MARCA, ALTO_MARCA, 24);
                marca.setMaterial(materialMora(colorMora));
                marca.setTranslateX(posicion.getX());
                marca.setTranslateY(posicion.getY() - ALTO_UNIDAD / 2 - ALTO_MARCA / 2);
                marca.setTranslateZ(posicion.getZ());
                grupo.getChildren().add(marca);
                mapaBloques.put(marca, bloque);
            }
        }

        mapaBloques.put(grupo, bloque);
        mapaBloques.put(caja, bloque);
        nodosPorBloque.put(bloque, grupo);
        textosHover.put(bloque, descripcionHover(bloque));
        return grupo;
    }

    private PhongMaterial materialPara(EstadoInmueble estado) {
        return materiales.get(estado);
    }

    private PhongMaterial materialMora(Color color) {
        return materialesMora.computeIfAbsent(color, this::material);
    }

    private Color colorMora(SemaforoMorosidad semaforo) {
        if (semaforo == null) return null;
        return switch (semaforo) {
            case MOROSO -> COLOR_MORA_MOROSO;
            case ATRASO_LEVE -> COLOR_MORA_ATRASO;
            case AL_DIA -> null;
        };
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
        if (evento.getButton() != MouseButton.PRIMARY) return;
        xPresionado = evento.getSceneX();
        yPresionado = evento.getSceneY();
        arrastrando = true;
        subEscena.setCursor(Cursor.CLOSED_HAND);
    }

    private void alArrastrar(MouseEvent evento) {
        if (!arrastrando) return;
        double dx = evento.getSceneX() - xPresionado;
        double dy = evento.getSceneY() - yPresionado;
        xPresionado = evento.getSceneX();
        yPresionado = evento.getSceneY();

        rotacionY.setAngle(Math.IEEEremainder(rotacionY.getAngle() + dx * 0.3, 360));
        rotacionX.setAngle(limitar(rotacionX.getAngle() + dy * 0.22, ANGULO_X_MINIMO, ANGULO_X_MAXIMO));
        actualizarEtiquetasPiso();
    }

    private void alSoltar(MouseEvent evento) {
        arrastrando = false;
        if (subEscena != null) subEscena.setCursor(Cursor.OPEN_HAND);
    }

    private void alMover(MouseEvent evento) {
        if (arrastrando) return;
        actualizarHover(mapaBloques.get(evento.getPickResult().getIntersectedNode()));
    }

    private void alSalir(MouseEvent evento) {
        arrastrando = false;
        actualizarHover(null);
        subEscena.setCursor(Cursor.DEFAULT);
    }

    private void alDesplazar(ScrollEvent evento) {
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
        evento.consume();
    }

    private void actualizarEncuadreYEtiquetas() {
        encuadrarAutomatico();
        actualizarEtiquetasPiso();
    }

    private void encuadrarAutomatico() {
        if (mundo == null || camara == null || subEscena == null || puntosEncuadre.isEmpty()) return;
        double anchoVista = subEscena.getWidth();
        double altoVista = subEscena.getHeight();
        if (anchoVista < 60 || altoVista < 60) return;

        Point3D[] puntos = new Point3D[puntosEncuadre.size()];
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < puntos.length; i++) {
            Point3D punto = mundo.localToParent(puntosEncuadre.get(i));
            puntos[i] = punto;
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
        double tanV = Math.tan(Math.toRadians(camara.getFieldOfView() / 2));
        double aspecto = anchoVista / altoVista;

        double ojoX = centroX;
        double ojoY = centroY;
        double distancia = Math.max(500, Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)));
        for (int iteracion = 0; iteracion < 8; iteracion++) {
            double minNdcX = Double.POSITIVE_INFINITY, maxNdcX = Double.NEGATIVE_INFINITY;
            double minNdcY = Double.POSITIVE_INFINITY, maxNdcY = Double.NEGATIVE_INFINITY;
            double profundidadMinima = Double.POSITIVE_INFINITY;
            for (Point3D punto : puntos) {
                double profundidad = distancia + punto.getZ() - centroZ;
                profundidadMinima = Math.min(profundidadMinima, profundidad);
                if (profundidad <= camara.getNearClip()) continue;
                double ndcX = (punto.getX() - ojoX) / (profundidad * tanV * aspecto);
                double ndcY = (punto.getY() - ojoY) / (profundidad * tanV);
                minNdcX = Math.min(minNdcX, ndcX);
                maxNdcX = Math.max(maxNdcX, ndcX);
                minNdcY = Math.min(minNdcY, ndcY);
                maxNdcY = Math.max(maxNdcY, ndcY);
            }

            if (profundidadMinima <= camara.getNearClip()
                    || !Double.isFinite(minNdcX) || !Double.isFinite(minNdcY)) {
                distancia *= 1.5;
                continue;
            }

            double rangoX = maxNdcX - minNdcX;
            double rangoY = maxNdcY - minNdcY;
            double escala = Math.max(rangoX, rangoY) / (2 * RELLENO_ENCUADRE);
            if (!Double.isFinite(escala) || escala <= 0) escala = 1;

            ojoX += ((minNdcX + maxNdcX) / 2) * distancia * tanV * aspecto;
            ojoY += ((minNdcY + maxNdcY) / 2) * distancia * tanV;
            distancia *= escala;
        }

        double foco = altoVista / (2 * tanV);
        distancia = Math.max(250, distancia);
        centroZAuto = centroZ;
        distanciaAuto = distancia;
        camara.setTranslateX(ojoX - anchoVista / 2);
        camara.setTranslateY(ojoY - altoVista / 2);
        camara.setTranslateZ(centroZ - distancia + foco);
        camara.setFarClip(Math.max(6000, distancia * 6));
        actualizarEtiquetasPiso();
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
            Point2D pantalla = proyectar(etiqueta.ancla(), proyeccion);
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

    private void actualizarHover(BloqueMaqueta bloque) {
        if (bloque == bloqueHover) return;

        if (nodoHover != null) escalar(nodoHover, 1);
        bloqueHover = bloque;
        nodoHover = bloque == null ? null : nodosPorBloque.get(bloque);

        if (nodoHover == null) {
            etiquetaHover.setVisible(false);
            return;
        }

        escalar(nodoHover, 1.045);
        etiquetaHover.setText(textosHover.get(bloque));
        etiquetaHover.setVisible(true);
    }

    private void escalar(Node nodo, double factor) {
        nodo.setScaleX(factor);
        nodo.setScaleY(factor);
        nodo.setScaleZ(factor);
    }

    @FXML
    private void onRestablecerVista() {
        if (rotacionX == null || rotacionY == null || camara == null) return;
        rotacionX.setAngle(ANGULO_X_INICIAL);
        rotacionY.setAngle(ANGULO_Y_INICIAL);
        encuadrarAutomatico();
        actualizarEtiquetasPiso();
        actualizarHover(null);
    }

    private void limpiarEscena() {
        if (mundo != null) mundo.getChildren().clear();
        if (capaEtiquetas != null) capaEtiquetas.getChildren().clear();
        mapaBloques.clear();
        nodosPorBloque.clear();
        textosHover.clear();
        etiquetasPiso.clear();
        geometria = null;
        puntosEncuadre = List.of();
        bloqueHover = null;
        nodoHover = null;
        if (etiquetaHover != null) etiquetaHover.setVisible(false);
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
        if (bloque.semaforo() != null) texto.append(" · ").append(bloque.semaforo().getEtiqueta());
        if (bloque.inconsistencia()) texto.append(" · Sin contrato vigente");
        return texto.toString();
    }

    private double limitar(double valor, double minimo, double maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    private record Geometria(double ancho, double profundidad, double alto,
                             double anchoBase, double profundidadBase,
                             double anchoTecho, double profundidadTecho,
                             double desplazamientoY, List<Integer> niveles,
                             Map<Integer, Double> yPorNivel, Map<Long, Point3D> posiciones) {
    }

    private record EtiquetaPiso(Label nodo, Point3D ancla) {
    }

    private record Proyeccion(double ojoX, double ojoY, double ojoZ, double tanV,
                              double aspecto, double ancho, double alto) {
    }

}

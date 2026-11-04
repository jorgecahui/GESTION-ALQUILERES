# AGENTS.md — Sistema de Gestión de Alquileres

Este archivo contiene las reglas **permanentes** del proyecto. Léelo completo antes de cada tarea.
El trabajo se divide en **fases** (`docs/fases/FASE_XX_*.md`). Solo se implementa la fase que el usuario indique.

## 1. Qué estamos construyendo

Aplicación de **escritorio** para administrar alquileres de inmuebles (departamentos, casas, locales, oficinas, cocheras, habitaciones): inmuebles, arrendatarios, contratos, cronograma de cuotas, cobranza con mora, recibos en PDF, mantenimiento, reportes en Excel y un dashboard con indicadores.

Contexto: Perú. Moneda **soles (S/)**, documentos **DNI / CE / Pasaporte / RUC**, recibos internos (no son comprobantes electrónicos SUNAT). **No se maneja IGV** en este proyecto.

## 2. Proyecto de referencia (SOLO LECTURA)

Existe un proyecto de gestión comercial ya terminado que define el **estilo visual, la arquitectura y las tecnologías** que debemos replicar:

- Ruta: `../crud-persona-spring/` (ajústala si el usuario indica otra).
- Análisis completo del referente: `docs/ANALISIS_PROYECTO.md` (si existe copia local).
- **Nunca modifiques el proyecto de referencia.** Solo léelo y copia/adapta lo reutilizable.
- Antes de crear un componente de UI nuevo, revisa si el referente ya tiene uno equivalente (`ui/`, `controller/`, `view/*.fxml`, `css/app.css`) y reutiliza su patrón.

### Equivalencias referente → proyecto nuevo

| Referente (ventas) | Nuevo (alquileres) |
|---|---|
| Persona (cliente) | Persona (arrendatario o garante) |
| Proveedor | Propietario |
| Categoría | Tipo de inmueble |
| Producto | Inmueble |
| Venta / comprobante | Pago / recibo |
| Punto de venta (carrito) | Nuevo contrato y Nuevo pago |
| Compra | Mantenimiento (gasto) |
| Inventario / kardex | Cuentas por cobrar / estado de cuenta |
| Stock | Estado del inmueble (disponible, alquilado…) |
| IGV | No aplica (se reemplaza por mora) |
| `DatosEmpresa` | `DatosArrendador` |
| `ComprobantePdfService` | `ReciboPdfService` |
| `ReporteVentasService` | `ReporteCobranzaService` |

## 3. Stack tecnológico (versiones fijas, iguales al referente)

| Área | Tecnología |
|---|---|
| Lenguaje | Java 17 (`java.version=17`; compatible con JDK 17 o superior) |
| Build | Maven con wrapper (`mvnw`, `mvnw.cmd`), plugins Spring Boot y `javafx-maven-plugin` 0.0.8 |
| Contenedor / datos | Spring Boot **3.3.4** (parent), Spring Data JPA, Bean Validation, Hibernate, `hibernate-community-dialects`, Lombok (opcional) |
| Base de datos | SQLite con `org.xerial:sqlite-jdbc` **3.45.3.0**, dialecto `SQLiteDialect`, Hikari con **máximo 1 conexión** |
| UI | JavaFX Controls + FXML **21.0.4**, AtlantaFX Base **2.0.1** (tema Primer claro/oscuro), Ikonli JavaFX + Feather Pack **12.3.1** |
| PDF | OpenPDF **1.3.43** |
| Excel | Apache POI `poi-ooxml` **5.2.5** |
| Gráficos | JavaFX Charts (sin librería externa) |
| Pruebas | JUnit 5 vía `spring-boot-starter-test` |

**Regla de dependencias:** no agregues librerías que no estén en esta tabla sin avisar y justificarlo. Única excepción prevista: `spring-security-crypto` (solo BCrypt) en la Fase 7.

Paquete base: `com.ejemplo.alquileres`. Coordenadas Maven: `com.ejemplo:gestion-alquileres:1.0.0`. Clase principal de arranque: `Launcher` (mismo patrón `Launcher` + `JavaFxApplication` + `Application` Spring del referente). Aplicación **sin servidor web** (`spring.main.web-application-type=none`).

## 4. Arquitectura y paquetes

Capas: `controller → service → repository → model`, igual que el referente. FXML solo declara nodos; los controladores coordinan eventos; los servicios concentran reglas, cálculos y transacciones; los repositorios son Spring Data JPA con JPQL.

```text
com.ejemplo.alquileres/
├── Launcher, AlquileresApplication, JavaFxApplication
├── config/        AppProperties, ClockConfig, DatosArrendador
├── controller/    un controlador por pantalla (+ MainController)
├── exception/     NegocioException
├── model/         entidades JPA y enums
├── repository/    interfaces Spring Data
├── seeder/        datos de ejemplo (solo perfil dev)
├── service/       casos de uso, cálculos, PDF y Excel
└── ui/            utilidades reutilizables (Tablas, Paginador, Formularios,
                   Notificador, Autocompletar, Refrescable, Exportador, Tareas…)
resources/
├── application.properties, application-dev.properties
├── css/app.css
└── view/*-view.fxml
```

## 5. Convenciones de código

- **Idioma:** nombres de dominio en español (`Contrato`, `Cuota`, `Inmueble`), igual que el referente. Comentarios y mensajes de UI en español. Código técnico genérico (utilidades) puede estar en inglés si es lo natural.
- **Codificación UTF-8** en todo (`project.build.sourceEncoding=UTF-8`, FXML con `<?xml version="1.0" encoding="UTF-8"?>`). Los textos de la UI llevan tildes y ñ correctas ("Evolución", "Último", "Cobranza"). Verifica que se vean bien en Windows.
- **Dinero:** `BigDecimal`, escala 2 (mora y tasas pueden usar más internamente), redondeo `RoundingMode.HALF_UP`. Nunca `double` para montos.
- **Fechas:** `LocalDate` para fechas de negocio, `LocalDateTime` para auditoría. **Nunca** llames `LocalDate.now()` directo en servicios: inyecta el `Clock` (`ClockConfig`) para poder probar con fechas fijas.
- **Enums** persistidos con `@Enumerated(EnumType.STRING)`.
- **IDs:** `Long`, `IDENTITY`.
- **Concurrencia optimista:** `Contrato`, `Cuota`, `Inmueble` y `Pago` llevan `@Version private Long version`.
- **Snapshots:** los documentos históricos (pago, detalle de pago) guardan copia de nombres, códigos y montos; no dependen de cambios posteriores.
- **Anular, no borrar:** pagos y contratos no se eliminan; se anulan/finalizan y quedan en el historial. Los catálogos se desactivan si tienen movimientos.
- **Errores de negocio:** lanza `NegocioException` con mensaje claro en español; la UI lo muestra con `Notificador` o diálogo.
- **Transacciones:** `@Transactional` en servicios de escritura. Mantén las transacciones cortas.
- **Hilos y SQLite:** el pool es de 1 conexión. Las consultas pesadas se ejecutan fuera del hilo de JavaFX con `ui/Tareas` (un único ejecutor secuencial para base de datos) y el resultado se aplica con `Platform.runLater`. Nunca bloquees el hilo de JavaFX con consultas.
- **Llaves foráneas en SQLite:** la URL JDBC debe activar `foreign_keys=on` (verifícalo con una prueba).
- **Numeraciones** (`INM-0001`, `CTR-000001`, recibos `R001-00000001`, `MNT-000001`) se generan con `SecuenciaService` (tabla `secuencia`, actualización atómica dentro de la transacción). No uses `MAX + 1`.
- **Lombok:** permitido igual que en el referente (inyección por constructor con `@RequiredArgsConstructor`, getters/setters en entidades).

## 6. Diseño visual

Debe verse **casi igual** al referente: mismo shell (menú lateral agrupado + área de contenido), tema AtlantaFX Primer claro/oscuro, mismas clases CSS (`.app`, `.sidebar`, `.nav-item`, `.pagina`, `.tarjeta`, `.tabla`, `.drawer`, `.dialogo`, `.chip`, `.kpi`), mismos radios, sombras y espaciados, e iconos Feather.

Guía completa: **`docs/DISENO_UI.md`**. Copia `css/app.css` del referente como base y extiéndelo; no reinventes estilos.

## 7. Cómo trabajar cada fase

1. Lee `AGENTS.md`, `docs/DISENO_UI.md`, `docs/00_PLAN_MAESTRO.md` y el archivo de la fase pedida.
2. **Presenta primero un plan corto** (archivos que crearás/modificarás y orden). Luego implementa.
3. Implementa **solo** lo que dice la fase. No adelantes funcionalidades de fases futuras. Si algo falta o es ambiguo, toma la decisión más simple coherente con este documento y anótala en `docs/PROGRESO.md` (sección "Decisiones").
4. No rompas lo ya entregado en fases previas. Si debes tocar código anterior, indícalo.
5. Al terminar:
   - `mvnw.cmd -q test` (Windows) o `./mvnw -q test` debe pasar.
   - La aplicación debe **arrancar y abrirse** (`mvnw.cmd javafx:run`).
   - Marca los criterios de aceptación en `docs/PROGRESO.md`.
   - Si el proyecto es un repositorio git, haz un commit: `fase-N: <resumen>`.
6. Entrega un resumen breve: qué se hizo, archivos clave, decisiones tomadas, pendientes y cómo probarlo manualmente.

## 8. Prohibiciones

- No modificar el proyecto de referencia.
- No añadir dependencias fuera de la tabla de stack sin permiso.
- No dejar `TODO` sin registrar en `docs/PROGRESO.md`.
- No dejar datos de ejemplo activos fuera del perfil `dev`.
- No usar `double`/`float` para dinero.
- No ejecutar consultas a base de datos en el hilo de JavaFX.
- No borrar físicamente pagos ni contratos.

## 9. Definición de terminado (aplica a toda fase)

- Compila y las pruebas pasan.
- La app abre sin errores en consola (salvo logs informativos).
- Todas las pantallas de la fase funcionan en **modo claro y oscuro**.
- Textos con tildes correctas y estados vacíos amigables ("Sin cobros en el periodo.").
- Validaciones con mensajes claros; no hay stack traces visibles para el usuario.
- Criterios de aceptación de la fase cumplidos y marcados en `docs/PROGRESO.md`.

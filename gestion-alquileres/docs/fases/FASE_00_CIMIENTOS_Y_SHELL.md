# FASE 0 — Cimientos y shell

**Prerrequisitos:** ninguno. Es la primera fase.

## 1. Objetivo

Crear el proyecto Maven desde cero, con la misma arquitectura, tecnologías y **aspecto visual** del referente, y dejar una aplicación que **abre**, muestra el menú lateral completo, cambia entre modo claro/oscuro y contrae el menú. Los módulos aún no construidos muestran una vista "Próximamente".

## 2. Alcance

**Incluye**
- Proyecto Maven `com.ejemplo:gestion-alquileres:1.0.0` con las dependencias y versiones de `AGENTS.md`, wrapper Maven copiado del referente y `javafx-maven-plugin` configurado con `Launcher` como clase principal.
- Arranque Spring + JavaFX con el patrón `Launcher` → `AlquileresApplication` → `JavaFxApplication`.
- Perfiles: base (producción) y `dev`.
- Shell de UI: `main-view.fxml`, `MainController`, `app.css` (copiado/adaptado del referente), tema Primer claro/oscuro con preferencia persistente, menú contraíble.
- Utilidades de UI genéricas copiadas/adaptadas del referente.
- Infraestructura: `Clock`, `AppProperties`, `NegocioException`, `Tareas`, `Navegacion`.
- Vista placeholder reutilizable.
- Prueba de arranque del contexto Spring.
- `.gitignore`, `README.md` corto y `docs/PROGRESO.md`.

**No incluye**
- Ninguna entidad de negocio, ningún CRUD, ningún dato de ejemplo.
- Nada de login, configuración en pantalla ni reportes.

## 3. Configuración

### `application.properties` (base / producción)
- `spring.main.web-application-type=none`
- `spring.datasource.url=jdbc:sqlite:${app.data-dir}/alquileres.db?foreign_keys=on` (agregar `busy_timeout` si es compatible con sqlite-jdbc)
- Hikari `maximum-pool-size=1`
- `spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect`
- `spring.jpa.hibernate.ddl-auto=update`, `spring.jpa.open-in-view=false`, `show-sql=false`
- `app.data-dir=${user.home}/.gestion-alquileres` (crear la carpeta si no existe antes de abrir la conexión)
- `app.seeder.enabled=false`
- `app.alertas.dias-vencimiento=60`
- `app.morosidad.umbral-leve-dias=15`, `app.morosidad.cuotas-rojo=2`
- `app.mora.tipo=PORCENTAJE_DIARIO`, `app.mora.valor=0.10`, `app.mora.dias-gracia=3`
- `app.recibos.serie=R001`
- `app.arrendador.*` (razón social, documento, dirección, teléfono, email) con valores de muestra
- Logging: raíz `WARN`, paquete `com.ejemplo.alquileres` `INFO`

### `application-dev.properties`
- `app.data-dir=./data`
- `app.seeder.enabled=true`
- `spring.jpa.show-sql=true`, nivel `DEBUG` solo para SQL de Hibernate
- El plugin JavaFX debe ejecutar con `-Dspring.profiles.active=dev` al correr `javafx:run`.

## 4. Estructura esperada

```text
gestion-alquileres/
├── pom.xml, mvnw, mvnw.cmd, .mvn/
├── .gitignore                      (target/, data/, *.db, logs/, .idea/, *.iml)
├── README.md
├── docs/                           (estos documentos)
└── src/
    ├── main/java/com/ejemplo/alquileres/
    │   ├── Launcher.java
    │   ├── AlquileresApplication.java
    │   ├── JavaFxApplication.java
    │   ├── config/        AppProperties, ClockConfig
    │   ├── controller/    MainController, PlaceholderController
    │   ├── exception/     NegocioException
    │   └── ui/            Tablas, Paginador, Formularios, Notificador, Autocompletar,
    │                      Refrescable, Exportador, Tareas, Navegacion, PreferenciaTema, Chips
    ├── main/resources/
    │   ├── application.properties, application-dev.properties
    │   ├── css/app.css
    │   └── view/          main-view.fxml, placeholder-view.fxml
    └── test/java/com/ejemplo/alquileres/
        └── ContextoTest.java, NavegacionTest.java
```

## 5. Qué copiar y qué no del referente

- **Copiar y adaptar:** `pom.xml` (cambiar coordenadas), `mvnw*`/`.mvn`, `Launcher`, `JavaFxApplication`, `MainController` (navegación, tema, contraer), `main-view.fxml`, **todo** `app.css`, utilidades genéricas de `ui/` (`Tablas`, `Paginador`, `Formularios`, `Notificador`, `Autocompletar`, `Refrescable`, `Exportador`).
- **No copiar:** `CeldasCarrito`, servicios, entidades, repositorios, seeders, FXML de ventas/compras/inventario, `CalculadoraIgv`, `ComprobantePdfService`, `DatosEmpresa`.
- Si una utilidad del referente depende de ventas, extrae solo la parte genérica.

## 6. Shell, navegación y tema

- El menú lateral se define con los grupos e ítems de `00_PLAN_MAESTRO.md` (sección 3) y los iconos de `DISENO_UI.md`.
- `Navegacion`: registro clave → FXML. Para módulos pendientes, la clave carga `placeholder-view.fxml` con título, icono, descripción y el texto "Disponible en la Fase N".
- `MainController` mantiene referencias a pantallas `Refrescable` y las refresca al volver a ellas.
- `PreferenciaTema` guarda y lee tema (claro/oscuro) y estado del menú (contraído/expandido) con `java.util.prefs`. Al iniciar se aplica lo guardado.
- La ventana tiene tamaño inicial ≈ 1280×800 y mínimo ≈ 1100×700, título `Panel administrativo | Contratos, Cobranza e Inmuebles` e icono de ventana (puede ser un PNG simple generado).
- Al cerrar la ventana, el contexto Spring se cierra limpiamente (`Platform.exit` + `SpringApplication.exit`).

## 7. Infraestructura transversal

- `ClockConfig`: bean `Clock` (`Clock.systemDefaultZone()`). Los servicios futuros lo inyectan.
- `AppProperties`: `@ConfigurationProperties("app")` tipado con las propiedades de la sección 3.
- `NegocioException extends RuntimeException`.
- `Tareas`: ejecutor de hilo único para trabajos de base de datos + método `ejecutar(Supplier<T>, Consumer<T> onExito, Consumer<Throwable> onError)` que entrega resultados con `Platform.runLater`. Documenta en el Javadoc por qué es de un solo hilo (pool Hikari de 1 conexión).
- `Chips`: fábrica de chips con clases `.chip-exito`, `.chip-acento`, `.chip-aviso`, `.chip-peligro`, `.chip-neutro` (definirlas en `app.css` con tokens Primer).

## 8. Pruebas obligatorias

- `ContextoTest`: el contexto Spring carga (sin iniciar JavaFX) con base SQLite temporal.
- `NavegacionTest`: cada clave del menú resuelve a un FXML que existe en `resources/view/` (verificar existencia del recurso, sin cargar JavaFX).
- Verificar (con una consulta `PRAGMA foreign_keys`) que las llaves foráneas están activas en la conexión.

## 9. Criterios de aceptación

- [ ] `mvnw.cmd -q test` pasa.
- [ ] `mvnw.cmd javafx:run` abre la ventana sin errores en consola.
- [ ] El menú muestra todos los grupos e ítems de `00_PLAN_MAESTRO.md` con sus iconos.
- [ ] El ítem activo se resalta; los módulos pendientes muestran "Disponible en la Fase N".
- [ ] El aspecto (sidebar, tipografía, radios, sombras, colores) es prácticamente igual al del referente en modo claro **y** oscuro.
- [ ] Contraer/expandir menú funciona y se recuerda al reabrir.
- [ ] El modo oscuro se recuerda al reabrir.
- [ ] Los textos con tildes/ñ se ven bien.
- [ ] El perfil base **no** crea datos de ejemplo; el perfil `dev` usa `./data`.
- [ ] Cerrar la ventana termina el proceso Java (sin procesos colgados).
- [ ] `README.md` explica cómo ejecutar y cómo correr pruebas (Windows y Linux/macOS).

## 10. Prompt para Codex

```text
Lee AGENTS.md, docs/DISENO_UI.md, docs/00_PLAN_MAESTRO.md y docs/fases/FASE_00_CIMIENTOS_Y_SHELL.md.
El proyecto de referencia (solo lectura) está en ../crud-persona-spring/.

Implementa EXCLUSIVAMENTE la Fase 0. Antes de escribir código, preséntame un plan corto con los
archivos que vas a crear y qué tomarás del referente. Luego impleméntalo.

Requisitos clave: mismo aspecto que el referente (copia app.css y el shell), menú completo con
placeholders, tema claro/oscuro persistente, menú contraíble, perfiles base/dev, Clock inyectable,
ui/Tareas con ejecutor secuencial, sin entidades de negocio todavía.

Al terminar: ejecuta las pruebas, arranca la app para comprobar que abre, marca los criterios de
aceptación en docs/PROGRESO.md y dame un resumen con decisiones tomadas y pasos para probar.
```

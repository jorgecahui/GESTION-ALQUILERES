# Gestión de Alquileres

Aplicación de escritorio para administrar inmuebles, arrendatarios, contratos, cobranza con mora, mantenimiento, reportes y respaldos. Está pensada para el mercado peruano: montos en **soles (S/)**, documentos DNI/CE/Pasaporte/RUC y recibos internos (no son comprobantes electrónicos SUNAT).

## Capturas

> El autor debe reemplazar estos marcadores por capturas reales antes de publicar el proyecto.

- `docs/capturas/dashboard.png` — Dashboard con KPIs y gráficos.
- `docs/capturas/contratos.png` — Lista de contratos y detalle de cuotas.
- `docs/capturas/pagos.png` — Registro de pagos y recibo PDF.
- `docs/capturas/configuracion.png` — Configuración por pestañas.
- `docs/capturas/respaldo.png` — Respaldo y restauración.

## Requisitos

- **JDK 17 o superior** para desarrollo (el ejecutable empaquetado incluye su propio runtime).
- Windows 10/11 para generar el instalador con `jpackage`.
- Maven Wrapper incluido (`mvnw.cmd` / `mvnw`); no hace falta instalar Maven.
- Para el instalador MSI opcional: [WiX Toolset](https://wixtoolset.org/) en el `PATH`.

## Ejecución

### Desarrollo

```powershell
.\mvnw.cmd javafx:run
```

En Linux/macOS:

```bash
./mvnw javafx:run
```

El comando de interfaz activa el perfil `dev`, usa los datos de ejemplo y guarda la base en `./data/alquileres.db`.

### Perfil base (producción)

El perfil base no activa seeders, no imprime SQL y escribe los logs en archivo. Para ejecutarlo con el jar delgado:

```powershell
.\mvnw.cmd clean package
Copy-Item target\gestion-alquileres-1.0.0.jar target\paquete\
java -Dapp.data-dir="$env:USERPROFILE\.gestion-alquileres" -jar target\paquete\gestion-alquileres-1.0.0.jar
```

También se genera `target\gestion-alquileres-1.0.0-exec.jar` (jar ejecutable autocontenido de Spring Boot) para pruebas rápidas:

```powershell
java -Dapp.data-dir="$env:USERPROFILE\.gestion-alquileres" -jar target\gestion-alquileres-1.0.0-exec.jar
```

En Linux/macOS cambia `.\mvnw.cmd` por `./mvnw` y las rutas por las equivalentes.

## Perfiles y datos

| Elemento | `dev` | Base |
|---|---|---|
| Datos de ejemplo | Sí (`app.seeder.enabled=true`) | No |
| SQL en consola | Sí (`spring.jpa.show-sql=true`) | No |
| Carpeta de datos | `./data` | `%USERPROFILE%\.gestion-alquileres` |
| Logs | `./data/logs/app.log` | `%USERPROFILE%\.gestion-alquileres\logs\app.log` |
| Nivel de logs | SQL en DEBUG | `WARN` general, `INFO` de la aplicación |

La carpeta de datos se puede cambiar con `-Dapp.data-dir=...`. Contiene `alquileres.db`, `logs/`, `respaldos/` y el logo copiado desde Configuración.

## Seguridad

- En el primer arranque se crea un único **ADMIN**; después se muestra el login.
- Roles: **ADMIN** (todo), **OPERADOR** (operación diaria, sin anulaciones/condonaciones/configuración) y **CONSULTA** (solo lectura).
- Contraseñas con BCrypt (`spring-security-crypto`), mínimo 8 caracteres con letras y números.
- Tras 5 intentos fallidos la cuenta se bloquea 5 minutos.
- Bloqueo por inactividad configurable (0 = desactivado).
- La autorización se valida en la UI **y** en los servicios.

## Pruebas

```powershell
.\mvnw.cmd -q test
```

La suite cubre catálogos, contratos, cobranza, mora, PDFs, Excel, recordatorios, configuración, autenticación, permisos, usuarios, auditoría y respaldos. Estado actual: **80 pruebas, 0 fallos**.

## Empaquetado

```powershell
powershell -ExecutionPolicy Bypass -File scripts\empaquetar.ps1
```

El script:

1. Ejecuta `mvnw clean package` y las pruebas.
2. Copia el jar delgado y las dependencias a `target\paquete`.
3. Genera un **app-image** con `jpackage` en `target\instalador\Gestión de Alquileres` (incluye runtime Java, icono y ejecutable).
4. Si WiX está instalado, genera además un instalador `.msi`; si no, muestra un aviso y omite ese paso.

Para generar solo el app-image:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\empaquetar.ps1 -SoloAppImage
```

> Verificación realizada: el app-image se generó y el ejecutable arrancó correctamente en este equipo. Falta probarlo en una máquina sin JDK instalado para confirmar la independencia total del entorno.

## Estructura

```text
src/main/java/com/ejemplo/alquileres/
├── config/        propiedades, reloj, arrendador, integridad, inicializadores
├── controller/    controladores JavaFX
├── exception/     NegocioException
├── model/         entidades y enums JPA
├── repository/    repositorios Spring Data
├── seeder/        datos de ejemplo (solo dev)
├── service/       reglas de negocio, PDF, Excel, seguridad, auditoría, respaldos
└── ui/            componentes reutilizables y coordinación de escenas
src/main/resources/
├── application.properties, application-dev.properties
├── css/app.css
├── icons/app.ico
└── view/*.fxml
scripts/            empaquetar.ps1
docs/               manual de usuario, guion de exposición, progreso y fases
```

## Documentación

- `docs/MANUAL_USUARIO.md` — guía por módulo, roles, respaldo y preguntas frecuentes.
- `docs/EXPOSICION.md` — guion de demostración de 8–10 minutos.
- `docs/PROGRESO.md` — estado por fase, decisiones y pendientes.

# FASE 7 — Configuración, seguridad y entrega

**Prerrequisitos:** Fases 0 a 6 terminadas.

## 1. Objetivo

Llevar el sistema a nivel profesional y dejarlo **listo para entregar**: parámetros editables desde la UI, inicio de sesión con roles, auditoría, respaldo y restauración, endurecimiento técnico (perfiles, logs, concurrencia) y empaquetado con instalador/ejecutable. Esta fase cubre las debilidades señaladas en el análisis del referente (sin autenticación, sin respaldo, configuración en texto plano, `ddl-auto=update` sin control, logs SQL).

## 2. Alcance

**Incluye**
- Pantalla **Configuración** con pestañas.
- Entidad `Configuracion` (reemplaza `app.arrendador.*`, `app.mora.*`, etc. como fuente de verdad; las propiedades quedan como valores por defecto iniciales).
- **Login** y **roles** (`ADMIN`, `OPERADOR`, `CONSULTA`), gestión de usuarios.
- **Auditoría** de operaciones sensibles.
- **Respaldo y restauración** de la base de datos.
- Endurecimiento: perfiles, logs en archivo, manejo global de errores, bloqueo por inactividad.
- Empaquetado con `jpackage` y documentación final.

**No incluye**
- Acceso remoto, multiusuario simultáneo en red ni sincronización en la nube.
- Cifrado de la base SQLite (se documenta como mejora futura).

## 3. Configuración (pantalla y entidad)

### `configuracion` (una sola fila, o pares clave-valor; elegir lo más simple y documentarlo)
- **Arrendador:** razón social/nombre, tipo y número de documento, dirección, teléfono, email, **logo** (ruta de imagen copiada a la carpeta de datos).
- **Recibos:** serie (por defecto `R001`), leyenda al pie del recibo.
- **Mora por defecto:** tipo, valor, días de gracia (se copian a cada contrato nuevo).
- **Alertas:** días para "por vencer" de contratos, días del recordatorio preventivo.
- **Semáforo:** umbral de atraso leve (días) y cuántas cuotas vencidas marcan rojo.
- **Plantillas de recordatorio:** editor de las tres plantillas con lista de marcadores disponibles y botón "Restaurar por defecto".
- **Seguridad:** minutos de inactividad para bloqueo (0 = desactivado).

`ConfiguracionService` expone los valores con **caché en memoria** que se invalida al guardar; todos los servicios de fases previas pasan a leer de aquí (y no de `AppProperties`) para esos parámetros. `DatosArrendador` se construye desde esta configuración. El recibo y los PDF usan el logo si existe.

Cambios de configuración se registran en auditoría.

## 4. Seguridad: login, roles y usuarios

- Dependencia permitida: `org.springframework.security:spring-security-crypto` (solo `BCryptPasswordEncoder`; **no** Spring Security completo).
- Entidad `usuario`: `id`, `username` (único, en minúsculas), `nombre`, `password_hash` (BCrypt), `rol`, `activo`, `ultimo_acceso`, `intentos_fallidos`, `bloqueado_hasta`.
- **Primer arranque:** si no hay usuarios, un asistente crea el primer **ADMIN** (usuario, nombre, contraseña con confirmación).
- **Pantalla de login** previa a la ventana principal, con el mismo estilo visual. Tras 5 intentos fallidos, bloqueo de 5 minutos. Mensaje genérico ("Usuario o contraseña incorrectos").
- Política mínima de contraseña: ≥ 8 caracteres, con letras y números.
- `SesionActual` (usuario en memoria) y `Permisos` (helper) que centralizan qué puede hacer cada rol:

| Capacidad | ADMIN | OPERADOR | CONSULTA |
|---|:-:|:-:|:-:|
| Ver todo (dashboard, listas, reportes) | ✔ | ✔ | ✔ |
| Crear/editar catálogos, contratos, pagos, mantenimiento | ✔ | ✔ | ✖ |
| Anular pagos, rescindir/finalizar contratos, condonar mora | ✔ | ✖ | ✖ |
| Configuración, usuarios, respaldo/restauración | ✔ | ✖ | ✖ |

- Los controles no permitidos se **ocultan o se deshabilitan** (con tooltip) y además el **servicio valida** el permiso (no confiar solo en la UI) lanzando `NegocioException("No tiene permiso para esta acción")`.
- Pantalla **Usuarios** (solo ADMIN) dentro de Configuración: crear, editar, activar/desactivar, restablecer contraseña. No se puede desactivar ni degradar al último ADMIN activo.
- **Cerrar sesión** en el pie del menú; muestra el usuario actual y su rol.
- **Bloqueo por inactividad:** tras N minutos sin eventos de mouse/teclado, se muestra la pantalla de bloqueo que pide la contraseña del usuario actual.

## 5. Auditoría

Entidad `auditoria_evento`: `id`, `fecha`, `usuario`, `accion`, `entidad`, `entidad_id`, `detalle` (texto breve, **sin contraseñas ni datos sensibles**).

Registrar: inicio/cierre de sesión y fallos, anulación de pagos, condonación de mora, creación/finalización/rescisión/renovación de contratos, cambios de configuración, gestión de usuarios, respaldos y restauraciones.

Pantalla **Auditoría** (solo ADMIN) en Configuración: tabla con filtros por fecha, usuario y acción, y exportar a Excel.

## 6. Respaldo y restauración

`RespaldoService`:
- **Crear respaldo:** copia consistente de la base con `VACUUM INTO '<archivo>'` (o cierre breve de conexión + copia si no es posible), nombre `alquileres-AAAAMMDD-HHmmss.db`, en la carpeta elegida por el usuario.
- **Respaldo automático al cerrar la app** en `<datos>/respaldos/`, conservando los últimos **10** (rotación).
- **Restaurar:** el usuario elige un archivo `.db`; validar que sea SQLite (cabecera `SQLite format 3`) y que contenga las tablas esperadas; crear antes un respaldo de seguridad de la base actual; sustituir el archivo y **reiniciar** la aplicación (o pedir cerrar y reabrir) con mensaje claro y confirmación explícita.
- Pestaña **Respaldo** en Configuración: botón crear ahora, lista de respaldos automáticos con fecha y tamaño, botón restaurar, ubicación de la carpeta de datos.

## 7. Endurecimiento técnico

- **Perfiles:** el perfil base (producción) debe tener `show-sql=false`, seeder desactivado, logs en nivel `INFO`/`WARN`. `dev` conserva los datos de ejemplo. Documentar cómo activar cada uno.
- **Logs en archivo** en `<datos>/logs/app.log` con **rotación** (tamaño y días máximos) mediante propiedades de Spring Boot (`logging.file.name`, `logging.logback.rollingpolicy.*`).
- **Manejador global de errores no controlados** (`Thread.setDefaultUncaughtExceptionHandler` y manejo en tareas): registra el error en el log y muestra un diálogo amigable con "Copiar detalles".
- **Concurrencia:** confirmar `@Version` en `Contrato`, `Cuota`, `Inmueble` y `Pago` y traducir `ObjectOptimisticLockingFailureException` a "El registro fue modificado por otra operación. Actualice e intente de nuevo". Verificar que `SecuenciaService` es atómico y que las restricciones únicas protegen contra duplicados.
- **Esquema:** mantener `ddl-auto=update` y agregar al arranque una **verificación de integridad** (`PRAGMA integrity_check`, aviso si falla). *Opcional:* evaluar Flyway con SQLite; adoptarlo solo si funciona sin fricción con el dialecto y las entidades existentes, y registrar la decisión en `PROGRESO.md`.
- Revisar que no queden credenciales ni datos reales en el repositorio, y que `.gitignore` excluya `data/`, `*.db` y `logs/`.

## 8. Empaquetado y entrega

- Configurar la compilación para producir un **jar ejecutable** y copiar dependencias (`maven-dependency-plugin`) si hace falta para `jpackage`.
- Script `scripts/empaquetar.ps1` (Windows) que ejecute `mvnw clean package` y `jpackage`:
  - Primero **`--type app-image`** (carpeta con ejecutable, no requiere herramientas adicionales).
  - Opcional: instalador `.exe`/`.msi` si WiX está instalado (documentar el requisito).
  - Nombre "Gestión de Alquileres", versión `1.0.0`, **icono `.ico`**, runtime Java incluido, clase principal `Launcher`, ruta de datos del usuario (`%USERPROFILE%\.gestion-alquileres`).
- Probar el ejecutable en una máquina **sin JDK** instalado (o declarar claramente si no se pudo verificar).
- Documentación:
  - `README.md` final: descripción, capturas (marcadores para que el autor las agregue), requisitos, ejecución, perfiles, pruebas, empaquetado.
  - `docs/MANUAL_USUARIO.md`: guía breve por módulo (flujo típico: crear inmueble → persona → contrato → registrar pago → ver dashboard), roles, respaldo y restauración, preguntas frecuentes.
  - `docs/EXPOSICION.md`: guion de demostración de 8–10 minutos con el orden recomendado de pantallas y los "detalles propios" a destacar (semáforo, mora automática, WhatsApp, rentabilidad, respaldo).

## 9. Estructura de archivos esperada

```text
model/        RolUsuario, Usuario, Configuracion, AuditoriaEvento
repository/   UsuarioRepository, ConfiguracionRepository, AuditoriaRepository
service/      ConfiguracionService, UsuarioService, AutenticacionService, SesionActual, Permisos,
              AuditoriaService, RespaldoService
controller/   LoginController, PrimerUsuarioController, ConfiguracionController, UsuariosController,
              AuditoriaController, RespaldoController, BloqueoController
view/         login-view.fxml, configuracion-view.fxml (con pestañas), bloqueo-view.fxml, …
scripts/      empaquetar.ps1
docs/         MANUAL_USUARIO.md, EXPOSICION.md
```

## 10. Pruebas obligatorias

- `AutenticacionServiceTest`: hash BCrypt (nunca texto plano), login correcto/incorrecto, bloqueo tras 5 fallos y desbloqueo por tiempo (`Clock` fijo), política de contraseña.
- `PermisosTest`: matriz de roles por acción; el servicio rechaza acciones no permitidas aunque la UI no lo impida.
- `UsuarioServiceTest`: no se desactiva ni degrada al último ADMIN; username único.
- `ConfiguracionServiceTest`: valores por defecto desde propiedades, guardado, invalidación de caché, aplicación de mora por defecto a un contrato nuevo.
- `RespaldoServiceTest` (carpeta temporal): crear, rotación a 10, restaurar válido, rechazo de archivo que no es SQLite o sin tablas esperadas.
- `AuditoriaServiceTest`: registra eventos sin datos sensibles.

## 11. Criterios de aceptación

- [ ] Pruebas pasan y la app abre.
- [ ] El primer arranque guía la creación del ADMIN; luego aparece el login.
- [ ] Cada rol ve y puede hacer solo lo que le corresponde (UI y servicios).
- [ ] Configuración guarda y los cambios se reflejan en recibos, mora por defecto y plantillas de recordatorio.
- [ ] La auditoría registra las operaciones sensibles y se puede filtrar/exportar.
- [ ] Crear y restaurar respaldo funciona; el respaldo automático al cerrar rota a 10 archivos.
- [ ] Bloqueo por inactividad funciona y exige contraseña.
- [ ] Perfil base: sin SQL en consola, sin datos de ejemplo, logs en archivo con rotación.
- [ ] Un error inesperado muestra diálogo amigable y queda en el log.
- [ ] Se genera el ejecutable/instalador con icono y funciona fuera del IDE.
- [ ] `README.md`, `MANUAL_USUARIO.md` y `EXPOSICION.md` completos.
- [ ] Todas las fases anteriores siguen funcionando; modo claro/oscuro correctos.

## 12. Prompt para Codex

```text
Lee AGENTS.md, docs/DISENO_UI.md, docs/00_PLAN_MAESTRO.md y docs/fases/FASE_07_CONFIGURACION_SEGURIDAD_ENTREGA.md.
Las fases 0 a 6 ya están terminadas. Referencia (solo lectura): ../crud-persona-spring/.

Implementa EXCLUSIVAMENTE la Fase 7. Es grande: divídela en tres entregas dentro de la misma fase,
presentando plan antes de cada una y verificando que compila y pasa pruebas entre ellas:
  A) Configuración + ConfiguracionService + migración de los servicios para leer de ahí.
  B) Seguridad (login, roles, usuarios, bloqueo) + auditoría.
  C) Respaldo/restauración + endurecimiento técnico + empaquetado y documentación.

Requisitos clave: solo spring-security-crypto (BCrypt), permisos validados también en los servicios,
auditoría sin datos sensibles, respaldo con VACUUM INTO y validación al restaurar, logs rotativos,
perfil base sin SQL ni seeders, Clock inyectado.

Al terminar cada entrega: ejecuta las pruebas, arranca la app y actualiza docs/PROGRESO.md. Al final,
resume decisiones, pendientes y pasos de prueba manual.
```

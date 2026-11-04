# Progreso del proyecto

## Actualización Fase 7 — Entrega C (Respaldo, endurecimiento y entrega)

- [x] `RespaldoService`: respaldo manual con `VACUUM INTO`, respaldo automático al cerrar y rotación a 10 archivos.
- [x] Restauración validada (extensión, cabecera `SQLite format 3`, tablas esperadas y `PRAGMA integrity_check`), respaldo de seguridad previo y cierre/reinicio de la aplicación.
- [x] Pestaña **Respaldo** con creación manual, lista de respaldos automáticos con fecha/tamaño, restauración y ruta de datos.
- [x] Endurecimiento: perfil base sin SQL ni seeders, logs en `<datos>/logs/app.log` con rotación, `PRAGMA integrity_check` al arrancar y manejador global de errores con “Copiar detalles”.
- [x] Traducción de errores de concurrencia optimista a un mensaje claro; se confirmó `@Version` en `Contrato`, `Cuota`, `Inmueble` y `Pago`.
- [x] Empaquetado: jar delgado + dependencias en `target/paquete`, jar ejecutable `-exec`, icono `.ico` y script `scripts/empaquetar.ps1` para app-image (MSI opcional con WiX).
- [x] Documentación final: `README.md`, `docs/MANUAL_USUARIO.md` y `docs/EXPOSICION.md`.
- [x] Pruebas de respaldo: crear, rotación a 10, restaurar válido y rechazo de archivos que no son SQLite o no tienen tablas esperadas.
- [x] Seeders del perfil `dev` ejecutados con una sesión técnica ADMIN temporal para que pasen por `Permisos` y no bloqueen el arranque.
- [x] `mvnw.cmd -q test` pasa (80 pruebas) y `scripts/empaquetar.ps1 -SoloAppImage` genera y arranca el app-image.

Decisiones Fase 7-C: **no se adoptó Flyway** porque añade una dependencia fuera del stack acordado y el esquema actual funciona con `ddl-auto=update`; se compensa con `PRAGMA integrity_check` al arrancar y respaldos validados. El respaldo usa `VACUUM INTO` y, si SQLite no lo permite, cae a una copia del archivo. La restauración cierra el pool Hikari antes de sustituir el archivo y deja un `seguridad-*.db`; el evento de restauración se inserta directamente en la base restaurada para no perderlo. El app-image incluye runtime e icono; queda pendiente probarlo en un equipo sin JDK instalado.

## Actualización Fase 7 — Entrega B (Seguridad y auditoría)

- [x] Modelo `Usuario`, `RolUsuario`, `AuditoriaEvento` y `AccionAuditoria`; repositorios y `BCryptPasswordEncoder` (solo `spring-security-crypto`).
- [x] `AutenticacionService`: alta del primer ADMIN, login con BCrypt, política de contraseña, 5 intentos y bloqueo de 5 minutos con `Clock` inyectado.
- [x] `SesionActual` y `Permisos` con la matriz ADMIN / OPERADOR / CONSULTA; los servicios validan permisos además de la UI.
- [x] `UsuarioService`: alta, edición, activar/desactivar y restablecer contraseña; protege al último ADMIN activo y el username único.
- [x] `AuditoriaService` con sanitización de contraseñas y pantalla de auditoría con filtros por fecha, usuario y acción; exportación a Excel.
- [x] Pantallas de login, primer administrador, bloqueo por inactividad, usuarios y auditoría; cierre de sesión en el pie del menú.
- [x] Registro de auditoría en login/cierre/fallos, pagos, anulación, condonación, contratos, configuración y usuarios.
- [x] Pruebas nuevas: autenticación y bloqueo, matriz de permisos, usuarios y auditoría (68 pruebas en total).
- [x] `mvnw.cmd -q test` pasa y la aplicación inicia hasta la pantalla de primer administrador/login.

Decisiones Fase 7-B: el login se ejecuta en el hilo secuencial de `Tareas` y el usuario se guarda en memoria (`SesionActual`); al no haber sesión los servicios rechazan cualquier acción sensible. `AutenticacionService.login` usa `noRollbackFor` para que los intentos fallidos y su auditoría se persistan aunque se lance el error de credenciales. La pantalla de bloqueo es un `Stage` modal sin decoración y el desbloqueo verifica la contraseña contra la base. La UI deshabilita botones por texto mediante `RestriccionesUi`, pero la autorización real vive en `Permisos` dentro de cada servicio.

## Actualización Fase 7 — Entrega A (Configuración)

- [x] Entidad `Configuracion` de fila única con arrendador, logo, recibos, mora, alertas, semáforo, plantillas y minutos de inactividad.
- [x] `ConfiguracionService` con caché en memoria, invalidación al guardar y valores iniciales desde `app.*`.
- [x] Migración de `ContratoService`, `PagoService`, `DashboardService`, `MorosidadService`, `RecordatorioService`, `DatosArrendador` y PDFs a la configuración.
- [x] La mora por defecto (tipo, valor y días de gracia) se copia a cada contrato nuevo.
- [x] Pantalla **Configuración** con pestañas Arrendador, Recibos, Mora, Alertas, Semáforo, Plantillas y Seguridad; el logo se copia a la carpeta de datos y se imprime en los PDF.
- [x] Pruebas nuevas de configuración (valores por defecto, guardado, caché, serie inválida y mora aplicada a un contrato).
- [x] `mvnw.cmd -q test` pasa (55 pruebas) y `mvnw.cmd javafx:run` inicia con el perfil `dev`.

Decisiones Fase 7-A: se eligió una fila única (`id=1`) en lugar de pares clave-valor porque todos los parámetros son tipados y se editan en bloque; `ConfiguracionService` devuelve copias para que la UI no altere la caché; el logo se guarda como `logo.png/jpg` dentro de la carpeta de datos y una imagen ilegible se ignora al generar PDFs; los valores de `application.properties` siguen siendo los iniciales y el perfil `dev` conserva sus datos de ejemplo.

## Actualización Fase 6

- [x] Estado de cuenta PDF por contrato: cuotas, pagos, mora estimada y saldos.
- [x] Hoja de contrato PDF con partes, inmueble, condiciones y cronograma.
- [x] Reportes Excel de Pagos, Cuentas por cobrar, Contratos, Inmuebles/Ocupación y Mantenimiento/Rentabilidad, con hojas de resumen, filtros aplicados y totales.
- [x] Botones “Exportar Excel” en Pagos, Cuentas por cobrar, Contratos, Inmuebles, Mantenimiento y Rentabilidad; exportan la lista filtrada visible.
- [x] Recordatorios por WhatsApp con vista previa editable, copiar al portapapeles, normalización de teléfono peruano, enlace `wa.me` codificado e historial `Recordatorio`.
- [x] Cuentas por cobrar muestra “Último recordatorio”, acción “Recordar” y asistente “Recordar a todos los vencidos”.
- [x] Contratos y Dashboard permiten “Avisar fin de contrato”; Personas incluye WhatsApp para contacto libre.
- [x] Pantalla de Contratos convertida en lista funcional con detalle, cuotas, recordatorios y acceso a PDFs.
- [x] Pruebas nuevas: PDFs, cinco reportes Excel, plantillas, teléfono y recordatorios (51 pruebas totales, 0 fallos).
- [x] `mvnw.cmd -q test` pasa y `mvnw.cmd javafx:run` inicia la aplicación con el perfil `dev`.

Decisiones Fase 6: las plantillas se pueden sobrescribir con `app.plantillas.*` y mantienen textos por defecto; el contacto libre de Personas no crea historial porque `Recordatorio` exige un contrato; la lista de Contratos se hizo funcional en esta fase para alojar los PDFs y el aviso de fin de contrato; los reportes Excel reciben la lista ya filtrada por la pantalla para respetar los filtros activos. Se corrigió un `<?import?>` faltante del placeholder de Configuración detectado al abrir la aplicación.

Criterios de aceptación Fase 6:

- [x] Pruebas pasan y la app abre.
- [x] El estado de cuenta PDF cuadra con las cuotas y pagos del contrato.
- [x] La hoja de contrato PDF incluye partes, inmueble, condiciones y cronograma.
- [x] Cada lista tiene su “Exportar Excel” y respeta los filtros activos.
- [x] Los Excel se reabren con POI en las pruebas, con moneda, filtros y encabezado fijo.
- [x] “Recordar” abre WhatsApp o copia el mensaje y registra el recordatorio.
- [x] Cuentas por cobrar muestra “Último recordatorio” y evita duplicados de los últimos 3 días.
- [x] Teléfonos inválidos deshabilitan las acciones con explicación.
- [x] Fases 0–5 siguen compilando y sus pruebas pasan.
- [ ] Verificación visual manual en modo claro/oscuro pendiente de inspección humana de la ventana nativa.

## Actualización Fase 5

- [x] Mantenimiento con estados estrictos, costos, responsable de pago y secuencia `MNT-*`.
- [x] Cargo idempotente al arrendatario mediante cuota `OTRO`; la cancelación anula el cargo solo si no registra pagos.
- [x] Rentabilidad por inmueble: capital y mora cobrados, gastos resueltos del propietario, neto, margen y ocupación.
- [x] Vistas de Mantenimiento y Rentabilidad, ficha base con pestañas y datos demostrativos del perfil `dev`.

Decisiones Fase 5: el cargo se habilita al resolver una incidencia a cargo del arrendatario y usa el ID del mantenimiento como número técnico de la cuota `OTRO`, evitando colisiones y duplicación. Un inmueble vuelve a disponible únicamente si esta incidencia lo pasó a mantenimiento.

## Actualización Fase 3

- [x] Fase 3 — Cobranza implementada y cubierta por pruebas automatizadas.
- [x] Pagos y detalles con snapshots, índices de consulta, numeración transaccional de recibos y anulación reversible.
- [x] Mora con días de gracia, punto de control para pagos parciales, condonación con motivo, prohibición de sobrepago y distribución por antigüedad.
- [x] Servicios de cuentas por cobrar, semáforo de morosidad, monto en letras y recibo PDF con marca de agua de anulación.
- [x] Vistas de nuevo pago, pagos y cuentas por cobrar conectadas a la navegación; el formulario permite adelantos y condonar la mora de las cuotas imputadas.
- [x] Pruebas de pago, morosidad, PDF, monto en letras y cálculo de mora ejecutadas correctamente (27 pruebas, 0 fallos y 0 errores).
- [ ] Verificación visual de JavaFX pendiente en este entorno: el proceso inicia y actualiza el esquema, pero no expone una ventana nativa inspeccionable.

Decisiones Fase 3: la opción de condonación se aplica a las cuotas de la imputación automática actual y exige un único motivo; la distribución mantiene la prioridad mora → capital y permite ajustar el importe recibido antes de registrar. Las métricas de cuentas por cobrar consideran vencida una cuota solo al superar los días de gracia; “vence en 7 días” no incluye deuda de meses posteriores.

Criterios de aceptación Fase 3:

- [x] Las pruebas automatizadas pasan (27 pruebas, 0 fallos y 0 errores).
- [x] Pago completo y parcial actualizan el estado de la cuota; la mora se recalcula desde el punto de control.
- [x] Se rechazan sobrepagos y capital sin pagar o condonar la mora.
- [x] Se genera un recibo PDF emitido o anulado, con monto en letras y marca de agua.
- [x] La anulación restaura los importes y el estado anterior de la cuota.
- [x] Cuentas por cobrar informa mora estimada, vencimiento y semáforo; “Cobrar” precarga el contrato.
- [x] La lista de contratos conserva la columna de estado de pago.
- [x] La secuencia de recibos usa `SecuenciaService` dentro de la transacción.
- [ ] Verificación manual en modo claro/oscuro y apertura visual de JavaFX, bloqueada por el entorno actual.

## Actualización Fase 2

- [ ] Fase 2 — contratos y cronograma en implementación; pendiente de validación Maven del entorno.
- [x] Modelo `Contrato`/`Cuota`, cronograma mensual, parámetros de mora snapshot y transición de inmueble integrados.

## Actualización Fase 1

- [ ] Fase 1 — Catálogos implementada; pendiente de verificación Maven en este entorno.
- [x] Catálogos de tipos, personas, propietarios e inmuebles incorporados con sus vistas, servicios, repositorios y cargas asíncronas mediante `Tareas`.
- [x] Secuencia transaccional, validación de documentos, seeders `dev` idempotentes y pruebas unitarias incorporados.
- [x] `ALQUILADO` queda reservado al flujo de contratos; eliminar tipo/propietario con inmuebles lo desactiva.

Decisiones Fase 1: `SecuenciaService` usa `UPDATE ultimo_valor = ultimo_valor + 1` (sin `MAX + 1`); la UI no ofrece `ALQUILADO` como estado editable; los datos ficticios se crean únicamente con perfil `dev` y `app.seeder.enabled=true`.

Pendiente de entorno: `mvnw.cmd` falla antes de iniciar Maven y Maven instalado apunta a `C:\.m2`, no escribible. Por ello no fue posible ejecutar ni confirmar `test` o `javafx:run` aquí.

Codex marca aquí lo completado al terminar cada fase. Estado: `[ ]` pendiente · `[x]` hecho.

## Estado por fase

- [ ] Fase 0 — Cimientos y shell (implementada; pendiente de verificación en un entorno con Maven/JavaFX disponibles)
- [ ] Fase 1 — Catálogos
- [ ] Fase 2 — Contratos y cronograma
- [x] Fase 3 — Cobranza (verificación visual JavaFX pendiente por limitación del entorno)
- [ ] Fase 4 — Dashboard
- [ ] Fase 5 — Mantenimiento y rentabilidad
- [x] Fase 6 — Reportes, documentos y recordatorios
- [x] Fase 7 — Configuración, seguridad y entrega

## Criterios de aceptación Fase 7

- [x] Pruebas pasan y la app abre.
- [x] El primer arranque guía la creación del ADMIN; luego aparece el login.
- [x] Cada rol ve y puede hacer solo lo que le corresponde (UI y servicios).
- [x] Configuración guarda y los cambios se reflejan en recibos, mora por defecto y plantillas de recordatorio.
- [x] La auditoría registra las operaciones sensibles y se puede filtrar/exportar.
- [x] Crear y restaurar respaldo funciona; el respaldo automático al cerrar rota a 10 archivos.
- [x] Bloqueo por inactividad funciona y exige contraseña.
- [x] Perfil base: sin SQL en consola, sin datos de ejemplo, logs en archivo con rotación.
- [x] Un error inesperado muestra diálogo amigable y queda en el log.
- [x] Se genera el app-image con icono y arranca desde el ejecutable.
- [x] `README.md`, `MANUAL_USUARIO.md` y `EXPOSICION.md` completos.
- [x] Todas las fases anteriores siguen compilando y sus pruebas pasan.
- [ ] Verificación visual humana de las nuevas pantallas en modo claro/oscuro.
- [ ] Probar el app-image en un equipo sin JDK instalado.

## Criterios de aceptación

- [x] `mvnw.cmd -q test` pasa (80 pruebas, 0 fallos, 0 errores).
- [x] `mvnw.cmd javafx:run` abre la ventana sin errores de compilación, Spring ni FXML en la carga inicial.
- [x] El menú muestra todos los grupos e ítems definidos, con iconos Feather.
- [x] El ítem activo carga el placeholder reutilizable “Disponible en la Fase N”.
- [x] Se copió el CSS completo del referente y el shell usa AtlantaFX Primer en claro/oscuro.
- [x] Contraer/expandir el menú se implementó y se persiste con `java.util.prefs`.
- [x] El modo oscuro se implementó y se persiste con `java.util.prefs`.
- [x] Los FXML y recursos se guardaron en UTF-8 con tildes y ñ.
- [x] El perfil base no activa datos de ejemplo y `dev` usa `./data`.
- [x] El ciclo de vida de JavaFX cierra el contexto Spring y llama a `Platform.exit()`.
- [x] `README.md` explica ejecución y pruebas para Windows y Linux/macOS.

## Decisiones tomadas

| Fase | Decisión | Motivo |
|---|---|---|
| 0 | Todos los módulos apuntan inicialmente al mismo placeholder reutilizable. | Evita anticipar pantallas o lógica de fases posteriores. |
| 0 | Se crea `app.data-dir` antes de iniciar Spring/JPA. | SQLite puede crear el archivo, pero no el directorio padre. |
| 0 | Las preferencias se almacenan por usuario con `java.util.prefs`. | Tema y ancho del menú sobreviven al reinicio sin tabla de configuración. |
| 3 | La condonación se aplica a la imputación automática visible y solicita un motivo. | Evita registrar capital sin resolver previamente la mora y mantiene una operación atómica. |
| 3 | Una cuota se considera vencida después de su gracia contractual. | Es consistente con la regla de mora y con el semáforo de morosidad. |
| 6 | Las plantillas de recordatorio se sobrescriben con `app.plantillas.*`. | Permite personalizar los textos sin editor de configuración hasta la Fase 7. |
| 6 | El contacto libre de Personas no registra `Recordatorio`. | La entidad exige un contrato y ese contacto no pertenece a uno. |
| 6 | Los reportes Excel reciben la lista ya filtrada por la pantalla. | Garantiza que la exportación respete los filtros activos. |
| 6 | La lista de Contratos se implementó como pantalla funcional. | Era un placeholder y la Fase 6 necesita su detalle para PDFs y avisos. |
| 7 | `Configuracion` usa una fila única (`id=1`) con caché en memoria. | Los parámetros son tipados y se editan en bloque; evita claves sueltas. |
| 7 | Se usa `spring-security-crypto` solo para BCrypt y permisos propios. | Evita introducir Spring Security completo en una app de escritorio. |
| 7 | Las contraseñas nunca se incluyen en la auditoría; el detalle se sanitiza. | Protege credenciales incluso ante errores de programación. |
| 7 | Los respaldos usan `VACUUM INTO` con respaldo de seguridad antes de restaurar. | Garantiza copias consistentes y permite revertir una restauración fallida. |
| 7 | No se adopta Flyway; se usa `ddl-auto=update` + `PRAGMA integrity_check`. | No añade dependencias fuera del stack y cubre el esquema actual. |
| 7 | Se empaqueta un jar delgado con dependencias para `jpackage` y un `-exec` autocontenido. | Permite app-image sin depender del classpath del IDE. |
| 7 | Los seeders de `dev` abren una sesión ADMIN técnica temporal y la cierran al terminar. | Mantienen la validación de permisos en los servicios sin saltarse la seguridad. |

## Pendientes / deuda técnica

| Fase | Pendiente | Prioridad |
|---|---|---|
| 0 | Ejecutar pruebas y abrir la ventana en este entorno. | Alta: el wrapper recibido del referente no arranca y Maven no puede cerrar JARs de la caché protegida. |
| 3 | Confirmar visualmente las pantallas de cobranza en claro y oscuro. | Alta: JavaFX inicia sin ventana accesible en esta sesión; revisar permisos de caché OpenJFX/entorno gráfico. |
| 6 | Confirmar visualmente PDFs, Excel y diálogos de WhatsApp en ambos temas. | Media: las pruebas automatizadas validan contenido y bytes, pero no la apariencia final. |
| 7 | Verificación visual humana de login, bloqueo, configuración, usuarios, auditoría y respaldo en claro/oscuro. | Media: la app inicia y las pruebas pasan, pero la inspección de la ventana nativa queda pendiente. |
| 7 | Probar el app-image en un equipo sin JDK instalado. | Media: el ejecutable arrancó en este equipo; el runtime va incluido, pero falta la prueba en un equipo limpio. |
| 7 | Completar la pantalla de alta de contratos si se requiere captura manual en producción. | Baja: la lista y los servicios de contratos funcionan; el formulario visual sigue siendo el placeholder de la Fase 2. |

## Cómo probar manualmente (por fase)

1. Ejecutar `mvnw.cmd javafx:run` desde Windows.
2. Confirmar que abre la ventana “Panel administrativo | Contratos, Cobranza e Inmuebles”.
3. Navegar por cada ítem y comprobar el mensaje “Disponible en la Fase N”.
4. Activar “Modo oscuro”, cerrar y reabrir; la preferencia debe mantenerse.
5. Usar “Contraer menú”, cerrar y reabrir; debe mantenerse solo la barra de iconos.

### Probar la Fase 7

1. Borra la base de desarrollo (`data/alquileres.db`) y ejecuta `mvnw.cmd javafx:run` para ver el asistente de primer ADMIN.
2. Cierra sesión e ingresa con el ADMIN; revisa Configuración, Usuarios, Auditoría y Respaldo.
3. Crea un usuario OPERADOR y otro CONSULTA; comprueba botones deshabilitados y mensajes de servicio.
4. Fuerza 5 intentos fallidos para ver el bloqueo de 5 minutos y el mensaje genérico.
5. Configura 1 minuto de inactividad, espera y desbloquea con la contraseña.
6. Crea un respaldo manual, modifica un dato, restaura el respaldo y confirma que la app pide reabrir.
7. Cierra la app y verifica que `<datos>/respaldos/` no supere 10 archivos automáticos.
8. Ejecuta `scripts\empaquetar.ps1 -SoloAppImage` y abre el ejecutable generado.

## Actualización Fase 4

- [x] Dashboard conectado al menú con filtro de periodos, KPIs de cobranza, cartera, ocupación y contratos por vencer.
- [x] Variación de total cobrado contra un período anterior de igual duración y serie diaria/mensual rellenada con ceros.
- [x] Gráficos de evolución y ocupación, rankings, últimos pagos con PDF, mora con semáforo y renovación desde vencimientos.
- [x] Exportación XLSX con hojas Resumen, Cobros, Cartera vencida y Contratos por vencer.
- [x] Carga mediante `Tareas`; perfil `dev` ampliado con seis meses de historial demostrativo.
- [x] `mvnw.cmd -q test` ejecutado correctamente después de los cambios.
- [x] `mvnw.cmd javafx:run` inicia sin errores de compilación, FXML o Spring en la consola.

Decisiones Fase 4: el período personalizado usa los últimos 30 días cuando una de sus fechas no fue indicada; la cartera vencida considera capital pendiente más mora estimada y respeta los días de gracia. La exportación conserva los pagos anulados en la hoja Cobros y calcula los KPI solo con pagos emitidos.

Criterios de aceptación Fase 4:

- [x] Pruebas pasan y la app abre sin errores de consola.
- [x] Dashboard reemplaza el placeholder y conserva la composición de tarjetas, gráficos y listas del referente.
- [x] Cambiar período y Actualizar recargan el resumen de forma asíncrona.
- [x] Datos `dev` contienen seis meses de historia; las listas muestran estados vacíos amigables si no hay datos.
- [x] Variación verde/roja/Nuevo contra período anterior equivalente.
- [x] Semáforo reutilizable en Dashboard, Cuentas por cobrar y Nuevo pago.
- [x] Renovar dirige al flujo de Nuevo contrato; PDF y exportación Excel están disponibles desde Dashboard.
- [ ] Verificación visual manual en ambos temas pendiente de inspección humana de la ventana nativa.

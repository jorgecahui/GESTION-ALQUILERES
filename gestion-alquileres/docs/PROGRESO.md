# Progreso del proyecto

## Mejora de la sección Contratos (noviembre 2026)

- [x] El detalle del contrato se rediseñó como una ficha estructurada: código, arrendatario, chip de estado, inmueble, vigencia, renta, día de pago y garantía en una grilla de lectura rápida.
- [x] Acciones agrupadas y consistentes: documentos (Estado de cuenta / Hoja de contrato) y gestión (Renovar, Avisar fin, Finalizar, Rescindir).
- [x] Cuotas y recordatorios se movieron a pestañas para evitar el apilado vertical y dejar más espacio a la información principal.
- [x] La lista principal muestra el estado como chip de color y la vigencia en formato corto para evitar truncamientos.
- [x] El cuerpo del detalle ahora tiene scroll propio (cabecera fija), de modo que la rueda del mouse permite recorrer la información y las pestañas cuando la ventana es baja; las tablas conservan su propio scroll.
- [x] La tabla de cuotas quedó con las columnas esenciales (concepto, vence, monto, estado) y la de recordatorios combina tipo/canal con tooltip en los textos largos.
- [x] Se conservaron los filtros, KPIs, búsqueda, exportación, PDFs, renovación, finalización y rescisión existentes.
- [x] Verificado visualmente en modo claro y con la suite completa en verde.

## Rediseño del panel “Nuevo propietario” (noviembre 2026)

- [x] Panel lateral derecho de 560 px con overlay, cabecera fija, cuerpo con scroll y pie fijo.
- [x] Etiquetas arriba, asteriscos en Tipo de documento/Documento/Nombre y validación en línea.
- [x] Tipo y Documento en dos columnas; Teléfono/Email y Banco/Cuenta en filas de dos columnas.
- [x] Estilos compartidos `panel-formulario`; sin títulos de sección.
- [x] Cierre con ✕, Cancelar o clic en el overlay; se conservan búsqueda, edición y eliminación/desactivación.
- [x] Guardado intacto: mismo `Propietario` enviado a `PropietarioService.guardar`, preservando el estado activo al editar.
- [x] Verificado visualmente en modo claro y prueba de humo de FXML añadida.

## Rediseño del panel “Nueva persona” (noviembre 2026)

- [x] Panel lateral derecho de 560 px con overlay, cabecera fija, cuerpo con scroll y pie fijo.
- [x] Etiquetas arriba, asteriscos en Tipo de documento/Documento/Nombres y validación en línea.
- [x] Tipo y Documento en dos columnas; Nombres/Apellidos y Teléfono/Email en filas de dos columnas.
- [x] Estilos compartidos `panel-formulario`; sin títulos de sección para mantener la ventana limpia.
- [x] Cierre con ✕, Cancelar o clic en el overlay; se conservan búsqueda, edición, desactivación y WhatsApp.
- [x] Guardado intacto: mismo `Persona` enviado a `PersonaService.guardar`, preservando el estado activo al editar.
- [x] Verificado visualmente en modo claro y prueba de humo de FXML añadida.

## Rediseño del panel “Nuevo tipo de inmueble” (noviembre 2026)

- [x] Panel lateral derecho de 560 px con overlay, cabecera fija con título y ✕, cuerpo con scroll y pie fijo.
- [x] Campos con etiquetas arriba y asteriscos en Código/Nombre, validación en línea y estilos compartidos `panel-formulario`.
- [x] Código y Nombre en dos columnas; Descripción opcional a ancho completo.
- [x] Cierre con ✕, Cancelar o clic en el overlay; se conserva el listado y las acciones de editar/eliminar.
- [x] Guardado intacto: mismo `TipoInmueble` enviado a `TipoInmuebleService.guardar`; solo se añadió validación previa.
- [x] Verificado visualmente en modo claro (los estilos usan los mismos tokens del modo oscuro).

## Rediseño del panel “Nuevo inmueble” (noviembre 2026)

- [x] Panel lateral derecho de 560 px con overlay, cabecera fija, cuerpo con scroll y pie fijo con “Cancelar” y “Guardar inmueble”.
- [x] Etiquetas visibles arriba, obligatorios con asterisco y validación en línea para Código y Nombre.
- [x] Formulario compacto en dos bloques con separadores, sin títulos de sección: identificación, clasificación, dirección, área/renta y estado.
- [x] Renta referencial con prefijo “S/” dentro del campo; se reutilizaron los estilos del formulario de Mantenimiento (`panel-formulario`).
- [x] La ficha del inmueble también usa el overlay y los paneles son mutuamente excluyentes (abrir uno cierra el otro).
- [x] Guardado intacto: se sigue enviando el mismo `Inmueble` a `InmuebleService.guardar`; solo se añadió validación previa de Código/Nombre.
- [x] Verificado visualmente en modo claro y oscuro.

## Rediseño del panel “Nuevo mantenimiento” (noviembre 2026)

- [x] Panel lateral derecho de 560 px, altura completa, sombra hacia la izquierda y overlay sobre el contenido; se cierra con la ✕, el botón Cancelar o clic en el overlay.
- [x] Cabecera fija con título/subtítulo y pie fijo con “Cancelar” y “Guardar mantenimiento”; cuerpo con scroll propio.
- [x] Formulario con etiquetas visibles, asteriscos rojos y divisores; se quitaron los títulos de sección (“¿Qué pasó?”, “Clasificación”, “Fechas y costo”) para aligerar la vista.
- [x] Prioridad convertida en grupo de cuatro botones excluyentes (Baja, Media, Alta, Urgente) con Media seleccionada por defecto; sigue enviando `PrioridadMantenimiento` al mismo campo del backend.
- [x] Costo estimado con prefijo “S/” dentro del campo; fechas con formato `dd/mm/aaaa` y calendario nativo.
- [x] Validación en línea para Inmueble, Título y Categoría; el guardado continúa usando `MantenimientoService.registrar` con la misma `Solicitud`.
- [x] Estilos con tokens de color del proyecto, verificados visualmente en modo claro y oscuro.
- [x] Prueba de humo de FXML añadida para `mantenimiento-view.fxml`.

## Revisión funcional y visual (noviembre 2026)

- [x] **Desplegables de entidades**: `Tipo de inmueble`, `Propietario` (Inmuebles) e `Inmueble` (Mantenimiento) muestran nombre/código en lugar de `clase@hash` mediante `StringConverter`.
- [x] **Edición de inmueble alquilado**: se preserva el estado `ALQUILADO` al editar datos descriptivos; el combo queda bloqueado y el servicio ya no rechaza la edición completa.
- [x] **Formularios limpios**: “Nuevo inmueble” limpia tipo/propietario y “Nuevo mantenimiento” limpia el inmueble seleccionado previamente.
- [x] **Semáforo en Nuevo pago**: se muestra el chip de morosidad del contrato y se actualiza tras registrar el pago.
- [x] **Hilos de UI**: los valores de controles JavaFX se capturan antes de enviar trabajo a `Tareas` (configuración, auditoría, filtros, login, mantenimiento, pagos y exportaciones).
- [x] **Validaciones de servicio**: mensajes claros para nombre/código obligatorios, área/renta inválidas, fechas nulas de contrato y pago.
- [x] **Ajustes visuales**: botones primarios (`.boton-primario`), botones outlined, títulos de tarjeta y filas del ranking usan las clases CSS correctas; se corrigieron KPIs de Mantenimiento y el ancho de la columna Inmueble.
- [x] **Rentabilidad**: el selector de periodo muestra las fechas solo para “Personalizado” y recarga al cambiar de periodo.
- [x] Pruebas: **99 en total, 0 fallos** (2 nuevas para edición de inmuebles alquilados y 4 de humo para Mantenimiento, Tipos de inmueble, Personas y Propietarios).

Deuda detectada en esta revisión (no corregida por ser alcance de fase): paginación real en catálogos (el `Paginador` es solo un contenedor), filtro por tipo en Inmuebles, captura de todos los datos del inmueble en el drawer (distrito, dormitorios, baños, amoblado, servicios) y reactivación explícita de personas/propietarios/tipos desactivados.

## Corrección de prioridad alta — Contratos y pagos demo

- [x] **Nuevo contrato** dejó de ser una maqueta: `ContratoFormController` carga inmuebles disponibles y personas activas, calcula fecha fin, total y cronograma en vivo, y registra el contrato con `ContratoService.crear`.
- [x] **Renovar** abre el mismo formulario precargado (inmueble, arrendatario, fecha de inicio obligatoria, renta con incremento y garantía transferida) y llama a `ContratoService.renovar`.
- [x] **Finalizar** y **Rescindir** tienen diálogo con fecha de término, motivo, liquidación de garantía y motivo del descuento; llaman a `ContratoService.terminar`.
- [x] `ContratoService` valida fecha de término, calcula el estado de garantía (`DEVUELTA`, `RETENIDA_PARCIAL`, `SIN_GARANTIA`) y tolera garantías nulas.
- [x] `HistoriaDashboardSeeder` crea `PagoDetalle` para los pagos demo nuevos y repara los pagos `R001-DEV-*` existentes sin detalle. No toca pagos reales.
- [x] Pruebas: 89 en total, 0 fallos. Nuevas: `ContratoServiceTest` (5), `HistoriaDashboardSeederTest` (2) y `FxmlSmokeTest` (2).
- [x] Verificación manual de arranque con perfil `dev`: la reparación de detalles se ejecutó sobre `data/alquileres.db` (25 pagos con su detalle) y la app cargó sin errores de FXML.

Decisiones de esta corrección: el formulario reutiliza el mismo controlador para alta y renovación; en renovación se bloquean inmueble, arrendatario, fecha de inicio y garantía porque el servicio exige conservarlos y traslada la garantía. La reparación de `PagoDetalle` se limita a recibos con prefijo `R001-DEV-` para no alterar pagos reales. Se añadió una prueba de humo de FXML porque los errores de `fx:id`/`onAction` no los detectan las pruebas de servicio.

## Corrección de prioridad media y baja — Presentación y datos demo

- [x] **Ficha del inmueble** conectada a `FichaInmuebleController`: pestañas Datos, Contratos, Mantenimiento e Ingresos/gastos. Los contratos se cargan con `listarPorInmueble` (fetch de arrendatario, inmueble y cuotas) para evitar `LazyInitializationException`.
- [x] **Cronograma demo completo**: los contratos `CTR-DEV-2/3/4` ahora tienen 13 cuotas (mayo 2026–mayo 2027) en lugar de 8. La reparación se aplica a bases existentes y solo toca contratos `CTR-DEV-*`.
- [x] **Conceptos de cuotas en español**: “Renta mayo 2026”, “Renta diciembre 2026”, etc. Se normalizaron los datos demo existentes.
- [x] **Fechas visibles** en `dd/MM/yyyy` en Pagos, Cuentas por cobrar, Mantenimiento, Nuevo pago y ficha del inmueble mediante `ui/Fechas`.
- [x] **Etiquetas legibles** para estados, prioridades, categorías, medios de pago y semáforo: “Por vencer”, “Pagada”, “En proceso”, “Depósito”, “Al día”, etc. Aplicado a tablas, combos, PDFs y Excel.
- [x] **Personas y propietarios inactivos** conservan su estado al editarse; el controlador ya no envía `activo(true)` fijo.
- [x] **Nuevo pago** disponible directamente desde la pantalla Pagos.
- [x] Pruebas: **93 en total, 0 fallos**. Nuevas coberturas de reparación de cronograma, etiquetas/formatos en FXML y preservación de inactivos.

Decisiones: la reparación de cronogramas y conceptos también se limita al prefijo `CTR-DEV-` para no modificar contratos reales; las etiquetas se implementaron con `getEtiqueta()` y `toString()` en los enums, sin cambiar la persistencia (`EnumType.STRING` sigue guardando el nombre técnico).

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
| 7 | Completar la pantalla de alta de contratos si se requiere captura manual en producción. | **Resuelto** en la corrección de prioridad alta: alta, renovación, finalización y rescisión ya tienen UI funcional. |

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

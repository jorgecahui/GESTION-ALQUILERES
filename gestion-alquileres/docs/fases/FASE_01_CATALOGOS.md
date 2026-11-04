# FASE 1 — Catálogos

**Prerrequisitos:** Fase 0 terminada y funcionando.

## 1. Objetivo

Construir los cuatro catálogos base sobre los que se apoyan contratos y cobranza: **Tipos de inmueble, Propietarios, Personas (arrendatarios y garantes) e Inmuebles**, con el mismo patrón de listado + drawer + paginación del referente (Categorías, Proveedores, Personas, Productos). Incluye la numeración automática con `SecuenciaService` y los datos de ejemplo para el perfil `dev`.

## 2. Alcance

**Incluye**
- Entidades, repositorios, servicios, controladores y FXML de los 4 catálogos.
- `Secuencia` + `SecuenciaService` (se usará luego para contratos, recibos y mantenimientos).
- Validadores de documento (DNI, CE, pasaporte, RUC).
- Seeders (solo `dev`).
- Pruebas de servicios y validadores.

**No incluye**
- Contratos, cuotas ni pagos (aunque la ficha del inmueble deja espacio para el historial de contratos, que se llena en la Fase 2).
- Importación masiva desde Excel.

## 3. Modelo de datos

### `secuencia`
`id`, `clave` (único, ej. `INMUEBLE`, `CONTRATO`, `RECIBO-R001`, `MANTENIMIENTO`), `ultimo_valor` (Long).

### `tipo_inmueble`
`id`, `codigo` (único, ej. `TI-DEP`), `nombre`, `descripcion`.
Semilla: Departamento, Casa, Local comercial, Oficina, Cochera, Habitación, Terreno.

### `propietario`
`id`, `tipo_documento`, `numero_documento`, `nombre` (nombre o razón social), `telefono`, `email`, `banco`, `cuenta` (cuenta/CCI), `activo`, `fecha_registro`. Único (`tipo_documento`, `numero_documento`).

### `persona`
`id`, `tipo_documento` (`DNI`, `CE`, `PASAPORTE`, `RUC`), `numero_documento`, `nombres` (o razón social si es RUC), `apellidos` (opcional), `email`, `telefono`, `telefono_secundario`, `direccion`, `ocupacion`, `observaciones`, `activo`, `fecha_registro`. Único (`tipo_documento`, `numero_documento`) → `uk_persona_documento`.

### `inmueble`
`id`, `codigo` (único, `INM-0001`), `nombre` (alias, ej. "Depa 301 · Av. Lima"), `tipo_id` (nullable → `tipo_inmueble`), `propietario_id` (nullable → `propietario`), `direccion`, `referencia`, `distrito`, `provincia`, `departamento`, `piso`, `area_m2` BigDecimal(8,2), `dormitorios`, `banos`, `estacionamientos`, `amoblado` boolean, `renta_referencial` BigDecimal(10,2), `estado` (`DISPONIBLE`, `ALQUILADO`, `MANTENIMIENTO`, `INACTIVO`; por defecto `DISPONIBLE`), `servicios_incluidos` (texto), `descripcion`, `fecha_registro`, `version` (`@Version`).

Índices: `inmueble(estado)`, `inmueble(tipo_id)`, `persona(numero_documento)`.

## 4. Reglas de negocio

- **Documentos:** DNI = 8 dígitos; RUC = 11 dígitos y prefijo válido (10, 15, 17 o 20); CE = 9 a 12 caracteres alfanuméricos; Pasaporte = 6 a 12 alfanuméricos. Validación en `ValidadorDocumento` (clase reutilizable con pruebas) y mensajes claros.
- **Teléfono:** opcional; si se ingresa, aceptar móvil peruano de 9 dígitos que empieza con 9 (con o sin `+51`) o fijo; normalizar al guardar.
- **Email:** opcional, formato válido.
- **Unicidad:** documento por persona y por propietario; código por tipo e inmueble. Mensaje: "Ya existe una persona con ese documento".
- **Código de inmueble:** se propone automáticamente (`INM-0001`) desde `SecuenciaService`; editable solo al crear, y debe seguir siendo único.
- **Estado del inmueble:** la UI **no permite** poner `ALQUILADO` manualmente; solo lo establecen los contratos (Fase 2). Manualmente se puede alternar entre `DISPONIBLE`, `MANTENIMIENTO` e `INACTIVO`, y no se puede tocar un inmueble `ALQUILADO` (salvo editar sus datos descriptivos).
- **Eliminar:** un catálogo con movimientos no se elimina; se **desactiva** (`activo=false` o `INACTIVO`). Se puede eliminar físicamente solo si no tiene referencias (validar con consultas). Un tipo de inmueble con inmuebles asociados no se elimina.
- **Renta referencial** ≥ 0; **área** > 0 si se informa; dormitorios/baños/estacionamientos ≥ 0.
- **Búsqueda:** por nombre, documento, código, dirección (sin distinguir mayúsculas ni tildes si es viable).
- **`SecuenciaService.siguiente(clave)`:** incrementa y devuelve el valor dentro de la transacción; si la clave no existe, la crea en 0. Incluir método `formatear(prefijo, valor, ancho)`.

## 5. Pantallas

| Pantalla | FXML / Controlador | Contenido |
|---|---|---|
| Inmuebles | `inmueble-view.fxml` / `InmuebleController` | Tabla: código, inmueble (celda con icono/avatar + nombre + dirección), tipo, propietario, renta referencial, estado (chip). Filtros: estado, tipo, buscador. Drawer de alta/edición. Acción "Ver ficha" abre un drawer con datos completos (sección "Contratos" vacía por ahora con mensaje "Disponible en la Fase 2"). |
| Tipos de inmueble | `tipo-inmueble-view.fxml` / `TipoInmuebleController` | CRUD equivalente a Categorías del referente. |
| Personas | `persona-view.fxml` / `PersonaController` | Tabla: avatar con iniciales, nombre, documento, teléfono, email, estado. Filtro por tipo de documento y activos. Drawer con validación en línea. Botón de WhatsApp opcional queda para la Fase 6. |
| Propietarios | `propietario-view.fxml` / `PropietarioController` | CRUD equivalente a Proveedores del referente. |

Cada pantalla implementa `Refrescable` y usa `Tareas` para consultar sin bloquear la UI. Al guardar: toast de éxito, cierre del drawer y recarga de la tabla manteniendo la página.

Reemplazar en el menú los placeholders de estos 4 ítems por las vistas reales.

## 6. Estructura de archivos esperada

```text
model/        TipoDocumento, EstadoInmueble, Secuencia, TipoInmueble, Propietario, Persona, Inmueble
repository/   SecuenciaRepository, TipoInmuebleRepository, PropietarioRepository, PersonaRepository, InmuebleRepository
service/      SecuenciaService, ValidadorDocumento, TipoInmuebleService, PropietarioService, PersonaService, InmuebleService
controller/   InmuebleController, TipoInmuebleController, PersonaController, PropietarioController
seeder/       DatabaseSeeder, TipoInmuebleSeeder, PropietarioSeeder, PersonaSeeder, InmuebleSeeder
view/         inmueble-view.fxml, tipo-inmueble-view.fxml, persona-view.fxml, propietario-view.fxml
```

Seeders (solo con `app.seeder.enabled=true`, idempotentes): 7 tipos, 2 propietarios, 12 personas con documentos válidos de ejemplo, 10 inmuebles variados (distintos tipos, estados `DISPONIBLE`, uno en `MANTENIMIENTO`, uno `INACTIVO`) con direcciones y rentas realistas en soles. **Todos los datos deben ser ficticios.**

## 7. Pruebas obligatorias

- `ValidadorDocumentoTest`: DNI/RUC/CE/Pasaporte válidos e inválidos (longitudes, prefijos RUC, caracteres).
- `SecuenciaServiceTest`: incrementa, crea clave nueva, formato con relleno, valores consecutivos sin repetirse.
- `PersonaServiceTest`: alta, documento duplicado, edición sin cambiar documento, desactivación.
- `InmuebleServiceTest`: código autogenerado, no permite `ALQUILADO` manual, filtros por estado/tipo, no elimina un tipo con inmuebles.

## 8. Criterios de aceptación

- [ ] Pruebas pasan y la app abre.
- [ ] Los 4 catálogos permiten crear, editar, buscar, filtrar, paginar y desactivar/eliminar según las reglas.
- [ ] Las validaciones de documento, email y teléfono muestran mensajes claros en el drawer.
- [ ] El código `INM-0001`, `INM-0002`… se genera sin repetirse, incluso tras reiniciar la app.
- [ ] No se puede marcar un inmueble como `ALQUILADO` desde la UI.
- [ ] El perfil `dev` carga los datos de ejemplo una sola vez (reiniciar no duplica).
- [ ] El perfil base arranca con base vacía y las pantallas muestran estados vacíos amigables.
- [ ] Las pantallas se ven bien en modo claro y oscuro y siguen el patrón visual del referente.
- [ ] Ninguna consulta bloquea la UI visiblemente.

## 9. Prompt para Codex

```text
Lee AGENTS.md, docs/DISENO_UI.md, docs/00_PLAN_MAESTRO.md y docs/fases/FASE_01_CATALOGOS.md.
La Fase 0 ya está terminada. El proyecto de referencia (solo lectura) está en ../crud-persona-spring/.

Implementa EXCLUSIVAMENTE la Fase 1. Antes de escribir código, preséntame un plan corto (entidades,
servicios, controladores y FXML a crear, y qué patrones copiarás de Persona/Categoría/Producto/Proveedor
del referente). Luego impleméntalo.

Requisitos clave: Secuencia + SecuenciaService atómico (sin MAX+1), ValidadorDocumento con pruebas,
reglas de estado del inmueble (ALQUILADO solo por contratos), desactivar en lugar de borrar cuando
hay referencias, seeders solo en perfil dev e idempotentes, @Version en Inmueble, consultas fuera del
hilo de JavaFX con ui/Tareas.

Al terminar: ejecuta las pruebas, arranca la app, marca los criterios en docs/PROGRESO.md y dame un
resumen con decisiones y pasos de prueba manual.
```

# FASE 5 — Mantenimiento y rentabilidad

**Prerrequisitos:** Fases 0 a 4 terminadas (MVP aprobado).

## 1. Objetivo

Registrar incidencias y gastos de mantenimiento de cada inmueble y calcular su **rentabilidad** (ingresos cobrados menos gastos). Es el equivalente a "Compras" del referente: aquí el gasto es el mantenimiento. Se integra con cobranza permitiendo **cargar al arrendatario** una reparación como cuota adicional.

## 2. Alcance

**Incluye**
- Entidad `Mantenimiento`, `MantenimientoService`, `RentabilidadService`.
- Pantallas **Mantenimiento** y **Rentabilidad**.
- **Ficha del inmueble** completa con pestañas.
- Integración: generar cuota tipo `OTRO` al arrendatario.
- Widgets nuevos opcionales en el Dashboard.

**No incluye**
- Gestión de proveedores/técnicos como catálogo (el técnico es texto libre).
- Adjuntar fotos o archivos.
- Exportaciones Excel/PDF de estos módulos (Fase 6).

## 3. Modelo de datos

### `mantenimiento`
`id`, `codigo` (único, `MNT-000001`, `SecuenciaService`), `inmueble_id` (obligatorio), `contrato_id` (nullable; se sugiere el contrato vigente del inmueble), `fecha_reporte`, `titulo`, `descripcion`, `categoria` (`PLOMERIA`, `ELECTRICIDAD`, `PINTURA`, `CARPINTERIA`, `CERRAJERIA`, `LIMPIEZA`, `OTROS`), `prioridad` (`BAJA`, `MEDIA`, `ALTA`, `URGENTE`), `estado` (`REPORTADO`, `EN_PROCESO`, `RESUELTO`, `CANCELADO`), `responsable_pago` (`PROPIETARIO`, `ARRENDATARIO`), `tecnico`, `telefono_tecnico`, `costo_estimado` BigDecimal(10,2), `costo_real` BigDecimal(10,2), `fecha_programada`, `fecha_resolucion`, `cuota_cargo_id` (nullable → `cuota`, si se generó cargo al arrendatario), `observaciones`, `fecha_registro`.

Índices: `mantenimiento(inmueble_id)`, `mantenimiento(estado)`, `mantenimiento(fecha_reporte)`.

## 4. Reglas de negocio

- `titulo`, `inmueble`, `categoria`, `prioridad` y `fecha_reporte` obligatorios; `fecha_reporte` ≤ hoy.
- **Transiciones permitidas:** `REPORTADO → EN_PROCESO | CANCELADO`; `EN_PROCESO → RESUELTO | CANCELADO`. `RESUELTO` y `CANCELADO` son finales (para corregir, el usuario crea un nuevo registro).
- `RESUELTO` exige `fecha_resolucion` (≥ `fecha_reporte`, ≤ hoy) y `costo_real` ≥ 0.
- Costos ≥ 0. Si `responsable_pago = ARRENDATARIO` y hay contrato asociado, se ofrece **"Generar cargo al arrendatario"**: crea una `Cuota` tipo `OTRO` en ese contrato con concepto "Reparación: <título>", monto = `costo_real`, vencimiento elegido por el usuario; guarda `cuota_cargo_id`. El cargo **no puede generarse dos veces**. Si el mantenimiento se cancela después de haber generado el cargo, la cuota pasa a `ANULADA` solo si no tiene pagos; si ya tiene pagos, se rechaza la cancelación con un mensaje claro.
- **Poner el inmueble en mantenimiento:** opción al pasar a `EN_PROCESO`, permitida solo si el inmueble está `DISPONIBLE`; al `RESUELTO`/`CANCELADO` el inmueble vuelve a `DISPONIBLE` si había sido puesto por este mantenimiento.
- Los gastos que cuentan para rentabilidad son los de estado `RESUELTO` con `responsable_pago = PROPIETARIO` (el costo de los asumidos por el arrendatario no es gasto del propietario).

### Rentabilidad por inmueble (`RentabilidadService.porInmueble(desde, hasta)`)
Para cada inmueble:
- **Ingresos** = capital cobrado de pagos `EMITIDO` del periodo + mora cobrada (mostrar ambos desglosados).
- **Gastos** = `costo_real` de mantenimientos `RESUELTO` del propietario con `fecha_resolucion` en el periodo.
- **Neto** = ingresos − gastos. **Margen %** = neto ÷ ingresos (0 si no hay ingresos).
- **Días ocupado** en el periodo (según vigencia de contratos) y **% de ocupación** del inmueble.
- Totales generales al pie.

## 5. Pantallas

### Mantenimiento (`mantenimiento-view.fxml` / `MantenimientoController`)
- Tira de resumen: abiertos, urgentes, en proceso, gasto del mes.
- Tabla: código, inmueble, título, categoría, prioridad (chip), estado (chip), responsable, costo, fecha. Filtros: estado, prioridad, inmueble, categoría, buscador.
- Drawer de alta/edición con los campos; acciones por fila: **Iniciar**, **Resolver** (pide fecha y costo real), **Cancelar**, **Generar cargo**.
- Prioridad `URGENTE` en rojo, `ALTA` en ámbar.

### Rentabilidad (`rentabilidad-view.fxml` / `RentabilidadController`)
- Encabezado con selector de periodo (mismo `Periodo` de la Fase 4) y botón Actualizar.
- Tarjetas KPI: ingresos, gastos, **neto**, margen promedio.
- Tabla por inmueble (ingresos, gastos, neto, margen, ocupación %) con orden por columna y barras de margen.
- Gráfico de barras: neto por inmueble (top 10).

### Ficha del inmueble (reemplaza el drawer de Fase 1/2)
Drawer/diálogo amplio con **pestañas**: *Datos*, *Contratos* (historial), *Mantenimiento* (incidencias del inmueble), *Ingresos y gastos* (mini resumen del año). Accesible desde Inmuebles y desde enlaces del Dashboard.

### Dashboard (opcional, ligero)
Agregar una tarjeta "Mantenimientos abiertos" (conteo + urgentes) sin alterar el layout principal.

Reemplazar los placeholders "Mantenimiento" y "Rentabilidad".

## 6. Estructura de archivos esperada

```text
model/        CategoriaMantenimiento, PrioridadMantenimiento, EstadoMantenimiento, ResponsablePago, Mantenimiento
repository/   MantenimientoRepository
service/      MantenimientoService, RentabilidadService
controller/   MantenimientoController, RentabilidadController, FichaInmuebleController
view/         mantenimiento-view.fxml, rentabilidad-view.fxml, ficha-inmueble-view.fxml
seeder/       MantenimientoSeeder
```

## 7. Pruebas obligatorias

- `MantenimientoServiceTest`: transiciones válidas e inválidas; resolver exige fecha y costo; generar cargo crea la cuota `OTRO` una sola vez; inmueble vuelve a `DISPONIBLE` al terminar si lo puso este mantenimiento; no pone en mantenimiento un inmueble alquilado.
- `RentabilidadServiceTest` (con `Clock` fijo): ingresos solo de pagos emitidos, gastos solo de resueltos del propietario, neto y margen, ocupación en días con contratos parciales en el periodo, margen 0 sin ingresos.

## 8. Criterios de aceptación

- [ ] Pruebas pasan y la app abre.
- [ ] Se registran, filtran y avanzan mantenimientos según el flujo de estados.
- [ ] "Generar cargo" crea la cuota en el contrato y aparece en Cuentas por cobrar.
- [ ] Rentabilidad muestra ingresos, gastos, neto y margen correctos con datos `dev`.
- [ ] La ficha del inmueble muestra datos, contratos, mantenimiento y resumen financiero.
- [ ] Prioridades y estados con chips semánticos correctos en ambos temas.
- [ ] Fases 0–4 siguen funcionando.

## 9. Prompt para Codex

```text
Lee AGENTS.md, docs/DISENO_UI.md, docs/00_PLAN_MAESTRO.md y docs/fases/FASE_05_MANTENIMIENTO_Y_RENTABILIDAD.md.
Las fases 0 a 4 ya están terminadas. Referencia (solo lectura): ../crud-persona-spring/ (patrón de Compras
y de las pantallas de listado con drawer).

Implementa EXCLUSIVAMENTE la Fase 5. Antes de escribir código, preséntame un plan corto: entidad,
servicios, pantallas, ficha con pestañas y cómo integrarás el cargo al arrendatario con Cuota tipo OTRO.
Luego impleméntalo.

Requisitos clave: flujo de estados estricto, gastos de rentabilidad solo de mantenimientos RESUELTOS a
cargo del propietario, cargo al arrendatario sin duplicar, inmueble en MANTENIMIENTO solo si estaba
disponible, Clock inyectado, consultas fuera del hilo de JavaFX.

Al terminar: ejecuta las pruebas, arranca la app, marca los criterios en docs/PROGRESO.md y resume
decisiones y pasos de prueba manual.
```

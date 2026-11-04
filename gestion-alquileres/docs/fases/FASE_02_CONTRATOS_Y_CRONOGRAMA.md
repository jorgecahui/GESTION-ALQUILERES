# FASE 2 — Contratos y cronograma de cuotas

**Prerrequisitos:** Fases 0 y 1 terminadas.

## 1. Objetivo

Permitir registrar contratos de alquiler y que cada contrato genere automáticamente su **cronograma de cuotas** (la deuda esperada mes a mes). Incluye renovar, finalizar y rescindir contratos, y liquidar la garantía. Es el equivalente a "Nueva venta / Ventas" del referente, pero con ciclo de vida.

## 2. Alcance

**Incluye**
- Entidades `Contrato` y `Cuota` (con todos los campos de mora ya definidos, aunque el cobro llega en la Fase 3).
- `ContratoService`, `CronogramaGenerador`, `ContratoRepository`, `CuotaRepository`.
- Pantallas **Nuevo contrato** y **Contratos**, y el historial de contratos en la ficha del inmueble.
- Estados visuales derivados (*Por vencer*, *Vencido*).
- Seeder de contratos de ejemplo (`dev`).

**No incluye**
- Registrar pagos, calcular mora cobrada ni recibos (Fase 3).
- PDF del contrato (Fase 6).
- Cuotas por servicios adicionales (se podrán agregar en Fases 3 y 5 como tipo `SERVICIO` u `OTRO`).

## 3. Modelo de datos

### `contrato`
`id`, `codigo` (único, `CTR-000001`, desde `SecuenciaService`), `inmueble_id` (obligatorio), `arrendatario_id` → `persona` (obligatorio), `garante_id` → `persona` (nullable), `fecha_inicio`, `fecha_fin`, `dia_pago` (1–28), `renta_mensual` BigDecimal(10,2), `incremento_anual_pct` BigDecimal(5,2) default 0, `garantia_monto` BigDecimal(10,2) default 0, `garantia_estado` (`SIN_GARANTIA`, `PENDIENTE`, `RECIBIDA`, `DEVUELTA`, `RETENIDA_PARCIAL`), `garantia_devuelta` y `garantia_descontada` BigDecimal, `garantia_motivo`, `dias_gracia` (int), `tipo_mora` (`PORCENTAJE_DIARIO`, `MONTO_DIARIO`), `valor_mora` BigDecimal(10,4), `estado` (`VIGENTE`, `FINALIZADO`, `RESCINDIDO`), `contrato_anterior_id` (nullable, auto-referencia), `fecha_termino_efectiva`, `motivo_termino`, `observaciones`, `fecha_registro`, `version`.

Índices: `contrato(estado)`, `contrato(inmueble_id)`, `contrato(arrendatario_id)`, `contrato(fecha_fin)`.

### `cuota`
`id`, `contrato_id` (obligatorio), `numero` (1…n), `tipo` (`RENTA`, `SERVICIO`, `OTRO`), `concepto` (texto, ej. "Renta noviembre 2026"), `periodo` (LocalDate: primer día del mes alquilado), `fecha_vencimiento`, `monto_cuota` BigDecimal(10,2), `monto_pagado` BigDecimal(10,2) default 0, `mora_acumulada` BigDecimal(10,2) default 0, `mora_cobrada` BigDecimal(10,2) default 0, `mora_condonada` BigDecimal(10,2) default 0, `mora_desde` (LocalDate, inicialmente = `fecha_vencimiento`), `estado` (`PENDIENTE`, `PARCIAL`, `PAGADA`, `ANULADA`), `version`.

Restricción única (`contrato_id`, `numero`, `tipo`) para cuotas de renta. Índices: `cuota(contrato_id)`, `cuota(fecha_vencimiento, estado)`.

> `mora_acumulada`, `mora_cobrada`, `mora_condonada` y `mora_desde` se definen ahora y se usan en la Fase 3.

## 4. Reglas de negocio

### Validaciones al crear
- Inmueble en estado `DISPONIBLE` y activo.
- **Sin traslape:** no puede existir otro contrato `VIGENTE` del mismo inmueble cuyo rango de fechas se cruce con el nuevo.
- Arrendatario obligatorio y activo; **garante distinto del arrendatario**.
- `fecha_fin` > `fecha_inicio`; **duración en meses completos** (la UI pide "Duración (meses)" y calcula `fecha_fin = fecha_inicio + N meses − 1 día`).
- `renta_mensual` > 0; `dia_pago` entre 1 y 28 (por defecto el día de `fecha_inicio`, topado a 28).
- `garantia_monto` ≥ 0 (si es 0 → `SIN_GARANTIA`; si > 0 → `PENDIENTE` o `RECIBIDA` según un check "Garantía recibida").
- `valor_mora` ≥ 0; `dias_gracia` ≥ 0. Los valores por defecto salen de `app.mora.*` y quedan **copiados al contrato** (snapshot).
- `fecha_inicio` no puede ser anterior a hoy menos 12 meses (evita errores de digitación; mensaje claro).

### Cronograma (`CronogramaGenerador`)
- N cuotas de tipo `RENTA`, una por mes, con `numero` 1…N.
- `periodo(n)` = primer día del mes (`YearMonth(fecha_inicio) + n − 1`).
- `fecha_vencimiento(n)` = día `dia_pago` del mes del periodo (si el mes es más corto, usar el último día); si en la cuota 1 esa fecha queda antes de `fecha_inicio`, usar `fecha_inicio`.
- **Incremento anual:** para la cuota `n`, `año = (n − 1) / 12`; `monto = renta × (1 + inc/100)^año`, redondeado HALF_UP a 2 decimales en cada paso anual (`renta_año1 = renta`, `renta_año2 = round(renta_año1 × (1+inc))`, …).
- `concepto` = "Renta <mes> <año>" (mes en español).
- Las cuotas se crean en la **misma transacción** que el contrato.

### Efectos sobre el inmueble
- Crear contrato → inmueble `ALQUILADO`.
- Finalizar / rescindir → inmueble `DISPONIBLE` (salvo que otra regla lo deje en `MANTENIMIENTO`).
- Renovar → el inmueble permanece `ALQUILADO`.

### Operaciones
- **Editar:** solo datos no económicos (garante, observaciones, parámetros de mora para cuotas futuras). Cambiar renta, fechas o día de pago **no se permite** si alguna cuota tiene `monto_pagado > 0`; si no hay pagos, se permite y se **regenera** el cronograma (borrando las cuotas sin pagos).
- **Finalizar** (solo `VIGENTE`): pide fecha de término efectiva (≤ `fecha_fin` o posterior si hubo prórroga) y liquidación de garantía (`garantia_devuelta` + `garantia_descontada` ≤ `garantia_monto`, con motivo si hay descuento). Las cuotas **no pagadas con vencimiento posterior** a la fecha de término pasan a `ANULADA`; las vencidas siguen siendo deuda. Estado `FINALIZADO`.
- **Rescindir:** igual que finalizar pero estado `RESCINDIDO` y **motivo obligatorio**.
- **Renovar** (solo `VIGENTE`): crea un contrato nuevo desde el anterior (mismo inmueble y arrendatario, `fecha_inicio = fecha_fin anterior + 1 día`, nuevos plazo y renta propuestos con el incremento), enlaza `contrato_anterior_id`, marca el anterior `FINALIZADO` con motivo "Renovado" y **traslada la garantía** (nuevo contrato con `garantia_estado = RECIBIDA` y la misma garantía; el anterior queda sin liquidar). Las cuotas pendientes del anterior se conservan.
- **Estados visuales (derivados, calculados con `Clock`):** *Por vencer* si `VIGENTE` y `fecha_fin − hoy ≤ app.alertas.dias-vencimiento`; *Vencido* si `VIGENTE` y `fecha_fin < hoy`.
- No hay eliminación de contratos.

## 5. Pantallas

### Nuevo contrato (`contrato-form-view.fxml` / `ContratoFormController`)
Formulario de página completa estilo punto de venta del referente:
- **Columna izquierda:** autocompletado de **Inmueble** (solo disponibles, muestra código, nombre y renta referencial), **Arrendatario** y **Garante** (autocompletado de Personas, con enlace rápido "Nueva persona" que abre el drawer de Personas), fecha de inicio, duración en meses, día de pago, renta mensual (propuesta desde la renta referencial), incremento anual %, garantía (monto + "recibida"), mora (tipo, valor), días de gracia, observaciones.
- **Columna derecha:** tarjeta **Resumen** con renta, garantía, nº de cuotas, total del contrato, fecha fin calculada; debajo, vista previa del **cronograma** (tabla con periodo, vencimiento, monto) que se recalcula al editar; botón primario "Registrar contrato".
- Al registrar: toast, limpiar formulario y ofrecer "Ver contrato".

### Contratos (`contratos-view.fxml` / `ContratosController`)
- Tira de resumen superior (como en Ventas del referente): contratos vigentes, por vencer, vencidos, renta mensual total vigente.
- Tabla: código, inmueble, arrendatario, vigencia, renta, estado (chip: Vigente/Por vencer/Vencido/Finalizado/Rescindido). Filtros: estado, por vencer, buscador, rango de fechas.
- Acciones por fila: **Detalle** (drawer con datos, garantía y tabla de cronograma), **Renovar**, **Finalizar**, **Rescindir**, **Editar**.
- Diálogos de Finalizar y Rescindir con los campos descritos y confirmación.
- En el drawer de detalle, la columna "Estado de cuota" muestra Pendiente/Pagada/Anulada (aún sin pagos).

### Ficha del inmueble (Fase 1)
Completar la sección "Contratos": historial del inmueble con código, arrendatario, vigencia y estado.

Reemplazar los placeholders "Nuevo contrato" y "Contratos" del menú.

## 6. Estructura de archivos esperada

```text
model/        EstadoContrato, GarantiaEstado, TipoMora, TipoCuota, EstadoCuota, Contrato, Cuota
repository/   ContratoRepository, CuotaRepository
service/      ContratoService, CronogramaGenerador, ContratoResumen (DTO), NombresMes (utilidad)
controller/   ContratoFormController, ContratosController
seeder/       ContratoSeeder
view/         contrato-form-view.fxml, contratos-view.fxml
```

Seeder `dev`: 5 contratos con fechas relativas a hoy (uno por vencer en 20 días, uno vencido hace 5 días, uno de hace 8 meses, uno finalizado, uno reciente) sobre inmuebles de la Fase 1.

## 7. Pruebas obligatorias

- `CronogramaGeneradorTest` (con `Clock` fijo): 12 cuotas; contrato iniciado el 31 con `dia_pago` 28; febrero; incremento anual del 5 % a 24 meses (renta 1,000.00 → cuotas 1–12 = 1,000.00, 13–24 = 1,050.00); cuota 1 con `fecha_inicio` posterior al día de pago; periodos y conceptos correctos.
- `ContratoServiceTest`:
  - crea contrato, genera N cuotas y deja el inmueble `ALQUILADO`;
  - rechaza traslape de fechas, inmueble no disponible, garante = arrendatario, duración inválida;
  - finalizar anula cuotas futuras no pagadas y libera el inmueble;
  - rescindir exige motivo;
  - renovar enlaza contrato anterior, traslada garantía y mantiene el inmueble `ALQUILADO`;
  - estados *Por vencer* y *Vencido* con `Clock` fijo;
  - editar renta rechazado si existe cuota con `monto_pagado > 0`.

## 8. Criterios de aceptación

- [ ] Pruebas pasan y la app abre.
- [ ] Se puede registrar un contrato y ver la vista previa del cronograma antes de guardar.
- [ ] Al guardar se crean las cuotas correctas y el inmueble pasa a `ALQUILADO` (visible en Inmuebles).
- [ ] No se puede crear un contrato sobre un inmueble ocupado ni con fechas traslapadas.
- [ ] Finalizar, rescindir y renovar funcionan con sus reglas y liquidación de garantía.
- [ ] La lista de contratos filtra por estado y muestra Por vencer / Vencido correctamente.
- [ ] La ficha del inmueble muestra su historial de contratos.
- [ ] Datos de ejemplo `dev` coherentes (cuotas con fechas pasadas y futuras).
- [ ] Modo claro/oscuro correctos; chips con colores semánticos.
- [ ] Las fases 0 y 1 siguen funcionando.

## 9. Prompt para Codex

```text
Lee AGENTS.md, docs/DISENO_UI.md, docs/00_PLAN_MAESTRO.md y docs/fases/FASE_02_CONTRATOS_Y_CRONOGRAMA.md.
Las fases 0 y 1 ya están terminadas. Referencia (solo lectura): ../crud-persona-spring/ (toma como patrón
el punto de venta y la pantalla de Ventas).

Implementa EXCLUSIVAMENTE la Fase 2. Antes de escribir código, preséntame un plan corto: entidades,
CronogramaGenerador, servicios, pantallas y qué reutilizarás del referente. Luego impleméntalo.

Requisitos clave: duración en meses completos, cronograma generado en la misma transacción del contrato,
incremento anual con redondeo HALF_UP, sin traslapes por inmueble, snapshot de parámetros de mora en el
contrato, renovar/finalizar/rescindir con las reglas del documento, Clock inyectado (no LocalDate.now()),
campos de mora de Cuota ya definidos pero sin lógica de cobro todavía, @Version en Contrato y Cuota.

Al terminar: ejecuta las pruebas, arranca la app, marca los criterios en docs/PROGRESO.md y resume
decisiones y pasos de prueba manual.
```

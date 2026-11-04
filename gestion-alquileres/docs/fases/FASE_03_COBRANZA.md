# FASE 3 — Cobranza: pagos, mora y recibos

**Prerrequisitos:** Fases 0, 1 y 2 terminadas.

## 1. Objetivo

Registrar pagos de los arrendatarios contra las cuotas del cronograma, **calcular la mora automáticamente**, emitir el **recibo en PDF** y ofrecer una vista de **cuentas por cobrar**. Es el equivalente a "Nueva venta / Ventas / comprobante PDF" del referente.

## 2. Alcance

**Incluye**
- Entidades `Pago` y `PagoDetalle`.
- `CalculadoraMora`, `PagoService`, `CuentasPorCobrarService`, `ReciboPdfService`, `NumeroALetras` (copiar del referente).
- Pantallas **Nuevo pago**, **Pagos** y **Cuentas por cobrar**.
- Anulación de pagos con reversión exacta.
- Condonación de mora.
- Estado visual de cuota *Vencida* y cálculo del **semáforo** (la lógica; su visualización en el dashboard es de la Fase 4).

**No incluye**
- Exportar a Excel (Fase 6) ni estado de cuenta en PDF (Fase 6).
- Recordatorios por WhatsApp (Fase 6).
- Saldo a favor / sobrepagos (no se permiten).
- Dashboard.

## 3. Modelo de datos

### `pago`
`id`, `serie` (ej. `R001`), `numero` (int), `recibo` (único, ej. `R001-00000001`), `contrato_id`, `fecha_pago` (LocalDate), `fecha_registro` (LocalDateTime), `medio_pago` (`EFECTIVO`, `TRANSFERENCIA`, `DEPOSITO`, `YAPE`, `PLIN`, `CHEQUE`, `OTRO`), `referencia` (nº de operación, opcional), `monto_total` BigDecimal(10,2), `monto_mora` BigDecimal(10,2), `monto_capital` BigDecimal(10,2), `estado` (`EMITIDO`, `ANULADO`), `observacion`, `motivo_anulacion`, `fecha_anulacion`, `version`. Snapshots: `arrendatario_nombre`, `arrendatario_documento`, `inmueble_codigo`, `inmueble_nombre`.

Índices: `pago(fecha_pago)`, `pago(contrato_id)`, único `uk_pago_recibo`.

### `pago_detalle`
`id`, `pago_id`, `cuota_id`, `item`, snapshots (`concepto`, `periodo`, `fecha_vencimiento`), `monto_capital` BigDecimal, `monto_mora` BigDecimal (mora cobrada), `monto_mora_condonada` BigDecimal, y para poder revertir: `mora_acumulada_antes`, `mora_desde_antes`, `estado_cuota_antes`.

## 4. Reglas de negocio

### 4.1 Cálculo de mora (`CalculadoraMora`)
Entradas: cuota, fecha de pago (o "hoy"), parámetros del contrato (`tipo_mora`, `valor_mora`, `dias_gracia`).

- `diasAtraso = fechaPago − fecha_vencimiento` (en días).
- **Gracia:** si `diasAtraso ≤ dias_gracia` y la cuota aún no tuvo ningún pago parcial → mora = 0. Superada la gracia, los días cuentan **desde el vencimiento** (la gracia solo exonera, no descuenta).
- Si la cuota no está vencida o su saldo de capital es 0 → solo corresponde `mora_acumulada` pendiente (si la hubiera).
- `PORCENTAJE_DIARIO`: `moraNueva = saldoCapital × (valor_mora/100) × díasDesde(mora_desde → fechaPago)`.
- `MONTO_DIARIO`: `moraNueva = valor_mora × díasDesde(mora_desde → fechaPago)`.
- `moraTotal = mora_acumulada + moraNueva`; `moraPendiente = moraTotal − mora_cobrada − mora_condonada` (nunca negativa).
- Redondeo HALF_UP a 2 decimales al final.
- Tras un **pago parcial de capital**, se guarda un punto de control: `mora_acumulada = moraTotal` (lo que se devengó hasta ese día), `mora_desde = fechaPago`. La gracia **no se vuelve a aplicar** después de un pago parcial.

**Casos de prueba de referencia** (cuota 1,000.00 vence 2026-01-05, gracia 3 días):
| Situación | Resultado |
|---|---|
| Pago el 2026-01-08 (3 días) | mora 0.00 |
| 0.10 % diario, pago el 2026-01-12 (7 días) | 1000 × 0.001 × 7 = **7.00** |
| Monto diario 5.00, pago el 2026-01-12 | 5.00 × 7 = **35.00** |
| Pago parcial de 400.00 el 2026-01-12 (mora 7.00 cobrada); luego pago el 2026-01-22 | saldo 600 × 0.001 × 10 = **6.00** |
| Pago antes del vencimiento | mora 0.00 |

### 4.2 Registrar un pago (`PagoService`)
- Un pago pertenece a **un contrato** (`VIGENTE`, `FINALIZADO` o `RESCINDIDO` — se puede cobrar deuda pendiente de contratos terminados).
- Debe tener al menos un detalle. Cada detalle: `monto_capital` > 0 y ≤ saldo de la cuota (`monto_cuota − monto_pagado`), `monto_mora` ≤ mora pendiente.
- **La mora se paga primero:** para aplicar capital a una cuota con mora pendiente, esa mora debe cubrirse completa en el mismo pago **o condonarse**. Mensaje: "La mora pendiente debe pagarse o condonarse antes de aplicar capital".
- **Condonar mora:** casilla por línea (con motivo obligatorio); suma a `mora_condonada`. Se registra en auditoría cuando exista (Fase 7).
- **Sin sobrepagos:** no se aceptan montos mayores al saldo.
- Se permite pagar **cuotas futuras** (adelantos) solo si se activa "Permitir adelantos"; por defecto se muestran sólo cuotas con vencimiento hasta el fin del mes en curso y las vencidas.
- Efectos en cada cuota: `monto_pagado += capital`, `mora_cobrada += mora`, `mora_condonada += condonada`; estado `PAGADA` si saldo = 0, `PARCIAL` si 0 < pagado < cuota; punto de control de mora si queda saldo.
- **Distribución automática:** dado un "monto recibido", `PagoService.proponerImputacion(contrato, monto, fechaPago)` reparte desde la cuota **más antigua**, mora antes que capital. El usuario puede ajustar manualmente cada línea.
- Numeración con `SecuenciaService` (clave `RECIBO-<serie>`, por ejemplo `RECIBO-R001`), formato `R001-00000001`, todo dentro de la transacción.
- Fecha de pago por defecto = hoy (`Clock`); no puede ser futura ni anterior a la fecha de inicio del contrato.

### 4.3 Anular un pago
- Solo `EMITIDO`. Motivo obligatorio.
- **Solo si es el último pago activo de cada cuota involucrada**; si existe un pago posterior (no anulado) sobre alguna de esas cuotas, rechazar con "Anule primero el pago posterior {recibo}".
- Revierte exactamente: resta `monto_pagado`, `mora_cobrada`, `mora_condonada`; restaura `mora_acumulada`, `mora_desde` y `estado` usando los campos `*_antes` del detalle.
- El pago queda `ANULADO` y conserva su número (no se reutiliza).

### 4.4 Estados derivados y semáforo
- Cuota **Vencida**: `PENDIENTE`/`PARCIAL` con `fecha_vencimiento + dias_gracia < hoy` (calculado con `Clock`).
- **Semáforo por contrato** (`MorosidadService.semaforo(contrato)`), con umbrales de `app.morosidad.*`:
  - 🟢 **Al día:** sin cuotas vencidas.
  - 🟡 **Atraso leve:** hay vencidas, con atraso máximo ≤ `umbral-leve-dias` (15) **y** menos de `cuotas-rojo` (2) cuotas vencidas.
  - 🔴 **Moroso:** atraso máximo > `umbral-leve-dias` **o** vencidas ≥ `cuotas-rojo`.

### 4.5 Recibo PDF (`ReciboPdfService`, OpenPDF)
Contenido: datos del arrendador (`DatosArrendador`, desde `app.arrendador.*`), "RECIBO DE ALQUILER" con número `R001-00000001`, fecha, arrendatario y documento, inmueble y dirección, tabla de conceptos (concepto/periodo, vencimiento, renta, mora), total, **monto en letras** (`NumeroALetras`, ej. "SON: MIL DOSCIENTOS CINCUENTA Y 00/100 SOLES"), medio de pago y referencia, **saldo pendiente del contrato** tras este pago, líneas de firma y la leyenda "Documento interno sin validez tributaria". Si el pago está `ANULADO`, marca de agua "ANULADO". Se genera por código (sin plantilla externa), igual que el referente.

## 5. Pantallas

### Nuevo pago (`pago-form-view.fxml` / `PagoFormController`)
- **Izquierda:** autocompletado de **contrato** (busca por arrendatario, inmueble o código) → muestra tarjeta de resumen (arrendatario, inmueble, renta, semáforo, deuda vencida). Tabla de **cuotas pendientes** con: casilla, concepto, vencimiento, días de atraso, saldo, mora calculada, monto a pagar (editable), casilla "Condonar mora". Botones "Pagar todo lo vencido" y "Aplicar monto recibido…" (campo de monto que usa la distribución automática).
- **Derecha:** tarjeta **Resumen** (subtotal capital, mora, condonado, **total a pagar**), fecha de pago, medio de pago, referencia, observación, botón primario "Registrar pago".
- Recalcula mora al cambiar la fecha de pago.
- Al guardar: diálogo "Pago registrado" con botones **Imprimir recibo (PDF)** y **Nuevo pago**.
- Se puede llegar desde Cuentas por cobrar con el contrato preseleccionado.

### Pagos (`pagos-view.fxml` / `PagosController`)
- Tira de resumen: total cobrado (filtro actual), nº de recibos, mora cobrada, anulados.
- Tabla: recibo, fecha, arrendatario, inmueble, medio, total, estado (chip). Filtros: periodo/fechas, medio, estado, buscador.
- Acciones: **Detalle** (drawer), **Recibo PDF**, **Anular** (diálogo con motivo).

### Cuentas por cobrar (`cuentas-por-cobrar-view.fxml` / `CuentasPorCobrarController`)
- Tira superior: total por cobrar, **vencido**, por vencer en 7 días, nº de contratos morosos.
- Tabla de cuotas pendientes: arrendatario, inmueble, concepto, vencimiento, días de atraso, saldo, mora estimada a hoy, total, semáforo. Filtros: Todas / Vencidas / Vencen en 7 días / Morosos (rojo), buscador.
- Acción por fila: **Cobrar** (abre Nuevo pago con el contrato cargado).

### Cambios en Contratos (Fase 2)
- Añadir columna **Estado de pago** (semáforo) en la lista de contratos.
- En el drawer de detalle: cuotas con estado real (Pendiente/Parcial/Pagada/Vencida) y pagos aplicados.

Reemplazar los placeholders "Nuevo pago", "Pagos" y "Cuentas por cobrar".

## 6. Estructura de archivos esperada

```text
model/        MedioPago, EstadoPago, Pago, PagoDetalle, SemaforoMorosidad
repository/   PagoRepository, PagoDetalleRepository (+ consultas en CuotaRepository)
service/      CalculadoraMora, PagoService, MorosidadService, CuentasPorCobrarService,
              ReciboPdfService, NumeroALetras, DatosArrendador (en config/)
controller/   PagoFormController, PagosController, CuentasPorCobrarController
view/         pago-form-view.fxml, pagos-view.fxml, cuentas-por-cobrar-view.fxml
```

Actualizar `ContratoSeeder` (dev) para que algunos contratos tengan pagos al día, otros con cuotas vencidas (uno amarillo, uno rojo) y un pago parcial.

## 7. Pruebas obligatorias

- `CalculadoraMoraTest`: todos los casos de la tabla de 4.1, más pago parcial con punto de control, mora cero con saldo cero, y tope de redondeo.
- `PagoServiceTest`: pago completo; pago parcial; mora primero; rechazo de capital sin cubrir mora; condonación con motivo; sobrepago rechazado; distribución automática (más antigua primero); numeración consecutiva `R001-00000001`, `…02`; anulación revierte exacto; anulación rechazada si hay pago posterior; pago sobre contrato finalizado con deuda.
- `MorosidadServiceTest`: verde, amarillo, rojo (por días y por cantidad de cuotas) con `Clock` fijo.
- `NumeroALetrasTest` (adaptado del referente): 0, 1, 15, 100, 1,250.50, 21,000.
- `ReciboPdfServiceTest`: genera bytes no vacíos que empiezan con `%PDF` para un pago emitido y uno anulado.

## 8. Criterios de aceptación

- [ ] Pruebas pasan y la app abre.
- [ ] Registrar un pago completo de una cuota actualiza su estado a Pagada y se ve en Pagos.
- [ ] La mora se calcula según los casos de referencia y cambia al modificar la fecha de pago.
- [ ] Pago parcial deja la cuota `PARCIAL` y la mora posterior se calcula sobre el saldo.
- [ ] No se puede sobrepagar ni aplicar capital sin cubrir o condonar la mora.
- [ ] El recibo PDF se genera, se abre/guarda con `Exportador` y se ve profesional (con monto en letras y saldo pendiente).
- [ ] Anular un pago revierte exactamente las cuotas y el recibo sale con marca "ANULADO".
- [ ] Cuentas por cobrar lista la deuda con semáforo y "Cobrar" precarga el contrato.
- [ ] La lista de contratos muestra el semáforo de pago.
- [ ] Numeración de recibos sin saltos ni repeticiones tras reiniciar la app.
- [ ] Modo claro/oscuro correctos. Fases 0–2 siguen funcionando.

## 9. Prompt para Codex

```text
Lee AGENTS.md, docs/DISENO_UI.md, docs/00_PLAN_MAESTRO.md y docs/fases/FASE_03_COBRANZA.md.
Las fases 0, 1 y 2 ya están terminadas. Referencia (solo lectura): ../crud-persona-spring/ (toma como
patrón ComprobantePdfService, NumeroALetras, la pantalla Ventas y el punto de venta).

Implementa EXCLUSIVAMENTE la Fase 3. Antes de escribir código, preséntame un plan corto: entidades,
CalculadoraMora, PagoService, pantallas y qué reutilizarás del referente. Luego impleméntalo.

Requisitos clave: mora con gracia, puntos de control en pagos parciales y mora cobrada antes que capital;
condonación con motivo; sin sobrepagos; anulación solo del último pago activo por cuota con reversión
exacta usando los campos *_antes; numeración con SecuenciaService (no MAX+1); recibo PDF con monto en
letras y saldo pendiente; Clock inyectado; pruebas con los casos numéricos del documento.

Al terminar: ejecuta las pruebas, arranca la app, marca los criterios en docs/PROGRESO.md y resume
decisiones y pasos de prueba manual.
```

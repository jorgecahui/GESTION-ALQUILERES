# FASE 6 — Reportes, documentos y recordatorios

**Prerrequisitos:** Fases 0 a 5 terminadas.

## 1. Objetivo

Completar la capa de salida del sistema: **PDFs** (estado de cuenta y hoja de contrato), **Excel** completos para cada módulo, y **recordatorios de cobro por WhatsApp** con historial. Es el equivalente a los reportes de ventas/inventario del referente.

## 2. Alcance

**Incluye**
- `EstadoCuentaPdfService` y `ContratoPdfService` (OpenPDF).
- Reportes Excel de Pagos, Cuentas por cobrar, Contratos, Inmuebles/Ocupación y Mantenimiento/Rentabilidad (POI, reutilizando `LibroExcel`).
- Botones "Exportar Excel" en las listas correspondientes.
- `RecordatorioService`, entidad `Recordatorio`, plantillas de mensaje y acciones **Copiar** / **Abrir WhatsApp**.
- Botón de WhatsApp en Personas.

**No incluye**
- Envío automático de mensajes (no hay API de WhatsApp; se abre el enlace `wa.me` y la persona envía).
- Editor de plantillas en pantalla (llega en la Fase 7, Configuración). Aquí las plantillas son constantes con valores por defecto sobreescribibles por `application.properties`.
- Contrato legal firmable (el PDF es una **hoja resumen**).

## 3. Documentos PDF

### Estado de cuenta (`EstadoCuentaPdfService`)
Por **contrato**: encabezado con datos del arrendador, arrendatario, inmueble, vigencia y renta; tabla de **cuotas** (concepto, vencimiento, monto, pagado, mora, saldo, estado); tabla de **pagos** (recibo, fecha, medio, monto); **resumen** (total facturado, total pagado, mora cobrada, saldo pendiente, mora estimada a hoy); fecha de emisión. Acceso: botón "Estado de cuenta" en el detalle de Contratos, en Cuentas por cobrar y en la tarjeta de resumen de Nuevo pago.

### Hoja de contrato (`ContratoPdfService`)
Resumen del contrato: partes, inmueble (dirección, área, características), vigencia, renta e incremento anual, día de pago, garantía, política de mora y días de gracia, y **cronograma**. Pie: "Hoja resumen. No reemplaza el contrato firmado por las partes." Acceso: botón "Hoja de contrato" en el detalle de Contratos.

Ambos PDF se generan por código, con el mismo estilo del recibo de la Fase 3 (tipografías, encabezado y colores), y se abren/guardan con `Exportador`.

## 4. Reportes Excel

Reutilizar `LibroExcel` (estilos, formato de moneda `S/ #,##0.00`, fechas `dd/MM/yyyy`, anchos automáticos, fila de encabezado congelada, autofiltro).

| Reporte | Origen | Hojas |
|---|---|---|
| Pagos | Pantalla Pagos (respeta filtros) | Resumen, Pagos, Detalle de conceptos |
| Cuentas por cobrar | Pantalla Cuentas por cobrar | Resumen, Cuotas pendientes, Morosos |
| Contratos | Pantalla Contratos | Resumen, Contratos, Por vencer |
| Inmuebles y ocupación | Pantalla Inmuebles | Resumen, Inmuebles, Ocupación por tipo |
| Mantenimiento y rentabilidad | Pantallas Mantenimiento y Rentabilidad | Resumen, Mantenimientos, Rentabilidad por inmueble |

Cada exportación incluye en "Resumen" el periodo/filtros aplicados, la fecha de generación y los totales. Los archivos se nombran `reporte-<módulo>-AAAA-MM-DD.xlsx`.

## 5. Recordatorios por WhatsApp

### Entidad `recordatorio`
`id`, `contrato_id`, `cuota_id` (nullable), `tipo` (`PREVENTIVO`, `VENCIDO`, `FIN_CONTRATO`), `canal` (`WHATSAPP`, `COPIADO`), `telefono`, `mensaje`, `fecha_envio` (LocalDateTime), `usuario` (nullable hasta la Fase 7).

### Plantillas (con marcadores)
Marcadores: `{nombre}`, `{inmueble}`, `{concepto}`, `{monto}`, `{vencimiento}`, `{dias}`, `{arrendador}`, `{contrato}`, `{fecha_fin}`.

- **Preventivo** (vence en ≤ 3 días): saludo cordial + recordatorio del monto y fecha.
- **Vencido**: aviso amable con días de atraso, monto y mora estimada a hoy.
- **Fin de contrato**: aviso de vencimiento próximo para coordinar renovación.

Tono cordial y profesional, en español peruano neutro.

### Reglas
- Teléfono normalizado a formato internacional de Perú: 9 dígitos que empiezan con 9 → `51` + número; ya con `+51`/`51` se respeta; si no es válido, la acción se deshabilita con tooltip "Teléfono no válido o ausente".
- **Abrir WhatsApp:** `https://wa.me/<telefono>?text=<mensaje URL-encoded UTF-8>` mediante `HostServices.showDocument` (o el mecanismo equivalente del referente para abrir enlaces).
- **Copiar mensaje:** al portapapeles, con toast de confirmación.
- Ambas acciones muestran **vista previa editable** del mensaje antes de continuar y registran un `Recordatorio` al confirmar.
- `RecordatorioService.sugerencias(hoy)`: lista de cuotas que necesitan recordatorio (vencidas o por vencer en ≤ 3 días) **sin recordatorio en los últimos 3 días**.

### Dónde aparece
- **Cuentas por cobrar:** columna "Último recordatorio" ("hace 2 días" / "—") y acción **Recordar** por fila. Botón superior "Recordar a todos los vencidos" que abre un asistente para revisar y enviar uno por uno.
- **Contratos por vencer** (lista de Contratos y tarjeta del Dashboard): acción **Avisar fin de contrato**.
- **Personas:** botón de WhatsApp para contacto libre (sin plantilla).
- **Detalle de contrato:** pestaña/tabla "Recordatorios enviados".

## 6. Estructura de archivos esperada

```text
model/        TipoRecordatorio, CanalRecordatorio, Recordatorio
repository/   RecordatorioRepository
service/      EstadoCuentaPdfService, ContratoPdfService, RecordatorioService,
              PlantillasMensaje, TelefonoUtil,
              ReportePagosExcelService, ReporteCuentasPorCobrarExcelService, ReporteContratosExcelService,
              ReporteInmueblesExcelService, ReporteMantenimientoExcelService
controller/   (ajustes en Pagos, CuentasPorCobrar, Contratos, Inmuebles, Persona, Mantenimiento, Rentabilidad)
ui/           DialogoRecordatorio (vista previa + copiar/abrir)
```

## 7. Pruebas obligatorias

- `EstadoCuentaPdfServiceTest` y `ContratoPdfServiceTest`: bytes no vacíos que comienzan con `%PDF`, para contrato con y sin pagos.
- Pruebas de cada servicio Excel: el libro se reabre con POI, contiene las hojas esperadas y los totales de Resumen coinciden con los datos.
- `PlantillasMensajeTest`: reemplazo de marcadores, importes con formato `S/ 1,250.00`, fechas legibles, sin marcadores sin resolver.
- `TelefonoUtilTest`: `987654321`, `+51987654321`, `51987654321`, `0987654321` (inválido), vacío, con espacios/guiones.
- `RecordatorioServiceTest`: sugerencias excluyen cuotas con recordatorio reciente; URL de WhatsApp correctamente codificada (tildes, saltos de línea, `S/`).

## 8. Criterios de aceptación

- [ ] Pruebas pasan y la app abre.
- [ ] El estado de cuenta PDF cuadra con las cuotas y pagos del contrato.
- [ ] La hoja de contrato PDF se ve profesional y completa.
- [ ] Cada lista tiene su "Exportar Excel" y respeta los filtros activos.
- [ ] Los Excel abren sin errores en Excel/LibreOffice, con formato de moneda, filtros y encabezado fijo.
- [ ] "Recordar" abre WhatsApp con el mensaje correcto o lo copia al portapapeles, y queda registrado.
- [ ] Cuentas por cobrar muestra "Último recordatorio" y no sugiere duplicados recientes.
- [ ] Teléfonos inválidos deshabilitan las acciones con explicación.
- [ ] Fases 0–5 siguen funcionando; modo claro/oscuro correctos.

## 9. Prompt para Codex

```text
Lee AGENTS.md, docs/DISENO_UI.md, docs/00_PLAN_MAESTRO.md y docs/fases/FASE_06_REPORTES_Y_RECORDATORIOS.md.
Las fases 0 a 5 ya están terminadas. Referencia (solo lectura): ../crud-persona-spring/ (patrón de
ComprobantePdfService, ReporteExcelService, ReporteInventarioExcelService, LibroExcel y Exportador).

Implementa EXCLUSIVAMENTE la Fase 6. Antes de escribir código, preséntame un plan corto: PDFs, servicios
Excel, módulo de recordatorios y cambios en las pantallas existentes. Luego impleméntalo.

Requisitos clave: reutilizar LibroExcel y el estilo del recibo PDF, exportaciones que respeten filtros,
plantillas de mensaje con marcadores, normalización de teléfono peruano, enlace wa.me bien codificado,
vista previa editable antes de enviar/copiar, registro de Recordatorio, Clock inyectado.

Al terminar: ejecuta las pruebas, arranca la app, marca los criterios en docs/PROGRESO.md y resume
decisiones y pasos de prueba manual.
```

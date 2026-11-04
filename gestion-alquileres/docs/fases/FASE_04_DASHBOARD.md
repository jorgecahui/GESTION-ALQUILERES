# FASE 4 — Dashboard

**Prerrequisitos:** Fases 0 a 3 terminadas.

## 1. Objetivo

Construir el **Dashboard** con el mismo aspecto y comportamiento que el del referente: filtro de periodo, botones *Exportar Excel* y *Actualizar*, tarjetas KPI con variación frente al periodo anterior, gráficos y tablas. Incluye el **semáforo de morosidad** y la alerta de contratos por vencer. Con esta fase se completa la **versión presentable mínima (MVP)**.

## 2. Alcance

**Incluye**
- `DashboardService` y `ReporteCobranzaService` (resúmenes con periodo).
- Enum `Periodo` y selector de periodo.
- `DashboardController` + `dashboard-view.fxml`.
- Exportación básica a Excel del dashboard (`ReporteCobranzaExcelService` + `LibroExcel` copiado/adaptado del referente).
- Componente `ui/Semaforo` y uso del semáforo en listas.
- Seeder `dev` ampliado para que el dashboard tenga datos significativos (varios meses de pagos).

**No incluye**
- Reportes Excel completos y PDFs (Fase 6), mantenimiento/rentabilidad (Fase 5).

## 3. Periodos

`Periodo`: `ULTIMOS_7_DIAS`, `ULTIMOS_30_DIAS` (por defecto), `ESTE_MES`, `MES_ANTERIOR`, `ULTIMOS_90_DIAS`, `ESTE_ANIO`, `PERSONALIZADO` (muestra dos `DatePicker`). Cada uno resuelve a `[desde, hasta]` usando el `Clock`. El **periodo anterior** tiene la misma duración y termina el día antes de `desde`.

## 4. Indicadores (tarjetas KPI, fila superior)

Mismo diseño que el referente (título, icono en cuadro azul claro, valor grande, línea inferior):

| KPI | Valor | Línea inferior | Cálculo |
|---|---|---|---|
| **Total cobrado** (icono `TRENDING_UP`) | `S/ 0.00` | chip `▲/▼ x.x %` + "vs. periodo anterior" | Suma de `pago.monto_total` con estado `EMITIDO` y `fecha_pago` en el periodo. Variación = `(actual − anterior) / anterior`; si el anterior es 0 y el actual > 0 muestra "Nuevo"; si ambos 0, `▼ 0.0 %` neutro. Rojo si baja, verde si sube. |
| **Cartera vencida** (icono `ALERT_CIRCLE`) | `S/ 0.00` | "N cuotas · M arrendatarios · Morosidad X %" | Instantánea **a hoy** (no depende del periodo): suma de saldo de capital + mora pendiente de cuotas vencidas. Morosidad % = contratos `VIGENTE` con al menos una cuota vencida ÷ contratos `VIGENTE`. |
| **Ocupación** (icono `HOME`) | `0 %` | "X de Y alquilados · Z disponibles" | Inmuebles `ALQUILADO` ÷ inmuebles no `INACTIVO`. |
| **Contratos por vencer** (icono `CALENDAR`) | `0` | "Próximo: CTR-… en N días" o "Ninguno en los próximos 60 días" | Contratos `VIGENTE` con `fecha_fin` entre hoy y hoy + `app.alertas.dias-vencimiento`. |

## 5. Gráficos y tablas

Layout y proporciones iguales al referente (ver `DISENO_UI.md` §5).

1. **Evolución de cobros** (tarjeta ancha, línea): total cobrado por día (o por mes si el periodo > 90 días). **Rellenar con 0** los días sin pagos para que la línea sea continua. Eje Y `S/ …`, cuadrícula punteada, eje X con fechas cortas (`05 oct.`). Subtítulo: "Total cobrado por día (S/)".
2. **Inmuebles con mayor ingreso** (top 5 por importe cobrado en el periodo): lista con posición, nombre, importe y barra proporcional. Vacío: "Sin cobros en el periodo."
3. **Últimos pagos** (tabla de 8 filas): recibo, fecha, arrendatario, inmueble, total y botón **PDF** (usa `ReciboPdfService`); enlace "Ver todos" navega a Pagos. Subtítulo: "Imprime el recibo en PDF con un clic".
4. **Arrendatarios en mora** (top 5 por deuda vencida): avatar con iniciales, nombre, inmueble, deuda, días de atraso y **semáforo**. Vacío: "Nadie en mora. ¡Buen trabajo!"
5. **Ocupación por tipo** (dona): inmuebles por tipo con leyenda y porcentaje; colores de la paleta de series.
6. **Contratos por vencer** (lista): código, inmueble, arrendatario, fecha de fin y "en N días"; botón **Renovar** que abre el flujo de renovación de la Fase 2.

Opcional si hay espacio: barras "Esperado vs. cobrado" por mes (esperado = suma de `monto_cuota` con vencimiento en el mes; cobrado = capital cobrado).

## 6. Comportamiento

- Encabezado: título "Dashboard", subtítulo "Resumen general · actualizado a las HH:mm", y a la derecha el combo de periodo, **Exportar Excel** y **Actualizar**.
- Cambiar el periodo o pulsar Actualizar recarga **todo** con `Tareas`, mostrando un indicador de carga discreto y sin congelar la ventana. Al cargar, la hora del subtítulo se actualiza.
- `DashboardController` implementa `Refrescable` y se recarga al volver a la pantalla.
- Valores monetarios con formato `S/ 1,234.50`.
- Modo claro/oscuro: los gráficos usan la paleta de series del referente en ambos temas.
- Exportar Excel genera `.xlsx` con hojas: **Resumen** (KPI del periodo), **Cobros** (pagos del periodo), **Cartera vencida** (cuotas vencidas con mora estimada) y **Contratos por vencer**. Se abre el diálogo de guardado con `Exportador`.

## 7. Semáforo en el resto del sistema

- `ui/Semaforo` renderiza la pastilla (verde/amarilla/roja) reutilizando `MorosidadService` de la Fase 3.
- Usarlo en: Dashboard (arrendatarios en mora), Contratos, Cuentas por cobrar y en la tarjeta de resumen de Nuevo pago.

## 8. Estructura de archivos esperada

```text
model/        Periodo
service/      DashboardService, ReporteCobranzaService (con records/DTO de resumen),
              ReporteCobranzaExcelService, LibroExcel (adaptado)
controller/   DashboardController
ui/           Semaforo, componentes de tarjeta KPI y gráficos (métodos de apoyo)
view/         dashboard-view.fxml
seeder/       ampliar ContratoSeeder / PagoSeeder para 6 meses de historia
```

## 9. Pruebas obligatorias

Con `Clock` fijo y datos construidos en la prueba:
- `ReporteCobranzaServiceTest`: total cobrado excluye anulados; variación vs. periodo anterior (sube, baja, anterior 0); serie diaria rellena con ceros; top inmuebles; periodo mes anterior/este mes.
- `DashboardServiceTest`: cartera vencida (capital + mora), morosidad %, ocupación, contratos por vencer (límite exacto de 60 días).
- `ReporteCobranzaExcelServiceTest`: el libro generado contiene las hojas esperadas y se puede reabrir con POI.

## 10. Criterios de aceptación

- [ ] Pruebas pasan y la app abre.
- [ ] El Dashboard luce como el del referente (misma estructura de tarjetas, tipografías, espaciados, iconos).
- [ ] Cambiar de periodo recalcula KPI, gráficos y tablas; "Actualizar" funciona y actualiza la hora.
- [ ] Con datos `dev` los gráficos muestran información coherente; sin datos aparecen mensajes de vacío correctos.
- [ ] La variación "vs. periodo anterior" se comporta como se describe (verde/rojo/"Nuevo").
- [ ] El semáforo aparece en Dashboard, Contratos y Cuentas por cobrar con la misma lógica.
- [ ] "Renovar" desde Contratos por vencer abre el flujo correcto.
- [ ] El botón PDF de Últimos pagos abre el recibo.
- [ ] Exportar Excel genera el archivo con las 4 hojas.
- [ ] Carga sin congelar la ventana; modo claro/oscuro correctos. Fases 0–3 siguen funcionando.

> **Hito:** al aprobar esta fase se obtiene el MVP presentable (catálogos, contratos, cobranza, recibo PDF, dashboard).

## 11. Prompt para Codex

```text
Lee AGENTS.md, docs/DISENO_UI.md, docs/00_PLAN_MAESTRO.md y docs/fases/FASE_04_DASHBOARD.md.
Las fases 0 a 3 ya están terminadas. Referencia (solo lectura): ../crud-persona-spring/ (toma como patrón
DashboardController, dashboard-view.fxml, ReporteVentasService, ReporteExcelService y LibroExcel).

Implementa EXCLUSIVAMENTE la Fase 4. Antes de escribir código, preséntame un plan corto: servicios,
DTOs de resumen, componentes de gráfico y qué reutilizarás del referente. Luego impleméntalo.

Requisitos clave: mismo layout y estilo que el dashboard del referente, KPI con variación vs. periodo
anterior de igual duración, series rellenas con ceros, semáforo reutilizable, carga asíncrona con
ui/Tareas, Clock inyectado, exportación Excel con 4 hojas, seeder dev con 6 meses de historia.

Al terminar: ejecuta las pruebas, arranca la app, marca los criterios en docs/PROGRESO.md y resume
decisiones y pasos de prueba manual.
```

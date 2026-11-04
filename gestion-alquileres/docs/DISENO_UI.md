# Guía de diseño UI

Objetivo: que la aplicación se vea **casi idéntica** al sistema de referencia. La fuente de verdad visual es el proyecto de referencia (`../crud-persona-spring/src/main/resources/css/app.css` y `view/*.fxml`). Este documento describe qué replicar y cómo adaptarlo al dominio de alquileres.

> Regla de oro: **copia primero, adapta después.** No inventes colores, radios ni espaciados; toma los valores de `app.css` del referente. Los valores de abajo son los confirmados en el análisis; si difieren del CSS real, manda el CSS.

## 1. Base visual

- Tema: **AtlantaFX Primer** (`PrimerLight` / `PrimerDark`). Los colores de fondo, texto, bordes y acentos salen de los tokens (`-color-bg-default`, `-color-fg-default`, `-color-accent-emphasis`, `-color-success-*`, `-color-danger-*`, `-color-warn-*`). **No hardcodees hex** salvo en la paleta de gráficos.
- Tipografía: la predeterminada del sistema, tamaño base **13 px**. Títulos de página **26 px**, valor de KPI **28 px**, texto de marca **16 px**.
- Espaciados: página ≈ 30/34/26 px, tarjetas ≈ 20/22/18 px (copiar los valores exactos del CSS).
- Radios: tarjetas **16 px**, botones e inputs **8–10 px**, diálogos **18 px**.
- Sombras gaussianas suaves en tarjetas, drawer y diálogos.
- Iconos: **Ikonli Feather** (`Feather.*`). Verifica que cada constante exista en el enum.

### Paleta de series para gráficos (del referente)

| Serie | Modo claro | Modo oscuro |
|---|---|---|
| 1 azul | `#2a78d6` | `#3987e5` |
| 2 naranja | `#eb6834` | `#d95926` |
| 3 verde | `#1baf7a` | `#199e70` |
| 4 ámbar | `#eda100` | `#c98500` |
| 5 rosa | `#e87ba4` | `#d55181` |
| 6 verde oscuro | `#008300` | `#008300` |
| Otros | `#9a9994` | `#6e6d68` |

Las series se redefinen bajo `.app.dark` como en el referente.

## 2. Shell de la aplicación

Ventana principal (`main-view.fxml` + `MainController`), copiada del referente:

- **Título de ventana:** `Panel administrativo | Contratos, Cobranza e Inmuebles`.
- **Menú lateral izquierdo** (`.sidebar`):
  - Cabecera: cuadro de icono azul con el logo (icono Feather `home`) + texto **"Alquileres"** (16 px, negrita) y debajo **"Panel administrativo"** (gris, pequeño).
  - Grupos con etiqueta en mayúsculas pequeñas grises: `GENERAL`, `CONTRATOS`, `COBRANZA`, `MANTENIMIENTO`, `CATÁLOGO`.
  - Ítems (`.nav-item`) con icono + texto. El ítem activo tiene fondo azul claro y texto azul en negrita.
  - Pie separado por una línea: **Configuración**, **Modo oscuro**, **Contraer menú**.
- **Área de contenido:** a la derecha, con scroll vertical, fondo `-color-bg-subtle`/similar; cada pantalla se carga desde `/view/<nombre>-view.fxml`.
- **Contraer menú:** reduce a solo iconos con transición suave; recuerda el estado durante la sesión.
- **Modo oscuro:** alterna PrimerLight/PrimerDark y agrega/quita la clase `.dark` en la raíz. **Persistir la preferencia** con `java.util.prefs` (mejora respecto al referente).

### Iconos sugeridos por ítem

| Ítem | Icono Feather |
|---|---|
| Dashboard | `GRID` |
| Nuevo contrato | `FILE_PLUS` |
| Contratos | `FILE_TEXT` |
| Nuevo pago | `DOLLAR_SIGN` |
| Pagos | `CREDIT_CARD` |
| Cuentas por cobrar | `ALERT_CIRCLE` |
| Mantenimiento | `TOOL` |
| Rentabilidad | `BAR_CHART_2` |
| Inmuebles | `HOME` |
| Tipos de inmueble | `TAG` |
| Personas | `USERS` |
| Propietarios | `KEY` |
| Configuración | `SETTINGS` |
| Modo oscuro / claro | `MOON` / `SUN` |
| Contraer menú | `CHEVRONS_LEFT` |

## 3. Anatomía de una pantalla

Todas las pantallas siguen la estructura del referente:

1. **Encabezado de página:** título grande (26 px) + subtítulo gris ("Resumen general · actualizado a las 21:20") a la izquierda; acciones a la derecha (combo de periodo, `Exportar Excel`, `Actualizar`, botón primario).
2. **Etiqueta de sección** en mayúsculas pequeñas grises (`CONTRATOS`, `COBRANZA`…) sobre las tarjetas KPI.
3. **Contenido en tarjetas** (`.tarjeta`): blancas en modo claro, borde sutil, radio 16, sombra suave.
4. **Estados vacíos** amigables dentro de la tarjeta ("Sin cobros en el periodo.").

### Patrones por tipo de pantalla

- **Listado (catálogos, contratos, pagos):** barra superior con buscador + filtros (combos) + botón primario "Nuevo …"; tabla (`.tabla`) con paginación (`Paginador`); acciones por fila (editar, ver, anular) con icono; formulario de alta/edición en **drawer lateral** (`.drawer`) con validación en línea (`Formularios`).
- **Formulario de página completa (Nuevo contrato, Nuevo pago):** equivalente al punto de venta del referente: columna izquierda con campos y autocompletado (`Autocompletar`), columna derecha con tarjeta **Resumen** (totales) y botón de acción principal.
- **Detalle:** drawer o diálogo con datos, tabla secundaria y acciones (PDF, anular).
- **Confirmaciones destructivas:** diálogo `.dialogo` con texto claro y botón rojo.
- **Notificaciones:** `Notificador` (toast) para éxito/error; nunca stack traces.

## 4. Componentes reutilizables (copiar/adaptar del referente)

`Tablas` (celdas de moneda, estado/chip, avatar, vacío), `Paginador`, `Formularios`, `Notificador`, `Autocompletar`, `Refrescable`, `Exportador`. Se agregan:

- `ui/Chips`: crea chips de estado con la clase y color semántico correctos.
- `ui/Tareas`: ejecuta consultas en un ejecutor secuencial y entrega el resultado al hilo de JavaFX.
- `ui/Semaforo`: pastilla verde/amarilla/roja con texto ("Al día", "Atraso leve", "Moroso").
- `ui/Navegacion`: registro clave → FXML del menú.

### Chips de estado (usar tokens semánticos, no hex)

| Estado | Semántica |
|---|---|
| Disponible, Vigente, Pagada, Emitido, Resuelto, Al día | éxito (verde) |
| Alquilado, En proceso, Parcial | acento (azul) |
| Por vencer, Pendiente, Mantenimiento, Reportado, Atraso leve | advertencia (ámbar) |
| Vencida, Vencido, Anulado, Moroso, Urgente | peligro (rojo) |
| Finalizado, Inactivo, Cancelado, Anulada | neutro (gris) |

## 5. Dashboard (referencia de composición)

Replica el layout del referente, adaptando el contenido (detalle de indicadores en `FASE_04`):

```text
Encabezado: Dashboard · "Resumen general · actualizado a las HH:mm"     [Últimos 30 días ▾] [Exportar Excel] [Actualizar]
Sección: COBRANZA
[KPI Total cobrado ▲%] [KPI Cartera vencida] [KPI Ocupación] [KPI Contratos por vencer]
[Tarjeta ancha: Evolución de cobros (línea)]            [Tarjeta: Inmuebles con mayor ingreso (top 5)]
[Tarjeta ancha: Últimos pagos (tabla + PDF, "Ver todos")] [Tarjeta: Arrendatarios en mora (semáforo, top 5)]
[Tarjeta: Ocupación por tipo (dona)]                    [Tarjeta: Contratos por vencer (lista, "Renovar")]
```

- Tarjeta KPI: título pequeño arriba a la izquierda, **icono en cuadro redondeado azul claro** arriba a la derecha, valor grande (28 px), y debajo una línea gris o un chip de variación (`▲ 12.5 %` verde / `▼ 8.0 %` rojo + "vs. periodo anterior").
- Gráfico de línea: eje Y con `S/ 0, S/ 20…`, líneas de cuadrícula punteadas, eje X con fechas cortas (`05 oct.`).
- Valores monetarios siempre con formato `S/ 1,234.50`.

## 6. Accesibilidad y calidad visual

- Contraste suficiente en ambos temas (probar cada pantalla en claro y oscuro).
- Tamaño mínimo de ventana razonable (≈ 1100×700); el contenido hace scroll, no se corta.
- Atajos: `Enter` guarda en formularios, `Esc` cierra drawer/diálogo.
- Tablas con orden por columna donde tenga sentido (fecha, monto).
- Coherencia: mismo orden de botones (secundario a la izquierda, primario a la derecha) en todos los diálogos.

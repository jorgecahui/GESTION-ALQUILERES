# Manual de usuario — Gestión de Alquileres

Esta guía resume el uso diario del sistema. Todas las pantallas están disponibles en modo claro y oscuro desde el pie del menú lateral.

## 1. Primer arranque

1. Abre la aplicación. Si la base está vacía verás la pantalla **Primer administrador**.
2. Elige un usuario (por ejemplo `admin`), un nombre y una contraseña de al menos 8 caracteres con letras y números.
3. Confirma la contraseña y pulsa **Crear administrador**.
4. Ingresa con esas credenciales en la pantalla de **Inicio de sesión**.

Si olvidas la contraseña, otro ADMIN puede restablecerla desde **Configuración → Usuarios**. Si eres el único administrador, conserva el acceso: el sistema impide desactivar o degradar al último ADMIN activo.

## 2. Roles

| Capacidad | ADMIN | OPERADOR | CONSULTA |
|---|:-:|:-:|:-:|
| Ver dashboard, listas y reportes | ✔ | ✔ | ✔ |
| Crear/editar catálogos, contratos, pagos y mantenimiento | ✔ | ✔ | ✖ |
| Anular pagos, finalizar/rescindir contratos, condonar mora | ✔ | ✖ | ✖ |
| Configuración, usuarios, auditoría y respaldos | ✔ | ✖ | ✖ |

Los botones no permitidos se deshabilitan con un tooltip. Además, el servicio rechaza la operación aunque se intente por otra vía.

## 3. Cerrar sesión y bloqueo

- El pie del menú muestra tu nombre y rol. El botón de salida cierra la sesión y vuelve al login.
- Si pasan los minutos configurados sin actividad, aparece la pantalla **Sesión bloqueada**. Ingresa tu contraseña para continuar o cierra la sesión.
- Para desactivar el bloqueo, pon `0` en **Configuración → Seguridad → Minutos de inactividad**.

## 4. Flujo típico

1. **Catálogo:** crea tipos de inmueble, propietarios y personas (arrendatarios/garantes).
2. **Inmuebles:** registra dirección, tipo, propietario, renta referencial y características.
3. **Contratos:** revisa la lista, el detalle, las cuotas y el estado de pago. Desde el detalle puedes generar el **estado de cuenta PDF** y la **hoja de contrato PDF**, y enviar el aviso de fin de contrato por WhatsApp.
4. **Cobranza:** en **Nuevo pago** selecciona el contrato, el monto recibido, la fecha y el medio de pago. El sistema imputa mora y capital; permite condonar la mora con un motivo (solo ADMIN) y registrar adelantos.
5. **Dashboard:** revisa cobros del periodo, cartera vencida, ocupación, contratos por vencer y arrendatarios en mora. Exporta el resumen a Excel.

## 5. Módulos

### Dashboard
Filtra por periodo (últimos 30 días, mes, año o rango personalizado). Usa **Actualizar** para recalcular y **Exportar Excel** para descargar el resumen.

### Contratos
Lista con búsqueda, filtro por estado y “solo por vencer”. El detalle muestra cuotas, recordatorios y accesos a documentos. El estado visual (Vigente, Por vencer, Vencido, Finalizado, Rescindido) se calcula con la fecha actual.

**Nuevo contrato** permite elegir un inmueble disponible, arrendatario y garante, y muestra el cronograma previsto antes de guardar. Desde el detalle se puede **Renovar** (precarga el mismo inmueble y arrendatario con la nueva fecha), **Finalizar** o **Rescindir** (con fecha de término y liquidación de garantía).

### Pagos
Consulta recibos por fecha. El botón **Nuevo pago** abre directamente el formulario de cobranza. Un pago emitido puede anularse (solo ADMIN) indicando el motivo; la anulación restaura los importes de las cuotas. El recibo PDF incluye el detalle, la mora y una marca de agua si está anulado.

### Cuentas por cobrar
Muestra cuotas pendientes y vencidas con semáforo (Al día / Atraso leve / Moroso), mora estimada y último recordatorio. **Cobrar** abre el formulario de pago con el contrato precargado; **Recordar** prepara el mensaje de WhatsApp.

### Mantenimiento
Registra incidencias por inmueble, prioridad, responsable del gasto y costos. Los estados son Reportado → En proceso → Resuelto/Cancelado. Al resolver una incidencia a cargo del arrendatario se puede generar un cargo en sus cuotas.

### Rentabilidad
Compara ingresos cobrados (capital + mora) contra gastos de mantenimiento por inmueble en un periodo, con margen y días de ocupación.

### Inmuebles, Tipos, Personas y Propietarios
Catálogos con búsqueda, edición y desactivación. Los registros con movimientos no se borran: se desactivan para conservar el historial. **Ver ficha** de un inmueble abre sus datos, historial de contratos, mantenimientos e ingresos/gastos del año.

### Recordatorios por WhatsApp
Desde Cuentas por cobrar, Contratos o Personas se abre una vista previa editable. Puedes **Copiar mensaje** o **Abrir WhatsApp**; el envío queda registrado en el historial del contrato.

## 6. Configuración

Solo el ADMIN ve esta opción. Las pestañas son:

- **Arrendador:** razón social, tipo y número de documento, dirección, teléfono, email y logo. El logo se copia a la carpeta de datos y se imprime en los PDF.
- **Recibos:** serie (por defecto `R001`) y leyenda al pie.
- **Mora:** tipo, valor y días de gracia por defecto. Se copian a cada contrato nuevo.
- **Alertas:** días para “por vencer” y días del recordatorio preventivo.
- **Semáforo:** umbral de atraso leve en días y cantidad de cuotas vencidas que marcan rojo.
- **Plantillas:** textos de recordatorio preventivo, vencido y fin de contrato, con marcadores como `{nombre}`, `{monto}`, `{dias}`. **Restaurar textos por defecto** recupera los originales.
- **Seguridad:** minutos de inactividad.
- **Respaldo:** crear respaldo, ver respaldos automáticos y restaurar.
- **Usuarios:** alta, edición, activación/desactivación y restablecimiento de contraseña.
- **Auditoría:** eventos por fecha, usuario y acción, con exportación a Excel.

Los cambios se guardan con **Guardar cambios** y quedan registrados en auditoría.

## 7. Respaldo y restauración

- **Crear respaldo ahora:** elige una carpeta; se genera `alquileres-AAAAMMDD-HHmmss.db` con `VACUUM INTO`.
- **Respaldo automático:** al cerrar la aplicación se guarda una copia en `<datos>/respaldos/` y se conservan las últimas 10.
- **Restaurar respaldo:** elige un archivo `.db`. El sistema verifica la cabecera SQLite, las tablas esperadas y `PRAGMA integrity_check`; antes de sustituir crea un respaldo de seguridad y luego cierra la aplicación. Vuelve a abrirla para trabajar con los datos restaurados.

La carpeta de datos se muestra en la pestaña Respaldo. En producción suele ser `%USERPROFILE%\.gestion-alquileres`.

## 8. Preguntas frecuentes

**¿Por qué no puedo anular un pago?**  
Solo el rol ADMIN puede anular pagos, condonar mora y finalizar/rescindir contratos.

**¿Por qué no veo Configuración?**  
La opción está reservada al ADMIN. Un OPERADOR o CONSULTA no la ve en el menú.

**¿Qué pasa si dos personas editan el mismo registro?**  
El sistema usa control de concurrencia optimista. Si el registro cambió, verás “El registro fue modificado por otra operación. Actualice e intente de nuevo.”

**¿Dónde están los logs?**  
En `<datos>/logs/app.log`, con rotación por tamaño y antigüedad. En desarrollo, `./data/logs/app.log`.

**¿Los recibos son comprobantes SUNAT?**  
No. Son documentos internos de control; no tienen validez tributaria.

**¿Puedo cambiar la carpeta de datos?**  
Sí, con `-Dapp.data-dir=...` al iniciar el jar o el app-image.

**¿Cómo actualizo el ejecutable?**  
Vuelve a ejecutar `scripts\empaquetar.ps1` y conserva tu carpeta de datos; la base no se borra al reinstalar.

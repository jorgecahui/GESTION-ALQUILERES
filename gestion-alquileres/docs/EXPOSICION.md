# Guion de exposición — 8 a 10 minutos

Objetivo: mostrar que el sistema resuelve el día a día de un administrador de alquileres y que tiene detalles propios frente a un CRUD genérico.

## Antes de empezar (1 minuto)

- Ejecuta `mvnw.cmd javafx:run` o abre el app-image.
- Ten a mano un usuario ADMIN y, si es posible, un usuario OPERADOR para demostrar permisos.
- Verifica que el perfil `dev` tenga datos de ejemplo (inmuebles, personas, contratos y pagos).

## 1. Login y roles (1 minuto)

1. Muestra la pantalla de login y explica que las contraseñas se guardan con BCrypt.
2. Ingresa como ADMIN. Señala el nombre y rol en el pie del menú.
3. Menciona el bloqueo tras 5 intentos y el bloqueo por inactividad configurable.
4. Si tienes un usuario OPERADOR, inicia sesión con él y muestra que Configuración no aparece y que Anular/Condonar están deshabilitados.

**Mensaje clave:** la seguridad no es solo visual; los servicios vuelven a validar el permiso.

## 2. Dashboard (1.5 minutos)

1. Abre **Dashboard** y recorre los KPIs: total cobrado, cartera vencida, ocupación y contratos por vencer.
2. Cambia el periodo y pulsa **Actualizar**.
3. Señala la evolución de cobros, el top de inmuebles, los últimos pagos y el semáforo de morosos.
4. Abre un **PDF** de un pago reciente y muestra el recibo.
5. Pulsa **Exportar Excel** y comenta las hojas del reporte.

**Detalle propio:** semáforo de morosidad y comparación contra el periodo anterior.

## 3. Catálogos (1 minuto)

1. **Inmuebles:** muestra la lista, los filtros y la ficha con renta y estado.
2. **Personas:** búsqueda por documento, contacto por WhatsApp.
3. **Propietarios y Tipos:** explica que los registros con movimientos se desactivan, no se borran.

## 4. Contratos y cobranza (2 minutos)

1. Abre **Contratos**, selecciona uno y muestra el detalle: cuotas, estado de pago, recordatorios.
2. Genera la **hoja de contrato PDF** y el **estado de cuenta PDF**.
3. Abre **Nuevo pago**, selecciona el contrato y muestra cómo se imputa primero la mora y luego el capital.
4. Registra un pago de prueba y abre el recibo generado.
5. En **Pagos**, explica la anulación reversible (solo ADMIN) y la marca de agua **ANULADO**.
6. En **Cuentas por cobrar**, muestra el semáforo, la mora estimada y el botón **Recordar** con la vista previa de WhatsApp.

**Detalle propio:** mora automática con días de gracia, condonación con motivo y mensaje de WhatsApp listo para enviar.

## 5. Mantenimiento y rentabilidad (1 minuto)

1. Abre **Mantenimiento**, muestra una incidencia resuelta y el costo real.
2. Explica el cargo al arrendatario cuando corresponde.
3. Abre **Rentabilidad** y muestra ingresos, gastos, neto y margen por inmueble.

**Detalle propio:** rentabilidad real por inmueble, no solo cobranza.

## 6. Reportes y recordatorios (0.5 minutos)

1. En **Pagos**, **Contratos**, **Inmuebles**, **Mantenimiento** o **Rentabilidad**, pulsa **Exportar Excel** y recuerda que respeta los filtros activos.
2. Menciona el estado de cuenta PDF y la hoja de contrato.

## 7. Configuración, usuarios y auditoría (1.5 minutos)

1. Abre **Configuración → Arrendador** y muestra el logo y los datos que salen en los PDF.
2. Cambia la serie del recibo o la mora por defecto, guarda y explica que los contratos nuevos toman esos valores.
3. Muestra las plantillas de recordatorio y el botón **Restaurar textos por defecto**.
4. En **Usuarios**, crea un OPERADOR o muestra la protección del último ADMIN.
5. En **Auditoría**, filtra por acción y exporta a Excel. Señala que nunca guarda contraseñas.

**Detalle propio:** configuración centralizada con caché y auditoría de operaciones sensibles.

## 8. Respaldo y cierre (1 minuto)

1. En **Configuración → Respaldo**, muestra la ruta de datos y los respaldos automáticos.
2. Crea un respaldo manual.
3. Explica la restauración: validación SQLite, respaldo de seguridad y reinicio.
4. Cierra la aplicación y recuerda que el respaldo automático conserva los últimos 10.
5. Si queda tiempo, muestra el **modo oscuro**.

## Cierre (30 segundos)

> “El sistema cubre el ciclo completo: catálogo, contrato, cobranza con mora, recibos, mantenimiento, rentabilidad, reportes, seguridad por roles, auditoría y respaldo. Está empaquetado con runtime incluido y se puede instalar sin depender de un JDK en la máquina del usuario.”

## Preguntas frecuentes del jurado

- **¿Dónde está la mora?** En cada cuota, calculada al momento del pago con días de gracia y opción de condonación auditada.
- **¿Cómo evitan duplicados?** Restricciones únicas en documentos, códigos y recibos; numeración atómica con `SecuenciaService`.
- **¿Qué pasa si se cae la aplicación?** La base SQLite se respalda al cerrar; además hay respaldo manual y restauración validada.
- **¿Es multiusuario?** Es una aplicación de escritorio con un usuario a la vez; el control de concurrencia optimista protege ediciones simultáneas.
- **¿Los recibos son SUNAT?** No; son documentos internos de control, claramente marcados como tales.

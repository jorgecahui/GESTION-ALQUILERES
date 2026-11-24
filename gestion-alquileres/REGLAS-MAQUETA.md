# REGLAS-MAQUETA: reglas fijas del trabajo sobre la Maqueta 3D

Estas reglas aplican a **todas** las fases de `PLAN-MAQUETA.md`. Si el plan y este archivo chocan, **gana este archivo**.

## 1. Reglas técnicas del proyecto

1. **Sin dependencias nuevas.** No modificar `pom.xml`. Solo JavaFX 3D nativo (`javafx-graphics`): `Box`, `Cylinder`, `Sphere`, `SubScene`, `PerspectiveCamera`, `PhongMaterial`, luces, `Group`, `Rotate`, `Translate`, `Timeline`, `KeyValue`, `Interpolator`, `Canvas`.
2. **Sin modelos ni texturas externas** (.obj, .fbx, glTF, imágenes de muebles). Solo geometría y colores generados por código.
3. **Sin cambios de esquema ni de base de datos.** No agregar columnas ni entidades.
4. **Solo DTOs (records) en la UI.** Nunca entidades JPA hacia el controlador (`open-in-view=false`: provoca `LazyInitializationException`).
5. **Acceso a BD solo vía `Tareas.ejecutar(...)`**, con el resultado devuelto al hilo JavaFX. Ningún nodo 3D ni de UI se crea fuera del hilo JavaFX.
6. **Servicios de lectura** con `@Transactional(readOnly = true)`. Sin N+1: usar consultas con `join fetch` o cruces en memoria como en `MaquetaService`.
7. **Fechas con el `Clock` inyectado**, nunca `LocalDate.now()` suelto. Dinero en `BigDecimal` (escala 2, `HALF_UP`).
8. **`MaquetaController` es `@Scope("prototype")`.** No convertirlo en singleton ni guardar estado de escena en beans singleton. Cada carga del FXML debe tener su propio `SubScene`, mundo y cámara.
9. **Controladores resueltos por Spring** (`setControllerFactory(context::getBean)`), nunca con `new`.
10. **Permisos:** la maqueta es solo lectura. No agregar acciones de escritura. Lo que muestra el panel de datos debe ser lo que ya ven los demás roles en otras pantallas.
11. **Textos de UI en español.** Nombres de clases y métodos en español, como el resto del proyecto.
12. **No hacer `git commit`.** El usuario decide cuándo.
13. **Si falta un permiso, dato o decisión, pregunta o elige lo más simple y anótalo en el informe.** No inventes datos de negocio.

## 2. Reglas específicas de JavaFX 3D (lecciones ya aprendidas en este proyecto)

1. **No animar `opacity` de nodos 3D** ni depender de transparencias superpuestas: producen artefactos de orden de dibujo. Para ocultar: escala a 0 + `setVisible(false)`, o intercambio de materiales precalculados. Para fundidos, usar una **capa 2D superpuesta** (`Pane`/`Rectangle` en el `StackPane`) cuya opacidad sube y baja.
2. **Reutilizar `PhongMaterial`.** Un material por categoría, precalculado. Prohibido crear materiales en eventos de mouse o durante animaciones.
3. **Liberar nodos** al reconstruir o cambiar de nivel: quitar hijos, limpiar mapas `Node → BloqueMaqueta` y listeners. Entrar y salir 20 veces no debe hacer crecer la escena.
4. **Picking vs. arrastre:** un clic cuenta solo si el mouse se movió ≤ 5 px entre presionar y soltar. Arrastrar para rotar nunca abre nada.
5. **Cámara:** `PerspectiveCamera` con `fixedEyeAtCameraZero(false)`; recordar el desplazamiento focal al calcular el encuadre. Todo encuadre se calcula desde la geometría real, sin números fijos.
6. **Etiquetas:** los rótulos de piso, unidad y ambiente son `Label` 2D proyectados con `localToScene` sobre una capa transparente, actualizados al rotar, hacer zoom o redimensionar. Se ocultan o reducen si hay demasiados o si el zoom es muy lejano.
7. **Profundidad y antialiasing:** `SubScene` con depth buffer y `SceneAntialiasing.BALANCED`. Fondo transparente para respetar el tema.
8. **Comprobar soporte 3D** con `Platform.isSupported(ConditionalFeature.SCENE3D)` y mostrar un mensaje claro si no hay.
9. **Animaciones:** una sola animación de navegación activa a la vez. Guardar la referencia, detenerla en `refrescar()`, al cambiar de pantalla y al reiniciar la vista. Nunca dejar `Timeline` huérfanas.
10. **Errores:** si construir una escena falla, limpiar lo parcial, mostrar mensaje dentro del contenedor y `Notificador`. Nunca dejar el panel en blanco.
11. **Tema claro y oscuro:** todo color y trazo nuevo debe verse bien en ambos temas. Las clases CSS van en `app.css`; evitar estilos en línea repetidos.
12. **Organización del código:** `MaquetaController` ya es grande. Extrae a clases nuevas lo que corresponda (por ejemplo `NivelVista`, `GeneradorEsquema`, `EscenaPlanta`, `EscenaUnidad`, `TransicionMaqueta`) en lugar de seguir inflándolo. La lógica de cálculo (encuadres, anchos, esquema) debe ser **pura y testeable sin UI**.

## 3. Lo que NO se puede romper (verificar en cada fase)

- Layout por piso: una fila por piso, centrado; división en filas si el piso más ancho supera 8 unidades.
- Números de unidad y etiquetas de piso.
- Marcas de mora (solo en modo Estado), sobre su bloque.
- Selector **Ver por** (Estado, Mora, Renta, Vencimiento), recoloreo sin reconstruir, persistencia con `java.util.prefs`.
- Leyenda y resumen de contadores según modo; filtro de leyenda y bloques atenuados clicables.
- Hover, clic con modal (con dato del modo activo) y botón **Ver en Inmuebles**.
- Encuadre automático y escalado con cualquier cantidad de bloques y pisos.
- Rotación, zoom y **Restablecer vista** (habilitado para todos los roles, incluido CONSULTA).
- Aviso de inmuebles sin ubicación y estado vacío.
- Actualización al volver a la pantalla (`Refrescable`).
- Funcionamiento tras cerrar sesión y entrar con otro rol.
- Tema claro y oscuro.

## 4. Tests

- Línea base: **146 pasan, 0 fallos, 0 errores**. Cada fase agrega pruebas y nunca baja ese número.
- Comando: `.\mvnw.cmd -q test`.
- La lógica pura (encuadres, anchos proporcionales, generador de esquema, máquina de estados) se prueba **sin JavaFX**.
- Los DTO nuevos no deben contener tipos `Inmueble` ni `Contrato`.
- Actualizar `FxmlSmokeTest` si cambia algún FXML o controlador.

## 5. Máquina de estados de navegación

```
EDIFICIO ──clic piso──▶ PISO ──clic unidad──▶ UNIDAD
   ▲                      │                      │
   └──────── Volver ◀─────┴────── Volver ◀───────┘
Restablecer vista y Esc fuera de UNIDAD/PISO: vuelven a EDIFICIO
```

- Transiciones permitidas definidas en un solo lugar y probadas.
- Transición en curso → ignorar nuevas órdenes de navegación o cancelar la anterior de forma limpia.
- `Esc` vuelve **un** nivel. "Restablecer vista" vuelve directamente a `EDIFICIO`.
- Duraciones sugeridas: 400 a 600 ms, `Interpolator.EASE_BOTH`. Mismas duraciones en todos los niveles.

## 6. Generador de esquema (Fase 3)

Clase pura, determinista, sin JavaFX. Entradas: `areaM2` (puede ser nula), `dormitorios`, `banos`, `amoblado`. Salida: lista de recintos `(tipo, x, y, ancho, alto)` en metros, más un indicador `ajustado` si hubo que reducir ambientes por falta de área. Rótulo obligatorio en pantalla: **"Esquema referencial"**.

### 6.1 Valores por defecto y límites
- Área nula o ≤ 0: usar un área por defecto según dormitorios (0 → 30 m², 1 → 45, 2 → 65, 3 → 90, ≥4 → 90 + 20 por dormitorio adicional) y marcar `areaEstimada = true` (mostrar "área estimada").
- Proporción del rectángulo exterior: ancho/alto ≈ 1,4 (ancho = √(área × 1,4); alto = área / ancho).
- Valores topados: dormitorios ≤ 8, baños ≤ 6 (si hay más, dibujar el tope y anotarlo).

### 6.2 Tamaños objetivo de ambientes
- Dormitorio: 9 a 14 m² (el primero, dormitorio principal, es el mayor: 12 a 16 m²).
- Baño: 3 a 4,5 m².
- Cocina: 6 a 9 m². Sala-comedor: el resto, debe ser **al menos 30 %** del área.
- Pasillo: ~1 m de ancho si hay 2 o más dormitorios.
- Si la suma de objetivos supera el área, reducir **proporcionalmente** dormitorios y baños hasta sus mínimos (dormitorio 7 m², baño 2,5 m²). Si aun así no entra, marcar `ajustado = true` y dejar el resto para sala-cocina.

### 6.3 Esquema completo (`dormitorios >= 1`)
Disposición por franjas, de izquierda a derecha:
1. **Franja social** (≈ 45 a 55 % del ancho): sala-comedor y cocina apilados; la cocina contra el borde superior.
2. **Franja privada** (resto): dormitorios apilados a lo largo del alto, con baño(s) entre ellos y la franja social, o pegados al dormitorio principal (baño del principal primero). Con ≥ 2 dormitorios, un pasillo vertical entre franjas.
3. Los ambientes deben **llenar el rectángulo exterior sin huecos ni solapes**; el reparto del alto se hace proporcional al área objetivo de cada ambiente de la franja.
4. Puerta principal sobre el borde inferior de la franja social (solo un marcador en el plano).

### 6.4 Recinto simple (`dormitorios = 0`)
- Un único ambiente rectangular con todo el área, rotulado según tipo (por ejemplo "Local", "Oficina", "Ambiente").
- Si `banos > 0`: un baño de 3 m² en la esquina superior derecha (más baños, uno al lado del otro, hasta el tope).
- Acceso sobre el borde inferior.

### 6.5 Propiedades que los tests deben verificar
- Determinismo: mismos datos → misma salida.
- La suma de áreas de los recintos = área total (tolerancia ≤ 1 %).
- Sin solapes entre recintos y todos dentro del rectángulo exterior.
- Cantidad de dormitorios y baños dibujados = la registrada (hasta los topes).
- Casos límite: área nula, 1 m², 500 m², 0 dormitorios con 0 baños, 6 dormitorios, 5 baños.

### 6.6 Representación
- **Plano (2D):** paredes como líneas de ~3 px, rótulos de ambiente y medida aproximada, puerta como arco o marca. Color de línea según tema.
- **3D:** paredes como `Box` finos (grosor ~0,15 m, altura ~2,6 m) alrededor de cada recinto; piso de cada ambiente con color suave por tipo (sala, cocina, dormitorio, baño); sin techo, sin muebles. Cámara cenital con ligera inclinación, rotación y zoom acotados.
- Máximo de nodos por unidad: mantenerlo bajo (agrupar paredes contiguas cuando sea posible).

## 7. Qué hacer si algo falla

1. Si una animación o escena produce artefactos, **simplifica** (menos nodos, sin transparencia, sin animar propiedades problemáticas) antes de añadir complejidad.
2. Si no puedes resolver algo con JavaFX nativo, **dilo en el informe** y propón la alternativa más simple; no añadas dependencias por tu cuenta.
3. Si un test existente falla por tu cambio, corrige tu cambio o actualiza el test con justificación, nunca lo borres para que pase.
4. Si dudas de si algo rompe una función existente, revísalo contra la sección 3 antes de continuar.

## 8. Verificación visual

- Si tu entorno permite ejecutar la app (`.\mvnw.cmd javafx:run`) y capturar pantalla, hazlo en tema claro y oscuro, en los tres niveles, y reporta lo que realmente observaste (incluido lo que se vea mal).
- Si no puedes ver la pantalla, dilo y pide una captura al usuario. No describas resultados visuales que no hayas comprobado.
- Después de rotar, hacer zoom y entrar/salir de niveles repetidamente, revisa la consola: no debe haber excepciones de JavaFX.

# PLAN-MAQUETA: navegación Edificio › Piso › Unidad

> **Antes de hacer nada, lee `REGLAS-MAQUETA.md` completo.** Ese archivo manda sobre este plan si hay conflicto.

## 1. Objetivo

Convertir la Maqueta 3D actual (un edificio, bloques por nivel y orden, color por estado/mora/renta/vencimiento) en una experiencia de tres niveles, con transiciones suaves, usando **solo JavaFX nativo**:

1. **Edificio** (ya existe): torre con todas las unidades coloreadas según "Ver por".
2. **Piso**: al elegir un piso, los demás se retiran y la cámara baja a una **vista de planta** de ese piso.
3. **Unidad**: al elegir una unidad, se pasa a su **esquema** (paredes finas y zonas rotuladas), con alternancia **Plano (líneas) / 3D (volumen)** y un **panel de datos** con contrato, mora y cuotas.

La referencia es un visor inmobiliario comercial (torre → planta → plano → interior 3D). Se busca **la misma estructura y flujo**, no el realismo de muebles, plantas ni texturas, que JavaFX no puede igualar sin modelos importados.

## 2. Alcance

**Dentro:**
- Navegación en tres niveles con breadcrumb y botón Volver.
- Animaciones de cámara y transiciones entre niveles.
- Planta del piso con unidades como recintos, coloreados según el modo "Ver por".
- Esquema de unidad generado desde datos reales (`areaM2`, `dormitorios`, `banos`).
- Alternancia Plano / 3D y panel de datos.
- Tests de la lógica pura (encuadres, generador de esquema, máquina de estados).

**Fuera (no hacer):**
- Plano subido por el usuario (se evaluará más adelante, no ahora).
- Muebles, plantas, texturas fotográficas, sombras realistas.
- Varios edificios, campo `edificio`, cambios de esquema de BD.
- Librerías o modelos 3D externos.

## 3. Cómo trabajar este plan

1. **Una fase por vez.** Al terminar una fase, **detente**, entrega el informe (sección 9) y espera confirmación del usuario antes de empezar la siguiente.
2. No adelantes trabajo de fases futuras.
3. Antes de empezar cada fase, lee el código actual de `MaquetaController`, `MaquetaService`, `BloqueMaqueta` y los tests relacionados. No asumas su contenido.
4. Si una decisión de diseño no está clara, elige la opción más simple que cumpla los criterios de aceptación y déjala anotada en el informe.
5. **Verificación visual:** si tu entorno permite ejecutar la app y capturar pantalla, hazlo y revisa el resultado antes de reportar, en tema claro y oscuro. Describe lo que realmente observaste. Si no puedes ver la pantalla, dilo y pide al usuario una captura. No afirmes nada visual que no hayas comprobado.
6. Línea base de tests: **146 pasan**. Nunca deben bajar. Cada fase agrega los suyos.

## 4. Datos disponibles (no inventar otros)

- `BloqueMaqueta`: id, codigo, nombre, tipo, nivel, orden, estado, contratoId, arrendatario, semaforo, rentaReferencial, fechaFinContrato, diasParaVencer.
- `Inmueble`: areaM2, dormitorios, banos, estacionamientos, amoblado, rentaReferencial, nivel, orden, tipo.
- Modos "Ver por": Estado, Mora, Renta, Vencimiento (materiales precalculados y recoloreo sin reconstruir).
- Detalle bajo demanda: `MaquetaService.detalle(Long)` devuelve DTO con inmueble, contrato vigente, semáforo, cuotas vencidas y monto pendiente.
- Datos demo: edificio de 4 niveles × 5 unidades. Nivel 1: locales y oficinas (0 dormitorios). Niveles 2 a 4: departamentos.

Si una fase necesita un dato que el DTO actual no trae (por ejemplo `areaM2`, `dormitorios`, `banos`), se **agrega al DTO** y a `MaquetaService` sin N+1, sin entidades JPA y sin cambiar el esquema.

---

## 5. Fase 1: Piso enfocado

**Meta:** pasar del edificio a un piso con una transición suave.

**Comportamiento**
1. Clic en la etiqueta "Piso N" o en la losa de ese piso → entra en estado `PISO`.
2. Los demás pisos desaparecen en ~400 ms. **No animar `opacity` de nodos 3D.** Usar escala hacia 0 en Y y luego `setVisible(false)`, o intercambio a materiales precalculados; documentar la elección. Al volver, se restauran.
3. La cámara se mueve con `Timeline` e `Interpolator` hasta encuadrar el piso (reutilizar la lógica de encuadre automático, calculada con el ancho real de ese piso).
4. Rotación por arrastre y zoom siguen funcionando, sin saltos al empezar a arrastrar.
5. Barra sobre la escena: `Edificio › Piso N` y botón **Volver al edificio**. La tecla `Esc` también vuelve.
6. En este estado funcionan: hover, clic con modal, números de unidad, marcas de mora, selector "Ver por", filtro de leyenda. El resumen de contadores pasa a ser el del piso, con un rótulo claro.
7. Clic en otro piso estando enfocado: no se soporta; solo Volver.

**Estado y animaciones**
- Máquina de estados explícita (`NivelVista`: `EDIFICIO`, `PISO`, `UNIDAD`) con transiciones permitidas definidas en un solo lugar.
- Mientras una animación corre, ignorar nuevos clics de navegación o cancelar la anterior. Nunca dejar estado a medias.
- `refrescar()`, cambio de pantalla y "Restablecer vista" detienen las animaciones activas; "Restablecer vista" vuelve a `EDIFICIO`.

**Criterios de aceptación**
- [ ] Entrar y volver 20 veces seguidas no deja pisos ocultos, cámara descentrada ni animaciones colgadas.
- [ ] Funciona con cualquier cantidad de pisos y de unidades por piso.
- [ ] Cambiar de modo "Ver por" estando enfocado recolorea sin salir del piso.
- [ ] Cerrar sesión y entrar con otro usuario no rompe la escena (controlador prototype).
- [ ] Tests: encuadre de un piso, transiciones válidas e inválidas de la máquina de estados.

---

## 6. Fase 2: Planta del piso

**Meta:** que el piso enfocado se vea como una **planta vista desde arriba**, equivalente a la planta del visor de referencia.

**Comportamiento**
1. Al entrar a `PISO`, la cámara pasa de la vista oblicua a una **vista cenital** (o casi cenital, con ligera inclinación configurable) con transición animada.
2. Las unidades se dibujan como **recintos en planta**: rectángulos con paredes finas, alineados a lo largo de un **pasillo** común. Ancho de cada recinto proporcional a su `areaM2` (con mínimo y máximo razonables); profundidad constante. Si el piso tiene muchas unidades, dividir en dos hileras enfrentadas al pasillo.
3. Cada recinto conserva color según "Ver por", número de unidad y marca de mora (en modo Estado).
4. Rótulos de unidad legibles en la planta (Label 2D proyectado, como las etiquetas actuales).
5. Rotación limitada en este estado (por ejemplo solo giro alrededor del eje vertical y zoom), para no perder la orientación de planta.
6. Clic en un recinto → modal actual (hasta que exista la Fase 3), con botón **Ver unidad** deshabilitado o preparado.
7. Unidades sin `areaM2` usan un ancho por defecto y se rotulan sin m².

**Criterios de aceptación**
- [ ] La planta se genera desde los datos reales del piso, sin números fijos de unidades.
- [ ] Pisos con 3, 5 u 8 unidades se ven correctos y centrados.
- [ ] Hover, clic, filtro de leyenda y "Ver por" funcionan.
- [ ] Volver restaura la vista del edificio con animación.
- [ ] Tests: cálculo de anchos proporcionales (incluidos mínimo, máximo y área nula) y de la distribución en una o dos hileras.

---

## 7. Fase 3: Unidad esquemática

**Meta:** al elegir una unidad, mostrar su **esquema referencial** con alternancia **Plano / 3D** y un **panel de datos**.

**Principio de honestidad:** la distribución interior es **inventada por el sistema**. Debe rotularse en pantalla **"Esquema referencial"**. Lo único real son los datos: área, dormitorios y baños. No se inventan cantidades, solo la ubicación de cada ambiente.

**Generador de esquema (lógica pura, testeable sin UI)**
- Clase independiente (por ejemplo `GeneradorEsquema`) que recibe `areaM2`, `dormitorios`, `banos`, `amoblado` y devuelve una lista de recintos (tipo, x, y, ancho, alto) en metros, más un indicador de si el área alcanzó.
- Reglas detalladas en `REGLAS-MAQUETA.md`, sección "Generador de esquema".
- **Esquema completo** si `dormitorios >= 1`. **Recinto simple** si `dormitorios = 0` (locales, oficinas, lofts): un solo ambiente rectangular con su área y, si `banos > 0`, un baño pequeño en una esquina.

**Vista Plano (2D)**
- Dibujada con `Canvas` o formas 2D: paredes en líneas, rótulos de ambiente y medidas aproximadas. Líneas claras sobre fondo oscuro o líneas oscuras sobre fondo claro, según el tema.

**Vista 3D**
- Escena propia: piso del recinto, paredes como cajas finas de altura uniforme, zonas de piso coloreadas por tipo de ambiente (suave, no saturado) y rótulos. Sin muebles. Cámara cenital con rotación y zoom acotados.
- Botón **Plano / 3D** (estilo del botón naranja del visor, adaptado al tema de la app). Alternar con **fundido en una capa 2D superpuesta**, no animando `opacity` de nodos 3D.

**Panel de datos (lateral)**
- Reutiliza el contenido del modal actual (inmueble, contrato vigente, arrendatario, fechas, renta, semáforo, cuotas vencidas, monto pendiente, aviso de inconsistencia). Mismas reglas de lectura y permisos.
- Botón **Ver en Inmuebles** conservado.

**Navegación**
- Breadcrumb `Edificio › Piso N › Unidad` con Volver (un nivel) y `Esc`. "Restablecer vista" vuelve a `EDIFICIO`.
- Desde la unidad no se puede saltar a otra unidad sin volver al piso (simplicidad).

**Criterios de aceptación**
- [ ] Todas las unidades del seed muestran un esquema coherente (depto 1, 2 y 3 dormitorios; locales y oficinas simples).
- [ ] La suma de áreas de los ambientes coincide con el área de la unidad (tolerancia pequeña).
- [ ] El esquema es **determinista**: los mismos datos producen siempre el mismo dibujo.
- [ ] Casos límite sin fallo: área nula o muy pequeña, muchos baños, 0 dormitorios con baños, área enorme.
- [ ] Alternar Plano/3D 20 veces no acumula nodos ni deja la escena en blanco.
- [ ] El rótulo "Esquema referencial" está siempre visible.
- [ ] Tests: generador (casos normales y límite, determinismo, suma de áreas, sin solapes), selección completo/simple, DTO con los campos nuevos.

---

## 8. Fase 4: Pulido

**Meta:** que todo se sienta coherente y presentable.

1. Transiciones homogéneas entre niveles (misma duración y curvas).
2. Tema claro y oscuro revisados en los tres niveles (rótulos, líneas del plano, suelo, paredes).
3. Revisar rendimiento: sin fugas de nodos ni materiales al entrar y salir muchas veces; liberar escenas de niveles no visibles.
4. Estados vacíos y de error con mensaje claro dentro del contenedor y `Notificador`.
5. Atajos: `Esc` para volver; la rueda del mouse mantiene su zoom en todos los niveles.
6. Revisar con rol CONSULTA, OPERADOR y ADMIN, y con cierre y reinicio de sesión.
7. Revisión de ventana pequeña y maximizada en cada nivel.
8. Actualizar `README.md` y `AGENTS.md` con la navegación, el selector "Ver por" y el esquema referencial.

**Criterios de aceptación**
- [ ] Recorrido completo Edificio → Piso → Unidad → Volver, repetido, sin errores en consola.
- [ ] Todo lo existente sigue funcionando (lista de `REGLAS-MAQUETA.md`, sección 3).
- [ ] Tests sin regresiones y documentación actualizada.

---

## 9. Informe al terminar cada fase

Entrega siempre, en este formato:

1. **Archivos creados y modificados.**
2. **Decisiones de diseño** y por qué (en especial las animaciones y qué técnica elegiste para ocultar nodos).
3. **Resultado de `.\mvnw.cmd -q test`:** cuántos pasan (línea base 146) y cuáles son los nuevos.
4. **Verificación visual:** qué ejecutaste y qué observaste, o la indicación de que necesitas una captura del usuario.
5. **Riesgos o puntos débiles** que detectaste.
6. **Qué debe probar el usuario a mano.**
7. Detente y espera confirmación antes de la siguiente fase.

## 10. Orden y dependencias

`Fase 1 → Fase 2 → Fase 3 → Fase 4`. Cada una depende de la anterior. No se omite ninguna.

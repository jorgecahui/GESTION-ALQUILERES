# AGENTS.md

## Proyecto

Aplicación de escritorio JavaFX + Spring Boot para gestión de alquileres, en español y orientada al mercado peruano. La base de datos es SQLite.

## Comandos

```powershell
.\mvnw.cmd -q test          # suite completa (174 pruebas al cierre de la maqueta)
.\mvnw.cmd javafx:run       # aplicación con perfil dev y datos de ejemplo
```

## Reglas de trabajo

- Leer `REGLAS-MAQUETA.md` y `PLAN-MAQUETA.md` antes de tocar la maqueta.
- No agregar dependencias (`pom.xml`), entidades, columnas ni modelos/texturas externas.
- La maqueta es solo lectura y no debe agregar acciones de escritura.
- `MaquetaController` es `@Scope("prototype")`; no convertirlo en singleton ni guardar nodos de escena en beans compartidos.
- Controladores resueltos por Spring (`setControllerFactory(context::getBean)`), nunca con `new`.
- No hacer `git commit`; lo decide el usuario.
- Los DTO que llegan a la UI no pueden contener entidades JPA (`Inmueble`, `Contrato`).

## Maqueta 3D

- Navegación: `EDIFICIO > PISO > UNIDAD`, definida en `ui/NivelVista`.
- Estado del edificio: `MaquetaController`; cálculo puro en `ui/PlantaPiso` (planta del piso) y `ui/GeneradorEsquema` (esquema referencial de la unidad).
- `BloqueMaqueta` transporta `areaM2`, `dormitorios`, `banos` y `amoblado`, además de los datos de estado, mora, renta y vencimiento.
- Selector **Ver por**: Estado, Mora, Renta y Vencimiento; persiste en `java.util.prefs` con la clave `maqueta.modo`.
- Vista de piso: recintos en planta, pasillo común y una o dos hileras.
- Vista de unidad: alternancia Plano (Canvas) / 3D (JavaFX nativo), panel lateral con detalle y aviso obligatorio **"Esquema referencial"**.
- Transiciones: una sola animación de navegación activa, 450 ms y `Interpolator.EASE_BOTH`; los fundidos de vista usan una capa 2D. No animar `opacity` de nodos 3D.
- Nodos de la unidad: se crean al entrar y se liberan al salir; no deben acumularse al repetir el recorrido.

## Documentación

- `README.md`: descripción general, ejecución y sección de la maqueta.
- `REGLAS-MAQUETA.md` y `PLAN-MAQUETA.md`: reglas y fases de la maqueta.
- `docs/`: manual de usuario, exposición y progreso.

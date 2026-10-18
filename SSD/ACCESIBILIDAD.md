# T-39: auditoría de accesibilidad y tareas

Fecha de la ejecución automática: 18 de octubre de 2026. Rama: `yang_dev`.
Seguimiento: issue #62, continuación de #28. Se usan únicamente identidades,
teléfonos, conversaciones y mensajes ficticios.

## Alcance y perfiles

La revisión cubre creación de cuenta, inicio, alta de contacto, lista y detalle
de conversación, texto, grabación/subida/reproducción de audio, perfil y
ajustes. Se aplican tres perfiles representativos:

1. baja visión: escala de texto al 200 % en 320 dp y 411 dp;
2. menor precisión motora: objetivos táctiles de 48 × 48 dp y acciones de una
   pulsación;
3. poca experiencia tecnológica: nombres explícitos, estado actual, errores y
   recuperación comprensibles.

## Evidencia automática reproducible

La suite instrumentada habilita el Accessibility Test Framework oficial de
Android en las pruebas Compose de los siete flujos. Antes de las interacciones
y en los escenarios a escala máxima se revisan, entre otros, etiquetas,
acciones y tamaño táctil. Esta comprobación complementa, pero no reemplaza, la
escucha con TalkBack.

Entorno utilizado:

| Dato | Valor |
|---|---|
| Emulador | `NAMI_API_36` (`sdk_gphone64_x86_64`) |
| Android | 16 / API 36 |
| Resolución y densidad | 1080 × 2400 px, 420 dpi (perfil de 411 dp) |
| Segundo ancho | contenedor Compose de 320 dp |
| Escala máxima | 200 % mediante `NamiTheme`/`FontScale` |
| TalkBack instalado | `com.google.android.marvin.talkback` |
| Resultado | 54 pruebas de `com.proyectonami.app.feature`, 0 fallos, 0 errores, 0 omitidas |

| Flujo | 320/411 dp al 200 % | Estados y semántica cubiertos |
|---|---|---|
| Cuenta | ambos | título, campos, validación, error, reintento y confirmación |
| Inicio | ambos | carga, vacío, error, conversación, avatar, no leídos y acciones |
| Contacto | ambos | formulario, pendiente, vinculado, error y reintento |
| Conversación/texto | ambos | historial, vacío, envío, fallo y reintento sin perder el borrador |
| Audio | ambos | permiso, grabación, vista previa, escuchar, borrar, enviar y límites |
| Perfil | ambos | contenido, carga/error anunciable, guardar, reintentar y volver |
| Ajustes | ambos | valor seleccionado, lectura en voz alta y persistencia |

Los componentes compartidos fijan un mínimo de 48 dp y muestran texto junto a
los iconos esenciales. No se encontró ninguna acción productiva que exija
pulsación prolongada ni que comunique el resultado únicamente mediante color.
Los títulos usan semántica de encabezado y los estados dinámicos relevantes
usan regiones vivas.

Comando ejecutado:

```powershell
.\gradlew.bat connectedDebugAndroidTest `
  "-Pandroid.testInstrumentationRunnerArguments.package=com.proyectonami.app.feature"
```

Informe local: `app/build/reports/androidTests/connected/debug/index.html`.

## Número de acciones

El recorrido principal de texto cumple el máximo de cuatro acciones desde
inicio: abrir conversación (1), enfocar el campo (2), escribir (3) y enviar
(4). El recorrido de audio cumple cinco: abrir conversación (1), grabar (2),
detener (3), escuchar/revisar (4) y enviar (5). Borrar o reintentar son rutas de
recuperación alternativas y no pasos obligatorios del camino de éxito.

## Hallazgos y correcciones

| Severidad | Hallazgo | Corrección | Estado |
|---|---|---|---|
| Alta | Perfil de error sin desplazamiento podía ocultar acciones al 200 %. | Se añadieron `safeDrawing`, desplazamiento vertical y prueba a 320/411 dp. | Cerrado |
| Media | Los errores de carga/avatar del perfil no declaraban anuncio dinámico. | Se añadieron regiones vivas asertivas y una prueba semántica. | Cerrado |
| Media | La auditoría anterior comprobaba propiedades aisladas, no el árbol completo. | Se habilitó Accessibility Test Framework en cuenta, inicio, contacto, chat, audio, perfil y ajustes. | Cerrado |

No quedan fallos críticos abiertos en la comprobación automática del 18 de
octubre de 2026.

## Protocolo manual con TalkBack

La persona revisora debe ejecutar cada fila al 100 % y 200 %, primero con
411 dp y después con 320 dp. Debe recorrer con deslizamientos derecha/izquierda,
activar con doble toque y registrar el resultado sin usar datos reales.

| Flujo | Qué debe anunciar y permitir | Autor/a | Segunda persona |
|---|---|---|---|
| Cuenta | título, nombre de cada campo, error, acción y confirmación | Pendiente | Pendiente |
| Inicio/contacto | título, vacío/carga/error, contacto, no leídos y navegación | Pendiente | Pendiente |
| Texto | historial en orden, campo, deshabilitado/válido, enviado y reintento | Pendiente | Pendiente |
| Audio | permiso, grabando, duración, escuchar/pausar, borrar, enviar y fallo | Pendiente | Pendiente |
| Perfil | título, campos, avatar/error, guardar, reintentar y volver | Pendiente | Pendiente |
| Ajustes | nombre, valor seleccionado, estado de lectura y acción | Pendiente | Pendiente |

Para cerrar la revisión, cada persona debe añadir nombre o identificador de
equipo, dispositivo/AVD, versión Android, ancho, escala, fecha, resultado y
hallazgos. La definición de hecho de T-39 no se considera completamente cerrada
hasta completar estas dos columnas; una ejecución automatizada no puede validar
la inteligibilidad auditiva ni sustituir la revisión independiente.

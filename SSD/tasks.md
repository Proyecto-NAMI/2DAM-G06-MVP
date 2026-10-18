# Tareas del MVP de NAMI

**Versión:** 1.0  
**Fecha:** 8 de octubre de 2026  
**Nota:** el reparto es una propuesta y debe confirmarse antes de empezar.

## Criterio general de “hecho”

Además del criterio específico, toda tarea debe cumplir `constitution.md`, incluir las pruebas aplicables, pasar las comprobaciones disponibles, usar datos ficticios y recibir revisión de otra persona.

## 1. Preparación

| ID | Requisito | Tarea | Depende de | Responsable | Hecho cuando... |
|---|---|---|---|---|---|
| T-01 | Todos | Revisar y aprobar los cinco documentos SDD | - | Todo el equipo | Las dudas son decisiones o pendientes con responsable y los cuatro integrantes confirman el alcance. |
| T-02 | RNF-07 | Crear proyecto Android, catálogo de versiones y entornos | T-01 | G06-xx (por confirmar) | Compila en limpio, abre una pantalla mínima y no contiene secretos. |
| T-03 | Todos | Configurar formato, lint, pruebas y CI | T-02 | G06-xx (por confirmar) | Los comandos reales sustituyen `PENDIENTE_*` de `AGENTS.md` y CI ejecuta build, análisis y pruebas. |
| T-04 | RNF-01 | Crear tema, tokens y componentes accesibles | T-02 | G06-xx (por confirmar) | Tipografía, colores, botones y campos admiten 200 % de texto, 48 dp y contraste exigido. |
| T-05 | RF-01, RF-03, RF-05, RF-06 | Configurar Firebase y emuladores | T-02 | G06-xx (por confirmar) | Auth, Firestore y Storage funcionan con datos ficticios y los archivos sensibles están ignorados. |

## 2. Cuenta y perfil

| ID | Requisito | Tarea | Depende de | Responsable | Hecho cuando... |
|---|---|---|---|---|---|
| T-06 | RF-01 | Implementar validación de nombre, apellidos y teléfono | T-02 | G06-xx (por confirmar) | Entradas válidas e inválidas tienen pruebas y mensajes comprensibles. |
| T-07 | RF-01, RNF-05 | Implementar autenticación telefónica y sesión | T-05, T-06 | G06-xx (por confirmar) | Un número ficticio puede verificar, reiniciar y conservar sesión; fallo y caducidad permiten reintento. |
| T-08 | RF-01, RNF-01 | Construir creación de cuenta accesible | T-04, T-06, T-07 | G06-xx (por confirmar) | Reproduce el flujo de cuenta, persiste el perfil tras verificar, anuncia errores y pasa pruebas UI de éxito, fallo y caducidad, incluido texto al 200 %. |
| T-09 | RF-02 | Implementar consulta y edición de perfil | T-07, T-04 | G06-xx (por confirmar) | Nombre y avatar opcional se muestran y actualizan; cambiar teléfono exige nueva verificación. |
| T-10 | RF-10 | Implementar cierre de sesión | T-07, T-15 | G06-xx (por confirmar) | La pantalla principal solicita confirmación; cancelar conserva la sesión y confirmar limpia estado y multimedia, revoca Auth y bloquea el retorno a pantallas privadas. |

## 3. Contactos y conversaciones

| ID | Requisito | Tarea | Depende de | Responsable | Hecho cuando... |
|---|---|---|---|---|---|
| T-11 | RF-03, RNF-05 | Diseñar vinculación privada por teléfono | T-05, T-07 | G06-xx (por confirmar) | No permite enumerar usuarios, normaliza teléfonos y supera pruebas de autorización. |
| T-12 | RF-03 | Implementar repositorio y alta de contactos | T-06, T-11 | G06-xx (por confirmar) | Crea contacto vinculado o pendiente, evita duplicados y tiene pruebas. |
| T-13 | RF-03 | Construir pantalla `Nuevo contacto` | T-04, T-12 | G06-xx (por confirmar) | Añadir, validar y detectar duplicados funciona con teclado, lector y texto al 200 %. |
| T-14 | RF-04 | Implementar consulta y orden de conversaciones | T-05, T-12 | G06-xx (por confirmar) | Devuelve solo conversaciones autorizadas, ordenadas y con contador correcto. |
| T-15 | RF-04 | Construir pantalla principal y estados | T-04, T-14 | G06-xx (por confirmar) | Nombre, avatar y no leídos aparecen; vacío, carga, error y navegación están probados. |

## 4. Chat de texto

| ID | Requisito | Tarea | Depende de | Responsable | Hecho cuando... |
|---|---|---|---|---|---|
| T-16 | RF-05, RNF-04 | Implementar repositorio idempotente de mensajes | T-05, T-14 | G06-xx (por confirmar) | Enviar y reintentar con el mismo ID produce una sola copia. |
| T-17 | RF-05 | Implementar historial y composición de texto | T-04, T-16 | G06-xx (por confirmar) | El historial se ordena, texto vacío no se envía y el borrador sobrevive a recreación. |
| T-18 | RF-08 | Implementar progreso, confirmación y error | T-16, T-17 | G06-xx (por confirmar) | Éxito anuncia “Mensaje enviado” y fallo permite reintento sin duplicado. |
| T-19 | RF-05, RNF-01 | Crear pruebas UI del flujo de texto | T-17, T-18 | G06-xx (por confirmar) | Cubren envío, vacío, fallo/reintento, foco y escala al 200 %. |

## 5. Notas de voz y lectura

| ID | Requisito | Tarea | Depende de | Responsable | Hecho cuando... |
|---|---|---|---|---|---|
| T-20 | RF-06 | Implementar permiso y estados de grabación | T-02 | G06-xx (por confirmar) | El grabador Android se conecta al chat: inicio con un toque, duración, parada, denegación e interrupción están probados. |
| T-21 | RF-06 | Implementar vista previa, borrado y límite | T-20 | G06-xx (por confirmar) | Se puede escuchar, borrar o entregar la nota al envío; 120 s y 5 MiB detienen y eliminan el temporal con aviso. |
| T-22 | RF-06, RNF-05 | Implementar subida segura y reintento de audio | T-05, T-16, T-21 | G06-xx (por confirmar) | Solo participantes acceden; fallo conserva copia temporal para reintentar o borrar. |
| T-23 | RF-07 | Implementar reproducción con pausa | T-21, T-22 | G06-xx (por confirmar) | El historial muestra audio, solo uno suena, el mismo control pausa/reanuda y los errores son recuperables. |
| T-24 | RF-07, RF-09 | Implementar texto a voz y preferencia | T-04, T-17 | G06-xx (por confirmar) | Puede leer un mensaje, la preferencia persiste y la ausencia del motor no bloquea el chat. |

## 6. Accesibilidad, seguridad y validación

| ID | Requisito | Tarea | Depende de | Responsable | Hecho cuando... |
|---|---|---|---|---|---|
| T-25 | RF-09, RNF-01 | Implementar tamaño de texto y ajustes | T-04 | G06-xx (por confirmar) | Persiste, se aplica a todas las pantallas y no recorta al 200 %. |
| T-26 | RNF-05, RNF-06 | Probar reglas de Firestore y Storage | T-11, T-16, T-22 | G06-xx (por confirmar) | El emulador permite a participantes y deniega a terceros en perfiles, mensajes y audios. |
| T-27 | RNF-03, RNF-04, RNF-09 | Probar rendimiento, conexión e interrupciones | T-18, T-22, T-23 | G06-xx (por confirmar) | Hay mediciones, no hay duplicados y texto/grabación quedan seguros tras interrupciones. |
| T-28 | RNF-01, RNF-02 | Auditar accesibilidad y tareas | T-13, T-19, T-23, T-25 | G06-xx (por confirmar) | Se prueban TalkBack, 200 %, dos pantallas y perfiles representativos; se corrigen fallos críticos. |
| T-29 | Todos | Revisar trazabilidad y regresión | T-26, T-27, T-28 | Todo el equipo | Cada RF/RNF tiene prueba o evidencia, no está activo lo excluido y pasa la suite. |
| T-30 | Todos | Preparar piloto y entrega | T-29 | Todo el equipo | APK, guía, datos ficticios, limitaciones y evidencias están listos y revisados. |
| T-31 | RF-03, RF-04, RF-05; RNF-04, RNF-05 | Crear y abrir una conversación desde un contacto vinculado | T-11 a T-17 | G06-xx (por confirmar) | Una operación autenticada crea o recupera una única conversación para un contacto `linked`; `pending` y terceros quedan denegados y el flujo funciona desde Emulator Suite vacío. |
| T-32 | RF-10 | Integrar cierre seguro en conversaciones | T-10, T-15 | G06-xx (por confirmar) | La pantalla productiva confirma el cierre, limpia multimedia y evita volver a rutas privadas. |
| T-33 | RF-06; RNF-01, RNF-09 | Integrar grabación y vista previa en el chat | T-20, T-21, T-17 | G06-xx (por confirmar) | Una pulsación inicia una grabación privada con permiso, límites, escucha, borrado y entrega al envío. |
| T-34 | RF-06, RF-08; RNF-04, RNF-05, RNF-09 | Completar subida, reglas y reintento de notas de voz | T-16, T-22, T-33 | G06-xx (por confirmar) | El UUID, la ruta y los metadatos son idempotentes; fallo conserva el temporal, reintento no duplica y terceros quedan denegados. |
| T-35 | RF-07 | Integrar reproducción de notas de voz con pausa y errores recuperables | T-23, T-34 | G06-xx (por confirmar) | El historial muestra audio, solo uno suena, el mismo control pausa/reanuda y los errores son recuperables. |
| T-36 | RF-07, RF-09; RNF-01 | Integrar texto a voz real y la preferencia de lectura | T-24, T-25 | G06-xx (por confirmar) | El historial usa el motor TTS del sistema, respeta la preferencia persistente, coordina multimedia y conserva visible el texto ante errores. |
| T-37 | RF-09; RNF-01 | Crear la pantalla de ajustes de accesibilidad | T-25, T-36 | G06-xx (por confirmar) | Inicio abre Ajustes; tamaño 100/150/200 % y lectura en voz alta se aplican al instante, persisten y son accesibles en 320/411 dp. |

## 7. Matriz de trazabilidad

| Requisito | Tareas principales |
|---|---|
| RF-01 | T-06, T-07, T-08 |
| RF-02 | T-09 |
| RF-03 | T-11, T-12, T-13, T-31 |
| RF-04 | T-14, T-15, T-31 |
| RF-05 | T-16, T-17, T-19, T-31 |
| RF-06 | T-20, T-21, T-22, T-33, T-34 |
| RF-07 | T-23, T-24, T-35, T-36 |
| RF-08 | T-18, T-34 |
| RF-09 | T-24, T-25, T-36, T-37 |
| RF-10 | T-10, T-32 |
| RNF-01/RNF-02 | T-04, T-19, T-25, T-28, T-36, T-37 |
| RNF-03/RNF-04/RNF-09 | T-16, T-18, T-27, T-31, T-33, T-34 |
| RNF-05/RNF-06 | T-11, T-22, T-26, T-31, T-34 |
| RNF-07/RNF-08 | T-02, T-03, T-29 |

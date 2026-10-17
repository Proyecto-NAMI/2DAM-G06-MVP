# Plan técnico del MVP de NAMI

**Versión:** 1.0  
**Fecha:** 8 de octubre de 2026  
**Estado:** propuesta para revisión del equipo

> Estado T-30: preparado para piloto y entrega. El acceso docente, la auditoria manual con TalkBack y la segunda revision se declaran como pasos externos en `VALIDACION.md`.

## 1. Decisiones técnicas

| Área | Elección | Justificación |
|---|---|---|
| Cliente | Android nativo con Kotlin y Jetpack Compose | Encaja con 2.º DAM, permite una UI móvil accesible y reduce duplicación. |
| Diseño | Material 3 adaptado a Figma | Aporta componentes accesibles, semántica y escalado. |
| Arquitectura | Presentación, dominio y datos; ViewModel y repositorios | Separa UI, reglas y Firebase y facilita las pruebas. |
| Asincronía | Coroutines y Flow | Modelo idiomático para estado, red, grabación y reproducción. |
| Identidad | Firebase Authentication por teléfono | Se alinea con Figma y evita contraseñas complejas. |
| Datos | Cloud Firestore | Sincronización de conversaciones y reglas de acceso. |
| Audio | Firebase Storage | Adecuado para binarios; Firestore guarda solo metadatos. |
| Preferencias | DataStore | Persistencia local simple de accesibilidad. |
| Pruebas | JUnit, fakes, Compose UI Test y Firebase Emulator Suite | Cubre dominio, UI, integración y autorización con datos ficticios. |

Las versiones se fijan en el catálogo de versiones de Gradle. T-02 establece `minSdk 26`, `targetSdk 36` y `compileSdk 37`; la compatibilidad mínima deberá validarse en los dispositivos del piloto antes de cerrar RNF-07.

T-02 usa el soporte Kotlin integrado en AGP 9.4.1, sin aplicar `org.jetbrains.kotlin.android`, que entra en conflicto con este soporte. Se mantiene el plugin del compilador de Compose y el objetivo JVM 17.

T-03 añade Spotless con ktlint para comprobar Kotlin y scripts Gradle Kotlin DSL en CI, y Kover para generar y verificar cobertura de pruebas en código Kotlin/Android. Se justifican porque proporcionan tareas Gradle reproducibles y permiten imponer el 70 % de líneas en cada paquete de dominio (RNF-08); al inicio, el filtro no encuentra paquetes de dominio en T-02 y empezará a verificar cuando se incorporen. Kover recopila cobertura de pruebas unitarias JVM, no de pruebas instrumentadas ejecutadas en el dispositivo. Android Lint cubre análisis estático, JUnit las pruebas locales y Compose UI Test las pruebas instrumentadas. Firebase Emulator Suite queda fuera de estos comandos hasta T-05, que debe aportar `firebase.json` y reglas.

T-06 sitúa la validación de nombre, apellidos y teléfono en `core.domain.validation` con Kotlin puro, sin dependencias de Android ni de Firebase: los nombres y apellidos se recortan, se limitan a 50 caracteres Unicode y rechazan cifras y símbolos, y el teléfono se normaliza a E.164 comprobando prefijo internacional, entre 7 y 15 cifras y una primera cifra distinta de 0. No se añade ninguna librería de numeración telefónica, así que no se validan los prefijos ni las longitudes propias de cada país; adoptar `libphonenumber` exigiría justificarlo antes en este documento.

T-07 amplía el repositorio de autenticación con verificación telefónica Firebase, observación de la sesión y reenvío del código. Los errores del SDK se traducen a motivos de dominio; el reto de verificación y el token de reenvío se conservan tras un código incorrecto para permitir el reintento. `PhoneAuthViewModel` mantiene el nombre, los apellidos y el teléfono mientras presenta errores o solicita un nuevo código; la sesión comienza en estado `Restoring` hasta recibir el estado persistido de Firebase. Firebase Auth mantiene la sesión localmente entre aperturas de la aplicación; T-08 conectará este estado con la pantalla de creación de cuenta.

T-08 conecta el estado de autenticación a una pantalla Compose de creación de cuenta. El teléfono se presenta con `+34` editable y se valida con las reglas de T-06; tras verificarlo se guarda el perfil mínimo (`displayName`, `surname` y `phoneE164`) en el documento privado `users/{uid}` antes de mostrar el estado de cuenta lista. Un fallo al guardar conserva el formulario y permite reintentar el guardado sin volver a solicitar el código. La pantalla usa recursos de cadenas, campos etiquetados, errores semánticos anunciables, acciones de teclado y desplazamiento vertical para texto ampliado.

T-09 añade `LoadProfileUseCase`, `UpdateProfileUseCase` y `ProfileViewModel`. El perfil privado se carga desde `users/{uid}` y permite editar nombre, apellidos y `avatarPath` opcional; los nombres y el teléfono se normalizan con T-06. Un cambio de teléfono solicita y verifica un nuevo código mediante `updatePhoneNumber`, conservando el UID actual, antes de persistir el perfil. La pantalla cubre carga, error, guardado y confirmación con controles accesibles.

T-12 y T-13 resuelven el alta de contactos con `core.domain.contacts` y `FirebaseContactsRepository`, que no escribe en Firestore: llama a la función `addContact` de T-11. El cliente solo puede leer sus propios contactos (`users/{uid}/contacts`), las reglas deniegan cualquier escritura directa y la vinculación la decide el servidor. El identificador del contacto es el SHA-256 del teléfono normalizado, así que el mismo teléfono apunta siempre al mismo documento y el alta es idempotente; el duplicado se informa con el contacto existente. Nombre, apellidos y teléfono se validan con T-06 antes de salir del dispositivo. La comprobación de si el teléfono tiene cuenta NAMI ocurre dentro de la función, con sesión de teléfono verificado, App Check en producción y cuota por UID, y nunca desde el cliente: no existe ninguna API de directorio.

T-13 construye la pantalla `Nuevo contacto`, que se abre desde el «+» de la pantalla principal (T-15). `feature.contacts.presentation` añade `AddContactViewModel` (estado inmutable en `StateFlow`), la validación conjunta de los tres campos y un reductor puro del resultado del alta, y `AddContactScreen` con su `AddContactRoute`. La pantalla usa los tokens y componentes accesibles de T-04, reutiliza las etiquetas y el prefijo `+34` inicial editable de la creación de cuenta (duda 7 de `spec.md`), valida los tres campos en un solo intento para no descubrir un error por envío, informa del duplicado nombrando el contacto existente sin escribir nada en ese caso y anuncia errores, duplicado y confirmación como región viva. Los mensajes de validación de T-06 se comparten con T-08 en `ui/ValidationMessages.kt`.

T-31 conecta el alta con la mensajería mediante la callable `openConversation`. El cliente envía el ID opaco de un contacto propio; el servidor comprueba que el documento fue creado por `addContact`, que su estado es `linked` y que contiene otro UID válido. El ID de conversación es el SHA-256 estable de la pareja ordenada, por lo que ambos participantes convergen en un único documento. La creación usa Admin SDK y una transacción; las reglas deniegan toda creación directa desde clientes. La pantalla ofrece «Abrir conversación» solo para contactos vinculados y navega al chat cuando la callable devuelve el ID.

La pantalla distingue los tres desenlaces que devuelve la función: si el servidor confirma que el teléfono tiene cuenta NAMI, la confirmación lo dice («ya usa NAMI»); si no la tiene, el contacto queda `pending` y solo se confirma que se ha guardado; y si la comprobación no está disponible (cuota agotada o servicio caído), avisa de que **no se ha guardado nada** y permite reintentar. Se validan los tres campos en el cliente para poder señalarlos todos, pero quien decide sigue siendo el servidor: el caso de uso valida de nuevo y el repositorio nunca escribe Firestore.

### Dependencias Firebase de T-05

- **Necesidad:** Firebase Authentication permite implementar la identidad por teléfono definida en RF-01; Cloud Firestore almacena perfiles, contactos y mensajes; Firebase Storage conserva las notas de voz fuera de Firestore. Firebase BoM mantiene versiones compatibles entre los tres SDK.
- **Mantenimiento:** son SDK oficiales mantenidos por Google. La versión de la BoM queda fijada en el catálogo Gradle y cualquier actualización deberá pasar compilación, lint, pruebas unitarias, pruebas instrumentadas y Emulator Suite antes de incorporarse.
- **Licencia:** el repositorio abierto de Firebase Android declara Apache License 2.0 para sus componentes, entre ellos Firestore y Storage. Firebase Authentication no forma parte de ese repositorio abierto; antes de distribuir el piloto se revisarán y registrarán en los avisos de terceros la licencia y las condiciones declaradas por su artefacto y sus dependencias transitivas.
- **Privacidad:** `debug` utiliza exclusivamente el proyecto ficticio `demo-nami` y Emulator Suite. Las reglas exigen autenticación y pertenencia a la conversación, los archivos sensibles permanecen fuera de Git y los logs no incluyen teléfonos, mensajes, tokens ni rutas privadas de audio.

## 2. Arquitectura

```text
Jetpack Compose
      |
ViewModels + UiState
      |
Casos de uso de dominio
      |
Interfaces de repositorio
      |
+------------------+------------------+------------------+
| Firebase Auth    | Firestore        | Storage          |
| cuenta/sesión    | datos/mensajes   | notas de voz     |
+------------------+------------------+------------------+
      |
DataStore (preferencias locales)
```

### Paquetes principales

- `core.designsystem`: colores, tipografía, dimensiones y componentes accesibles.
- `core.domain`: reglas y validaciones compartidas sin dependencias de Android ni de Firebase.
- `core.model`: modelos compartidos sin dependencias de Firebase.
- `core.data`: configuración, mapeos, resultados y utilidades.
- `feature.auth`, `feature.profile`, `feature.contacts`.
- `feature.conversations`, `feature.chat`, `feature.audio`, `feature.settings`.

El MVP puede conservar un único módulo Gradle `app`. Solo se separarán módulos si el tiempo y la compilación lo justifican.

## 3. Flujo de estado

- Cada pantalla expone un `UiState` inmutable desde su `ViewModel`.
- Las acciones de usuario llegan como eventos explícitos.
- Los casos de uso contienen validación y reglas independientes de Android cuando sea posible.
- Los repositorios traducen Firebase a modelos y errores de dominio.
- Cada mensaje recibe un UUID antes del envío y reutiliza ese ID al reintentar.
- T-16 implementa `core.domain.messages` y `FirebaseTextMessagesRepository`.
  `OutgoingTextMessage.create` genera el UUID una sola vez; el mensaje conserva
  conversación, remitente y texto inmutables y rechaza texto vacío. El caso de
  uso emite pendiente y, tras la respuesta del servidor, enviado o fallido con
  motivo. El consumidor debe conservar el mismo mensaje para reintentar; no
  debe invocar de nuevo la fábrica. T-17/T-18 conectarán estos estados con la UI
  y la conservación del borrador.
- El repositorio usa una transacción sobre
  `conversations/{conversationId}/messages/{clientMessageId}`: crea el mensaje,
  fecha de servidor, actividad y contador del destinatario de forma atómica.
  Si el documento ya existe con el mismo contenido, confirma sin modificarlo
  ni incrementar de nuevo el contador. Reutilizar el ID con otro contenido
  devuelve conflicto. Los reintentos concurrentes quedan serializados por
  Firestore. Una transacción sin red falla explícitamente; no se presenta una
  escritura local en caché como enviada ni se implementa una cola offline.
  Cancelar la espera no garantiza cancelar el commit remoto: se debe reintentar
  con el mismo UUID para resolver una confirmación perdida.
- T-17 añade la observación ordenada de `conversations/{id}/messages`, con
  estados inmutables de carga, vacío, cargado y error. `TextConversationViewModel`
  conserva el borrador en `SavedStateHandle`, rechaza texto vacío o solo de
  espacios y reutiliza `SendTextMessageUseCase` para emitir pendiente, enviado o
  fallido. La pantalla queda preparada para conectarse a la navegación de T-15
  sin inventar una conversación ni datos de usuario.
- T-18 conserva el `clientMessageId` de un envío fallido al reintentar, bloquea
  acciones mientras el mensaje está pendiente y expone una confirmación
  accesible (`Mensaje enviado`) cuando Firestore confirma la persistencia.
- T-19 conecta la lista de conversaciones con el compositor: abrir una conversación
  requiere una acción y reutiliza los estados de T-18 para enviar o reintentar sin
  duplicar mensajes.
- T-20/T-21 fijan el límite de las notas de voz en 120 segundos y 5 MiB; la
  grabación se detiene de forma segura al alcanzar cualquiera de los dos límites.
- T-22 sube el contenido a Storage bajo `conversations/{id}/audio/{messageId}` y
  guarda únicamente metadatos en Firestore; los reintentos reutilizan el mismo ID.
- T-23 centraliza reproducción y pausa en un único coordinador multimedia; cerrar
  sesión usa el registro común para detener cualquier sesión activa.
- T-24 separa el motor TTS de la interfaz, de modo que un motor o idioma no
  disponible produce un estado explicable sin ocultar el mensaje.
- T-25 persiste escala de texto y lectura en voz alta en DataStore y aplica la
  escala mediante la tipografía compartida del tema.
- Un único controlador coordina grabación y reproducción para impedir que ambas estén activas a la vez.
- T-10 solicita confirmación antes del cierre desde un único diálogo propiedad de `NamiApp`, usado tanto por la ruta provisional como por la pantalla productiva de conversaciones. Al confirmarlo elimina las rutas privadas de chat, perfil y contactos, borra el estado de autenticación en memoria, cierra grabación, reproducción y texto a voz registrados en `MediaSessionCoordinator`, y revoca Firebase Auth. Los fallos de parada no impiden el cierre y se notifican en el acceso.

## 4. Modelo de datos propuesto

### `users/{userId}`

```text
displayName: string
surname: string
phoneE164: string
avatarPath: string?
createdAt: timestamp
updatedAt: timestamp
```

### `users/{userId}/contacts/{contactId}`

```text
linkedUserId: string?
displayName: string
surname: string
phoneE164: string
status: "linked" | "pending"
createdAt: timestamp
```

`phoneE164` es el teléfono introducido por la persona propietaria del contacto.
No existe un índice de teléfonos legible por clientes: los perfiles solo se
pueden leer por su propio UID. El cliente no consulta el directorio de usuarios.
La callable `addContact` de T-11 exige sesión con teléfono verificado y App Check
en producción, valida E.164, limita cada UID a 5 consultas por minuto y 20 por
día, consulta solo ese teléfono mediante Firebase Admin Auth y crea el contacto
en una transacción. El ID es el SHA-256 del E.164; la función devuelve solo el
contacto añadido/ya existente, no ofrece una API de directorio. Las reglas
deniegan toda escritura cliente en `contacts` y en `contactLookupLimits`. En
debug, App Check solo se omite dentro de Functions Emulator; release usa Play Integrity.

T-12 y T-13 usan esa función a través de `core.domain.contacts` y
`FirebaseContactsRepository`; el cliente no escribe contactos. `contactId` no es
aleatorio: la función lo deriva del teléfono normalizado con SHA-256 truncado,
de modo que el alta es idempotente y el teléfono no aparece en la ruta del
documento. El contacto se guarda con `status: "linked"` y `linkedUserId` cuando
el servidor encuentra la cuenta, y con `status: "pending"` y `linkedUserId` nulo
cuando no la encuentra. Si la comprobación no está disponible (cuota o servicio),
la función no escribe nada y el cliente muestra un aviso recuperable; el
repositorio traduce `RESOURCE_EXHAUSTED`, `UNAVAILABLE` y `DEADLINE_EXCEEDED` a
`LookupUnavailable` para no confundirlo con un alta correcta.

### `conversations/{conversationId}`

```text
participantIds: [string, string]
participantDetails: map<uid, {displayName: string, surname: string, avatarPath: string?}>
unreadCountByUser: map<uid, integer>
lastMessageType: "text" | "audio"?
lastMessagePreview: string?
lastActivityAt: timestamp
```

T-14 consulta solo documentos que contienen el UID autenticado, ordena por
`lastActivityAt` descendente y lee el nombre/avatar del otro participante junto
al contador del usuario actual. Las reglas limitan la lectura a participantes;
el índice compuesto de `participantIds` y `lastActivityAt` está declarado en
`firestore.indexes.json`.

### `conversations/{conversationId}/messages/{messageId}`

```text
clientMessageId: string
senderId: string
type: "text" | "audio"
text: string?
audioPath: string?
audioDurationMs: number?
createdAt: timestamp
status: "sent"
```

### Preferencias locales

```text
textScale: float
textToSpeechEnabled: boolean
soundConfirmationEnabled: boolean
```

El audio no se almacenará en Firestore. Las reglas de Firestore y Storage comprobarán que `request.auth.uid` pertenece a `participantIds`. La búsqueda por teléfono deberá impedir que se enumere el directorio.

## 5. Relación con Figma

| Pantalla o estado | Implementación MVP | Requisitos |
|---|---|---|
| `Creación de cuenta` | Formulario, verificación telefónica y alta de perfil. | RF-01 |
| `Conversaciones y pantalla principal` | Lista, avatar, nombre y no leídos. | RF-04 |
| `Nuevo contacto` | Alta manual y control de duplicados. | RF-03 |
| Chats con Carlos, Paula y Pedro | Una plantilla reutilizable de chat uno a uno. | RF-05, RF-06, RF-07 |
| Estados `escribiendo` | Estado local del campo; no indicador remoto. | RF-05 |
| Estados `mensaje enviado` | Confirmación visible y sonora tras persistir. | RF-08 |
| `Modificación y opciones` | Tamaño de texto y texto a voz; un clic es regla transversal. | RF-09 |
| `Foto de perfil` | Consulta y edición básica. | RF-02 |
| Cámara, teléfono y vídeo | Ocultos o deshabilitados con explicación. | Fuera de alcance |

La implementación será adaptable y no copiará tamaños absolutos de 390 x 844.

### El prototipo es un boceto, no una especificación de estilo

Las capturas de [`Prototype_Reference/`](../Prototype_Reference) y el archivo de Figma son un prototipo de baja fidelidad. Se usan como referencia de **contenido y flujo** (qué pantallas existen, qué datos pide cada una y en qué orden), no de estilo ni de interacción:

- La implementación debe **mejorar el estilo visual y la forma de interactuar**. El boceto no fija colores, tipografías, iconos, medidas ni gestos.
- Los colores, tipografías, espaciados y controles salen de los tokens y componentes accesibles de T-04, con las garantías de RNF-01 (objetivo táctil de 48 dp, texto hasta el 200 %, semántica y contraste), no del aspecto del boceto.
- Los iconos de cámara, llamada y vídeo que aparecen en el chat son exploraciones visuales y no se activan (`spec.md` §3).
- Si el boceto contradice `constitution.md`, `spec.md` o este plan, se aplica el orden de prioridad de `constitution.md` y se anota la diferencia.

## 6. Seguridad y privacidad

- Autenticación obligatoria para datos remotos.
- Reglas de Firestore y Storage probadas con casos permitidos y denegados.
- Validación de participantes en backend; el cliente no será la frontera de seguridad.
- Teléfonos normalizados a E.164 y perfiles privados por propietario. El cliente
  no puede consultar el directorio; cualquier búsqueda exacta futura debe estar
  mediada por backend autenticado y limitada.
- Entornos separados para desarrollo/pruebas y piloto.
- Datos ficticios en emuladores, capturas y repositorio.
- Logs sin mensajes, URLs firmadas, tokens o teléfonos completos.
- Retención y eliminación resueltas antes de usar datos reales.

## 7. Accesibilidad

- Tokens de tamaño y espaciado centralizados.
- Controles principales de 48 dp o más.
- Semántica y descripciones para iconos.
- Foco lógico y estados anunciados por servicios de accesibilidad.
- Contraste verificado automática y manualmente.
- Pruebas al 200 %, con TalkBack y una mano en dispositivo físico.
- Confirmación visual y sonora; vibración como mejora posterior.

## 8. Estrategia de pruebas

### Unitarias

- Perfil y teléfono; contactos duplicados.
- Envío idempotente y reintento.
- Estados de grabación/reproducción.
- Mapeos entre Firebase y dominio.

### Integración

- Autenticación y persistencia de sesión.
- Lectura, escritura y orden de mensajes.
- Subida y acceso autorizado a audio.
- Reglas que permiten participantes y rechazan terceros.
- Recuperación de conexión sin duplicados.
- Alta de contactos a través de la función `addContact`: vinculación con una cuenta
  existente, contacto pendiente cuando el teléfono no tiene cuenta, duplicado sin
  segunda escritura y denegación de cualquier escritura directa desde el cliente.
- Las funciones se prueban con `npm test` dentro de `functions` (cuota por minuto
  y por día, E.164, normalización de nombres e identificadores) y el Emulator
  Suite de CI arranca auth, firestore, storage **y** functions. Un paso posterior
  falla si alguna prueba instrumentada se ha omitido, para que las reglas y la
  función no vuelvan a quedarse sin ejecutar en silencio.

### Interfaz

- Crear cuenta, añadir contacto, abrir conversación y enviar texto.
- Grabar, revisar, eliminar y enviar audio.
- Error y reintento.
- Tamaño de texto y texto a voz simulado.
- Etiquetas, foco y ausencia de recortes al 200 %.

### Validación humana

- Revisión por otra persona del equipo.
- Prueba de tareas con perfiles representativos y datos ficticios.
- Registro de bloqueos, ayuda solicitada, tiempo y comprensión.

## 9. Entrega incremental

1. Base, navegación, tema y datos de demostración.
2. Cuenta, perfil y contactos.
3. Lista e historial de conversaciones.
4. Texto con confirmación, fallo y reintento.
5. Audio con vista previa, envío y reproducción.
6. Ajustes y texto a voz.
7. Reglas, pruebas, auditoría y piloto.

## 10. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| La verificación SMS bloquea desarrollo | Usar números ficticios de Firebase y repositorios sustituibles. |
| Reglas exponen conversaciones | Probar en emulador con participantes y cuentas atacantes. |
| Audio consume tiempo o datos | Fijar límite, comprimir y mostrar progreso/reintento. |
| Texto grande rompe pantallas | UI adaptable, scroll y pruebas al 200 % en dos tamaños. |
| Crece el alcance por Figma | Aplicar las exclusiones de `spec.md`. |
| Búsqueda por teléfono expone datos | Prototipar en emulador y revisar privacidad antes del piloto. |

## 11. Revisión requerida

Antes de implementar, las cuatro personas deben confirmar el alcance, versión mínima de Android, límite de audio, búsqueda privada por teléfono, retención/borrado y reparto de `tasks.md`.

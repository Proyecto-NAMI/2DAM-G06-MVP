# Plan técnico del MVP de NAMI

**Versión:** 1.0  
**Fecha:** 8 de octubre de 2026  
**Estado:** propuesta para revisión del equipo

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

T-12 añade `feature.contacts.domain` con el modelo `Contact`, el puerto `ContactRepository` y `AddContactUseCase`, y su implementación `FirebaseContactRepository` sobre `users/{uid}/contacts`. El identificador del contacto se deriva del teléfono normalizado (`ContactIdFactory`), de modo que el alta es idempotente: el mismo teléfono apunta siempre al mismo documento y no puede generar dos contactos. El duplicado se detecta leyendo ese documento antes de escribir y se informa con el contacto existente para poder indicar cuál es. Nombre, apellidos y teléfono se validan con T-06 y se guardan normalizados. La vinculación con una cuenta existente depende del puerto `ContactAccountResolver`; el MVP inyecta `DisabledContactAccountResolver`, que responde `Unavailable`, así que los contactos se guardan como `pending` y el cliente sigue sin consultar el directorio de personas usuarias, como exige T-11. Activar la vinculación exigirá la operación de backend autenticada, con App Check y cuota por UID, descrita en `FIREBASE.md`.

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
- Un único controlador coordina grabación y reproducción para impedir que ambas estén activas a la vez.
- T-10 solicita confirmación antes del cierre; al confirmarlo borra el estado de autenticación en memoria, cierra los recursos de audio registrados en `MediaSessionCoordinator` y revoca Firebase Auth. Cada grabador o reproductor deberá registrar su sesión activa para que el coordinador pueda detenerla; los fallos de parada no impiden el cierre y se notifican en el acceso.

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
La consulta exacta de una cuenta, cuando se integre en T-12, deberá pasar por
una operación de backend autenticada y limitada; una búsqueda de directorio no
se habilita mediante reglas de Firestore.

T-12 implementa el alta sin activar esa consulta. `contactId` no es aleatorio:
`ContactIdFactory` lo deriva del teléfono normalizado con SHA-256 truncado, para
que el alta sea idempotente y para no escribir el teléfono en la ruta del
documento. La vinculación queda como puerto (`ContactAccountResolver`) y el MVP
guarda siempre `status: "pending"` con `linkedUserId` nulo; activar la
vinculación requiere la operación de backend anterior y su cuota.

### `conversations/{conversationId}`

```text
participantIds: [string, string]
lastMessageType: "text" | "audio"?
lastMessagePreview: string?
lastActivityAt: timestamp
```

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

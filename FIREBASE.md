# Firebase y Emulator Suite (T-05)

Esta configuración cubre **T-05**, los requisitos **RF-01, RF-03, RF-05, RF-06** y los requisitos no funcionales **RNF-05 y RNF-06**. El flujo completo de autenticación telefónica pertenece a T-07; aquí se prepara el SDK de Authentication y su emulador.

## Entornos

- `debug` usa por defecto el proyecto ficticio `demo-nami` y conecta Auth, Firestore y Storage al equipo anfitrión mediante `10.0.2.2`.
- En CI, Firebase Emulator Suite se enlaza temporalmente a `0.0.0.0` para que el emulador Android alojado en la máquina pueda acceder a los puertos del host; la configuración temporal se elimina al terminar y no cambia el enlace local por defecto.
- `release` no usa emuladores y exige configuración local. Si faltan valores, la aplicación falla de forma explícita al solicitar el contenedor Firebase, en vez de conectar accidentalmente con un proyecto.
- Solo `debug` permite tráfico HTTP sin cifrar, y únicamente hacia `10.0.2.2`, `localhost` y `127.0.0.1` (`app/src/debug/res/xml/network_security_config.xml`), porque los emuladores de Auth y Storage no usan HTTPS. `release` mantiene el bloqueo por defecto.
- `google-services.json`, `.firebaserc`, `firebase.local.properties`, cuentas de servicio, exportaciones y logs están ignorados.

No se debe usar un identificador de proyecto real con los emuladores. La clase `FirebaseEnvironment` exige el prefijo `demo-` para reducir el riesgo de escribir en servicios reales.

## Preparación local

1. Instala Node.js 22, Firebase CLI y comprueba que Java 21 o posterior esté disponible.
2. En `functions`, instala dependencias y valida la función:

  ```powershell
  npm install
  npm test
  npm run build
  ```

3. Copia `firebase.local.properties.example` como `firebase.local.properties` si necesitas cambiar host o puertos.
4. Copia `.firebaserc.example` como `.firebaserc`, o indica siempre `--project demo-nami`.
5. Inicia los servicios:

   ```powershell
  firebase emulators:start --project demo-nami --only auth,firestore,functions,storage
   ```

5. En otro proceso, compila y ejecuta la prueba de integración en un emulador Android:

   ```powershell
   .\gradlew.bat connectedDebugAndroidTest
   ```

La prueba `FirebaseEmulatorSmokeTest` usa exclusivamente cuentas efímeras y datos ficticios. Además de Auth, Firestore y Storage, verifica las callables `addContact` y `openConversation`: pending, duplicado, cuota, creación idempotente de una conversación vinculada, envío de texto y rechazo de escrituras directas desde cliente. El código SMS de prueba se obtiene en memoria desde `http://<host>:9099/emulator/v1/projects/demo-nami/verificationCodes`; no se registra ni persiste. El Emulator Suite no envía SMS reales. Si falta cualquiera de los emuladores requeridos, las pruebas se omiten.

CI arranca `auth`, `firestore`, `storage` y `functions`, ejecuta `npm test` dentro de `functions` (cuota, E.164, normalización e identificadores) y comprueba después que ninguna prueba instrumentada se haya omitido: si el Emulator Suite no levanta todos los emuladores necesarios, el trabajo falla en vez de dar por buena una suite vacía.

## Inyección

`FirebaseRepositoryContainer` construye `AuthRepository`, `FirestoreRepository` y `AudioStorageRepository` a partir de `FirebaseServices`. `AuthRepository` expone la sesión como un flujo, solicita y reenvía códigos, y traduce fallos de Firebase a motivos estables para que la presentación muestre mensajes comprensibles. `PhoneAuthViewModel` conserva nombre, apellidos y teléfono entre errores y reenvíos, y limpia el campo de código cuando se envía uno nuevo. Firebase Auth conserva la sesión local entre aperturas de la aplicación; la prueba instrumentada confirma que el repositorio puede volver a observarla tras verificarse. La comprobación manual tras forzar el cierre y abrir de nuevo la aplicación queda pendiente para T-08, cuando exista navegación basada en la sesión. Las pantallas y casos de uso reciben estas interfaces; no acceden directamente a Firebase.

## Seguridad y privacidad

- Firestore exige autenticación y limita perfiles y lectura de contactos a su
  propietario. El cliente no puede crear, modificar ni borrar contactos: solo
  `addContact` escribe mediante Admin SDK.
- Un cliente no puede listar perfiles ni consultar `users` por teléfono: las
  reglas solo permiten leer `users/{uid}` cuando el UID coincide con la sesión.
  Los documentos solo pueden incluir `phoneE164` si coincide con el teléfono
  verificado en la credencial Firebase; el teléfono es inmutable desde el
  cliente. El repositorio valida y normaliza a E.164 antes de guardarlo.
- **Modelo de amenaza de T-11:** un usuario autenticado malicioso podría probar
  números probables, consultar repetidamente el directorio o leer un perfil
  ajeno; también podría intentar registrar un teléfono inventado para envenenar
  una búsqueda. Los activos son el teléfono, la existencia de una cuenta y los
  datos del perfil. Las reglas deniegan lecturas cruzadas, consultas de colección
  y teléfonos no verificados; los contactos y teléfonos pendientes quedan
  privados bajo el UID propietario.
- No existe consulta de teléfonos desde el cliente. `addContact` exige una
  sesión telefónica verificada y App Check en producción, valida E.164, aplica
  5 consultas por minuto y 20 por día por UID, resuelve solo el teléfono
  solicitado y devuelve el contacto creado, nunca un endpoint de directorio.
  Usa hash SHA-256 como ID determinista y una transacción para impedir
  duplicados. El emulador desactiva App Check únicamente dentro del proceso local;
  el cliente release usa Play Integrity.
- Conversaciones, mensajes y audios exigen pertenecer a `participantIds`.
- Los clientes no pueden crear conversaciones directamente. `openConversation`
  valida un contacto propio `linked`, crea o recupera en transacción el ID estable
  de la pareja y devuelve solo ese identificador. Un contacto `pending` o un ID
  ajeno se rechaza antes de escribir.
- Storage acepta únicamente contenido con tipo `audio/*` en la ruta de una conversación autorizada.
- No se registran teléfonos, tokens, mensajes ni rutas de audio.
- Los valores del ejemplo son ficticios y no conceden acceso a recursos reales.

Las pruebas exhaustivas de todas las operaciones y combinaciones de participantes corresponden a T-26 y siguen pendientes.

## Envío de texto idempotente (T-16, RF-05, RNF-04)

El contenedor expone `TextMessagesRepository`; `SendTextMessageUseCase` emite
`Pending`, `Sent` o `Failed` con el mensaje original y un motivo de error.
El UUID se genera en `OutgoingTextMessage.create` antes de enviar. Los reintentos
deben reutilizar ese objeto, también después de cancelar una espera o perder una
confirmación. Crear otro objeto con la fábrica representa otro mensaje, aunque
el texto coincida. No se registra el contenido.

La transacción Firestore crea el documento con ese UUID como ID y
`clientMessageId`, junto con `senderId`, `type: "text"`, `text`, `createdAt` de
servidor y `status: "sent"`. Actualiza actividad y no leídos del destinatario
una sola vez. Un documento existente e idéntico devuelve éxito sin escribir;
otro contenido con el mismo UUID devuelve `ID_CONFLICT`. Las reglas exigen
participación, remitente autenticado, UUID coincidente, texto no vacío,
fecha de servidor y esquema cerrado; no permiten editar ni borrar mensajes.
El esquema de texto permanece separado del esquema cerrado de audio descrito a
continuación.

Las transacciones requieren red: pendiente significa espera de confirmación y
fallido significa que el intento devolvió un error, no que pueda afirmarse que
el servidor nunca recibió el mensaje. No hay cola offline ni persistencia del
estado de envío entre procesos en T-16. Historial, composición, restauración del
borrador y mensajes accesibles de confirmación corresponden a T-17/T-18.

`FirebaseTextMessagesTest` solo necesita Auth y Firestore, y usa datos ficticios:

```powershell
firebase emulators:start --project demo-nami --only auth,firestore
# En otro terminal, con un emulador Android arrancado:
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.proyectonami.app.FirebaseTextMessagesTest"
```

Comprueba reintentos secuenciales y concurrentes, recreación del repositorio,
una sola copia y un solo incremento de no leídos, conflicto de contenido,
fallo de transporte contra un puerto local no disponible y recuperación con el
mismo UUID, sesión, autorización y reglas de escritura.
Si faltan Auth o Firestore las pruebas se omiten; comprobar que el informe
indica cero omisiones antes de dar por validada la integración.

## Notas de voz idempotentes (T-34, RF-06/RF-08)

`FirebaseAudioNotesRepository` usa un UUID estable tanto para el documento como
para `conversations/{conversationId}/audio/{uuid}/note.m4a`. Primero reconoce un
mensaje ya confirmado, después sube con progreso y finalmente ejecuta una
transacción que crea el metadato y actualiza actividad/no leídos una sola vez.
Si se pierde la confirmación, el reintento con el mismo UUID devuelve éxito sin
duplicar. El temporal privado se conserva ante fallo y se elimina únicamente al
confirmar el envío o al elegir «Borrar».

Firestore acepta esquemas cerrados y separados para texto y audio. En audio
exige UUID, remitente autenticado, duración entre 1 y 120000 ms, tamaño entre 1
y 5 MiB, MIME `audio/mp4`, ruta determinista, fecha de servidor y estado
`sent`. Storage aplica las mismas condiciones de participación, UUID, nombre,
tamaño y MIME; permite reemplazar el objeto durante un reintento solo antes de
que exista el metadato y luego lo deja inmutable.

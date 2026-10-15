# Firebase y Emulator Suite (T-05)

Esta configuración cubre **T-05**, los requisitos **RF-01, RF-03, RF-05, RF-06** y los requisitos no funcionales **RNF-05 y RNF-06**. El flujo completo de autenticación telefónica pertenece a T-07; aquí se prepara el SDK de Authentication y su emulador.

## Entornos

- `debug` usa por defecto el proyecto ficticio `demo-nami` y conecta Auth, Firestore y Storage al equipo anfitrión mediante `10.0.2.2`.
- `release` no usa emuladores y exige configuración local. Si faltan valores, la aplicación falla de forma explícita al solicitar el contenedor Firebase, en vez de conectar accidentalmente con un proyecto.
- Solo `debug` permite tráfico HTTP sin cifrar, y únicamente hacia `10.0.2.2`, `localhost` y `127.0.0.1` (`app/src/debug/res/xml/network_security_config.xml`), porque los emuladores de Auth y Storage no usan HTTPS. `release` mantiene el bloqueo por defecto.
- `google-services.json`, `.firebaserc`, `firebase.local.properties`, cuentas de servicio, exportaciones y logs están ignorados.

No se debe usar un identificador de proyecto real con los emuladores. La clase `FirebaseEnvironment` exige el prefijo `demo-` para reducir el riesgo de escribir en servicios reales.

## Preparación local

1. Instala Firebase CLI y comprueba que Java 21 o posterior esté disponible.
2. Copia `firebase.local.properties.example` como `firebase.local.properties` si necesitas cambiar host o puertos.
3. Copia `.firebaserc.example` como `.firebaserc`, o indica siempre `--project demo-nami`.
4. Inicia los servicios:

   ```powershell
   firebase emulators:start --project demo-nami --only auth,firestore,storage
   ```

5. En otro proceso, compila y ejecuta la prueba de integración en un emulador Android:

   ```powershell
   .\gradlew.bat connectedDebugAndroidTest
   ```

La prueba `FirebaseEmulatorSmokeTest` crea exclusivamente cuentas anónimas efímeras, un perfil y una conversación ficticios y un pequeño contenido de audio ficticio. Valida conjuntamente Authentication, Firestore, Storage y casos representativos de las reglas: una sesión anónima puede operar sobre sus recursos, una sesión cerrada no puede escribir un perfil y una tercera persona no puede leer la conversación ni su audio. También verifica el teléfono ficticio reservado `+15555550100`: solicita el código al Authentication Emulator, comprueba que un código incorrecto se puede recuperar solicitando otro y completa la verificación. El código se obtiene en memoria desde `http://<host>:9099/emulator/v1/projects/demo-nami/verificationCodes`; no se escribe en logs ni en archivos. La prueba comprueba además que una nueva instancia del repositorio observa la sesión autenticada. Si el Emulator Suite no responde en el host y los puertos configurados, la prueba se omite (no falla) para no bloquear el resto de pruebas instrumentadas. El emulador no envía SMS reales.

## Inyección

`FirebaseRepositoryContainer` construye `AuthRepository`, `FirestoreRepository` y `AudioStorageRepository` a partir de `FirebaseServices`. `AuthRepository` expone la sesión como un flujo, solicita y reenvía códigos, y traduce fallos de Firebase a motivos estables para que la presentación muestre mensajes comprensibles. `PhoneAuthViewModel` conserva nombre, apellidos y teléfono entre errores y reenvíos, y limpia el campo de código cuando se envía uno nuevo. Firebase Auth conserva la sesión local entre aperturas de la aplicación; la prueba instrumentada confirma que el repositorio puede volver a observarla tras verificarse. La comprobación manual tras forzar el cierre y abrir de nuevo la aplicación queda pendiente para T-08, cuando exista navegación basada en la sesión. Las pantallas y casos de uso reciben estas interfaces; no acceden directamente a Firebase.

## Seguridad y privacidad

- Firestore exige autenticación y limita perfiles y contactos a su propietario.
- Conversaciones, mensajes y audios exigen pertenecer a `participantIds`.
- Storage acepta únicamente contenido con tipo `audio/*` en la ruta de una conversación autorizada.
- No se registran teléfonos, tokens, mensajes ni rutas de audio.
- Los valores del ejemplo son ficticios y no conceden acceso a recursos reales.

Las pruebas exhaustivas de todas las operaciones y combinaciones de participantes corresponden a T-26 y siguen pendientes.

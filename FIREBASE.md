# Firebase y Emulator Suite (T-05)

Esta configuración cubre **T-05**, los requisitos **RF-01, RF-03, RF-05, RF-06** y los requisitos no funcionales **RNF-05 y RNF-06**. El flujo completo de autenticación telefónica pertenece a T-07; aquí se prepara el SDK de Authentication y su emulador.

## Entornos

- `debug` usa por defecto el proyecto ficticio `demo-nami` y conecta Auth, Firestore y Storage al equipo anfitrión mediante `10.0.2.2`.
- `release` no usa emuladores y exige configuración local. Si faltan valores, la aplicación falla de forma explícita al solicitar el contenedor Firebase, en vez de conectar accidentalmente con un proyecto.
- `google-services.json`, `.firebaserc`, `firebase.local.properties`, cuentas de servicio, exportaciones y logs están ignorados.

No se debe usar un identificador de proyecto real con los emuladores. La clase `FirebaseEnvironment` exige el prefijo `demo-` para reducir el riesgo de escribir en servicios reales.

## Preparación local

1. Instala Firebase CLI y comprueba que Java esté disponible.
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

La prueba `FirebaseEmulatorSmokeTest` crea exclusivamente una cuenta anónima efímera, un perfil y una conversación ficticios y un pequeño contenido de audio ficticio. Valida conjuntamente Authentication, Firestore, Storage y las reglas. La verificación telefónica emulada se añadirá con el flujo de T-07; el emulador no envía SMS reales.

## Inyección

`FirebaseRepositoryContainer` construye `AuthRepository`, `FirestoreRepository` y `AudioStorageRepository` a partir de `FirebaseServices`. Las pantallas y casos de uso futuros recibirán estas interfaces; no accederán directamente a Firebase.

## Seguridad y privacidad

- Firestore exige autenticación y limita perfiles y contactos a su propietario.
- Conversaciones, mensajes y audios exigen pertenecer a `participantIds`.
- Storage acepta únicamente contenido con tipo `audio/*` en la ruta de una conversación autorizada.
- No se registran teléfonos, tokens, mensajes ni rutas de audio.
- Los valores del ejemplo son ficticios y no conceden acceso a recursos reales.

Las pruebas exhaustivas de reglas para participantes y terceros corresponden a T-26 y siguen pendientes.


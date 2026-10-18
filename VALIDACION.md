# Validación de T-09 (RF-02)

# Validación de T-23/T-24/T-25 (RF-07, RF-09, RNF-01)

Fecha: 16 de octubre de 2026. Rama: `yang_dev`. Responsable: G06-xx.

- `MediaPlaybackCoordinator` detiene el recurso anterior antes de reproducir otro, permite pausar y expone errores recuperables.
- `TextToSpeechCoordinator` permite iniciar/detener lectura y conserva el texto visible cuando el motor falla.
- `DataStoreAccessibilityPreferencesRepository` persiste escala de texto (1–2x) y lectura en voz alta; `NamiTheme` aplica la escala transversalmente.
- Pruebas con reproductor y motor simulados cubren reproducción exclusiva, pausa, error, lectura y detención.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat spotlessApply` | Correcto |
| `gradlew.bat testDebugUnitTest` | Correcto |
| `gradlew.bat koverVerifyDebug` | Correcto |
| `gradlew.bat assembleDebug` | Correcto |
| Revisión manual al 200 % en dispositivo/emulador | Pendiente |
| Revisión por otra persona | Pendiente |

# Validación de T-20/T-21/T-22 (RF-06, RNF-05, RNF-06, RNF-09)

Fecha: 16 de octubre de 2026. Rama: `yang_dev`. Responsable: G06-xx.

- `RECORD_AUDIO` se solicita antes de iniciar; la UI anuncia grabando, duración, parada, denegación e interrupción.
- La máquina de estados conserva una vista previa o un estado seguro; nunca presenta una grabación activa sin indicador.
- Límite acordado y documentado: 120 segundos o 5 MiB, lo que ocurra primero.
- Storage usa una ruta por conversación y `clientMessageId`; Firestore recibe solo metadatos y las reglas restringen el acceso a participantes.
- Cobertura añadida en `AudioRecordingT20T21Test` para grabación vacía, límite y estados visibles.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat spotlessApply` | Correcto |
| `gradlew.bat testDebugUnitTest` | Correcto |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat assembleDebugAndroidTest` | Correcto |
| Revisión manual en dispositivo/emulador | Pendiente |
| Revisión por otra persona | Pendiente |

Fecha: 16 de octubre de 2026. Rama: `yang_dev`.

- `ProfileViewModel` carga y actualiza nombre, apellidos, teléfono y avatar opcional.
- El cambio de teléfono usa una nueva verificación y conserva el UID de Firebase mediante `updatePhoneNumber`.
- Se cubren estados de carga, error, guardado y confirmación; los casos de uso validan y normalizan datos.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat spotlessApply` | Correcto |
| `gradlew.bat testDebugUnitTest` | 105 pruebas, 0 fallos |
| `gradlew.bat assembleDebug` | Correcto |
| Revisión manual en dispositivo/emulador | Pendiente |
| Revisión por otra persona | Pendiente |

# Validación de T-17 (RF-05, RNF-09)

Fecha: 16 de octubre de 2026. Rama: `yang_dev`.

- Historial de texto observado desde Firestore y ordenado por `createdAt`.
- Estados de carga, vacío, cargado y error representados de forma inmutable.
- El borrador se conserva en `SavedStateHandle` y se restaura tras recreación.
- El compositor ignora texto vacío o compuesto solo por espacios; un fallo de
  envío restaura el borrador para permitir reintentar con el mismo UUID.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat spotlessApply` | Correcto |
| `gradlew.bat testDebugUnitTest` | 107 pruebas, 0 fallos |
| `gradlew.bat assembleDebug` | Correcto |
| Revisión manual en dispositivo/emulador | Pendiente |
| Revisión por otra persona | Pendiente |

# Validación de T-18 (RF-08, RNF-04)

Fecha: 16 de octubre de 2026. Rama: `yang_dev-t18`.

- El envío muestra estado pendiente y bloquea el doble envío.
- La confirmación `Mensaje enviado` se muestra y se anuncia como región viva.
- Los fallos conservan el borrador y el reintento reutiliza el mismo UUID del cliente.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat spotlessApply` | Correcto |
| `gradlew.bat testDebugUnitTest` | 108 pruebas, 0 fallos |
| `gradlew.bat assembleDebug` | Correcto |
| Revisión manual en dispositivo/emulador | Pendiente |
| Revisión por otra persona | Pendiente |

# Validación de T-19 (RF-05, RNF-01, RNF-02)

Fecha: 16 de octubre de 2026. Rama de trabajo: `yang_dev-t18` (PR compartida con T-18).

- La pantalla de conversaciones observa estados de carga, vacío y error, ordena por actividad y abre el chat con una acción.
- La composición reutiliza el flujo de T-18: etiqueta accesible, foco/IME de envío, rechazo de texto vacío, bloqueo del doble envío, confirmación y reintento idempotente.
- `ConversationsT19Test` cubre orden reciente, estado vacío y propagación de fallo.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat spotlessApply` | Correcto |
| `gradlew.bat testDebugUnitTest` | 110 pruebas, 0 fallos |
| `gradlew.bat assembleDebug` | Correcto |
| Revisión manual al 200 % en dispositivo/emulador | Pendiente |
| Revisión por otra persona | Pendiente |

# Validación de T-16 (RF-05, RNF-04)

Fecha: 16 de octubre de 2026. Rama: `yang_dev`.
Responsable: G06-xx (por confirmar). Implementación asistida por IA y pendiente
de revisión por otra persona del equipo.

- UUID de cliente previo al envío, reutilizable en reintentos.
- Estados de dominio pendiente, enviado y fallido con motivo explícito.
- Transacción Firestore: una copia por UUID, sin repetir actividad ni no leídos;
  rechazo de contenido distinto para el mismo ID.
- Inyección del repositorio y reglas para texto autenticado e inmutable.
- No incorpora la interfaz de chat ni una cola persistente offline (T-17/T-18).

| Comprobación | Resultado |
|---|---|
| `gradlew.bat testDebugUnitTest` | 102 pruebas, 0 fallos; incluye 8 pruebas nuevas de dominio |
| `gradlew.bat koverVerifyDebug` | Correcto; mínimo del 70 % por paquete de dominio |
| `gradlew.bat lintDebug` | Correcto, sin errores |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat spotlessCheck` | Correcto |
| `FirebaseTextMessagesTest` en Android local API 36 | 7 pruebas, 0 fallos y 0 omitidas; incluye reintento con el mismo UUID tras fallo de transporte |
| Revisión manual del flujo en dispositivo/emulador | Pendiente; T-16 no añade interfaz de chat, que corresponde a T-17/T-18 |
| Revisión por otra persona | Pendiente |

El entorno local dispone de SDK en `%LOCALAPPDATA%\Android\Sdk`, indicado con
`ANDROID_HOME`. La integración usa Auth y Firestore del proyecto ficticio
`demo-nami`; la suite instrumentada de T-16 se ejecutó localmente en Android
16/API 36 con ambos emuladores disponibles.
No considerar cerrada la definición de hecho hasta completar las revisiones
manual y humana.

# Validación local de T-02 (RNF-07)

Fecha: 13 de octubre de 2026. Rama: `pr-31`.

Se elimina el plugin `org.jetbrains.kotlin.android` del módulo, raíz y catálogo para usar el soporte integrado de AGP 9.4.1. Se conserva Compose y JVM 17.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat tasks --all` | Correcto |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat testDebugUnitTest` | 1 prueba, 0 fallos |
| `gradlew.bat lintDebug` | Sin errores; 5 advertencias |
| `gradlew.bat assembleDebugAndroidTest` | Correcto |
| `gradlew.bat connectedDebugAndroidTest` | Correcto en Android 16/API 36: 10 pruebas, 0 fallos y 0 omitidas (15/10/2026, rama integrada `yang_dev`) |
| `git diff --check` | Correcto |

Las advertencias de lint son: objetivo Android anterior al último disponible, versiones más recientes de Core KTX y del compilador Compose, ausencia de reglas de extracción de datos y ausencia de icono de aplicación. La compilación también avisa de la API obsoleta `createAndroidComposeRule`.

El fallo histórico de infraestructura en Android 17 (`NoSuchMethodException: android.hardware.input.InputManager.getInstance`) quedó resuelto al repetir la suite en Android 16/API 36. La validación instrumentada completa pasó en la rama integrada `yang_dev`.

No hay tarea de formato ni configuración Firebase en esta base. Siguen pendientes la validación manual en dispositivo, accesibilidad y revisión por otra persona, conforme a la constitución.

Informes locales: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/lint-results-debug.html` y `app/build/reports/androidTests/connected/debug/index.html`.

# Validación de T-04 (RNF-01)

Rama: `t-04-tema-accesible`. Cambios generados con ayuda de IA (Claude) y pendientes de revisión humana.

Qué incluye:

- `core.designsystem.theme`: paleta clara y oscura (`NamiColorTokens`), tipografía en `sp` (mínimo 14 sp, cuerpo 20 sp), espaciados y tamaños (`NamiSpacing`, `NamiSize`, objetivo táctil de 48 dp y controles de 56 dp) y `NamiTheme` sin color dinámico.
- `core.designsystem.component`: `NamiPrimaryButton`, `NamiSecondaryButton` (siempre texto + icono, texto que pasa de línea en vez de cortarse) y `NamiTextField` (etiqueta obligatoria, error visible y marcado para el lector de pantalla).
- Vistas previas en 320 dp y 411 dp, al 100 % y al 200 %, en claro y oscuro.

El contraste de la paleta se calculó con la fórmula WCAG 2.2 fuera de Gradle: todas las parejas de texto superan 4,5:1 (la menor, 5,65:1) y bordes y componentes superan 3:1.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat assembleDebug` | Correcto (15/10/2026, en local) |
| `gradlew.bat testDebugUnitTest` (incluye `NamiColorContrastTest`) | Correcto (15/10/2026, en local) |
| `gradlew.bat lintDebug` | Sin errores (15/10/2026, en local) |
| `gradlew.bat connectedDebugAndroidTest` (incluye `NamiComponentsTest`) | 6 pruebas del sistema de diseño, 0 fallos y 0 omitidas en Android 16/API 36 (15/10/2026) |
| Revisión manual de las vistas previas y en emulador al 200 % | Pendiente |
| Comparación de la paleta con el prototipo de Figma | Pendiente: la paleta es provisional |

No se añadieron dependencias: los iconos son vectores locales en `res/drawable`.

# Validación de T-06 (RF-01)

Fecha: 15 de octubre de 2026. Rama: `t-06-validacion-cuenta`, integrada con la versión actual de `main` (`10557d2`). Cambios generados con ayuda de IA (DeepSeek Harness) y pendientes de revisión humana.

Qué incluye:

- `core.domain.validation`: `PersonNameValidator` y `PhoneNumberValidator`, con Kotlin puro (sin Android ni Firebase) para poder probarlos en la JVM y reutilizarlos en cuenta y contactos.
- Nombres y apellidos: recorte, espacios repetidos reducidos a uno, entre 1 y 50 caracteres Unicode completos, letras de cualquier alfabeto con marcas de acento, espacios, guion, apóstrofo y punto. Se rechazan cifras, emojis y símbolos, y los separadores no pueden abrir, cerrar ni repetirse.
- Teléfono: normalización a E.164 con `+` opcional, prefijo de país opcional (`defaultCallingCode`), entre 7 y 15 cifras y primera cifra distinta de 0. Admite espacios y `+ - ( ) .` y convierte cifras de otros alfabetos.
- Los validadores devuelven un motivo (`PersonNameError`, `PhoneNumberError`) en lugar de texto, y `strings.xml` incluye un mensaje comprensible por motivo con la convención `validation_person_name_<motivo>` y `validation_phone_number_<motivo>`. T-08 los muestra bajo el campo correspondiente y los expone como errores de accesibilidad.
- `app/build.gradle.kts`: el filtro de Kover de T-03 (RNF-08) se amplía a `core.domain.*`, porque solo cubría `feature.*.domain` y el primer paquete de dominio del proyecto es este.
- `SSD/plan.md` documenta el paquete `core.domain` y la decisión de no añadir una librería de numeración telefónica; `SSD/spec.md` registra `+34` editable como prefijo inicial de la creación de cuenta.
- `defaultCallingCode` solo acepta de una a tres cifras (o un `+` inicial), rechaza otros caracteres y normaliza las cifras Unicode a ASCII antes de generar E.164.
- Los mensajes cubren también separadores repetidos y explican correctamente que se rechaza el cero inicial.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat testDebugUnitTest` | Correcto: 39 pruebas, 0 fallos |
| `gradlew.bat koverVerifyDebug` | Correcto: cumple el mínimo del 70 % de líneas por paquete de dominio |
| `gradlew.bat lintDebug` | Correcto, sin errores |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat spotlessCheck` | Correcto tras integrar `main` y aplicar el formato de T-03 |
| Revisión manual en dispositivo o emulador | No procede en esta tarea: T-06 no añade pantalla, el flujo se revisa en T-08 |
| Revisión por otra persona | Pendiente |

## Limitaciones

- No se validan prefijos ni longitudes por país: haría falta una tabla internacional o `libphonenumber`, que es una dependencia nueva y exige justificación en `plan.md`.
- La pantalla de cuenta incluye el prefijo inicial `+34`, que se puede sustituir por otro prefijo internacional.
- Los mensajes todavía no se han visto en pantalla: T-06 solo garantiza que cada motivo tiene texto, con `ValidationMessagesTest`.

Nota de entorno: las comprobaciones se ejecutaron localmente en Windows con `gradlew.bat`; se indicó el SDK Android instalado mediante `ANDROID_HOME` y `ANDROID_SDK_ROOT`.

Informes locales: `app/build/reports/tests/testDebugUnitTest/index.html` y `app/build/reports/lint-results-debug.html`.

# Validación de T-07 (RF-01, RNF-05)

Fecha: 15 de octubre de 2026. Verificación telefónica y sesión con Firebase Authentication.

Qué incluye:

- `FirebasePhoneAuthRepository` solicita y reenvía códigos usando el SDK de Firebase, verifica el código y expone los cambios de sesión como un flujo.
- Los fallos de número, código incorrecto, código caducado, límite de solicitudes y red se traducen a motivos estables con recursos de mensaje; un código incorrecto no descarta el reto ni el token de reenvío.
- `PhoneAuthViewModel` expone un `StateFlow` con los campos del formulario, el reto y los estados de carga/error. Mantiene nombre, apellidos y teléfono ante un código incorrecto y un reenvío; limpia el código anterior cuando llega uno nuevo. La sesión empieza en `Restoring` y se actualiza con el estado persistido de Firebase.
- El Authentication Emulator verifica el número ficticio reservado `+15555550100`. La prueba instrumentada introduce un código incorrecto, solicita otro, verifica el nuevo y confirma que una nueva instancia del repositorio recupera la sesión.
- T-08 conecta el estado con la pantalla de creación de cuenta. La comprobación manual tras forzar el cierre y volver a abrir la aplicación sigue pendiente.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat testDebugUnitTest` | 43 pruebas, 0 fallos |
| `gradlew.bat koverVerifyDebug` | Correcto; cumple el 70 % por paquete de dominio |
| `gradlew.bat spotlessCheck` | Correcto |
| `gradlew.bat lintDebug` | Correcto, sin errores |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat :app:compileDebugAndroidTestKotlin` | Correcto |
| `gradlew.bat connectedDebugAndroidTest` | 11 pruebas en Android 16/API 36 con Firebase Emulator Suite, 0 fallos y 0 omitidas |

# Validación de T-08 (RF-01, RNF-01)

Fecha: 15 de octubre de 2026. Pantalla Compose de creación de cuenta, verificación telefónica y guardado del perfil.

Qué incluye:

- La app restaura primero la sesión Firebase y presenta el formulario accesible cuando no hay sesión. Incluye nombre, apellidos y teléfono con `+34` editable; valida con los validadores T-06 y utiliza acciones de teclado y desplazamiento vertical.
- Los errores de formulario y autenticación se muestran en pantalla y se exponen a TalkBack; el código incorrecto y el caducado conservan los datos y permiten reenvío.
- Tras verificar, guarda `displayName`, `surname` y `phoneE164` en `users/{uid}` antes de marcar la cuenta como lista. Un fallo de Firestore ofrece reintentar sin volver a verificar.
- Las pruebas UI usan fakes y cubren registro correcto, código incorrecto con conservación del formulario, caducidad y reenvío, fallo/reintento de guardado, validación anunciable, uso con texto al 200 % y escritura del teléfono después del prefijo inicial.
- Revisión manual en el emulador Android 16/API 36 con TalkBack habilitado: se comprobó el orden y foco de los campos, la apertura del teclado numérico y los errores visibles. La persona usuaria confirmó que los anuncios se oyen correctamente.
- Se verificó el teléfono reservado `+15555550100` en Firebase Emulator Suite con el perfil ficticio `NamiTest Familia`; después de `am force-stop` y abrir NAMI de nuevo, la sesión se restauró y volvió a mostrarse «Cuenta creada».
- Se revisó el marco «Creación de cuenta» en Figma: aparecen los mismos tres campos y la acción de crear cuenta. La implementación aún difiere visualmente en cabecera rosa, logo, avatar y estilo de campos; mantiene componentes Material 3 accesibles y añade el paso de verificación exigido por RF-01/T-07.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat testDebugUnitTest` | 43 pruebas, 0 fallos |
| `gradlew.bat koverVerifyDebug` | Correcto; cumple el 70 % por paquete de dominio |
| `gradlew.bat spotlessCheck` | Correcto |
| `gradlew.bat lintDebug` | Correcto, sin errores |
| `gradlew.bat assembleDebug` | Correcto |
| `gradlew.bat connectedDebugAndroidTest` | Correcto en Android 16/API 36 con Firebase Emulator Suite activo: 18 pruebas, 0 fallos y 0 omitidas. Pasaron las 7 de `AccountCreationScreenTest`, las 4 de `FirebaseEmulatorSmokeTest`, `MainActivityTest` y las 6 de `NamiComponentsTest`. |

Limitaciones: la sesión tras cierre forzado y reapertura se verificó manualmente con Firebase Emulator Suite en Android 16/API 36 usando datos ficticios. La pantalla de conversaciones real pertenece a T-15; tras guardar el perfil, T-08 muestra el estado «Cuenta creada» hasta que se implemente esa pantalla. Revisión por otra persona pendiente.

# Validación de T-11 (RF-03, RNF-05, RNF-06)

Fecha: 16 de octubre de 2026. El cliente guarda el teléfono como `phoneE164`,
validado y normalizado a E.164. Las reglas permiten leer únicamente el perfil
propio, rechazan consultas al directorio y exigen que un teléfono escrito
coincida con la credencial telefónica verificada. No hay búsqueda de cuentas
habilitada en el cliente; el diseño para T-12 requiere backend autenticado,
App Check y cuota por UID antes de activar la vinculación.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat testDebugUnitTest` | 43 pruebas, 0 fallos |
| `gradlew.bat spotlessCheck` | Correcto |
| `gradlew.bat :app:compileDebugAndroidTestKotlin` | Correcto |
| `firebase emulators:exec ... connectedDebugAndroidTest` | Android 16/API 36: 23 pruebas, 0 fallos, 0 errores y 0 omitidas |
| `FirebaseEmulatorSmokeTest` | 6 pruebas; incluyen lectura propia permitida, lectura ajena y consulta de directorio denegadas, escritura de teléfono sin verificar denegada y teléfono verificado guardado en E.164 |
| Repetición focalizada de `phoneVerificationCanRetryAndSessionIsRestoredByANewRepository` | Correcta al guardar una entrada con formato y comprobar su forma E.164 persistida |

La revisión manual del flujo de búsqueda/vinculación y la revisión por otra
persona del equipo quedan pendientes: la UI y el backend de búsqueda todavía no
existen en T-11 y deben revisarse en T-12/T-13 antes de habilitar la vinculación.

# Validación de T-12 (RF-03, RNF-05, RNF-08)

Fecha: 16 de octubre de 2026. Rama: `t-12-repositorio-contactos`, creada sobre `main` (`da0b25f`). Cambios generados con ayuda de IA (DeepSeek Harness) y pendientes de revisión humana.

> Nota posterior: la pila de contactos que describe esta entrada se retiró al integrar la función `addContact`; ver «Validación de T-12/T-13».


Qué incluye:

- `feature.contacts.domain`: `Contact` (con `status` deducido de `linkedUserId`), el puerto `ContactRepository`, `AddContactUseCase` y `ContactIdFactory`, en Kotlin puro y probado en la JVM.
- `AddContactUseCase` valida nombre, apellidos y teléfono con los validadores de T-06, guarda el teléfono normalizado en E.164 y devuelve `Added`, `Duplicate` (con el contacto existente), `Invalid` (con el campo y el motivo) o `Failed`.
- Detección de duplicados sin consultar el directorio: `ContactIdFactory` deriva el identificador del teléfono normalizado con SHA-256 truncado, así que el mismo teléfono apunta siempre al mismo documento y el alta es idempotente. `FirebaseContactRepository` lee ese documento antes de escribir y devuelve el contacto ya guardado cuando existe.
- `core.data.firebase.FirebaseContactRepository` escribe en `users/{uid}/contacts` los campos de `plan.md` y añade `createdAt`; el contenedor de repositorios lo expone como `contactRepository`.
- Vinculación: el puerto `ContactAccountResolver` distingue `Found`, `NotFound` y `Unavailable`. El MVP inyecta `DisabledContactAccountResolver`, que responde `Unavailable`, de modo que el contacto se guarda `pending` y el cliente sigue sin consultar el directorio de personas usuarias, como decidió T-11. La ruta vinculada está implementada y probada con un resolver falso, pero no se activa en producción.
- `strings.xml`: mensajes `contact_add_duplicate` (nombra el contacto existente) y `contact_add_failed`.

| Comprobación | Resultado |
|---|---|
| `./gradlew testDebugUnitTest` | 69 pruebas, 0 fallos (23 nuevas de T-12) |
| `./gradlew koverVerifyDebug` | Correcto: `feature.contacts.domain` con 100 % de líneas (76/76) |
| `./gradlew spotlessCheck` | Correcto |
| `./gradlew assembleDebug assembleRelease` | Correcto |
| `./gradlew lintDebug` | Sin errores; 10 advertencias, ninguna en el código nuevo |
| `./gradlew assembleDebugAndroidTest` | Correcto |
| `./gradlew connectedDebugAndroidTest` con Emulator Suite | No ejecutable en local: no hay Firebase CLI, KVM ni AVD disponibles |
| CI `Android CI`, run `38913929782` sobre el commit `cddf8fd` | Correcto en los dos jobs: `quality` (formato, compilación debug y release, lint, pruebas unitarias y cobertura) y `Android 16 y Firebase Emulator Suite` (instrumentadas en Android 16/API 36 con Auth, Firestore y Storage, incluida `contactCreationIsIdempotentAndContactsStayPrivate`) |
| Revisión manual en dispositivo o emulador | No procede en esta tarea: T-12 no añade pantalla y el flujo se revisa en T-13 |
| Revisión por otra persona | Pendiente |

Pruebas nuevas: `AddContactUseCaseTest` (10), `ContactIdFactoryTest` (5), `FirebaseContactRepositoryMappingTest` (4), `ContactTest` (2) y `ContactMessagesTest` (2). En `FirebaseEmulatorSmokeTest` se añade `contactCreationIsIdempotentAndContactsStayPrivate`, que comprueba contra el Emulator Suite que el mismo teléfono deja un único documento pendiente y que otra sesión no puede leer el contacto.

## Limitaciones

- La vinculación con una cuenta existente sigue cerrada: activarla requiere la operación de backend autenticada, con App Check y cuota por UID, descrita en `FIREBASE.md`, que el MVP no incluye. `spec.md` (RF-03) todavía pide «añadirlo o vincularlo», así que el alta funciona hoy siempre como pendiente y la duda queda recogida en `spec.md` para que el equipo decida.
- La comprobación de duplicados usa la caché local de Firestore cuando no hay red: el alta puede quedar en cola y sincronizarse después, sin crear duplicados porque el identificador es determinista.
- No se validan prefijos ni longitudes por país: limitación heredada de T-06.

Nota de entorno: las comprobaciones se ejecutaron en Linux con `./gradlew` y con `GRADLE_USER_HOME` y `ANDROID_USER_HOME` dentro del proyecto, porque `$HOME` era de solo lectura en el entorno usado; `.gradle/` está ignorado.

Informes locales: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/kover/reportDebug.xml` y `app/build/reports/lint-results-debug.html`.

# Validación de T-13 (RF-03, RNF-01, RNF-08)

Fecha: 16 de octubre de 2026. Rama: `t-13-pantalla-nuevo-contacto`, creada sobre `main` (`203bb58`). Cambios generados con ayuda de IA (DeepSeek Harness) y pendientes de revisión humana.

> Nota posterior: la pila de contactos que describe esta entrada se retiró al integrar la función `addContact`; ver «Validación de T-12/T-13».


Qué incluye:

- `feature.contacts.presentation`: `AddContactViewModel` con `AddContactUiState` inmutable en `StateFlow`, la validación conjunta de los tres campos (`validateContactForm`, con los validadores de T-06) y el reductor puro `afterAddResult`; `AddContactScreen` y su `AddContactRoute` en Compose.
- La pantalla reutiliza los componentes accesibles de T-04, pide el teléfono con el mismo prefijo `+34` inicial editable que la creación de cuenta (duda 7 de `spec.md`), valida los tres campos en un solo intento para no descubrir un error por envío, muestra el duplicado nombrando el contacto existente sin escribir nada, permite reintentar un fallo sin duplicar y confirma el alta. El título es un encabezado, los errores de campo se exponen como errores de accesibilidad, el duplicado y el fallo se anuncian como región viva y todo el contenido es desplazable para el texto al 200 %.
- Teclado: «Siguiente» en nombre y apellidos y «Listo» en el teléfono, que guarda el contacto; los campos se bloquean mientras se guarda.
- Los mensajes de validación de T-06 se comparten con T-08 en `ui/ValidationMessages.kt`; no se añadieron dependencias ni se modificó el caso de uso de T-12.
- Acceso provisional: el estado «Cuenta creada» de T-08 incorpora el botón `Añadir un contacto`. T-15 sustituirá ese punto de entrada por el `+` de la pantalla principal.
- La vinculación sigue cerrada: `AddContactUseCase` se construye con `DisabledContactAccountResolver`, así que el contacto queda `pending` y la interfaz no afirma que la otra persona no tenga cuenta NAMI.
- `Prototype_Reference/` (commit anterior de esta rama) guarda las seis capturas del prototipo; `SSD/AGENTS.md`, `SSD/plan.md`, `SSD/README.md` y `README.md` avisan de que es un boceto de baja fidelidad y de que la implementación mejora el estilo y la interacción con los tokens de T-04.

| Comprobación | Resultado |
|---|---|
| `./gradlew testDebugUnitTest` | 83 pruebas, 0 fallos (14 nuevas: `AddContactFormValidationTest` con 5 y `AddContactViewModelTest` con 9) |
| `./gradlew spotlessCheck` | Correcto |
| `./gradlew assembleDebug assembleRelease` | Correcto |
| `./gradlew lintDebug` | Sin errores; 9 advertencias, ninguna en el código nuevo |
| `./gradlew koverVerifyDebug` | Correcto: `feature.contacts.domain` mantiene el 100 % de líneas |
| `./gradlew assembleDebugAndroidTest` | Correcto |
| `./gradlew connectedDebugAndroidTest` con Emulator Suite | No ejecutable en local: no hay Firebase CLI, KVM ni AVD disponibles; lo ejecuta CI en el PR |
| `AddContactScreenTest` (nueva, 9 pruebas) | Pendiente de CI en Android 16/API 36: alta y confirmación, acción «Listo» del teclado, duplicado que nombra el contacto existente, edición del teléfono que retira el aviso, validación anunciada al lector, fallo con reintento sin duplicar, control de volver, «añadir otro contacto» y texto al 200 % |
| Revisión manual en dispositivo o emulador al 200 % | Pendiente |
| Revisión por otra persona | Pendiente |

## Limitaciones

- El estado `pending` del contacto no se muestra en la interfaz: con el resolver desactivado el cliente no puede saber si la otra persona tiene cuenta NAMI (duda 8 de `spec.md`). Tampoco se puede «abrir su conversación» desde esta pantalla, porque la lista de conversaciones y el chat son de T-14 a T-17; CA-02 solo se cierra por completo cuando existan.
- El acceso a la pantalla es provisional desde el estado «Cuenta creada»; el definitivo llega con T-15.
- `AddContactScreenTest` no se pudo ejecutar en el entorno local: requiere un emulador Android y no hay KVM ni AVD disponibles. Se confirma con el trabajo instrumentado de CI.
- No se validan prefijos ni longitudes por país: limitación heredada de T-06.
- La detección de duplicados lee el documento del contacto antes de escribir; sin red usa la caché local de Firestore, como en T-12.

Nota de entorno: las comprobaciones se ejecutaron en Linux con `./gradlew` y con `GRADLE_USER_HOME` y `ANDROID_USER_HOME` dentro del proyecto, porque `$HOME` era de solo lectura en el entorno usado; `.gradle/` está ignorado.

# Validación de T-12/T-13 (RF-03, RNF-05, RNF-08)

Fecha: 17 de octubre de 2026. Rama: `t-12-t13-integrar-callable`, creada sobre `main` (`2aa115a`). Cambios generados con ayuda de IA (DeepSeek Harness) y pendientes de revisión humana.

Qué incluye:

- El alta de contactos deja de usar la pila de T-12 (`feature.contacts.domain`, `FirebaseContactRepository` y el puerto `ContactAccountResolver`) y pasa a `core.domain.contacts` + `FirebaseContactsRepository`, que llama a la función `addContact` de T-11. Las reglas de Firestore ya deniegan toda escritura directa del cliente en `contacts`, así que el camino anterior no podía funcionar: el alta fallaba con «no se pudo guardar».
- `AddContactViewModel` y `AddContactScreen` distinguen los tres desenlaces de la función: vinculado (el servidor confirma que el teléfono tiene cuenta NAMI y la confirmación lo dice), pendiente (no la tiene) y comprobación no disponible (cuota agotada o servicio caído), que **no guarda nada** y se puede reintentar. Desaparece `DisabledContactAccountResolver`, así que el estado «siempre pendiente» ya no se aplica.
- Se borran la pila antigua, sus pruebas y `FirebaseContactRepositoryMappingTest`, cuyas clases ya no existen. Los mensajes de validación de T-06 siguen compartidos con T-08 (`ui/ValidationMessages.kt`).
- CI: el trabajo de emuladores arranca también `functions`, el de calidad ejecuta `npm test` dentro de `functions` y un paso nuevo falla si alguna prueba instrumentada se ha omitido. Sin ese paso, `FirebaseEmulatorSmokeTest` se saltaba entero (pedía el puerto de Functions, que no se arrancaba) y el trabajo quedaba verde con la suite de reglas sin ejecutar.
- El smoke test de contactos se reescribe: crea dos cuentas con teléfono verificado, comprueba que el teléfono conocido queda `linked` con el UID correcto, que un teléfono sin cuenta queda `pending`, que el duplicado no escribe una segunda vez, que cada teléfono deja un solo documento y que otra sesión no puede leerlo.

| Comprobación | Resultado |
|---|---|
| `cd functions && npm ci && npm test` | 4 pruebas, 0 fallos (cuota por minuto y por día, E.164, normalización de nombres e identificadores estables) |
| `cd functions && npm run build` | Correcto (`lib/index.js` y `lib/contactSecurity.js`) |
| `./gradlew testDebugUnitTest` | 101 pruebas, 0 fallos |
| `./gradlew spotlessCheck` | Correcto |
| `./gradlew assembleDebug assembleRelease` | Correcto |
| `./gradlew lintDebug` | Sin errores; 12 advertencias, ninguna en el código nuevo (la única que apuntaba a una cadena nueva era un falso positivo del corrector sobre «momento», ya reescrito) |
| `./gradlew koverVerifyDebug` | Correcto |
| `./gradlew assembleDebugAndroidTest` | Correcto |
| `connectedDebugAndroidTest` con Emulator Suite | No ejecutable en local (sin Firebase CLI, KVM ni AVD); lo ejecuta CI con los cuatro emuladores |
| CI `Android CI`, run `38962582758` sobre el commit `63a25e4` | Correcto en los dos trabajos: 50 pruebas instrumentadas, 0 fallos, 0 errores y **0 omitidas**, con Auth, Firestore, Storage y Functions |
| `FirebaseEmulatorSmokeTest.addContactLinksKnownPhonesAndStoresUnknownOnesAsPending` | Correcto en Android 16/API 36: el teléfono con cuenta queda `linked` con el UID correcto, el desconocido queda `pending` y el duplicado no escribe una segunda vez |
| Revisión por otra persona | Pendiente |

La primera ejecución completa de la suite (run `38960340598`) hizo 50 pruebas y **0 omisiones**, y solo falló la preparación de `FirebaseTextMessagesTest.retriesAndNewRepositoryKeepOneCopyAndOneUnreadIncrement`: agotó los 20 s de `Tasks.await` en su primera escritura (20,06 s exactos) mientras el resto de la clase tardaba entre 0,06 s y 8,8 s. Era la espera de la primera conexión con el emulador, así que los límites de las dos clases de integración pasan a 60 s y el trabajo quedó verde.

## Limitaciones

- La creación de `conversations/{conversationId}` sigue sin tarea asignada (`spec.md`, duda 9): el alta ya puede vincular un contacto, pero abrir su conversación necesita que alguien cree ese documento. Las sesiones que se ven hoy en la aplicación vienen de datos de prueba.
- La cuota de la función se prueba en `functions/test`; el caso de cuota agotada se cubre en la interfaz con un repositorio falso y no contra el emulador, para no depender del reloj.
- App Check solo se puede comprobar en un proyecto real con Play Integrity: en el emulador la función lo desactiva a propósito y en `debug` el cliente no lo instala.

# Validación de T-05 (RF-01, RF-03, RF-05, RF-06; RNF-05, RNF-06)

Rama: `yang_dev`, con `main` integrada (incluye T-03). Cambios de cierre generados con ayuda de IA (Claude) y pendientes de revisión humana.

Qué se completó sobre la configuración inicial de T-05:

- Integración de `main` (T-03: Spotless y Kover) y resolución del conflicto en `AndroidManifest.xml`.
- Formato Spotless/ktlint en todo el código y `.editorconfig` para admitir nombres en PascalCase en funciones `@Composable`; sin esto `spotlessCheck` fallaba también en `main`.
- `network_security_config` solo para `debug`: permite HTTP sin cifrar únicamente hacia `10.0.2.2`, `localhost` y `127.0.0.1`, necesario para los emuladores de Auth y Storage. `release` mantiene el bloqueo por defecto.
- `FirebaseEmulatorSmokeTest` se omite si el Emulator Suite no responde, en vez de fallar y bloquear el resto de pruebas instrumentadas.
- `FirebaseEmulatorSmokeTest` incluye casos representativos de denegación: una sesión cerrada no puede escribir perfiles y una tercera persona no puede leer conversaciones ni audios ajenos.
- Comando `EMULADORES_FIREBASE` documentado en `SSD/AGENTS.md`.
- Workflow `.github/workflows/android-ci.yml` para repetir formato, compilación, lint, pruebas unitarias, cobertura y pruebas instrumentadas con Android 16/API 36 y Firebase Emulator Suite.

| Comprobación | Resultado |
|---|---|
| `gradlew.bat spotlessCheck` | Correcto (15/10/2026, en local) |
| `gradlew.bat assembleDebug assembleRelease` | Correcto (15/10/2026, en local) |
| `gradlew.bat testDebugUnitTest` | 10 pruebas, 0 fallos (incluye `FirebaseEnvironmentTest` y `FirebasePathsTest`) |
| `gradlew.bat lintDebug` | Sin errores; 7 advertencias |
| `gradlew.bat koverVerifyDebug` | Correcto (aún no hay paquetes de dominio) |
| `gradlew.bat assembleDebugAndroidTest` | Correcto |
| `gradlew.bat connectedDebugAndroidTest` con Emulator Suite activo | 10 pruebas, 0 fallos, 0 errores y 0 omitidas en `NAMI_API_36` (Android 16/API 36) |
| `FirebaseEmulatorSmokeTest` | 3 pruebas: flujo autorizado, sesión no autenticada y tercera persona; 0 fallos y 0 omitidas |
| Revisión de Auth, Firestore y Storage en Emulator Suite UI | Correcto: cuentas anónimas efímeras, conversaciones ficticias y audios `audio/mp4` visibles; sin datos reales |

## Validación de T-15, T-26 y T-27

- T-15 cubre la lista de conversaciones en Compose: carga, vacío, error,
  nombre, avatar opcional, no leídos, navegación al chat y escala de texto al
  200 % en `ConversationsScreenTest`.
- T-26 amplía `FirebaseEmulatorSmokeTest` con autorización negativa para el
  documento de mensaje, además de los perfiles, conversaciones y audios ya
  cubiertos. El comando real está documentado en `SSD/README.md` y
  `SSD/RENDIMIENTO.md`.
- T-27 documenta objetivos y límites en `SSD/RENDIMIENTO.md`; las pruebas
  unitarias de `FlowMetricsT27Test` verifican que no haya duplicados y que el
  borrador/audio permanezcan en estado seguro.

La pérdida de red física, el audio en hardware real y la revisión de otra
persona siguen siendo validaciones manuales pendientes antes de fusionar.

## Validación de T-28 (RNF-01, RNF-02)

La auditoría automática queda registrada en [`SSD/ACCESIBILIDAD.md`](SSD/ACCESIBILIDAD.md):
semántica para TalkBack, estados anunciables, foco, objetivos táctiles de 48 dp,
texto al 200 % en 320/411 dp y perfiles representativos con datos ficticios.
`ConversationsScreenTest` comprueba además que el título es encabezado, el
avatar tiene descripción y la acción de conversación mantiene el tamaño mínimo.

La activación de TalkBack en un dispositivo/emulador y la revisión por otra
persona deben completarse antes de fusionar; no se afirma que una prueba
instrumentada sustituya esa comprobación manual.

# Validación de T-29 (todos los RF/RNF)

Fecha: 17 de octubre de 2026. Rama: `yang_dev`. Responsable: G06-xx.

Esta revisión consolida la trazabilidad de `SSD/spec.md` y `SSD/tasks.md`.
Las referencias a pruebas son nombres reales de clases y métodos del
repositorio; los datos de prueba son sintéticos (`Persona Ficticia`,
`conversation-fictitious` y teléfonos reservados del emulador).

## Matriz de requisitos y evidencias

| Requisito | Prueba o evidencia verificable |
|---|---|
| RF-01 | `AccountCreationScreenTest` (verificación correcta, código incorrecto, caducidad, reintento y formulario al 200 %); `FirebaseEmulatorSmokeTest.phoneVerificationCanRetryAndSessionIsRestoredByANewRepository`. |
| RF-02 | `ProfileUseCasesTest` (carga, normalización, avatar opcional y rechazo sin escritura); `ProfileViewModel` expone carga/error/guardado. |
| RF-03 | `AddContactUseCaseTest`, `AddContactScreenTest` y `FirebaseEmulatorSmokeTest.addContactLinksKnownPhonesAndStoresUnknownOnesAsPending` (vinculación, pendiente, duplicado, fallo y reintento). |
| RF-04 | `ObserveConversationsUseCaseTest`, `ConversationsT19Test`, `ConversationsScreenTest` y `FirebaseEmulatorSmokeTest.conversationsAreScopedToCurrentUserAndOrderedWithUnreadCounts`. |
| RF-05 | `TextConversationT17Test`, `SendTextMessageUseCaseTest`, `TextConversationScreenTest` y `FirebaseTextMessagesTest` (historial, borrador, vacío, reintento idempotente y reglas). |
| RF-06 | `AudioRecordingT20T21Test`, `AudioRecordingScreenTest` y `FirebaseAudioNotesTest` (permiso, duración, límites, vista previa, subida, reintento, reglas y acciones accesibles); `FirebaseEmulatorSmokeTest.thirdPartyCannotReadConversationOrAudio`. |
| RF-07 | `MediaPlaybackT23Test`, `MediaSessionCoordinatorTest`, `TextConversationScreenTest`, `FirebaseAudioPlaybackTest` y `TextToSpeechT24Test` (historial mixto, descarga autorizada, reproducción exclusiva, pausa/reanudación, lectura TTS, error recuperable, interrupción y detener sesión). |
| RF-08 | `TextConversationScreenTest`, `FirebaseAudioNotesTest` y `SendTextMessageUseCaseTest` (pendiente, progreso, enviado, fallido, confirmación, reintento y ausencia de duplicados). |
| RF-09 | `AccessibilityPreferencesT25Test`, `AccessibilityPreferencesPersistenceTest`, `SettingsViewModelTest`, `SettingsScreenTest`, `NamiComponentsTest`, `ConversationsScreenTest`, `TextConversationScreenTest` y `TextToSpeechT24Test` (ajustes productivos, escala persistente, 200 %, preferencia de lectura y estados anunciables). |
| RF-10 | `AccountCreationScreenTest.signOutRequiresConfirmationAndCanBeCancelled`, `confirmedSignOutStopsMediaAndClearsPrivateState` y `MediaSessionCoordinatorTest`. |
| RNF-01 | `NamiColorContrastTest`, `NamiComponentsTest` y las pruebas UI de cuenta, conversaciones y chat al 200 %; auditoría detallada en `SSD/ACCESIBILIDAD.md`. |
| RNF-02 | `ConversationsScreenTest.contentShowsAvatarUnreadCountAndNavigatesToConversation` y `TextConversationScreenTest`; el flujo inicio → conversación → campo → enviar queda en cuatro acciones como máximo. |
| RNF-03 | `FlowMetricsT27Test`, `FirebasePerformanceRecoveryTest` y `SSD/RENDIMIENTO.md` calculan p95 sobre 20 muestras productivas: 11 ms para contenido y 30 ms para confirmación en el AVD local. |
| RNF-04 | `SendTextMessageUseCaseTest`, `FirebaseTextMessagesTest`, `FirebaseAudioNotesTest`, `FlowMetricsT27Test` y `FirebasePerformanceRecoveryTest` comprueban UUID de cliente, reintento secuencial/concurrente y ausencia de duplicados. |
| RNF-05 | `FirebaseEmulatorSmokeTest`, `FirebaseTextMessagesTest` y `FirebaseAudioNotesTest` cubren sesión no autenticada, terceros y reglas de perfiles, conversaciones, mensajes y audios. |
| RNF-06 | Reglas, repositorios y pruebas usan datos ficticios; la revisión de fuentes no encuentra logging de contenido, teléfonos completos ni rutas privadas. Los archivos sensibles están excluidos por `.gitignore`. |
| RNF-07 | CI valida Android 16/API 36 en el emulador. La compatibilidad física con la versión mínima provisional (API 26) y con un segundo tamaño de dispositivo queda pendiente. |
| RNF-08 | El trabajo `quality` de CI ejecuta `koverVerifyDebug` y termina correctamente con el mínimo del 70 % por paquete de dominio. |
| RNF-09 | `TextConversationT17Test`, `AudioRecordingT20T21Test`, `FlowMetricsT27Test`, `TextConversationScreenTest` y `FirebasePerformanceRecoveryTest` mantienen borrador/audio en estado seguro durante recreación, salida, fallo y reintento. |

## Criterios de aceptación revisados

- **Flujo principal:** se recorrió mediante `ConversationsScreenTest` y
  `TextConversationScreenTest`: abrir una conversación, escribir y enviar un
  texto ficticio, con confirmación visible/anunciable y máximo cuatro acciones
  desde inicio.
- **Error:** `TextConversationScreenTest` y
  `FirebaseTextMessagesTest.unavailableFirestoreRetainsUuidAndRetriesSuccessfully`
  fuerzan un fallo de transporte; el estado queda fallido, permite reintentar
  y conserva el mismo UUID sin crear otra copia.
- **Caso límite:** se cubren texto vacío o solo con espacios, grabación vacía,
  límite de 120 segundos o 5 MiB, audio ausente/no decodificable y texto al
  200 % en anchuras de 320 dp y 411 dp.

## Regresión y resultado

| Comprobación | Resultado |
|---|---|
| `git diff --check` | Correcto antes de este cambio documental. |
| CI `Android CI`, run `38972892443`, commit `a87c3bc` | Correcto: `quality` y `Android 16 y Firebase Emulator Suite` en verde. |
| `quality` | Correcto: Cloud Functions, Spotless, compilación debug/release, pruebas JVM, lint, Kover y `assembleDebugAndroidTest`. |
| Suite instrumentada | Correcta en Android 16/API 36; el paso de comprobación de omitidas también terminó correctamente. |
| Regresión local en este entorno | No ejecutable: el SDK local no permite leer `platforms/android-37.0/package.xml` y Gradle no puede descargar los manifiestos sin red. No se presenta este intento como resultado positivo. |

## Revisión de funciones excluidas y limitaciones

La inspección estática de `app/src/main` no encuentra controles ni rutas
activas para chats grupales, llamadas, videollamadas, fotografías, vídeos,
documentos, ubicación, inicio de sesión biométrico, panel web, cifrado de
extremo a extremo, tutoriales ni contenido generado por IA. Los iconos de
cámara, llamada y vídeo de Figma siguen siendo exploraciones visuales, tal como
declara `SSD/spec.md` §3; no se han conectado a ninguna acción.

Quedan abiertas las limitaciones ya declaradas en la especificación: prueba
física con TalkBack por otra persona, repetición de las métricas y del corte de
red en hardware real, audio en hardware real, compatibilidad con API 26 y
revisión humana formal.
La definición de hecho no se marca completa hasta que el equipo registre esas
revisiones.

## T-30 - paquete final

- `README.md` y `SSD/README.md` describen un clon limpio, la compilacion del APK y la ejecucion de Auth, Firestore, Storage y Functions con Emulator Suite.
- `entrega_G06/` contiene la presentacion PDF, su fuente, el manifiesto para Moodle y el APK debug disponible con SHA-256.
- `SSD/DESIGN_THINKING.md`, `SSD/FIGMA.md`, `SSD/uso-ia.md` y `evidencias-ia/` enlazan las decisiones, capturas y casos de uso de IA.
- Verificacion local de artefactos: `git diff --check` pasa, el PDF se renderizo a PNG y se revisaron las paginas 1, 4 y 5; el APK se pudo resumir con `Get-FileHash`.
- Regresion local de Functions: `npm ci` pasa; `npm test` queda bloqueado en este entorno por `uv_os_get_passwd returned ENOMEM` con Node 24. La regresion completa se delega al workflow de CI, que usa Node 22.
- Siguen siendo pasos externos la comprobacion de acceso docente, la auditoria TalkBack en dispositivo/emulador y la segunda revision humana.

# Validación de T-31 (RF-03, RF-04, RF-05; RNF-04, RNF-05)

Fecha: 17 de octubre de 2026. Rama: `yang_dev`.

- La callable autenticada `openConversation` sólo acepta un documento de contacto
  `linked` perteneciente al UID llamante. El ID SHA-256 de la pareja ordenada y
  una transacción garantizan una sola conversación 1:1 por pareja.
- La conversación nace con los dos participantes, sus datos visibles,
  `unreadCountByUser` a cero y `lastActivityAt`; el cliente abre inmediatamente
  el chat desde un contacto vinculado y explica por qué uno pendiente no puede
  abrirse.
- Las reglas deniegan toda creación directa desde clientes. La integración
  comprueba además que una tercera cuenta no puede reutilizar el contacto ni
  crear, leer o modificar la conversación.
- `seedConversationForTests` sustituye la siembra directa de fixtures antiguos y
  rechaza cualquier ejecución fuera de Firebase Emulator Suite.

| Comprobación | Resultado |
|---|---|
| `npm run build` | Correcto |
| Pruebas unitarias de Functions compiladas y ejecutadas con `node --test` | 4 pruebas, 0 fallos |
| `spotlessCheck testDebugUnitTest koverVerifyDebug lintDebug assembleDebug assembleDebugAndroidTest` | Correcto |
| Suite instrumentada completa con Auth, Firestore, Functions y Storage en Android 16/API 36 | Correcta, 0 fallos |
| `FirebaseEmulatorSmokeTest` tras ampliar seguridad | 9 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Flujo desde Emulator Suite vacío | Correcto: dos cuentas ficticias, vínculo, apertura idempotente y envío de texto ficticio |
| Revisión manual exploratoria | Pendiente |
| Revisión por otra persona | Pendiente |

`npm test` no arranca con el Node 24 instalado localmente por el error del
cargador `tsx` `uv_os_get_passwd returned ENOMEM`; el proyecto y CI fijan Node
22. Para no presentar un falso positivo, en local se compiló la misma prueba
TypeScript y se ejecutó directamente con `node --test`, con resultado verde.

# Integración productiva de T-20/T-21 en el chat (RF-06; RNF-01, RNF-09)

Fecha: 17 de octubre de 2026. Rama: `yang_dev`.

- `AndroidAudioRecorder` usa `MediaRecorder` con AAC/MPEG-4, crea el temporal
  únicamente en la caché privada y libera o elimina recursos al cancelar,
  interrumpir, superar límites, salir del chat o cerrar sesión.
- `MainActivity` crea `AudioRecordingViewModel` por usuario y conversación. Una
  pulsación en «Grabar nota de voz» abre el diálogo e inicia la grabación si el
  permiso ya está concedido; la denegación temporal o permanente se explica y
  siempre permite continuar escribiendo.
- Una nota válida ofrece «Escuchar», «Borrar» y «Enviar». La reproducción previa
  usa `MediaPlayer`; «Enviar» entrega el resultado local al punto de extensión
  del chat sin ejecutar la subida ni el reintento remoto reservados para T-34.
- El límite se mantiene en 120 segundos y 5 MiB. Vacío, exceso e interrupción
  eliminan el archivo y muestran una acción recuperable.
- Todos los textos están en recursos, los botones reutilizan los controles de
  al menos 48 dp y los cambios de estado se anuncian como regiones vivas.

| Comprobación | Resultado |
|---|---|
| `spotlessCheck testDebugUnitTest koverVerifyDebug assembleDebug assembleDebugAndroidTest lintDebug` | Correcto; lint sin errores y sin advertencias nuevas tras retirar `EmptySuperCall` |
| `AudioRecordingScreenTest` en `NAMI_API_36` | 5 pruebas, 0 fallos, 0 errores y 0 omitidas |
| `TextConversationScreenTest` en `NAMI_API_36` | 3 pruebas, 0 fallos, 0 errores y 0 omitidas |
| `AndroidAudioRecorderTest` en `NAMI_API_36` | 1 prueba real con `MediaRecorder`: temporal privado, duración/tamaño positivos y borrado correcto |
| Revisión manual de micrófono y reproducción en emulador/dispositivo | Pendiente |
| Revisión por otra persona | Pendiente |

# Integración productiva de T-34 (RF-06, RF-08; RNF-04, RNF-05, RNF-09)

Fecha: 17 de octubre de 2026. Rama: `yang_dev`. Seguimiento: #57.

- El chat entrega la vista previa a `FirebaseAudioNotesRepository`, muestra
  pendiente/progreso/enviado y conserva el temporal con «Reintentar» y «Borrar»
  cuando la operación falla.
- Cada intento conserva el mismo UUID y la ruta
  `conversations/{conversationId}/audio/{uuid}/note.m4a`. La transacción reconoce
  un mensaje idéntico y no repite el incremento de no leídos.
- Firestore valida un esquema cerrado de nueve campos. Storage exige participante,
  UUID, `note.m4a`, `audio/mp4` y un máximo de 5 MiB; después de crear el metadato
  el objeto remoto queda inmutable.
- El éxito elimina el temporal. Una denegación o fallo lo conserva hasta reintento
  o borrado explícito, sin registrar rutas privadas ni contenido.

| Comprobación | Resultado |
|---|---|
| `npm test` en `functions` | 4 pruebas, 0 fallos y 0 omitidas |
| `npm run build` en `functions` | Correcto |
| `FirebaseAudioNotesTest` con Auth, Firestore, Functions y Storage | 4 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Suite instrumentada completa en `NAMI_API_36` | 63 pruebas, 0 fallos, 0 errores y 0 omitidas |
| `TextConversationScreenTest` | 4 pruebas; fallo, conservación, reintento con mismo UUID y éxito accesible |
| Revisión manual del envío real desde micrófono | Pendiente |
| Revisión por otra persona | Pendiente |

# Integración productiva de T-35 (RF-07; issue #58, seguimiento de #23)

Fecha: 17 de octubre de 2026. Rama: `yang_dev`.

- El historial productivo distingue mensajes de texto y audio y valida el
  esquema, UUID, duración, tamaño, MIME y ruta antes de representar una nota.
- `FirebaseAudioPlaybackSourceRepository` descarga mediante la sesión
  autenticada de Storage a una caché privada, sin URLs públicas ni rutas en
  logs. La copia se elimina al cerrar sesión.
- `AndroidAudioPlayer` y `MediaPlaybackCoordinator` ofrecen reproducir, pausar,
  reanudar y detener; iniciar otra nota detiene la anterior.
- Los fallos de autorización, red, ausencia o decodificación son recuperables y
  no bloquean el envío de texto. Salir del chat o cerrar sesión detiene el audio.
- La integración graba una nota real con `MediaRecorder`, la envía al Emulator
  Suite, la observa en el historial, la descarga y la reproduce con
  `MediaPlayer` desde la caché privada.

| Comprobación | Resultado |
|---|---|
| `spotlessCheck testDebugUnitTest koverVerifyDebug assembleDebug assembleDebugAndroidTest lintDebug` | Correcto |
| Compilación y pruebas unitarias de Functions | `npm run build` correcto; 4 pruebas compiladas y ejecutadas con `node --test`, 0 fallos y 0 omitidas |
| `TextConversationScreenTest` en `NAMI_API_36` | 6 pruebas, 0 fallos, 0 errores y 0 omitidas |
| `FirebaseAudioPlaybackTest` con Auth, Firestore, Functions y Storage | 1 prueba real, 0 fallos, 0 errores y 0 omitidas |
| Suite instrumentada completa en `NAMI_API_36` | 66 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Revisión manual de reproducción en emulador/dispositivo | Pendiente |
| Revisión por otra persona | Pendiente |

# Integración productiva de T-36 (RF-07, RF-09; RNF-01; issue #59)

Fecha: 17 de octubre de 2026. Rama: `yang_dev`. Seguimiento: #24.

- `AndroidTextToSpeechEngine` inicializa el motor del sistema, comprueba la voz
  española, notifica fin/error y libera `TextToSpeech` de forma idempotente.
- La preferencia `readMessagesAloud` controla las acciones del chat y sigue
  persistida en DataStore al recrear el repositorio.
- Cada texto conserva su contenido visible y ofrece «Escuchar», «Detener
  lectura» o «Reintentar lectura» según el estado; los cambios se anuncian como
  región viva y los botones mantienen el objetivo táctil de 56 dp.
- Una lectura nueva detiene audio y lectura anteriores. Reproducir audio, salir
  del chat o cerrar sesión detiene TTS.
- Motor o idioma ausentes producen un mensaje recuperable sin ocultar el texto.

| Comprobación | Resultado |
|---|---|
| `spotlessCheck testDebugUnitTest koverVerifyDebug assembleDebug assembleDebugAndroidTest lintDebug` | Correcto; 108 pruebas JVM, 0 fallos, 0 errores y 0 omitidas |
| `TextToSpeechT24Test` | 4 pruebas: lectura/detención, motor ausente, idioma ausente, finalización, interrupción y liberación |
| `TextConversationScreenTest` en `NAMI_API_36` | 9 pruebas, 0 fallos, 0 errores y 0 omitidas |
| `AccessibilityPreferencesPersistenceTest` en `NAMI_API_36` | 1 prueba, 0 fallos, 0 errores y 0 omitidas |
| Suite instrumentada completa con Emulator Suite en `NAMI_API_36` | 70 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Revisión manual con TTS del sistema | Pendiente |
| Revisión por otra persona | Pendiente |

# Integración productiva de T-37 (RF-09; RNF-01; issue #60)

Fecha: 18 de octubre de 2026. Rama: `yang_dev`. Seguimiento: #25.

- La pantalla principal ofrece una entrada visible a «Ajustes» y mantiene sus
  acciones en una lista desplazable para no recortarlas con texto ampliado.
- `SettingsViewModel` conecta la interfaz con DataStore y ofrece tamaño normal
  (100 %), grande (150 %) y máximo (200 %), además de la lectura en voz alta.
- El `NamiTheme` compartido observa la misma preferencia, por lo que la escala
  cambia inmediatamente y se conserva al reiniciar la aplicación.
- Cada opción expone etiqueta, selección/estado y acción; la pantalla es
  desplazable y se prueba a 320 dp y 411 dp con escala máxima.
- El control provisional del chat se retira: Ajustes es la única fuente de
  modificación y el chat se limita a respetar la preferencia persistida.

| Comprobación | Resultado |
|---|---|
| `spotlessCheck testDebugUnitTest koverVerifyDebug assembleDebug assembleDebugAndroidTest lintDebug` | Correcto; 109 pruebas JVM, 0 fallos, 0 errores y 0 omitidas |
| `SettingsViewModelTest` | 1 prueba, cambio de ambas preferencias y recreación con valores conservados |
| `SettingsScreenTest` en `NAMI_API_36` | 3 pruebas, controles semánticos y anchos de 320/411 dp al 200 % |
| `AccessibilityPreferencesPersistenceTest` | Preferencias de escala y lectura persistidas tras recrear el repositorio |
| Suite instrumentada completa con Emulator Suite en `NAMI_API_36` | 73 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Auditoría manual con TalkBack | Pendiente |
| Revisión por otra persona | Pendiente |

# Integración productiva de T-10/T-15 (RF-10)

Fecha: 17 de octubre de 2026. Rama: `yang_dev`.

- `NamiApp` mantiene una única confirmación de cierre para la pantalla
  provisional y la pantalla productiva de conversaciones; ninguna pantalla
  llama ya directamente a `PhoneAuthViewModel.signOut()` desde el botón.
- Cancelar conserva la sesión y `Mis conversaciones`. Confirmar elimina las
  rutas privadas de chat, perfil y contactos antes de detener multimedia y
  revocar Firebase Auth.
- La limpieza cubre grabación, reproducción y texto a voz a través del
  `MediaSessionController`. Un resultado parcial se anuncia en el acceso sin
  restaurar la sesión.
- La prueba de retorno envía `KEYCODE_BACK` después del cierre y confirma que la
  sesión continúa revocada y la pantalla privada no reaparece.

| Comprobación | Resultado |
|---|---|
| `spotlessCheck testDebugUnitTest koverVerifyDebug assembleDebug assembleDebugAndroidTest` | Correcto |
| `AccountCreationScreenTest` en Android 16/API 36 | 11 pruebas, 0 fallos, 0 errores y 0 omitidas |
| `ConversationsScreenTest` en Android 16/API 36 | 4 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Revisión manual en emulador | Pendiente |
| Revisión por otra persona | Pendiente |

# Integración productiva de T-38 (RNF-03, RNF-04, RNF-09; issue #61)

Fecha: 18 de octubre de 2026. Rama: `yang_dev`. Seguimiento: #27.

- `FlowPerformanceSummary` exige al menos 20 muestras, calcula p95 mediante
  *nearest rank* y aplica literalmente los límites menores de 2 s y 3 s.
- `FirebasePerformanceRecoveryTest` recorre los repositorios productivos de
  conversaciones, texto y audio contra Auth, Firestore, Functions y Storage.
  Tras tres calentamientos mide 20 cargas y 20 envíos, y deja el detalle en un
  JSON privado que CI adjunta al informe instrumentado.
- Un endpoint local inalcanzable simula la pérdida de conexión. Tras recuperar
  el emulador, el texto se reintenta cuatro veces de forma concurrente con el
  mismo UUID y el audio conserva su temporal hasta confirmar el mismo UUID.
  Firestore termina con dos mensajes y dos incrementos de no leídos, sin copias
  adicionales.
- La conversación elegida usa estado guardable durante recreación; el borrador
  reside en `SavedStateHandle`. Una salida durante subida conserva un fallo
  reintentable y la siguiente operación reutiliza el UUID.
- Los datos son sintéticos y el log solo publica los dos p95; no contiene texto,
  teléfono ni rutas privadas.

| Comprobación | Resultado |
|---|---|
| `spotlessCheck testDebugUnitTest koverVerifyDebug lintDebug assembleDebug assembleDebugAndroidTest` | Correcto; 110 pruebas JVM, 0 fallos, 0 errores y 0 omitidas; cobertura y lint en verde |
| Functions: `npm run build` y prueba TypeScript compilada con `node --test` | Correcto; 4 pruebas, 0 fallos y 0 omitidas. `npm test` no arranca localmente con Node 24 por `uv_os_get_passwd returned ENOMEM`; CI usa Node 22 |
| `FirebasePerformanceRecoveryTest` en `NAMI_API_36`, Android 16/API 36 | 2 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Contenido inicial, 20 muestras + 3 calentamientos | mín. 8 ms; media 9,45 ms; p95 11 ms; máx. 15 ms; objetivo < 2.000 ms, correcto |
| Confirmación de envío, 20 muestras + 3 calentamientos | mín. 23 ms; media 25,90 ms; p95 30 ms; máx. 31 ms; objetivo < 3.000 ms, correcto |
| Pérdida/recuperación de texto y audio | Correcto; temporal conservado y mismo UUID al reintentar |
| Reintentos concurrentes | Correcto; sin mensajes ni contadores duplicados |
| Recreación de borrador y salida durante subida con fakes | Correcto en `TextConversationScreenTest` |
| Suite instrumentada completa con Emulator Suite | 78 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Ejecución en dispositivo físico y modo avión real | Pendiente; instrucciones en `SSD/RENDIMIENTO.md` |
| Revisión por otra persona | Pendiente |

Esta evidencia satisface la ejecución automatizada en un emulador y un tamaño
representativo. No sustituye el paso pendiente en hardware físico ni permite
marcar todavía como completa la revisión humana de la definición de hecho.

# Integración de T-39 (RNF-01, RNF-02; CA-06; issue #62)

Fecha: 18 de octubre de 2026. Rama: `yang_dev`. Seguimiento: #28.

- Las pruebas Compose de cuenta, inicio, contacto, chat, audio, perfil y ajustes
  habilitan Accessibility Test Framework en Android 16/API 36.
- Los siete flujos tienen casos a 320 dp y 411 dp con texto al 200 %, incluidos
  éxito, vacío, error y reintento. Los escenarios a escala máxima ejecutan la
  revisión del árbol semántico completo.
- El perfil corrige dos hallazgos: el estado de error es desplazable con áreas
  seguras y sus errores de carga/avatar se anuncian mediante regiones vivas.
- Los componentes esenciales mantienen al menos 48 × 48 dp, texto visible para
  sus iconos y acciones de una pulsación. No hay acciones esenciales basadas
  solo en color o pulsación prolongada.
- El flujo de texto necesita como máximo cuatro acciones y el de audio cinco;
  el desglose está registrado en `SSD/ACCESIBILIDAD.md`.

| Comprobación | Resultado |
|---|---|
| `spotlessApply assembleDebugAndroidTest` | Correcto |
| Suite `com.proyectonami.app.feature` en `NAMI_API_36` | 54 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Suite instrumentada completa con Emulator Suite | 85 pruebas, 0 fallos, 0 errores y 0 omitidas |
| JVM, cobertura y lint | 110 pruebas, 0 fallos, 0 errores y 0 omitidas; `koverVerifyDebug` y `lintDebug` correctos |
| Anchos y escala | 320 dp y 411 dp al 200 % en todos los flujos auditados |
| Fallos críticos automáticos abiertos | 0 |
| Recorrido auditivo con TalkBack real | Pendiente de firma humana |
| Repetición por una segunda persona | Pendiente de firma humana |

La evidencia detallada, el entorno, las correcciones y la tabla que deben
firmar ambas personas están en [`SSD/ACCESIBILIDAD.md`](SSD/ACCESIBILIDAD.md).
La suite automática queda cerrada; la definición de hecho completa permanece
pendiente hasta escuchar el recorrido con TalkBack y registrar la revisión
independiente.

# Regresión final integrada de T-40 (todos los RF/RNF y CA; issue #63)

Fecha: 18 de octubre de 2026. Rama: `yang_dev`. Seguimiento: #29 y #30.

- El nuevo recorrido
  `emptySuiteJourneyCreatesUsersConversationTextAndAudioWithoutSeeding` parte
  de una Emulator Suite vacía, crea dos cuentas ficticias, vincula el contacto,
  abre la conversación mediante `openConversation`, envía texto y audio,
  descarga el audio y deniega ambos recursos a una tercera cuenta.
- El recorrido usa los repositorios y funciones productivos y no llama a
  `seedConversationForTests`. Esa función queda limitada a fixtures
  especializados y rechaza cualquier entorno que no sea el emulador.
- Se revisó el cableado completo de autenticación, perfil, contactos,
  conversaciones, texto, grabación, subida y reproducción de audio, TTS y
  preferencias. La matriz está en `SSD/TRAZABILIDAD_FINAL.md`.
- CI fija Node 22 tanto en calidad como en Emulator Suite. La comprobación
  local limpia se ejecutó con Node 22.23.3; el Firebase CLI instalado en el
  equipo anfitrión avisó de que su propio proceso local usa Node 24, sin
  afectar al resultado del emulador.
- La revisión de fuentes no encontró logs productivos con teléfonos, mensajes,
  tokens ni rutas privadas. Las pruebas y documentación usan datos ficticios.
- Se regeneró `entrega_G06/NAMI-debug.apk` con el código integrado.

| Comprobación | Resultado |
|---|---|
| Functions con Node 22.23.3: instalación limpia, `npm test` y `npm run build` | Correcto; 4 pruebas, 0 fallos y 0 omitidas; TypeScript compila |
| `spotlessCheck assembleDebug assembleRelease testDebugUnitTest lintDebug koverVerifyDebug assembleDebugAndroidTest` | Correcto |
| JVM | 110 pruebas, 0 fallos, 0 errores y 0 omitidas |
| Recorrido vacío T-40 aislado | 1 prueba, 0 fallos, 0 errores y 0 omitidas |
| Suite instrumentada completa con Auth, Firestore, Functions y Storage | 86 pruebas, 0 fallos, 0 errores y 0 omitidas; 7 min 49 s |
| APK debug | SHA-256 `C87AF29B620A18658213D03B549D4ED13E3DC458BC16742736338661DC794BE5` |
| TalkBack real, dispositivo físico/API 26 y revisión por otra persona | Pendiente de firma humana |
| Acceso docente al repositorio y artefacto | Pendiente de confirmación externa |

La regresión técnica queda en verde, pero T-40 y el issue #63 no deben cerrarse
hasta registrar en `SSD/ACCESIBILIDAD.md` y `SSD/RENDIMIENTO.md` las
comprobaciones humanas y físicas pendientes.

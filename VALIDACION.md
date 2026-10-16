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

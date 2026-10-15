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
| `gradlew.bat connectedDebugAndroidTest` | 1 fallo de infraestructura en Medium_Phone, Android 17 |
| `git diff --check` | Correcto |

Las advertencias de lint son: objetivo Android anterior al último disponible, versiones más recientes de Core KTX y del compilador Compose, ausencia de reglas de extracción de datos y ausencia de icono de aplicación. La compilación también avisa de la API obsoleta `createAndroidComposeRule`.

La prueba instrumentada falla antes de comprobar la pantalla por `NoSuchMethodException: android.hardware.input.InputManager.getInstance`. Solo está instalada la imagen Android 37.2; queda pendiente repetirla con Android 16/API 36 o una infraestructura de pruebas compatible con Android 17. No se puede afirmar que la validación de interfaz haya pasado.

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
| `gradlew.bat connectedDebugAndroidTest` (incluye `NamiComponentsTest`) | Pendiente; ver el fallo de infraestructura en Android 17 descrito arriba |
| Revisión manual de las vistas previas y en emulador al 200 % | Pendiente |
| Comparación de la paleta con el prototipo de Figma | Pendiente: la paleta es provisional |

No se añadieron dependencias: los iconos son vectores locales en `res/drawable`.

# Validación de T-06 (RF-01)

Fecha: 15 de octubre de 2026. Rama: `t-06-validacion-cuenta`, creada sobre `main` (`ce8eeec`). Cambios generados con ayuda de IA (DeepSeek Harness) y pendientes de revisión humana.

Qué incluye:

- `core.domain.validation`: `PersonNameValidator` y `PhoneNumberValidator`, con Kotlin puro (sin Android ni Firebase) para poder probarlos en la JVM y reutilizarlos en cuenta y contactos.
- Nombres y apellidos: recorte, espacios repetidos reducidos a uno, entre 1 y 50 caracteres Unicode completos, letras de cualquier alfabeto con marcas de acento, espacios, guion, apóstrofo y punto. Se rechazan cifras, emojis y símbolos, y los separadores no pueden abrir, cerrar ni repetirse.
- Teléfono: normalización a E.164 con `+` opcional, prefijo de país opcional (`defaultCallingCode`), entre 7 y 15 cifras y primera cifra distinta de 0. Admite espacios y `+ - ( ) .` y convierte cifras de otros alfabetos.
- Los validadores devuelven un motivo (`PersonNameError`, `PhoneNumberError`) en lugar de texto, y `strings.xml` incluye un mensaje comprensible por motivo con la convención `validation_person_name_<motivo>` y `validation_phone_number_<motivo>`. La pantalla que resuelve esos textos llega en T-08.
- `app/build.gradle.kts`: el filtro de Kover de T-03 (RNF-08) se amplía a `core.domain.*`, porque solo cubría `feature.*.domain` y el primer paquete de dominio del proyecto es este.
- `plan.md` documenta el paquete `core.domain` y la decisión de no añadir una librería de numeración telefónica; `spec.md` añade la duda pendiente sobre prefijo internacional.

| Comprobación | Resultado |
|---|---|
| `./gradlew testDebugUnitTest` | Correcto: 33 pruebas, 0 fallos (28 nuevas de T-06) |
| `./gradlew koverVerifyDebug` | Correcto: 100 % de líneas en `core.domain.validation` (108/108) |
| `./gradlew lintDebug` | Sin errores; 17 advertencias, ninguna en el código nuevo |
| `./gradlew assembleDebug` | Correcto |
| `./gradlew spotlessCheck` | Falla por problemas preexistentes ajenos a T-06 (ver abajo) |
| `./gradlew :app:spotlessKotlinCheck` | Los archivos de T-06 no aparecen; falla en archivos de T-02/T-04 |
| Revisión manual en dispositivo o emulador | No procede en esta tarea: T-06 no añade pantalla, el flujo se revisa en T-08 |
| Revisión por otra persona | Pendiente |

## Formato: fallo preexistente

`spotlessCheck` ya fallaba en `main` antes de esta rama:

- `:spotlessKotlinGradleCheck` señala la línea en blanco final de `build.gradle.kts` y `settings.gradle.kts` (corregible con `spotlessApply`).
- `:app:spotlessKotlinCheck` señala violaciones en `MainActivity.kt`, `Contrast.kt`, `NamiColorTokens.kt`, `NamiTypography.kt`, `MainActivityTest.kt`, `NamiComponentsTest.kt`, `AppConfigurationTest.kt` y `NamiColorContrastTest.kt` (T-02/T-04), además de cinco errores `ktlint(standard:function-naming)` en funciones `@Composable` con nombre en mayúscula (`NamiPrimaryButton`, `NamiComponentsShowcase`, `NamiTextField`, `NamiTheme`, `NamiApp`).

Ninguno de esos archivos pertenece a T-06: los archivos nuevos no aparecen en el informe. Arreglarlo implica tocar la configuración de T-03 y reformatear ocho archivos de T-02/T-04, así que se deja fuera de este cambio y se propone como tarea aparte.

## Limitaciones

- No se validan prefijos ni longitudes por país: haría falta una tabla internacional o `libphonenumber`, que es una dependencia nueva y exige justificación en `plan.md`.
- Queda en `spec.md` (punto 7) la decisión de si el prefijo internacional es obligatorio u opcional; T-08 debe elegir.
- Los mensajes todavía no se han visto en pantalla: T-06 solo garantiza que cada motivo tiene texto, con `ValidationMessagesTest`.

Nota de entorno: las comprobaciones se ejecutaron en Linux con `./gradlew` (equivalente a `gradlew.bat`) y con `GRADLE_USER_HOME` y `ANDROID_USER_HOME` dentro del proyecto, porque `$HOME` era de solo lectura en el entorno usado; `.gradle/` está ignorado y no se modificó ningún archivo versionado por ese motivo.

Informes locales: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/kover/reportDebug.xml` y `app/build/reports/lint-results-debug.html`.

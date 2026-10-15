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

Fuera de alcance y pendiente: pruebas exhaustivas de todas las operaciones y combinaciones de participantes (T-26), flujo telefónico (T-07) y tamaño o duración máximos del audio en `storage.rules` (pregunta abierta de `SSD/spec.md`). La revisión de otra persona del equipo sigue siendo necesaria antes de fusionar.

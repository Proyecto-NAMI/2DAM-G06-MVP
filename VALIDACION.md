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
| `gradlew.bat assembleDebug` | Pendiente de ejecutar en local |
| `gradlew.bat testDebugUnitTest` (incluye `NamiColorContrastTest`) | Pendiente de ejecutar en local |
| `gradlew.bat lintDebug` | Pendiente de ejecutar en local |
| `gradlew.bat connectedDebugAndroidTest` (incluye `NamiComponentsTest`) | Pendiente; ver el fallo de infraestructura en Android 17 descrito arriba |
| Revisión manual de las vistas previas y en emulador al 200 % | Pendiente |
| Comparación de la paleta con el prototipo de Figma | Pendiente: la paleta es provisional |

No se añadieron dependencias: los iconos son vectores locales en `res/drawable`.

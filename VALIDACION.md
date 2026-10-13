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

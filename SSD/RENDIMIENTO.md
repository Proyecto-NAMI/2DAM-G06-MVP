# T-38: rendimiento y recuperación del flujo productivo

## Alcance y criterio

La medición atraviesa los repositorios productivos contra Firebase Emulator
Suite vacío. Se descartan tres iteraciones de calentamiento y se conservan 20
muestras independientes por tramo. El percentil 95 se calcula con *nearest
rank*; la prueba falla con menos de 20 muestras o cuando el p95 no es
estrictamente menor que el objetivo.

| Tramo | Objetivo | Muestra | Mín. | Media | p95 | Máx. |
|---|---:|---:|---:|---:|---:|---:|
| Cuenta autenticada hasta lista con contenido | < 2.000 ms | 20 + 3 calentamientos | 8 ms | 9,45 ms | **11 ms** | 15 ms |
| Envío hasta confirmación remota | < 3.000 ms | 20 + 3 calentamientos | 23 ms | 25,90 ms | **30 ms** | 31 ms |

Entorno de esta evidencia, capturada el 18/10/2026: `NAMI_API_36 (AVD)`, modelo
`sdk_gphone64_x86_64`, Android 16/API 36, 411 × 914 dp, proyecto ficticio
`demo-nami` y Auth, Firestore, Functions y Storage locales. Estas
cifras caracterizan el entorno local, no la latencia de Firebase en producción.
El informe JSON conserva también mínimo, media, máximo y las 20 observaciones,
sin teléfonos, texto real ni rutas privadas.

## Recuperación e idempotencia

`FirebasePerformanceRecoveryTest` fuerza un endpoint Firebase inalcanzable y
después recupera el servicio real del emulador:

| Escenario | Resultado esperado y comprobado |
|---|---|
| Pérdida de red durante texto | Estado `NETWORK`; el reintento reutiliza el UUID y se confirma. |
| Cuatro reintentos concurrentes de texto | Un solo documento y un solo incremento de no leídos. |
| Pérdida de red durante audio | Estado recuperable y temporal privado conservado. |
| Reintento de audio | Mismo UUID, una sola copia remota y eliminación del temporal tras éxito. |
| Recreación con borrador | `SavedStateHandle` conserva el texto no enviado y la conversación seleccionada se restaura. |
| Salida durante subida | El estado queda fallido/reintentable; el segundo intento conserva el UUID. |
| Grabación al recrear o salir | El ViewModel retenido mantiene el indicador; una salida explícita detiene y libera la sesión. |

Al terminar la recuperación se consultan Firestore y sus contadores desde
servidor: existen exactamente un mensaje de texto y uno de audio, y el contador
del destinatario vale dos. No se considera confirmada una escritura local en
caché.

## Ejecución reproducible

Con un emulador Android arrancado:

```powershell
firebase emulators:exec `
  --config firebase.json `
  --project demo-nami `
  --only auth,firestore,functions,storage `
  '.\gradlew.bat connectedDebugAndroidTest "-PNAMI_FIREBASE_EMULATOR_HOST=10.0.2.2" "-Pandroid.testInstrumentationRunnerArguments.class=com.proyectonami.app.FirebasePerformanceRecoveryTest"'
```

La ejecución completa de CI usa `.github/scripts/run-instrumented-tests.sh`.
La prueba guarda una copia privada en el dispositivo y publica en su log
instrumentado únicamente el JSON sintético de métricas. Después de una suite
verde, el script extrae esa línea a
`app/build/reports/performance/t38-performance.json` y la publica dentro del
artefacto `android-instrumented-test-reports`. Si falta o está vacío, el trabajo
falla. No se usa `run-as` tras `connectedDebugAndroidTest`, porque Gradle
desinstala la aplicación al terminar.

## Limitaciones y revisión manual

- La caída de red está automatizada mediante endpoints locales inalcanzables;
  conviene repetir manualmente con modo avión para observar también la UX.
- La ejecución en hardware físico, el audio real y la revisión por otra persona
  siguen pendientes. No se presentan como completadas por una prueba de AVD.
- En el dispositivo físico se debe repetir la orden usando la IP LAN del equipo
  en `NAMI_FIREBASE_EMULATOR_HOST`, registrar modelo, API, tamaño y resultados,
  y adjuntar el JSON a la revisión.

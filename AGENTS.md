# Instrucciones para asistentes de IA - NAMI

## 1. Contexto

NAMI es un MVP Android de mensajería familiar accesible, pensado especialmente para personas mayores con visión reducida, menor precisión motora o poca experiencia tecnológica. El objetivo no es competir con aplicaciones de mensajería generalistas, sino permitir una comunicación directa mediante texto y notas de voz con el mínimo de decisiones por pantalla.

Equipo: grupo G06. Las aportaciones individuales se identificarán con códigos `G06-xx` cuando el equipo los confirme.

## 2. Documentos que se deben leer

Antes de proponer o modificar código, leer en este orden:

1. `constitution.md`: reglas no negociables.
2. `spec.md`: alcance, requisitos y aceptación.
3. `plan.md`: arquitectura, tecnologías y datos.
4. `tasks.md`: unidad de trabajo, dependencias y responsable.
5. Prototipo de Figma: <https://www.figma.com/design/bGCnnDncM5CUdgVfMmRgz3/NAMI?node-id=0-1&t=NXeWbgK48RRls40z-1>.

Si los documentos discrepan, no elegir una interpretación en silencio. Señalar el conflicto y aplicar el orden de prioridad de `constitution.md`.

## 3. Tecnologías acordadas

- Kotlin, Android, Jetpack Compose y Material 3.
- Arquitectura por capas con ViewModel, casos de uso y repositorios.
- Coroutines y Flow para asincronía y estado.
- Firebase Authentication por teléfono, Cloud Firestore y Firebase Storage.
- DataStore para preferencias locales.
- JUnit y fakes, Compose UI Test y Firebase Emulator Suite.

No sustituir estas tecnologías ni añadir una dependencia sin justificarlo en `plan.md` y obtener revisión humana.

## 4. Convenciones

- Producto y documentación en español; código y nombres técnicos en inglés claro.
- Requisitos `RF-##` y `RNF-##`; tareas `T-##`.
- Paquetes por funcionalidad y capa, por ejemplo `feature.chat.presentation`, `feature.chat.domain` y `feature.chat.data`.
- Estado de pantalla inmutable expuesto por `StateFlow`; eventos de una sola vez representados explícitamente.
- Composables pequeños, sin acceso directo a Firebase ni lógica de negocio.
- Repositorios definidos por interfaces e implementaciones inyectadas.
- Textos visibles en recursos de cadenas, no escritos directamente en composables.
- No registrar tokens, teléfonos, contenido de mensajes ni rutas privadas de audio.
- Usar datos ficticios en pruebas y capturas.
- Mantener cada cambio limitado a una tarea o conjunto estrechamente relacionado.

## 5. Comportamiento esperado del asistente

1. Identificar el requisito y la tarea antes de editar.
2. Inspeccionar la estructura existente y reutilizar patrones del proyecto.
3. Proponer el cambio mínimo que satisfaga la aceptación sin ampliar el MVP.
4. Tratar entradas, permisos, red, reintentos y estados vacíos o de error.
5. Añadir o actualizar pruebas proporcionales al riesgo.
6. Ejecutar las comprobaciones disponibles y comunicar exactamente cuáles pasaron o no pudieron ejecutarse.
7. Resumir archivos modificados, trazabilidad y riesgos pendientes.

Ante una duda que afecte al alcance, privacidad, datos o experiencia principal, detener la implementación y formular una pregunta concreta. Ante una duda menor, elegir la opción más simple y accesible y documentar la suposición.

## 6. Comprobaciones

Comandos de calidad para la configuración Gradle actual:

```text
FORMATO_CHECK=./gradlew spotlessCheck (Windows: .\gradlew.bat spotlessCheck)
FORMATO_APPLY=./gradlew spotlessApply (Windows: .\gradlew.bat spotlessApply)
ANALISIS=./gradlew lintDebug (Windows: .\gradlew.bat lintDebug)
TEST_UNITARIO=./gradlew testDebugUnitTest (Windows: .\gradlew.bat testDebugUnitTest)
TEST_UI=./gradlew connectedDebugAndroidTest (Windows: .\gradlew.bat connectedDebugAndroidTest; requiere emulador o dispositivo conectado)
BUILD=./gradlew assembleDebug (Windows: .\gradlew.bat assembleDebug)
EMULADORES_FIREBASE=Pendiente de T-05: aún no hay firebase.json ni reglas de emulador.
```

CI ejecuta formato, lint, pruebas unitarias, pruebas instrumentadas en emulador y compilación debug. En Windows se usa `gradlew.bat` en lugar de `./gradlew`.

## 7. Revisión de cambios generados con IA

- [ ] Pertenece al alcance de `spec.md` y señala tarea/requisitos.
- [ ] No aparecen secretos ni datos personales reales.
- [ ] Las operaciones remotas comprueban autenticación y autorización.
- [ ] La interfaz conserva texto escalable, contraste y objetivos táctiles adecuados.
- [ ] Los estados de carga, vacío, éxito y error están cubiertos.
- [ ] Las pruebas relevantes se ejecutaron o se indicó la limitación.
- [ ] Una persona del equipo revisó la propuesta.

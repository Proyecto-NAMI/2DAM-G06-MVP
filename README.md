# NAMI

Aplicación Android MVP para comunicación familiar accesible, diseñada para personas mayores, con prioridad en mensajes de texto y notas de voz, controles grandes, contraste y flujo sencillo.

## Descripción del proyecto

NAMI es una app de mensajería privada entre una persona mayor y sus contactos de confianza. La experiencia se centra en:

- Cuenta y sesión con verificación por teléfono
- Perfil personal con nombre, apellidos y teléfono
- Contactos de confianza
- Conversaciones 1:1
- Mensajes de texto
- Grabación, revisión y envío de notas de voz
- Accesibilidad: texto grande, lectura en voz alta y contrastes adecuados
- Seguridad por autenticación y reglas de Firebase

El proyecto se desarrolla en Kotlin con Jetpack Compose y Material 3, usando Firebase Authentication, Firestore y Storage, con configuración preparada para Firebase Emulator Suite en `debug`.

## Estado actual

Este repositorio contiene el candidato técnico del MVP revisado en T-40. El
cierre de entrega sigue condicionado a TalkBack, dispositivo físico, acceso
docente y revisión por otra persona.

Incluye:

- Aplicación Android con Compose y Material 3
- Configuración del proyecto Android y Gradle
- Soporte de Firebase en entorno local con emuladores
- Validación de identidad y sesión por teléfono
- Modelo de datos y repositorios para Auth, Firestore y Storage
- Tema y componentes de diseño accesibles
- Alta de contactos con validación de los tres campos, aviso de duplicado, vinculación con cuentas existentes y confirmación
- Reglas y documentación de seguridad y privacidad
- Estructura de trazabilidad del proyecto en la carpeta `SSD/`
- Paquete de entrega en `entrega_G06/`, incluida la presentación PDF y el APK debug disponible

## Stack tecnológico

- Kotlin
- Jetpack Compose
- Material 3
- Android SDK min 26 / target 36
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- Gradle 9.6.0
- AGP 9.4.1
- JDK 17+

## Estructura del repositorio

```text
2DAM-G06-MVP/
├── app/                         # Módulo principal de la aplicación Android
│   ├── build.gradle.kts
│   └── src/
├── SSD/                        # Documentación del proyecto y SDD
│   ├── README.md
│   ├── constitution.md
│   ├── plan.md
│   ├── spec.md
│   ├── tasks.md
│   └── uso-ia.md
├── evidencias-ia/              # Evidencias visuales del proceso
├── Prototype_Reference/        # Capturas del prototipo de baja fidelidad
├── functions/                  # Cloud Functions: alta de contactos (T-11)
├── FIREBASE.md                 # Guía de Firebase y emuladores
├── build.gradle.kts            # Configuración raíz de Gradle
├── firebase.local.properties.example
├── firebase.json               # Configuración del proyecto Firebase
├── firestore.rules            # Reglas de Firestore
├── firestore.indexes.json
├── gradlew / gradlew.bat
├── local.properties.example
├── settings.gradle.kts
├── storage.rules
├── README.md                  # Este documento
└── ...
```

## Requisitos previos

- Android Studio compatible con AGP 9.4+
- Android SDK Platform 37 y compilación compatible
- JDK 17 o superior
- Firebase CLI instalado si se va a usar Emulator Suite
- Conexión a internet durante la primera sincronización

## Preparación local

1. Clona el repositorio:

```bash
git clone https://github.com/Proyecto-NAMI/2DAM-G06-MVP.git
cd 2DAM-G06-MVP
```

2. Configura el SDK local:

- Crea `local.properties` a partir de `local.properties.example`
- Asegúrate de que la ruta del Android SDK sea correcta

3. Si vas a trabajar con Firebase localmente:

- Crea `firebase.local.properties` a partir de `firebase.local.properties.example`
- Revisa la guía de Firebase en [FIREBASE.md](FIREBASE.md)

4. Sincroniza Gradle y abre el proyecto en Android Studio

## Compilación y pruebas

Windows:

```powershell
./gradlew.bat assembleDebug
./gradlew.bat testDebugUnitTest
```

macOS / Linux:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Para Firebase local con emuladores (incluye la callable de Functions):

```powershell
firebase emulators:start --project demo-nami --only auth,firestore,functions,storage
```

Y luego, en otra terminal:

```powershell
./gradlew.bat connectedDebugAndroidTest
```

Para reproducir la ejecución completa, usa Node 22 para Functions:

```powershell
Push-Location functions
node --version # debe empezar por v22
npm ci
npm test
npm run build
Pop-Location
firebase emulators:exec --config firebase.json --project demo-nami --only auth,firestore,functions,storage ".\gradlew.bat connectedDebugAndroidTest"
```

La prueba
`FirebaseEmulatorSmokeTest.emptySuiteJourneyCreatesUsersConversationTextAndAudioWithoutSeeding`
demuestra el recorrido principal sin crear manualmente una conversación.

El APK debug queda en `app/build/outputs/apk/debug/app-debug.apk`. Para registrar su huella:

```powershell
Get-FileHash .\app\build\outputs\apk\debug\app-debug.apk -Algorithm SHA256
```

La regresión de CI que valida compilación, JVM y Emulator Suite se enlaza desde
[`VALIDACION.md`](VALIDACION.md). Si el SDK local no tiene las plataformas o Build Tools requeridas,
se debe usar Android Studio o el runner de CI en lugar de relajar las comprobaciones.

## Funcionalidades clave del MVP

### Autenticación y sesión

- Registro con nombre, apellidos y teléfono
- Verificación por código
- Mantener sesión activa en la app
- Reintentos y mensajes de error claros

### Perfil y contactos

- Consulta y actualización de datos del usuario
- Alta manual de contactos de confianza
- Validación de nombre y teléfono
- Prevención de duplicados
- Vinculación con cuentas existentes mediante la función `addContact`: el servidor comprueba el teléfono con App Check y cuota por UID, y el cliente nunca escribe contactos
- Creación idempotente de conversación mediante `openConversation`, limitada a contactos propios vinculados y sin escrituras directas del cliente

### Mensajería

- Conversaciones privadas uno a uno
- Envío de mensajes de texto
- Envío de notas de voz con grabación, reproducción previa e historial reproducible
- Confirmación visible del estado de envío
- Reintentos ante errores

### Accesibilidad

- Texto grande con ajustes locales
- Pantalla de Ajustes con tamaño normal, grande o máximo (200 %)
- Botones grandes y acciones simples
- Lectura en voz alta de mensajes de texto con el motor del sistema y preferencia persistente
- Contraste y diseño pensados para usuarios mayores

### Seguridad y privacidad

- Firebase Auth con sesión obligatoria
- Reglas de Firestore y Storage limitando acceso por propietario
- Restricción de datos sensibles
- Uso de emuladores en entorno de desarrollo
- No se almacenan credenciales ni datos reales en el repositorio

## Documentación del proyecto

- [SSD/README.md](SSD/README.md): resumen general del proyecto y del flujo de trabajo
- [SSD/spec.md](SSD/spec.md): especificación funcional del MVP
- [SSD/plan.md](SSD/plan.md): arquitectura y estrategia
- [SSD/tasks.md](SSD/tasks.md): trazabilidad de tareas
- [SSD/constitution.md](SSD/constitution.md): principios del proyecto
- [SSD/uso-ia.md](SSD/uso-ia.md): uso de IA en el desarrollo
- [SSD/DESIGN_THINKING.md](SSD/DESIGN_THINKING.md): decisiones de Design Thinking y límites del piloto
- [SSD/FIGMA.md](SSD/FIGMA.md): enlace y capturas esenciales de Figma
- [FIREBASE.md](FIREBASE.md): configuración segura de Firebase y emuladores
- [evidencias-ia/](evidencias-ia/): evidencias visuales del proceso
- [entrega_G06/](entrega_G06/): presentación, manifiesto Moodle y APK debug disponible

## Notas importantes

- El proyecto es un candidato técnico de T-40; la revisión humana final, el
  dispositivo físico y el acceso docente se deben confirmar externamente.
- No se activan funciones excluidas como grupos, llamadas, vídeo, adjuntos no previstos o cifrado E2E propio.
- La configuración sensible no se sube al repositorio (`local.properties`, `google-services.json`, `.firebaserc`, credenciales, logs, etc.).
- En `debug` el proyecto usa valores ficticios y emuladores. `release` exige configuración real y no usa emuladores.

## Enlaces útiles

- Prototipo Figma de NAMI: https://www.figma.com/design/bGCnnDncM5CUdgVfMmRgz3/NAMI
- [Prototype_Reference/](Prototype_Reference/): capturas locales del prototipo. Es un boceto de baja fidelidad y sirve como referencia de contenido y flujo, no de estilo ni de interacción: la implementación debe mejorar el estilo visual y la interacción con los tokens accesibles de `core.designsystem` (RNF-01) y respetar las exclusiones de `SSD/spec.md`.

## Licencia

Este proyecto se encuentra en desarrollo interno del curso 2DAM-G06 y no especifica licencia pública en el repositorio actual.

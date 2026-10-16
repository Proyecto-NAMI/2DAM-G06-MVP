# NAMI - MVP Android del grupo G06

NAMI es una aplicación Android de mensajería familiar accesible, pensada para personas mayores con visión reducida, menor precisión motora o poca experiencia tecnológica.

Esta rama contiene la base técnica de **T-02** y la configuración Firebase de **T-05**. El MVP usa Kotlin, Jetpack Compose y Material 3. Authentication, Firestore y Storage se abstraen mediante repositorios inyectables y, en `debug`, se conectan únicamente al Emulator Suite con datos ficticios.

## Configuración Android

| Parámetro | Valor |
|---|---:|
| Android mínimo | Android 8.0, API 26 |
| Android objetivo | Android 16, API 36 |
| SDK de compilación | API 37 |
| JDK | 17 o posterior compatible |
| Gradle | 9.6.0 |
| Android Gradle Plugin | 9.4.1 |

La versión mínima debe validarse en los dispositivos reales del piloto antes de cerrar RNF-07.

## Requisitos previos

- Android Studio compatible con AGP 9.4.
- Android SDK Platform 37 y Build Tools 36.0.0.
- JDK 17 o superior compatible; puede utilizarse el JDK incluido en Android Studio.
- Acceso a Internet durante la primera sincronización de dependencias.

## Preparación local

1. Clona el repositorio y cambia a la rama de trabajo:

   ```bash
   git clone https://github.com/Proyecto-NAMI/2DAM-G06-MVP.git
   cd 2DAM-G06-MVP
   git switch yang_dev
   ```

2. Abre el proyecto en Android Studio o crea `local.properties` a partir de `local.properties.example` con la ruta local del SDK.
3. Sincroniza Gradle.

No subas `local.properties`, `google-services.json`, almacenes de claves ni credenciales. La configuración sensible está excluida mediante `.gitignore`.

Para Firebase local, consulta [`FIREBASE.md`](FIREBASE.md) y parte de `firebase.local.properties.example` y `.firebaserc.example`. Los valores incluidos son ficticios; el build `release` no recibe credenciales desde el repositorio.

## Compilación y pruebas

En Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
```

En macOS o Linux:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
```

La prueba instrumentada `FirebaseEmulatorSmokeTest` está preparada para ejecutarse con
Firebase Emulator Suite y un emulador Android activos; el flujo telefónico completo
se implementará en T-07.

## Documentación

| Archivo | Contenido |
|---|---|
| [`constitution.md`](constitution.md) | Principios obligatorios del proyecto. |
| [`AGENTS.md`](AGENTS.md) | Reglas para asistentes de IA. |
| [`spec.md`](spec.md) | Alcance, requisitos y criterios de aceptación. |
| [`plan.md`](plan.md) | Arquitectura, tecnologías y estrategia de pruebas. |
| [`tasks.md`](tasks.md) | Tareas, dependencias y trazabilidad. |
| [`uso-ia.md`](uso-ia.md) | Registro crítico del uso de IA. |
| [`FIREBASE.md`](FIREBASE.md) | Configuración segura de Firebase y Emulator Suite. |
| [`evidencias-ia/`](evidencias-ia/) | Evidencias visuales del proceso SDD. |

## Prototipo

- [Prototipo NAMI en Figma](https://www.figma.com/design/bGCnnDncM5CUdgVfMmRgz3/NAMI?node-id=0-1&t=NXeWbgK48RRls40z-1)
- [`Prototype_Reference/`](../Prototype_Reference): seis capturas locales (creación de cuenta, nuevo contacto, conversaciones y pantalla principal, chat, foto de perfil y modificación y opciones). Son imágenes JPEG con extensión `.png`; se conservan tal como las entregó el equipo.

El prototipo es un boceto muy preliminar. Fija el contenido y el flujo de cada pantalla, pero no su estilo ni su forma de interactuar: la implementación debe mejorar el estilo visual y la interacción con los tokens accesibles de T-04 y RNF-01, sin copiar los tamaños absolutos del boceto, y respetar las exclusiones de `spec.md` §3 (los iconos de cámara, llamada y vídeo no se activan en el MVP).

## Estado y limitaciones

- T-02 establece la base del proyecto y una pantalla mínima.
- T-05 configura Authentication, Firestore y Storage para el Emulator Suite; el flujo de teléfono se implementará en T-07.
- No se incluyen datos personales, teléfonos reales, conversaciones ni credenciales.
- La prueba instrumentada requiere Firebase CLI y un emulador Android en ejecución.
- La revisión manual y la revisión por otra persona siguen pendientes antes de cerrar la tarea.


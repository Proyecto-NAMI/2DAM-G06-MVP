# NAMI - MVP Android del grupo G06

NAMI es una aplicación Android de mensajería familiar accesible, pensada para personas mayores con visión reducida, menor precisión motora o poca experiencia tecnológica.

Esta rama contiene la base técnica de la tarea **T-02**, trazada con **RNF-07**. El MVP usa Kotlin, Jetpack Compose y Material 3. En esta fase solo se muestra una pantalla inicial mínima; Firebase, navegación y funcionalidades de mensajería pertenecen a tareas posteriores.

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

   ```

2. Abre el proyecto en Android Studio o crea `local.properties` a partir de `local.properties.example` con la ruta local del SDK.
3. Sincroniza Gradle.

No subas `local.properties`, `google-services.json`, almacenes de claves ni credenciales. La configuración sensible está excluida mediante `.gitignore`.

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

La prueba instrumentada se ejecutará en una tarea posterior con un emulador o dispositivo configurado.

## Documentación

| Archivo | Contenido |
|---|---|
| [`constitution.md`](constitution.md) | Principios obligatorios del proyecto. |
| [`AGENTS.md`](AGENTS.md) | Reglas para asistentes de IA. |
| [`spec.md`](spec.md) | Alcance, requisitos y criterios de aceptación. |
| [`plan.md`](plan.md) | Arquitectura, tecnologías y estrategia de pruebas. |
| [`tasks.md`](tasks.md) | Tareas, dependencias y trazabilidad. |
| [`uso-ia.md`](uso-ia.md) | Registro crítico del uso de IA. |
| [`evidencias-ia/`](evidencias-ia/) | Evidencias visuales del proceso SDD. |

## Prototipo

[Prototipo NAMI en Figma](https://www.figma.com/design/bGCnnDncM5CUdgVfMmRgz3/NAMI?node-id=0-1&t=NXeWbgK48RRls40z-1)

## Estado y limitaciones

- T-02 establece la base del proyecto y una pantalla mínima.
- No hay servicios Firebase configurados en esta tarea.
- No se incluyen datos personales, teléfonos reales, conversaciones ni credenciales.
- La revisión manual en dispositivo/emulador y la revisión por otra persona siguen pendientes antes de cerrar la tarea.

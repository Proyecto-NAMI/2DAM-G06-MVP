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

Este repositorio contiene una base técnica y funcional del MVP, con la configuración de Firebase y la estructura de dominio/ui del producto.

Incluye:

- Aplicación Android con Compose y Material 3
- Configuración del proyecto Android y Gradle
- Soporte de Firebase en entorno local con emuladores
- Validación de identidad y sesión por teléfono
- Modelo de datos y repositorios para Auth, Firestore y Storage
- Tema y componentes de diseño accesibles
- Reglas y documentación de seguridad y privacidad
- Estructura de trazabilidad del proyecto en la carpeta `SSD/`

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

Para Firebase local con emuladores:

```powershell
firebase emulators:start --project demo-nami --only auth,firestore,storage
```

Y luego, en otra terminal:

```powershell
./gradlew.bat connectedDebugAndroidTest
```

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

### Mensajería

- Conversaciones privadas uno a uno
- Envío de mensajes de texto
- Envío de notas de voz con grabación y reproducción previa
- Confirmación visible del estado de envío
- Reintentos ante errores

### Accesibilidad

- Texto grande con ajustes locales
- Botones grandes y acciones simples
- Lectura en voz alta de mensajes de texto
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
- [FIREBASE.md](FIREBASE.md): configuración segura de Firebase y emuladores
- [evidencias-ia/](evidencias-ia/): evidencias visuales del proceso

## Notas importantes

- El proyecto está en fase de desarrollo y validación técnica del MVP.
- Algunas partes de la experiencia y del flujo completo quedan pendientes para próximas tareas.
- La configuración sensible no se sube al repositorio (`local.properties`, `google-services.json`, `.firebaserc`, credenciales, logs, etc.).
- En `debug` el proyecto usa valores ficticios y emuladores. `release` exige configuración real y no usa emuladores.

## Enlaces útiles

- Prototipo Figma de NAMI: https://www.figma.com/design/bGCnnDncM5CUdgVfMmRgz3/NAMI

## Licencia

Este proyecto se encuentra en desarrollo interno del curso 2DAM-G06 y no especifica licencia pública en el repositorio actual.

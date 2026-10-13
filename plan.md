# Plan técnico del MVP de NAMI

**Versión:** 1.0  
**Fecha:** 8 de octubre de 2026  
**Estado:** propuesta para revisión del equipo

## 1. Decisiones técnicas

| Área | Elección | Justificación |
|---|---|---|
| Cliente | Android nativo con Kotlin y Jetpack Compose | Encaja con 2.º DAM, permite una UI móvil accesible y reduce duplicación. |
| Diseño | Material 3 adaptado a Figma | Aporta componentes accesibles, semántica y escalado. |
| Arquitectura | Presentación, dominio y datos; ViewModel y repositorios | Separa UI, reglas y Firebase y facilita las pruebas. |
| Asincronía | Coroutines y Flow | Modelo idiomático para estado, red, grabación y reproducción. |
| Identidad | Firebase Authentication por teléfono | Se alinea con Figma y evita contraseñas complejas. |
| Datos | Cloud Firestore | Sincronización de conversaciones y reglas de acceso. |
| Audio | Firebase Storage | Adecuado para binarios; Firestore guarda solo metadatos. |
| Preferencias | DataStore | Persistencia local simple de accesibilidad. |
| Pruebas | JUnit, fakes, Compose UI Test y Firebase Emulator Suite | Cubre dominio, UI, integración y autorización con datos ficticios. |

Las versiones se fijan en el catálogo de versiones de Gradle. T-02 establece `minSdk 26`, `targetSdk 36` y `compileSdk 37`; la compatibilidad mínima deberá validarse en los dispositivos del piloto antes de cerrar RNF-07.

## 2. Arquitectura

```text
Jetpack Compose
      |
ViewModels + UiState
      |
Casos de uso de dominio
      |
Interfaces de repositorio
      |
+------------------+------------------+------------------+
| Firebase Auth    | Firestore        | Storage          |
| cuenta/sesión    | datos/mensajes   | notas de voz     |
+------------------+------------------+------------------+
      |
DataStore (preferencias locales)
```

### Paquetes principales

- `core.designsystem`: colores, tipografía, dimensiones y componentes accesibles.
- `core.model`: modelos compartidos sin dependencias de Firebase.
- `core.data`: configuración, mapeos, resultados y utilidades.
- `feature.auth`, `feature.profile`, `feature.contacts`.
- `feature.conversations`, `feature.chat`, `feature.audio`, `feature.settings`.

El MVP puede conservar un único módulo Gradle `app`. Solo se separarán módulos si el tiempo y la compilación lo justifican.

## 3. Flujo de estado

- Cada pantalla expone un `UiState` inmutable desde su `ViewModel`.
- Las acciones de usuario llegan como eventos explícitos.
- Los casos de uso contienen validación y reglas independientes de Android cuando sea posible.
- Los repositorios traducen Firebase a modelos y errores de dominio.
- Cada mensaje recibe un UUID antes del envío y reutiliza ese ID al reintentar.
- Un único controlador coordina grabación y reproducción para impedir que ambas estén activas a la vez.

## 4. Modelo de datos propuesto

### `users/{userId}`

```text
displayName: string
surname: string
phoneE164: string
avatarPath: string?
createdAt: timestamp
updatedAt: timestamp
```

### `users/{userId}/contacts/{contactId}`

```text
linkedUserId: string?
displayName: string
surname: string
phoneLookup: string
status: "linked" | "pending"
createdAt: timestamp
```

### `conversations/{conversationId}`

```text
participantIds: [string, string]
lastMessageType: "text" | "audio"?
lastMessagePreview: string?
lastActivityAt: timestamp
```

### `conversations/{conversationId}/messages/{messageId}`

```text
clientMessageId: string
senderId: string
type: "text" | "audio"
text: string?
audioPath: string?
audioDurationMs: number?
createdAt: timestamp
status: "sent"
```

### Preferencias locales

```text
textScale: float
textToSpeechEnabled: boolean
soundConfirmationEnabled: boolean
```

El audio no se almacenará en Firestore. Las reglas de Firestore y Storage comprobarán que `request.auth.uid` pertenece a `participantIds`. La búsqueda por teléfono deberá impedir que se enumere el directorio.

## 5. Relación con Figma

| Pantalla o estado | Implementación MVP | Requisitos |
|---|---|---|
| `Creación de cuenta` | Formulario, verificación telefónica y alta de perfil. | RF-01 |
| `Conversaciones y pantalla principal` | Lista, avatar, nombre y no leídos. | RF-04 |
| `Nuevo contacto` | Alta manual y control de duplicados. | RF-03 |
| Chats con Carlos, Paula y Pedro | Una plantilla reutilizable de chat uno a uno. | RF-05, RF-06, RF-07 |
| Estados `escribiendo` | Estado local del campo; no indicador remoto. | RF-05 |
| Estados `mensaje enviado` | Confirmación visible y sonora tras persistir. | RF-08 |
| `Modificación y opciones` | Tamaño de texto y texto a voz; un clic es regla transversal. | RF-09 |
| `Foto de perfil` | Consulta y edición básica. | RF-02 |
| Cámara, teléfono y vídeo | Ocultos o deshabilitados con explicación. | Fuera de alcance |

La implementación será adaptable y no copiará tamaños absolutos de 390 x 844.

## 6. Seguridad y privacidad

- Autenticación obligatoria para datos remotos.
- Reglas de Firestore y Storage probadas con casos permitidos y denegados.
- Validación de participantes en backend; el cliente no será la frontera de seguridad.
- Teléfonos normalizados a E.164 y búsqueda limitada, sin enumeración de cuentas.
- Entornos separados para desarrollo/pruebas y piloto.
- Datos ficticios en emuladores, capturas y repositorio.
- Logs sin mensajes, URLs firmadas, tokens o teléfonos completos.
- Retención y eliminación resueltas antes de usar datos reales.

## 7. Accesibilidad

- Tokens de tamaño y espaciado centralizados.
- Controles principales de 48 dp o más.
- Semántica y descripciones para iconos.
- Foco lógico y estados anunciados por servicios de accesibilidad.
- Contraste verificado automática y manualmente.
- Pruebas al 200 %, con TalkBack y una mano en dispositivo físico.
- Confirmación visual y sonora; vibración como mejora posterior.

## 8. Estrategia de pruebas

### Unitarias

- Perfil y teléfono; contactos duplicados.
- Envío idempotente y reintento.
- Estados de grabación/reproducción.
- Mapeos entre Firebase y dominio.

### Integración

- Autenticación y persistencia de sesión.
- Lectura, escritura y orden de mensajes.
- Subida y acceso autorizado a audio.
- Reglas que permiten participantes y rechazan terceros.
- Recuperación de conexión sin duplicados.

### Interfaz

- Crear cuenta, añadir contacto, abrir conversación y enviar texto.
- Grabar, revisar, eliminar y enviar audio.
- Error y reintento.
- Tamaño de texto y texto a voz simulado.
- Etiquetas, foco y ausencia de recortes al 200 %.

### Validación humana

- Revisión por otra persona del equipo.
- Prueba de tareas con perfiles representativos y datos ficticios.
- Registro de bloqueos, ayuda solicitada, tiempo y comprensión.

## 9. Entrega incremental

1. Base, navegación, tema y datos de demostración.
2. Cuenta, perfil y contactos.
3. Lista e historial de conversaciones.
4. Texto con confirmación, fallo y reintento.
5. Audio con vista previa, envío y reproducción.
6. Ajustes y texto a voz.
7. Reglas, pruebas, auditoría y piloto.

## 10. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| La verificación SMS bloquea desarrollo | Usar números ficticios de Firebase y repositorios sustituibles. |
| Reglas exponen conversaciones | Probar en emulador con participantes y cuentas atacantes. |
| Audio consume tiempo o datos | Fijar límite, comprimir y mostrar progreso/reintento. |
| Texto grande rompe pantallas | UI adaptable, scroll y pruebas al 200 % en dos tamaños. |
| Crece el alcance por Figma | Aplicar las exclusiones de `spec.md`. |
| Búsqueda por teléfono expone datos | Prototipar en emulador y revisar privacidad antes del piloto. |

## 11. Revisión requerida

Antes de implementar, las cuatro personas deben confirmar el alcance, versión mínima de Android, límite de audio, búsqueda privada por teléfono, retención/borrado y reparto de `tasks.md`.

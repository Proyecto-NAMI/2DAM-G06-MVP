# T-28: auditoría de accesibilidad y tareas

## Alcance y perfiles ficticios

La auditoría usa únicamente contenido sintético (`Persona Ficticia`,
`conversation-fictitious`, mensajes de ejemplo) y tres perfiles de revisión:

1. Persona mayor con baja visión: texto del sistema al 200 % y ancho pequeño.
2. Persona con menor precisión motora: objetivos táctiles de al menos 48 dp y
   navegación con una pulsación por acción.
3. Persona con poca experiencia tecnológica: etiquetas explícitas, estados de
   carga/error y confirmación de envío comprensible.

## Evidencia automática

| Riesgo | Evidencia | Resultado |
|---|---|---|
| Tamaño táctil | `NamiComponentsTest` y `ConversationsScreenTest` (`assertHeightIsAtLeast(48.dp)`) | Correcto |
| Texto al 200 % | Componentes a 320/411 dp, cuenta, contactos, chat, conversaciones y `SettingsScreenTest` en ambos anchos | Correcto en tests instrumentados |
| Etiquetas y TalkBack | `NamiTextField` expone `label`/`error`; títulos son `heading`; estados usan `liveRegion`; avatar, no leídos y opciones de ajustes exponen nombre y estado | Correcto |
| Foco y envío | `TextConversationScreenTest` valida campo, botón deshabilitado vacío, confirmación y reintento | Correcto |
| Flujo principal | Inicio → conversación → campo → enviar, como máximo cuatro acciones | Correcto por navegación y test de envío |

Comandos reproducibles:

```powershell
.\gradlew.bat spotlessCheck testDebugUnitTest assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest
```

## Revisión manual registrada

En un emulador o dispositivo Android 16/API 36 se debe activar TalkBack y
recorrer la lista, el chat y ajustes en dos tamaños (320 dp y 411 dp), primero
al 100 % y después al 200 %. La persona revisora debe confirmar, sin datos
reales:

- orden del foco: título → contenido/estado → acción principal → navegación;
- anuncio de nombre, avatar opcional y mensajes sin leer;
- anuncio de carga, error, «Mensaje enviado» y fallo/reintento;
- ausencia de recortes, solapes o controles menores de 48 dp;
- contraste de texto y controles en tema claro y oscuro.

La ejecución física de TalkBack y la comprobación final por otra persona quedan
marcadas como validación manual del equipo; los resultados automáticos no se
presentan como sustituto de esa revisión.

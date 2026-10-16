# T-27: medición y recuperación del flujo principal

## Alcance

El flujo medido es abrir una conversación, redactar un mensaje, enviarlo y
reintentar cuando falla la red. La misma matriz cubre recreación de pantalla e
interrupción de audio mediante estados persistentes y transitorios.

| Tramo | Objetivo | Evidencia automatizada |
|---|---:|---|
| Abrir conversación desde la lista | ≤ 1.000 ms | `ConversationsScreenTest` valida navegación y estados de carga |
| Confirmar un envío | ≤ 2.000 ms | `TextConversationScreenTest` valida confirmación y reintento |
| Recuperar un envío fallido | Sin duplicados | El reintento conserva `clientMessageId` y las reglas de Firestore son idempotentes |
| Recrear pantalla/interrumpir audio | Estado seguro | borrador/audio pendiente no se consideran enviados hasta persistir |

Los tiempos de red se deben recoger en el emulador de CI; no se inventan
latencias locales. El comando reproducible es:

```bash
firebase emulators:exec --config .firebase-ci.json --project demo-nami \
  --only auth,firestore,storage "./gradlew connectedDebugAndroidTest"
```

Las limitaciones no validadas en este entorno son una pérdida de red física,
un dispositivo con audio real y pruebas de rendimiento bajo carga. La suite
deja documentado el resultado funcional y falla si aparecen duplicados o se
pierde el borrador/audio pendiente.

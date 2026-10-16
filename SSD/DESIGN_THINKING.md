# Design Thinking de NAMI

Este documento resume las decisiones de diseño que orientan el piloto del MVP. Los perfiles y ejemplos son ficticios y no representan datos de personas reales.

## Empatizar

- Perfil principal: persona mayor que necesita leer mensajes con comodidad, botones grandes y confirmaciones claras.
- Perfil de apoyo: familiar o persona cuidadora que necesita enviar mensajes y notas de voz sin pasos innecesarios.
- Contexto: uso ocasional, posible baja precision motora, texto ampliado y conectividad intermitente.

Las necesidades se contrastaron con el prototipo de Figma, las capturas de `Prototype_Reference/` y los requisitos RF/RNF de `spec.md`.

## Definir

Problema de piloto: una persona mayor debe poder abrir una conversacion, escribir o grabar un mensaje y saber si se ha enviado, sin depender de iconos ambiguos ni perder el borrador.

Se priorizaron accesibilidad, estado visible, idempotencia y privacidad. El alcance y las exclusiones estan en [`spec.md`](spec.md) y la trazabilidad en [`tasks.md`](tasks.md).

## Idear

Se evaluaron alternativas para:

- estados `loading`, `empty`, `error`, `pending`, `sent` y `failed`;
- acciones de reintento que reutilizan el mismo identificador de mensaje;
- controles de texto grande y lectura en voz alta;
- confirmacion visible y anunciable de "Mensaje enviado".

La solucion elegida mantiene la logica en casos de uso y ViewModel, con repositorios inyectables para poder probar fallos de red y motores multimedia simulados.

## Prototipar

El prototipo visual de Figma se conserva como referencia de contenido y flujo, no como especificacion de tamanos absolutos. Las pantallas y el enlace estan documentados en [`FIGMA.md`](FIGMA.md). Las exclusiones (llamadas, video, grupos y adjuntos no previstos) permanecen inactivas.

## Probar

- Las pruebas unitarias cubren validacion, borrador, orden del historial, reintento e idempotencia.
- Las pruebas de integracion usan Firebase Emulator Suite con cuentas y datos ficticios.
- La regresion disponible de CI queda enlazada en [`VALIDACION.md`](../VALIDACION.md).
- La auditoria manual con TalkBack, dos tamanos y texto al 200 % requiere un dispositivo o emulador interactivo y una segunda persona; queda declarada como limitacion cuando no se puede ejecutar en este entorno.

## Decisiones y limites

No se usan nombres, telefonos, rutas privadas, tokens ni credenciales reales. El flujo de telefono, el acceso docente y la revision humana final deben comprobarse con las cuentas del equipo antes de la entrega en Moodle. Los servicios Firebase se ejecutan en emuladores durante desarrollo y CI.

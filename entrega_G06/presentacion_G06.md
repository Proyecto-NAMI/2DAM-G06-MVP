# NAMI - Presentacion final G06

## 1. Proposito

NAMI es un MVP Android de mensajeria familiar accesible para personas mayores. El piloto prioriza texto legible, acciones grandes, confirmaciones claras y privacidad.

## 2. Flujo principal

1. Iniciar sesion con verificacion de telefono.
2. Abrir una conversacion 1:1.
3. Componer texto o grabar una nota de voz.
4. Enviar, confirmar el estado y reintentar sin duplicar.

El borrador sobrevive a la recreacion de pantalla y los estados de carga, vacio, error, pendiente y exito son visibles y anunciables.

## 3. Arquitectura

- Kotlin, Jetpack Compose y Material 3.
- ViewModel y casos de uso para estado inmutable.
- Repositorios inyectables para Auth, Firestore y Storage.
- Firebase Emulator Suite para Authentication, Firestore, Storage y Functions.
- Reglas de seguridad y mensajes idempotentes con identificador de cliente.

## 4. Accesibilidad y diseño

Texto escalable hasta el 200 %, objetivos tactiles amplios, lectura en voz alta y foco visible. La referencia visual esta en Figma y en `Prototype_Reference/`; la implementacion respeta las exclusiones de `spec.md`.

## 5. Verificacion

La matriz RF/RNF, los resultados de CI, las limitaciones y las evidencias se encuentran en `VALIDACION.md`. El clon limpio reproduce la compilacion y las pruebas con los comandos del README. Los datos y cuentas son ficticios.

## 6. Entrega

El paquete incluye el APK debug, esta presentacion, su fuente y `entrega_G06.txt`. La URL privada y el SHA de referencia estan en el manifiesto. El acceso docente y la revision humana final deben confirmarse antes de subir a Moodle.

## 7. Uso responsable de IA

Se documentan tres casos de uso de IA (alcance, plan tecnico y trazabilidad) en `SSD/uso-ia.md`. La decision final, la revision de cambios y la declaracion de limitaciones corresponden al equipo G06.

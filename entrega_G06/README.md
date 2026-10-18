# Paquete de entrega G06

Este directorio contiene los artefactos preparados para el piloto y Moodle:

- `presentacion_G06.pdf`: presentacion final en PDF.
- `presentacion_G06.md`: fuente editable de la presentacion.
- `NAMI-debug.apk`: APK debug generado para el entorno ficticio de desarrollo, si el artefacto esta disponible.
- `entrega_G06.txt`: manifiesto con identificador, URL de acceso, estado del
  commit/PR de T-40 y SHA-256 del APK.

## Comprobacion rapida

```powershell
Get-FileHash .\NAMI-debug.apk -Algorithm SHA256
```

Para reproducir el APK desde un clon limpio, sigue [`../README.md`](../README.md) y ejecuta `.\gradlew.bat assembleDebug`. Los datos de prueba usan Firebase Emulator Suite; no se debe conectar el APK debug a un proyecto real.

El APK corresponde al candidato técnico validado por T-40 y el manifiesto
identifica su commit técnico y la PR. Antes de publicar en Moodle, la persona
responsable debe confirmar el acceso docente y registrar TalkBack, dispositivo
físico/API 26 y revisión independiente.

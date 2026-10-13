# Especificación del MVP de NAMI

**Versión:** 1.0  
**Fecha:** 8 de octubre de 2026  
**Equipo:** G06  
**Curso:** 2.º DAM  
**Prototipo:** <https://www.figma.com/design/bGCnnDncM5CUdgVfMmRgz3/NAMI?node-id=0-1&t=NXeWbgK48RRls40z-1>

## 1. Problema y personas usuarias

Muchas personas mayores tienen menos contacto del que desean con familiares y amistades. Aunque existen aplicaciones de mensajería, sus menús, iconos, contraseñas y múltiples funciones pueden generar inseguridad o impedir una participación autónoma.

NAMI ofrece un espacio privado de comunicación uno a uno con pocas opciones visibles, texto grande y prioridad para el audio.

- **María/Carmen, 78 años:** visión reducida, poca familiaridad con el móvil y temor a equivocarse. Necesita acciones claras, botones grandes, confirmación del resultado y la posibilidad de escuchar contenido.
- **Carlos, 22 años, familiar:** quiere comunicarse de forma frecuente sin obligar a la persona mayor a aprender una aplicación compleja.
- **Familiar o persona de confianza:** puede ser añadido como contacto y participar en una conversación privada.

## 2. Objetivo del MVP

Permitir que una persona mayor cree y mantenga un perfil, gestione una lista pequeña de contactos de confianza y mantenga conversaciones privadas uno a uno mediante texto y notas de voz, con confirmaciones claras y controles básicos de accesibilidad.

### Indicadores de éxito del piloto

- Al menos 4 de 5 participantes mayores completan, sin ayuda directa, la apertura de una conversación y el envío de un texto o audio.
- El 100 % identifica si el mensaje se envió o falló.
- Abrir una conversación y enviar un texto requiere como máximo 4 acciones desde la pantalla principal.
- No hay incumplimientos críticos de contraste, etiquetado o tamaño táctil en el flujo principal.

## 3. Alcance

### Incluido

- Creación de cuenta con nombre, apellidos y teléfono, y sesión persistente.
- Consulta y edición básica del perfil.
- Alta manual de un contacto por nombre, apellidos y teléfono.
- Lista de conversaciones con nombre, avatar opcional y mensajes pendientes.
- Conversación privada uno a uno.
- Envío y recepción de mensajes de texto.
- Grabación, reproducción previa, envío y reproducción de notas de voz.
- Confirmación visible y sonora de envío; error recuperable con reintento.
- Ajuste local de tamaño de texto y activación de texto a voz.
- Lectura en voz alta de mensajes de texto.

### Excluido

- Chats grupales, llamadas y videollamadas.
- Fotografías, vídeos, documentos o ubicación.
- Inicio de sesión por voz, rostro o huella.
- Dinámicas familiares, sugerencias automáticas o contenido generado por IA.
- Juegos, tutoriales interactivos y panel web de administración.
- Cifrado de extremo a extremo.
- Modo oscuro propio; se respetará el modo del sistema cuando sea compatible.

Los iconos de cámara, llamada y vídeo presentes en Figma son exploraciones visuales y no estarán activos en el MVP.

## 4. Requisitos funcionales

### RF-01. Cuenta y sesión

- **Cuando** una persona introduzca nombre, apellidos y un teléfono válido y complete la verificación, **el sistema deberá** crear su cuenta y abrir la pantalla de conversaciones.
- **Mientras** exista una sesión válida, **el sistema deberá** mantener el acceso al volver a abrir la aplicación.
- **Si** la verificación falla o caduca, **el sistema deberá** explicarlo y permitir solicitar un nuevo código sin perder el formulario.

### RF-02. Perfil

- **Cuando** la persona abra su perfil, **el sistema deberá** mostrar nombre, apellidos, teléfono y avatar opcional.
- **Cuando** guarde un nombre o avatar válido, **el sistema deberá** actualizar el perfil y reflejarlo en la interfaz.

### RF-03. Contactos de confianza

- **Cuando** la persona introduzca nombre, apellidos y teléfono válido de un contacto, **el sistema deberá** añadirlo o vincularlo con una cuenta existente.
- **Si** el teléfono ya pertenece a un contacto, **el sistema deberá** impedir el duplicado e indicar qué contacto existe.
- **Si** el teléfono no tiene cuenta NAMI, **el sistema deberá** conservar el contacto como pendiente sin enviar invitaciones automáticas.

### RF-04. Lista de conversaciones

- **Cuando** la persona acceda a la pantalla principal, **el sistema deberá** mostrar sus conversaciones ordenadas por actividad reciente.
- Cada elemento deberá mostrar nombre, avatar opcional y contador de mensajes no leídos.
- **Cuando** pulse una conversación, **el sistema deberá** abrir el historial con ese contacto.

### RF-05. Mensajes de texto

- **Cuando** la persona escriba texto no vacío y pulse enviar, **el sistema deberá** guardar el mensaje, mostrarlo y confirmar el envío.
- **Si** el texto está vacío o solo contiene espacios, **el sistema deberá** mantener deshabilitado el envío.
- **Si** el envío falla, **el sistema deberá** marcar el mensaje como no enviado y ofrecer reintento sin duplicarlo.

### RF-06. Grabación y envío de audio

- **Cuando** la persona pulse una vez el micrófono y conceda permiso, **el sistema deberá** iniciar la grabación y mostrar estado y duración.
- **Cuando** pulse detener, **el sistema deberá** permitir reproducir, eliminar o enviar la nota antes de publicarla.
- **Si** se deniega el permiso, **el sistema deberá** explicar para qué se necesita y permitir continuar con texto.
- **Si** la subida falla, **el sistema deberá** conservar temporalmente el audio y ofrecer reintento o eliminación.

### RF-07. Reproducción y texto a voz

- **Cuando** la persona pulse un mensaje de audio, **el sistema deberá** reproducirlo y ofrecer pausa.
- **Cuando** pulse escuchar en un mensaje de texto y el texto a voz esté activado, **el sistema deberá** leerlo en voz alta.
- **Si** el motor de voz no está disponible, **el sistema deberá** explicarlo y mantener visible el texto.

### RF-08. Confirmación y estado

- **Cuando** un mensaje se guarde en el servidor, **el sistema deberá** mostrar “Mensaje enviado” y emitir una señal sonora breve si está habilitada.
- **Mientras** se envía, **el sistema deberá** mostrar progreso y evitar envíos duplicados.
- **Si** se pierde la conexión, **el sistema deberá** diferenciar entre pendiente y fallido.

### RF-09. Preferencias de accesibilidad

- **Cuando** la persona cambie el tamaño de texto, **el sistema deberá** aplicarlo a todas las pantallas del MVP y conservarlo localmente.
- **Cuando** active o desactive texto a voz, **el sistema deberá** conservar la preferencia.
- Las acciones esenciales deberán ejecutarse con pulsación simple; no se exigirá mantener pulsado el micrófono.

### RF-10. Cierre de sesión

- **Cuando** la persona confirme el cierre de sesión, **el sistema deberá** revocar la sesión local, detener reproducción o grabación y volver al acceso.

## 5. Requisitos no funcionales

- **RNF-01 Accesibilidad:** contraste WCAG 2.2 AA aplicable, texto al 200 %, etiquetas para lector de pantalla y controles de al menos 48 x 48 dp.
- **RNF-02 Facilidad de uso:** enviar texto requerirá como máximo 4 acciones desde inicio; grabar, revisar y enviar audio, como máximo 5.
- **RNF-03 Rendimiento:** con conexión estable, inicio mostrará contenido o carga en menos de 2 segundos y confirmará envío en menos de 3 segundos en el percentil 95 del piloto.
- **RNF-04 Fiabilidad:** reintentar no creará duplicados; cada envío tendrá un identificador idempotente generado en cliente.
- **RNF-05 Seguridad:** toda operación remota exigirá autenticación y las reglas impedirán acceder a conversaciones ajenas.
- **RNF-06 Privacidad:** no se registrará contenido de mensajes, audio ni teléfonos completos; las pruebas usarán datos ficticios.
- **RNF-07 Compatibilidad:** funcionará en la versión mínima de Android que el equipo fije y se validará en un teléfono y un emulador de tamaños distintos.
- **RNF-08 Mantenibilidad:** la lógica crítica tendrá pruebas unitarias y al menos 70 % de cobertura en paquetes de dominio.
- **RNF-09 Recuperación:** una recreación de pantalla no perderá el texto aún no enviado ni dejará una grabación activa sin indicador.

## 6. Criterios de aceptación

### CA-01. Primer acceso

**Dado** un número ficticio aceptado en pruebas, **cuando** la persona completa la verificación, **entonces** se crea el perfil, aparece inicio y la sesión continúa tras reiniciar.

### CA-02. Añadir contacto

**Dada** una sesión válida, **cuando** se registra un teléfono válido no repetido, **entonces** el contacto aparece una sola vez y puede abrirse su conversación; un duplicado muestra aviso y no crea otra entrada.

### CA-03. Enviar texto

**Dada** una conversación, **cuando** se envía “Hola, ¿cómo estás?”, **entonces** aparece una sola vez, se anuncia “Mensaje enviado” y el campo queda vacío.

### CA-04. Fallo de red

**Dada** una conversación sin conexión, **cuando** se intenta enviar, **entonces** el mensaje queda pendiente o fallido, ofrece reintento y, recuperada la red, se envía una sola copia.

### CA-05. Nota de voz

**Dado** permiso de micrófono, **cuando** se inicia y detiene una grabación, **entonces** puede escucharse antes de enviarla o borrarla; enviada, el destinatario puede reproducirla.

### CA-06. Accesibilidad

**Dado** el tamaño de texto máximo, **cuando** se recorren cuenta, inicio, chat y ajustes, **entonces** el contenido esencial no se corta ni superpone y los controles son accesibles con lector de pantalla.

### CA-07. Privacidad

**Dadas** dos cuentas ajenas a la misma conversación, **cuando** una intenta consultar datos de la otra, **entonces** el backend deniega la operación.

## 7. Casos límite

- Nombre vacío, muy largo o con caracteres internacionales.
- Teléfono internacional, inválido, duplicado o ya registrado.
- Código incorrecto, caducado o solicitado demasiadas veces.
- Lista sin contactos o conversaciones.
- Texto solo con espacios, demasiado largo o enviado repetidamente.
- Permiso de micrófono denegado temporal o permanentemente.
- Grabación vacía, interrumpida, demasiado larga o sin almacenamiento.
- Pérdida de red durante subida de audio o cierre durante envío.
- Audio ausente o no decodificable.
- Texto a voz no instalado, idioma no disponible o reproducción interrumpida.
- Cambio de tamaño de texto con una pantalla abierta.
- Acceso tras retirar o bloquear un contacto, decisión aún pendiente.

## 8. Dudas pendientes

1. Validar Android 8.0 (API 26) como versión mínima en los dispositivos del piloto; T-02 adopta esta base provisional.
2. Definir duración y tamaño máximos de una nota de voz.
3. Decidir si y cómo invitar a un contacto pendiente; queda fuera del MVP actual.
4. Definir conservación y borrado de cuenta, mensajes y audios antes de usar datos reales.
5. Decidir si el estado escuchado/leído entrará después del MVP.
6. Confirmar que los nombres y teléfonos visibles en el prototipo son ficticios y autorizados para pruebas.

# Constitución del proyecto NAMI

**Versión:** 1.0  
**Fecha:** 8 de octubre de 2026  
**Ámbito:** MVP de la aplicación móvil NAMI

## 1. Propósito

NAMI busca facilitar la comunicación frecuente entre personas mayores y sus familiares mediante una experiencia privada, sencilla y accesible. Esta constitución define las reglas que prevalecen sobre decisiones técnicas, propuestas automáticas y preferencias individuales durante el MVP.

## 2. Principios obligatorios

### P1. Accesibilidad desde el diseño

- La interfaz se diseñará primero para una persona mayor con visión reducida, menor precisión motora y poca familiaridad tecnológica.
- El texto base será legible al tamaño configurado por el sistema y nunca quedará cortado al ampliarlo al 200 %.
- Los controles táctiles principales tendrán al menos 48 x 48 dp y separación suficiente para evitar pulsaciones accidentales.
- Las acciones principales usarán texto e icono; el color no será el único medio para comunicar estado o significado.
- El contraste mínimo será 4,5:1 para texto normal y 3:1 para texto grande y componentes gráficos relevantes.
- Los flujos esenciales no requerirán más pasos de los definidos en `spec.md`, y toda acción irreversible pedirá confirmación.
- Los mensajes deberán poder reproducirse en voz alta mediante la función de texto a voz del dispositivo.

### P2. Privacidad y protección de datos por defecto

- Solo se recogerán los datos necesarios para crear el perfil, relacionar contactos y prestar el servicio de mensajería.
- El acceso a conversaciones y audios se limitará a sus participantes mediante reglas de autorización verificadas en el backend.
- Las comunicaciones usarán HTTPS/TLS y los datos persistentes usarán el cifrado ofrecido por la plataforma. El MVP no afirmará disponer de cifrado de extremo a extremo.
- No se incluirán datos personales, teléfonos reales, credenciales ni secretos en el repositorio, capturas, logs o datos de prueba.
- Los secretos y archivos de configuración sensibles se mantendrán fuera de Git y se proporcionarán por variables o archivos locales ignorados.
- Los logs de producción no incluirán el texto ni el audio de los mensajes.
- La persona usuaria podrá cerrar sesión; la eliminación completa de cuenta y datos se documentará como limitación si no entra en el MVP.

### P3. MVP pequeño y trazable

- Solo se implementarán funcionalidades incluidas explícitamente en `spec.md`.
- Cada cambio deberá enlazar al menos un requisito `RF-*` o `RNF-*` y una tarea `T-*`.
- Llamadas, videollamadas, envío de fotografías, acceso biométrico y dinámicas familiares automáticas quedan fuera del MVP salvo acuerdo del equipo y actualización previa de los cinco documentos SDD.
- Una ampliación de alcance exige estimación, responsable, criterio de aceptación y revisión del equipo.

### P4. Calidad técnica verificable

- La lógica de negocio no se implementará dentro de componentes visuales.
- Se emplearán nombres claros, código formateado, análisis estático y pruebas automáticas en las rutas críticas.
- No se fusionará código que rompa la compilación, el análisis estático o las pruebas acordadas.
- Las operaciones remotas representarán al menos los estados cargando, éxito, vacío y error recuperable.
- Los errores mostrados a la persona usuaria serán comprensibles y ofrecerán una acción segura cuando sea posible.
- Las dependencias nuevas deberán justificar su necesidad, mantenimiento, licencia y efecto sobre privacidad.

### P5. Revisión humana de la IA

- La IA puede proponer requisitos, código, pruebas o documentación, pero no aprueba ni fusiona cambios.
- Toda salida de IA se revisará contra `constitution.md`, `spec.md`, `plan.md`, el prototipo de Figma y las pruebas del proyecto.
- La persona responsable comprobará especialmente permisos, tratamiento de datos, reglas de acceso, mensajes de error y accesibilidad.
- No se enviarán a servicios de IA credenciales, teléfonos reales, conversaciones privadas ni otros datos personales.
- Si una propuesta de IA amplía el alcance o contradice un documento, se descartará o se elevará como duda; nunca se aceptará de forma silenciosa.

## 3. Puertas de calidad

Una tarea solo podrá considerarse terminada cuando:

1. cumpla su criterio de finalización en `tasks.md`;
2. tenga trazabilidad con los requisitos y criterios de aceptación correspondientes;
3. haya pasado las comprobaciones disponibles de compilación, formato, análisis y pruebas;
4. se haya revisado manualmente el flujo afectado en un dispositivo o emulador;
5. no introduzca datos sensibles ni debilite las reglas de acceso;
6. haya sido revisada por al menos una persona distinta de la autora.

## 4. Gestión de cambios

- Las decisiones funcionales se actualizan primero en `spec.md`.
- Las decisiones de arquitectura o tecnología se actualizan en `plan.md`.
- El trabajo resultante se refleja en `tasks.md` antes de implementarse.
- Los cambios de principios requieren acuerdo de las cuatro personas del equipo y una nueva versión de este archivo.
- En caso de conflicto, el orden de prioridad es: protección de las personas y sus datos, accesibilidad, alcance del MVP, corrección técnica y velocidad de entrega.


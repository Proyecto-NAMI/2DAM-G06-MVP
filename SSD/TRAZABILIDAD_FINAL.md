# T-40: trazabilidad y regresión final del MVP integrado

Fecha técnica: 18 de octubre de 2026. Rama: `yang_dev`. Issue: #63.
Seguimiento: #29 y #30. Esta matriz no permite cerrar T-40 mientras falten las
firmas humanas indicadas al final.

## Flujo productivo desde Firebase vacío

`FirebaseEmulatorSmokeTest.emptySuiteJourneyCreatesUsersConversationTextAndAudioWithoutSeeding`
se ejecuta dentro de un `firebase emulators:exec` nuevo y realiza este recorrido:

1. crea y verifica por teléfono dos cuentas ficticias y guarda sus perfiles;
2. confirma que la cuenta propietaria comienza sin conversaciones;
3. añade el teléfono de la otra cuenta mediante `addContact` y recibe `linked`;
4. crea la conversación exclusivamente mediante `openConversation`;
5. envía un texto mediante `FirebaseTextMessagesRepository`;
6. sube una nota mediante `FirebaseAudioNotesRepository`;
7. observa ambos mensajes, descarga el audio mediante
   `FirebaseAudioPlaybackSourceRepository` y comprueba los dos no leídos;
8. entra como tercera persona y confirma la denegación de conversación y audio.

La prueba no llama a `seedConversationForTests`, no escribe una conversación
desde el cliente y no sustituye ningún repositorio productivo por un fake.

## Requisitos funcionales

| Requisito | Evidencia productiva o complementaria |
|---|---|
| RF-01 | Recorrido vacío de T-40 y `phoneVerificationCanRetryAndSessionIsRestoredByANewRepository`: verificación, perfil y sesión restaurada. |
| RF-02 | `FirebaseAccountProfileRepository`, `ProfileUseCasesTest` y `ProfileScreenTest`: carga, actualización, error y avatar opcional. |
| RF-03 | Recorrido vacío de T-40 y `addContactLinksKnownPhonesAndStoresUnknownOnesAsPending`: vinculado, pendiente y duplicado. |
| RF-04 | Recorrido vacío de T-40, `FirebaseConversationsRepository` y `ConversationsScreenTest`: vacío, orden, datos visibles y no leídos. |
| RF-05 | Recorrido vacío de T-40, `FirebaseTextMessagesTest` y `TextConversationScreenTest`: envío, historial, vacío y reintento. |
| RF-06 | Recorrido vacío de T-40, `AndroidAudioRecorderTest`, `FirebaseAudioNotesTest` y `AudioRecordingScreenTest`: grabación, vista previa, subida y recuperación. |
| RF-07 | `FirebaseAudioPlaybackTest`, `MediaPlaybackT23Test`, `TextToSpeechT24Test` y `TextConversationScreenTest`: reproducción, pausa, TTS y errores recuperables. |
| RF-08 | Recorrido vacío de T-40, `FirebaseTextMessagesTest` y `FirebaseAudioNotesTest`: pendiente, progreso, confirmación, fallo y reintento idempotente. |
| RF-09 | `AccessibilityPreferencesPersistenceTest`, `SettingsScreenTest` y auditoría T-39: escala, lectura persistente y pulsación simple. |
| RF-10 | `AccountCreationScreenTest.confirmedSignOutStopsMediaAndClearsPrivateState` y `MediaSessionCoordinatorTest`: confirmación, limpieza y bloqueo de retorno. |

## Requisitos no funcionales

| Requisito | Evidencia |
|---|---|
| RNF-01 | Accessibility Test Framework, pruebas 320/411 dp al 200 % y `SSD/ACCESIBILIDAD.md`; revisión auditiva humana pendiente. |
| RNF-02 | Desglose de cuatro acciones para texto y cinco para audio en `SSD/ACCESIBILIDAD.md`. |
| RNF-03 | `FirebasePerformanceRecoveryTest` y `SSD/RENDIMIENTO.md`: 20 muestras y p95 bajo los límites. |
| RNF-04 | Recorrido vacío de T-40 y pruebas concurrentes de texto/audio: UUID estable y contadores sin duplicar. |
| RNF-05 | Reglas negativas en `FirebaseEmulatorSmokeTest`, `FirebaseTextMessagesTest` y `FirebaseAudioNotesTest`, incluido tercero en el recorrido T-40. |
| RNF-06 | Datos `demo-nami` y ficticios; revisión de fuentes sin logs de teléfonos, mensajes, tokens ni rutas privadas. |
| RNF-07 | Debug/release compilan y API 36 se prueba en AVD; dispositivo físico/API 26 sigue pendiente. |
| RNF-08 | `koverVerifyDebug` exige 70 % por paquete de dominio y pasa junto a 110 pruebas JVM. |
| RNF-09 | `FirebasePerformanceRecoveryTest`, `TextConversationScreenTest` y `AudioRecordingScreenTest`: borrador, salida y grabación quedan seguros. |

## Criterios de aceptación

| Criterio | Evidencia |
|---|---|
| CA-01 | Recorrido T-40 y prueba de restauración de sesión telefónica. |
| CA-02 | Recorrido T-40 crea un único contacto vinculado y abre la conversación; la suite de contactos cubre duplicado. |
| CA-03 | Recorrido T-40 persiste un único texto; la UI anuncia envío y vacía el campo. |
| CA-04 | Pruebas de pérdida/recuperación de texto y audio conservan UUID y una sola copia. |
| CA-05 | Recorrido T-40 sube y descarga audio; `FirebaseAudioPlaybackTest` usa una grabación real y `AudioRecordingScreenTest` cubre escuchar/borrar. |
| CA-06 | T-39 comprueba 320/411 dp, 200 %, semántica y 48 dp; recorrido TalkBack firmado pendiente. |
| CA-07 | El recorrido T-40 y las suites de reglas rechazan al tercero en Firestore y Storage. |

## Cableado productivo

| Contrato | Implementación Android/Firebase | Punto de entrada |
|---|---|---|
| `AuthRepository` | `FirebasePhoneAuthRepository` | `FirebaseRepositoryContainer` → `PhoneAuthViewModel` |
| `AccountProfileRepository` | `FirebaseAccountProfileRepository` | `ProfileViewModelFactory` |
| `ContactsRepository` | `FirebaseContactsRepository` | `AddContactViewModelFactory` |
| `ConversationsRepository` | `FirebaseConversationsRepository` | `ConversationsViewModelFactory` |
| `TextMessagesRepository` | `FirebaseTextMessagesRepository` | `TextConversationViewModelFactory` |
| `AudioRecorder` | `AndroidAudioRecorder` | `AudioRecordingViewModelFactory` |
| `AudioNotesRepository` | `FirebaseAudioNotesRepository` | `UploadAudioNoteUseCase` del chat |
| `AudioPlayer` | `AndroidAudioPlayer` | `NamiApplication.audioPlaybackController` |
| `AudioPlaybackSourceRepository` | `FirebaseAudioPlaybackSourceRepository` | `TextConversationViewModelFactory` |
| `TextToSpeechEngine` | `AndroidTextToSpeechEngine` | `NamiApplication.textToSpeechController` |
| `AccessibilityPreferencesRepository` | `DataStoreAccessibilityPreferencesRepository` | tema y `SettingsViewModel` |

No se encontraron contratos del MVP sin implementación ni implementaciones
anteriores sin punto de entrada productivo.

## Uso permitido de siembra

`seedConversationForTests` permanece como fixture explícito para aislar pruebas
de reglas, idempotencia, recuperación y reproducción. Solo funciona cuando
`FUNCTIONS_EMULATOR=true` y rechaza producción. Ningún resultado de creación de
contacto/conversación ni el recorrido T-40 se acredita con esa utilidad.

## Pendientes humanos de cierre

- Recorrido completo con TalkBack firmado por la persona autora.
- Repetición por otra persona del equipo.
- Ejecución en un dispositivo físico representativo y comprobación de API 26.
- Confirmación del acceso docente al repositorio/artefacto.

Hasta registrar esos puntos, el candidato técnico puede entregarse para
revisión, pero T-40 y el issue #63 deben permanecer abiertos.

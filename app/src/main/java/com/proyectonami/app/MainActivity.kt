package com.proyectonami.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.proyectonami.app.core.data.firebase.AuthRepository
import com.proyectonami.app.core.data.preferences.DataStoreAccessibilityPreferencesRepository
import com.proyectonami.app.core.domain.audio.UploadAudioNoteUseCase
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.domain.contacts.AddContactUseCase
import com.proyectonami.app.core.domain.contacts.OpenConversationUseCase
import com.proyectonami.app.core.domain.conversations.ObserveConversationsUseCase
import com.proyectonami.app.core.domain.media.AudioPlaybackController
import com.proyectonami.app.core.domain.media.AudioPlaybackSourceRepository
import com.proyectonami.app.core.domain.messages.ObserveTextMessagesUseCase
import com.proyectonami.app.core.domain.messages.SendTextMessageUseCase
import com.proyectonami.app.core.domain.tts.TextToSpeechController
import com.proyectonami.app.core.media.ActiveMediaSession
import com.proyectonami.app.core.media.AndroidAudioRecorder
import com.proyectonami.app.core.media.MediaSessionController
import com.proyectonami.app.core.media.MediaSessionCoordinator
import com.proyectonami.app.feature.audio.presentation.AudioRecordingViewModel
import com.proyectonami.app.feature.auth.presentation.PhoneAuthViewModel
import com.proyectonami.app.feature.chat.presentation.TextConversationViewModel
import com.proyectonami.app.feature.contacts.presentation.AddContactViewModel
import com.proyectonami.app.feature.conversations.presentation.ConversationsViewModel
import com.proyectonami.app.feature.profile.domain.LoadProfileUseCase
import com.proyectonami.app.feature.profile.domain.UpdateProfileUseCase
import com.proyectonami.app.feature.profile.presentation.ProfileViewModel
import com.proyectonami.app.ui.NamiApp
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var phoneAuthViewModel: PhoneAuthViewModel
    private lateinit var addContactViewModel: AddContactViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var conversationsViewModel: ConversationsViewModel
    private var playbackCleanupRegistration: AutoCloseable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val namiApplication = application as NamiApplication
        val repositories = namiApplication.repositoryContainer
        playbackCleanupRegistration =
            namiApplication.mediaSessionCoordinator.register(
                ActiveMediaSession {
                    namiApplication.audioPlaybackController.stop()
                    namiApplication.textToSpeechController.stop()
                    repositories.audioPlaybackSourceRepository.clear()
                },
            )
        phoneAuthViewModel =
            ViewModelProvider(
                this,
                PhoneAuthViewModelFactory(
                    authRepository = repositories.authRepository,
                    accountProfileRepository = repositories.accountProfileRepository,
                    mediaSessionController = namiApplication.mediaSessionCoordinator,
                ),
            )[PhoneAuthViewModel::class.java]
        addContactViewModel =
            ViewModelProvider(
                this,
                AddContactViewModelFactory(
                    // El alta pasa por la función addContact (T-11): exige teléfono
                    // verificado, App Check en producción y cuota por UID, y decide en
                    // el servidor si el contacto queda vinculado o pendiente.
                    addContactUseCase = AddContactUseCase(repositories.contactsRepository),
                    openConversationUseCase = OpenConversationUseCase(repositories.contactsRepository),
                ),
            )[AddContactViewModel::class.java]
        profileViewModel =
            ViewModelProvider(
                this,
                ProfileViewModelFactory(
                    authRepository = repositories.authRepository,
                    accountProfileRepository = repositories.accountProfileRepository,
                    currentUserId = { repositories.authRepository.currentUserId },
                ),
            )[ProfileViewModel::class.java]
        conversationsViewModel =
            ViewModelProvider(
                this,
                ConversationsViewModelFactory(ObserveConversationsUseCase(repositories.conversationsRepository)),
            )[ConversationsViewModel::class.java]
        setContent {
            NamiApp(
                activity = this,
                viewModel = phoneAuthViewModel,
                addContactViewModel = addContactViewModel,
                profileViewModel = profileViewModel,
                conversationsViewModel = conversationsViewModel,
                createConversationViewModel = { conversationId ->
                    ViewModelProvider(
                        this,
                        TextConversationViewModelFactory(
                            conversationId = conversationId,
                            senderId = { repositories.authRepository.currentUserId },
                            observeTextMessages = ObserveTextMessagesUseCase(repositories.textMessagesRepository),
                            sendTextMessage = SendTextMessageUseCase(repositories.textMessagesRepository),
                            audioPlaybackController = namiApplication.audioPlaybackController,
                            audioPlaybackSourceRepository = repositories.audioPlaybackSourceRepository,
                            textToSpeechController = namiApplication.textToSpeechController,
                        ),
                    ).get("text-conversation-$conversationId", TextConversationViewModel::class.java)
                },
                createAudioRecordingViewModel = { conversationId ->
                    val userId = repositories.authRepository.currentUserId.orEmpty()
                    ViewModelProvider(
                        this,
                        AudioRecordingViewModelFactory(
                            context = applicationContext,
                            playbackController = namiApplication.audioPlaybackController,
                            mediaSessionCoordinator = namiApplication.mediaSessionCoordinator,
                            conversationId = conversationId,
                            senderId = { repositories.authRepository.currentUserId },
                            uploadAudioNote = UploadAudioNoteUseCase(repositories.audioNotesRepository),
                        ),
                    ).get("audio-recording-$userId-$conversationId", AudioRecordingViewModel::class.java)
                },
                accessibilityPreferencesRepository = DataStoreAccessibilityPreferencesRepository(applicationContext),
            )
        }
    }

    override fun onDestroy() {
        playbackCleanupRegistration?.close()
        playbackCleanupRegistration = null
        super.onDestroy()
    }
}

private class AudioRecordingViewModelFactory(
    private val context: Context,
    private val playbackController: AudioPlaybackController,
    private val mediaSessionCoordinator: MediaSessionCoordinator,
    private val conversationId: String,
    private val senderId: () -> String?,
    private val uploadAudioNote: UploadAudioNoteUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(AudioRecordingViewModel::class.java))
        val recorder = AndroidAudioRecorder(context)
        val registration =
            mediaSessionCoordinator.register(
                ActiveMediaSession {
                    recorder.close()
                    playbackController.stop()
                },
            )
        return checkNotNull(
            modelClass.cast(
                AudioRecordingViewModel(
                    recorder = recorder,
                    clock = recorder,
                    localPath = {
                        File(context.cacheDir, "nami-audio-${UUID.randomUUID()}.m4a").absolutePath
                    },
                    playbackController = playbackController,
                    mediaRegistration = registration,
                    conversationId = conversationId,
                    senderId = senderId,
                    uploadAudioNote = uploadAudioNote,
                ),
            ),
        )
    }
}

private class ConversationsViewModelFactory(
    private val observeConversations: ObserveConversationsUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(ConversationsViewModel::class.java))
        return checkNotNull(modelClass.cast(ConversationsViewModel(observeConversations)))
    }
}

private class TextConversationViewModelFactory(
    private val conversationId: String,
    private val senderId: () -> String?,
    private val observeTextMessages: ObserveTextMessagesUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val audioPlaybackController: AudioPlaybackController,
    private val audioPlaybackSourceRepository: AudioPlaybackSourceRepository,
    private val textToSpeechController: TextToSpeechController,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(TextConversationViewModel::class.java))
        return checkNotNull(
            modelClass.cast(
                TextConversationViewModel(
                    conversationId = conversationId,
                    senderId = senderId,
                    observeTextMessages = observeTextMessages,
                    sendTextMessage = sendTextMessage,
                    audioPlaybackController = audioPlaybackController,
                    audioPlaybackSourceRepository = audioPlaybackSourceRepository,
                    textToSpeechController = textToSpeechController,
                ),
            ),
        )
    }
}

private class ProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val accountProfileRepository: AccountProfileRepository,
    private val currentUserId: () -> String?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
        return checkNotNull(
            modelClass.cast(
                ProfileViewModel(
                    authRepository = authRepository,
                    loadProfileUseCase = LoadProfileUseCase(accountProfileRepository),
                    updateProfileUseCase = UpdateProfileUseCase(accountProfileRepository),
                    currentUserId = currentUserId,
                ),
            ),
        )
    }
}

private class AddContactViewModelFactory(
    private val addContactUseCase: AddContactUseCase,
    private val openConversationUseCase: OpenConversationUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(AddContactViewModel::class.java))
        return checkNotNull(
            modelClass.cast(
                AddContactViewModel(addContactUseCase, openConversationUseCase),
            ),
        )
    }
}

private class PhoneAuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val accountProfileRepository: AccountProfileRepository,
    private val mediaSessionController: MediaSessionController,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(PhoneAuthViewModel::class.java))
        return checkNotNull(
            modelClass.cast(
                PhoneAuthViewModel(
                    authRepository,
                    accountProfileRepository,
                    mediaSessionController,
                ),
            ),
        )
    }
}

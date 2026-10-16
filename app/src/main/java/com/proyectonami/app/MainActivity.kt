package com.proyectonami.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.proyectonami.app.core.data.firebase.AuthRepository
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.domain.conversations.ObserveConversationsUseCase
import com.proyectonami.app.core.domain.messages.ObserveTextMessagesUseCase
import com.proyectonami.app.core.domain.messages.SendTextMessageUseCase
import com.proyectonami.app.core.media.MediaSessionController
import com.proyectonami.app.feature.auth.presentation.PhoneAuthViewModel
import com.proyectonami.app.feature.chat.presentation.TextConversationViewModel
import com.proyectonami.app.feature.contacts.domain.AddContactUseCase
import com.proyectonami.app.feature.contacts.presentation.AddContactViewModel
import com.proyectonami.app.feature.conversations.presentation.ConversationsViewModel
import com.proyectonami.app.feature.profile.domain.LoadProfileUseCase
import com.proyectonami.app.feature.profile.domain.UpdateProfileUseCase
import com.proyectonami.app.feature.profile.presentation.ProfileViewModel
import com.proyectonami.app.ui.NamiApp

class MainActivity : ComponentActivity() {
    private lateinit var phoneAuthViewModel: PhoneAuthViewModel
    private lateinit var addContactViewModel: AddContactViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var conversationsViewModel: ConversationsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repositories = (application as NamiApplication).repositoryContainer
        phoneAuthViewModel =
            ViewModelProvider(
                this,
                PhoneAuthViewModelFactory(
                    authRepository = repositories.authRepository,
                    accountProfileRepository = repositories.accountProfileRepository,
                    mediaSessionController = (application as NamiApplication).mediaSessionCoordinator,
                ),
            )[PhoneAuthViewModel::class.java]
        addContactViewModel =
            ViewModelProvider(
                this,
                AddContactViewModelFactory(
                    // El MVP no activa la vinculación: AddContactUseCase usa
                    // DisabledContactAccountResolver y los contactos quedan pendientes (T-11, T-12).
                    addContactUseCase = AddContactUseCase(repositories.contactRepository),
                    currentUserId = { repositories.authRepository.currentUserId },
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
                        ),
                    ).get("text-conversation-$conversationId", TextConversationViewModel::class.java)
                },
            )
        }
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
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(TextConversationViewModel::class.java))
        return checkNotNull(modelClass.cast(TextConversationViewModel(conversationId, senderId, observeTextMessages, sendTextMessage)))
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
    private val currentUserId: () -> String?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(AddContactViewModel::class.java))
        return checkNotNull(
            modelClass.cast(
                AddContactViewModel(
                    addContactUseCase = addContactUseCase,
                    currentUserId = currentUserId,
                ),
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

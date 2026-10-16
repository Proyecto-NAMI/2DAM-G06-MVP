package com.proyectonami.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.proyectonami.app.core.data.firebase.AuthRepository
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.media.MediaSessionController
import com.proyectonami.app.feature.auth.presentation.PhoneAuthViewModel
import com.proyectonami.app.feature.contacts.domain.AddContactUseCase
import com.proyectonami.app.feature.contacts.presentation.AddContactViewModel
import com.proyectonami.app.ui.NamiApp

class MainActivity : ComponentActivity() {
    private lateinit var phoneAuthViewModel: PhoneAuthViewModel
    private lateinit var addContactViewModel: AddContactViewModel

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
        setContent {
            NamiApp(
                activity = this,
                viewModel = phoneAuthViewModel,
                addContactViewModel = addContactViewModel,
            )
        }
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

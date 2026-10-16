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
import com.proyectonami.app.ui.NamiApp

class MainActivity : ComponentActivity() {
    private lateinit var phoneAuthViewModel: PhoneAuthViewModel

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
        setContent {
            NamiApp(
                activity = this,
                viewModel = phoneAuthViewModel,
            )
        }
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

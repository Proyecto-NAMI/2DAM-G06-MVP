package com.proyectonami.app.feature.profile

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.proyectonami.app.core.data.firebase.AuthRepository
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.core.domain.auth.AccountProfile
import com.proyectonami.app.core.domain.auth.AccountProfileLoadResult
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.domain.auth.AccountProfileSaveResult
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.core.domain.auth.PhoneCodeVerificationResult
import com.proyectonami.app.core.domain.auth.PhoneVerificationChallenge
import com.proyectonami.app.core.domain.auth.PhoneVerificationRequestResult
import com.proyectonami.app.feature.profile.domain.LoadProfileUseCase
import com.proyectonami.app.feature.profile.domain.UpdateProfileUseCase
import com.proyectonami.app.feature.profile.presentation.ProfileRoute
import com.proyectonami.app.feature.profile.presentation.ProfileViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Auditoría T-39 del perfil a escala máxima y con Accessibility Test Framework. */
@OptIn(ExperimentalTestApi::class)
class ProfileScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = Activity()
        }
    }

    @Test
    fun profileRemainsUsableAt320DpAnd200Percent() {
        assertProfileAtWidth(320)
    }

    @Test
    fun profileRemainsUsableAt411DpAnd200Percent() {
        assertProfileAtWidth(411)
    }

    @Test
    fun loadErrorIsAnnouncedAndRetryRemainsAccessible() {
        val repository = FakeProfileRepository(AccountProfileLoadResult.Failed)
        val viewModel = viewModel(repository)
        var backRequests = 0
        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            NamiTheme(textScale = 2f) {
                ProfileRoute(activity, state, viewModel, onBack = { backRequests++ })
            }
        }

        composeRule
            .onNodeWithText("No se pudo cargar el perfil. Comprueba la conexión e inténtalo de nuevo.")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule.onNodeWithText("Reintentar").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Volver").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backRequests)
    }

    private fun assertProfileAtWidth(widthDp: Int) {
        val profile = AccountProfile(USER_ID, "Ana", "Prueba", "+34612345678")
        val repository = FakeProfileRepository(AccountProfileLoadResult.Loaded(profile))
        val viewModel = viewModel(repository)
        var backRequests = 0
        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            Box(Modifier.size(widthDp.dp, 720.dp)) {
                NamiTheme(textScale = 2f) {
                    ProfileRoute(activity, state, viewModel, onBack = { backRequests++ })
                }
            }
        }

        composeRule.onNodeWithText("Mi perfil").assertIsDisplayed()
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule
            .onNodeWithText("Guardar cambios")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithText("Volver")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, backRequests)
    }

    private fun viewModel(repository: AccountProfileRepository) =
        ProfileViewModel(
            authRepository = FakeAuthRepository,
            loadProfileUseCase = LoadProfileUseCase(repository),
            updateProfileUseCase = UpdateProfileUseCase(repository),
            currentUserId = { USER_ID },
        )

    private class FakeProfileRepository(
        private val loadResult: AccountProfileLoadResult,
    ) : AccountProfileRepository {
        override suspend fun saveProfile(profile: AccountProfile): AccountProfileSaveResult = AccountProfileSaveResult.Saved

        override suspend fun loadProfile(userId: String): AccountProfileLoadResult = loadResult

        override suspend fun updateProfile(profile: AccountProfile): AccountProfileSaveResult = AccountProfileSaveResult.Saved
    }

    private object FakeAuthRepository : AuthRepository {
        override val currentUserId: String = USER_ID

        override fun observeSession(): Flow<AuthSession> = flowOf(AuthSession.SignedIn(USER_ID))

        override suspend fun requestPhoneCode(
            activity: Activity,
            phoneNumber: String,
            previousChallenge: PhoneVerificationChallenge?,
        ): PhoneVerificationRequestResult = PhoneVerificationRequestResult.Failed(PhoneAuthFailure.UNKNOWN)

        override suspend fun verifyPhoneCode(
            challenge: PhoneVerificationChallenge,
            code: String,
        ): PhoneCodeVerificationResult = PhoneCodeVerificationResult.Failed(PhoneAuthFailure.UNKNOWN)

        override fun signOut() = Unit
    }

    private companion object {
        const val USER_ID = "profile-user-fictitious"
    }
}

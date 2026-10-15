package com.proyectonami.app.feature.auth

import android.app.Activity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.proyectonami.app.core.data.firebase.AuthRepository
import com.proyectonami.app.core.domain.auth.AccountProfile
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.domain.auth.AccountProfileSaveResult
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.core.domain.auth.PhoneCodeVerificationResult
import com.proyectonami.app.core.domain.auth.PhoneVerificationChallenge
import com.proyectonami.app.core.domain.auth.PhoneVerificationRequestResult
import com.proyectonami.app.feature.auth.presentation.PhoneAuthViewModel
import com.proyectonami.app.ui.NamiApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountCreationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private lateinit var activity: Activity
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var profileRepository: FakeAccountProfileRepository
    private lateinit var viewModel: PhoneAuthViewModel

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = Activity()
        }
        authRepository = FakeAuthRepository()
        profileRepository = FakeAccountProfileRepository()
        viewModel =
            PhoneAuthViewModel(
                authRepository = authRepository,
                accountProfileRepository = profileRepository,
            )
        composeRule.setContent {
            val fontScale = if (testName.methodName == "accountFormRemainsScrollableAt200PercentText") 2f else 1f
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                NamiApp(
                    activity = activity,
                    viewModel = viewModel,
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(PhoneAuthViewModel.DEFAULT_PHONE_PREFIX, viewModel.uiState.value.phoneNumber)
    }

    @Test
    fun verifiedAccountSavesProfileAndShowsSuccess() {
        fillAccountForm()
        composeRule.onNodeWithText("Enviar código de verificación").performClick()
        composeRule
            .onNodeWithText("Te hemos enviado un código. Escríbelo aquí para verificar tu teléfono.")
            .assertIsDisplayed()
        enterCode()
        composeRule
            .onNodeWithText("Verificar teléfono")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Cuenta creada").assertIsDisplayed()

        assertEquals("+15555550100", authRepository.requestedPhoneNumber)
        assertEquals(
            AccountProfile(
                userId = "fictitious-user",
                displayName = "Ana",
                surname = "Prueba",
                phoneNumber = "+15555550100",
            ),
            profileRepository.savedProfiles.single(),
        )
    }

    @Test
    fun incorrectCodeIsAnnouncedAndKeepsTheFormValues() {
        authRepository.verificationResult =
            PhoneCodeVerificationResult.Failed(PhoneAuthFailure.INVALID_CODE)
        fillAccountForm()
        composeRule.onNodeWithText("Enviar código de verificación").performClick()
        enterCode()
        composeRule
            .onNodeWithText("Verificar teléfono")
            .performScrollTo()
            .performClick()

        assertEquals(PhoneAuthFailure.INVALID_CODE, viewModel.uiState.value.failure)
        composeRule
            .onNodeWithText("Ese código no es correcto. Compruébalo o solicita otro.")
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals("Ana", viewModel.uiState.value.name)
        assertEquals("Prueba", viewModel.uiState.value.surname)
        assertEquals("+15555550100", viewModel.uiState.value.phoneNumber)
        assertEquals("123456", viewModel.uiState.value.verificationCode)
        assertEquals(0, profileRepository.savedProfiles.size)
    }

    @Test
    fun expiredCodeCanBeResentWithoutLosingTheForm() {
        authRepository.verificationResult =
            PhoneCodeVerificationResult.Failed(PhoneAuthFailure.CODE_EXPIRED)
        fillAccountForm()
        composeRule.onNodeWithText("Enviar código de verificación").performClick()
        enterCode()
        composeRule
            .onNodeWithText("Verificar teléfono")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithText("El código ha caducado. Solicita uno nuevo para continuar.")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Solicitar otro código")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { authRepository.requestCount == 2 }
        assertEquals(2, authRepository.requestCount)
        assertEquals(0, profileRepository.savedProfiles.size)
        assertEquals("Ana", viewModel.uiState.value.name)
        assertEquals("+15555550100", viewModel.uiState.value.phoneNumber)
        composeRule
            .onNodeWithText("Te hemos enviado un código. Escríbelo aquí para verificar tu teléfono.")
            .assertIsDisplayed()
    }

    @Test
    fun failedProfileSaveCanBeRetriedWithoutRequestingAnotherCode() {
        profileRepository.saveResults.add(AccountProfileSaveResult.Failed)
        profileRepository.saveResults.add(AccountProfileSaveResult.Saved)
        fillAccountForm()
        composeRule.onNodeWithText("Enviar código de verificación").performClick()
        enterCode()
        composeRule
            .onNodeWithText("Verificar teléfono")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithText(
                "El teléfono está verificado, pero no se pudo guardar tu cuenta. Comprueba la conexión e inténtalo de nuevo.",
            ).assertIsDisplayed()

        composeRule.onNodeWithText("Reintentar guardado").performClick()
        composeRule.onNodeWithText("Cuenta creada").assertIsDisplayed()

        assertEquals(1, authRepository.requestCount)
        assertEquals(2, profileRepository.savedProfiles.size)
    }

    @Test
    fun validationErrorsAreVisibleAndExposedToAssistiveTechnology() {
        composeRule.onNodeWithText("Enviar código de verificación").performClick()

        composeRule
            .onAllNodesWithText("Escribe este dato para continuar")
            .assertCountEquals(2)
        composeRule
            .onAllNodes(
                hasSetTextAction() and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Error),
            ).assertCountEquals(3)
        assertEquals(0, authRepository.requestCount)
    }

    @Test
    fun phoneNumberTypingStartsAfterDefaultCountryPrefix() {
        val phoneField = composeRule.onAllNodes(hasSetTextAction())[2]
        phoneField.performTouchInput {
            click(Offset(x = 1f, y = 50f))
        }
        phoneField.performTextInput("612345678")

        assertEquals("+34 612345678", viewModel.uiState.value.phoneNumber)
    }

    @Test
    fun accountFormRemainsScrollableAt200PercentText() {
        composeRule.onNodeWithText("Crear cuenta").assertIsDisplayed()
        composeRule
            .onNodeWithText("Enviar código de verificación")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun fillAccountForm() {
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Ana")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("Prueba")
        composeRule
            .onAllNodes(hasSetTextAction())[2]
            .performTextClearance()
        composeRule
            .onAllNodes(hasSetTextAction())[2]
            .performTextInput("+15555550100")
    }

    private fun enterCode() {
        composeRule
            .onAllNodes(hasSetTextAction())[3]
            .performTextInput("123456")
    }
}

private class FakeAuthRepository : AuthRepository {
    private val session = MutableStateFlow<AuthSession>(AuthSession.SignedOut)
    var requestedPhoneNumber: String? = null
        private set
    var requestCount: Int = 0
        private set
    var verificationResult: PhoneCodeVerificationResult =
        PhoneCodeVerificationResult.Verified("fictitious-user")

    override val currentUserId: String?
        get() = (session.value as? AuthSession.SignedIn)?.userId

    override fun observeSession(): Flow<AuthSession> = session

    override suspend fun requestPhoneCode(
        activity: Activity,
        phoneNumber: String,
        previousChallenge: PhoneVerificationChallenge?,
    ): PhoneVerificationRequestResult {
        requestedPhoneNumber = phoneNumber
        requestCount++
        return PhoneVerificationRequestResult.CodeSent(
            PhoneVerificationChallenge("fictitious-challenge-$requestCount"),
        )
    }

    override suspend fun verifyPhoneCode(
        challenge: PhoneVerificationChallenge,
        code: String,
    ): PhoneCodeVerificationResult = verificationResult

    override fun signOut() {
        session.value = AuthSession.SignedOut
    }
}

private class FakeAccountProfileRepository : AccountProfileRepository {
    val savedProfiles = mutableListOf<AccountProfile>()
    val saveResults = mutableListOf<AccountProfileSaveResult>()

    override suspend fun saveProfile(profile: AccountProfile): AccountProfileSaveResult {
        savedProfiles += profile
        return if (saveResults.isEmpty()) {
            AccountProfileSaveResult.Saved
        } else {
            saveResults.removeAt(0)
        }
    }
}

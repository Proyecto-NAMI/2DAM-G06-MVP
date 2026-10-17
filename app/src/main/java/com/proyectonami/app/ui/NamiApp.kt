package com.proyectonami.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.component.NamiPrimaryButton
import com.proyectonami.app.core.designsystem.component.NamiSecondaryButton
import com.proyectonami.app.core.designsystem.component.NamiTextField
import com.proyectonami.app.core.designsystem.theme.NamiSpacing
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.core.domain.accessibility.AccessibilityPreferencesRepository
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.feature.auth.presentation.PhoneAuthUiState
import com.proyectonami.app.feature.auth.presentation.PhoneAuthViewModel
import com.proyectonami.app.feature.chat.presentation.TextConversationRoute
import com.proyectonami.app.feature.chat.presentation.TextConversationViewModel
import com.proyectonami.app.feature.contacts.presentation.AddContactRoute
import com.proyectonami.app.feature.contacts.presentation.AddContactViewModel
import com.proyectonami.app.feature.conversations.presentation.ConversationsRoute
import com.proyectonami.app.feature.conversations.presentation.ConversationsViewModel
import com.proyectonami.app.feature.profile.presentation.ProfileRoute
import com.proyectonami.app.feature.profile.presentation.ProfileViewModel

@Composable
fun NamiApp(
    activity: Activity,
    viewModel: PhoneAuthViewModel,
    addContactViewModel: AddContactViewModel,
    profileViewModel: ProfileViewModel? = null,
    conversationsViewModel: ConversationsViewModel? = null,
    createConversationViewModel: ((String) -> TextConversationViewModel)? = null,
    accessibilityPreferencesRepository: AccessibilityPreferencesRepository? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val accessibilityPreferences by (
        accessibilityPreferencesRepository?.preferences
            ?: kotlinx.coroutines.flow.flowOf(
                com.proyectonami.app.core.domain.accessibility
                    .AccessibilityPreferences(),
            )
    ).collectAsState(
        initial =
            com.proyectonami.app.core.domain.accessibility
                .AccessibilityPreferences(),
    )
    val signedInUserId = (state.session as? AuthSession.SignedIn)?.userId
    // El alta de contactos se abre desde el estado «Cuenta creada» hasta que T-15
    // construya la pantalla principal. Al cambiar de sesión se vuelve a la raíz.
    var showAddContact by remember(signedInUserId) { mutableStateOf(false) }
    var showProfile by remember(signedInUserId) { mutableStateOf(false) }
    var selectedConversation by remember(signedInUserId) { mutableStateOf<TextConversationViewModel?>(null) }
    NamiTheme(textScale = accessibilityPreferences.textScale) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                state.session is AuthSession.Restoring ->
                    LoadingScreen()
                signedInUserId != null && state.isAccountReady && selectedConversation != null ->
                    TextConversationRoute(
                        viewModel = selectedConversation!!,
                        onBack = { selectedConversation = null },
                    )
                signedInUserId != null && state.isAccountReady && showProfile && profileViewModel != null ->
                    ProfileRoute(
                        activity = activity,
                        state = profileViewModel.uiState.collectAsState().value,
                        viewModel = profileViewModel,
                        onBack = { showProfile = false },
                    )
                signedInUserId != null && state.isAccountReady && showAddContact ->
                    AddContactRoute(
                        viewModel = addContactViewModel,
                        onBack = { showAddContact = false },
                        onOpenConversation = { conversationId ->
                            createConversationViewModel?.let { factory ->
                                showAddContact = false
                                selectedConversation = factory(conversationId)
                            }
                        },
                    )
                signedInUserId != null && state.isAccountReady ->
                    if (conversationsViewModel != null && createConversationViewModel != null) {
                        ConversationsRoute(
                            viewModel = conversationsViewModel,
                            onOpenConversation = { id -> selectedConversation = createConversationViewModel(id) },
                            onSignOut = viewModel::signOut,
                            onAddContact = { showAddContact = true },
                            onProfile = {
                                profileViewModel?.load()
                                showProfile = profileViewModel != null
                            },
                        )
                    } else {
                        AccountReadyScreen(
                            onSignOut = viewModel::signOut,
                            onAddContact = { showAddContact = true },
                            onProfile = {
                                profileViewModel?.load()
                                showProfile = profileViewModel != null
                            },
                        )
                    }
                else ->
                    CreateAccountScreen(
                        activity = activity,
                        state = state,
                        viewModel = viewModel,
                    )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.account_restoring_session),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun CreateAccountScreen(
    activity: Activity,
    state: PhoneAuthUiState,
    viewModel: PhoneAuthViewModel,
) {
    val focusManager = LocalFocusManager.current
    val formEnabled =
        state.verifiedUserId == null &&
            !state.isRequestingCode &&
            !state.isVerifyingCode &&
            !state.isSavingProfile

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
    ) {
        Text(
            text = stringResource(R.string.account_create_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.account_create_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.mediaCleanupFailed) {
            Text(
                text = stringResource(R.string.logout_media_cleanup_failed),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        NamiTextField(
            value = state.name,
            onValueChange = viewModel::updateName,
            label = stringResource(R.string.account_name_label),
            errorMessage = personNameErrorMessage(state.nameError),
            enabled = formEnabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions =
                KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
        )
        NamiTextField(
            value = state.surname,
            onValueChange = viewModel::updateSurname,
            label = stringResource(R.string.account_surname_label),
            errorMessage = personNameErrorMessage(state.surnameError),
            enabled = formEnabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions =
                KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
        )
        NamiTextField(
            value = state.phoneNumber,
            onValueChange = viewModel::updatePhoneNumber,
            label = stringResource(R.string.account_phone_label),
            supportingText = stringResource(R.string.account_phone_supporting),
            errorMessage = phoneNumberErrorMessage(state.phoneNumberError),
            enabled = formEnabled,
            moveCursorToEndOnFocus = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (state.challenge == null) {
                            viewModel.requestCode(activity)
                        }
                    },
                ),
        )

        if (state.challenge != null && state.verifiedUserId == null) {
            Text(
                text = stringResource(R.string.account_code_sent),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
            )
            NamiTextField(
                value = state.verificationCode,
                onValueChange = viewModel::updateVerificationCode,
                label = stringResource(R.string.account_code_label),
                enabled = !state.isVerifyingCode && !state.isSavingProfile,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.verifyCode()
                        },
                    ),
            )
        }

        state.failure?.let { failure ->
            Text(
                text = stringResource(failure.messageResource()),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (state.profileSaveFailed) {
            Text(
                text = stringResource(R.string.account_profile_save_failed),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
            NamiPrimaryButton(
                text = stringResource(R.string.account_retry_profile_save),
                icon = painterResource(R.drawable.ic_nami_refresh),
                onClick = viewModel::retryProfileSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSavingProfile,
            )
        } else if (state.isSavingProfile) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.account_saving_profile),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
            )
        } else if (state.challenge == null) {
            NamiPrimaryButton(
                text =
                    stringResource(
                        if (state.isRequestingCode) {
                            R.string.account_requesting_code
                        } else {
                            R.string.account_request_code
                        },
                    ),
                icon = painterResource(R.drawable.ic_nami_arrow_forward),
                onClick = {
                    focusManager.clearFocus()
                    viewModel.requestCode(activity)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRequestingCode,
            )
        } else if (state.verifiedUserId == null) {
            NamiPrimaryButton(
                text =
                    stringResource(
                        if (state.isVerifyingCode) {
                            R.string.account_verifying_code
                        } else {
                            R.string.account_verify_code
                        },
                    ),
                icon = painterResource(R.drawable.ic_nami_check),
                onClick = {
                    focusManager.clearFocus()
                    viewModel.verifyCode()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isVerifyingCode && !state.isRequestingCode,
            )
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.requestCode(activity)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRequestingCode && !state.isVerifyingCode,
            ) {
                Text(
                    text =
                        stringResource(
                            if (state.isRequestingCode) {
                                R.string.account_requesting_another_code
                            } else {
                                R.string.account_resend_code
                            },
                        ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun AccountReadyScreen(
    onSignOut: () -> Unit,
    onAddContact: () -> Unit,
    onProfile: () -> Unit,
) {
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
    ) {
        Text(
            text = stringResource(R.string.account_created_title),
            modifier =
                Modifier
                    .semantics { heading() }
                    .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.account_created_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        // Punto de entrada provisional al alta de contactos hasta que T-15
        // construya la pantalla principal con la lista de conversaciones.
        NamiSecondaryButton(
            text = stringResource(R.string.contact_add_entry),
            icon = painterResource(R.drawable.ic_nami_add),
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth(),
        )
        NamiSecondaryButton(
            text = stringResource(R.string.profile_entry),
            icon = painterResource(R.drawable.ic_nami_check),
            onClick = onProfile,
            modifier = Modifier.fillMaxWidth(),
        )
        NamiPrimaryButton(
            text = stringResource(R.string.logout_request),
            icon = painterResource(R.drawable.ic_nami_logout),
            onClick = { showSignOutConfirmation = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = {
                Text(
                    text = stringResource(R.string.logout_confirmation_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.logout_confirmation_message),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirmation = false
                        onSignOut()
                    },
                ) {
                    Text(stringResource(R.string.logout_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmation = false }) {
                    Text(stringResource(R.string.logout_cancel))
                }
            },
        )
    }
}

private fun PhoneAuthFailure.messageResource(): Int =
    when (this) {
        PhoneAuthFailure.INVALID_PHONE_NUMBER -> R.string.auth_phone_invalid_phone_number
        PhoneAuthFailure.INVALID_CODE -> R.string.auth_phone_invalid_code
        PhoneAuthFailure.CODE_EXPIRED -> R.string.auth_phone_code_expired
        PhoneAuthFailure.TOO_MANY_REQUESTS -> R.string.auth_phone_too_many_requests
        PhoneAuthFailure.NETWORK -> R.string.auth_phone_network
        PhoneAuthFailure.UNKNOWN -> R.string.auth_phone_unknown
    }

@Preview(showBackground = true)
@Composable
private fun CreateAccountScreenPreview() {
    NamiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Text(
                text = stringResource(R.string.account_create_title),
                modifier = Modifier.padding(NamiSpacing.Large),
            )
        }
    }
}

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.proyectonami.app.core.designsystem.component.NamiTextField
import com.proyectonami.app.core.designsystem.theme.NamiSpacing
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.feature.auth.presentation.PhoneAuthUiState
import com.proyectonami.app.feature.auth.presentation.PhoneAuthViewModel

@Composable
fun NamiApp(
    activity: Activity,
    viewModel: PhoneAuthViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    NamiTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                state.session is AuthSession.Restoring ->
                    LoadingScreen()
                state.session is AuthSession.SignedIn && state.isAccountReady ->
                    AccountReadyScreen()
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
private fun AccountReadyScreen() {
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
    }
}

@Composable
private fun personNameErrorMessage(error: PersonNameError?): String? =
    error?.let {
        stringResource(
            when (it) {
                PersonNameError.EMPTY -> R.string.validation_person_name_empty
                PersonNameError.TOO_LONG -> R.string.validation_person_name_too_long
                PersonNameError.INVALID_CHARACTERS -> R.string.validation_person_name_invalid_characters
                PersonNameError.INVALID_FORMAT -> R.string.validation_person_name_invalid_format
            },
        )
    }

@Composable
private fun phoneNumberErrorMessage(error: PhoneNumberError?): String? =
    error?.let {
        stringResource(
            when (it) {
                PhoneNumberError.EMPTY -> R.string.validation_phone_number_empty
                PhoneNumberError.INVALID_CHARACTERS -> R.string.validation_phone_number_invalid_characters
                PhoneNumberError.MISSING_COUNTRY_CODE -> R.string.validation_phone_number_missing_country_code
                PhoneNumberError.TOO_SHORT -> R.string.validation_phone_number_too_short
                PhoneNumberError.TOO_LONG -> R.string.validation_phone_number_too_long
                PhoneNumberError.INVALID_COUNTRY_CODE -> R.string.validation_phone_number_invalid_country_code
            },
        )
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

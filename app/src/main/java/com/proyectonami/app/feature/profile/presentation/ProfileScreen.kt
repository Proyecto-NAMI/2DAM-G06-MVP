package com.proyectonami.app.feature.profile.presentation

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.component.NamiPrimaryButton
import com.proyectonami.app.core.designsystem.component.NamiSecondaryButton
import com.proyectonami.app.core.designsystem.component.NamiTextField
import com.proyectonami.app.core.designsystem.theme.NamiSpacing
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PhoneNumberError

@Composable
fun ProfileRoute(
    activity: Activity,
    state: ProfileUiState,
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
) {
    if (state.isLoading) {
        Column(Modifier.fillMaxSize().padding(NamiSpacing.Large), verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
            Text(stringResource(R.string.profile_loading), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        return
    }
    if (state.loadFailed) {
        Column(
            Modifier.fillMaxSize().padding(NamiSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
        ) {
            Text(stringResource(R.string.profile_load_failed), color = MaterialTheme.colorScheme.error)
            NamiPrimaryButton(
                stringResource(R.string.profile_retry),
                painterResource(R.drawable.ic_nami_refresh),
                viewModel::retry,
                Modifier.fillMaxWidth(),
            )
            NamiSecondaryButton(
                stringResource(R.string.profile_back),
                painterResource(R.drawable.ic_nami_back),
                onBack,
                Modifier.fillMaxWidth(),
            )
        }
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing,
            ).imePadding()
            .verticalScroll(rememberScrollState())
            .padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
    ) {
        Text(
            stringResource(R.string.profile_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(stringResource(R.string.profile_description), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(
                R.string.profile_avatar_preview,
                state.avatarPath.ifBlank {
                    stringResource(R.string.profile_avatar_none)
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        NamiTextField(
            state.name,
            viewModel::updateName,
            stringResource(R.string.account_name_label),
            errorMessage = personError(state.nameError),
            enabled = state.isFormEnabled,
        )
        NamiTextField(
            state.surname,
            viewModel::updateSurname,
            stringResource(R.string.account_surname_label),
            errorMessage = personError(state.surnameError),
            enabled = state.isFormEnabled,
        )
        NamiTextField(
            state.phoneNumber,
            viewModel::updatePhoneNumber,
            stringResource(R.string.account_phone_label),
            supportingText = stringResource(R.string.profile_phone_supporting),
            errorMessage = phoneError(state.phoneNumberError),
            enabled = state.isFormEnabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        NamiTextField(
            state.avatarPath,
            viewModel::updateAvatarPath,
            stringResource(R.string.profile_avatar_label),
            supportingText = stringResource(R.string.profile_avatar_supporting),
            enabled = state.isFormEnabled,
        )
        if (state.phoneVerificationRequired) {
            Text(
                stringResource(R.string.profile_phone_requires_verification),
                modifier =
                    Modifier.semantics {
                        liveRegion =
                            LiveRegionMode.Polite
                    },
            )
            state.challenge?.let {
                NamiTextField(
                    state.verificationCode,
                    viewModel::updateVerificationCode,
                    stringResource(R.string.account_code_label),
                    enabled = !state.isVerifyingCode,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                )
            }
        }
        state.failure?.let {
            Text(
                stringResource(it.messageResource()),
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier.semantics {
                        liveRegion =
                            LiveRegionMode.Assertive
                    },
            )
        }
        if (state.saveFailed) {
            Text(
                stringResource(R.string.profile_save_failed),
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier.semantics {
                        liveRegion =
                            LiveRegionMode.Assertive
                    },
            )
        }
        if (state.avatarError) Text(stringResource(R.string.profile_avatar_invalid), color = MaterialTheme.colorScheme.error)
        if (state.saved) Text(stringResource(R.string.profile_saved), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        NamiPrimaryButton(
            text =
                when {
                    state.isSaving -> stringResource(R.string.profile_saving)
                    state.isRequestingCode -> stringResource(R.string.profile_requesting_code)
                    state.isVerifyingCode -> stringResource(R.string.profile_verifying_code)
                    else -> stringResource(R.string.profile_save)
                },
            icon = painterResource(R.drawable.ic_nami_check),
            onClick = { viewModel.save(activity) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isFormEnabled,
        )
        NamiSecondaryButton(
            stringResource(R.string.profile_back),
            painterResource(R.drawable.ic_nami_back),
            onBack,
            Modifier.fillMaxWidth(),
        )
    }
}

private fun personError(error: PersonNameError?): String? =
    error?.let {
        when (it) {
            PersonNameError.EMPTY -> "Escribe este dato para continuar"
            PersonNameError.TOO_LONG -> "Es demasiado largo: máximo 50 caracteres"
            PersonNameError.INVALID_CHARACTERS -> "Solo se admiten letras, espacios, guiones y apóstrofos"
            PersonNameError.INVALID_FORMAT -> "Revisa el formato del nombre"
        }
    }

private fun phoneError(error: PhoneNumberError?): String? =
    error?.let {
        when (it) {
            PhoneNumberError.EMPTY -> "Escribe el teléfono para continuar"
            PhoneNumberError.INVALID_CHARACTERS -> "Revisa los caracteres del teléfono"
            PhoneNumberError.MISSING_COUNTRY_CODE -> "Incluye el prefijo del país, por ejemplo +34"
            PhoneNumberError.TOO_SHORT -> "Faltan cifras en el teléfono"
            PhoneNumberError.TOO_LONG -> "El teléfono tiene demasiadas cifras"
            PhoneNumberError.INVALID_COUNTRY_CODE -> "El prefijo del país no es válido"
        }
    }

private fun com.proyectonami.app.core.domain.auth.PhoneAuthFailure.messageResource(): Int =
    when (this) {
        com.proyectonami.app.core.domain.auth.PhoneAuthFailure.INVALID_PHONE_NUMBER -> R.string.auth_phone_invalid_phone_number
        com.proyectonami.app.core.domain.auth.PhoneAuthFailure.INVALID_CODE -> R.string.auth_phone_invalid_code
        com.proyectonami.app.core.domain.auth.PhoneAuthFailure.CODE_EXPIRED -> R.string.auth_phone_code_expired
        com.proyectonami.app.core.domain.auth.PhoneAuthFailure.TOO_MANY_REQUESTS -> R.string.auth_phone_too_many_requests
        com.proyectonami.app.core.domain.auth.PhoneAuthFailure.NETWORK -> R.string.auth_phone_network
        com.proyectonami.app.core.domain.auth.PhoneAuthFailure.UNKNOWN -> R.string.auth_phone_unknown
    }

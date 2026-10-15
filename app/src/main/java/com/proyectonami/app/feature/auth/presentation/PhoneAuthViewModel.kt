package com.proyectonami.app.feature.auth.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.data.firebase.AuthRepository
import com.proyectonami.app.core.domain.auth.AccountProfile
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.domain.auth.AccountProfileSaveResult
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.core.domain.auth.PhoneCodeVerificationResult
import com.proyectonami.app.core.domain.auth.PhoneVerificationChallenge
import com.proyectonami.app.core.domain.auth.PhoneVerificationRequestResult
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PersonNameValidation
import com.proyectonami.app.core.domain.validation.PersonNameValidator
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.core.domain.validation.PhoneNumberValidation
import com.proyectonami.app.core.domain.validation.PhoneNumberValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhoneAuthUiState(
    val name: String = "",
    val surname: String = "",
    val phoneNumber: String = "",
    val verificationCode: String = "",
    val challenge: PhoneVerificationChallenge? = null,
    val session: AuthSession = AuthSession.Restoring,
    val nameError: PersonNameError? = null,
    val surnameError: PersonNameError? = null,
    val phoneNumberError: PhoneNumberError? = null,
    val verifiedUserId: String? = null,
    val isAccountReady: Boolean = false,
    val isRequestingCode: Boolean = false,
    val isVerifyingCode: Boolean = false,
    val isSavingProfile: Boolean = false,
    val profileSaveFailed: Boolean = false,
    val failure: PhoneAuthFailure? = null,
)

class PhoneAuthViewModel(
    private val authRepository: AuthRepository,
    private val accountProfileRepository: AccountProfileRepository,
) : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(
            PhoneAuthUiState(phoneNumber = DEFAULT_PHONE_PREFIX),
        )
    val uiState: StateFlow<PhoneAuthUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                mutableUiState.update { state ->
                    state.copy(
                        session = session,
                        isAccountReady =
                            when (session) {
                                AuthSession.Restoring, AuthSession.SignedOut -> false
                                is AuthSession.SignedIn ->
                                    if (state.verifiedUserId == session.userId) {
                                        state.isAccountReady
                                    } else {
                                        !state.isRequestingCode &&
                                            !state.isVerifyingCode &&
                                            !state.isSavingProfile &&
                                            state.verifiedUserId == null
                                    }
                            },
                    )
                }
            }
        }
    }

    fun updateName(value: String) {
        mutableUiState.update {
            if (it.verifiedUserId == null) {
                it.copy(name = value, nameError = null)
            } else {
                it
            }
        }
    }

    fun updateSurname(value: String) {
        mutableUiState.update {
            if (it.verifiedUserId == null) {
                it.copy(surname = value, surnameError = null)
            } else {
                it
            }
        }
    }

    fun updatePhoneNumber(value: String) {
        mutableUiState.update {
            if (it.verifiedUserId == null) {
                it.copy(
                    phoneNumber = value,
                    challenge = if (it.phoneNumber == value) it.challenge else null,
                    phoneNumberError = null,
                    failure = null,
                )
            } else {
                it
            }
        }
    }

    fun updateVerificationCode(value: String) {
        mutableUiState.update {
            it.copy(
                verificationCode = value,
                failure = null,
            )
        }
    }

    fun requestCode(activity: Activity) {
        val previousState = mutableUiState.value
        if (previousState.isRequestingCode || previousState.isVerifyingCode || previousState.isSavingProfile) {
            return
        }
        val nameValidation = PersonNameValidator.validate(previousState.name)
        val surnameValidation = PersonNameValidator.validate(previousState.surname)
        val phoneValidation = PhoneNumberValidator.validate(previousState.phoneNumber)
        if (nameValidation !is PersonNameValidation.Valid ||
            surnameValidation !is PersonNameValidation.Valid ||
            phoneValidation !is PhoneNumberValidation.Valid
        ) {
            mutableUiState.update {
                it.copy(
                    nameError = (nameValidation as? PersonNameValidation.Invalid)?.error,
                    surnameError = (surnameValidation as? PersonNameValidation.Invalid)?.error,
                    phoneNumberError = (phoneValidation as? PhoneNumberValidation.Invalid)?.error,
                    failure = null,
                )
            }
            return
        }
        val normalizedName = nameValidation.value
        val normalizedSurname = surnameValidation.value
        val normalizedPhoneNumber = phoneValidation.e164
        mutableUiState.update {
            it.copy(
                name = normalizedName,
                surname = normalizedSurname,
                phoneNumber = normalizedPhoneNumber,
                nameError = null,
                surnameError = null,
                phoneNumberError = null,
                isRequestingCode = true,
                failure = null,
                profileSaveFailed = false,
            )
        }
        viewModelScope.launch {
            val result =
                authRepository.requestPhoneCode(
                    activity = activity,
                    phoneNumber = normalizedPhoneNumber,
                    previousChallenge = previousState.challenge,
                )
            mutableUiState.update { state ->
                when (result) {
                    is PhoneVerificationRequestResult.CodeSent ->
                        state.copy(
                            challenge = result.challenge,
                            verificationCode = "",
                            isRequestingCode = false,
                            failure = null,
                        )
                    is PhoneVerificationRequestResult.Verified ->
                        state
                    is PhoneVerificationRequestResult.Failed ->
                        state.copy(isRequestingCode = false, failure = result.reason)
                }
            }
            if (result is PhoneVerificationRequestResult.Verified) {
                saveVerifiedProfile(
                    userId = result.userId,
                    name = normalizedName,
                    surname = normalizedSurname,
                    phoneNumber = normalizedPhoneNumber,
                )
            }
        }
    }

    fun verifyCode() {
        val previousState = mutableUiState.value
        if (previousState.isRequestingCode || previousState.isVerifyingCode || previousState.isSavingProfile) {
            return
        }
        val challenge = previousState.challenge
        if (challenge == null) {
            mutableUiState.update { it.copy(failure = PhoneAuthFailure.INVALID_CODE) }
            return
        }
        mutableUiState.update { it.copy(isVerifyingCode = true, failure = null) }
        viewModelScope.launch {
            val result =
                authRepository.verifyPhoneCode(
                    challenge = challenge,
                    code = previousState.verificationCode,
                )
            mutableUiState.update { state ->
                when (result) {
                    is PhoneCodeVerificationResult.Verified ->
                        state
                    is PhoneCodeVerificationResult.Failed ->
                        state.copy(isVerifyingCode = false, failure = result.reason)
                }
            }
            if (result is PhoneCodeVerificationResult.Verified) {
                saveVerifiedProfile(
                    userId = result.userId,
                    name = previousState.name,
                    surname = previousState.surname,
                    phoneNumber = previousState.phoneNumber,
                )
            }
        }
    }

    fun retryProfileSave() {
        val state = mutableUiState.value
        val userId = state.verifiedUserId ?: (state.session as? AuthSession.SignedIn)?.userId ?: return
        if (state.isSavingProfile || state.isAccountReady) {
            return
        }
        viewModelScope.launch {
            saveVerifiedProfile(
                userId = userId,
                name = state.name,
                surname = state.surname,
                phoneNumber = state.phoneNumber,
            )
        }
    }

    private suspend fun saveVerifiedProfile(
        userId: String,
        name: String,
        surname: String,
        phoneNumber: String,
    ) {
        mutableUiState.update {
            it.copy(
                session = AuthSession.SignedIn(userId),
                verifiedUserId = userId,
                name = name,
                surname = surname,
                phoneNumber = phoneNumber,
                isRequestingCode = false,
                isVerifyingCode = false,
                isSavingProfile = true,
                profileSaveFailed = false,
                isAccountReady = false,
                failure = null,
            )
        }
        when (
            accountProfileRepository.saveProfile(
                AccountProfile(
                    userId = userId,
                    displayName = name,
                    surname = surname,
                    phoneNumber = phoneNumber,
                ),
            )
        ) {
            AccountProfileSaveResult.Saved ->
                mutableUiState.update {
                    it.copy(
                        isSavingProfile = false,
                        profileSaveFailed = false,
                        isAccountReady = true,
                    )
                }
            AccountProfileSaveResult.Failed ->
                mutableUiState.update {
                    it.copy(
                        isSavingProfile = false,
                        profileSaveFailed = true,
                        isAccountReady = false,
                    )
                }
        }
    }

    companion object {
        const val DEFAULT_PHONE_PREFIX = "+34 "
    }
}

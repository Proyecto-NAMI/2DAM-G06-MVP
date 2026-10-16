package com.proyectonami.app.feature.profile.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.data.firebase.AuthRepository
import com.proyectonami.app.core.domain.auth.AccountProfileLoadResult
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.core.domain.auth.PhoneCodeVerificationResult
import com.proyectonami.app.core.domain.auth.PhoneVerificationChallenge
import com.proyectonami.app.core.domain.auth.PhoneVerificationRequestResult
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.feature.profile.domain.LoadProfileUseCase
import com.proyectonami.app.feature.profile.domain.ProfileDraft
import com.proyectonami.app.feature.profile.domain.ProfileFieldError
import com.proyectonami.app.feature.profile.domain.ProfileUpdateResult
import com.proyectonami.app.feature.profile.domain.UpdateProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val userId: String? = null,
    val name: String = "",
    val surname: String = "",
    val phoneNumber: String = "",
    val avatarPath: String = "",
    val nameError: PersonNameError? = null,
    val surnameError: PersonNameError? = null,
    val phoneNumberError: PhoneNumberError? = null,
    val avatarError: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
    val phoneVerificationRequired: Boolean = false,
    val challenge: PhoneVerificationChallenge? = null,
    val verificationCode: String = "",
    val isRequestingCode: Boolean = false,
    val isVerifyingCode: Boolean = false,
    val failure: PhoneAuthFailure? = null,
) {
    val isFormEnabled: Boolean
        get() = !isLoading && !isSaving && !isRequestingCode && !isVerifyingCode
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val loadProfileUseCase: LoadProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val currentUserId: () -> String?,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = mutableUiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val userId = currentUserId()
        if (userId.isNullOrBlank()) {
            mutableUiState.value = ProfileUiState(isLoading = false, loadFailed = true)
            return
        }
        mutableUiState.update { it.copy(isLoading = true, loadFailed = false, userId = userId) }
        viewModelScope.launch {
            when (val result = loadProfileUseCase.load(userId)) {
                is AccountProfileLoadResult.Loaded ->
                    mutableUiState.value =
                        ProfileUiState(
                            isLoading = false,
                            userId = userId,
                            name = result.profile.displayName,
                            surname = result.profile.surname,
                            phoneNumber = result.profile.phoneNumber,
                            avatarPath = result.profile.avatarPath.orEmpty(),
                        )
                AccountProfileLoadResult.Missing, AccountProfileLoadResult.Failed ->
                    mutableUiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun updateName(value: String) = mutableUiState.update { it.copy(name = value, nameError = null, saved = false) }

    fun updateSurname(value: String) = mutableUiState.update { it.copy(surname = value, surnameError = null, saved = false) }

    fun updateAvatarPath(value: String) = mutableUiState.update { it.copy(avatarPath = value, avatarError = false, saved = false) }

    fun updatePhoneNumber(value: String) =
        mutableUiState.update {
            it.copy(
                phoneNumber = value,
                phoneNumberError = null,
                phoneVerificationRequired = value != it.phoneNumber,
                challenge = null,
                failure = null,
                saved = false,
            )
        }

    fun updateVerificationCode(value: String) = mutableUiState.update { it.copy(verificationCode = value, failure = null) }

    fun save(activity: Activity) {
        val state = mutableUiState.value
        if (!state.isFormEnabled || state.userId == null) return
        if (state.phoneVerificationRequired && state.challenge == null) {
            requestPhoneVerification(activity)
            return
        }
        if (state.phoneVerificationRequired) {
            verifyPhoneAndSave()
        } else {
            viewModelScope.launch { persist(state) }
        }
    }

    fun retry() {
        if (mutableUiState.value.loadFailed) load() else mutableUiState.update { it.copy(saveFailed = false) }
    }

    private fun requestPhoneVerification(activity: Activity) {
        val state = mutableUiState.value
        mutableUiState.update { it.copy(isRequestingCode = true, failure = null, saveFailed = false) }
        viewModelScope.launch {
            when (val result = authRepository.requestPhoneCode(activity, state.phoneNumber, state.challenge)) {
                is PhoneVerificationRequestResult.CodeSent ->
                    mutableUiState.update { it.copy(isRequestingCode = false, challenge = result.challenge) }
                is PhoneVerificationRequestResult.Verified ->
                    mutableUiState.update { it.copy(isRequestingCode = false, challenge = null) }
                is PhoneVerificationRequestResult.Failed ->
                    mutableUiState.update { it.copy(isRequestingCode = false, failure = result.reason) }
            }
        }
    }

    private fun verifyPhoneAndSave() {
        val state = mutableUiState.value
        val challenge = state.challenge ?: return
        mutableUiState.update { it.copy(isVerifyingCode = true, failure = null) }
        viewModelScope.launch {
            when (val result = authRepository.verifyPhoneNumberChange(challenge, state.verificationCode)) {
                is PhoneCodeVerificationResult.Verified -> persist(mutableUiState.value)
                is PhoneCodeVerificationResult.Failed ->
                    mutableUiState.update { it.copy(isVerifyingCode = false, failure = result.reason) }
            }
        }
    }

    private suspend fun persist(state: ProfileUiState) {
        mutableUiState.update { it.copy(isSaving = true, saveFailed = false, saved = false) }
        when (
            val result =
                updateProfileUseCase.update(
                    state.userId ?: return,
                    ProfileDraft(state.name, state.surname, state.phoneNumber, state.avatarPath),
                )
        ) {
            is ProfileUpdateResult.Validated ->
                mutableUiState.update {
                    it.copy(
                        isSaving = false,
                        isVerifyingCode = false,
                        phoneVerificationRequired = false,
                        challenge = null,
                        saved = true,
                        name = result.profile.displayName,
                        surname = result.profile.surname,
                        phoneNumber = result.profile.phoneNumber,
                        avatarPath = result.profile.avatarPath.orEmpty(),
                    )
                }
            is ProfileUpdateResult.Invalid ->
                mutableUiState.update { it.copy(isSaving = false, isVerifyingCode = false).withError(result.error) }
            ProfileUpdateResult.Failed ->
                mutableUiState.update { it.copy(isSaving = false, isVerifyingCode = false, saveFailed = true) }
        }
    }

    private fun ProfileUiState.withError(error: ProfileFieldError): ProfileUiState =
        when (error) {
            is ProfileFieldError.Name -> copy(nameError = error.reason)
            is ProfileFieldError.Surname -> copy(surnameError = error.reason)
            is ProfileFieldError.Phone -> copy(phoneNumberError = error.reason)
            ProfileFieldError.Avatar -> copy(avatarError = true)
        }
}

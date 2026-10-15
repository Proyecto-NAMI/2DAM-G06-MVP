package com.proyectonami.app.feature.auth.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyectonami.app.core.data.firebase.AuthRepository
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.core.domain.auth.PhoneCodeVerificationResult
import com.proyectonami.app.core.domain.auth.PhoneVerificationChallenge
import com.proyectonami.app.core.domain.auth.PhoneVerificationRequestResult
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
    val isRequestingCode: Boolean = false,
    val isVerifyingCode: Boolean = false,
    val failure: PhoneAuthFailure? = null,
)

class PhoneAuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PhoneAuthUiState())
    val uiState: StateFlow<PhoneAuthUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                mutableUiState.update { it.copy(session = session) }
            }
        }
    }

    fun updateName(value: String) {
        mutableUiState.update { it.copy(name = value) }
    }

    fun updateSurname(value: String) {
        mutableUiState.update { it.copy(surname = value) }
    }

    fun updatePhoneNumber(value: String) {
        mutableUiState.update {
            it.copy(
                phoneNumber = value,
                challenge = if (it.phoneNumber == value) it.challenge else null,
                failure = null,
            )
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
        if (previousState.isRequestingCode || previousState.isVerifyingCode) {
            return
        }
        mutableUiState.update { it.copy(isRequestingCode = true, failure = null) }
        viewModelScope.launch {
            val result =
                authRepository.requestPhoneCode(
                    activity = activity,
                    phoneNumber = previousState.phoneNumber,
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
                        state.copy(
                            session = AuthSession.SignedIn(result.userId),
                            isRequestingCode = false,
                            failure = null,
                        )
                    is PhoneVerificationRequestResult.Failed ->
                        state.copy(isRequestingCode = false, failure = result.reason)
                }
            }
        }
    }

    fun verifyCode() {
        val previousState = mutableUiState.value
        if (previousState.isRequestingCode || previousState.isVerifyingCode) {
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
                        state.copy(
                            session = AuthSession.SignedIn(result.userId),
                            isVerifyingCode = false,
                            failure = null,
                        )
                    is PhoneCodeVerificationResult.Failed ->
                        state.copy(isVerifyingCode = false, failure = result.reason)
                }
            }
        }
    }
}

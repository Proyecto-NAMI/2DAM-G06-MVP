package com.proyectonami.app.core.domain.auth

enum class PhoneAuthFailure {
    INVALID_PHONE_NUMBER,
    INVALID_CODE,
    CODE_EXPIRED,
    TOO_MANY_REQUESTS,
    NETWORK,
    UNKNOWN,
}

sealed interface AuthSession {
    data object Restoring : AuthSession

    data object SignedOut : AuthSession

    data class SignedIn(
        val userId: String,
    ) : AuthSession
}

data class PhoneVerificationChallenge(
    val verificationId: String,
)

sealed interface PhoneVerificationRequestResult {
    data class CodeSent(
        val challenge: PhoneVerificationChallenge,
    ) : PhoneVerificationRequestResult

    data class Verified(
        val userId: String,
    ) : PhoneVerificationRequestResult

    data class Failed(
        val reason: PhoneAuthFailure,
    ) : PhoneVerificationRequestResult
}

sealed interface PhoneCodeVerificationResult {
    data class Verified(
        val userId: String,
    ) : PhoneCodeVerificationResult

    data class Failed(
        val reason: PhoneAuthFailure,
    ) : PhoneCodeVerificationResult
}

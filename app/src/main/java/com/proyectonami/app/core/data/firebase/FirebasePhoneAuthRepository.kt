package com.proyectonami.app.core.data.firebase

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.proyectonami.app.core.domain.auth.AuthSession
import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import com.proyectonami.app.core.domain.auth.PhoneCodeVerificationResult
import com.proyectonami.app.core.domain.auth.PhoneVerificationChallenge
import com.proyectonami.app.core.domain.auth.PhoneVerificationRequestResult
import com.proyectonami.app.core.domain.validation.PhoneNumberValidation
import com.proyectonami.app.core.domain.validation.PhoneNumberValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class FirebasePhoneAuthRepository(
    private val auth: FirebaseAuth,
) : AuthRepository {
    private val resendingTokens = ConcurrentHashMap<String, PhoneAuthProvider.ForceResendingToken>()

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSession(): Flow<AuthSession> =
        callbackFlow {
            val listener =
                FirebaseAuth.AuthStateListener { firebaseAuth ->
                    val session =
                        firebaseAuth.currentUser?.uid?.let(AuthSession::SignedIn)
                            ?: AuthSession.SignedOut
                    trySend(session)
                }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }

    override suspend fun requestPhoneCode(
        activity: Activity,
        phoneNumber: String,
        previousChallenge: PhoneVerificationChallenge?,
    ): PhoneVerificationRequestResult {
        val e164 =
            when (val validation = PhoneNumberValidator.validate(phoneNumber)) {
                is PhoneNumberValidation.Valid -> validation.e164
                is PhoneNumberValidation.Invalid ->
                    return PhoneVerificationRequestResult.Failed(PhoneAuthFailure.INVALID_PHONE_NUMBER)
            }
        val forceResendingToken = previousChallenge?.let { resendingTokens[it.verificationId] }

        return suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)

            fun finish(result: PhoneVerificationRequestResult) {
                if (completed.compareAndSet(false, true) && continuation.isActive) {
                    continuation.resume(result)
                }
            }

            val callbacks =
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        auth.signInWithCredential(credential).addOnCompleteListener { task ->
                            val userId = if (task.isSuccessful) task.result?.user?.uid else null
                            if (task.isSuccessful && userId != null) {
                                resendingTokens.clear()
                                finish(PhoneVerificationRequestResult.Verified(userId))
                            } else {
                                finish(
                                    PhoneVerificationRequestResult.Failed(
                                        task.exception?.toPhoneAuthFailure() ?: PhoneAuthFailure.UNKNOWN,
                                    ),
                                )
                            }
                        }
                    }

                    override fun onVerificationFailed(exception: com.google.firebase.FirebaseException) {
                        finish(
                            PhoneVerificationRequestResult.Failed(exception.toPhoneAuthFailure()),
                        )
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken,
                    ) {
                        resendingTokens.clear()
                        resendingTokens[verificationId] = token
                        finish(
                            PhoneVerificationRequestResult.CodeSent(
                                PhoneVerificationChallenge(verificationId),
                            ),
                        )
                    }
                }
            val options =
                PhoneAuthOptions
                    .newBuilder(auth)
                    .setPhoneNumber(e164)
                    .setTimeout(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .apply {
                        if (forceResendingToken != null) {
                            setForceResendingToken(forceResendingToken)
                        }
                    }.build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    override suspend fun verifyPhoneCode(
        challenge: PhoneVerificationChallenge,
        code: String,
    ): PhoneCodeVerificationResult {
        if (challenge.verificationId.isBlank() || code.isBlank()) {
            return PhoneCodeVerificationResult.Failed(PhoneAuthFailure.INVALID_CODE)
        }
        val credential = PhoneAuthProvider.getCredential(challenge.verificationId, code.trim())
        return auth.signInWithCredential(credential).awaitPhoneCodeResult().also { result ->
            when (result) {
                is PhoneCodeVerificationResult.Verified -> resendingTokens.clear()
                is PhoneCodeVerificationResult.Failed ->
                    if (result.reason == PhoneAuthFailure.CODE_EXPIRED) {
                        resendingTokens.remove(challenge.verificationId)
                    }
            }
        }
    }

    override fun signOut() {
        resendingTokens.clear()
        auth.signOut()
    }

    private suspend fun Task<com.google.firebase.auth.AuthResult>.awaitPhoneCodeResult(): PhoneCodeVerificationResult =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (continuation.isActive) {
                    val userId = if (task.isSuccessful) task.result?.user?.uid else null
                    val result =
                        if (task.isSuccessful && userId != null) {
                            PhoneCodeVerificationResult.Verified(userId)
                        } else {
                            PhoneCodeVerificationResult.Failed(
                                task.exception?.toPhoneAuthFailure() ?: PhoneAuthFailure.UNKNOWN,
                            )
                        }
                    continuation.resume(result)
                }
            }
        }

    companion object {
        private const val VERIFICATION_TIMEOUT_SECONDS = 60L
    }
}

internal fun Throwable.toPhoneAuthFailure(): PhoneAuthFailure =
    when (this) {
        is FirebaseTooManyRequestsException -> PhoneAuthFailure.TOO_MANY_REQUESTS
        is FirebaseNetworkException -> PhoneAuthFailure.NETWORK
        is FirebaseAuthInvalidCredentialsException -> errorCode.toPhoneAuthFailure()
        else -> PhoneAuthFailure.UNKNOWN
    }

internal fun String.toPhoneAuthFailure(): PhoneAuthFailure =
    when (this) {
        "ERROR_INVALID_PHONE_NUMBER" -> PhoneAuthFailure.INVALID_PHONE_NUMBER
        "ERROR_SESSION_EXPIRED" -> PhoneAuthFailure.CODE_EXPIRED
        "ERROR_INVALID_VERIFICATION_CODE" -> PhoneAuthFailure.INVALID_CODE
        else -> PhoneAuthFailure.UNKNOWN
    }

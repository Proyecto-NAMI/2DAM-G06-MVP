package com.proyectonami.app.core.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PhoneAuthModelsTest {
    @Test
    fun authSessionsIdentifySignedInAndSignedOutStates() {
        assertEquals(AuthSession.Restoring, AuthSession.Restoring)
        assertEquals(AuthSession.SignedOut, AuthSession.SignedOut)
        assertEquals(AuthSession.SignedIn("fictitious-user"), AuthSession.SignedIn("fictitious-user"))
        assertNotEquals(AuthSession.Restoring, AuthSession.SignedOut)
        assertNotEquals(AuthSession.SignedOut, AuthSession.SignedIn("fictitious-user"))
        assertEquals("fictitious-user", (AuthSession.SignedIn("fictitious-user")).userId)
    }

    @Test
    fun phoneVerificationResultsCarryChallengesUsersAndFailures() {
        val challenge = PhoneVerificationChallenge("fictitious-verification")
        assertEquals(challenge, PhoneVerificationRequestResult.CodeSent(challenge).challenge)
        assertEquals(
            "fictitious-user",
            (PhoneVerificationRequestResult.Verified("fictitious-user")).userId,
        )
        assertEquals(
            PhoneAuthFailure.NETWORK,
            (PhoneVerificationRequestResult.Failed(PhoneAuthFailure.NETWORK)).reason,
        )
        assertEquals(
            "fictitious-user",
            (PhoneCodeVerificationResult.Verified("fictitious-user")).userId,
        )
        assertEquals(
            PhoneAuthFailure.CODE_EXPIRED,
            (PhoneCodeVerificationResult.Failed(PhoneAuthFailure.CODE_EXPIRED)).reason,
        )
        assertEquals(6, PhoneAuthFailure.entries.size)
    }
}

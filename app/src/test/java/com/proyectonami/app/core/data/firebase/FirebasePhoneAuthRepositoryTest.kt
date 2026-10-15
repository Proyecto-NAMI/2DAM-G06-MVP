package com.proyectonami.app.core.data.firebase

import com.proyectonami.app.core.domain.auth.PhoneAuthFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebasePhoneAuthRepositoryTest {
    @Test
    fun mapsFirebasePhoneVerificationErrorCodes() {
        assertEquals(
            PhoneAuthFailure.INVALID_PHONE_NUMBER,
            "ERROR_INVALID_PHONE_NUMBER".toPhoneAuthFailure(),
        )
        assertEquals(
            PhoneAuthFailure.INVALID_CODE,
            "ERROR_INVALID_VERIFICATION_CODE".toPhoneAuthFailure(),
        )
        assertEquals(
            PhoneAuthFailure.CODE_EXPIRED,
            "ERROR_SESSION_EXPIRED".toPhoneAuthFailure(),
        )
        assertEquals(PhoneAuthFailure.UNKNOWN, "ERROR_OTHER".toPhoneAuthFailure())
    }
}

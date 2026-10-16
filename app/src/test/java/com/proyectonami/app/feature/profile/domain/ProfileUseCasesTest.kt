package com.proyectonami.app.feature.profile.domain

import com.proyectonami.app.core.domain.auth.AccountProfile
import com.proyectonami.app.core.domain.auth.AccountProfileLoadResult
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.domain.auth.AccountProfileSaveResult
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUseCasesTest {
    @Test
    fun updateNormalizesValidFieldsAndKeepsOptionalAvatar() =
        runBlocking {
            val repository = FakeProfileRepository()
            val result =
                UpdateProfileUseCase(repository).update(
                    "user-fictitious",
                    ProfileDraft("  Ana  ", " Prueba ", "+34 612 345 678", "avatars/ana.png"),
                )

            assertEquals(
                AccountProfile("user-fictitious", "Ana", "Prueba", "+34612345678", "avatars/ana.png"),
                (result as ProfileUpdateResult.Validated).profile,
            )
            assertEquals((result as ProfileUpdateResult.Validated).profile, repository.updated)
        }

    @Test
    fun invalidNameAndPhoneAreReportedWithoutWriting() =
        runBlocking {
            val repository = FakeProfileRepository()
            val useCase = UpdateProfileUseCase(repository)

            assertEquals(
                ProfileFieldError.Name(PersonNameError.INVALID_CHARACTERS),
                (
                    useCase.update(
                        "user-fictitious",
                        ProfileDraft("Ana1", "Prueba", "+34612345678", null),
                    ) as ProfileUpdateResult.Invalid
                ).error,
            )
            assertEquals(
                ProfileFieldError.Phone(PhoneNumberError.TOO_SHORT),
                (useCase.update("user-fictitious", ProfileDraft("Ana", "Prueba", "+34 12", null)) as ProfileUpdateResult.Invalid).error,
            )
            assertTrue(repository.updated == null)
        }

    @Test
    fun loadUseCaseReturnsStoredProfile() =
        runBlocking {
            val profile = AccountProfile("user-fictitious", "Ana", "Prueba", "+34612345678")
            val result = LoadProfileUseCase(FakeProfileRepository(profile)).load(profile.userId)
            assertEquals(AccountProfileLoadResult.Loaded(profile), result)
        }
}

private class FakeProfileRepository(
    private val loaded: AccountProfile? = null,
) : AccountProfileRepository {
    var updated: AccountProfile? = null

    override suspend fun saveProfile(profile: AccountProfile): AccountProfileSaveResult = AccountProfileSaveResult.Saved

    override suspend fun loadProfile(userId: String): AccountProfileLoadResult =
        loaded?.let(AccountProfileLoadResult::Loaded) ?: AccountProfileLoadResult.Missing

    override suspend fun updateProfile(profile: AccountProfile): AccountProfileSaveResult {
        updated = profile
        return AccountProfileSaveResult.Saved
    }
}

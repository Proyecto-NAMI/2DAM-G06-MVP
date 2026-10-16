package com.proyectonami.app.core.domain.auth

data class AccountProfile(
    val userId: String,
    val displayName: String,
    val surname: String,
    val phoneNumber: String,
    val avatarPath: String? = null,
)

sealed interface AccountProfileLoadResult {
    data class Loaded(
        val profile: AccountProfile,
    ) : AccountProfileLoadResult

    data object Missing : AccountProfileLoadResult

    data object Failed : AccountProfileLoadResult
}

sealed interface AccountProfileSaveResult {
    data object Saved : AccountProfileSaveResult

    data object Failed : AccountProfileSaveResult
}

interface AccountProfileRepository {
    suspend fun saveProfile(profile: AccountProfile): AccountProfileSaveResult

    suspend fun loadProfile(userId: String): AccountProfileLoadResult = AccountProfileLoadResult.Failed

    suspend fun updateProfile(profile: AccountProfile): AccountProfileSaveResult = saveProfile(profile)
}

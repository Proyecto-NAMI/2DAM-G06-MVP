package com.proyectonami.app.core.domain.auth

data class AccountProfile(
    val userId: String,
    val displayName: String,
    val surname: String,
    val phoneNumber: String,
)

sealed interface AccountProfileSaveResult {
    data object Saved : AccountProfileSaveResult

    data object Failed : AccountProfileSaveResult
}

interface AccountProfileRepository {
    suspend fun saveProfile(profile: AccountProfile): AccountProfileSaveResult
}

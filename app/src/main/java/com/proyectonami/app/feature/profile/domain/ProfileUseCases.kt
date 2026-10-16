package com.proyectonami.app.feature.profile.domain

import com.proyectonami.app.core.domain.auth.AccountProfile
import com.proyectonami.app.core.domain.auth.AccountProfileLoadResult
import com.proyectonami.app.core.domain.auth.AccountProfileRepository
import com.proyectonami.app.core.domain.auth.AccountProfileSaveResult
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PersonNameValidation
import com.proyectonami.app.core.domain.validation.PersonNameValidator
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.core.domain.validation.PhoneNumberValidation
import com.proyectonami.app.core.domain.validation.PhoneNumberValidator

data class ProfileDraft(
    val displayName: String,
    val surname: String,
    val phoneNumber: String,
    val avatarPath: String?,
)

sealed interface ProfileFieldError {
    data class Name(
        val reason: PersonNameError,
    ) : ProfileFieldError

    data class Surname(
        val reason: PersonNameError,
    ) : ProfileFieldError

    data class Phone(
        val reason: PhoneNumberError,
    ) : ProfileFieldError

    data object Avatar : ProfileFieldError
}

sealed interface ProfileUpdateResult {
    data class Validated(
        val profile: AccountProfile,
    ) : ProfileUpdateResult

    data class Invalid(
        val error: ProfileFieldError,
    ) : ProfileUpdateResult

    data object Failed : ProfileUpdateResult
}

class LoadProfileUseCase(
    private val repository: AccountProfileRepository,
) {
    suspend fun load(userId: String): AccountProfileLoadResult = repository.loadProfile(userId)
}

class UpdateProfileUseCase(
    private val repository: AccountProfileRepository,
) {
    suspend fun update(
        userId: String,
        draft: ProfileDraft,
    ): ProfileUpdateResult {
        val name = PersonNameValidator.validate(draft.displayName)
        if (name !is PersonNameValidation.Valid) {
            return ProfileUpdateResult.Invalid(ProfileFieldError.Name((name as PersonNameValidation.Invalid).error))
        }
        val surname = PersonNameValidator.validate(draft.surname)
        if (surname !is PersonNameValidation.Valid) {
            return ProfileUpdateResult.Invalid(ProfileFieldError.Surname((surname as PersonNameValidation.Invalid).error))
        }
        val phone = PhoneNumberValidator.validate(draft.phoneNumber)
        if (phone !is PhoneNumberValidation.Valid) {
            return ProfileUpdateResult.Invalid(ProfileFieldError.Phone((phone as PhoneNumberValidation.Invalid).error))
        }
        val avatar = draft.avatarPath?.trim()?.takeIf { it.isNotEmpty() }
        if (avatar != null && avatar.length > 512) return ProfileUpdateResult.Invalid(ProfileFieldError.Avatar)
        val profile = AccountProfile(userId, name.value, surname.value, phone.e164, avatar)
        return when (repository.updateProfile(profile)) {
            AccountProfileSaveResult.Saved -> ProfileUpdateResult.Validated(profile)
            AccountProfileSaveResult.Failed -> ProfileUpdateResult.Failed
        }
    }
}

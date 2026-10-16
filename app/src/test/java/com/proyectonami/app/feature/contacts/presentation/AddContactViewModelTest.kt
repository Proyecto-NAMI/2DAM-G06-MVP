package com.proyectonami.app.feature.contacts.presentation

import com.proyectonami.app.core.domain.contacts.AddContactResult
import com.proyectonami.app.core.domain.contacts.AddContactUseCase
import com.proyectonami.app.core.domain.contacts.Contact
import com.proyectonami.app.core.domain.contacts.ContactDraft
import com.proyectonami.app.core.domain.contacts.ContactSaveResult
import com.proyectonami.app.core.domain.contacts.ContactStatus
import com.proyectonami.app.core.domain.contacts.ContactsRemoteState
import com.proyectonami.app.core.domain.contacts.ContactsRepository
import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estado del alta de contactos (T-13, RF-03).
 *
 * Solo se prueban aquí la parte síncrona y el reductor de resultados; el envío
 * con `viewModelScope` se cubre en la prueba instrumentada `AddContactScreenTest`.
 */
class AddContactViewModelTest {
    private val carlos =
        Contact(
            id = "contact_carlos",
            displayName = "Carlos",
            surname = "Prueba",
            phoneE164 = "+34612345678",
            linkedUserId = null,
            status = ContactStatus.PENDING,
        )

    @Test
    fun initialStateOffersTheSameEditablePrefixAsAccountCreation() {
        val state = newViewModel().uiState.value

        assertEquals(AddContactUiState.DEFAULT_PHONE_PREFIX, state.phoneNumber)
        assertEquals("+34 ", state.phoneNumber)
        assertEquals("", state.name)
        assertEquals("", state.surname)
        assertNull(state.nameError)
        assertNull(state.surnameError)
        assertNull(state.phoneNumberError)
        assertNull(state.addedContact)
        assertNull(state.duplicateContact)
        assertFalse(state.isSaving)
        assertFalse(state.lookupUnavailable)
        assertFalse(state.saveFailed)
        assertTrue(state.isFormEnabled)
    }

    @Test
    fun typingUpdatesTheThreeFields() {
        val viewModel = newViewModel()

        viewModel.updateName("Carlos")
        viewModel.updateSurname("Prueba")
        viewModel.updatePhoneNumber("+34 612345678")

        val state = viewModel.uiState.value
        assertEquals("Carlos", state.name)
        assertEquals("Prueba", state.surname)
        assertEquals("+34 612345678", state.phoneNumber)
    }

    @Test
    fun addingAnotherContactClearsTheFormAndKeepsThePrefix() {
        val viewModel = newViewModel()
        viewModel.updateName("Carlos")
        viewModel.updateSurname("Prueba")
        viewModel.updatePhoneNumber("+34 612345678")

        viewModel.addAnotherContact()

        assertEquals(AddContactUiState(), viewModel.uiState.value)
    }

    @Test
    fun theFormStopsAcceptingChangesWhileSavingOrAfterSaving() {
        assertTrue(AddContactUiState().isFormEnabled)
        assertFalse(AddContactUiState(isSaving = true).isFormEnabled)
        assertFalse(AddContactUiState(addedContact = carlos).isFormEnabled)
    }

    @Test
    fun addedResultShowsTheSavedContactAndClosesTheForm() {
        val state =
            AddContactUiState(isSaving = true, duplicateContact = carlos, lookupUnavailable = true)
                .afterAddResult(AddContactResult.Added(carlos))

        assertEquals(carlos, state.addedContact)
        assertNull(state.duplicateContact)
        assertFalse(state.isSaving)
        assertFalse(state.lookupUnavailable)
        assertFalse(state.saveFailed)
        assertFalse(state.isFormEnabled)
    }

    @Test
    fun duplicateResultKeepsTheExistingContactWithoutSavingAnotherOne() {
        val state = AddContactUiState(isSaving = true).afterAddResult(AddContactResult.Duplicate(carlos))

        assertEquals(carlos, state.duplicateContact)
        assertNull(state.addedContact)
        assertFalse(state.isSaving)
        assertFalse(state.lookupUnavailable)
        assertFalse(state.saveFailed)
        assertTrue(state.isFormEnabled)
    }

    @Test
    fun invalidResultsAreShownOnTheirOwnField() {
        val nameState =
            AddContactUiState().afterAddResult(
                AddContactResult.InvalidName(PersonNameError.TOO_LONG),
            )
        val phoneState =
            AddContactUiState().afterAddResult(
                AddContactResult.InvalidPhone(PhoneNumberError.TOO_SHORT),
            )

        assertEquals(PersonNameError.TOO_LONG, nameState.nameError)
        assertNull(nameState.phoneNumberError)
        assertEquals(PhoneNumberError.TOO_SHORT, phoneState.phoneNumberError)
        assertNull(phoneState.nameError)
    }

    @Test
    fun unavailableLookupIsReportedAsRecoverableWithoutSaving() {
        val state = AddContactUiState(isSaving = true).afterAddResult(AddContactResult.LookupUnavailable)

        assertTrue(state.lookupUnavailable)
        assertFalse(state.isSaving)
        assertFalse(state.saveFailed)
        assertNull(state.addedContact)
        assertNull(state.duplicateContact)
        assertTrue(state.isFormEnabled)
    }

    @Test
    fun failedResultAllowsRetryingTheSameForm() {
        val state = AddContactUiState(isSaving = true).afterAddResult(AddContactResult.Failed)

        assertTrue(state.saveFailed)
        assertFalse(state.lookupUnavailable)
        assertFalse(state.isSaving)
        assertTrue(state.isFormEnabled)
        assertNull(state.addedContact)
    }

    private fun newViewModel(): AddContactViewModel = AddContactViewModel(AddContactUseCase(FakeContactsRepository()))
}

private class FakeContactsRepository : ContactsRepository {
    override fun observeContacts(): Flow<ContactsRemoteState> = flowOf(ContactsRemoteState.Loaded(emptyList()))

    override suspend fun addContact(contact: ContactDraft): ContactSaveResult = ContactSaveResult.Failed
}

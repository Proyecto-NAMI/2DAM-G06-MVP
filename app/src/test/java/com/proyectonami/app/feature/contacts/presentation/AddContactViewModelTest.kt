package com.proyectonami.app.feature.contacts.presentation

import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import com.proyectonami.app.feature.contacts.domain.AddContactResult
import com.proyectonami.app.feature.contacts.domain.AddContactUseCase
import com.proyectonami.app.feature.contacts.domain.Contact
import com.proyectonami.app.feature.contacts.domain.ContactCreateResult
import com.proyectonami.app.feature.contacts.domain.ContactFieldError
import com.proyectonami.app.feature.contacts.domain.ContactRepository
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
    fun savingKeepsTheFormClosedWhileTheContactIsBeingSaved() {
        val viewModel = newViewModel()
        viewModel.updateName("Carlos")

        val saving = viewModel.uiState.value.copy(isSaving = true)
        assertFalse(saving.isFormEnabled)
        assertEquals("Carlos", saving.name)
    }

    @Test
    fun addedResultShowsTheSavedContactAndClosesTheForm() {
        val state =
            AddContactUiState(isSaving = true, duplicateContact = carlos)
                .afterAddResult(AddContactResult.Added(carlos))

        assertEquals(carlos, state.addedContact)
        assertNull(state.duplicateContact)
        assertFalse(state.isSaving)
        assertFalse(state.saveFailed)
        assertFalse(state.isFormEnabled)
    }

    @Test
    fun duplicateResultKeepsTheExistingContactWithoutSavingAnotherOne() {
        val state = AddContactUiState(isSaving = true).afterAddResult(AddContactResult.Duplicate(carlos))

        assertEquals(carlos, state.duplicateContact)
        assertNull(state.addedContact)
        assertFalse(state.isSaving)
        assertFalse(state.saveFailed)
        assertTrue(state.isFormEnabled)
    }

    @Test
    fun invalidResultIsShownOnItsOwnField() {
        val nameState =
            AddContactUiState().afterAddResult(
                AddContactResult.Invalid(ContactFieldError.Name(PersonNameError.TOO_LONG)),
            )
        val surnameState =
            AddContactUiState().afterAddResult(
                AddContactResult.Invalid(ContactFieldError.Surname(PersonNameError.EMPTY)),
            )
        val phoneState =
            AddContactUiState().afterAddResult(
                AddContactResult.Invalid(ContactFieldError.Phone(PhoneNumberError.TOO_SHORT)),
            )

        assertEquals(PersonNameError.TOO_LONG, nameState.nameError)
        assertNull(nameState.surnameError)
        assertNull(nameState.phoneNumberError)
        assertEquals(PersonNameError.EMPTY, surnameState.surnameError)
        assertEquals(PhoneNumberError.TOO_SHORT, phoneState.phoneNumberError)
        assertNull(phoneState.nameError)
    }

    @Test
    fun failedResultAllowsRetryingTheSameForm() {
        val state = AddContactUiState(isSaving = true).afterAddResult(AddContactResult.Failed)

        assertTrue(state.saveFailed)
        assertFalse(state.isSaving)
        assertTrue(state.isFormEnabled)
        assertNull(state.addedContact)
    }

    private fun newViewModel(): AddContactViewModel =
        AddContactViewModel(
            addContactUseCase = AddContactUseCase(FakeContactRepository()),
            currentUserId = { "fictitious-owner" },
        )
}

private class FakeContactRepository : ContactRepository {
    override suspend fun createContact(
        userId: String,
        contact: Contact,
    ): ContactCreateResult = ContactCreateResult.Created
}

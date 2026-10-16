package com.proyectonami.app.core.domain.contacts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddContactUseCaseTest {
    @Test
    fun savesUnknownPhoneAsPendingAndRejectsEquivalentDuplicate() =
        runBlocking {
            val repository = FakeContactsRepository()
            val useCase = AddContactUseCase(repository, defaultCallingCode = "1")

            val first = useCase(NewContact(" Ana   María ", " García ", "555 555 0100"))
            val duplicate = useCase(NewContact("Otro nombre", "Ficticio", "+1 (555) 555-0100"))

            assertTrue(first is AddContactResult.Added)
            assertEquals(ContactStatus.PENDING, (first as AddContactResult.Added).contact.status)
            assertEquals("Ana María", first.contact.displayName)
            assertEquals("+15555550100", first.contact.phoneE164)
            assertTrue(duplicate is AddContactResult.Duplicate)
            assertEquals("Ana María", (duplicate as AddContactResult.Duplicate).existing.displayName)
            assertEquals(1, repository.contacts.size)
        }

    @Test
    fun linksAnExistingAccountReturnedBySecureRepository() =
        runBlocking {
            val repository = FakeContactsRepository(status = ContactStatus.LINKED)
            val useCase = AddContactUseCase(repository, defaultCallingCode = "1")

            val result = useCase(NewContact("Persona", "Ficticia", "5555550100"))

            assertTrue(result is AddContactResult.Added)
            val contact = (result as AddContactResult.Added).contact
            assertEquals(ContactStatus.LINKED, contact.status)
            assertEquals("uid-ficticio", contact.linkedUserId)
        }

    @Test
    fun reportsWhenSecureLookupIsUnavailable() =
        runBlocking {
            val repository = FakeContactsRepository().apply { nextFailure = ContactSaveResult.LookupUnavailable }
            val useCase = AddContactUseCase(repository, defaultCallingCode = "1")

            assertEquals(
                AddContactResult.LookupUnavailable,
                useCase(NewContact("Persona", "Ficticia", "5555550100")),
            )
            assertTrue(repository.contacts.isEmpty())
        }

    @Test
    fun mapsRepositoryExceptionToRecoverableFailure() =
        runBlocking {
            val repository = FakeContactsRepository().apply { failWithException = true }
            val useCase = AddContactUseCase(repository, defaultCallingCode = "1")

            assertEquals(
                AddContactResult.Failed,
                useCase(NewContact("Persona", "Ficticia", "5555550100")),
            )
            assertEquals(1, repository.addCalls)
        }

    @Test
    fun rejectsInvalidInputBeforeRemoteLookup() =
        runBlocking {
            val repository = FakeContactsRepository()
            val useCase = AddContactUseCase(repository, defaultCallingCode = "1")

            val result = useCase(NewContact("Nombre3", "Ficticia", "5555550100"))

            assertTrue(result is AddContactResult.InvalidName)
            assertEquals(0, repository.addCalls)
        }

    @Test
    fun rejectsInvalidPhoneBeforeCallingRepository() =
        runBlocking {
            val repository = FakeContactsRepository()
            val useCase = AddContactUseCase(repository, defaultCallingCode = "1")

            val result = useCase(NewContact("Persona", "Ficticia", "not-a-phone"))

            assertTrue(result is AddContactResult.InvalidPhone)
            assertEquals(0, repository.addCalls)
        }

    private class FakeContactsRepository(
        private val status: ContactStatus = ContactStatus.PENDING,
    ) : ContactsRepository {
        val contacts = linkedMapOf<String, Contact>()
        var addCalls = 0
        var nextFailure: ContactSaveResult? = null
        var failWithException = false

        override fun observeContacts(): Flow<ContactsRemoteState> = flowOf(ContactsRemoteState.Loaded(contacts.values.toList()))

        override suspend fun addContact(contact: ContactDraft): ContactSaveResult {
            addCalls += 1
            if (failWithException) error("backend unavailable")
            nextFailure?.let { return it }
            val existing = contacts[contact.phoneE164]
            if (existing != null) return ContactSaveResult.Duplicate(existing)
            val saved =
                Contact(
                    id = "fake-${contacts.size + 1}",
                    displayName = contact.displayName,
                    surname = contact.surname,
                    phoneE164 = contact.phoneE164,
                    linkedUserId = if (status == ContactStatus.LINKED) "uid-ficticio" else null,
                    status = status,
                )
            contacts[contact.phoneE164] = saved
            return ContactSaveResult.Added(saved)
        }
    }
}

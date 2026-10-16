package com.proyectonami.app.feature.contacts.domain

import com.proyectonami.app.core.domain.validation.PersonNameError
import com.proyectonami.app.core.domain.validation.PhoneNumberError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AddContactUseCaseTest {
    private val repository = FakeContactRepository()

    @Test
    fun validContactIsStoredAsPendingWithNormalizedData() =
        runBlocking {
            val added =
                addContact(
                    displayName = "  Ana   María ",
                    surname = "Núñez",
                    phoneNumber = "+34 612 345 678",
                ).assertAdded()

            assertEquals("Ana María", added.contact.displayName)
            assertEquals("Núñez", added.contact.surname)
            assertEquals("+34612345678", added.contact.phoneE164)
            assertEquals(ContactStatus.PENDING, added.contact.status)
            assertNull(added.contact.linkedUserId)
            assertEquals(1, repository.storedCount(USER_ID))
            assertEquals(added.contact, repository.storedContact(USER_ID, added.contact.id))
        }

    @Test
    fun contactIsLinkedWhenTheResolverFindsAnAccount() =
        runBlocking {
            val resolver = FixedAccountResolver(ResolvedAccount.Found("user-2"))

            val added = addContact(resolver = resolver).assertAdded()

            assertEquals(ContactStatus.LINKED, added.contact.status)
            assertEquals("user-2", added.contact.linkedUserId)
            assertEquals(listOf("+34612345678"), resolver.queriedPhones)
        }

    @Test
    fun contactStaysPendingWhenTheResolverFindsNoAccount() =
        runBlocking {
            val resolver = FixedAccountResolver(ResolvedAccount.NotFound)

            val added = addContact(resolver = resolver).assertAdded()

            assertEquals(ContactStatus.PENDING, added.contact.status)
            assertNull(added.contact.linkedUserId)
            assertEquals(1, repository.storedCount(USER_ID))
        }

    @Test
    fun mvpResolverKeepsContactsPendingWithoutLookingUpTheDirectory() =
        runBlocking {
            val added = addContact().assertAdded()

            assertEquals(ContactStatus.PENDING, added.contact.status)
            assertEquals(ResolvedAccount.Unavailable, DisabledContactAccountResolver.resolveAccount("+34612345678"))
        }

    @Test
    fun samePhoneInAnyFormatIsRejectedAsDuplicate() =
        runBlocking {
            addContact(displayName = "Ana", phoneNumber = "+34612345678").assertAdded()

            val duplicate = addContact(displayName = "Otra Persona", phoneNumber = "+34 612.345.678").assertDuplicate()

            assertEquals("Ana", duplicate.existing.displayName)
            assertEquals("+34612345678", duplicate.existing.phoneE164)
            assertEquals(1, repository.storedCount(USER_ID))
            assertEquals(2, repository.createCalls)
        }

    @Test
    fun differentPhonesCreateDifferentContacts() =
        runBlocking {
            addContact(phoneNumber = "+34612345678").assertAdded()
            addContact(phoneNumber = "+34612345679").assertAdded()

            assertEquals(2, repository.storedCount(USER_ID))
        }

    @Test
    fun contactsAreScopedToTheirOwner() =
        runBlocking {
            addContact(userId = "user-1").assertAdded()
            addContact(userId = "user-2").assertAdded()

            assertEquals(1, repository.storedCount("user-1"))
            assertEquals(1, repository.storedCount("user-2"))
        }

    @Test
    fun failedSaveCanBeRetriedWithoutDuplicatingTheContact() =
        runBlocking {
            repository.failNextWrites = true
            assertEquals(AddContactResult.Failed, addContact())

            repository.failNextWrites = false
            addContact().assertAdded()

            assertEquals(1, repository.storedCount(USER_ID))
        }

    @Test
    fun invalidFieldsAreReportedWithoutTouchingTheRepository() =
        runBlocking {
            assertEquals(
                AddContactResult.Invalid(ContactFieldError.Name(PersonNameError.EMPTY)),
                addContact(displayName = "   "),
            )
            assertEquals(
                AddContactResult.Invalid(ContactFieldError.Surname(PersonNameError.INVALID_CHARACTERS)),
                addContact(surname = "Ficticia2"),
            )
            assertEquals(
                AddContactResult.Invalid(ContactFieldError.Phone(PhoneNumberError.EMPTY)),
                addContact(phoneNumber = ""),
            )
            assertEquals(
                AddContactResult.Invalid(ContactFieldError.Name(PersonNameError.EMPTY)),
                addContact(displayName = "", phoneNumber = "sin-prefijo"),
            )

            assertEquals(0, repository.createCalls)
        }

    @Test
    fun blankOwnerIsRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    useCase().add(
                        userId = "  ",
                        request = NewContactRequest("Ana", "Ficticia", "+34612345678"),
                    )
                }
            }

        assertEquals("userId no puede estar vacío", error.message)
    }

    private fun useCase(resolver: ContactAccountResolver = DisabledContactAccountResolver) =
        AddContactUseCase(repository = repository, accountResolver = resolver)

    private suspend fun addContact(
        displayName: String = "Ana",
        surname: String = "Ficticia",
        phoneNumber: String = "+34612345678",
        resolver: ContactAccountResolver = DisabledContactAccountResolver,
        userId: String = USER_ID,
    ): AddContactResult =
        useCase(resolver).add(
            userId = userId,
            request = NewContactRequest(displayName = displayName, surname = surname, phoneNumber = phoneNumber),
        )

    private fun AddContactResult.assertAdded(): AddContactResult.Added {
        assertTrue("Se esperaba Added y se recibió $this", this is AddContactResult.Added)
        return this as AddContactResult.Added
    }

    private fun AddContactResult.assertDuplicate(): AddContactResult.Duplicate {
        assertTrue("Se esperaba Duplicate y se recibió $this", this is AddContactResult.Duplicate)
        return this as AddContactResult.Duplicate
    }

    private class FixedAccountResolver(
        private val account: ResolvedAccount,
    ) : ContactAccountResolver {
        val queriedPhones = mutableListOf<String>()

        override suspend fun resolveAccount(phoneE164: String): ResolvedAccount {
            queriedPhones += phoneE164
            return account
        }
    }

    private class FakeContactRepository : ContactRepository {
        private val stored = linkedMapOf<Pair<String, String>, Contact>()

        var failNextWrites = false
        var createCalls = 0

        override suspend fun createContact(
            userId: String,
            contact: Contact,
        ): ContactCreateResult {
            createCalls++
            if (failNextWrites) {
                return ContactCreateResult.Failed
            }
            val key = userId to contact.id
            stored[key]?.let { existing -> return ContactCreateResult.AlreadyExists(existing) }
            stored[key] = contact
            return ContactCreateResult.Created
        }

        fun storedCount(userId: String): Int = stored.keys.count { (owner, _) -> owner == userId }

        fun storedContact(
            userId: String,
            contactId: String,
        ): Contact? = stored[userId to contactId]
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}

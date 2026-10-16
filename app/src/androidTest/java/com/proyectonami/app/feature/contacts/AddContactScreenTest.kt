package com.proyectonami.app.feature.contacts

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proyectonami.app.FakeContactsRepository
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.core.domain.contacts.AddContactUseCase
import com.proyectonami.app.core.domain.contacts.Contact
import com.proyectonami.app.core.domain.contacts.ContactStatus
import com.proyectonami.app.feature.contacts.presentation.AddContactRoute
import com.proyectonami.app.feature.contacts.presentation.AddContactUiState
import com.proyectonami.app.feature.contacts.presentation.AddContactViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

/**
 * Pantalla `Nuevo contacto` (T-13, RF-03): alta, vinculación, validación,
 * duplicados, reintento, acción «Listo» del teclado y texto al 200 %.
 *
 * Usa datos ficticios y un repositorio en memoria (`FakeContactsRepository`),
 * que reproduce el contrato de la función `addContact` de T-11 sin necesitar
 * Firebase.
 */
@RunWith(AndroidJUnit4::class)
class AddContactScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private lateinit var repository: FakeContactsRepository
    private lateinit var viewModel: AddContactViewModel
    private var backPresses: Int = 0

    @Before
    fun setUp() {
        repository = FakeContactsRepository()
        viewModel = AddContactViewModel(AddContactUseCase(repository))
        backPresses = 0
        composeRule.setContent {
            val fontScale = if (testName.methodName == "formRemainsScrollableAt200PercentText") 2f else 1f
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                NamiTheme {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        AddContactRoute(
                            viewModel = viewModel,
                            onBack = { backPresses++ },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(AddContactUiState.DEFAULT_PHONE_PREFIX, viewModel.uiState.value.phoneNumber)
    }

    @Test
    fun validContactIsSavedOnceAsPendingAndConfirmed() {
        fillForm()
        composeRule.onNodeWithText("Crear contacto").performScrollTo().performClick()

        composeRule.onNodeWithText("Contacto añadido").assertIsDisplayed()
        composeRule
            .onNodeWithText("Carlos Prueba se ha guardado en tus contactos.")
            .performScrollTo()
            .assertIsDisplayed()

        val saved = repository.stored.single()
        assertEquals(ContactStatus.PENDING, saved.status)
        assertNull(saved.linkedUserId)
        assertEquals("Carlos", saved.displayName)
        assertEquals("Prueba", saved.surname)
        assertEquals("+34612345678", saved.phoneE164)
        assertEquals("Carlos", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.isFormEnabled)
    }

    @Test
    fun linkedContactIsConfirmedWithTheNamiMessage() {
        repository.savedStatus = ContactStatus.LINKED
        fillForm()
        composeRule.onNodeWithText("Crear contacto").performScrollTo().performClick()

        composeRule
            .onNodeWithText("Carlos Prueba ya usa NAMI.")
            .performScrollTo()
            .assertIsDisplayed()

        val saved = repository.stored.single()
        assertEquals(ContactStatus.LINKED, saved.status)
        assertEquals(FakeContactsRepository.LINKED_USER_ID, saved.linkedUserId)
    }

    @Test
    fun keyboardDoneActionSavesTheContact() {
        fillForm()
        // El teclado solo envía la acción «Listo» al campo enfocado: se toca el
        // campo después de escribirlo para no mover el cursor antes de tiempo.
        composeRule.onAllNodes(hasSetTextAction())[2].performTouchInput { click() }
        composeRule.onAllNodes(hasSetTextAction())[2].performImeAction()

        composeRule.waitUntil(timeoutMillis = 5_000) { repository.stored.size == 1 }
        composeRule.onNodeWithText("Contacto añadido").assertIsDisplayed()
        assertEquals("+34612345678", repository.stored.single().phoneE164)
    }

    @Test
    fun duplicateIsReportedWithTheExistingContactAndNothingIsOverwritten() {
        val existing = seedContact()
        fillForm(name = "Carlos Nuevo", surname = "Prueba")
        composeRule.onNodeWithText("Crear contacto").performScrollTo().performClick()

        composeRule
            .onNodeWithText("Ya tienes ese teléfono en tus contactos: Carlos Viejo")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Contacto añadido").assertDoesNotExist()

        assertEquals(1, repository.createAttempts)
        assertEquals(existing, repository.stored.single())
        assertTrue(viewModel.uiState.value.isFormEnabled)
        assertEquals("Carlos Nuevo", viewModel.uiState.value.name)
    }

    @Test
    fun editingThePhoneClearsTheDuplicateWarning() {
        seedContact()
        fillForm()
        composeRule.onNodeWithText("Crear contacto").performScrollTo().performClick()
        composeRule
            .onNodeWithText("Ya tienes ese teléfono en tus contactos: Carlos Viejo")
            .performScrollTo()
            .assertIsDisplayed()

        val phoneField = composeRule.onAllNodes(hasSetTextAction())[2]
        phoneField.performTextClearance()
        phoneField.performTextInput("+34 699 888 777")

        composeRule
            .onAllNodesWithText("Ya tienes ese teléfono en tus contactos: Carlos Viejo")
            .assertCountEquals(0)
        assertNull(viewModel.uiState.value.duplicateContact)
    }

    @Test
    fun invalidFieldsAreAnnouncedAndNothingIsSaved() {
        composeRule.onNodeWithText("Crear contacto").performScrollTo().performClick()

        composeRule
            .onAllNodesWithText("Escribe este dato para continuar")
            .assertCountEquals(2)
        composeRule
            .onAllNodes(
                hasSetTextAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Error),
            ).assertCountEquals(3)
        assertEquals(0, repository.createAttempts)
        assertEquals(0, repository.stored.size)
    }

    @Test
    fun failedSaveCanBeRetriedWithoutCreatingASecondContact() {
        repository.failuresBeforeSuccess = 1
        fillForm()
        composeRule.onNodeWithText("Crear contacto").performScrollTo().performClick()
        composeRule
            .onNodeWithText("No se pudo guardar el contacto. Comprueba la conexión e inténtalo de nuevo.")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Reintentar el guardado del contacto")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Contacto añadido").assertIsDisplayed()
        assertEquals(2, repository.createAttempts)
        assertEquals(1, repository.stored.size)
    }

    @Test
    fun unavailableLookupIsExplainedAndNothingIsSavedUntilRetry() {
        repository.lookupsUnavailableBeforeSuccess = 1
        fillForm()
        composeRule.onNodeWithText("Crear contacto").performScrollTo().performClick()

        composeRule
            .onNodeWithText(
                "No hemos podido comprobar si esa persona ya usa NAMI, así que no se ha guardado " +
                    "el contacto. Espera un poco e inténtalo de nuevo.",
            ).performScrollTo()
            .assertIsDisplayed()
        assertEquals(0, repository.stored.size)
        assertTrue(viewModel.uiState.value.lookupUnavailable)

        composeRule
            .onNodeWithText("Reintentar el guardado del contacto")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Contacto añadido").assertIsDisplayed()
        assertFalse(viewModel.uiState.value.lookupUnavailable)
        assertEquals(1, repository.stored.size)
    }

    @Test
    fun backControlNotifiesTheRequestedNavigation() {
        composeRule.onNodeWithContentDescription("Volver").performClick()

        assertEquals(1, backPresses)
    }

    @Test
    fun afterSavingThePersonCanAddAnotherContact() {
        fillForm()
        composeRule.onNodeWithText("Crear contacto").performScrollTo().performClick()
        composeRule.onNodeWithText("Contacto añadido").assertIsDisplayed()

        composeRule.onNodeWithText("Añadir otro contacto").performScrollTo().performClick()

        composeRule.onNodeWithText("Nuevo contacto").assertIsDisplayed()
        assertEquals(AddContactUiState(), viewModel.uiState.value)
        assertTrue(viewModel.uiState.value.isFormEnabled)
        composeRule
            .onAllNodesWithText("Carlos Prueba se ha guardado en tus contactos.")
            .assertCountEquals(0)
    }

    @Test
    fun formRemainsScrollableAt200PercentText() {
        composeRule.onNodeWithText("Nuevo contacto").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Volver").assertIsDisplayed()
        composeRule.onNodeWithText("Crear contacto").performScrollTo().assertIsDisplayed()
        composeRule
            .onAllNodes(hasSetTextAction())[2]
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun fillForm(
        name: String = "Carlos",
        surname: String = "Prueba",
    ) {
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(name)
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput(surname)
        composeRule.onAllNodes(hasSetTextAction())[2].performTextInput("612345678")
    }

    private fun seedContact(): Contact {
        val contact =
            Contact(
                id = "contact_existente",
                displayName = "Carlos",
                surname = "Viejo",
                phoneE164 = "+34612345678",
                linkedUserId = null,
                status = ContactStatus.PENDING,
            )
        repository.seed(contact)
        return contact
    }
}

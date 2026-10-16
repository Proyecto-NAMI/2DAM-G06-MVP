package com.proyectonami.app.feature.contacts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.component.NamiPrimaryButton
import com.proyectonami.app.core.designsystem.component.NamiSecondaryButton
import com.proyectonami.app.core.designsystem.component.NamiTextField
import com.proyectonami.app.core.designsystem.theme.NamiSize
import com.proyectonami.app.core.designsystem.theme.NamiSpacing
import com.proyectonami.app.core.designsystem.theme.NamiTheme
import com.proyectonami.app.core.domain.contacts.Contact
import com.proyectonami.app.core.domain.contacts.ContactStatus
import com.proyectonami.app.ui.personNameErrorMessage
import com.proyectonami.app.ui.phoneNumberErrorMessage

/**
 * Conecta el estado de [AddContactViewModel] con la pantalla (T-13, RF-03).
 *
 * Se abre desde el «+» de la pantalla principal (T-15); el guardado pasa por la
 * función `addContact` de T-11, así que el cliente nunca escribe el contacto.
 */
@Composable
fun AddContactRoute(
    viewModel: AddContactViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    AddContactScreen(
        state = state,
        onNameChange = viewModel::updateName,
        onSurnameChange = viewModel::updateSurname,
        onPhoneNumberChange = viewModel::updatePhoneNumber,
        onSubmit = viewModel::saveContact,
        onAddAnother = viewModel::addAnotherContact,
        onBack = onBack,
    )
}

/**
 * Pantalla `Nuevo contacto` (T-13, RF-03), sin lógica de negocio ni acceso a datos.
 *
 * Accesibilidad: el título es un encabezado, los errores de campo se exponen como
 * errores de accesibilidad, el duplicado y el fallo se anuncian como región viva,
 * la confirmación se anuncia de forma educada y todo el contenido es desplazable
 * para que el texto al 200 % no se recorte. El campo de teléfono admite la acción
 * «Listo» del teclado, que guarda el contacto.
 */
@Composable
fun AddContactScreen(
    state: AddContactUiState,
    onNameChange: (String) -> Unit,
    onSurnameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAddAnother: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
    ) {
        AddContactHeader(onBack = onBack)
        val addedContact = state.addedContact
        if (addedContact == null) {
            ContactForm(
                state = state,
                onNameChange = onNameChange,
                onSurnameChange = onSurnameChange,
                onPhoneNumberChange = onPhoneNumberChange,
                onSubmit = onSubmit,
            )
        } else {
            ContactAddedConfirmation(
                contact = addedContact,
                onAddAnother = onAddAnother,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun AddContactHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NamiSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(NamiSize.MinTouchTarget),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nami_back),
                contentDescription = stringResource(R.string.contact_add_back),
            )
        }
        Text(
            text = stringResource(R.string.contact_add_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Composable
private fun ContactForm(
    state: AddContactUiState,
    onNameChange: (String) -> Unit,
    onSurnameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSubmit()
    }
    Text(
        text = stringResource(R.string.contact_add_description),
        style = MaterialTheme.typography.bodyLarge,
    )
    NamiTextField(
        value = state.name,
        onValueChange = onNameChange,
        label = stringResource(R.string.account_name_label),
        errorMessage = personNameErrorMessage(state.nameError),
        enabled = state.isFormEnabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions =
            KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
    )
    NamiTextField(
        value = state.surname,
        onValueChange = onSurnameChange,
        label = stringResource(R.string.account_surname_label),
        errorMessage = personNameErrorMessage(state.surnameError),
        enabled = state.isFormEnabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions =
            KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
    )
    NamiTextField(
        value = state.phoneNumber,
        onValueChange = onPhoneNumberChange,
        label = stringResource(R.string.account_phone_label),
        supportingText = stringResource(R.string.account_phone_supporting),
        errorMessage = phoneNumberErrorMessage(state.phoneNumberError),
        enabled = state.isFormEnabled,
        moveCursorToEndOnFocus = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
    )
    state.duplicateContact?.let { existing ->
        Text(
            text = stringResource(R.string.contact_add_duplicate, existing.fullName()),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    if (state.lookupUnavailable) {
        Text(
            text = stringResource(R.string.contact_add_lookup_unavailable),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
        NamiPrimaryButton(
            text = stringResource(R.string.contact_add_retry),
            icon = painterResource(R.drawable.ic_nami_refresh),
            onClick = submit,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isFormEnabled,
        )
    } else if (state.saveFailed) {
        Text(
            text = stringResource(R.string.contact_add_failed),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
        NamiPrimaryButton(
            text = stringResource(R.string.contact_add_retry),
            icon = painterResource(R.drawable.ic_nami_refresh),
            onClick = submit,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isFormEnabled,
        )
    } else if (state.isSaving) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.contact_add_saving),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyLarge,
        )
    } else {
        NamiPrimaryButton(
            text = stringResource(R.string.contact_add_submit),
            icon = painterResource(R.drawable.ic_nami_add),
            onClick = submit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ContactAddedConfirmation(
    contact: Contact,
    onAddAnother: () -> Unit,
    onBack: () -> Unit,
) {
    Text(
        text = stringResource(R.string.contact_add_success_title),
        modifier =
            Modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        text =
            stringResource(
                if (contact.status == ContactStatus.LINKED) {
                    R.string.contact_add_success_linked
                } else {
                    R.string.contact_add_success_description
                },
                contact.fullName(),
            ),
        style = MaterialTheme.typography.bodyLarge,
    )
    NamiSecondaryButton(
        text = stringResource(R.string.contact_add_another),
        icon = painterResource(R.drawable.ic_nami_add),
        onClick = onAddAnother,
        modifier = Modifier.fillMaxWidth(),
    )
    NamiSecondaryButton(
        text = stringResource(R.string.contact_add_back),
        icon = painterResource(R.drawable.ic_nami_back),
        onClick = onBack,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Nombre completo tal como se muestra en la confirmación y en el aviso de duplicado. */
private fun Contact.fullName(): String = listOf(displayName, surname).filter { it.isNotBlank() }.joinToString(" ")

@Preview(showBackground = true)
@Composable
private fun AddContactScreenPreview() {
    NamiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AddContactScreen(
                state = AddContactUiState(),
                onNameChange = {},
                onSurnameChange = {},
                onPhoneNumberChange = {},
                onSubmit = {},
                onAddAnother = {},
                onBack = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddContactScreenDuplicatePreview() {
    NamiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AddContactScreen(
                state =
                    AddContactUiState(
                        name = "Carlos",
                        surname = "Prueba",
                        phoneNumber = "+34612345678",
                        duplicateContact =
                            Contact(
                                id = "contact_preview",
                                displayName = "Carlos",
                                surname = "Prueba",
                                phoneE164 = "+34612345678",
                                linkedUserId = null,
                                status = ContactStatus.PENDING,
                            ),
                    ),
                onNameChange = {},
                onSurnameChange = {},
                onPhoneNumberChange = {},
                onSubmit = {},
                onAddAnother = {},
                onBack = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddContactScreenLinkedPreview() {
    NamiTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AddContactScreen(
                state =
                    AddContactUiState(
                        addedContact =
                            Contact(
                                id = "contact_preview",
                                displayName = "Carlos",
                                surname = "Prueba",
                                phoneE164 = "+34612345678",
                                linkedUserId = "uid-ficticio",
                                status = ContactStatus.LINKED,
                            ),
                    ),
                onNameChange = {},
                onSurnameChange = {},
                onPhoneNumberChange = {},
                onSubmit = {},
                onAddAnother = {},
                onBack = {},
            )
        }
    }
}

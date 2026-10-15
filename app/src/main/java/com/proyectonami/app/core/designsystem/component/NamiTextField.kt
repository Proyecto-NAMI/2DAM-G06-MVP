package com.proyectonami.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.proyectonami.app.core.designsystem.theme.NamiSize

/**
 * Campo de texto accesible de NAMI (T-04, RNF-01).
 *
 * - La etiqueta es obligatoria y el lector de pantalla la anuncia.
 * - Si hay [errorMessage], se muestra bajo el campo y se expone como error de
 *   accesibilidad, de modo que no se comunica solo con el color rojo.
 * - Ocupa todo el ancho y mide al menos 56 dp de alto.
 */
@Composable
fun NamiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    moveCursorToEndOnFocus: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            ),
        )
    }
    SideEffect {
        if (textFieldValue.text != value) {
            textFieldValue =
                TextFieldValue(
                    text = value,
                    selection = TextRange(value.length),
                )
        }
    }
    LaunchedEffect(isFocused, moveCursorToEndOnFocus) {
        if (isFocused && moveCursorToEndOnFocus) {
            withFrameNanos { }
            textFieldValue =
                textFieldValue.copy(
                    selection = TextRange(textFieldValue.text.length),
                )
        }
    }
    val helperText = errorMessage ?: supportingText
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onValueChange(it.text)
        },
        modifier =
            modifier
                .onFocusChanged {
                    isFocused = it.isFocused
                }.fillMaxWidth()
                .heightIn(min = NamiSize.PrimaryControlHeight)
                .semantics {
                    if (errorMessage != null) error(errorMessage)
                },
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(text = label) },
        supportingText =
            if (helperText != null) {
                { Text(text = helperText) }
            } else {
                null
            },
        isError = errorMessage != null,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.medium,
    )
}

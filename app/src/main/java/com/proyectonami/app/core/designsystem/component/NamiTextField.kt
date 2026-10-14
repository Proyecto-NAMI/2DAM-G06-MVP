package com.proyectonami.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val helperText = errorMessage ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NamiSize.PrimaryControlHeight)
            .semantics {
                if (errorMessage != null) error(errorMessage)
            },
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(text = label) },
        supportingText = helperText?.let { message -> { Text(text = message) } },
        isError = errorMessage != null,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.medium,
    )
}

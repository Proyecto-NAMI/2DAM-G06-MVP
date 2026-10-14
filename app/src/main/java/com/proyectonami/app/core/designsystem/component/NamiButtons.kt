package com.proyectonami.app.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import com.proyectonami.app.core.designsystem.theme.NamiSize
import com.proyectonami.app.core.designsystem.theme.NamiSpacing

private val ButtonContentPadding = PaddingValues(
    horizontal = NamiSpacing.Large,
    vertical = NamiSpacing.Small,
)

/**
 * Botón de la acción principal de una pantalla (T-04, RNF-01).
 *
 * Siempre muestra texto e icono, mide al menos 56 dp de alto y el texto pasa a
 * varias líneas en lugar de cortarse cuando se amplía el tamaño de letra.
 * El icono es decorativo: el lector de pantalla lee el texto del botón.
 */
@Composable
fun NamiPrimaryButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(
            minWidth = NamiSize.MinTouchTarget,
            minHeight = NamiSize.PrimaryControlHeight,
        ),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        contentPadding = ButtonContentPadding,
    ) {
        NamiButtonContent(text = text, icon = icon)
    }
}

/**
 * Botón de una acción secundaria (T-04, RNF-01).
 *
 * Usa un borde grueso para distinguirse del botón principal sin depender solo
 * del color. Mantiene las mismas garantías de tamaño, texto e icono.
 */
@Composable
fun NamiSecondaryButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(
            minWidth = NamiSize.MinTouchTarget,
            minHeight = NamiSize.PrimaryControlHeight,
        ),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = NamiSize.ControlBorder,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
        contentPadding = ButtonContentPadding,
    ) {
        NamiButtonContent(text = text, icon = icon)
    }
}

@Composable
private fun RowScope.NamiButtonContent(text: String, icon: Painter) {
    Row(
        modifier = Modifier.weight(weight = 1f, fill = false),
        horizontalArrangement = Arrangement.spacedBy(NamiSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(NamiSize.ControlIcon),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

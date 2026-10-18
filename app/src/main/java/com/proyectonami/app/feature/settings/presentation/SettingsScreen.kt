package com.proyectonami.app.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.component.NamiPrimaryButton
import com.proyectonami.app.core.designsystem.component.NamiSecondaryButton
import com.proyectonami.app.core.designsystem.theme.NamiSpacing

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    SettingsScreen(
        state = state,
        onTextScaleSelected = viewModel::selectTextScale,
        onReadMessagesAloudChange = viewModel::setReadMessagesAloud,
        onBack = onBack,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onTextScaleSelected: (TextScaleOption) -> Unit,
    onReadMessagesAloudChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readAloudStateDescription =
        stringResource(
            if (state.readMessagesAloud) {
                R.string.settings_state_enabled
            } else {
                R.string.settings_state_disabled
            },
        )
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.settings_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.settings_text_size_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text =
                stringResource(
                    R.string.settings_text_size_current,
                    textScaleLabel(state.textScale),
                ),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyLarge,
        )
        TextScaleOption.entries.forEach { option ->
            TextScaleButton(
                option = option,
                selected = option.isSelected(state.textScale),
                onClick = { onTextScaleSelected(option) },
            )
        }
        Text(
            text = stringResource(R.string.settings_read_aloud_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.settings_read_aloud_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text =
                stringResource(
                    if (state.readMessagesAloud) {
                        R.string.tts_preference_enabled_status
                    } else {
                        R.string.tts_preference_disabled_status
                    },
                ),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyLarge,
        )
        NamiSecondaryButton(
            text =
                stringResource(
                    if (state.readMessagesAloud) {
                        R.string.tts_preference_disable
                    } else {
                        R.string.tts_preference_enable
                    },
                ),
            icon =
                painterResource(
                    if (state.readMessagesAloud) {
                        R.drawable.ic_nami_stop
                    } else {
                        R.drawable.ic_nami_play
                    },
                ),
            onClick = { onReadMessagesAloudChange(!state.readMessagesAloud) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        stateDescription = readAloudStateDescription
                    },
        )
        NamiPrimaryButton(
            text = stringResource(R.string.settings_back),
            icon = painterResource(R.drawable.ic_nami_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TextScaleButton(
    option: TextScaleOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = textScaleOptionLabel(option)
    val state =
        stringResource(
            if (selected) {
                R.string.settings_option_selected
            } else {
                R.string.settings_option_not_selected
            },
        )
    val modifier =
        Modifier
            .fillMaxWidth()
            .semantics {
                this.selected = selected
                stateDescription = state
            }
    if (selected) {
        NamiPrimaryButton(
            text = label,
            icon = painterResource(R.drawable.ic_nami_check),
            onClick = onClick,
            modifier = modifier,
        )
    } else {
        NamiSecondaryButton(
            text = label,
            icon = painterResource(R.drawable.ic_nami_settings),
            onClick = onClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun textScaleLabel(scale: Float): String = textScaleOptionLabel(TextScaleOption.entries.minBy { kotlin.math.abs(it.scale - scale) })

@Composable
private fun textScaleOptionLabel(option: TextScaleOption): String =
    stringResource(
        when (option) {
            TextScaleOption.NORMAL -> R.string.settings_text_size_normal
            TextScaleOption.LARGE -> R.string.settings_text_size_large
            TextScaleOption.MAXIMUM -> R.string.settings_text_size_maximum
        },
    )

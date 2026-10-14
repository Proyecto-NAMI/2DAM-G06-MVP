package com.proyectonami.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.theme.NamiSpacing
import com.proyectonami.app.core.designsystem.theme.NamiTheme

@Composable
fun NamiApp() {
    NamiTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NamiWelcomeScreen()
        }
    }
}

@Composable
private fun NamiWelcomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Large),
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = stringResource(R.string.welcome_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Text(
                text = stringResource(R.string.project_status),
                modifier = Modifier.padding(NamiSpacing.Large),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NamiWelcomeScreenPreview() {
    NamiApp()
}


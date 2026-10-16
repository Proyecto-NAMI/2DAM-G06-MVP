package com.proyectonami.app.feature.conversations.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.semantics
import com.proyectonami.app.R
import com.proyectonami.app.core.designsystem.component.NamiSecondaryButton
import com.proyectonami.app.core.designsystem.theme.NamiSpacing
import com.proyectonami.app.core.domain.conversations.ConversationsRemoteState

@Composable
fun ConversationsRoute(
    viewModel: ConversationsViewModel,
    onOpenConversation: (String) -> Unit,
    onAddContact: () -> Unit,
    onProfile: () -> Unit,
    onSignOut: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
    ) {
        Text(
            stringResource(R.string.conversations_title),
            modifier =
                Modifier.semantics {
                    heading()
                },
            style = MaterialTheme.typography.headlineLarge,
        )
        when (val remote = state) {
            ConversationsRemoteState.Loading -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.conversations_loading), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            ConversationsRemoteState.Failed ->
                Text(
                    stringResource(R.string.conversations_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier.semantics {
                            liveRegion =
                                LiveRegionMode.Assertive
                        },
                )
            is ConversationsRemoteState.Loaded -> {
                if (remote.conversations.isEmpty()) {
                    Text(stringResource(R.string.conversations_empty), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NamiSpacing.Small)) {
                        items(remote.conversations, key = { it.id }) { conversation ->
                            NamiSecondaryButton(
                                text = "${conversation.displayName} ${conversation.surname}".trim(),
                                icon = painterResource(R.drawable.ic_nami_arrow_forward),
                                onClick = { onOpenConversation(conversation.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        NamiSecondaryButton(
            stringResource(R.string.contact_add_entry),
            painterResource(R.drawable.ic_nami_add),
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth(),
        )
        NamiSecondaryButton(
            stringResource(R.string.profile_entry),
            painterResource(R.drawable.ic_nami_check),
            onClick = onProfile,
            modifier = Modifier.fillMaxWidth(),
        )
        NamiSecondaryButton(
            stringResource(R.string.logout_request),
            painterResource(R.drawable.ic_nami_logout),
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

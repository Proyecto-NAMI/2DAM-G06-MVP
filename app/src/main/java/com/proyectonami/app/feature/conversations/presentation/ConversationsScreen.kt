package com.proyectonami.app.feature.conversations.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    onSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(NamiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(NamiSpacing.Medium),
    ) {
        item {
            Text(
                stringResource(R.string.conversations_title),
                modifier =
                    Modifier.semantics {
                        heading()
                    },
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        when (val remote = state) {
            ConversationsRemoteState.Loading -> {
                item { CircularProgressIndicator() }
                item {
                    Text(
                        stringResource(R.string.conversations_loading),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            ConversationsRemoteState.Failed ->
                item {
                    Text(
                        stringResource(R.string.conversations_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier =
                            Modifier.semantics {
                                liveRegion =
                                    LiveRegionMode.Assertive
                            },
                    )
                }
            is ConversationsRemoteState.Loaded -> {
                if (remote.conversations.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.conversations_empty),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                } else {
                    items(remote.conversations, key = { it.id }) { conversation ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NamiSpacing.Small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .semantics {
                                            contentDescription =
                                                conversationAvatarDescription(
                                                    conversation.displayName,
                                                    conversation.surname,
                                                    conversation.avatarPath != null,
                                                )
                                        },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    initials(conversation.displayName, conversation.surname),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            NamiSecondaryButton(
                                text = conversationLabel(conversation.displayName, conversation.surname, conversation.unreadCount),
                                icon = painterResource(R.drawable.ic_nami_arrow_forward),
                                onClick = { onOpenConversation(conversation.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        item {
            NamiSecondaryButton(
                stringResource(R.string.contact_add_entry),
                painterResource(R.drawable.ic_nami_add),
                onClick = onAddContact,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            NamiSecondaryButton(
                stringResource(R.string.profile_entry),
                painterResource(R.drawable.ic_nami_check),
                onClick = onProfile,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            NamiSecondaryButton(
                stringResource(R.string.settings_entry),
                painterResource(R.drawable.ic_nami_settings),
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            NamiSecondaryButton(
                stringResource(R.string.logout_request),
                painterResource(R.drawable.ic_nami_logout),
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun conversationLabel(
    displayName: String,
    surname: String,
    unreadCount: Int,
): String {
    val name = "$displayName $surname".trim()
    return if (unreadCount > 0) "$name, $unreadCount mensajes sin leer" else name
}

private fun initials(
    displayName: String,
    surname: String,
): String = listOf(displayName, surname).mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString("")

private fun conversationAvatarDescription(
    displayName: String,
    surname: String,
    hasAvatar: Boolean,
): String = if (hasAvatar) "Avatar de $displayName $surname" else "Iniciales de $displayName $surname"

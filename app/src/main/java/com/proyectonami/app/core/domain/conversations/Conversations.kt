package com.proyectonami.app.core.domain.conversations

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ConversationParticipant(
    val displayName: String,
    val surname: String,
    val avatarPath: String?,
)

data class ConversationRecord(
    val id: String,
    val participantIds: List<String>,
    val participantDetails: Map<String, ConversationParticipant>,
    val unreadCountByUser: Map<String, Long>,
    val lastActivityAtEpochMillis: Long,
)

data class ConversationSummary(
    val id: String,
    val otherParticipantId: String,
    val displayName: String,
    val surname: String,
    val avatarPath: String?,
    val unreadCount: Int,
    val lastActivityAtEpochMillis: Long,
)

sealed interface ConversationsRemoteState {
    data object Loading : ConversationsRemoteState

    data class Loaded(
        val conversations: List<ConversationSummary>,
    ) : ConversationsRemoteState

    data object Failed : ConversationsRemoteState
}

interface ConversationsRepository {
    fun observeConversations(): Flow<ConversationsRemoteState>
}

object ConversationMapper {
    fun toSummary(
        record: ConversationRecord,
        currentUserId: String,
    ): ConversationSummary? {
        if (currentUserId.isBlank()) return null
        if (record.participantIds.size != 2 || record.participantIds.distinct().size != 2) return null
        if (currentUserId !in record.participantIds) return null

        val otherParticipantId = record.participantIds.first { it != currentUserId }
        val participant = record.participantDetails[otherParticipantId] ?: return null
        if (participant.displayName.isBlank()) return null

        val unreadCount = (record.unreadCountByUser[currentUserId] ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        return ConversationSummary(
            id = record.id,
            otherParticipantId = otherParticipantId,
            displayName = participant.displayName,
            surname = participant.surname,
            avatarPath = participant.avatarPath,
            unreadCount = unreadCount,
            lastActivityAtEpochMillis = record.lastActivityAtEpochMillis,
        )
    }
}

class ObserveConversationsUseCase(
    private val repository: ConversationsRepository,
) {
    operator fun invoke(): Flow<ConversationsRemoteState> =
        repository.observeConversations().map { state ->
            when (state) {
                is ConversationsRemoteState.Loaded ->
                    ConversationsRemoteState.Loaded(
                        state.conversations.sortedByDescending { it.lastActivityAtEpochMillis },
                    )

                ConversationsRemoteState.Loading -> ConversationsRemoteState.Loading
                ConversationsRemoteState.Failed -> ConversationsRemoteState.Failed
            }
        }
}

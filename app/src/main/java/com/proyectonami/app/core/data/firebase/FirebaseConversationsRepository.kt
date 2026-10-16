package com.proyectonami.app.core.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.proyectonami.app.core.domain.conversations.ConversationMapper
import com.proyectonami.app.core.domain.conversations.ConversationParticipant
import com.proyectonami.app.core.domain.conversations.ConversationRecord
import com.proyectonami.app.core.domain.conversations.ConversationsRemoteState
import com.proyectonami.app.core.domain.conversations.ConversationsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class FirebaseConversationsRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ConversationsRepository {
    override fun observeConversations(): Flow<ConversationsRemoteState> =
        flow {
            emit(ConversationsRemoteState.Loading)
            emitAll(
                callbackFlow {
                    val userId = auth.currentUser?.uid
                    if (userId == null) {
                        trySend(ConversationsRemoteState.Failed)
                        close()
                        return@callbackFlow
                    }

                    val registration =
                        firestore
                            .collection("conversations")
                            .whereArrayContains("participantIds", userId)
                            .orderBy("lastActivityAt", Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null || snapshot == null) {
                                    trySend(ConversationsRemoteState.Failed)
                                } else {
                                    val conversations =
                                        snapshot.documents.mapNotNull { document ->
                                            document
                                                .toConversationRecord()
                                                ?.let { record -> ConversationMapper.toSummary(record, userId) }
                                        }
                                    trySend(ConversationsRemoteState.Loaded(conversations))
                                }
                            }
                    awaitClose { registration.remove() }
                },
            )
        }
}

private fun DocumentSnapshot.toConversationRecord(): ConversationRecord? {
    val participantIds =
        (get("participantIds") as? List<*>)
            ?.map { it as? String ?: return null }
            ?: return null
    val participantDetails =
        (get("participantDetails") as? Map<*, *>)
            ?.mapNotNull { (uid, value) ->
                val participant = value as? Map<*, *> ?: return@mapNotNull null
                val participantUid = uid as? String ?: return@mapNotNull null
                val displayName = participant["displayName"] as? String ?: return@mapNotNull null
                val surname = participant["surname"] as? String ?: return@mapNotNull null
                val avatarPath = participant["avatarPath"] as? String
                participantUid to ConversationParticipant(displayName, surname, avatarPath)
            }?.toMap()
            ?: return null
    val unreadCountByUser =
        (get("unreadCountByUser") as? Map<*, *>)
            ?.mapNotNull { (uid, count) ->
                val participantUid = uid as? String ?: return@mapNotNull null
                val unreadCount = (count as? Number)?.toLong() ?: return@mapNotNull null
                participantUid to unreadCount
            }?.toMap()
            ?: emptyMap()
    val lastActivityAt = getTimestamp("lastActivityAt") ?: return null

    return ConversationRecord(
        id = id,
        participantIds = participantIds,
        participantDetails = participantDetails,
        unreadCountByUser = unreadCountByUser,
        lastActivityAtEpochMillis = lastActivityAt.toDate().time,
    )
}

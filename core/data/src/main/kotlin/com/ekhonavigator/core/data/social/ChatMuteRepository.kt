package com.ekhonavigator.core.data.social

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMuteRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    fun observeConversationMuted(
        userId: String,
        conversationId: String,
    ): Flow<Boolean> = callbackFlow {
        val registration = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CHAT_SETTINGS_COLLECTION)
            .document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }

                val muted = snapshot?.getBoolean(MUTED_FIELD) ?: false
                trySend(muted)
            }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun isConversationMuted(
        userId: String,
        conversationId: String,
    ): Boolean {
        val snapshot = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CHAT_SETTINGS_COLLECTION)
            .document(conversationId)
            .get()
            .await()

        return snapshot.getBoolean(MUTED_FIELD) ?: false
    }

    suspend fun setConversationMuted(
        userId: String,
        conversationId: String,
        muted: Boolean,
    ) {
        val data = mapOf(
            MUTED_FIELD to muted,
            UPDATED_AT_FIELD to System.currentTimeMillis(),
        )

        firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CHAT_SETTINGS_COLLECTION)
            .document(conversationId)
            .set(data)
            .await()
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val CHAT_SETTINGS_COLLECTION = "chatSettings"
        private const val MUTED_FIELD = "muted"
        private const val UPDATED_AT_FIELD = "updatedAt"
    }
}
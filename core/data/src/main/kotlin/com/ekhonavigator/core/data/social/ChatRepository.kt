package com.ekhonavigator.core.data.social

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    private fun buildConversationId(userA: String, userB: String): String {
        return listOf(userA, userB).sorted().joinToString("_")
    }

    suspend fun getOrCreateConversation(
        currentUserId: String,
        currentUserName: String,
        friendUserId: String,
        friendDisplayName: String,
    ): ChatConversation {
        val conversationId = buildConversationId(currentUserId, friendUserId)
        val conversationRef = firestore.collection("conversations").document(conversationId)

        val now = System.currentTimeMillis()
        val conversationData = mapOf(
            "participantIds" to listOf(currentUserId, friendUserId),
            "participantNames" to mapOf(
                currentUserId to currentUserName,
                friendUserId to friendDisplayName,
            ),
            "createdAt" to now,
        )

        conversationRef.set(conversationData, SetOptions.merge()).await()

        return ChatConversation(
            id = conversationId,
            participantIds = listOf(currentUserId, friendUserId),
            participantNames = mapOf(
                currentUserId to currentUserName,
                friendUserId to friendDisplayName,
            ),
            createdAt = now,
        )
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.map { doc ->
                    val timestampValue = doc.get("timestamp")
                    val timestampMillis = when (timestampValue) {
                        is com.google.firebase.Timestamp -> timestampValue.toDate().time
                        is Long -> timestampValue
                        is Number -> timestampValue.toLong()
                        else -> doc.getLong("clientTimestamp") ?: 0L
                    }

                    val sharedLocationData = doc.get("sharedLocation") as? Map<String, Any>
                    val sharedLocation = sharedLocationData?.let {
                        com.ekhonavigator.core.model.SharedLocation(
                            title = it["title"] as? String ?: "",
                            latitude = (it["latitude"] as? Number)?.toDouble() ?: 0.0,
                            longitude = (it["longitude"] as? Number)?.toDouble() ?: 0.0,
                            details = it["details"] as? String ?: ""
                        )
                    }

                    ChatMessage(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = timestampMillis,
                        readBy = (doc.get("readBy") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList(),
                        clientMessageId = doc.getString("clientMessageId") ?: "",
                        sharedLocation = sharedLocation
                    )
                }
                    ?.sortedBy { it.timestamp }
                    .orEmpty()

                trySend(messages)
            }

        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String,
        clientMessageId: String,
        sharedLocation: com.ekhonavigator.core.model.SharedLocation? = null
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank() && sharedLocation == null) return

        val now = System.currentTimeMillis()
        val conversationRef = firestore.collection("conversations").document(conversationId)
        val messageRef = conversationRef.collection("messages").document()

        val messageData = mutableMapOf<String, Any?>(
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to trimmed,
            "timestamp" to FieldValue.serverTimestamp(),
            "clientTimestamp" to now,
            "readBy" to listOf(senderId),
            "clientMessageId" to clientMessageId,
        )

        if (sharedLocation != null) {
            messageData["sharedLocation"] = sharedLocation
        }

        val batch = firestore.batch()
        batch.set(messageRef, messageData)
        batch.update(
            conversationRef,
            mapOf(
                "lastMessage" to trimmed,
                "lastSenderId" to senderId,
                "lastTimestamp" to FieldValue.serverTimestamp(),
            )
        )
        batch.commit().await()
    }

    suspend fun markMessagesAsRead(
        conversationId: String,
        currentUserId: String,
    ) {
        val messagesRef = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")

        val snapshot = messagesRef.get().await()

        val batch = firestore.batch()

        snapshot.documents.forEach { doc ->
            val senderId = doc.getString("senderId") ?: return@forEach
            val readBy = (doc.get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            if (senderId != currentUserId && currentUserId !in readBy) {
                batch.update(doc.reference, "readBy", FieldValue.arrayUnion(currentUserId))
            }
        }

        batch.commit().await()
    }
}
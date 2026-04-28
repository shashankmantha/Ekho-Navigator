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
                    doc.toChatMessage()
                }
                    ?.sortedBy { it.timestamp }
                    .orEmpty()

                trySend(messages)
            }

        awaitClose { registration.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toChatMessage(): ChatMessage {
        val timestampValue = get("timestamp")
        val timestampMillis = when (timestampValue) {
            is com.google.firebase.Timestamp -> timestampValue.toDate().time
            is Long -> timestampValue
            is Number -> timestampValue.toLong()
            else -> getLong("clientTimestamp") ?: 0L
        }

        val sharedLocationData = get("sharedLocation") as? Map<String, Any>
        val sharedLocation = sharedLocationData?.let {
            com.ekhonavigator.core.model.SharedLocation(
                title = it["title"] as? String ?: "",
                latitude = (it["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (it["longitude"] as? Number)?.toDouble() ?: 0.0,
                details = it["details"] as? String ?: ""
            )
        }

        return ChatMessage(
            id = id,
            senderId = getString("senderId") ?: "",
            senderName = getString("senderName") ?: "",
            text = getString("text") ?: "",
            timestamp = timestampMillis,
            readBy = (get("readBy") as? List<*>)?.filterIsInstance<String>()
                ?: emptyList(),
            clientMessageId = getString("clientMessageId") ?: "",
            sharedLocation = sharedLocation
        )
    }

    fun observeUnreadMessagesCount(currentUserId: String): Flow<Int> = callbackFlow {
        // We'll use a collectionGroup query to find all messages that the current user hasn't read yet.
        // This requires the message to have a 'participantIds' field to be queryable by collectionGroup safely for the user,
        // OR we just query all messages where 'readBy' does not contain currentUserId.
        // NOTE: 'not-in' or 'array-contains-any' might be needed.
        // Firestore doesn't support 'array-not-contains'.
        
        // A common pattern is to store unread status on the conversation document.
        // Let's check the conversation doc again.
        val registration = firestore.collection("conversations")
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }

                val unreadConversations = snapshot?.documents?.count { doc ->
                    val lastSenderId = doc.getString("lastSenderId")
                    val readBy = (doc.get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    lastSenderId != null && lastSenderId != currentUserId && currentUserId !in readBy
                } ?: 0
                
                trySend(unreadConversations)
            }
        awaitClose { registration.remove() }
    }

    fun observeAllConversations(currentUserId: String): Flow<List<ChatConversation>> = callbackFlow {
        val registration = firestore.collection("conversations")
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val conversations = snapshot?.documents?.mapNotNull { it.toChatConversation() } ?: emptyList()
                trySend(conversations)
            }
        awaitClose { registration.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toChatConversation(): ChatConversation? {
        if (!exists()) return null
        
        val timestampValue = get("lastTimestamp")
        val lastTimestamp = when (timestampValue) {
            is com.google.firebase.Timestamp -> timestampValue.toDate().time
            is Long -> timestampValue
            is Number -> timestampValue.toLong()
            else -> 0L
        }

        val createdAtValue = get("createdAt")
        val createdAt = when (createdAtValue) {
            is com.google.firebase.Timestamp -> createdAtValue.toDate().time
            is Long -> createdAtValue
            is Number -> createdAtValue.toLong()
            else -> 0L
        }

        return ChatConversation(
            id = id,
            participantIds = (get("participantIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            participantNames = (get("participantNames") as? Map<*, *>)?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap(),
            lastMessage = getString("lastMessage") ?: "",
            lastSenderId = getString("lastSenderId") ?: "",
            lastTimestamp = lastTimestamp,
            readBy = (get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            unreadCount = getLong("unreadCount")?.toInt() ?: 0,
            createdAt = createdAt,
        )
    }

    fun observeConversation(currentUserId: String, friendUserId: String): Flow<ChatConversation?> = callbackFlow {
        val conversationId = buildConversationId(currentUserId, friendUserId)
        val registration = firestore.collection("conversations")
            .document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toChatConversation())
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
                "lastMessage" to (if (trimmed.isEmpty() && sharedLocation != null) "Shared a location" else trimmed),
                "lastSenderId" to senderId,
                "lastTimestamp" to FieldValue.serverTimestamp(),
                "readBy" to listOf(senderId),
                "unreadCount" to FieldValue.increment(1)
            )
        )
        batch.commit().await()
    }

    suspend fun markMessagesAsRead(
        conversationId: String,
        currentUserId: String,
    ) {
        val conversationRef = firestore.collection("conversations").document(conversationId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(conversationRef)
            val lastSenderId = snapshot.getString("lastSenderId")
            
            // Only reset if the current user is NOT the last sender 
            // OR if there is an actual unread count to clear.
            if (lastSenderId != currentUserId) {
                transaction.update(conversationRef, 
                    mapOf(
                        "readBy" to FieldValue.arrayUnion(currentUserId),
                        "unreadCount" to 0
                    )
                )
            }
        }.await()

        // Also mark individual messages as read (outside the transaction to keep it simple, 
        // as this can be many documents)
        val messagesRef = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")

        val messageSnapshot = messagesRef.get().await()
        val batch = firestore.batch()
        messageSnapshot.documents.forEach { doc ->
            val senderId = doc.getString("senderId") ?: return@forEach
            val readBy = (doc.get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            if (senderId != currentUserId && currentUserId !in readBy) {
                batch.update(doc.reference, "readBy", FieldValue.arrayUnion(currentUserId))
            }
        }
        batch.commit().await()
    }
}
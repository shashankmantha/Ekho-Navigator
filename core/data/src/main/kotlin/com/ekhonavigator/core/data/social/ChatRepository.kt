package com.ekhonavigator.core.data.social

import com.ekhonavigator.core.model.SharedLocation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
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

    fun buildDirectConversationId(
        userA: String,
        userB: String,
    ): String {
        return listOf(userA, userB)
            .sorted()
            .joinToString("_")
    }

    suspend fun findDirectConversationId(
        currentUserId: String,
        friendUserId: String,
    ): String? {
        val conversationId = buildDirectConversationId(
            userA = currentUserId,
            userB = friendUserId,
        )

        val snapshot = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .get()
            .await()

        return if (snapshot.exists()) {
            conversationId
        } else {
            null
        }
    }

    suspend fun findDirectConversation(
        currentUserId: String,
        friendUserId: String,
    ): ChatConversation? {
        val conversationId = buildDirectConversationId(
            userA = currentUserId,
            userB = friendUserId,
        )

        return firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .get()
            .await()
            .toChatConversation()
    }

    suspend fun getOrCreateConversation(
        currentUserId: String,
        currentUserName: String,
        friendUserId: String,
        friendDisplayName: String,
    ): ChatConversation {
        return getOrCreateDirectConversation(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            friendUserId = friendUserId,
            friendDisplayName = friendDisplayName,
        )
    }

    suspend fun getOrCreateDirectConversation(
        currentUserId: String,
        currentUserName: String,
        friendUserId: String,
        friendDisplayName: String,
    ): ChatConversation {
        val conversationId = buildDirectConversationId(
            userA = currentUserId,
            userB = friendUserId,
        )

        val conversationRef = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)

        val existingConversation = conversationRef.get().await()

        if (!existingConversation.exists()) {
            val now = System.currentTimeMillis()

            val conversationData = mapOf(
                "type" to ChatConversation.TYPE_DIRECT,
                "isGroup" to false,
                "title" to "",
                "participantIds" to listOf(currentUserId, friendUserId),
                "participantNames" to mapOf(
                    currentUserId to currentUserName,
                    friendUserId to friendDisplayName,
                ),
                "createdBy" to currentUserId,
                "createdAt" to now,
                "lastMessage" to "",
                "lastSenderId" to "",
                "lastTimestamp" to 0L,
                "readBy" to listOf(currentUserId),
                "unreadCount" to 0,
            )

            conversationRef.set(conversationData).await()
        } else {
            val updatedConversationData = mapOf(
                "participantNames.$currentUserId" to currentUserName,
                "participantNames.$friendUserId" to friendDisplayName,
                "type" to ChatConversation.TYPE_DIRECT,
                "isGroup" to false,
            )

            conversationRef.set(
                updatedConversationData,
                SetOptions.merge(),
            ).await()
        }

        return conversationRef.get().await().toChatConversation()
            ?: ChatConversation(
                id = conversationId,
                type = ChatConversation.TYPE_DIRECT,
                title = "",
                isGroup = false,
                participantIds = listOf(currentUserId, friendUserId),
                participantNames = mapOf(
                    currentUserId to currentUserName,
                    friendUserId to friendDisplayName,
                ),
                createdBy = currentUserId,
                createdAt = System.currentTimeMillis(),
            )
    }

    suspend fun sendFirstDirectMessage(
        currentUserId: String,
        currentUserName: String,
        friendUserId: String,
        friendDisplayName: String,
        text: String,
        clientMessageId: String,
        sharedLocation: SharedLocation? = null,
    ): String {
        val trimmedText = text.trim()

        if (trimmedText.isBlank() && sharedLocation == null) {
            return buildDirectConversationId(
                userA = currentUserId,
                userB = friendUserId,
            )
        }

        val conversation = getOrCreateDirectConversation(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            friendUserId = friendUserId,
            friendDisplayName = friendDisplayName,
        )

        sendMessage(
            conversationId = conversation.id,
            senderId = currentUserId,
            senderName = currentUserName,
            text = trimmedText,
            clientMessageId = clientMessageId,
            sharedLocation = sharedLocation,
        )

        return conversation.id
    }

    suspend fun createGroupConversation(
        currentUserId: String,
        currentUserName: String,
        groupTitle: String,
        participantNames: Map<String, String>,
    ): ChatConversation {
        val conversationRef = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document()

        val conversationId = conversationRef.id
        val now = System.currentTimeMillis()

        val allParticipantNames = participantNames
            .toMutableMap()
            .apply {
                put(currentUserId, currentUserName)
            }

        val participantIds = allParticipantNames.keys.toList()

        val conversationData = mapOf(
            "type" to ChatConversation.TYPE_GROUP,
            "isGroup" to true,
            "title" to groupTitle.trim(),
            "participantIds" to participantIds,
            "participantNames" to allParticipantNames,
            "createdBy" to currentUserId,
            "createdAt" to now,
            "lastMessage" to "",
            "lastSenderId" to "",
            "lastTimestamp" to 0L,
            "readBy" to listOf(currentUserId),
            "unreadCount" to 0,
        )

        conversationRef.set(conversationData).await()

        return conversationRef.get().await().toChatConversation()
            ?: ChatConversation(
                id = conversationId,
                type = ChatConversation.TYPE_GROUP,
                title = groupTitle.trim(),
                isGroup = true,
                participantIds = participantIds,
                participantNames = allParticipantNames,
                createdBy = currentUserId,
                createdAt = now,
            )
    }

    suspend fun sendFirstGroupMessage(
        currentUserId: String,
        currentUserName: String,
        groupTitle: String,
        participantNames: Map<String, String>,
        text: String,
        clientMessageId: String,
        sharedLocation: SharedLocation? = null,
    ): String {
        val trimmedText = text.trim()

        if (trimmedText.isBlank() && sharedLocation == null) {
            return ""
        }

        val conversation = createGroupConversation(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            groupTitle = groupTitle,
            participantNames = participantNames,
        )

        sendMessage(
            conversationId = conversation.id,
            senderId = currentUserId,
            senderName = currentUserName,
            text = trimmedText,
            clientMessageId = clientMessageId,
            sharedLocation = sharedLocation,
        )

        return conversation.id
    }

    suspend fun renameGroupConversation(
        conversationId: String,
        newTitle: String,
    ) {
        val conversationRef = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(conversationRef)
            val isGroup = snapshot.getBoolean("isGroup") ?: false

            if (!isGroup) {
                error("Cannot rename a direct conversation")
            }

            transaction.update(
                conversationRef,
                "title",
                newTitle.trim(),
            )
        }.await()
    }

    suspend fun addParticipantsToGroupConversation(
        conversationId: String,
        newParticipantNames: Map<String, String>,
    ) {
        if (newParticipantNames.isEmpty()) return

        val conversationRef = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(conversationRef)
            val isGroup = snapshot.getBoolean("isGroup") ?: false

            if (!isGroup) {
                error("Cannot add participants to a direct conversation")
            }

            val currentParticipantIds = snapshot.getStringList("participantIds")
            val currentParticipantNames = snapshot.getStringMap("participantNames")

            val updatedParticipantIds = (
                    currentParticipantIds + newParticipantNames.keys
                    )
                .distinct()

            val updatedParticipantNames = currentParticipantNames + newParticipantNames

            transaction.update(
                conversationRef,
                mapOf(
                    "participantIds" to updatedParticipantIds,
                    "participantNames" to updatedParticipantNames,
                ),
            )
        }.await()
    }

    suspend fun leaveGroupConversation(
        conversationId: String,
        currentUserId: String,
    ) {
        val conversationRef = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(conversationRef)
            val isGroup = snapshot.getBoolean("isGroup") ?: false

            if (!isGroup) {
                error("Cannot leave a direct conversation")
            }

            val updatedParticipantIds = snapshot
                .getStringList("participantIds")
                .filterNot { participantId ->
                    participantId == currentUserId
                }

            val updatedParticipantNames = snapshot
                .getStringMap("participantNames")
                .filterKeys { participantId ->
                    participantId != currentUserId
                }

            transaction.update(
                conversationRef,
                mapOf(
                    "participantIds" to updatedParticipantIds,
                    "participantNames" to updatedParticipantNames,
                    "readBy" to FieldValue.arrayRemove(currentUserId),
                ),
            )
        }.await()
    }

    suspend fun searchMessages(
        conversationId: String,
        query: String,
    ): List<ChatMessage> {
        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) return emptyList()

        val snapshot = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_COLLECTION)
            .get()
            .await()

        return snapshot.documents
            .map { document ->
                document.toChatMessage()
            }
            .filter { message ->
                message.text.contains(trimmedQuery, ignoreCase = true) ||
                        message.senderName.contains(trimmedQuery, ignoreCase = true) ||
                        message.sharedLocation?.title?.contains(trimmedQuery, ignoreCase = true) == true ||
                        message.sharedLocation?.details?.contains(trimmedQuery, ignoreCase = true) == true
            }
            .sortedBy { message ->
                message.timestamp
            }
    }

    fun observeConversationById(
        conversationId: String,
    ): Flow<ChatConversation?> = callbackFlow {
        val registration = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.toChatConversation())
            }

        awaitClose {
            registration.remove()
        }
    }

    fun observeConversation(
        currentUserId: String,
        friendUserId: String,
    ): Flow<ChatConversation?> = callbackFlow {
        val conversationId = buildDirectConversationId(
            userA = currentUserId,
            userB = friendUserId,
        )

        val registration = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.toChatConversation())
            }

        awaitClose {
            registration.remove()
        }
    }

    fun observeAllConversations(
        currentUserId: String,
    ): Flow<List<ChatConversation>> = callbackFlow {
        val registration = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val conversations = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        document.toChatConversation()
                    }
                    ?.sortedByDescending { conversation ->
                        conversation.lastTimestamp
                    }
                    .orEmpty()

                trySend(conversations)
            }

        awaitClose {
            registration.remove()
        }
    }

    fun observeMessages(
        conversationId: String,
    ): Flow<List<ChatMessage>> = callbackFlow {
        val registration = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot
                    ?.documents
                    ?.map { document ->
                        document.toChatMessage()
                    }
                    ?.sortedBy { message ->
                        message.timestamp
                    }
                    .orEmpty()

                trySend(messages)
            }

        awaitClose {
            registration.remove()
        }
    }

    fun observeUnreadMessagesCount(
        currentUserId: String,
    ): Flow<Int> = callbackFlow {
        val registration = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }

                val unreadConversations = snapshot?.documents?.count { document ->
                    val lastSenderId = document.getString("lastSenderId")
                    val readBy = document.getStringList("readBy")

                    lastSenderId != null &&
                            lastSenderId != currentUserId &&
                            currentUserId !in readBy
                } ?: 0

                trySend(unreadConversations)
            }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String,
        clientMessageId: String,
        sharedLocation: SharedLocation? = null,
    ) {
        val trimmedText = text.trim()

        if (trimmedText.isBlank() && sharedLocation == null) return

        val now = System.currentTimeMillis()

        val conversationRef = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)

        val messageRef = conversationRef
            .collection(MESSAGES_COLLECTION)
            .document()

        val messageData = mutableMapOf<String, Any?>(
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to trimmedText,
            "timestamp" to FieldValue.serverTimestamp(),
            "clientTimestamp" to now,
            "readBy" to listOf(senderId),
            "clientMessageId" to clientMessageId,
        )

        if (sharedLocation != null) {
            messageData["sharedLocation"] = sharedLocation
        }

        val lastMessageText = when {
            trimmedText.isNotBlank() -> trimmedText
            sharedLocation != null -> "Shared a location"
            else -> "Sent a message"
        }

        val batch = firestore.batch()

        batch.set(messageRef, messageData)

        batch.update(
            conversationRef,
            mapOf(
                "lastMessage" to lastMessageText,
                "lastSenderId" to senderId,
                "lastTimestamp" to FieldValue.serverTimestamp(),
                "readBy" to listOf(senderId),
                "unreadCount" to FieldValue.increment(1),
            ),
        )

        batch.commit().await()
    }

    suspend fun markMessagesAsRead(
        conversationId: String,
        currentUserId: String,
    ) {
        markConversationAsRead(
            conversationId = conversationId,
            currentUserId = currentUserId,
        )

        markUnreadMessagesAsRead(
            conversationId = conversationId,
            currentUserId = currentUserId,
        )
    }

    private suspend fun markConversationAsRead(
        conversationId: String,
        currentUserId: String,
    ) {
        val conversationRef = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(conversationRef)
            val lastSenderId = snapshot.getString("lastSenderId")

            if (lastSenderId != currentUserId) {
                transaction.update(
                    conversationRef,
                    mapOf(
                        "readBy" to FieldValue.arrayUnion(currentUserId),
                        "unreadCount" to 0,
                    ),
                )
            }
        }.await()
    }

    private suspend fun markUnreadMessagesAsRead(
        conversationId: String,
        currentUserId: String,
    ) {
        val messagesRef = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_COLLECTION)

        val messageSnapshot = messagesRef.get().await()

        if (messageSnapshot.isEmpty) return

        val batch = firestore.batch()
        var hasUpdates = false

        messageSnapshot.documents.forEach { document ->
            val senderId = document.getString("senderId") ?: return@forEach
            val readBy = document.getStringList("readBy")

            if (senderId != currentUserId && currentUserId !in readBy) {
                batch.update(
                    document.reference,
                    "readBy",
                    FieldValue.arrayUnion(currentUserId),
                )
                hasUpdates = true
            }
        }

        if (hasUpdates) {
            batch.commit().await()
        }
    }

    private fun DocumentSnapshot.toChatMessage(): ChatMessage {
        val sharedLocationData = get("sharedLocation") as? Map<*, *>
        val sharedLocation = sharedLocationData?.toSharedLocation()

        return ChatMessage(
            id = id,
            senderId = getString("senderId") ?: "",
            senderName = getString("senderName") ?: "",
            text = getString("text") ?: "",
            timestamp = getTimestampMillis(
                fieldName = "timestamp",
                fallbackFieldName = "clientTimestamp",
            ),
            readBy = getStringList("readBy"),
            clientMessageId = getString("clientMessageId") ?: "",
            sharedLocation = sharedLocation,
        )
    }

    private fun DocumentSnapshot.toChatConversation(): ChatConversation? {
        if (!exists()) return null

        val type = getString("type") ?: ChatConversation.TYPE_DIRECT
        val isGroup = getBoolean("isGroup") ?: (type == ChatConversation.TYPE_GROUP)

        return ChatConversation(
            id = id,
            type = type,
            title = getString("title") ?: "",
            isGroup = isGroup,
            participantIds = getStringList("participantIds"),
            participantNames = getStringMap("participantNames"),
            createdBy = getString("createdBy") ?: "",
            createdAt = getTimestampMillis("createdAt"),
            lastMessage = getString("lastMessage") ?: "",
            lastSenderId = getString("lastSenderId") ?: "",
            lastTimestamp = getTimestampMillis("lastTimestamp"),
            readBy = getStringList("readBy"),
            unreadCount = getLong("unreadCount")?.toInt() ?: 0,
        )
    }

    private fun DocumentSnapshot.getTimestampMillis(
        fieldName: String,
        fallbackFieldName: String? = null,
    ): Long {
        val timestampValue = get(fieldName)

        return when (timestampValue) {
            is Timestamp -> timestampValue.toDate().time
            is Long -> timestampValue
            is Number -> timestampValue.toLong()
            else -> {
                if (fallbackFieldName != null) {
                    getLong(fallbackFieldName) ?: 0L
                } else {
                    0L
                }
            }
        }
    }

    private fun DocumentSnapshot.getStringList(
        fieldName: String,
    ): List<String> {
        return (get(fieldName) as? List<*>)
            ?.filterIsInstance<String>()
            .orEmpty()
    }

    private fun DocumentSnapshot.getStringMap(
        fieldName: String,
    ): Map<String, String> {
        return (get(fieldName) as? Map<*, *>)
            ?.mapNotNull { entry ->
                val key = entry.key?.toString() ?: return@mapNotNull null
                val value = entry.value?.toString() ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            .orEmpty()
    }

    private fun Map<*, *>.toSharedLocation(): SharedLocation {
        return SharedLocation(
            title = this["title"] as? String ?: "",
            latitude = (this["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (this["longitude"] as? Number)?.toDouble() ?: 0.0,
            details = this["details"] as? String ?: "",
        )
    }

    companion object {
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION = "messages"
    }
}
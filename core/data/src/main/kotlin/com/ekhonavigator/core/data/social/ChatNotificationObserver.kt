package com.ekhonavigator.core.data.social

import android.content.Context
import com.ekhonavigator.core.data.auth.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Singleton
class ChatNotificationObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val chatNotificationManager: ChatNotificationManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerJob: Job? = null

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun start() {
        if (observerJob?.isActive == true) return

        observerJob = scope.launch {
            authRepository.userFlow().collectLatest { currentUserId ->
                if (currentUserId == null) {
                    return@collectLatest
                }

                chatRepository.observeAllConversations(currentUserId)
                    .collect { conversations ->
                        conversations.forEach { conversation ->
                            handleConversation(
                                currentUserId = currentUserId,
                                conversation = conversation,
                            )
                        }
                    }
            }
        }
    }

    private fun handleConversation(
        currentUserId: String,
        conversation: ChatConversation,
    ) {
        if (conversation.lastMessage.isBlank()) return
        if (conversation.lastSenderId.isBlank()) return
        if (conversation.lastSenderId == currentUserId) return

        val lastSeenTimestamp = prefs.getLong(
            conversationTimestampKey(conversation.id),
            0L,
        )

        val currentTimestamp = conversation.lastTimestamp

        if (currentTimestamp <= 0L) return

        if (lastSeenTimestamp == 0L) {
            saveLastSeenTimestamp(conversation.id, currentTimestamp)
            return
        }

        if (currentTimestamp <= lastSeenTimestamp) return

        val senderName = conversation.participantNames[conversation.lastSenderId]
            ?: "New message"

        val notificationTitle = buildConversationNotificationTitle(
            currentUserId = currentUserId,
            conversation = conversation,
        )

        chatNotificationManager.showMessageNotification(
            conversationId = conversation.id,
            senderId = conversation.lastSenderId,
            senderName = senderName,
            messageText = conversation.lastMessage,
            isGroup = conversation.isGroup,
            chatTitle = notificationTitle,
        )

        saveLastSeenTimestamp(conversation.id, currentTimestamp)
    }

    private fun buildConversationNotificationTitle(
        currentUserId: String,
        conversation: ChatConversation,
    ): String {
        if (!conversation.isGroup) {
            return conversation.participantNames[conversation.lastSenderId]
                ?: "New message"
        }

        if (conversation.title.isNotBlank()) {
            return conversation.title
        }

        return conversation.participantNames
            .filterKeys { participantId ->
                participantId != currentUserId
            }
            .values
            .filter { name ->
                name.isNotBlank()
            }
            .joinToString(", ")
            .ifBlank {
                "Group Chat"
            }
    }

    private fun saveLastSeenTimestamp(
        conversationId: String,
        timestamp: Long,
    ) {
        prefs.edit()
            .putLong(conversationTimestampKey(conversationId), timestamp)
            .apply()
    }

    private fun conversationTimestampKey(conversationId: String): String {
        return "last_notified_$conversationId"
    }

    companion object {
        private const val PREFS_NAME = "chat_notifications"
    }
}
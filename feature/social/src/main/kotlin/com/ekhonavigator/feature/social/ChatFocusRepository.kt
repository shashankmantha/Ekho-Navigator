package com.ekhonavigator.feature.social

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class ChatFocusRequest(
    val conversationId: String,
    val messageId: String,
)

@Singleton
class ChatFocusRepository @Inject constructor() {

    private val _focusRequests = MutableSharedFlow<ChatFocusRequest>(
        extraBufferCapacity = 1,
    )

    val focusRequests: SharedFlow<ChatFocusRequest> = _focusRequests

    fun requestFocus(
        conversationId: String,
        messageId: String,
    ) {
        _focusRequests.tryEmit(
            ChatFocusRequest(
                conversationId = conversationId,
                messageId = messageId,
            ),
        )
    }
}
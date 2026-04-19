package com.ekhonavigator.core.data.social

import com.ekhonavigator.core.model.SharedLocation

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val readBy: List<String> = emptyList(),
    val clientMessageId: String = "",

    val sharedLocation: SharedLocation? = null
)
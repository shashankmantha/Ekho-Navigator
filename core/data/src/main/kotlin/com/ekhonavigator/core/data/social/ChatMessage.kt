package com.ekhonavigator.core.data.social

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val readBy: List<String> = emptyList(),
    val clientMessageId: String = "",
)
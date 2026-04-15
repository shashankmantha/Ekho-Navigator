package com.ekhonavigator.core.data.social

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val readBy: List<String> = emptyList(),
    val clientMessageId: String = "",
)
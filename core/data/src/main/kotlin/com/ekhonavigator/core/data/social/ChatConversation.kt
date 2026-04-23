package com.ekhonavigator.core.data.social

data class ChatConversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastTimestamp: Long = 0L,
    val readBy: List<String> = emptyList(),
    val createdAt: Long = 0L,
)
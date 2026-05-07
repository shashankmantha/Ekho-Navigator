package com.ekhonavigator.core.data.social

data class ChatConversation(
    val id: String = "",
    val type: String = TYPE_DIRECT,
    val title: String = "",
    val isGroup: Boolean = false,
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastTimestamp: Long = 0L,
    val readBy: List<String> = emptyList(),
    val unreadCount: Int = 0,
) {
    companion object {
        const val TYPE_DIRECT = "direct"
        const val TYPE_GROUP = "group"
    }
}
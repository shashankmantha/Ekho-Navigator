package com.ekhonavigator.feature.social

data class ChatMessageUi(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val readBy: List<String> = emptyList(),
    val deliveryState: MessageDeliveryState = MessageDeliveryState.SENT,
    val clientMessageId: String = "",
)

enum class MessageDeliveryState {
    SENDING,
    FAILED,
    SENT,
    READ,
}
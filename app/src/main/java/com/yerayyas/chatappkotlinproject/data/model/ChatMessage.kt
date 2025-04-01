package com.yerayyas.chatappkotlinproject.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val messageType: MessageType = MessageType.TEXT,
    val readStatus: ReadStatus = ReadStatus.SENT
)

enum class MessageType {
    TEXT,
    IMAGE
}

enum class ReadStatus {
    SENT,
    DELIVERED,
    READ
}

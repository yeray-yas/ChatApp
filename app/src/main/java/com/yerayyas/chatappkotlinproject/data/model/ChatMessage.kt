package com.yerayyas.chatappkotlinproject.data.model

/**
 * Represents a message within a chat conversation.
 *
 * @property id Unique identifier of the message.
 * @property senderId ID of the user who sent the message.
 * @property receiverId ID of the user who received the message.
 * @property message Text content of the message.
 * @property timestamp Time the message was sent, in milliseconds.
 * @property imageUrl Optional image URL associated with the message (used if the message is of type IMAGE).
 * @property messageType Type of the message: TEXT or IMAGE.
 * @property readStatus Status of the message: SENT, DELIVERED, or READ.
 */
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val messageType: MessageType = MessageType.TEXT,
    val readStatus: ReadStatus = ReadStatus.SENT
) {

    /**
     * Checks whether the message was sent by the given user.
     *
     * @param userId The ID of the user to compare.
     * @return true if the message was sent by the user, false otherwise.
     */
    fun isSentBy(userId: String): Boolean = senderId == userId

    /**
     * Checks whether the message was received by the given user.
     *
     * @param userId The ID of the user to compare.
     * @return true if the message was received by the user, false otherwise.
     */
    fun isReceivedBy(userId: String): Boolean = receiverId == userId

}

/**
 * Enum representing the type of a message.
 */
enum class MessageType {
    TEXT,
    IMAGE
}

/**
 * Enum representing the read status of a message.
 */
enum class ReadStatus {
    SENT,
    DELIVERED,
    READ
}

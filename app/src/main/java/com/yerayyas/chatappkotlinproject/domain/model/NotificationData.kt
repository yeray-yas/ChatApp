package com.yerayyas.chatappkotlinproject.domain.model

/**
 * Domain model representing the data needed to display a chat notification.
 *
 * This is a pure domain object that contains all the essential information
 * for creating a notification without any Android-specific dependencies.
 */
data class NotificationData(
    val senderId: String,
    val senderName: String,
    val messageBody: String,
    val chatId: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(senderId.isNotBlank()) { "Sender ID cannot be blank" }
        require(senderName.isNotBlank()) { "Sender name cannot be blank" }
        require(messageBody.isNotBlank()) { "Message body cannot be blank" }
        require(chatId.isNotBlank()) { "Chat ID cannot be blank" }
    }

    /**
     * Generates a unique notification ID based on the sender ID.
     */
    val notificationId: Int
        get() = senderId.hashCode()
}
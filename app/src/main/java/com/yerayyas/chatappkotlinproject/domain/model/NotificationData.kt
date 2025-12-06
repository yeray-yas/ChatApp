package com.yerayyas.chatappkotlinproject.domain.model

/**
 * Domain model representing the data needed to display a chat notification.
 *
 * This is a pure domain object that contains all the essential information
 * for creating a notification without any Android-specific dependencies.
 *
 * Supports both individual and group chat notifications.
 */
data class NotificationData(
    val senderId: String,
    val senderName: String,
    val messageBody: String,
    val chatId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGroupMessage: Boolean = false,
    val groupName: String? = null
) {
    init {
        require(senderId.isNotBlank()) { "Sender ID cannot be blank" }
        require(senderName.isNotBlank()) { "Sender name cannot be blank" }
        require(messageBody.isNotBlank()) { "Message body cannot be blank" }
        require(chatId.isNotBlank()) { "Chat ID cannot be blank" }

        // If it's a group message, group name should be provided
        if (isGroupMessage) {
            require(!groupName.isNullOrBlank()) { "Group name cannot be blank for group messages" }
        }
    }

    /**
     * Generates a unique notification ID based on the chat type and ID.
     * For individual chats, uses sender ID hash.
     * For group chats, uses group ID hash.
     */
    val notificationId: Int
        get() = if (isGroupMessage) {
            chatId.hashCode()
        } else {
            senderId.hashCode()
        }

    /**
     * Gets the display title for the notification.
     * For individual chats: sender name
     * For group chats: group name
     */
    val notificationTitle: String
        get() = if (isGroupMessage) {
            groupName ?: "Group Chat"
        } else {
            senderName
        }

    /**
     * Gets the display content for the notification.
     * For individual chats: message body
     * For group chats: sender name + message body
     */
    val notificationContent: String
        get() = if (isGroupMessage) {
            "$senderName: $messageBody"
        } else {
            messageBody
        }
}

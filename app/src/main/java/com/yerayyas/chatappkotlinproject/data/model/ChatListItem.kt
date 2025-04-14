package com.yerayyas.chatappkotlinproject.data.model

/**
 * Represents a chat item displayed in the chat list UI.
 *
 * This data class holds the necessary information to show an overview
 * of a conversation, including the chat ID, the other participant's ID and name,
 * the last message exchanged, a timestamp, and the number of unread messages.
 *
 * @property chatId Unique identifier for the chat conversation.
 * @property otherUserId The user ID of the other participant in the chat.
 * @property otherUsername The username of the other participant.
 * @property lastMessage The most recent message in the chat.
 * @property timestamp Timestamp of the last message, usually in milliseconds.
 * @property unreadCount Number of unread messages in the chat (default is 0).
 */
data class ChatListItem(
    val chatId: String,
    val otherUserId: String,
    val otherUsername: String,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0
)

package com.yerayyas.chatappkotlinproject.data.model

/**
 * Represents a single item in the user's list of ongoing chats.
 *
 * This data class is a UI model, typically constructed by aggregating data from different sources.
 * It holds the essential information required to display a preview of a chat conversation
 * in a list format.
 *
 * @property chatId The unique identifier for the chat conversation.
 * @property otherUserId The unique ID of the other user in the conversation.
 * @property otherUsername The display name of the other user, used as the main title in the list item.
 * @property lastMessage A preview of the most recent message in the chat, used as the subtitle.
 * @property timestamp The timestamp of the `lastMessage`, used for sorting the chat list.
 * @property unreadCount The number of messages in the chat that are unread by the current user.
 */
data class ChatListItem(
    val chatId: String = "",
    val otherUserId: String = "",
    val otherUsername: String = "",
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)

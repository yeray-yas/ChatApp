package com.yerayyas.chatappkotlinproject.domain.repository

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over chat data operations.
 */
interface ChatRepository {
    /** Returns current signed-in user’s ID or empty if none. */
    fun getCurrentUserId(): String

    /** Streams the messages in the chat with [otherUserId]. */
    fun getMessages(otherUserId: String): Flow<List<ChatMessage>>

    /** Marks all unread messages in [chatId] as read. */
    suspend fun markMessagesAsRead(chatId: String)

    /** Sends a text message. */
    suspend fun sendTextMessage(receiverId: String, messageText: String)

    /** Sends an image message. */
    suspend fun sendImageMessage(receiverId: String, imageUri: Uri)
}
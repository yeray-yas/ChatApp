package com.yerayyas.chatappkotlinproject.domain.repository

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for individual chat operations and messaging.
 *
 * This interface defines the core contract for one-on-one chat functionality including
 * message sending, receiving, and management. It abstracts the data layer implementation
 * while providing a clean domain interface for chat operations.
 *
 * Key responsibilities:
 * - **User Session Management**: Handle current user identification and authentication
 * - **Message Streaming**: Provide real-time message updates through reactive streams
 * - **Message Sending**: Support text and image message transmission with reply functionality
 * - **Read Status Management**: Track and update message read states for delivery confirmations
 * - **Real-time Updates**: Enable live chat experiences through Flow-based data streams
 *
 * The repository follows Clean Architecture principles by:
 * - Defining platform-agnostic contracts for chat operations
 * - Using domain models (ChatMessage) rather than data layer models
 * - Providing reactive interfaces through Kotlin Flow
 * - Abstracting persistence and networking concerns from domain logic
 *
 * Implementation considerations:
 * - All operations should be thread-safe and suitable for concurrent access
 * - Message streams should handle network connectivity changes gracefully
 * - Error handling should use appropriate exception types or Result wrappers
 * - Real-time updates should be efficient and minimize unnecessary emissions
 */
interface ChatRepository {

    /**
     * Retrieves the unique identifier of the currently authenticated user.
     *
     * This method provides access to the current user's session information
     * without exposing authentication implementation details to the domain layer.
     * Essential for determining message ownership and chat participation.
     *
     * @return Current user's unique identifier, or empty string if not authenticated
     */
    fun getCurrentUserId(): String

    /**
     * Provides a reactive stream of messages for a specific chat conversation.
     *
     * This method returns a Flow that emits the complete list of messages whenever
     * the conversation data changes. The implementation should handle real-time
     * updates from the backend and provide ordered message sequences.
     *
     * Side effects:
     * - Automatically marks unread messages as read when the stream is observed
     * - Orders messages by timestamp for proper conversation flow
     * - Handles message updates, deletions, and new arrivals
     *
     * @param otherUserId Unique identifier of the other participant in the conversation
     * @return Flow emitting ordered lists of ChatMessage objects for real-time updates
     */
    fun getMessages(otherUserId: String): Flow<List<ChatMessage>>

    /**
     * Marks all unread messages in a chat as read by the current user.
     *
     * This operation updates the read status of messages where the current user
     * is the recipient, enabling read receipt functionality and proper message
     * state management for UI indicators.
     *
     * @param chatId Unique identifier of the chat conversation to update
     * @throws Exception if the read status update operation fails
     */
    suspend fun markMessagesAsRead(chatId: String)

    /**
     * Marks a specific message as read by a particular user.
     *
     * This method enables granular read status management, particularly useful
     * for group chat scenarios or advanced read receipt tracking where individual
     * message read states need to be recorded per user.
     *
     * @param messageId Unique identifier of the message to mark as read
     * @param userId Unique identifier of the user who read the message
     * @throws Exception if the read status update operation fails
     */
    suspend fun markMessageAsRead(messageId: String, userId: String)

    /**
     * Sends a plain text message to another user.
     *
     * This method handles the transmission of text-based messages including
     * message validation, timestamp assignment, and delivery to the recipient.
     * The message will be associated with the current authenticated user as sender.
     *
     * @param receiverId Unique identifier of the message recipient
     * @param messageText Content of the text message (must not be blank)
     * @throws Exception if message sending fails due to network, validation, or permission issues
     */
    suspend fun sendTextMessage(receiverId: String, messageText: String)

    /**
     * Sends an image message to another user.
     *
     * This method handles image upload and message creation for media-based messages.
     * The implementation should handle image processing, cloud storage upload,
     * and message creation with the resulting image URL.
     *
     * @param receiverId Unique identifier of the message recipient
     * @param imageUri Local URI of the image to upload and send
     * @throws Exception if image upload or message sending fails
     */
    suspend fun sendImageMessage(receiverId: String, imageUri: Uri)

    /**
     * Sends a text message as a reply to an existing message.
     *
     * This method creates a text message with reply context, preserving the
     * relationship to the original message for proper conversation threading
     * and UI display of reply chains.
     *
     * @param receiverId Unique identifier of the message recipient
     * @param messageText Content of the reply text message
     * @param replyToMessage The original message being replied to
     * @throws Exception if reply message sending fails
     */
    suspend fun sendTextMessageReply(
        receiverId: String,
        messageText: String,
        replyToMessage: ChatMessage
    )

    /**
     * Sends an image message as a reply to an existing message.
     *
     * This method combines image upload functionality with reply context,
     * creating an image message that references the original message for
     * proper conversation threading.
     *
     * @param receiverId Unique identifier of the message recipient
     * @param imageUri Local URI of the image to upload and send
     * @param replyToMessage The original message being replied to
     * @throws Exception if image upload or reply message sending fails
     */
    suspend fun sendImageMessageReply(
        receiverId: String,
        imageUri: Uri,
        replyToMessage: ChatMessage
    )
}

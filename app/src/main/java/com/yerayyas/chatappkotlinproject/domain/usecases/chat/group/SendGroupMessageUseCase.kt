package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupActivityType
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupMessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Domain use case for sending various types of messages to group chat conversations.
 *
 * This use case encapsulates the business logic for group message transmission,
 * supporting text messages, image messages, and system notifications. It handles
 * message validation, authentication verification, and proper message creation
 * for different message types within group chat contexts.
 *
 * Key responsibilities:
 * - Validate user authentication and message content
 * - Handle different message types (text, image, system messages)
 * - Support advanced features (mentions, replies, captions)
 * - Manage image upload operations for media messages
 * - Create properly formatted GroupMessage objects
 * - Provide consistent error handling across all message types
 *
 * The use case follows Clean Architecture principles by delegating persistence
 * operations to the repository while maintaining business logic validation.
 */
class SendGroupMessageUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Sends a text message to a group chat with optional advanced features.
     *
     * This method handles plain text message sending with support for user mentions,
     * message replies, and comprehensive validation. The message is associated with
     * the currently authenticated user as the sender.
     *
     * @param groupId Unique identifier of the target group chat
     * @param message Text content of the message (1-1000 characters)
     * @param senderName Display name of the sender at the time of sending
     * @param senderImageUrl Optional profile image URL of the sender
     * @param mentionedUsers List of user IDs mentioned in the message
     * @param replyToMessageId Optional ID of the message being replied to
     * @param replyToMessage Optional complete message object being replied to
     * @return Result indicating success or failure with error details
     */
    suspend fun sendTextMessage(
        groupId: String,
        message: String,
        senderName: String,
        senderImageUrl: String? = null,
        mentionedUsers: List<String> = emptyList(),
        replyToMessageId: String? = null,
        replyToMessage: GroupMessage? = null
    ): Result<Unit> {
        return try {
            // Validate authentication
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Validate message content
            if (message.isBlank()) {
                return Result.failure(IllegalArgumentException("Message cannot be empty"))
            }

            if (message.length > 1000) {
                return Result.failure(IllegalArgumentException("Message cannot exceed 1000 characters"))
            }

            val groupMessage = GroupMessage(
                groupId = groupId,
                senderId = currentUserId,
                senderName = senderName,
                senderImageUrl = senderImageUrl,
                message = message.trim(),
                timestamp = System.currentTimeMillis(),
                messageType = GroupMessageType.TEXT,
                readStatus = ReadStatus.SENT,
                mentionedUsers = mentionedUsers,
                replyToMessageId = replyToMessageId,
                replyToMessage = replyToMessage
            )

            groupRepository.sendMessageToGroup(groupId, groupMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends an image message to a group chat with optional caption and advanced features.
     *
     * This method handles image message sending including image upload to cloud storage,
     * optional caption text, and support for mentions and replies. The image is uploaded
     * to Firebase Storage before creating the message with the resulting URL.
     *
     * @param groupId Unique identifier of the target group chat
     * @param imageUri Local URI of the image to upload and send
     * @param caption Optional text caption for the image (max 1000 characters)
     * @param senderName Display name of the sender at the time of sending
     * @param senderImageUrl Optional profile image URL of the sender
     * @param mentionedUsers List of user IDs mentioned in the caption
     * @param replyToMessageId Optional ID of the message being replied to
     * @param replyToMessage Optional complete message object being replied to
     * @return Result indicating success or failure with error details
     */
    suspend fun sendImageMessage(
        groupId: String,
        imageUri: Uri,
        caption: String = "",
        senderName: String,
        senderImageUrl: String? = null,
        mentionedUsers: List<String> = emptyList(),
        replyToMessageId: String? = null,
        replyToMessage: GroupMessage? = null
    ): Result<Unit> {
        return try {
            // Validate authentication
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            if (caption.length > 1000) {
                return Result.failure(IllegalArgumentException("Caption cannot exceed 1000 characters"))
            }

            // Upload image to Firebase Storage first
            val uploadResult = groupRepository.uploadGroupMessageImage(groupId, imageUri)
            if (uploadResult.isFailure) {
                return Result.failure(
                    uploadResult.exceptionOrNull() ?: Exception("Error uploading image")
                )
            }

            val imageUrl = uploadResult.getOrThrow()

            val groupMessage = GroupMessage(
                groupId = groupId,
                senderId = currentUserId,
                senderName = senderName,
                senderImageUrl = senderImageUrl,
                message = caption.trim(),
                timestamp = System.currentTimeMillis(),
                messageType = GroupMessageType.IMAGE,
                imageUrl = imageUrl,
                readStatus = ReadStatus.SENT,
                mentionedUsers = mentionedUsers,
                replyToMessageId = replyToMessageId,
                replyToMessage = replyToMessage
            )

            groupRepository.sendMessageToGroup(groupId, groupMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends a system-generated message for group activity notifications.
     *
     * This method creates automatic notification messages for group activities such as
     * member additions, removals, admin promotions, and other group management events.
     * System messages are distinguished by their isSystemMessage flag and special styling.
     *
     * @param groupId Unique identifier of the target group chat
     * @param message Content of the system notification message
     * @param systemMessageType Type of group activity this message represents
     * @return Result indicating success or failure with error details
     */
    suspend fun sendSystemMessage(
        groupId: String,
        message: String,
        systemMessageType: GroupActivityType
    ): Result<Unit> {
        return try {
            // Validate authentication
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            val groupMessage = GroupMessage(
                groupId = groupId,
                senderId = currentUserId,
                senderName = "System",
                message = message,
                timestamp = System.currentTimeMillis(),
                messageType = GroupMessageType.SYSTEM_MESSAGE,
                readStatus = ReadStatus.SENT,
                isSystemMessage = true,
                systemMessageType = systemMessageType
            )

            groupRepository.sendMessageToGroup(groupId, groupMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
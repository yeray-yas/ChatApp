package com.yerayyas.chatappkotlinproject.domain.usecases.group

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupMessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Use case for sending messages to a group
 */
class SendGroupMessageUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Sends a text message to a group
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
     * Sends a message with image to a group
     */
    suspend fun sendImageMessage(
        groupId: String,
        imageUri: android.net.Uri,
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
     * Sends a system message (automatic notifications)
     */
    suspend fun sendSystemMessage(
        groupId: String,
        message: String,
        systemMessageType: com.yerayyas.chatappkotlinproject.data.model.GroupActivityType
    ): Result<Unit> {
        return try {
            // Validate authentication
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            val groupMessage = GroupMessage(
                groupId = groupId,
                senderId = currentUserId,
                senderName = "Sistema",
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
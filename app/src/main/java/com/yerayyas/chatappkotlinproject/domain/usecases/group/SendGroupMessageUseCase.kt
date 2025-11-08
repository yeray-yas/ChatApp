package com.yerayyas.chatappkotlinproject.domain.usecases.group

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupMessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Use case para enviar mensajes a un grupo
 */
class SendGroupMessageUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Envía un mensaje de texto a un grupo
     */
    suspend fun sendTextMessage(
        groupId: String,
        message: String,
        senderName: String,
        senderImageUrl: String? = null,
        mentionedUsers: List<String> = emptyList(),
        replyToMessageId: String? = null
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            if (message.isBlank()) {
                return Result.failure(IllegalArgumentException("El mensaje no puede estar vacío"))
            }

            if (message.length > 1000) {
                return Result.failure(IllegalArgumentException("El mensaje no puede tener más de 1000 caracteres"))
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
                replyToMessageId = replyToMessageId
            )

            groupRepository.sendMessageToGroup(groupId, groupMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envía un mensaje con imagen a un grupo
     */
    suspend fun sendImageMessage(
        groupId: String,
        imageUrl: String,
        caption: String = "",
        senderName: String,
        senderImageUrl: String? = null,
        mentionedUsers: List<String> = emptyList(),
        replyToMessageId: String? = null
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            if (imageUrl.isBlank()) {
                return Result.failure(IllegalArgumentException("La URL de la imagen no puede estar vacía"))
            }

            if (caption.length > 1000) {
                return Result.failure(IllegalArgumentException("El caption no puede tener más de 1000 caracteres"))
            }

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
                replyToMessageId = replyToMessageId
            )

            groupRepository.sendMessageToGroup(groupId, groupMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envía un mensaje del sistema (notificaciones automáticas)
     */
    suspend fun sendSystemMessage(
        groupId: String,
        message: String,
        systemMessageType: com.yerayyas.chatappkotlinproject.data.model.GroupActivityType
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

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
package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupMessageType
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import java.util.UUID
import javax.inject.Inject

class SendGroupImageMessageUseCase @Inject constructor(
    private val repository: GroupChatRepository
) {
    suspend operator fun invoke(
        groupId: String,
        senderId: String,
        senderName: String,
        senderImageUrl: String?,
        imageUri: Uri,
        replyToMessage: GroupMessage? = null
    ): Result<Unit> {
        val uploadResult = repository.uploadGroupMessageImage(groupId, imageUri)

        return if (uploadResult.isSuccess) {
            val imageUrl =
                uploadResult.getOrNull() ?: return Result.failure(Exception("Image upload failed"))

            val message = GroupMessage(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                senderId = senderId,
                senderName = senderName,
                senderImageUrl = senderImageUrl,
                message = "Image",
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis(),
                messageType = GroupMessageType.IMAGE,
                replyToMessageId = replyToMessage?.id,
                replyToMessage = replyToMessage

            )

            repository.sendMessageToGroup(groupId, message)
        } else {
            Result.failure(uploadResult.exceptionOrNull() ?: Exception("Unknown upload error"))
        }
    }
}

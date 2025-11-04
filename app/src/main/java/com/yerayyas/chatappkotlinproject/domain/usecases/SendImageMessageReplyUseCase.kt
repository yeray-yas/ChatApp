package com.yerayyas.chatappkotlinproject.domain.usecases

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends an image message as a reply to another message.
 *
 * @param repository Handles the actual image upload and message send with reply information.
 */
@Singleton
class SendImageMessageReplyUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Sends an image reply at [imageUri] to the user with [receiverId] in response to [replyToMessage].
     *
     * @param receiverId ID of the target user.
     * @param imageUri URI of the image to send as a reply.
     * @param replyToMessage The original message being replied to.
     */
    suspend operator fun invoke(receiverId: String, imageUri: Uri, replyToMessage: ChatMessage) {
        repository.sendImageMessageReply(receiverId, imageUri, replyToMessage)
    }
}
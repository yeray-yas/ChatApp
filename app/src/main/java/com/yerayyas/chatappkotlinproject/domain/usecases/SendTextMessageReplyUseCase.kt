package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends a text message as a reply to another message.
 *
 * @param repository Handles the actual message send with reply information.
 */
@Singleton
class SendTextMessageReplyUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Sends a text reply [text] to the user with [receiverId] in response to [replyToMessage].
     *
     * @param receiverId ID of the target user.
     * @param text The reply message content.
     * @param replyToMessage The original message being replied to.
     */
    suspend operator fun invoke(receiverId: String, text: String, replyToMessage: ChatMessage) {
        repository.sendTextMessageReply(receiverId, text, replyToMessage)
    }
}
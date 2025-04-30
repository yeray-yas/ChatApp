package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.data.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends a plain text message to a chat participant.
 *
 * @param repository Handles the actual message send.
 */
@Singleton
class SendTextMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Sends [text] to the user with [receiverId].
     *
     * @param receiverId ID of the target user.
     * @param text       The message content.
     */
    suspend operator fun invoke(receiverId: String, text: String) {
        repository.sendTextMessage(receiverId, text)
    }
}

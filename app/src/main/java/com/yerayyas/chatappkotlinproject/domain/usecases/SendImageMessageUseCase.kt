package com.yerayyas.chatappkotlinproject.domain.usecases

import android.net.Uri
import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends an image message to a chat participant.
 *
 * @param repository Handles the actual image upload and message send.
 */
@Singleton
class SendImageMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /**
     * Sends an image at [imageUri] to the user with [receiverId].
     *
     * @param receiverId ID of the target user.
     * @param imageUri   URI of the image to send.
     */
    suspend operator fun invoke(receiverId: String, imageUri: Uri) {
        repository.sendImageMessage(receiverId, imageUri)
    }
}

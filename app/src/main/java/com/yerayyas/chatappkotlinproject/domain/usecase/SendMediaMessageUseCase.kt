package com.yerayyas.chatappkotlinproject.domain.usecase

import android.net.Uri
import com.yerayyas.chatappkotlinproject.domain.ChatRepository
import javax.inject.Inject

class SendMediaMessageUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(receiverId: String, fileUri: Uri, messageType: String) {
        chatRepository.sendMediaMessage(receiverId, fileUri, messageType)
    }
}
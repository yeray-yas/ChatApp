package com.yerayyas.chatappkotlinproject.domain.usecase

import com.yerayyas.chatappkotlinproject.domain.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(receiverId: String, messageText: String) {
        chatRepository.sendMessage(receiverId, messageText)
    }
}
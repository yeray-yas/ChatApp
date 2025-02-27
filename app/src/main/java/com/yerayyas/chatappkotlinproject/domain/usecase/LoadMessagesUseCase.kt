package com.yerayyas.chatappkotlinproject.domain.usecase

import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadMessagesUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    operator fun invoke(receiverId: String): Flow<List<ChatMessage>> {
        return chatRepository.loadMessages(receiverId)
    }
}
package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves the stream of chat messages for a given conversation.
 *
 * @param repository Source of chat data.
 */
@Singleton
class LoadChatMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(otherUserId: String): Flow<List<ChatMessage>> =
        repository.getMessages(otherUserId)
}


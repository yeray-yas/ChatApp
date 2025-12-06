package com.yerayyas.chatappkotlinproject.domain.usecases.chat.unified

import com.yerayyas.chatappkotlinproject.domain.usecases.LoadChatMessagesUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.LoadGroupMessagesUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.GetCurrentUserIdUseCase
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.UnifiedMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveUnifiedMessagesUseCase @Inject constructor(
    private val loadChatMessagesUseCase: LoadChatMessagesUseCase,
    private val loadGroupMessagesUseCase: LoadGroupMessagesUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    operator fun invoke(chatId: String, chatType: ChatType): Flow<List<UnifiedMessage>> {
        val currentUserId = getCurrentUserIdUseCase() ?: return emptyFlow()

        return when (chatType) {
            is ChatType.Individual -> {
                loadChatMessagesUseCase(chatId).map { messages ->
                    messages.filterNot { msg ->
                        msg.senderId == currentUserId && msg.imageUrl == "PENDING_UPLOAD"
                    }.map { UnifiedMessage.Individual(it) }
                }
            }
            is ChatType.Group -> {
                loadGroupMessagesUseCase(chatId).map { messages ->
                    messages.filterNot { msg ->
                        msg.senderId == currentUserId && msg.imageUrl == "PENDING_UPLOAD"
                    }.map { UnifiedMessage.Group(it) }
                }
            }
        }
    }
}
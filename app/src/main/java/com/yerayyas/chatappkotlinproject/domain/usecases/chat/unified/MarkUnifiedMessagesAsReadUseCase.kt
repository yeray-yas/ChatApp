package com.yerayyas.chatappkotlinproject.domain.usecases.chat.unified

import com.yerayyas.chatappkotlinproject.domain.repository.ChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelChatNotificationsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.GetCurrentUserIdUseCase
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import javax.inject.Inject

/**
 * UseCase to handle the "Read Receipt" logic uniformly for both Individual and Group chats.
 *
 * Responsibilities:
 * 1. Update the read status in the database/repository.
 * 2. Cancel any active system notifications for this specific chat.
 */
class MarkUnifiedMessagesAsReadUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupChatRepository: GroupChatRepository,
    private val cancelChatNotificationsUseCase: CancelChatNotificationsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(chatId: String, chatType: ChatType) {
        val currentUserId = getCurrentUserIdUseCase() ?: return

        when (chatType) {
            is ChatType.Individual -> {
                try {
                    chatRepository.markMessagesAsRead(chatId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                cancelChatNotificationsUseCase(chatId)
            }

            is ChatType.Group -> {
                try {
                    groupChatRepository.markGroupMessagesAsRead(groupId = chatId, userId = currentUserId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                cancelChatNotificationsUseCase.cancelGroupNotifications(chatId)
            }
        }
    }
}
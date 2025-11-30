package com.yerayyas.chatappkotlinproject.domain.usecases.chat.unified

import com.yerayyas.chatappkotlinproject.domain.model.ChatHeaderInfo
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import javax.inject.Inject

/**
 * UseCase to retrieve the display information for the chat screen header (TopBar).
 *
 * It handles the logic of fetching either User details (for individual chats)
 * or Group details (for group chats) and mapping them to a unified UI model.
 */
class GetChatHeaderInfoUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val groupChatRepository: GroupChatRepository
) {
    suspend operator fun invoke(chatId: String, chatType: ChatType): ChatHeaderInfo {
        return when (chatType) {
            is ChatType.Individual -> {
                val user = userRepository.getUserById(chatId)

                ChatHeaderInfo(
                    title = user?.username ?: "Unknown User",
                    imageUrl = user?.profileImage,
                    subtitle = if (user?.status?.lowercase() == "online") "Online" else null
                )
            }
            is ChatType.Group -> {
                val group = groupChatRepository.getGroupById(chatId)

                val memberCount = group?.memberIds?.size ?: 0
                val memberSubtitle = if (memberCount == 1) "1 member" else "$memberCount members"

                ChatHeaderInfo(
                    title = group?.name ?: "Unknown Group",
                    imageUrl = group?.imageUrl,
                    subtitle = memberSubtitle
                )
            }
        }
    }
}
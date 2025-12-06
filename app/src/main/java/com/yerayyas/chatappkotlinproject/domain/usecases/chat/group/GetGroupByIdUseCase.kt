package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Use case to get a specific group by its unique ID.
 * This is a one-shot operation.
 */
class GetGroupByIdUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository
) {
    suspend operator fun invoke(groupId: String): GroupChat? {
        if (groupId.isBlank()) return null
        return groupRepository.getGroupById(groupId)
    }
}

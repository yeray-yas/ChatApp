package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

class LoadGroupInfoUseCase @Inject constructor(
    private val repository: GroupChatRepository
) {
    suspend operator fun invoke(groupId: String): GroupChat? {
        return repository.getGroupById(groupId)
    }
}

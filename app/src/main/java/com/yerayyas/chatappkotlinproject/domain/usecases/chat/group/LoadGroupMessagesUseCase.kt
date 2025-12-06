package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadGroupMessagesUseCase @Inject constructor(
    private val repository: GroupChatRepository
) {
    operator fun invoke(groupId: String): Flow<List<GroupMessage>> {
        return repository.getGroupMessages(groupId)
    }
}

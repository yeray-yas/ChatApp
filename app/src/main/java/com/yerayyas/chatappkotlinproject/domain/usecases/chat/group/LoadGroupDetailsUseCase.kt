package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.yerayyas.chatappkotlinproject.data.model.GroupDetails
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import javax.inject.Inject

class LoadGroupDetailsUseCase @Inject constructor(
    private val groupChatRepository: GroupChatRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(groupId: String): Result<GroupDetails> {
        return try {
            // 1. Obtener la información del grupo
            val group = groupChatRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Group not found"))

            // 2. Obtener los miembros usando los IDs del grupo
            val members = userRepository.getUsersByIds(group.memberIds)

            // 3. Devolver todo empaquetado
            Result.success(GroupDetails(group, members))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
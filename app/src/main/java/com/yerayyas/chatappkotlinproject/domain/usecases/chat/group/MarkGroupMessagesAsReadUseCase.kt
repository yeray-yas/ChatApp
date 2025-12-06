package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

class MarkGroupMessagesAsReadUseCase @Inject constructor(
    private val groupChatRepository: GroupChatRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(groupId: String): Result<Unit> {
        val userId = auth.currentUser?.uid
        return if (userId != null) {
            groupChatRepository.markGroupMessagesAsRead(groupId, userId)
        } else {
            Result.failure(Exception("User not authenticated"))
        }
    }
}

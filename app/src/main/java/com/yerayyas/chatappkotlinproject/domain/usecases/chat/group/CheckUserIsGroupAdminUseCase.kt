package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Use case to check if the current authenticated user is an administrator of a specific group.
 */
class CheckUserIsGroupAdminUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth
) {
    suspend operator fun invoke(groupId: String): Boolean {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return false
        if (groupId.isBlank()) return false

        val group = groupRepository.getGroupById(groupId)
        return group?.isAdmin(currentUserId) ?: false
    }
}


package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Use case for getting a real-time stream of the current user's groups.
 *
 * This use case provides a Flow that emits the list of groups the authenticated
 * user is a member of. It handles the case where the user is not authenticated
 * by returning an empty flow.
 *
 * @property groupRepository The repository to fetch group data from.
 * @property firebaseAuth The authentication service to identify the current user.
 */
class GetUserGroupsUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * By using the 'invoke' operator, this use case can be called as a function
     * directly on an instance of the class (e.g., `getUserGroupsUseCase()`).
     */
    operator fun invoke(): Flow<List<GroupChat>> {
        val currentUserId = firebaseAuth.currentUser?.uid
        return if (currentUserId != null) {
            groupRepository.getUserGroups(currentUserId)
        } else {
            // If the user is not authenticated, return a flow that emits an empty list.
            flowOf(emptyList())
        }
    }
}
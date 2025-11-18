package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

private const val TAG = "GetUnreadCountForGroup"

/**
 * A use case that retrieves a real-time stream of the unread message count for a specific group.
 *
 * This use case acts as a simple bridge between the ViewModel and the repository, ensuring
 * that the business logic for fetching the unread count for a single group is centralized.
 * It identifies the current user and requests the count from the repository.
 *
 * @property groupChatRepository The repository providing access to group data.
 * @property auth The authentication service to identify the current user.
 */
class GetUnreadMessagesCountForGroupUseCase @Inject constructor(
    private val groupChatRepository: GroupChatRepository,
    private val auth: FirebaseAuth
) {
    /**
     * Executes the use case to get the unread message count for a specific group.
     *
     * @param groupId The unique identifier of the group for which to fetch the count.
     * @return A [Flow] that emits the number of unread messages for the current user in that group.
     *         If the user is not authenticated, it returns a flow that immediately emits 0.
     */
    operator fun invoke(groupId: String): Flow<Int> {
        val currentUserId = auth.currentUser?.uid

        return if (currentUserId != null) {
            Log.d(TAG, "Requesting unread count for group '$groupId' for user '$currentUserId'")
            groupChatRepository.getUnreadMessagesCountForGroup(groupId, currentUserId)
        } else {
            Log.w(TAG, "User is not authenticated. Returning a flow of 0 for group '$groupId'.")
            flowOf(0)
        }
    }
}

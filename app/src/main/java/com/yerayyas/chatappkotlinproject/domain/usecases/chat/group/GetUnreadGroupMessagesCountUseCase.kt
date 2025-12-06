package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

private const val TAG = "GetUnreadGroupsUseCase"

/**
 * A use case that calculates the total number of unread messages across all groups for the current user.
 *
 * This use case orchestrates a multi-step, reactive data flow:
 * 1. It first fetches a real-time stream of all groups the user is a member of.
 * 2. Using `flatMapLatest`, it transforms the list of groups into a new flow. If the user joins or leaves a group, this ensures the entire operation restarts with the new list.
 * 3. For each group, it requests a separate, real-time flow that tracks the unread message count for that specific group.
 * 4. It then uses `combine` to merge the integer results from all individual group counters into a single integer: the total sum.
 *
 * This architectural pattern makes the system highly efficient and robust. The UI layer only needs to observe one final `Flow<Int>`,
 * and any change—from a new message in any group to the user joining a new group—will automatically trigger a recalculation and update the UI.
 *
 * @property groupChatRepository The repository providing access to group data primitives, such as the list of user groups and the unread count for a single group.
 * @property auth The authentication service to identify the current user.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetUnreadGroupMessagesCountUseCase @Inject constructor(
    private val groupChatRepository: GroupChatRepository,
    private val auth: FirebaseAuth
) {

    /**
     * Executes the use case.
     *
     * @return A [Flow] that emits the total count of unread messages across all of the user's groups.
     *         Emits 0 if the user is not authenticated or has no groups.
     */
    operator fun invoke(): Flow<Int> {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrEmpty()) {
            Log.w(TAG, "Current user is not authenticated. Returning a flow of 0.")
            return flowOf(0)
        }

        // Step 1: Get the stream of all groups the user belongs to.
        return groupChatRepository.getUserGroups(currentUserId)
            .flatMapLatest { groups ->
                if (groups.isEmpty()) {
                    // If the user has no groups, the total count is always 0.
                    Log.d(TAG, "User has no groups. Emitting 0.")
                    flowOf(0)
                } else {
                    // Step 2: For each group, create a flow that provides its individual unread count.
                    val unreadCountFlows: List<Flow<Int>> = groups.map { group ->
                        groupChatRepository.getUnreadMessagesCountForGroup(group.id, currentUserId)
                    }

                    // Step 3: Combine the results of all individual flows.
                    // The 'combine' operator takes all the flows and provides their latest values as an array.
                    Log.d(TAG, "Combining unread counts for ${unreadCountFlows.size} groups.")
                    combine(unreadCountFlows) { countsArray ->
                        // Sum up all the integers in the array to get the final total.
                        countsArray.sum()
                    }
                }
            }
    }
}


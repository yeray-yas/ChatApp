package com.yerayyas.chatappkotlinproject.domain.usecases.group

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * Use case for getting the current user's groups
 */
class GetUserGroupsUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Gets all groups of the current user as Flow
     */
    fun execute(): Flow<List<GroupChat>> {
        val currentUserId = firebaseAuth.currentUser?.uid
        return if (currentUserId != null) {
            groupRepository.getUserGroups(currentUserId)
        } else {
            emptyFlow()
        }
    }

    /**
     * Gets active groups of the user
     */
    fun getActiveGroups(): Flow<List<GroupChat>> {
        val currentUserId = firebaseAuth.currentUser?.uid
        return if (currentUserId != null) {
            groupRepository.getUserGroups(currentUserId)
        } else {
            emptyFlow()
        }
    }

    /**
     * Gets a specific group by ID
     */
    suspend fun getGroupById(groupId: String): GroupChat? {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid ?: return null
            val group = groupRepository.getGroupById(groupId)

            // Verify that the user is a member of the group
            if (group?.isMember(currentUserId) == true) {
                group
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the user is a member of a specific group
     */
    suspend fun isUserMemberOfGroup(groupId: String): Boolean {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid ?: return false
            val group = groupRepository.getGroupById(groupId)
            group?.isMember(currentUserId) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the user is an administrator of a specific group
     */
    suspend fun isUserAdminOfGroup(groupId: String): Boolean {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid ?: return false
            val group = groupRepository.getGroupById(groupId)
            group?.isAdmin(currentUserId) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets basic statistics of the user's groups
     */
    suspend fun getUserGroupStats(): GroupStats {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid ?: return GroupStats()

            // This would be a simplified implementation
            // In a real app, you could have specific endpoints for statistics
            GroupStats(
                totalGroups = 0, // Would be calculated from the Flow
                adminGroups = 0,
                activeGroups = 0,
                unreadMessages = 0
            )
        } catch (e: Exception) {
            GroupStats()
        }
    }
}

/**
 * Data class for user's group statistics
 */
data class GroupStats(
    val totalGroups: Int = 0,
    val adminGroups: Int = 0,
    val activeGroups: Int = 0,
    val unreadMessages: Int = 0
)
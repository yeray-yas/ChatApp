package com.yerayyas.chatappkotlinproject.domain.usecases.chat.group

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupActivityType
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Use case for managing group members
 */
class ManageGroupMembersUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth,
    private val sendGroupMessageUseCase: SendGroupMessageUseCase
) {
    /**
     * Adds a member to the group
     */
    suspend fun addMember(
        groupId: String,
        userId: String,
        userName: String,
        addedByName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Verify that the current user can add members
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Group not found"))

            if (!group.canAddMembers(currentUserId)) {
                return Result.failure(Exception("You don't have permission to add members"))
            }

            // Add the member
            val result = groupRepository.addMemberToGroup(groupId, userId)

            if (result.isSuccess) {
                // Send notification message
                sendGroupMessageUseCase.sendSystemMessage(
                    groupId = groupId,
                    message = "$addedByName added $userName to the group",
                    systemMessageType = GroupActivityType.USER_ADDED
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a member from the group
     */
    suspend fun removeMember(
        groupId: String,
        userId: String,
        userName: String,
        removedByName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Verify permissions
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Group not found"))

            if (!group.isAdmin(currentUserId) && currentUserId != userId) {
                return Result.failure(Exception("You don't have permission to remove this member"))
            }

            // Don't allow removing the group creator
            if (userId == group.createdBy) {
                return Result.failure(Exception("Cannot remove the group creator"))
            }

            // Remove the member
            val result = groupRepository.removeMemberFromGroup(groupId, userId)

            if (result.isSuccess) {
                // Send notification message
                val message = if (currentUserId == userId) {
                    "$userName left the group"
                } else {
                    "$removedByName removed $userName from the group"
                }

                sendGroupMessageUseCase.sendSystemMessage(
                    groupId = groupId,
                    message = message,
                    systemMessageType = if (currentUserId == userId)
                        GroupActivityType.USER_LEFT else GroupActivityType.USER_REMOVED
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Promotes a member to administrator
     */
    suspend fun promoteToAdmin(
        groupId: String,
        userId: String,
        userName: String,
        promotedByName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Verify that the current user is admin
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Group not found"))

            if (!group.isAdmin(currentUserId)) {
                return Result.failure(Exception("Only administrators can promote other members"))
            }

            if (group.isAdmin(userId)) {
                return Result.failure(Exception("User is already an administrator"))
            }

            // Promote to admin
            val result = groupRepository.makeAdmin(groupId, userId)

            if (result.isSuccess) {
                // Send notification message
                sendGroupMessageUseCase.sendSystemMessage(
                    groupId = groupId,
                    message = "$promotedByName made $userName an administrator",
                    systemMessageType = GroupActivityType.ADMIN_ADDED
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes administrator privileges from a member
     */
    suspend fun demoteAdmin(
        groupId: String,
        userId: String,
        userName: String,
        demotedByName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Verify permissions
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Group not found"))

            if (!group.isAdmin(currentUserId)) {
                return Result.failure(Exception("Only administrators can remove privileges"))
            }

            if (!group.isAdmin(userId)) {
                return Result.failure(Exception("User is not an administrator"))
            }

            // Don't allow demoting the group creator
            if (userId == group.createdBy) {
                return Result.failure(Exception("Cannot remove privileges from the group creator"))
            }

            // Remove admin privileges
            val result = groupRepository.removeAdmin(groupId, userId)

            if (result.isSuccess) {
                // Send notification message
                sendGroupMessageUseCase.sendSystemMessage(
                    groupId = groupId,
                    message = "$demotedByName removed administrator privileges from $userName",
                    systemMessageType = GroupActivityType.ADMIN_REMOVED
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leaves the group (current user)
     */
    suspend fun leaveGroup(
        groupId: String,
        userName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))

            // Verify that user is not the creator
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Group not found"))

            if (currentUserId == group.createdBy) {
                return Result.failure(Exception("The group creator cannot leave. Must transfer ownership first."))
            }

            // Leave the group
            removeMember(
                groupId = groupId,
                userId = currentUserId,
                userName = userName,
                removedByName = userName
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the list of group members
     */
    suspend fun getGroupMembers(groupId: String): List<String> {
        return try {
            groupRepository.getGroupMembers(groupId)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
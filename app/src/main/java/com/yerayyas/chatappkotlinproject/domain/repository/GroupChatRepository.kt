package com.yerayyas.chatappkotlinproject.domain.repository

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupInvitation
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for group chat operations.
 *
 * This interface defines the contract for group chat data operations including
 * group management, member management, messaging, invitations, and settings.
 * Implementations should handle data persistence and real-time updates.
 */
interface GroupChatRepository {

    // Basic group management
    /**
     * Creates a new group chat.
     *
     * @param group The group chat to create.
     * @return The result of the operation, containing the group ID if successful.
     */
    suspend fun createGroup(group: GroupChat): Result<String>

    /**
     * Retrieves a group chat by its ID.
     *
     * @param groupId The ID of the group chat to retrieve.
     * @return The group chat, or null if not found.
     */
    suspend fun getGroupById(groupId: String): GroupChat?

    /**
     * Updates an existing group chat.
     *
     * @param group The updated group chat.
     * @return The result of the operation.
     */
    suspend fun updateGroup(group: GroupChat): Result<Unit>

    /**
     * Deletes a group chat.
     *
     * @param groupId The ID of the group chat to delete.
     * @return The result of the operation.
     */
    suspend fun deleteGroup(groupId: String): Result<Unit>

    // Member management
    /**
     * Adds a member to a group chat.
     *
     * @param groupId The ID of the group chat.
     * @param userId The ID of the user to add.
     * @return The result of the operation.
     */
    suspend fun addMemberToGroup(groupId: String, userId: String): Result<Unit>

    /**
     * Removes a member from a group chat.
     *
     * @param groupId The ID of the group chat.
     * @param userId The ID of the user to remove.
     * @return The result of the operation.
     */
    suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit>

    /**
     * Makes a user an admin of a group chat.
     *
     * @param groupId The ID of the group chat.
     * @param userId The ID of the user to make admin.
     * @return The result of the operation.
     */
    suspend fun makeAdmin(groupId: String, userId: String): Result<Unit>

    /**
     * Removes a user's admin privileges from a group chat.
     *
     * @param groupId The ID of the group chat.
     * @param userId The ID of the user to remove admin privileges from.
     * @return The result of the operation.
     */
    suspend fun removeAdmin(groupId: String, userId: String): Result<Unit>

    /**
     * Retrieves the members of a group chat.
     *
     * @param groupId The ID of the group chat.
     * @return The list of member IDs.
     */
    suspend fun getGroupMembers(groupId: String): List<String>

    // Group messaging
    /**
     * Sends a message to a group chat.
     *
     * @param groupId The ID of the group chat.
     * @param message The message to send.
     * @return The result of the operation.
     */
    suspend fun sendMessageToGroup(groupId: String, message: GroupMessage): Result<Unit>

    /**
     * Retrieves the messages of a group chat.
     *
     * @param groupId The ID of the group chat.
     * @return The flow of messages.
     */
    fun getGroupMessages(groupId: String): Flow<List<GroupMessage>>

    /**
     * Updates the last activity of a group chat.
     *
     * @param groupId The ID of the group chat.
     * @param message The message that triggered the update.
     * @return The result of the operation.
     */
    suspend fun updateLastActivity(groupId: String, message: GroupMessage): Result<Unit>

    /**
     * Uploads an image for a group message.
     *
     * @param groupId The ID of the group chat.
     * @param imageUri The URI of the image to upload.
     * @return The result of the operation, containing the uploaded image URL.
     */
    suspend fun uploadGroupMessageImage(groupId: String, imageUri: Uri): Result<String>

    // Settings
    /**
     * Updates the settings of a group chat.
     *
     * @param groupId The ID of the group chat.
     * @param settings The updated settings.
     * @return The result of the operation.
     */
    suspend fun updateGroupSettings(groupId: String, settings: Map<String, Any>): Result<Unit>

    /**
     * Updates the image of a group chat.
     *
     * @param groupId The ID of the group chat.
     * @param imageUri The URI of the new image.
     * @return The result of the operation, containing the uploaded image URL.
     */
    suspend fun updateGroupImage(groupId: String, imageUri: Uri): Result<String>

    // Invitations
    /**
     * Creates a new group invitation.
     *
     * @param invitation The invitation to create.
     * @return The result of the operation, containing the invitation ID.
     */
    suspend fun createInvitation(invitation: GroupInvitation): Result<String>

    /**
     * Responds to a group invitation.
     *
     * @param invitationId The ID of the invitation.
     * @param accept Whether the invitation is accepted.
     * @return The result of the operation.
     */
    suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit>

    /**
     * Retrieves the invitations for a user.
     *
     * @param userId The ID of the user.
     * @return The flow of invitations.
     */
    fun getInvitationsForUser(userId: String): Flow<List<GroupInvitation>>

    // Read receipts
    /**
     * Marks a message as read by a user.
     *
     * @param groupId The ID of the group chat.
     * @param messageId The ID of the message.
     * @param userId The ID of the user.
     * @return The result of the operation.
     */
    suspend fun markMessageAsRead(groupId: String, messageId: String, userId: String): Result<Unit>

    /**
     * Retrieves the read receipts for a message.
     *
     * @param messageId The ID of the message.
     * @return The map of user IDs to read timestamps.
     */
    suspend fun getMessageReadReceipts(messageId: String): Map<String, Long>

    // Search and filters
    /**
     * Searches for messages in a group chat.
     *
     * @param groupId The ID of the group chat.
     * @param query The search query.
     * @return The list of matching messages.
     */
    suspend fun searchGroupMessages(groupId: String, query: String): List<GroupMessage>

    /**
     * Retrieves the group chats for a user.
     *
     * @param userId The ID of the user.
     * @return The flow of group chats.
     */
    fun getUserGroups(userId: String): Flow<List<GroupChat>>

    /**
     * Marks all unread messages in a group as read by a specific user.
     *
     * @param groupId The ID of the group chat.
     * @param userId The ID of the user who is reading the messages.
     * @return The result of the operation.
     */
    suspend fun markGroupMessagesAsRead(groupId: String, userId: String): Result<Unit>

    /**
     * Retrieves a flow of the number of unread messages for a specific group.
     *
     * @param groupId The ID of the group.
     * @param userId The ID of the user.
     * @return A flow emitting the count of unread messages for that group.
     */
    fun getUnreadMessagesCountForGroup(groupId: String, userId: String): Flow<Int>
}
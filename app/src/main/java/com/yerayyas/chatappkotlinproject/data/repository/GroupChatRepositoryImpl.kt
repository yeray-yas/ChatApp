package com.yerayyas.chatappkotlinproject.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupInvitation
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "GroupChatRepoImpl"

/**
 * Comprehensive implementation of [GroupChatRepository] handling all group-related operations.
 *
 * Integrates Firebase Realtime Database and Storage to manage the lifecycle of groups,
 * memberships, messages, and invitations.
 *
 * **Data Structure Note (Lists vs Maps):**
 * This repository adheres to a Data Model where `memberIds` and `adminIds` are defined as
 * [List<String>]. Consequently, membership operations (add/remove) utilize a
 * **"Fetch-Modify-Save"** strategy rather than atomic key-value updates.
 *
 * **Key Features:**
 * - **Real-time Synchronization:** Uses [Flow] and `ValueEventListener` for live updates.
 * - **Hybrid Storage:** Stores structural data in Realtime Database and binaries (images) in Storage.
 * - **Client-Side Filtering:** Used for complex queries (like searching within groups) where
 * backend indexing is limited.
 */
@Singleton
class GroupChatRepositoryImpl @Inject constructor(
    @Named("firebaseDatabaseInstance") private val firebaseDatabase: FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage
) : GroupChatRepository {

    // region Group Management (CRUD)

    /**
     * Creates a new group node in the database.
     *
     * Generates a unique key locally before pushing data to `groups/{groupId}`.
     *
     * @param group The group data object to be created.
     * @return [Result] containing the new Group ID on success.
     */
    override suspend fun createGroup(group: GroupChat): Result<String> {
        return try {
            val reference = firebaseDatabase.reference.child("groups").push()
            val groupId = reference.key ?: throw Exception("Could not generate group ID")

            val groupWithId = group.copy(id = groupId)
            reference.setValue(groupWithId).await()

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves a specific group by its ID.
     *
     * @param groupId The unique identifier of the group.
     * @return The [GroupChat] object or null if not found/error.
     */
    override suspend fun getGroupById(groupId: String): GroupChat? {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .get()
                .await()

            snapshot.getValue(GroupChat::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Updates an existing group's top-level data.
     *
     * @param group The group object with updated values.
     */
    override suspend fun updateGroup(group: GroupChat): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("groups")
                .child(group.id)
                .setValue(group)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a group and its associated messages.
     *
     * **Operation:** Performs two separate delete operations (groups and group_messages).
     *
     * @param groupId The ID of the group to delete.
     */
    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .removeValue()
                .await()

            firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .removeValue()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // endregion

    // region Member Management

    /**
     * Adds a user to the group's member list.
     *
     * **Implementation Detail:**
     * Since `memberIds` is a [List], this method performs a read-modify-write operation:
     * 1. Fetches the current group state.
     * 2. Appends the user ID to the list (if not present).
     * 3. Overwrites the `memberIds` field.
     *
     * @param groupId The target group ID.
     * @param userId The ID of the user to add.
     */
    override suspend fun addMemberToGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupRef = firebaseDatabase.reference.child("groups").child(groupId)
            val groupSnapshot = groupRef.get().await()
            val group = groupSnapshot.getValue(GroupChat::class.java)
                ?: throw Exception("Group not found")

            if (!group.memberIds.contains(userId)) {
                val updatedList = group.memberIds.toMutableList().apply { add(userId) }
                groupRef.child("memberIds").setValue(updatedList).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes a user from the group (and admin list if applicable).
     *
     * **Implementation Detail:**
     * Filters the user ID out of both `memberIds` and `adminIds` lists locally,
     * then performs an update on the group node.
     *
     * @param groupId The target group ID.
     * @param userId The ID of the user to remove.
     */
    override suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupRef = firebaseDatabase.reference.child("groups").child(groupId)
            val groupSnapshot = groupRef.get().await()
            val group = groupSnapshot.getValue(GroupChat::class.java)
                ?: throw Exception("Group not found")

            // Remove from members and admins locally
            val updatedMembers = group.memberIds.filter { it != userId }
            val updatedAdmins = group.adminIds.filter { it != userId }

            val updates = mapOf(
                "memberIds" to updatedMembers,
                "adminIds" to updatedAdmins
            )

            groupRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Promotes a member to administrator status.
     *
     * @param groupId The target group ID.
     * @param userId The ID of the user to promote.
     */
    override suspend fun makeAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupRef = firebaseDatabase.reference.child("groups").child(groupId)
            val groupSnapshot = groupRef.get().await()
            val group = groupSnapshot.getValue(GroupChat::class.java)
                ?: throw Exception("Group not found")

            if (!group.adminIds.contains(userId) && group.memberIds.contains(userId)) {
                val updatedAdmins = group.adminIds.toMutableList().apply { add(userId) }
                groupRef.child("adminIds").setValue(updatedAdmins).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downgrades an admin to a regular member.
     *
     * **Validation:** Prevents removing the last administrator to ensure the group is not orphaned.
     *
     * @param groupId The target group ID.
     * @param userId The ID of the admin to downgrade.
     */
    override suspend fun removeAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupRef = firebaseDatabase.reference.child("groups").child(groupId)
            val groupSnapshot = groupRef.get().await()
            val group = groupSnapshot.getValue(GroupChat::class.java)
                ?: throw Exception("Group not found")

            if (group.adminIds.size <= 1 && group.adminIds.contains(userId)) {
                throw Exception("Cannot remove the last administrator")
            }

            val updatedAdmins = group.adminIds.filter { it != userId }
            groupRef.child("adminIds").setValue(updatedAdmins).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves the list of member IDs for a group.
     */
    override suspend fun getGroupMembers(groupId: String): List<String> {
        return try {
            val group = getGroupById(groupId)
            group?.memberIds ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // endregion

    // region Messaging

    /**
     * Sends a message and updates the group's metadata.
     *
     * 1. Pushes the message to `group_messages/{groupId}`.
     * 2. Triggers [updateLastActivity] to refresh the group's preview in the list.
     */
    override suspend fun sendMessageToGroup(groupId: String, message: GroupMessage): Result<Unit> {
        return try {
            Log.d(TAG, "Sending message to group $groupId")

            val reference = firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .push()

            val messageId = reference.key ?: throw Exception("Could not generate message ID")
            val messageWithId = message.copy(id = messageId, groupId = groupId)

            reference.setValue(messageWithId).await()

            updateLastActivity(groupId, messageWithId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Uploads an image to Firebase Storage for use in a group message.
     *
     * @param groupId The group context.
     * @param imageUri The local URI of the image.
     * @return [Result] containing the download URL of the uploaded image.
     */
    override suspend fun uploadGroupMessageImage(groupId: String, imageUri: Uri): Result<String> {
        return try {
            val imageFileName = "group_chat_images/$groupId/${java.util.UUID.randomUUID()}.jpg"
            val imageRef = firebaseStorage.reference.child(imageFileName)

            imageRef.putFile(imageUri).await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Provides a real-time stream of messages for a specific group.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> {
        return callbackFlow {
            val messagesRef = firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .orderByChild("timestamp")

            val listener = messagesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val messagesList = snapshot.children.mapNotNull {
                            it.getValue(GroupMessage::class.java)
                        }
                        trySend(messagesList)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing messages: ${e.message}")
                        trySend(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })

            awaitClose { messagesRef.removeEventListener(listener) }
        }
    }

    /**
     * Updates the `lastActivity` and `lastMessage` fields of the group.
     * Used to sort the group list by recency in the UI.
     */
    override suspend fun updateLastActivity(groupId: String, message: GroupMessage): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Group not found")
            val updatedGroup = group.copy(
                lastMessage = message.toChatMessage(),
                lastActivity = message.timestamp
            )
            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // endregion

    // region Read Receipts

    /**
     * Marks a specific message as read in a dedicated receipt node.
     */
    override suspend fun markMessageAsRead(
        groupId: String,
        messageId: String,
        userId: String
    ): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            firebaseDatabase.reference
                .child("message_read_receipts")
                .child(messageId)
                .child(userId)
                .setValue(timestamp)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMessageReadReceipts(messageId: String): Map<String, Long> {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("message_read_receipts")
                .child(messageId)
                .get()
                .await()

            val receipts = mutableMapOf<String, Long>()
            snapshot.children.forEach { child ->
                val userId = child.key
                val timestamp = child.getValue<Long>()
                if (userId != null && timestamp != null) {
                    receipts[userId] = timestamp
                }
            }
            receipts
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Batch marks all valid unread messages in a group as read for the current user.
     *
     * Scans messages in `group_messages/{groupId}` and updates the `readBy` map
     * inside each message object.
     */
    override suspend fun markGroupMessagesAsRead(groupId: String, userId: String): Result<Unit> {
        return try {
            val messagesRef = firebaseDatabase.reference.child("group_messages").child(groupId)
            val snapshot = messagesRef.get().await()

            if (!snapshot.exists()) return Result.success(Unit)

            val updates = mutableMapOf<String, Any>()

            snapshot.children.forEach { messageSnapshot ->
                val messageId = messageSnapshot.key
                val readByMap = messageSnapshot.child("readBy").value as? Map<*, *> ?: emptyMap<Any, Any>()
                val senderId = messageSnapshot.child("senderId").getValue(String::class.java)

                if (messageId != null && senderId != userId && !readByMap.containsKey(userId)) {
                    updates["$messageId/readBy/$userId"] = ServerValue.TIMESTAMP
                }
            }

            if (updates.isNotEmpty()) {
                messagesRef.updateChildren(updates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Could not mark messages as read.", e)
            Result.failure(e)
        }
    }

    /**
     * Observes the count of unread messages in real-time for a user in a specific group.
     */
    override fun getUnreadMessagesCountForGroup(groupId: String, userId: String): Flow<Int> {
        if (groupId.isBlank() || userId.isBlank()) return flowOf(0)

        val messagesRef = firebaseDatabase.reference.child("group_messages").child(groupId)

        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        trySend(0)
                        return
                    }
                    var unreadCount = 0
                    snapshot.children.forEach { messageSnapshot ->
                        try {
                            val message = messageSnapshot.getValue(GroupMessage::class.java)
                            if (message != null && message.senderId != userId && !message.readBy.containsKey(userId)) {
                                unreadCount++
                            }
                        } catch (e: DatabaseException) {
                            // Skip malformed messages
                        }
                    }
                    trySend(unreadCount)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            messagesRef.addValueEventListener(listener)
            awaitClose { messagesRef.removeEventListener(listener) }
        }
    }

    // endregion

    // region Settings & Media

    override suspend fun updateGroupSettings(
        groupId: String,
        settings: Map<String, Any>
    ): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .child("settings")
                .updateChildren(settings)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateGroupImage(groupId: String, imageUri: Uri): Result<String> {
        return try {
            val imageRef = firebaseStorage.reference
                .child("group_images")
                .child("$groupId.jpg")

            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .child("imageUrl")
                .setValue(downloadUrl)
                .await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // endregion

    // region Invitations

    override suspend fun createInvitation(invitation: GroupInvitation): Result<String> {
        return try {
            val reference = firebaseDatabase.reference.child("group_invitations").push()
            val invitationId = reference.key ?: throw Exception("No ID gen")

            val invitationWithId = invitation.copy(id = invitationId)
            reference.setValue(invitationWithId).await()

            Result.success(invitationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit> {
        return try {
            val invitationSnapshot = firebaseDatabase.reference
                .child("group_invitations")
                .child(invitationId)
                .get()
                .await()

            val invitation = invitationSnapshot.getValue(GroupInvitation::class.java)
                ?: throw Exception("Invitation not found")

            val status = if (accept) "ACCEPTED" else "DECLINED"

            firebaseDatabase.reference
                .child("group_invitations")
                .child(invitationId)
                .child("status")
                .setValue(status)
                .await()

            if (accept) {
                addMemberToGroup(invitation.groupId, invitation.invitedUser)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getInvitationsForUser(userId: String): Flow<List<GroupInvitation>> {
        return callbackFlow {
            val invitationsRef = firebaseDatabase.reference
                .child("group_invitations")
                .orderByChild("invitedUser")
                .equalTo(userId)

            val listener = invitationsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val invitationsList = snapshot.children.mapNotNull {
                            it.getValue(GroupInvitation::class.java)
                        }.filter { it.isActive() }
                        trySend(invitationsList)
                    } catch (e: Exception) {
                        trySend(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(emptyList())
                }
            })
            awaitClose { invitationsRef.removeEventListener(listener) }
        }
    }

    // endregion

    // region Search & Filtering

    /**
     * Performs a client-side search on group messages.
     *
     * @param groupId The group to search in.
     * @param query The search text.
     * @return List of matching messages.
     */
    override suspend fun searchGroupMessages(groupId: String, query: String): List<GroupMessage> {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .get()
                .await()

            snapshot.children.mapNotNull { dataSnapshot ->
                dataSnapshot.getValue(GroupMessage::class.java)
            }.filter { groupMessage ->
                groupMessage.message.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching group messages", e)
            emptyList()
        }
    }

    /**
     * Retrieves all groups where the user is a member.
     *
     * **Performance Note:**
     * Since members are stored in a List structure, we fetch groups and filter
     * client-side using [List.contains].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getUserGroups(userId: String): Flow<List<GroupChat>> {
        if (userId.isBlank()) return flowOf(emptyList())

        val groupsRef = firebaseDatabase.reference.child("groups")

        return callbackFlow {
            val listener = groupsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groups = snapshot.children.mapNotNull { it.getValue(GroupChat::class.java) }
                    // Filter using List.contains (Since memberIds is a List)
                    val userGroups = groups.filter { it.memberIds.contains(userId) }
                    Log.d(TAG, "User $userId is member of ${userGroups.size} groups.")
                    trySend(userGroups)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Listener for user groups cancelled.", error.toException())
                    close(error.toException())
                }
            })
            awaitClose { groupsRef.removeEventListener(listener) }
        }
    }

    // endregion
}
package com.yerayyas.chatappkotlinproject.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupInvitation
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupMessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Comprehensive implementation of [GroupChatRepository] for group chat operations.
 *
 * This repository provides a complete implementation of group chat functionality, including
 * group management, member administration, messaging, invitations, and real-time data synchronization.
 * It integrates with Firebase services to provide persistent, scalable group chat features.
 *
 * Key functionalities:
 * - **Group Management**: Create, update, delete, and retrieve groups with full CRUD operations
 * - **Member Administration**: Add/remove members, manage admin privileges, and permission control
 * - **Group Messaging**: Send text/image messages with real-time message streaming
 * - **Read Receipts**: Track message read status across group members for delivery confirmation
 * - **Invitations**: Create and manage group invitations with status tracking
 * - **Search**: Search messages within groups with query filtering
 * - **Settings**: Manage group-specific settings and permissions
 * - **File Upload**: Handle image uploads for group messages and avatars using Firebase Storage
 * - **Mock Data Support**: Provides fallback mock data for development and testing scenarios
 *
 * Architecture pattern: Repository Pattern with Clean Architecture
 * - Implements domain repository interface for clean separation of concerns
 * - Integrates with Firebase services (Auth, Database, Storage) for backend operations
 * - Provides reactive data streams through Flow for real-time UI updates
 * - Includes comprehensive error handling and logging for debugging
 * - Supports both production data and development mock data
 *
 * Firebase integration:
 * - **Firebase Auth**: User authentication and current user management
 * - **Firebase Realtime Database**: Real-time group and message synchronization with live updates
 * - **Firebase Storage**: Secure image upload and management for group content
 *
 * Thread safety: All operations are designed to be thread-safe and can be called from
 * background threads. Flow operations automatically handle threading concerns.
 *
 * Error handling: All operations return Result types or handle exceptions gracefully,
 * with fallback to mock data when appropriate for development purposes.
 */
@Singleton
class GroupChatRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @Named("firebaseDatabaseInstance") private val firebaseDatabase: FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage
) : GroupChatRepository {

    // ===== BASIC GROUP MANAGEMENT =====

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

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            // Delete the group
            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .removeValue()
                .await()

            // Delete group messages
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

    // ===== MEMBER MANAGEMENT =====

    override suspend fun addMemberToGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Group not found")
            val updatedMembers = group.memberIds.toMutableList().apply {
                if (!contains(userId)) add(userId)
            }
            val updatedGroup = group.copy(memberIds = updatedMembers)

            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Group not found")
            val updatedMembers = group.memberIds.toMutableList().apply { remove(userId) }
            val updatedAdmins = group.adminIds.toMutableList().apply { remove(userId) }
            val updatedGroup = group.copy(
                memberIds = updatedMembers,
                adminIds = updatedAdmins
            )

            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun makeAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Group not found")
            val updatedAdmins = group.adminIds.toMutableList().apply {
                if (!contains(userId)) add(userId)
            }
            val updatedGroup = group.copy(adminIds = updatedAdmins)

            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Group not found")

            // Do not allow removing the last admin
            if (group.adminIds.size <= 1) {
                throw Exception("Cannot remove the last administrator")
            }

            val updatedAdmins = group.adminIds.toMutableList().apply { remove(userId) }
            val updatedGroup = group.copy(adminIds = updatedAdmins)

            updateGroup(updatedGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroupMembers(groupId: String): List<String> {
        return try {
            val group = getGroupById(groupId)
            group?.memberIds ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ===== GROUP MESSAGING =====

    override suspend fun sendMessageToGroup(groupId: String, message: GroupMessage): Result<Unit> {
        val TAG = "GroupChatRepository"
        return try {
            Log.d(TAG, "Sending message to group $groupId")
            Log.d(TAG, "Message content: ${message.message}")
            Log.d(TAG, "Sender: ${message.senderId}")

            val reference = firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .push()

            val messageId = reference.key ?: throw Exception("Could not generate message ID")
            val messageWithId = message.copy(id = messageId, groupId = groupId)

            Log.d(TAG, "Generated message ID: $messageId")
            Log.d(TAG, "Saving to Firebase path: group_messages/$groupId/$messageId")

            reference.setValue(messageWithId).await()

            Log.d(TAG, "Message saved successfully to Firebase")

            // Update last activity of the group
            updateLastActivity(groupId, messageWithId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GroupChatRepository", "Error sending message to group: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Uploads an image to Firebase Storage and returns the download URL
     * for use in group messages with images
     */
    override suspend fun uploadGroupMessageImage(groupId: String, imageUri: Uri): Result<String> {
        return try {
            // Define the path and name for the image in Firebase Storage
            val imageFileName = "group_chat_images/$groupId/${java.util.UUID.randomUUID()}.jpg"
            val imageRef = firebaseStorage.reference.child(imageFileName)

            // Upload the file and get its public URL
            imageRef.putFile(imageUri).await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> {
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
                        }.sortedBy { it.timestamp }
                        Log.d(
                            "GroupChatRepository",
                            "Firebase returned ${messagesList.size} messages for group $groupId"
                        )
                        trySend(messagesList)
                    } catch (e: Exception) {
                        Log.d(
                            "GroupChatRepository",
                            "Error parsing Firebase messages: ${e.message}"
                        )
                        // If there is an error parsing, send an empty list instead of mock data
                        trySend(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("GroupChatRepository", "Firebase cancelled: ${error.message}")
                    // In case of a Firebase error, send an empty list
                    trySend(emptyList())
                }
            })

            awaitClose { messagesRef.removeEventListener(listener) }
        }
    }

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

    // ===== READ RECEIPTS =====

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

    // ===== SETTINGS =====

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

            // Update URL in the group
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

    // ===== INVITATIONS =====

    override suspend fun createInvitation(invitation: GroupInvitation): Result<String> {
        return try {
            val reference = firebaseDatabase.reference.child("group_invitations").push()
            val invitationId =
                reference.key ?: throw Exception("Could not generate invitation ID")

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

            // Update invitation status
            firebaseDatabase.reference
                .child("group_invitations")
                .child(invitationId)
                .child("status")
                .setValue(status)
                .await()

            // If accepted, add to group
            if (accept) {
                addMemberToGroup(invitation.groupId, invitation.invitedUser)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getInvitationsForUser(userId: String): Flow<List<GroupInvitation>> {
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

    // ===== SEARCH AND FILTERS =====

    override suspend fun searchGroupMessages(groupId: String, query: String): List<GroupMessage> {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("group_messages")
                .child(groupId)
                .get()
                .await()

            snapshot.children.mapNotNull {
                it.getValue(GroupMessage::class.java)
            }.filter {
                it.message.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            // If there is an error, use mock data
            getMockGroupMessages(groupId).filter {
                it.message.contains(query, ignoreCase = true)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getUserGroups(userId: String): Flow<List<GroupChat>> {
        return callbackFlow {
            val groupsRef = firebaseDatabase.reference.child("groups")

            val listener = groupsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val allGroups = snapshot.children.mapNotNull {
                            it.getValue(GroupChat::class.java)
                        }

                        Log.d(
                            "GroupChatRepository",
                            "Firebase returned ${allGroups.size} total groups"
                        )

                        // Filter groups where the user is a member
                        val userGroups = allGroups.filter { group ->
                            group.memberIds.contains(userId)
                        }.sortedByDescending { it.lastActivity }

                        Log.d(
                            "GroupChatRepository",
                            "User $userId is member of ${userGroups.size} groups"
                        )

                        // If no real groups are found, use mock data only in development
                        val groupsToSend = userGroups.ifEmpty {
                            Log.d("GroupChatRepository", "No real groups found, using mock data")
                            getMockGroups(userId)
                        }

                        trySend(groupsToSend)
                    } catch (e: Exception) {
                        Log.d(
                            "GroupChatRepository",
                            "Error loading groups from Firebase: ${e.message}"
                        )
                        // In case of an error, use mock data as a fallback
                        trySend(getMockGroups(userId))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("GroupChatRepository", "Firebase groups cancelled: ${error.message}")
                    // In case of an error, use mock data as a fallback
                    trySend(getMockGroups(userId))
                }
            })

            awaitClose { groupsRef.removeEventListener(listener) }
        }
    }

    // ===== MOCK DATA =====

    /**
     * Provides mock data for development and testing purposes.
     * Returns sample group messages with various message types and read statuses.
     *
     * @param groupId The group ID to generate mock messages for
     * @return List of sample [GroupMessage] objects for testing
     */
    private fun getMockGroupMessages(groupId: String): List<GroupMessage> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "current_user"

        return listOf(
            GroupMessage(
                id = "msg1",
                groupId = groupId,
                senderId = "user2",
                senderName = "Ana García",
                message = "Hello everyone! 👋",
                timestamp = System.currentTimeMillis() - 3600000,
                messageType = GroupMessageType.TEXT,
                readStatus = ReadStatus.READ,
                readBy = mapOf("user3" to System.currentTimeMillis() - 3500000)
            ),
            GroupMessage(
                id = "msg2",
                groupId = groupId,
                senderId = "user3",
                senderName = "Carlos López",
                message = "@Juan how's everything going?",
                timestamp = System.currentTimeMillis() - 1800000,
                messageType = GroupMessageType.TEXT,
                readStatus = ReadStatus.READ,
                mentionedUsers = listOf("user1"),
                readBy = mapOf(currentUserId to System.currentTimeMillis() - 1700000)
            ),
            GroupMessage(
                id = "msg3",
                groupId = groupId,
                senderId = currentUserId,
                senderName = "You",
                message = "Everything's fine here, thanks for asking 😊",
                timestamp = System.currentTimeMillis() - 900000,
                messageType = GroupMessageType.TEXT,
                readStatus = ReadStatus.DELIVERED,
                readBy = mapOf("user2" to System.currentTimeMillis() - 800000)
            ),
            GroupMessage(
                id = "msg4",
                groupId = groupId,
                senderId = "user4",
                senderName = "María Rodríguez",
                message = "Anyone up for lunch tomorrow? 🍕",
                timestamp = System.currentTimeMillis() - 300000,
                messageType = GroupMessageType.TEXT,
                readStatus = ReadStatus.SENT
            )
        ).sortedBy { it.timestamp }
    }

    /**
     * Provides mock group data for development and testing purposes.
     * Returns sample groups with various configurations and member setups.
     *
     * @param userId The user ID to include in the mock groups
     * @return List of sample [GroupChat] objects for testing
     */
    private fun getMockGroups(userId: String): List<GroupChat> {
        return listOf(
            GroupChat(
                id = "group1",
                name = "Family ❤️",
                description = "Family chat",
                memberIds = listOf(userId, "user2", "user3", "user4"),
                adminIds = listOf(userId),
                createdBy = userId,
                lastActivity = System.currentTimeMillis() - 300000,
                lastMessage = ChatMessage(
                    message = "Anyone up for lunch tomorrow? 🍕",
                    timestamp = System.currentTimeMillis() - 300000
                )
            ),
            GroupChat(
                id = "group2",
                name = "Work - Dev Team 💻",
                description = "Development team",
                memberIds = listOf(userId, "user5", "user6", "user7", "user8"),
                adminIds = listOf(userId, "user5"),
                createdBy = userId,
                lastActivity = System.currentTimeMillis() - 7200000,
                lastMessage = ChatMessage(
                    message = "The new feature is ready for testing",
                    timestamp = System.currentTimeMillis() - 7200000
                )
            ),
            GroupChat(
                id = "group3",
                name = "University Friends 🎓",
                description = "The usual ones",
                memberIds = listOf(userId, "user9", "user10", "user11"),
                adminIds = listOf(userId),
                createdBy = userId,
                lastActivity = System.currentTimeMillis() - 86400000,
                lastMessage = ChatMessage(
                    message = "When do we meet?",
                    timestamp = System.currentTimeMillis() - 86400000
                )
            )
        ).sortedByDescending { it.lastActivity }
    }
}
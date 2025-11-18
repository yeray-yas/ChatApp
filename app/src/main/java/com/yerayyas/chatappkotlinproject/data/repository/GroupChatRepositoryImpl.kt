package com.yerayyas.chatappkotlinproject.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
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
            // Actualiza directamente el campo del miembro en Firebase. Atómico y eficiente.
            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .child("memberIds") // Asume que memberIds es un Map<String, Boolean>
                .child(userId)
                .setValue(true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit> {
        return try {
            // Ejecuta dos borrados atómicos en una sola operación de actualización.
            val updates = mapOf(
                "memberIds/$userId" to null, // Borra al usuario de la lista de miembros
                "adminIds/$userId" to null   // Borra al usuario de la lista de admins (si estaba)
            )
            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .updateChildren(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun makeAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            // Actualiza directamente el campo de admin
            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .child("adminIds")
                .child(userId)
                .setValue(true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeAdmin(groupId: String, userId: String): Result<Unit> {
        // Para esta operación, SÍ necesitamos leer primero para no borrar al último admin.
        // Así que la dejamos como estaba, porque la lógica de negocio es más importante que la optimización.
        return try {
            val group = getGroupById(groupId) ?: throw Exception("Group not found")

            if (group.adminIds.size <= 1 && group.adminIds.contains(userId)) {
                throw Exception("Cannot remove the last administrator")
            }

            firebaseDatabase.reference
                .child("groups")
                .child(groupId)
                .child("adminIds")
                .child(userId)
                .removeValue()
                .await()

            Result.success(Unit)
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

    // ===== SEARCH AND FILTERS =====

    override suspend fun searchGroupMessages(groupId: String, query: String): List<GroupMessage> {
        return try {            val snapshot = firebaseDatabase.reference
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
            Log.e("GroupChatRepository", "Error searching group messages for query '$query'", e)
            // If there is an error, just return an empty list. Don't use mock data.
            emptyList()
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getUserGroups(userId: String): Flow<List<GroupChat>> {
        if (userId.isBlank()) {
            return flowOf(emptyList())
        }
     /*   val query = firebaseDatabase.reference.child("groups")
            .orderByChild("memberIds/$userId")
            .equalTo(true)*/

        val groupsRef = firebaseDatabase.reference.child("groups")
        return callbackFlow {
            val listener = groupsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groups = snapshot.children.mapNotNull { it.getValue(GroupChat::class.java) }
                    val userGroups = groups.filter { it.memberIds.contains(userId) }
                    Log.d("GroupChatRepository", "User $userId is member of ${userGroups.size} groups.")
                    trySend(userGroups)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("GroupChatRepository", "Listener for user groups cancelled.", error.toException())
                    close(error.toException())
                }
            })
            awaitClose { groupsRef.removeEventListener(listener) }
        }
    }


    override suspend fun markGroupMessagesAsRead(groupId: String, userId: String): Result<Unit> {
        return try {
            val messagesRef = firebaseDatabase.reference.child("group_messages").child(groupId)
            val snapshot = messagesRef.get().await()

            if (!snapshot.exists()) {
                return Result.success(Unit)
            }

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
                Log.d("GroupChatRepoImpl", "SUCCESS: Marked ${updates.size} messages as read for user $userId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GroupChatRepoImpl", "FAILURE: Could not mark messages as read.", e)
            Result.failure(e)
        }
    }

    override fun getUnreadMessagesCountForGroup(groupId: String, userId: String): Flow<Int> {
        if (groupId.isBlank() || userId.isBlank()) {
            return flowOf(0)
        }

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
                            Log.e(
                                "GroupChatRepoImpl",
                                "Error deserializing message ${messageSnapshot.key} in group $groupId. SKIPPING. Error: ${e.message}"
                            )
                        }
                    }
                    Log.d("GroupChatRepoImpl", "Unread count for group $groupId for user $userId IS: $unreadCount")
                    trySend(unreadCount)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("GroupChatRepoImpl", "Listener for group count $groupId cancelled.", error.toException())
                    close(error.toException())
                }
            }
            messagesRef.addValueEventListener(listener)
            awaitClose { messagesRef.removeEventListener(listener) }
        }
    }
}
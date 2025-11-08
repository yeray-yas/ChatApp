package com.yerayyas.chatappkotlinproject.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.yerayyas.chatappkotlinproject.data.model.*
import com.yerayyas.chatappkotlinproject.domain.repository.GroupAction
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.GroupStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Implementación de GroupChatRepository usando Firebase
 */
@Singleton
class GroupChatRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @Named("firebaseDatabaseInstance") private val firebaseDatabase: FirebaseDatabase,
    private val firebaseStorage: FirebaseStorage
) : GroupChatRepository {

    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    // ===== GESTIÓN DE GRUPOS =====

    override suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        imageUri: Uri?
    ): Result<String> {
        return try {
            val groupId = firebaseDatabase.reference.child("Groups").push().key
                ?: return Result.failure(Exception("No se pudo generar ID del grupo"))

            val allMembers = (memberIds + currentUserId).distinct()

            val group = GroupChat(
                id = groupId,
                name = name,
                description = description,
                memberIds = allMembers,
                adminIds = listOf(currentUserId), // El creador es admin inicial
                createdBy = currentUserId,
                createdAt = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis(),
                isActive = true
            )

            // Guardar grupo en Firebase
            firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .setValue(group)
                .await()

            // Crear actividad de creación
            val activity = GroupActivity(
                id = firebaseDatabase.reference.child("GroupActivities").child(groupId).push().key
                    ?: "",
                groupId = groupId,
                type = GroupActivityType.GROUP_CREATED,
                performedBy = currentUserId,
                timestamp = System.currentTimeMillis()
            )

            recordGroupActivity(activity)

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroup(groupId: String): Result<GroupChat> {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .get()
                .await()

            val group = snapshot.getValue(GroupChat::class.java)
                ?: return Result.failure(Exception("Grupo no encontrado"))

            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUserGroups(): Flow<List<GroupChat>> {
        // Por ahora retornamos grupos mock
        // En implementación real, se observaría Firebase
        return flowOf(getMockGroups())
    }

    override suspend fun updateGroupInfo(
        groupId: String,
        name: String?,
        description: String?,
        imageUri: Uri?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()

            name?.let { updates["name"] = it }
            description?.let { updates["description"] = it }

            if (updates.isNotEmpty()) {
                firebaseDatabase.reference
                    .child("Groups")
                    .child(groupId)
                    .updateChildren(updates)
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateGroupSettings(
        groupId: String,
        settings: GroupSettings
    ): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .child("settings")
                .setValue(settings)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .removeValue()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== GESTIÓN DE MIEMBROS =====

    override suspend fun getGroupMembers(groupId: String): Result<List<User>> {
        // Mock implementation
        return Result.success(getMockUsers())
    }

    override suspend fun addMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) return Result.failure(Exception("Grupo no encontrado"))

            val group = groupResult.getOrNull()!!
            val updatedMembers = (group.memberIds + userId).distinct()

            firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .child("memberIds")
                .setValue(updatedMembers)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) return Result.failure(Exception("Grupo no encontrado"))

            val group = groupResult.getOrNull()!!
            val updatedMembers = group.memberIds.filter { it != userId }

            val updates = mutableMapOf<String, Any>()
            updates["memberIds"] = updatedMembers

            // También remover de admins si era admin
            if (group.isAdmin(userId)) {
                val updatedAdmins = group.adminIds.filter { it != userId }
                updates["adminIds"] = updatedAdmins
            }

            firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .updateChildren(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(groupId: String): Result<Unit> {
        return removeMember(groupId, currentUserId)
    }

    override suspend fun promoteToAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) return Result.failure(Exception("Grupo no encontrado"))

            val group = groupResult.getOrNull()!!
            val updatedAdmins = (group.adminIds + userId).distinct()

            firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .child("adminIds")
                .setValue(updatedAdmins)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun demoteFromAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) return Result.failure(Exception("Grupo no encontrado"))

            val group = groupResult.getOrNull()!!
            val updatedAdmins = group.adminIds.filter { it != userId }

            firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .child("adminIds")
                .setValue(updatedAdmins)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun muteMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) return Result.failure(Exception("Grupo no encontrado"))

            val group = groupResult.getOrNull()!!
            val updatedMuted = (group.mutedMembers + userId).distinct()

            firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .child("mutedMembers")
                .setValue(updatedMuted)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unmuteMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) return Result.failure(Exception("Grupo no encontrado"))

            val group = groupResult.getOrNull()!!
            val updatedMuted = group.mutedMembers.filter { it != userId }

            firebaseDatabase.reference
                .child("Groups")
                .child(groupId)
                .child("mutedMembers")
                .setValue(updatedMuted)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== IMPLEMENTACIONES MOCK (Para funcionalidad básica) =====

    override suspend fun createInvitation(groupId: String, userId: String): Result<String> {
        // Mock implementation
        return Result.success("mock_invitation_id")
    }

    override fun getPendingInvitations(): Flow<List<GroupInvitation>> {
        return flowOf(emptyList())
    }

    override suspend fun acceptInvitation(invitationId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun declineInvitation(invitationId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun cancelInvitation(invitationId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override fun getGroupMessages(groupId: String): Flow<List<ChatMessage>> {
        return flowOf(emptyList())
    }

    override suspend fun sendGroupTextMessage(
        groupId: String,
        message: String,
        replyToMessageId: String?
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun sendGroupImageMessage(
        groupId: String,
        imageUri: Uri,
        caption: String?,
        replyToMessageId: String?
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun pinMessage(groupId: String, messageId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun unpinMessage(groupId: String, messageId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getPinnedMessages(groupId: String): Result<List<ChatMessage>> {
        return Result.success(emptyList())
    }

    override suspend fun deleteGroupMessage(groupId: String, messageId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun clearGroupMessages(groupId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override fun getGroupActivities(groupId: String): Flow<List<GroupActivity>> {
        return flowOf(emptyList())
    }

    override suspend fun recordGroupActivity(activity: GroupActivity): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("GroupActivities")
                .child(activity.groupId)
                .child(activity.id)
                .setValue(activity)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchGroups(query: String): Result<List<GroupChat>> {
        return Result.success(getMockGroups().filter {
            it.name.contains(query, ignoreCase = true)
        })
    }

    override fun searchGroupMessages(groupId: String, query: String): Flow<List<ChatMessage>> {
        return flowOf(emptyList())
    }

    override suspend fun canPerformAction(groupId: String, action: GroupAction): Result<Boolean> {
        return try {
            val groupResult = getGroup(groupId)
            if (groupResult.isFailure) return Result.success(false)

            val group = groupResult.getOrNull()!!
            val userId = currentUserId

            val canPerform = when (action) {
                GroupAction.SEND_MESSAGE -> group.canSendMessages(userId)
                GroupAction.ADD_MEMBER -> group.canAddMembers(userId)
                GroupAction.REMOVE_MEMBER -> group.isAdmin(userId)
                GroupAction.EDIT_INFO -> group.canModify(userId)
                GroupAction.DELETE_GROUP -> group.createdBy == userId
                GroupAction.PIN_MESSAGE -> group.isAdmin(userId)
                GroupAction.CLEAR_MESSAGES -> group.isAdmin(userId)
                GroupAction.MUTE_MEMBER -> group.isAdmin(userId)
                GroupAction.PROMOTE_ADMIN -> group.createdBy == userId
                GroupAction.CHANGE_SETTINGS -> group.isAdmin(userId)
            }

            Result.success(canPerform)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroupStats(groupId: String): Result<GroupStats> {
        return Result.success(
            GroupStats(
                totalMessages = 0,
                totalMembers = 0,
                totalAdmins = 0,
                createdDaysAgo = 0,
                mostActiveMembers = emptyList(),
                messagesByDay = emptyMap(),
                mediaMessagesCount = 0,
                textMessagesCount = 0
            )
        )
    }

    // ===== FUNCIONES AUXILIARES =====

    private fun getMockGroups(): List<GroupChat> {
        return listOf(
            GroupChat(
                id = "group1",
                name = "Familia 👨‍👩‍👧‍👦",
                description = "Grupo familiar",
                memberIds = listOf("user1", "user2", "user3", "user4", currentUserId),
                adminIds = listOf(currentUserId),
                createdBy = currentUserId,
                createdAt = System.currentTimeMillis() - 86400000, // 1 día atrás
                lastActivity = System.currentTimeMillis() - 1800000 // 30 min atrás
            ),
            GroupChat(
                id = "group2",
                name = "Trabajo 💼",
                description = "Equipo de desarrollo",
                memberIds = listOf("user1", "user2", "user5", "user6", currentUserId),
                adminIds = listOf(currentUserId, "user1"),
                createdBy = currentUserId,
                createdAt = System.currentTimeMillis() - 259200000, // 3 días atrás
                lastActivity = System.currentTimeMillis() - 7200000 // 2 horas atrás
            ),
            GroupChat(
                id = "group3",
                name = "Amigos del Gym 💪",
                description = "Motivación diaria",
                memberIds = listOf("user7", "user8", "user9", currentUserId),
                adminIds = listOf(currentUserId),
                createdBy = currentUserId,
                createdAt = System.currentTimeMillis() - 604800000, // 1 semana atrás
                lastActivity = System.currentTimeMillis() - 3600000 // 1 hora atrás
            )
        )
    }

    private fun getMockUsers(): List<User> {
        return listOf(
            User(
                id = "user1",
                username = "juan_perez",
                email = "juan@example.com",
                profileImage = "",
                status = "online",
                isOnline = true
            ),
            User(
                id = "user2",
                username = "maria_garcia",
                email = "maria@example.com",
                profileImage = "",
                status = "offline",
                isOnline = false
            ),
            User(
                id = "user3",
                username = "carlos_rodriguez",
                email = "carlos@example.com",
                profileImage = "",
                status = "online",
                isOnline = true
            )
        )
    }
}
package com.yerayyas.chatappkotlinproject.domain.repository

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupInvitation
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import kotlinx.coroutines.flow.Flow

interface GroupChatRepository {

    // Gestión básica de grupos
    suspend fun createGroup(group: GroupChat): Result<String>
    suspend fun getGroupById(groupId: String): GroupChat?
    suspend fun updateGroup(group: GroupChat): Result<Unit>
    suspend fun deleteGroup(groupId: String): Result<Unit>

    // Gestión de miembros
    suspend fun addMemberToGroup(groupId: String, userId: String): Result<Unit>
    suspend fun removeMemberFromGroup(groupId: String, userId: String): Result<Unit>
    suspend fun makeAdmin(groupId: String, userId: String): Result<Unit>
    suspend fun removeAdmin(groupId: String, userId: String): Result<Unit>
    suspend fun getGroupMembers(groupId: String): List<String>

    // Mensajería grupal
    suspend fun sendMessageToGroup(groupId: String, message: GroupMessage): Result<Unit>
    suspend fun getGroupMessages(groupId: String): Flow<List<GroupMessage>>
    suspend fun updateLastActivity(groupId: String, message: GroupMessage): Result<Unit>
    suspend fun uploadGroupMessageImage(groupId: String, imageUri: Uri): Result<String>

    // Configuraciones
    suspend fun updateGroupSettings(groupId: String, settings: Map<String, Any>): Result<Unit>
    suspend fun updateGroupImage(groupId: String, imageUri: Uri): Result<String>

    // Invitaciones
    suspend fun createInvitation(invitation: GroupInvitation): Result<String>
    suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<Unit>
    suspend fun getInvitationsForUser(userId: String): Flow<List<GroupInvitation>>

    // Read receipts
    suspend fun markMessageAsRead(groupId: String, messageId: String, userId: String): Result<Unit>
    suspend fun getMessageReadReceipts(messageId: String): Map<String, Long>

    // Búsqueda y filtros
    suspend fun searchGroupMessages(groupId: String, query: String): List<GroupMessage>
    fun getUserGroups(userId: String): Flow<List<GroupChat>>
}
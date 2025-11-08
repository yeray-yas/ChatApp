package com.yerayyas.chatappkotlinproject.domain.repository

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface para gestionar chats grupales completos
 */
interface GroupChatRepository {

    // ===== GESTIÓN DE GRUPOS =====

    /**
     * Crea un nuevo grupo de chat
     */
    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        imageUri: Uri? = null
    ): Result<String> // Retorna el ID del grupo creado

    /**
     * Obtiene la información de un grupo específico
     */
    suspend fun getGroup(groupId: String): Result<GroupChat>

    /**
     * Obtiene todos los grupos del usuario actual
     */
    fun getUserGroups(): Flow<List<GroupChat>>

    /**
     * Actualiza la información básica del grupo
     */
    suspend fun updateGroupInfo(
        groupId: String,
        name: String? = null,
        description: String? = null,
        imageUri: Uri? = null
    ): Result<Unit>

    /**
     * Actualiza la configuración del grupo
     */
    suspend fun updateGroupSettings(
        groupId: String,
        settings: GroupSettings
    ): Result<Unit>

    /**
     * Elimina un grupo (solo el creador)
     */
    suspend fun deleteGroup(groupId: String): Result<Unit>

    // ===== GESTIÓN DE MIEMBROS =====

    /**
     * Obtiene la lista de miembros de un grupo
     */
    suspend fun getGroupMembers(groupId: String): Result<List<User>>

    /**
     * Agrega un miembro al grupo
     */
    suspend fun addMember(groupId: String, userId: String): Result<Unit>

    /**
     * Elimina un miembro del grupo
     */
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>

    /**
     * Abandona el grupo (usuario actual)
     */
    suspend fun leaveGroup(groupId: String): Result<Unit>

    /**
     * Promueve un miembro a administrador
     */
    suspend fun promoteToAdmin(groupId: String, userId: String): Result<Unit>

    /**
     * Quita permisos de administrador
     */
    suspend fun demoteFromAdmin(groupId: String, userId: String): Result<Unit>

    /**
     * Silencia un miembro
     */
    suspend fun muteMember(groupId: String, userId: String): Result<Unit>

    /**
     * Quita el silencio a un miembro
     */
    suspend fun unmuteMember(groupId: String, userId: String): Result<Unit>

    // ===== INVITACIONES =====

    /**
     * Crea una invitación para unirse al grupo
     */
    suspend fun createInvitation(groupId: String, userId: String): Result<String>

    /**
     * Obtiene las invitaciones pendientes de un usuario
     */
    fun getPendingInvitations(): Flow<List<GroupInvitation>>

    /**
     * Acepta una invitación
     */
    suspend fun acceptInvitation(invitationId: String): Result<Unit>

    /**
     * Rechaza una invitación
     */
    suspend fun declineInvitation(invitationId: String): Result<Unit>

    /**
     * Cancela una invitación (quien invita)
     */
    suspend fun cancelInvitation(invitationId: String): Result<Unit>

    // ===== MENSAJERÍA GRUPAL =====

    /**
     * Obtiene los mensajes de un grupo
     */
    fun getGroupMessages(groupId: String): Flow<List<ChatMessage>>

    /**
     * Envía un mensaje de texto al grupo
     */
    suspend fun sendGroupTextMessage(
        groupId: String,
        message: String,
        replyToMessageId: String? = null
    ): Result<Unit>

    /**
     * Envía una imagen al grupo
     */
    suspend fun sendGroupImageMessage(
        groupId: String,
        imageUri: Uri,
        caption: String? = null,
        replyToMessageId: String? = null
    ): Result<Unit>

    /**
     * Fija un mensaje en el grupo
     */
    suspend fun pinMessage(groupId: String, messageId: String): Result<Unit>

    /**
     * Desfija un mensaje
     */
    suspend fun unpinMessage(groupId: String, messageId: String): Result<Unit>

    /**
     * Obtiene los mensajes fijados del grupo
     */
    suspend fun getPinnedMessages(groupId: String): Result<List<ChatMessage>>

    /**
     * Elimina un mensaje del grupo (admins o autor)
     */
    suspend fun deleteGroupMessage(groupId: String, messageId: String): Result<Unit>

    /**
     * Elimina todos los mensajes del grupo (solo admins)
     */
    suspend fun clearGroupMessages(groupId: String): Result<Unit>

    // ===== ACTIVIDADES =====

    /**
     * Obtiene el historial de actividades del grupo
     */
    fun getGroupActivities(groupId: String): Flow<List<GroupActivity>>

    /**
     * Registra una nueva actividad en el grupo
     */
    suspend fun recordGroupActivity(activity: GroupActivity): Result<Unit>

    // ===== BÚSQUEDA =====

    /**
     * Busca grupos por nombre
     */
    suspend fun searchGroups(query: String): Result<List<GroupChat>>

    /**
     * Busca mensajes dentro de un grupo
     */
    fun searchGroupMessages(groupId: String, query: String): Flow<List<ChatMessage>>

    // ===== UTILIDADES =====

    /**
     * Verifica si el usuario actual puede realizar una acción específica
     */
    suspend fun canPerformAction(
        groupId: String,
        action: GroupAction
    ): Result<Boolean>

    /**
     * Obtiene estadísticas del grupo
     */
    suspend fun getGroupStats(groupId: String): Result<GroupStats>
}

/**
 * Acciones posibles en un grupo
 */
enum class GroupAction {
    SEND_MESSAGE,
    ADD_MEMBER,
    REMOVE_MEMBER,
    EDIT_INFO,
    DELETE_GROUP,
    PIN_MESSAGE,
    CLEAR_MESSAGES,
    MUTE_MEMBER,
    PROMOTE_ADMIN,
    CHANGE_SETTINGS
}

/**
 * Estadísticas de un grupo
 */
data class GroupStats(
    val totalMessages: Int,
    val totalMembers: Int,
    val totalAdmins: Int,
    val createdDaysAgo: Int,
    val mostActiveMembers: List<String>,
    val messagesByDay: Map<String, Int>,
    val mediaMessagesCount: Int,
    val textMessagesCount: Int
)
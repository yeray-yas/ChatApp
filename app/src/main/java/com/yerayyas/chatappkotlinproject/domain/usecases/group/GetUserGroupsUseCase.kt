package com.yerayyas.chatappkotlinproject.domain.usecases.group

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * Use case para obtener los grupos del usuario actual
 */
class GetUserGroupsUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Obtiene todos los grupos del usuario actual como Flow
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
     * Obtiene grupos activos del usuario
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
     * Obtiene un grupo específico por ID
     */
    suspend fun getGroupById(groupId: String): GroupChat? {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid ?: return null
            val group = groupRepository.getGroupById(groupId)

            // Verificar que el usuario es miembro del grupo
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
     * Verifica si el usuario es miembro de un grupo específico
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
     * Verifica si el usuario es administrador de un grupo específico
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
     * Obtiene estadísticas básicas de los grupos del usuario
     */
    suspend fun getUserGroupStats(): GroupStats {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid ?: return GroupStats()

            // Esta sería una implementación simplificada
            // En una app real, podrías tener endpoints específicos para estadísticas
            GroupStats(
                totalGroups = 0, // Se calcularía desde el Flow
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
 * Data class para estadísticas de grupos del usuario
 */
data class GroupStats(
    val totalGroups: Int = 0,
    val adminGroups: Int = 0,
    val activeGroups: Int = 0,
    val unreadMessages: Int = 0
)
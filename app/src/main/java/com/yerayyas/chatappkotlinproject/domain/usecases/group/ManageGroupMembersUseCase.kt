package com.yerayyas.chatappkotlinproject.domain.usecases.group

import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.GroupAction
import javax.inject.Inject

/**
 * Use case para gestionar miembros de grupos (agregar, eliminar, promover, etc.)
 */
class ManageGroupMembersUseCase @Inject constructor(
    private val groupChatRepository: GroupChatRepository
) {

    /**
     * Agrega un miembro al grupo
     */
    suspend fun addMember(groupId: String, userId: String): Result<Unit> {
        return try {
            // Verificar permisos
            val canAdd = groupChatRepository.canPerformAction(groupId, GroupAction.ADD_MEMBER)
            if (canAdd.isFailure || canAdd.getOrNull() != true) {
                return Result.failure(SecurityException("No tienes permisos para agregar miembros"))
            }

            // Verificar que el grupo existe y obtener info
            val groupResult = groupChatRepository.getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(Exception("Grupo no encontrado"))
            }

            val group = groupResult.getOrNull()!!

            // Verificar límites
            if (group.getMemberCount() >= 256) {
                return Result.failure(IllegalStateException("El grupo ha alcanzado el límite máximo de miembros"))
            }

            // Verificar que no sea ya miembro
            if (group.isMember(userId)) {
                return Result.failure(IllegalStateException("El usuario ya es miembro del grupo"))
            }

            groupChatRepository.addMember(groupId, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un miembro del grupo
     */
    suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return try {
            // Verificar permisos
            val canRemove = groupChatRepository.canPerformAction(groupId, GroupAction.REMOVE_MEMBER)
            if (canRemove.isFailure || canRemove.getOrNull() != true) {
                return Result.failure(SecurityException("No tienes permisos para eliminar miembros"))
            }

            // Verificar que el grupo existe
            val groupResult = groupChatRepository.getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(Exception("Grupo no encontrado"))
            }

            val group = groupResult.getOrNull()!!

            // No se puede eliminar al creador del grupo
            if (group.createdBy == userId) {
                return Result.failure(IllegalStateException("No se puede eliminar al creador del grupo"))
            }

            // Verificar que es miembro
            if (!group.isMember(userId)) {
                return Result.failure(IllegalStateException("El usuario no es miembro del grupo"))
            }

            groupChatRepository.removeMember(groupId, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Promueve un miembro a administrador
     */
    suspend fun promoteToAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            // Verificar permisos
            val canPromote =
                groupChatRepository.canPerformAction(groupId, GroupAction.PROMOTE_ADMIN)
            if (canPromote.isFailure || canPromote.getOrNull() != true) {
                return Result.failure(SecurityException("No tienes permisos para promover administradores"))
            }

            val groupResult = groupChatRepository.getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(Exception("Grupo no encontrado"))
            }

            val group = groupResult.getOrNull()!!

            // Verificar que es miembro
            if (!group.isMember(userId)) {
                return Result.failure(IllegalStateException("El usuario debe ser miembro del grupo"))
            }

            // Verificar que no es ya admin
            if (group.isAdmin(userId)) {
                return Result.failure(IllegalStateException("El usuario ya es administrador"))
            }

            groupChatRepository.promoteToAdmin(groupId, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Quita permisos de administrador
     */
    suspend fun demoteFromAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupResult = groupChatRepository.getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(Exception("Grupo no encontrado"))
            }

            val group = groupResult.getOrNull()!!

            // No se puede quitar admin al creador
            if (group.createdBy == userId) {
                return Result.failure(IllegalStateException("No se puede quitar permisos de admin al creador del grupo"))
            }

            // Verificar que es admin
            if (!group.isAdmin(userId)) {
                return Result.failure(IllegalStateException("El usuario no es administrador"))
            }

            groupChatRepository.demoteFromAdmin(groupId, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Silencia un miembro
     */
    suspend fun muteMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val canMute = groupChatRepository.canPerformAction(groupId, GroupAction.MUTE_MEMBER)
            if (canMute.isFailure || canMute.getOrNull() != true) {
                return Result.failure(SecurityException("No tienes permisos para silenciar miembros"))
            }

            val groupResult = groupChatRepository.getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(Exception("Grupo no encontrado"))
            }

            val group = groupResult.getOrNull()!!

            // No se puede silenciar al creador
            if (group.createdBy == userId) {
                return Result.failure(IllegalStateException("No se puede silenciar al creador del grupo"))
            }

            // No se puede silenciar a otros admins (a menos que seas el creador)
            if (group.isAdmin(userId)) {
                return Result.failure(IllegalStateException("No se puede silenciar a un administrador"))
            }

            if (group.isMuted(userId)) {
                return Result.failure(IllegalStateException("El usuario ya está silenciado"))
            }

            groupChatRepository.muteMember(groupId, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Quita el silencio a un miembro
     */
    suspend fun unmuteMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val groupResult = groupChatRepository.getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(Exception("Grupo no encontrado"))
            }

            val group = groupResult.getOrNull()!!

            if (!group.isMuted(userId)) {
                return Result.failure(IllegalStateException("El usuario no está silenciado"))
            }

            groupChatRepository.unmuteMember(groupId, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Abandona el grupo
     */
    suspend fun leaveGroup(groupId: String): Result<Unit> {
        return try {
            val groupResult = groupChatRepository.getGroup(groupId)
            if (groupResult.isFailure) {
                return Result.failure(Exception("Grupo no encontrado"))
            }

            val group = groupResult.getOrNull()!!

            // El creador no puede abandonar el grupo, debe transferir la propiedad o eliminarlo
            if (group.createdBy == getCurrentUserId()) {
                return Result.failure(IllegalStateException("El creador no puede abandonar el grupo. Debe transferir la propiedad o eliminar el grupo"))
            }

            groupChatRepository.leaveGroup(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mock function - en implementación real sería inyectado
    private fun getCurrentUserId(): String = "" // Implementar según la arquitectura actual
}
package com.yerayyas.chatappkotlinproject.domain.usecases.group

import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupActivityType
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Use case para gestionar miembros de un grupo
 */
class ManageGroupMembersUseCase @Inject constructor(
    private val groupRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth,
    private val sendGroupMessageUseCase: SendGroupMessageUseCase
) {
    /**
     * Añade un miembro al grupo
     */
    suspend fun addMember(
        groupId: String,
        userId: String,
        userName: String,
        addedByName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Verificar que el usuario actual puede añadir miembros
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Grupo no encontrado"))

            if (!group.canAddMembers(currentUserId)) {
                return Result.failure(Exception("No tienes permisos para añadir miembros"))
            }

            // Añadir el miembro
            val result = groupRepository.addMemberToGroup(groupId, userId)

            if (result.isSuccess) {
                // Enviar mensaje de notificación
                sendGroupMessageUseCase.sendSystemMessage(
                    groupId = groupId,
                    message = "$addedByName agregó a $userName al grupo",
                    systemMessageType = GroupActivityType.USER_ADDED
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un miembro del grupo
     */
    suspend fun removeMember(
        groupId: String,
        userId: String,
        userName: String,
        removedByName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Verificar permisos
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Grupo no encontrado"))

            if (!group.isAdmin(currentUserId) && currentUserId != userId) {
                return Result.failure(Exception("No tienes permisos para eliminar este miembro"))
            }

            // No permitir que se elimine el creador del grupo
            if (userId == group.createdBy) {
                return Result.failure(Exception("No se puede eliminar al creador del grupo"))
            }

            // Eliminar el miembro
            val result = groupRepository.removeMemberFromGroup(groupId, userId)

            if (result.isSuccess) {
                // Enviar mensaje de notificación
                val message = if (currentUserId == userId) {
                    "$userName dejó el grupo"
                } else {
                    "$removedByName eliminó a $userName del grupo"
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
     * Promueve un miembro a administrador
     */
    suspend fun promoteToAdmin(
        groupId: String,
        userId: String,
        userName: String,
        promotedByName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Verificar que el usuario actual es admin
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Grupo no encontrado"))

            if (!group.isAdmin(currentUserId)) {
                return Result.failure(Exception("Solo los administradores pueden promover a otros miembros"))
            }

            if (group.isAdmin(userId)) {
                return Result.failure(Exception("El usuario ya es administrador"))
            }

            // Promover a admin
            val result = groupRepository.makeAdmin(groupId, userId)

            if (result.isSuccess) {
                // Enviar mensaje de notificación
                sendGroupMessageUseCase.sendSystemMessage(
                    groupId = groupId,
                    message = "$promotedByName nombró administrador a $userName",
                    systemMessageType = GroupActivityType.ADMIN_ADDED
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Quita privilegios de administrador a un miembro
     */
    suspend fun demoteAdmin(
        groupId: String,
        userId: String,
        userName: String,
        demotedByName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Verificar permisos
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Grupo no encontrado"))

            if (!group.isAdmin(currentUserId)) {
                return Result.failure(Exception("Solo los administradores pueden quitar privilegios"))
            }

            if (!group.isAdmin(userId)) {
                return Result.failure(Exception("El usuario no es administrador"))
            }

            // No permitir que se degrade al creador del grupo
            if (userId == group.createdBy) {
                return Result.failure(Exception("No se puede quitar privilegios al creador del grupo"))
            }

            // Quitar privilegios de admin
            val result = groupRepository.removeAdmin(groupId, userId)

            if (result.isSuccess) {
                // Enviar mensaje de notificación
                sendGroupMessageUseCase.sendSystemMessage(
                    groupId = groupId,
                    message = "$demotedByName quitó privilegios de administrador a $userName",
                    systemMessageType = GroupActivityType.ADMIN_REMOVED
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Abandona el grupo (usuario actual)
     */
    suspend fun leaveGroup(
        groupId: String,
        userName: String
    ): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Verificar que no es el creador
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(Exception("Grupo no encontrado"))

            if (currentUserId == group.createdBy) {
                return Result.failure(Exception("El creador del grupo no puede abandonarlo. Debe transferir la propiedad primero."))
            }

            // Abandonar el grupo
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
     * Obtiene la lista de miembros del grupo
     */
    suspend fun getGroupMembers(groupId: String): List<String> {
        return try {
            groupRepository.getGroupMembers(groupId)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
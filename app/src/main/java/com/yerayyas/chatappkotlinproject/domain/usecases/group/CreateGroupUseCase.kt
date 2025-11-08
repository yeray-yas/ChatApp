package com.yerayyas.chatappkotlinproject.domain.usecases.group

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Use case para crear un nuevo grupo de chat
 */
class CreateGroupUseCase @Inject constructor(
    private val groupChatRepository: GroupChatRepository,
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Crea un nuevo grupo de chat
     *
     * @param name Nombre del grupo
     * @param description Descripción del grupo
     * @param memberIds Lista de IDs de miembros iniciales
     * @param imageUri URI de la imagen del grupo (opcional)
     * @return Result con el ID del grupo creado
     */
    suspend fun execute(
        name: String,
        description: String,
        memberIds: List<String>,
        imageUri: Uri? = null
    ): Result<String> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Validaciones
            if (name.isBlank()) {
                return Result.failure(IllegalArgumentException("El nombre del grupo no puede estar vacío"))
            }

            if (name.length > 100) {
                return Result.failure(IllegalArgumentException("El nombre del grupo no puede tener más de 100 caracteres"))
            }

            if (description.length > 500) {
                return Result.failure(IllegalArgumentException("La descripción no puede tener más de 500 caracteres"))
            }

            if (memberIds.isEmpty()) {
                return Result.failure(IllegalArgumentException("Debe agregar al menos un miembro al grupo"))
            }

            if (memberIds.size > 256) {
                return Result.failure(IllegalArgumentException("Un grupo no puede tener más de 256 miembros"))
            }

            // Crear objeto GroupChat
            val group = GroupChat(
                name = name.trim(),
                description = description.trim(),
                memberIds = (memberIds + currentUserId).distinct(), // Agregar el creador y remover duplicados
                adminIds = listOf(currentUserId), // El creador es administrador
                createdBy = currentUserId,
                createdAt = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis(),
                isActive = true
            )

            // Crear el grupo
            groupChatRepository.createGroup(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
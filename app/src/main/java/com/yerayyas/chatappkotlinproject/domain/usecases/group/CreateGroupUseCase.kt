package com.yerayyas.chatappkotlinproject.domain.usecases.group

import android.net.Uri
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import javax.inject.Inject

/**
 * Use case para crear un nuevo grupo de chat
 */
class CreateGroupUseCase @Inject constructor(
    private val groupChatRepository: GroupChatRepository
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

            // Crear el grupo
            groupChatRepository.createGroup(
                name = name.trim(),
                description = description.trim(),
                memberIds = memberIds.distinct(), // Remover duplicados
                imageUri = imageUri
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
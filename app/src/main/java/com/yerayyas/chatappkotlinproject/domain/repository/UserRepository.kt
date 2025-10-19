package com.yerayyas.chatappkotlinproject.domain.repository

/**
 * Interfaz para operaciones relacionadas con los datos del usuario,
 * incluyendo la gestión del token FCM.
 */
interface UserRepository {
    /**
     * Actualiza (o guarda si no existe) el token FCM para el usuario actualmente logueado
     * en la base de datos del backend.
     * @param token El nuevo token FCM a guardar.
     * @throws Exception si ocurre un error durante la operación de base de datos.
     */
    suspend fun updateCurrentUserFCMToken(token: String)

    /**
     * Elimina el token FCM asociado al usuario actualmente logueado
     * de la base de datos del backend (útil al cerrar sesión).
     * @throws Exception si ocurre un error durante la operación de base de datos.
     */
    suspend fun clearCurrentUserFCMToken()

    // Podrías añadir otras funciones relacionadas con el usuario aquí si es necesario
    // suspend fun getCurrentUserProfile(): UserProfile?
}
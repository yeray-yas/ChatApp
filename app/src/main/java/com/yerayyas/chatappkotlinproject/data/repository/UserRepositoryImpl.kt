package com.yerayyas.chatappkotlinproject.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await // Necesitas la dependencia kotlinx-coroutines-play-services
import javax.inject.Inject
import javax.inject.Singleton
import com.yerayyas.chatappkotlinproject.utils.Constants.FCM_TOKENS_PATH

private const val TAG = "UserRepositoryImpl"

/**
 * Implementación de [UserRepository] que usa Firebase Authentication y
 * Firebase Realtime Database para gestionar los tokens FCM.
 */
@Singleton // Hilt creará una sola instancia de esta implementación
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth, // Inyecta FirebaseAuth
    private val database: DatabaseReference // Inyecta la referencia raíz de RTDB
) : UserRepository {

    override suspend fun updateCurrentUserFCMToken(token: String) {
        val userId = auth.currentUser?.uid ?: return

        if (token.isBlank()) {
            Log.w(TAG, "Cannot update FCM token: Token is blank.")
            return
        }

        try {
            // Guarda/actualiza el token en /user_fcm_tokens/{userId}/token = "EL_TOKEN_FCM"
            // Usar un nodo específico para tokens es mejor que guardarlo dentro del perfil de usuario.
            // Podrías incluso guardar una lista o mapa si permites múltiples dispositivos.
            // Por simplicidad, aquí solo guardamos el último token.
            database.child(FCM_TOKENS_PATH).child(userId).child("token").setValue(token).await()
            Log.i(TAG, "FCM token updated successfully for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token in Realtime Database for user $userId", e)
            throw e // Relanza la excepción para que el llamador (el Service) sepa que falló
        }
    }

    override suspend fun clearCurrentUserFCMToken() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot clear FCM token: No user logged in.")
            return
        }

        try {
            // Elimina la entrada del token para este usuario
            database.child(FCM_TOKENS_PATH).child(userId).removeValue().await()
            Log.i(TAG, "FCM token cleared successfully for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing FCM token in Realtime Database for user $userId", e)
            // Decide si relanzar o solo loggear dependiendo de la criticidad
            // throw e
        }
    }
}
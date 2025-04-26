package com.yerayyas.chatappkotlinproject.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepositoryImpl" // Tag para logs
private const val USERS_COLLECTION = "users" // Nombre de la colección de usuarios en Firestore
private const val FCM_TOKEN_FIELD = "fcmToken" // Nombre del campo para el token en el documento

/**
 * Implementación de [UserRepository] que usa Firebase Authentication y
 * Firestore para gestionar los tokens FCM.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth, // Inyecta FirebaseAuth
    private val firestore: FirebaseFirestore // <-- Cambiado: Inyecta FirebaseFirestore
) : UserRepository {

    override suspend fun updateCurrentUserFCMToken(token: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot update FCM token: No user logged in.")
            // Podrías lanzar una excepción aquí si lo consideras un error grave
            return
        }

        if (token.isBlank()) {
            Log.w(TAG, "Cannot update FCM token for user $userId: Token is blank.")
            return
        }

        try {
            // Actualiza el campo 'fcmToken' en el documento del usuario dentro de la colección 'users'
            // Asume que el documento del usuario ya existe.
            // Si el documento podría NO existir, considera usar .set(mapOf(FCM_TOKEN_FIELD to token), SetOptions.merge())
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(FCM_TOKEN_FIELD, token) // Actualiza solo el campo del token
                .await() // Espera a que la operación termine

            Log.i(TAG, "FCM token updated successfully in Firestore for user $userId")

        } catch (e: Exception) {
            // Firestore puede lanzar una excepción si el documento no existe al usar update.
            // Podrías intentar crearlo aquí como fallback si fuera necesario.
            Log.e(TAG, "Error updating FCM token in Firestore for user $userId", e)
            throw e // Relanza para que el llamador sepa que falló
        }
    }

    override suspend fun clearCurrentUserFCMToken() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot clear FCM token: No user logged in.")
            return
        }

        try {
            // Elimina el campo 'fcmToken' del documento del usuario
            val updates = mapOf(
                FCM_TOKEN_FIELD to FieldValue.delete() // FieldValue.delete() elimina el campo
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates) // Usa update con un mapa para eliminar el campo
                .await() // Espera a que la operación termine

            Log.i(TAG, "FCM token field cleared successfully in Firestore for user $userId")

        } catch (e: Exception) {
            // Puede fallar si el documento no existe o por problemas de permisos/red.
            Log.e(TAG, "Error clearing FCM token field in Firestore for user $userId", e)
            // Decide si relanzar o solo loggear
            // throw e
        }
    }

    // --- Funciones existentes o futuras de UserRepositoryImpl ---
    // ...
}
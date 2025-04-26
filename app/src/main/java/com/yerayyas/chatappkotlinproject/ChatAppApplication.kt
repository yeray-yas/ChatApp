package com.yerayyas.chatappkotlinproject

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * ChatAppApplication is the application class for the Chat App.
 *
 * This class uses Hilt for dependency injection and implements
 * DefaultLifecycleObserver to update the user's online status
 * in Firebase based on the application's foreground/background state.
 *
 * Dependencies injected:
 * - FirebaseAuth: For authentication and obtaining the current user.
 * - DatabaseReference: For accessing and updating the Firebase Realtime Database.
 *
 * The class observes the lifecycle of the entire application using ProcessLifecycleOwner.
 * When the app goes to the foreground, the user's status is updated to "online",
 * and when it goes to the background, the status is updated to "offline".
 */
@HiltAndroidApp
class ChatAppApplication : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var auth: FirebaseAuth

    // @Inject // <-- Eliminar inyección de RTDB
    // lateinit var database: DatabaseReference

    @Inject // <-- Inyectar Firestore
    lateinit var firestore: FirebaseFirestore

    // isAppInForeground se maneja ahora en AppState,
    // pero la lógica de observer aquí sigue siendo válida para actualizar DB.
    // Podrías incluso inyectar AppState aquí si quieres sincronizar,
    // pero para solo actualizar DB, este observer funciona bien.

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Actualizar estado a 'online' al entrar en foreground
        updateUserStatus("online")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Actualizar estado a 'offline' al entrar en background
        updateUserStatus("offline")
    }

    /**
     * Updates the user's status and lastSeen timestamp in Firestore.
     *
     * It updates the "status" and "lastSeen" fields directly within the user's document
     * in the "users" collection.
     * If the update fails, an error message is logged.
     *
     * @param status A String representing the new status, e.g., "online" or "offline".
     */
    private fun updateUserStatus(status: String) {
        auth.currentUser?.uid?.let { uid ->
            // Referencia al documento del usuario en Firestore
            val userDocRef = firestore.collection("users").document(uid)

            // Mapa con los campos a actualizar
            val updates = mapOf(
                "status" to status,
                "lastSeen" to FieldValue.serverTimestamp() // <-- Timestamp del servidor de Firestore
            )

            // Ejecutar la actualización
            userDocRef.update(updates)
                .addOnSuccessListener {
                    Log.d("AppLifecycle", "User status successfully updated to '$status' in Firestore.")
                }
                .addOnFailureListener { e ->
                    // El error puede ser por documento no existente, permisos, red, etc.
                    Log.e("AppLifecycle", "Error updating user status to '$status' in Firestore", e)
                    // Considerar si el documento podría no existir y usar .set(updates, SetOptions.merge()) en su lugar si es necesario.
                }
        } ?: run {
            // Si no hay usuario logueado, no hacemos nada (o podríamos loggear un warning)
            Log.w("AppLifecycle", "Cannot update user status: No user logged in.")
        }
    }
}

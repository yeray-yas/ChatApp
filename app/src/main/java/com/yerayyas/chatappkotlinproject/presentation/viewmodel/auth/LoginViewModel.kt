package com.yerayyas.chatappkotlinproject.presentation.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "LoginViewModel"

/**
 * ViewModel for managing the login process.
 * Handles authentication and updates the FCM token upon successful login.
 *
 * @property auth The FirebaseAuth instance used for authentication.
 * @property userRepository Repository for updating user data, including FCM token.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    /**
     * Attempts to log in the user with the provided email and password.
     *
     * This function calls FirebaseAuth's signInWithEmailAndPassword method asynchronously
     * and invokes the provided callback with the result. If the login is successful,
     * the callback is triggered with a success flag and no error message. If there is an error,
     * the callback is triggered with a failure flag and the error message.
     *
     * @param email The email of the user trying to log in.
     * @param password The password of the user trying to log in.
     * @param onResult A callback function that returns a boolean indicating success or failure,
     *                 and an optional error message in case of failure.
     */
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting login for email: $email")
                auth.signInWithEmailAndPassword(email, password).await()
                Log.i(TAG, "Login successful for email: $email. User ID: ${auth.currentUser?.uid}")

                // --- 2. Llamar a la actualización del token DESPUÉS del await exitoso ---
                updateFcmTokenAfterLogin()

                // --- Llamar al callback de éxito DESPUÉS de intentar actualizar el token ---
                onResult(true, null)

            } catch (e: Exception) {
                Log.e(TAG, "Login failed for email: $email", e)
                onResult(false, e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Fetches the current FCM token and attempts to update it in the repository.
     * This should be called after a successful login ensures auth.currentUser is not null.
     */
    // --- 3. Crear función privada para actualizar el token ---
    private fun updateFcmTokenAfterLogin() {
        // El usuario DEBERÍA estar autenticado aquí, pero una comprobación extra no hace daño
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.e(TAG, "updateFcmTokenAfterLogin called, but currentUser is unexpectedly null!")
            return // Salir si no hay usuario (esto no debería pasar después de un login exitoso)
        }

        Log.d(TAG, "Attempting to fetch and update FCM token for user: $currentUserId")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed for user $currentUserId", task.exception)
                // No fallar el login completo solo por esto, solo loguear
                return@addOnCompleteListener
            }

            // Obtener el token FCM actual
            val token = task.result
            Log.d(TAG, "Fetched current FCM token: ${token?.take(10)}...")

            if (token != null) {
                // Lanzar una nueva coroutine para llamar a la función suspend del repositorio
                viewModelScope.launch {
                    try {
                        Log.d(TAG, "Calling userRepository.updateCurrentUserFCMToken from ViewModel for user $currentUserId...")
                        userRepository.updateCurrentUserFCMToken(token)
                        Log.i(TAG, "FCM Token update call initiated successfully after login for user $currentUserId.")
                    } catch (e: Exception) {
                        // Loguear el error pero no afectar el resultado del login
                        Log.e(TAG, "Error calling updateCurrentUserFCMToken after login for user $currentUserId", e)
                    }
                }
            } else {
                Log.w(TAG, "Fetched FCM token is null for user $currentUserId.")
            }
        }
    }
}

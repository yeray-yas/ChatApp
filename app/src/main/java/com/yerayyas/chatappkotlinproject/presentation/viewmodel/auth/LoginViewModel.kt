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

                try {
                    userRepository.updateUserStatus("online")
                    Log.i(TAG, "User status updated to online manually.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update user status to online", e)
                }
                updateFcmTokenAfterLogin()
                onResult(true, null)

            } catch (e: Exception) {
                Log.e(TAG, "Login failed for email: $email", e)
                onResult(false, e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Asynchronously fetches the current FCM token and updates it in the repository.
     * This is a fire-and-forget operation launched after a successful login.
     * Errors are logged but do not affect the main login result.
     */
    private fun updateFcmTokenAfterLogin() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.e(TAG, "updateFcmTokenAfterLogin called, but currentUser is unexpectedly null!")
            return
        }

        Log.d(TAG, "Attempting to fetch and update FCM token for user: $currentUserId")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed for user $currentUserId", task.exception)
                // Don't fail the entire login for this, just log it
                return@addOnCompleteListener
            }
            
            val token = task.result
            Log.d(TAG, "Fetched current FCM token: ${token?.take(10)}...")

            if (token != null) {
                viewModelScope.launch {
                    try {
                        Log.d(TAG, "Calling userRepository.updateCurrentUserFCMToken from ViewModel for user $currentUserId...")
                        userRepository.updateCurrentUserFCMToken(token)
                        Log.i(TAG, "FCM Token update call initiated successfully after login for user $currentUserId.")
                    } catch (e: Exception) {
                        // Log the error but don't affect the login result
                        Log.e(TAG, "Error calling updateCurrentUserFCMToken after login for user $currentUserId", e)
                    }
                }
            } else {
                Log.w(TAG, "Fetched FCM token is null for user $currentUserId.")
            }
        }
    }
}

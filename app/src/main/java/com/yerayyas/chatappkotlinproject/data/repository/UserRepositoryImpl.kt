package com.yerayyas.chatappkotlinproject.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.yerayyas.chatappkotlinproject.utils.Constants.FCM_TOKENS_PATH

private const val TAG = "UserRepositoryImpl"

/**
 * Concrete implementation of the [UserRepository] interface.
 *
 * This class interacts with Firebase Authentication to get the current user's ID and with
 * Firebase Realtime Database to manage the user's FCM (Firebase Cloud Messaging) token.
 * It is provided as a singleton throughout the application by Hilt.
 *
 * @property auth An instance of [FirebaseAuth] to retrieve the current user.
 * @property database A reference to the Firebase Realtime Database.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference
) : UserRepository {

    /**
     * Saves or updates the FCM token for the currently authenticated user in the Realtime Database.
     *
     * If no user is logged in or the token is blank, the operation is aborted. The token is stored
     * under the `/fcm_tokens/{userId}/` path.
     *
     * @param token The new FCM token to save.
     * @throws Exception if the database operation fails, propagating the error to the caller.
     */
    override suspend fun updateCurrentUserFCMToken(token: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot update FCM token: User is not authenticated.")
            return
        }
        if (token.isBlank()) {
            Log.w(TAG, "Cannot update FCM token: Token is blank.")
            return
        }

        try {
            database
                .child(FCM_TOKENS_PATH)
                .child(userId)
                .child("token")
                .setValue(token)
                .await()
            Log.i(TAG, "FCM token updated successfully for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token for user $userId", e)
            throw e // Re-throw the exception to be handled by the use case or ViewModel.
        }
    }

    /**
     * Removes the FCM token for the currently authenticated user from the Realtime Database.
     *
     * This is typically called during a sign-out process. If no user is logged in, the operation
     * does nothing. Any exceptions during the database operation are logged and suppressed.
     */
    override suspend fun clearCurrentUserFCMToken() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot clear FCM token: User is not authenticated.")
            return
        }

        try {
            database
                .child(FCM_TOKENS_PATH)
                .child(userId)
                .removeValue()
                .await()
            Log.i(TAG, "FCM token cleared successfully for user $userId")
        } catch (e: Exception) {
            // Log the error but suppress it to ensure the sign-out flow is not interrupted.
            Log.e(TAG, "Error clearing FCM token for user $userId", e)
        }
    }
}

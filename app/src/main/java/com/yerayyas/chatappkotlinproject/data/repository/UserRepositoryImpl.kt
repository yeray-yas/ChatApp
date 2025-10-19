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
 * Implementation of [UserRepository] that uses Firebase Authentication
 * and Firebase Realtime Database to manage FCM tokens.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference
) : UserRepository {

    override suspend fun updateCurrentUserFCMToken(token: String) {
        val userId = auth.currentUser?.uid ?: return
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
            throw e
        }
    }

    override suspend fun clearCurrentUserFCMToken() {
        val userId = auth.currentUser?.uid ?: return
        try {
            database
                .child(FCM_TOKENS_PATH)
                .child(userId)
                .removeValue()
                .await()
            Log.i(TAG, "FCM token cleared successfully for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing FCM token for user $userId", e)
        }
    }
}

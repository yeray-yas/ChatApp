package com.yerayyas.chatappkotlinproject.domain.repository

/**
 * Interface for user data-related operations,
 * including FCM token management.
 */
interface UserRepository {
    /**
     * Updates (or saves if it doesn't exist) the FCM token for the currently logged-in user
     * in the backend database.
     * @param token The new FCM token to save.
     * @throws Exception if an error occurs during the database operation.
     */
    suspend fun updateCurrentUserFCMToken(token: String)

    /**
     * Deletes the FCM token associated with the currently logged-in user
     * from the backend database (useful on logout).
     * @throws Exception if an error occurs during the database operation.
     */
    suspend fun clearCurrentUserFCMToken()
}

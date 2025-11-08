package com.yerayyas.chatappkotlinproject.domain.repository

import com.yerayyas.chatappkotlinproject.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user-related operations.
 * This interface defines methods for managing user data and FCM tokens.
 */
interface UserRepository {

    /**
     * Updates the FCM token for the current authenticated user.
     * @param token The FCM token to save.
     */
    suspend fun updateCurrentUserFCMToken(token: String)

    /**
     * Clears the FCM token for the current authenticated user.
     */
    suspend fun clearCurrentUserFCMToken()

    /**
     * Gets all users available for adding to groups or chats
     */
    suspend fun getAllUsers(): Flow<List<User>>

    /**
     * Gets a specific user by their ID.
     * @param userId The user ID to search for
     * @return User object if found, null otherwise
     */
    suspend fun getUserById(userId: String): User?

    /**
     * Gets the current authenticated user's data.
     * @return Current user's data if authenticated, null otherwise
     */
    suspend fun getCurrentUser(): User?

    /**
     * Searches users by username containing the query string.
     * @param query The search query
     * @return List of users matching the query
     */
    suspend fun searchUsers(query: String): List<User>

    /**
     * Gets multiple users by their IDs.
     * @param userIds List of user IDs to fetch
     * @return List of users found
     */
    suspend fun getUsersByIds(userIds: List<String>): List<User>
}

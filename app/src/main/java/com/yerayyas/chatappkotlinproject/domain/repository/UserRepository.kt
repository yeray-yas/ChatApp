package com.yerayyas.chatappkotlinproject.domain.repository

import com.yerayyas.chatappkotlinproject.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for user management and data operations.
 *
 * This interface defines the core contract for user-related functionality including
 * authentication management, user data retrieval, FCM token handling, and user discovery.
 * It abstracts the data layer implementation while providing a clean domain interface
 * for all user operations throughout the application.
 *
 * Key responsibilities:
 * - **FCM Token Management**: Handle push notification token lifecycle
 * - **User Data Retrieval**: Provide access to user information and profiles
 * - **User Discovery**: Support user search and contact management
 * - **Authentication Integration**: Bridge with authentication services
 * - **Real-time Data**: Enable reactive user data updates through Flow streams
 *
 * The repository follows Clean Architecture principles by:
 * - Using domain models (User) rather than data layer models
 * - Providing platform-agnostic contracts for user operations
 * - Abstracting Firebase and authentication implementation details
 * - Supporting both individual and batch user operations
 *
 * Implementation considerations:
 * - All operations should handle network connectivity gracefully
 * - User data should be cached appropriately for offline access
 * - Privacy considerations should be respected (public vs private data)
 * - Batch operations should be optimized for performance
 */
interface UserRepository {

    /**
     * Updates the FCM (Firebase Cloud Messaging) token for the current authenticated user.
     *
     * This operation is essential for enabling push notifications. The token should be
     * updated whenever the FCM service generates a new token or when the user logs in.
     * The implementation should handle token validation and persistence securely.
     *
     * @param token The FCM token to associate with the current user's account
     * @throws Exception if the token update fails due to network, authentication, or validation issues
     */
    suspend fun updateCurrentUserFCMToken(token: String)

    /**
     * Removes the FCM token for the current authenticated user from storage.
     *
     * This operation is typically performed during user logout to ensure the user
     * no longer receives push notifications after signing out. The implementation
     * should gracefully handle cases where no token exists or the user is not authenticated.
     *
     * Implementation note: This operation should not throw exceptions that could
     * interrupt the logout flow, but should log any errors that occur.
     */
    suspend fun clearCurrentUserFCMToken()

    /**
     * Retrieves a reactive stream of all users available in the system.
     *
     * This method provides access to the complete user directory, useful for
     * user discovery, contact selection, and group member addition. The returned
     * Flow emits updated user lists whenever the underlying data changes.
     *
     * Privacy considerations:
     * - Only public user data should be included in the results
     * - Private information (email, phone) should be filtered appropriately
     * - Users should be able to control their visibility in search results
     *
     * @return Flow emitting lists of all discoverable users with real-time updates
     */
    suspend fun getAllUsers(): Flow<List<User>>

    /**
     * Retrieves detailed information for a specific user by their unique identifier.
     *
     * This method provides access to user profile information necessary for displaying
     * user details, chat headers, and contact information. The implementation should
     * balance between providing necessary information and respecting privacy settings.
     *
     * @param userId The unique identifier of the user to retrieve
     * @return User object containing available profile information, or null if user not found
     */
    suspend fun getUserById(userId: String): User?

    /**
     * Retrieves the profile information for the currently authenticated user.
     *
     * This method provides access to the current user's complete profile including
     * both public and private information since the user has full access to their
     * own data. Essential for profile screens and user settings.
     *
     * @return Current user's complete profile information, or null if not authenticated
     */
    suspend fun getCurrentUser(): User?

    /**
     * Searches for users whose usernames contain the specified query string.
     *
     * This method enables user discovery and contact finding functionality.
     * The search should be case-insensitive and support partial matching for
     * improved user experience.
     *
     * Search considerations:
     * - Case-insensitive matching for better usability
     * - Partial username matching for flexible search
     * - Respect user privacy settings and visibility preferences
     * - Optimize for performance with appropriate caching and indexing
     *
     * @param query The search string to match against usernames
     * @return List of users whose usernames contain the query string
     */
    suspend fun searchUsers(query: String): List<User>

    /**
     * Retrieves multiple users efficiently using their unique identifiers.
     *
     * This method provides batch user retrieval for scenarios like displaying
     * group member lists, chat participant information, or any context where
     * multiple user profiles need to be loaded simultaneously.
     *
     * Performance considerations:
     * - Batch retrieval should be optimized for multiple user requests
     * - Missing users should be handled gracefully without failing the entire operation
     * - Results should maintain the same order as input IDs where possible
     * - Appropriate caching should be used to minimize redundant requests
     *
     * @param userIds List of unique user identifiers to retrieve
     * @return List of user objects for all found users (may be fewer than requested)
     */
    suspend fun getUsersByIds(userIds: List<String>): List<User>
}

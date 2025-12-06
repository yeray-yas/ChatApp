package com.yerayyas.chatappkotlinproject.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.utils.Constants.FCM_TOKENS_PATH
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepositoryImpl"

/**
 * Concrete implementation of [UserRepository] utilizing Firebase services.
 *
 * Handles data operations interacting with:
 * 1. **Firebase Authentication:** To identify the current session.
 * 2. **Firebase Realtime Database:** To store/retrieve user profiles and FCM tokens.
 *
 * **Data Structure Assumption:**
 * This repository assumes a specific DB structure where user data is split into:
 * - `Users/{userId}/public`: Contains publicly visible data (username, photo).
 * - `Users/{userId}/private`: Contains sensitive data (email, status, lastSeen).
 *
 * @property auth The Firebase Authentication instance.
 * @property database The Firebase Realtime Database reference.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: DatabaseReference
) : UserRepository {

    // region FCM Token Management

    /**
     * Persists the current user's FCM token to the Realtime Database.
     *
     * Stores the token at path: `[FCM_TOKENS_PATH]/{userId}/token`.
     * Validates that the user is authenticated and the token is not blank before writing.
     *
     * @param token The Firebase Cloud Messaging token.
     * @throws Exception If the database write operation fails.
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
            throw e
        }
    }

    /**
     * Removes the current user's FCM token.
     *
     * Typically invoked during sign-out to prevent notifications on this device.
     * Errors are logged but suppressed to ensure the logout flow continues smoothly.
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
            Log.e(TAG, "Error clearing FCM token for user $userId", e)
        }
    }

    // endregion

    // region User Data Operations

    /**
     * Provides a real-time stream of all users in the system.
     *
     * This flow emits a new list whenever any user data changes in the DB.
     *
     * **Privacy Note:**
     * The returned [User] objects explicitly exclude sensitive information (like email)
     * for privacy reasons, as this list is intended for public display.
     *
     * @return A [Flow] emitting lists of [User]s. Returns an empty list on error.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllUsers(): Flow<List<User>> {
        return callbackFlow {
            val usersRef = database.child("Users")

            val listener = usersRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val usersList = snapshot.children.mapNotNull { userSnapshot ->
                            try {
                                val userId = userSnapshot.key ?: return@mapNotNull null
                                val publicData = userSnapshot.child("public")
                                val privateData = userSnapshot.child("private")

                                val username =
                                    publicData.child("username").getValue(String::class.java)
                                val profileImage =
                                    publicData.child("profileImage").getValue(String::class.java)

                                // Only map users with valid public profiles
                                if (username != null) {
                                    User(
                                        id = userId,
                                        username = username,
                                        profileImage = profileImage ?: "",
                                        email = "", // Sensitive data hidden
                                        status = privateData.child("status")
                                            .getValue(String::class.java) ?: "offline",
                                        isOnline = privateData.child("status")
                                            .getValue(String::class.java) == "online",
                                        lastSeen = privateData.child("lastSeen")
                                            .getValue(Long::class.java) ?: 0L
                                    )
                                } else null
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing user data: ${e.message}")
                                null
                            }
                        }

                        Log.d(TAG, "Loaded ${usersList.size} users from Firebase")
                        trySend(usersList)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading users: ${e.message}")
                        trySend(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase listener cancelled: ${error.message}")
                    trySend(emptyList())
                }
            })

            awaitClose { usersRef.removeEventListener(listener) }
        }
    }

    /**
     * Retrieves a specific user's public profile by their ID.
     *
     * @param userId The unique identifier of the user.
     * @return The [User] object if found, or `null` if not found or on error.
     * Note: The email field will be empty for privacy.
     */
    override suspend fun getUserById(userId: String): User? {
        return try {
            Log.d(TAG, "Getting user by ID: $userId")
            val snapshot = database.child("Users").child(userId).get().await()

            if (snapshot.exists()) {
                val publicData = snapshot.child("public")
                val privateData = snapshot.child("private")

                val username = publicData.child("username").getValue(String::class.java)
                val profileImage = publicData.child("profileImage").getValue(String::class.java)

                if (username != null) {
                    User(
                        id = userId,
                        username = username,
                        profileImage = profileImage ?: "",
                        email = "", // Sensitive data hidden
                        status = privateData.child("status").getValue(String::class.java)
                            ?: "offline",
                        isOnline = privateData.child("status")
                            .getValue(String::class.java) == "online",
                        lastSeen = privateData.child("lastSeen").getValue(Long::class.java) ?: 0L
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user $userId: ${e.message}")
            null
        }
    }

    /**
     * Retrieves the full profile of the currently authenticated user.
     *
     * Unlike other methods, this **includes sensitive data** (like email) since
     * the user is viewing their own profile.
     *
     * @return The current [User] with full details, or `null` if unauthenticated.
     */
    override suspend fun getCurrentUser(): User? {
        val currentUserId = auth.currentUser?.uid ?: return null

        return try {
            Log.d(TAG, "Getting current user: $currentUserId")
            val snapshot = database.child("Users").child(currentUserId).get().await()

            if (snapshot.exists()) {
                val publicData = snapshot.child("public")
                val privateData = snapshot.child("private")

                val username = publicData.child("username").getValue(String::class.java)
                val profileImage = publicData.child("profileImage").getValue(String::class.java)
                val email = privateData.child("email").getValue(String::class.java)

                if (username != null) {
                    User(
                        id = currentUserId,
                        username = username,
                        profileImage = profileImage ?: "",
                        email = email ?: "", // Include email for own profile
                        status = privateData.child("status").getValue(String::class.java)
                            ?: "offline",
                        isOnline = privateData.child("status")
                            .getValue(String::class.java) == "online",
                        lastSeen = privateData.child("lastSeen").getValue(Long::class.java) ?: 0L
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user: ${e.message}")
            null
        }
    }

    /**
     * Performs a client-side search for users matching the query.
     *
     * **Performance Note:**
     * This method fetches the entire 'Users' node and filters in memory.
     * Efficient for small datasets, but consider server-side indexing (e.g., Algolia)
     * for large scale production use.
     *
     * @param query The string to search for in usernames.
     * @return A list of matching [User] objects.
     */
    override suspend fun searchUsers(query: String): List<User> {
        return try {
            Log.d(TAG, "Searching users with query: $query")
            val snapshot = database.child("Users").get().await()

            val allUsers = snapshot.children.mapNotNull { userSnapshot ->
                try {
                    val userId = userSnapshot.key ?: return@mapNotNull null
                    val publicData = userSnapshot.child("public")

                    val username = publicData.child("username").getValue(String::class.java)
                    val profileImage = publicData.child("profileImage").getValue(String::class.java)

                    if (username != null && username.contains(query, ignoreCase = true)) {
                        User(
                            id = userId,
                            username = username,
                            profileImage = profileImage ?: "",
                            email = "",
                            status = "offline", // Status not relevant for search results
                            isOnline = false,
                            lastSeen = 0L
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            Log.d(TAG, "Found ${allUsers.size} users matching '$query'")
            allUsers
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: ${e.message}")
            emptyList()
        }
    }

    /**
     * Batch retrieves multiple users by their IDs.
     *
     * Useful for populating lists (e.g., chat participants).
     *
     * @param userIds List of user IDs to fetch.
     * @return List of successfully retrieved [User] objects.
     */
    override suspend fun getUsersByIds(userIds: List<String>): List<User> {
        return try {
            Log.d(TAG, "Getting ${userIds.size} users by IDs")
            val users = mutableListOf<User>()

            userIds.forEach { userId ->
                getUserById(userId)?.let { user ->
                    users.add(user)
                }
            }

            Log.d(TAG, "Successfully loaded ${users.size} users")
            users
        } catch (e: Exception) {
            Log.e(TAG, "Error getting users by IDs: ${e.message}")
            emptyList()
        }
    }

    // endregion

    /**
     * Updates the online/offline status of the current user manually.
     * writes to 'Users/{uid}/private' to match the rest of the data structure.
     */
    override suspend fun updateUserStatus(status: String) {
        try {
            val currentUser = auth.currentUser ?: return
            val uid = currentUser.uid

            val updates = mapOf(
                "status" to status,
                "lastSeen" to com.google.firebase.database.ServerValue.TIMESTAMP
            )

            database.child("Users")
                .child(uid)
                .child("private")
                .updateChildren(updates)
                .await()

            Log.d(TAG, "User status updated manually to: $status")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user status manually", e)
            e.printStackTrace()
        }
    }
}
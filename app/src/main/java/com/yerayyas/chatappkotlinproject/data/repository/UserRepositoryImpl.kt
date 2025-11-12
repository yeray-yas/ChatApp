package com.yerayyas.chatappkotlinproject.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.yerayyas.chatappkotlinproject.utils.Constants.FCM_TOKENS_PATH

private const val TAG = "UserRepositoryImpl"

/**
 * Concrete implementation of the [UserRepository] interface.
 *
 * This class interacts with Firebase Authentication to get the current user's ID and with
 * Firebase Realtime Database to manage the user's FCM (Firebase Cloud Messaging) token
 * and user data operations.
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

    // ===== NEW FUNCTIONS FOR REAL USERS =====

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getAllUsers(): Flow<List<User>> {
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

                                // Only create User if it has basic public data
                                val username =
                                    publicData.child("username").getValue(String::class.java)
                                val profileImage =
                                    publicData.child("profileImage").getValue(String::class.java)

                                if (username != null) {
                                    User(
                                        id = userId,
                                        username = username,
                                        profileImage = profileImage ?: "",
                                        email = "", // Do not expose email for other users
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

                        println("DEBUG: UserRepository - Loaded ${usersList.size} users from Firebase")
                        trySend(usersList)
                    } catch (e: Exception) {
                        println("DEBUG: UserRepository - Error loading users: ${e.message}")
                        trySend(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: UserRepository - Firebase cancelled: ${error.message}")
                    trySend(emptyList())
                }
            })

            awaitClose { usersRef.removeEventListener(listener) }
        }
    }

    override suspend fun getUserById(userId: String): User? {
        return try {
            println("DEBUG: UserRepository - Getting user by ID: $userId")
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
                        email = "", // Do not expose email for other users
                        status = privateData.child("status").getValue(String::class.java)
                            ?: "offline",
                        isOnline = privateData.child("status")
                            .getValue(String::class.java) == "online",
                        lastSeen = privateData.child("lastSeen").getValue(Long::class.java) ?: 0L
                    )
                } else null
            } else null
        } catch (e: Exception) {
            println("DEBUG: UserRepository - Error getting user $userId: ${e.message}")
            null
        }
    }

    override suspend fun getCurrentUser(): User? {
        val currentUserId = auth.currentUser?.uid ?: return null

        return try {
            println("DEBUG: UserRepository - Getting current user: $currentUserId")
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
                        email = email ?: "",
                        status = privateData.child("status").getValue(String::class.java)
                            ?: "offline",
                        isOnline = privateData.child("status")
                            .getValue(String::class.java) == "online",
                        lastSeen = privateData.child("lastSeen").getValue(Long::class.java) ?: 0L
                    )
                } else null
            } else null
        } catch (e: Exception) {
            println("DEBUG: UserRepository - Error getting current user: ${e.message}")
            null
        }
    }

    override suspend fun searchUsers(query: String): List<User> {
        return try {
            println("DEBUG: UserRepository - Searching users with query: $query")
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
                            email = "", // Do not expose email for other users
                            status = "offline",
                            isOnline = false,
                            lastSeen = 0L
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            println("DEBUG: UserRepository - Found ${allUsers.size} users matching '$query'")
            allUsers
        } catch (e: Exception) {
            println("DEBUG: UserRepository - Error searching users: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getUsersByIds(userIds: List<String>): List<User> {
        return try {
            println("DEBUG: UserRepository - Getting ${userIds.size} users by IDs")
            val users = mutableListOf<User>()

            userIds.forEach { userId ->
                getUserById(userId)?.let { user ->
                    users.add(user)
                }
            }

            println("DEBUG: UserRepository - Successfully loaded ${users.size} users")
            users
        } catch (e: Exception) {
            println("DEBUG: UserRepository - Error getting users by IDs: ${e.message}")
            emptyList()
        }
    }
}

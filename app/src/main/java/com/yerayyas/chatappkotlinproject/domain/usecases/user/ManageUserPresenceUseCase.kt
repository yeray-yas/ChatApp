package com.yerayyas.chatappkotlinproject.domain.usecases.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import javax.inject.Inject
import javax.inject.Named

/**
 * Manages the user's online presence and activity tracking in Firebase Realtime Database.
 *
 * This use case acts as the bridge between the app's lifecycle and the database presence system.
 * It operates specifically on the node: `Users/{userId}/private`.
 *
 * Key responsibilities:
 * - Sets status to "online" when the app is in foreground.
 * - Schedules an automatic "offline" update using Firebase's [onDisconnect] system.
 * - Updates the `lastSeen` timestamp for activity tracking.
 * - Uses non-destructive updates ([updateChildren]) to preserve other private data like emails.
 */
class ManageUserPresenceUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @param:Named("firebaseDatabaseInstance") private val firebaseDatabase: FirebaseDatabase
) {
    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    /**
     * Starts monitoring and sets the user as "online".
     *
     * This method performs two critical operations atomically:
     * 1. Updates the current status to "online" immediately.
     * 2. Registers an [onDisconnect] hook on the server to automatically mark the user
     * as "offline" if the connection is lost unexpectedly (crash or network loss).
     */
    fun startPresenceUpdates() {
        val userId = currentUserId ?: return
        val userPrivateRef = firebaseDatabase.getReference("Users/$userId/private")

        val onlineData = mapOf(
            "status" to "online",
            "lastSeen" to ServerValue.TIMESTAMP
        )
        // We use updateChildren to avoid overwriting other fields in the 'private' node (e.g. email)
        userPrivateRef.updateChildren(onlineData)

        val offlineData = mapOf(
            "status" to "offline",
            "lastSeen" to ServerValue.TIMESTAMP
        )
        // Prepare the server-side trigger for disconnection
        userPrivateRef.onDisconnect().updateChildren(offlineData)
    }

    /**
     * Explicitly marks the user as "offline".
     *
     * Should be called when the app enters the background or the user logs out.
     * This updates the `lastSeen` timestamp to the current moment.
     */
    fun stopPresenceUpdates() {
        val userId = currentUserId ?: return
        val userPrivateRef = firebaseDatabase.getReference("Users/$userId/private")

        val offlineData = mapOf(
            "status" to "offline",
            "lastSeen" to ServerValue.TIMESTAMP
        )

        userPrivateRef.updateChildren(offlineData)
    }
}

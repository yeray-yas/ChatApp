package com.yerayyas.chatappkotlinproject.domain.usecases.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import javax.inject.Inject
import javax.inject.Named

/**
 * Manages the user's online presence in Firebase Realtime Database.
 *
 * This use case handles setting the user's status to "online" when they are active
 * and uses Firebase's `onDisconnect` mechanism to automatically set their status to
 * "offline" with a timestamp when they disconnect.
 */
class ManageUserPresenceUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @Named("firebaseDatabaseInstance") private val firebaseDatabase: FirebaseDatabase
) {

    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    /**
     * Starts monitoring and updating the user's presence.
     * Sets the user to "online" and prepares an `onDisconnect` operation.
     */
    fun startPresenceUpdates() {
        val userId = currentUserId ?: return
        val userStatusRef = firebaseDatabase.getReference("/status/$userId")

        // Set the user's status to "online"
        userStatusRef.setValue("online")

        // When the user disconnects, set their status to the server timestamp
        userStatusRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
    }

    /**
     * Manually stops the presence updates and sets the user to offline immediately.
     * Useful for explicit sign-out actions.
     */
    fun stopPresenceUpdates() {
        val userId = currentUserId ?: return
        val userStatusRef = firebaseDatabase.getReference("/status/$userId")

        // Manually set the user's status to "offline" timestamp
        userStatusRef.setValue(ServerValue.TIMESTAMP)
    }
}

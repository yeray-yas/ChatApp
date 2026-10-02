package com.yerayyas.chatappkotlinproject.domain.usecases.user

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "ManageUserPresenceUseCase"

/**
 * Manages the user's online presence and activity tracking in Firebase Realtime Database.
 *
 * This use case acts as the bridge between the application's lifecycle and Firebase's real-time
 * presence system. It operates specifically on the node: `Users/{userId}/private`.
 *
 * Key responsibilities:
 * - Monitors socket connection state via Firebase's internal `.info/connected` path.
 * - Re-arms the server-side [onDisconnect] hook on every connection handshake to withstand transient drops.
 * - Sets the user's status to "online" only after the disconnection hook is securely acknowledged by the server.
 * - Explicitly transitions status to "offline" and cancels pending triggers when transitioning to the background.
 * - Uses non-destructive updates ([updateChildren]) to preserve adjacent private attributes such as emails.
 *
 * @property firebaseAuth Firebase Authentication instance used to resolve the current session's UID.
 * @property firebaseDatabase Configured Realtime Database instance targeting the project's root.
 */
@Singleton
class ManageUserPresenceUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @param:Named("firebaseDatabaseInstance") private val firebaseDatabase: FirebaseDatabase
) {
    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    private var connectedListener: ValueEventListener? = null

    /**
     * Starts observing connection state changes and establishes presence hooks.
     *
     * Listens to `.info/connected` to continuously maintain presence:
     * 1. Queues an [onDisconnect] action on the server whenever a connection is established.
     * 2. Sets the local user status to "online" upon successful registration of the disconnect hook.
     *
     * Ensures any pre-existing listener is removed prior to attaching a new one to prevent leaks.
     */
    fun startPresenceUpdates() {
        val userId = currentUserId ?: return
        val userPrivateRef = firebaseDatabase.getReference("Users/$userId/private")
        val connectedRef = firebaseDatabase.getReference(".info/connected")

        // Clean up any previously attached listener to prevent duplicate subscriptions
        connectedListener?.let { connectedRef.removeEventListener(it) }

        connectedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isConnected = snapshot.getValue(Boolean::class.java) ?: false

                if (isConnected) {
                    val offlineData = mapOf(
                        "status" to "offline",
                        "lastSeen" to ServerValue.TIMESTAMP
                    )

                    // 1. Arm server-side disconnect action first to guarantee offline fallback on abrupt termination
                    userPrivateRef.onDisconnect().updateChildren(offlineData).addOnSuccessListener {
                        // 2. Mark online only after the server acknowledges the disconnect instruction
                        val onlineData = mapOf(
                            "status" to "online",
                            "lastSeen" to ServerValue.TIMESTAMP
                        )
                        userPrivateRef.updateChildren(onlineData)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database presence listener cancelled: ${error.message}", error.toException())
            }
        }.also {
            connectedRef.addValueEventListener(it)
        }
    }

    /**
     * Explicitly marks the user as "offline" and releases presence hooks.
     *
     * Invoked when the application transitions to the background:
     * 1. Detaches the `.info/connected` listener to avoid unwanted background reconnection events.
     * 2. Cancels pending server-side [onDisconnect] hooks to prevent race conditions.
     * 3. Explicitly updates database records to "offline" along with the current timestamp.
     */
    fun stopPresenceUpdates() {
        val userId = currentUserId ?: return
        val userPrivateRef = firebaseDatabase.getReference("Users/$userId/private")
        val connectedRef = firebaseDatabase.getReference(".info/connected")

        // 1. Detach connection listener so state changes are ignored while in background
        connectedListener?.let {
            connectedRef.removeEventListener(it)
            connectedListener = null
        }

        // 2. Cancel pending server-side disconnect hook to prevent premature triggers
        userPrivateRef.onDisconnect().cancel()

        // 3. Explicitly mark user as offline and record timestamp
        val offlineData = mapOf(
            "status" to "offline",
            "lastSeen" to ServerValue.TIMESTAMP
        )
        userPrivateRef.updateChildren(offlineData)
    }
}
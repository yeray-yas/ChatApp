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
 * This version is hardened against race conditions and abrupt application termination.
 */
@Singleton
class ManageUserPresenceUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @param:Named("firebaseDatabaseInstance") private val firebaseDatabase: FirebaseDatabase
) {
    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    private var connectedListener: ValueEventListener? = null
    private var isMonitoringActive = false

    fun startPresenceUpdates() {
        val userId = currentUserId ?: return
        if (isMonitoringActive) return
        isMonitoringActive = true

        val userPrivateRef = firebaseDatabase.getReference("Users/$userId/private")
        val connectedRef = firebaseDatabase.getReference(".info/connected")

        connectedListener?.let { connectedRef.removeEventListener(it) }

        connectedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isConnected = snapshot.getValue(Boolean::class.java) ?: false
                
                val sessionUid = firebaseAuth.currentUser?.uid
                if (!isMonitoringActive || sessionUid == null || sessionUid != userId) {
                    cleanupListener()
                    return
                }

                if (isConnected) {
                    val offlineData = mapOf("status" to "offline", "lastSeen" to ServerValue.TIMESTAMP)
                    val onlineData = mapOf("status" to "online", "lastSeen" to ServerValue.TIMESTAMP)

                    // 1. Armamos el seguro PRIMERO.
                    userPrivateRef.onDisconnect().updateChildren(offlineData).addOnSuccessListener {
                        // 2. Solo marcamos online si seguimos en la sesión y monitoreando.
                        if (isMonitoringActive && firebaseAuth.currentUser?.uid == userId) {
                            userPrivateRef.updateChildren(onlineData)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Presence listener cancelled: ${error.message}")
            }
        }.also { connectedRef.addValueEventListener(it) }
    }

    fun stopPresenceUpdates() {
        isMonitoringActive = false
        val userId = currentUserId ?: return
        
        cleanupListener()

        // LA CLAVE: No llamamos a onDisconnect().cancel(). 
        // Lo dejamos como respaldo en el servidor por si el update de abajo no llega a tiempo.

        val userPrivateRef = firebaseDatabase.getReference("Users/$userId/private")
        val offlineData = mapOf("status" to "offline", "lastSeen" to ServerValue.TIMESTAMP)
        
        userPrivateRef.updateChildren(offlineData)
    }

    private fun cleanupListener() {
        connectedListener?.let {
            firebaseDatabase.getReference(".info/connected").removeEventListener(it)
            connectedListener = null
        }
    }
}

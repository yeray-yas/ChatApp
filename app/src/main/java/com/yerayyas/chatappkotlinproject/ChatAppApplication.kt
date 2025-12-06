package com.yerayyas.chatappkotlinproject

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * ChatAppApplication is the application class for the Chat App.
 *
 * This class uses Hilt for dependency injection and implements
 * DefaultLifecycleObserver to update the user's online status
 * in Firebase based on the application's foreground/background state.
 *
 * Dependencies injected:
 * - FirebaseAuth: For authentication and obtaining the current user.
 * - DatabaseReference: For accessing and updating the Firebase Realtime Database.
 *
 * The class observes the lifecycle of the entire application using ProcessLifecycleOwner.
 * When the app goes to the foreground, the user's status is updated to "online",
 * and when it goes to the background, the status is updated to "offline".
 */
@HiltAndroidApp
class ChatAppApplication : Application(), DefaultLifecycleObserver {

    /**
     * Instance of FirebaseAuth for managing user authentication.
     */
    @Inject
    lateinit var auth: FirebaseAuth

    /**
     * Instance of DatabaseReference for accessing the Firebase Realtime Database.
     */
    @Inject
    lateinit var database: DatabaseReference

    /**
     * Flag to track whether the app is currently in the foreground.
     */
    private var isAppInForeground = false

    /**
     * Called when the application is created.
     * Registers this class as a lifecycle observer to track app foreground/background transitions.
     */
    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Called when the application moves to the foreground.
     * Updates the user's status to "online" if not already in the foreground.
     *
     * @param owner The LifecycleOwner (ProcessLifecycleOwner in this case).
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (!isAppInForeground) {
            isAppInForeground = true
            updateUserStatus("online")
        }
    }

    /**
     * Called when the application moves to the background.
     * Updates the user's status to "offline" if currently in the foreground.
     *
     * @param owner The LifecycleOwner (ProcessLifecycleOwner in this case).
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (isAppInForeground) {
            isAppInForeground = false
            updateUserStatus("offline")
        }
    }

    /**
     * Updates the user's status in the Firebase Realtime Database.
     *
     * It updates the "status" and "lastSeen" values under the user's private information in the database.
     * If the update fails, an error message is logged.
     *
     * @param status A String representing the new status, e.g., "online" or "offline".
     */
    private fun updateUserStatus(status: String) {
        auth.currentUser?.uid?.let { uid ->
            database.child("Users").child(uid).child("private").updateChildren(
                mapOf(
                    "status" to status,
                    "lastSeen" to ServerValue.TIMESTAMP
                )
            ).addOnFailureListener { e ->
                Log.e("AppLifecycle", "Error updating user status to $status", e)
            }
        }
    }
}

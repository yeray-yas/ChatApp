package com.yerayyas.chatappkotlinproject.utils

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that tracks the global state of the application.
 * It provides information on whether the app is in the foreground
 * and which chat screen is currently visible (if any).
 * This is crucial for deciding whether to show a push notification.
 */
@Singleton // Ensures there is only one instance throughout the app (thanks to Hilt)
class AppState @Inject constructor() { // Hilt handles the creation

    // Indicates if any app component (Activity) is visible.
    @Volatile // Ensures visibility across threads
    var isAppInForeground: Boolean = false
        private set // Can only be modified from within this class

    // Stores the ID of the user being actively chatted with.
    // It is set from ChatScreen and cleared upon exit. Null if no chat is open.
    @Volatile // Ensures visibility across threads
    var currentOpenChatUserId: String? = null

    // Lifecycle observer for the entire application process
    private val lifecycleEventObserver = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                isAppInForeground = true
                Log.d("AppState", "App entered foreground.")
            }
            Lifecycle.Event.ON_STOP -> {
                isAppInForeground = false
                Log.d("AppState", "App entered background.")
            }
            // Other lifecycle events are not needed for this purpose
            else -> Unit
        }
    }

    init {
        // Register the observer to receive process lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
        Log.d("AppState", "AppState Initialized and Lifecycle Observer added.")
    }

    // Method to clean up the observer if needed (though as a Singleton, it lives with the app)
    // fun cleanup() {
    //     ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleEventObserver)
    // }
}

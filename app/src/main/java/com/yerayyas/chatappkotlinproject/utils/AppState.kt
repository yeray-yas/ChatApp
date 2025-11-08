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
 *
 * Now supports both individual and group chats.
 */
@Singleton // Ensures there is only one instance throughout the app (thanks to Hilt)
class AppState @Inject constructor() { // Hilt handles the creation

    // Indicates if any app component (Activity) is visible.
    @Volatile // Ensures visibility across threads
    var isAppInForeground: Boolean = false
        private set // Can only be modified from within this class

    // Stores the ID of the user being actively chatted with in individual chat.
    // It is set from ChatScreen and cleared upon exit. Null if no individual chat is open.
    @Volatile // Ensures visibility across threads
    var currentOpenChatUserId: String? = null
        set(value) {
            Log.d("AppState", "CurrentOpenChatUserId changed from '${field}' to '$value'")
            field = value
        }

    // Stores the ID of the group being actively chatted with in group chat.
    // It is set from GroupChatScreen and cleared upon exit. Null if no group chat is open.
    @Volatile // Ensures visibility across threads
    var currentOpenGroupChatId: String? = null
        set(value) {
            Log.d("AppState", "CurrentOpenGroupChatId changed from '${field}' to '$value'")
            field = value
        }

    // Lifecycle observer for the entire application process
    private val lifecycleEventObserver = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                isAppInForeground = true
                Log.d(
                    "AppState",
                    "App entered foreground. isAppInForeground=$isAppInForeground, currentOpenChatUserId=$currentOpenChatUserId, currentOpenGroupChatId=$currentOpenGroupChatId"
                )
            }
            Lifecycle.Event.ON_STOP -> {
                isAppInForeground = false
                Log.d(
                    "AppState",
                    "App entered background. isAppInForeground=$isAppInForeground, currentOpenChatUserId=$currentOpenChatUserId, currentOpenGroupChatId=$currentOpenGroupChatId"
                )
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

    /**
     * Checks if a specific individual chat is currently open
     */
    fun isIndividualChatOpen(userId: String): Boolean {
        return currentOpenChatUserId == userId
    }

    /**
     * Checks if a specific group chat is currently open
     */
    fun isGroupChatOpen(groupId: String): Boolean {
        return currentOpenGroupChatId == groupId
    }

    /**
     * Checks if any chat (individual or group) is currently open
     */
    fun isAnyChatOpen(): Boolean {
        return currentOpenChatUserId != null || currentOpenGroupChatId != null
    }

    // Method to clean up the observer if needed (though as a Singleton, it lives with the app)
    // fun cleanup() {
    //     ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleEventObserver)
    // }
}

package com.yerayyas.chatappkotlinproject.utils

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppState"

/**
 * Global application state manager that tracks app lifecycle and active chat sessions.
 *
 * This singleton class serves as the central source of truth for application-wide state
 * management, particularly for determining notification display logic. It follows Clean
 * Architecture principles by providing a clean interface for state management across
 * all application layers.
 *
 * Key responsibilities:
 * - Tracking application foreground/background state
 * - Managing currently active individual chat sessions
 * - Managing currently active group chat sessions
 * - Providing intelligent notification suppression logic
 * - Observing process lifecycle events automatically
 * - Maintaining thread-safe state access across the application
 *
 * Architecture Pattern: Global State Manager (Singleton)
 * - Centralized state management for cross-screen coordination
 * - Thread-safe state access using @Volatile annotations
 * - Automatic lifecycle observation for background/foreground detection
 * - Clean interface for notification decision making
 * - Comprehensive logging for debugging and monitoring
 *
 * Notification Logic Integration:
 * This class is crucial for the notification system, allowing the app to:
 * - Suppress notifications when the user is actively viewing a specific chat
 * - Show notifications when the app is in the background
 * - Handle both individual and group chat notification scenarios
 * - Provide context-aware notification management
 *
 * Thread Safety:
 * All public properties use @Volatile annotation to ensure visibility across threads,
 * which is essential for notification services that may run on background threads.
 *
 * Usage Examples:
 * ```kotlin
 * // Check if individual chat should show notification
 * if (!appState.isAppInForeground || !appState.isIndividualChatOpen(userId)) {
 *     showNotification()
 * }
 *
 * // Set current active chat when user opens chat screen
 * appState.currentOpenChatUserId = userId
 *
 * // Clear active chat when user leaves chat screen
 * appState.currentOpenChatUserId = null
 * ```
 */
@Singleton
class AppState @Inject constructor() {

    /**
     * Indicates whether any activity of the application is currently visible to the user.
     *
     * This property tracks the application's visibility state:
     * - true: App is in foreground (at least one activity is visible)
     * - false: App is in background (all activities are stopped)
     *
     * Used for:
     * - Notification suppression when app is active
     * - Background task coordination
     * - Performance optimizations
     * - Analytics and user engagement tracking
     *
     * Thread Safety: @Volatile ensures visibility across threads
     * Lifecycle: Automatically managed through ProcessLifecycleOwner
     */
    @Volatile
    var isAppInForeground: Boolean = false
        private set

    /**
     * The unique identifier of the user currently being chatted with in individual chat.
     *
     * This property tracks the active individual chat session:
     * - Non-null: User is actively viewing an individual chat with this user ID
     * - null: No individual chat is currently active
     *
     * Used for:
     * - Suppressing notifications for the currently active individual chat
     * - Managing chat-specific UI states
     * - Coordinating with group chat state (mutually exclusive)
     * - Analytics and engagement tracking
     *
     * Management:
     * - Set when ChatScreen becomes active
     * - Cleared when ChatScreen is destroyed or navigated away
     * - Should be cleared when switching to group chat
     *
     * Thread Safety: @Volatile ensures visibility across threads
     * Logging: Changes are automatically logged for debugging
     */
    @Volatile
    var currentOpenChatUserId: String? = null
        set(value) {
            val previousValue = field
            field = value
            logChatStateChange("Individual", "currentOpenChatUserId", previousValue, value)
        }

    /**
     * The unique identifier of the group currently being chatted with in group chat.
     *
     * This property tracks the active group chat session:
     * - Non-null: User is actively viewing a group chat with this group ID
     * - null: No group chat is currently active
     *
     * Used for:
     * - Suppressing notifications for the currently active group chat
     * - Managing group-specific UI states
     * - Coordinating with individual chat state (mutually exclusive)
     * - Group analytics and engagement tracking
     *
     * Management:
     * - Set when GroupChatScreen becomes active
     * - Cleared when GroupChatScreen is destroyed or navigated away
     * - Should be cleared when switching to individual chat
     *
     * Thread Safety: @Volatile ensures visibility across threads
     * Logging: Changes are automatically logged for debugging
     */
    @Volatile
    var currentOpenGroupChatId: String? = null
        set(value) {
            val previousValue = field
            field = value
            logChatStateChange("Group", "currentOpenGroupChatId", previousValue, value)
        }

    /**
     * Lifecycle observer for monitoring the entire application process lifecycle.
     *
     * This observer automatically tracks when the application moves between
     * foreground and background states, updating isAppInForeground accordingly.
     *
     * Lifecycle Events Handled:
     * - ON_START: Application enters foreground (any activity becomes visible)
     * - ON_STOP: Application enters background (all activities are stopped)
     *
     * Other events (ON_CREATE, ON_RESUME, ON_PAUSE, ON_DESTROY) are not needed
     * for application-level state tracking and are ignored for performance.
     */
    private val lifecycleEventObserver = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                isAppInForeground = true
                logAppStateChange("foreground", true)
            }

            Lifecycle.Event.ON_STOP -> {
                isAppInForeground = false
                logAppStateChange("background", false)
            }

            else -> {
                // Other lifecycle events are not relevant for app-level state tracking
            }
        }
    }

    init {
        initializeLifecycleObserver()
    }

    /**
     * Checks if a specific individual chat is currently active and visible to the user.
     *
     * This method provides a clean interface for notification logic to determine
     * whether notifications should be suppressed for a specific individual chat.
     *
     * @param userId The unique identifier of the user to check
     * @return true if the specified individual chat is currently open, false otherwise
     *
     * Usage:
     * ```kotlin
     * if (appState.isIndividualChatOpen(senderId)) {
     *     // Suppress notification - user is viewing this chat
     * } else {
     *     // Show notification - user is not viewing this chat
     * }
     * ```
     */
    fun isIndividualChatOpen(userId: String): Boolean {
        return currentOpenChatUserId == userId
    }

    /**
     * Checks if a specific group chat is currently active and visible to the user.
     *
     * This method provides a clean interface for notification logic to determine
     * whether notifications should be suppressed for a specific group chat.
     *
     * @param groupId The unique identifier of the group to check
     * @return true if the specified group chat is currently open, false otherwise
     *
     * Usage:
     * ```kotlin
     * if (appState.isGroupChatOpen(groupId)) {
     *     // Suppress notification - user is viewing this group chat
     * } else {
     *     // Show notification - user is not viewing this group chat
     * }
     * ```
     */
    fun isGroupChatOpen(groupId: String): Boolean {
        return currentOpenGroupChatId == groupId
    }

    /**
     * Checks if any chat session (individual or group) is currently active.
     *
     * This method is useful for general chat state checking and can be used
     * for UI coordination, analytics, or performance optimizations.
     *
     * @return true if any chat is currently open, false if no chats are active
     *
     * Usage:
     * ```kotlin
     * if (appState.isAnyChatOpen()) {
     *     // User is actively chatting
     *     optimizeForActiveChat()
     * } else {
     *     // User is not in any chat
     *     optimizeForIdleState()
     * }
     * ```
     */
    fun isAnyChatOpen(): Boolean {
        return currentOpenChatUserId != null || currentOpenGroupChatId != null
    }

    /**
     * Provides a summary of the current application state for debugging and monitoring.
     *
     * This method returns a comprehensive state description that includes:
     * - Application foreground/background status
     * - Currently active individual chat (if any)
     * - Currently active group chat (if any)
     * - Overall chat activity status
     *
     * @return A formatted string describing the current application state
     */
    fun getCurrentStateDescription(): String {
        val foregroundStatus = if (isAppInForeground) "foreground" else "background"
        val individualChat = currentOpenChatUserId ?: "none"
        val groupChat = currentOpenGroupChatId ?: "none"
        val anyActive = if (isAnyChatOpen()) "yes" else "no"

        return "AppState(foreground=$foregroundStatus, individualChat=$individualChat, groupChat=$groupChat, anyActive=$anyActive)"
    }

    /**
     * Initializes the lifecycle observer for automatic state management.
     *
     * This method registers the lifecycle observer with ProcessLifecycleOwner
     * to automatically track application foreground/background transitions.
     */
    private fun initializeLifecycleObserver() {
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
            Log.i(
                TAG,
                "AppState initialized with lifecycle observer - initial state: ${getCurrentStateDescription()}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize lifecycle observer", e)
            // Continue without lifecycle observation - app will still function with manual state management
        }
    }

    /**
     * Logs application state changes for debugging and monitoring.
     *
     * @param newState Description of the new state (e.g., "foreground", "background")
     * @param isInForeground The new foreground status
     */
    private fun logAppStateChange(newState: String, isInForeground: Boolean) {
        Log.d(TAG, "App entered $newState - ${getCurrentStateDescription()}")

        // Additional contextual logging
        if (isInForeground && isAnyChatOpen()) {
            Log.d(TAG, "App returned to foreground with active chat session")
        } else if (!isInForeground) {
            Log.d(TAG, "App moved to background - notifications may be shown")
        }
    }

    /**
     * Logs chat state changes for debugging and coordination.
     *
     * @param chatType Type of chat ("Individual" or "Group")
     * @param property Name of the property that changed
     * @param previousValue The previous value (may be null)
     * @param newValue The new value (may be null)
     */
    private fun logChatStateChange(
        chatType: String,
        property: String,
        previousValue: String?,
        newValue: String?
    ) {
        Log.d(TAG, "$chatType chat state changed: $property from '$previousValue' to '$newValue'")

        // Log additional context
        when {
            previousValue == null && newValue != null -> {
                Log.i(TAG, "$chatType chat session started: $newValue")
            }

            previousValue != null && newValue == null -> {
                Log.i(TAG, "$chatType chat session ended: $previousValue")
            }

            previousValue != null && newValue != null -> {
                Log.i(TAG, "$chatType chat session switched: $previousValue -> $newValue")
            }
        }

        Log.d(TAG, "Updated state: ${getCurrentStateDescription()}")
    }

    // Note: Cleanup method is intentionally not implemented as this is a singleton
    // that should live for the entire application lifecycle. The ProcessLifecycleOwner
    // will handle cleanup automatically when the application process terminates.
}

package com.yerayyas.chatappkotlinproject.presentation.activity.services

import android.content.Intent
import android.util.Log
import com.yerayyas.chatappkotlinproject.domain.usecases.ProcessNotificationIntentUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationIntentService"

/**
 * Service responsible for processing notification intents and managing navigation state transitions.
 *
 * This service follows Clean Architecture principles by delegating intent processing logic
 * to the domain layer through the [ProcessNotificationIntentUseCase]. It serves as a coordinator
 * between the Android system (intents) and the presentation layer (ViewModels).
 *
 * Key responsibilities:
 * - Processing initial intents when the activity is created (cold/warm starts)
 * - Handling new intents while the app is running (notification taps)
 * - Coordinating with [MainActivityViewModel] for navigation state management
 * - Managing intent cleanup to prevent reprocessing
 * - Providing comprehensive logging for debugging deep-link navigation
 *
 * Architecture Pattern: Service Layer with Use Case delegation
 * - Uses domain layer use cases for business logic
 * - Maintains separation between system intents and UI navigation
 * - Provides error handling and recovery mechanisms
 * - Supports both individual and group chat navigation flows
 */
@Singleton
class NotificationIntentService @Inject constructor(
    private val processNotificationIntent: ProcessNotificationIntentUseCase
) {

    /**
     * Processes the initial intent when the activity is created.
     *
     * This method handles both cold starts (app not running) and warm starts (app in background).
     * It extracts navigation data from the intent and prepares the initial navigation state
     * for the UI layer to consume.
     *
     * The method includes:
     * - Intent validation and processing through domain layer
     * - State preparation for initial navigation
     * - Splash screen skip logic based on app state
     * - Intent cleanup to prevent reprocessing
     * - Comprehensive logging for debugging
     *
     * @param intent The intent that started the activity (may contain notification data)
     * @param isAppAlreadyRunning Flag indicating if this is a cold start (false) or warm start (true)
     * @return [NotificationNavigationState] if valid navigation data is found, null otherwise
     */
    fun processInitialIntent(
        intent: Intent?,
        isAppAlreadyRunning: Boolean
    ): NotificationNavigationState? {
        Log.d(TAG, "Processing initial intent - App already running: $isAppAlreadyRunning")

        return try {
            // Delegate intent processing to domain layer use case
            processNotificationIntent(intent)?.let { state ->
                // Prepare initial navigation state with app state awareness
                val initialState = state.copy(
                    skipSplash = isAppAlreadyRunning,
                    isInitialDestination = true
                )

                // Log navigation details for debugging
                logNavigationDetails(initialState, "Initial")

                // Clean up intent to prevent reprocessing
                cleanupIntentSafely(intent)

                initialState
            } ?: run {
                Log.d(TAG, "No valid navigation data found in initial intent")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing initial intent", e)
            // Clean up intent even on error to prevent issues
            cleanupIntentSafely(intent)
            null
        }
    }

    /**
     * Handles new intents received while the app is already running.
     *
     * This method processes notification taps when the app is in the background or foreground.
     * It extracts navigation data and queues the navigation request in the ViewModel for
     * the UI layer to handle when appropriate.
     *
     * The method includes:
     * - Intent processing through domain layer
     * - Navigation type detection (individual vs group chat)
     * - ViewModel coordination for pending navigation
     * - Intent cleanup and error handling
     *
     * @param intent The new intent received (typically from notification tap)
     * @param activityViewModel The [MainActivityViewModel] instance for navigation queuing
     */
    fun handleNotificationIntent(intent: Intent?, activityViewModel: MainActivityViewModel) {
        Log.d(TAG, "Handling new notification intent while app is running")

        try {
            // Process intent through domain layer use case
            processNotificationIntent(intent)?.let { state ->
                Log.d(TAG, "Valid navigation state extracted, queuing navigation")

                // Queue navigation based on chat type
                queueNavigationInViewModel(state, activityViewModel)

                // Log navigation details for debugging
                logNavigationDetails(state, "New intent")

                // Clean up intent to prevent reprocessing
                cleanupIntentSafely(intent)

            } ?: run {
                Log.d(TAG, "No valid navigation state could be extracted from new intent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification intent", e)
            // Clean up intent even on error
            cleanupIntentSafely(intent)
        }
    }

    /**
     * Queues the appropriate navigation request in the ViewModel based on chat type.
     *
     * This method centralizes the logic for determining whether to navigate to an individual
     * or group chat and delegates the actual navigation queuing to the ViewModel.
     *
     * @param state The validated [NotificationNavigationState] containing navigation data
     * @param activityViewModel The [MainActivityViewModel] for navigation queuing
     */
    private fun queueNavigationInViewModel(
        state: NotificationNavigationState,
        activityViewModel: MainActivityViewModel
    ) {
        try {
            if (state.isGroupChat) {
                // Queue group chat navigation
                activityViewModel.setPendingGroupNavigation(
                    groupId = state.groupId!!,
                    groupName = state.groupName!!,
                    skipSplash = true
                )
                Log.d(TAG, "Queued group chat navigation for: ${state.groupName}")
            } else {
                // Queue individual chat navigation
                activityViewModel.setPendingNavigation(
                    navigateTo = state.navigateTo,
                    userId = state.userId,
                    username = state.username,
                    skipSplash = true
                )
                Log.d(TAG, "Queued individual chat navigation for: ${state.username}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error queuing navigation in ViewModel", e)
        }
    }

    /**
     * Logs detailed navigation information for debugging purposes.
     *
     * @param state The [NotificationNavigationState] to log
     * @param context Additional context for the log message
     */
    private fun logNavigationDetails(state: NotificationNavigationState, context: String) {
        val chatType = if (state.isGroupChat) "group" else "individual"
        val destinationName = state.destinationName
        val eventId = state.eventId

        Log.d(
            TAG,
            "$context $chatType navigation - Destination: $destinationName, EventId: $eventId"
        )
    }

    /**
     * Safely clears navigation-related extras from the intent to prevent reprocessing.
     *
     * This method includes error handling to ensure that intent cleanup doesn't fail
     * and affect other app operations. It removes both individual and group chat extras.
     *
     * @param intent The intent from which to clear navigation extras
     */
    private fun cleanupIntentSafely(intent: Intent?) {
        try {
            intent?.apply {
                // Clear individual chat extras
                removeExtra("navigateTo")
                removeExtra("userId")
                removeExtra("username")

                // Clear group chat extras
                removeExtra("groupId")
                removeExtra("groupName")
                removeExtra("senderId")
                removeExtra("senderName")

                Log.d(TAG, "Intent extras cleared successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing intent extras", e)
            // Non-critical error, continue execution
        }
    }
}

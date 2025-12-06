package com.yerayyas.chatappkotlinproject.domain.usecases

import android.util.Log
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelUserNotificationsUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationNavigation"

/**
 * Use case responsible for handling navigation triggered by notification interactions.
 *
 * This use case orchestrates the complete flow of notification-driven navigation, including
 * notification cleanup, navigation timing, and proper screen routing. It follows Clean
 * Architecture principles by using domain layer use cases for business logic operations.
 *
 * Key responsibilities:
 * - Canceling notifications that triggered the navigation using domain layer use cases
 * - Managing navigation timing based on app state (cold start vs warm start)
 * - Routing to appropriate chat screens (individual vs group)
 * - Handling navigation errors gracefully to ensure user experience
 * - Coordinating splash screen timing for cold app launches
 *
 * Architecture Pattern: Domain Use Case with Navigation Coordination
 * - Uses other domain use cases for notification management
 * - Handles UI navigation through provided NavController
 * - Manages asynchronous operations with proper coroutine scope
 * - Provides comprehensive error handling and logging
 * - Maintains separation between navigation logic and notification management
 *
 * Navigation scenarios handled:
 * - Cold start: App launched from notification (shows splash, then navigates)
 * - Warm start: App in background, opened via notification (immediate navigation)
 * - Initial destination: Deep-link handled separately during app startup
 * - Individual chats: Direct user-to-user conversation navigation
 * - Group chats: Multi-user group conversation navigation
 *
 * @property cancelUserNotificationsUseCase Domain use case for canceling user-specific notifications
 */
@Singleton
class HandleNotificationNavigationUseCase @Inject constructor(
    private val cancelUserNotificationsUseCase: CancelUserNotificationsUseCase
) {
    /**
     * Executes the complete notification navigation flow.
     *
     * This method orchestrates the entire process of handling notification-triggered navigation:
     * 1. Cancels the relevant notification using domain layer use case
     * 2. Determines navigation timing based on app state
     * 3. Routes to the appropriate chat screen
     * 4. Handles errors gracefully to maintain user experience
     *
     * The navigation behavior adapts based on:
     * - App state (cold start vs warm start)
     * - Chat type (individual vs group)
     * - Initial destination flag (deep-link vs notification tap)
     *
     * @param navController The [NavController] for performing navigation operations
     * @param state The [NotificationNavigationState] containing navigation details
     */
    operator fun invoke(
        navController: NavController,
        state: NotificationNavigationState
    ) {
        Log.d(TAG, "Handling notification navigation: ${state.destinationName}")

        // Cancel the notification that triggered this navigation using domain layer
        cancelRelevantNotification(state)

        // Skip navigation for initial destinations (handled separately during app startup)
        if (state.isInitialDestination) {
            Log.d(TAG, "Skipping navigation - handled as initial destination during app startup")
            return
        }

        // Execute navigation based on app state and timing requirements
        executeNavigation(navController, state)
    }

    /**
     * Cancels the notification that triggered the navigation using domain layer use case.
     *
     * This method handles both individual and group chat notifications, using the appropriate
     * identifier for each type and providing proper error handling to ensure navigation
     * continues even if notification cancellation fails.
     *
     * @param state The navigation state containing notification details
     */
    private fun cancelRelevantNotification(state: NotificationNavigationState) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationId = if (state.isGroupChat) {
                    state.groupId ?: state.userId
                } else {
                    state.userId
                }

                Log.d(
                    TAG,
                    "Canceling ${if (state.isGroupChat) "group" else "individual"} notification for: ${state.destinationName}"
                )

                val result = cancelUserNotificationsUseCase(notificationId)

                if (result.isSuccess) {
                    Log.d(TAG, "Successfully canceled notification for: ${state.destinationName}")
                } else {
                    Log.w(
                        TAG,
                        "Failed to cancel notification for: ${state.destinationName}",
                        result.exceptionOrNull()
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error canceling notification for: ${state.destinationName}", e)
                // Don't throw - navigation should continue even if notification cancellation fails
            }
        }
    }

    /**
     * Executes the navigation based on app state and timing requirements.
     *
     * This method handles two main scenarios:
     * - Immediate navigation: For warm starts when app is already running
     * - Delayed navigation: For cold starts to allow splash screen display
     *
     * @param navController The NavController for navigation operations
     * @param state The navigation state containing destination details
     */
    private fun executeNavigation(
        navController: NavController,
        state: NotificationNavigationState
    ) {
        val destination = createNavigationRoute(state)
        val chatType = if (state.isGroupChat) "group" else "individual"

        if (state.skipSplash) {
            // Immediate navigation for warm app starts
            navigateImmediately(navController, destination, chatType, state.destinationName)
        } else {
            // Delayed navigation for cold app starts (show splash screen first)
            navigateWithSplashDelay(navController, destination, chatType, state.destinationName)
        }
    }

    /**
     * Creates the appropriate navigation route based on chat type.
     *
     * @param state The navigation state containing route parameters
     * @return The complete navigation route string
     */
    private fun createNavigationRoute(state: NotificationNavigationState): String {
        return if (state.isGroupChat) {
            Routes.GroupChat.createRoute(state.groupId!!)
        } else {
            "direct_chat/${state.userId}/${state.username}"
        }
    }

    /**
     * Performs immediate navigation for warm app starts.
     *
     * This method navigates directly to the target screen, typically used when
     * the app is already running and user taps a notification.
     *
     * @param navController The NavController for navigation
     * @param route The destination route
     * @param chatType Description of chat type for logging
     * @param destinationName Display name of destination for logging
     */
    private fun navigateImmediately(
        navController: NavController,
        route: String,
        chatType: String,
        destinationName: String
    ) {
        Log.d(TAG, "Navigating immediately to $chatType chat: $destinationName")

        try {
            navController.navigate(route) {
                // Clear back stack up to start destination without including it
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
            Log.d(TAG, "Successfully navigated to $chatType chat: $destinationName")
        } catch (e: Exception) {
            Log.e(TAG, "Error during immediate navigation to $chatType chat: $destinationName", e)
        }
    }

    /**
     * Performs delayed navigation for cold app starts with splash screen timing.
     *
     * This method waits for the splash screen to display before navigating to the
     * target screen, providing a smooth user experience during cold app launches.
     *
     * @param navController The NavController for navigation
     * @param route The destination route
     * @param chatType Description of chat type for logging
     * @param destinationName Display name of destination for logging
     */
    private fun navigateWithSplashDelay(
        navController: NavController,
        route: String,
        chatType: String,
        destinationName: String
    ) {
        Log.d(TAG, "Scheduling delayed navigation to $chatType chat: $destinationName")

        // Use Main dispatcher for UI operations
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Allow splash screen to display
                delay(1500)

                Log.d(TAG, "Executing delayed navigation to $chatType chat: $destinationName")

                navController.navigate(route) {
                    // Remove splash screen from back stack
                    popUpTo(Routes.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }

                Log.d(
                    TAG,
                    "Successfully completed delayed navigation to $chatType chat: $destinationName"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during delayed navigation to $chatType chat: $destinationName", e)
            }
        }
    }
}

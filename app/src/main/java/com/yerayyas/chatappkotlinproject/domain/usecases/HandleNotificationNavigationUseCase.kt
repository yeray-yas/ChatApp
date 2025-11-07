package com.yerayyas.chatappkotlinproject.domain.usecases

import android.util.Log
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.notifications.NotificationCanceller
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
 * A use case responsible for handling the navigation logic triggered by tapping on a chat notification.
 *
 * Its primary responsibilities are:
 * 1.  Canceling the notification that was tapped.
 * 2.  Determining whether the app is already running or being launched fresh.
 * 3.  Orchestrating the navigation to the correct chat screen, either immediately or after a delay
 *     to allow the splash screen to display.
 *
 * @property notificationCanceller The helper class used to dismiss the relevant notification.
 */
@Singleton
class HandleNotificationNavigationUseCase @Inject constructor(
    private val notificationCanceller: NotificationCanceller
) {
    /**
     * Executes the navigation logic based on the provided [NotificationNavigationState].
     *
     * The process is as follows:
     * - It first cancels the notification associated with the `userId` in the state.
     * - It checks if the navigation is an initial deep-link and skips if so (as it's handled elsewhere).
     * - If `skipSplash` is true, it navigates directly to the chat screen.
     * - If `skipSplash` is false, it launches a coroutine to wait for a short period, allowing the splash
     *   screen to be seen, and then navigates.
     *
     * @param navController The [NavController] used to perform the navigation.
     * @param state The [NotificationNavigationState] containing the details of the destination.
     */
    operator fun invoke(
        navController: NavController,
        state: NotificationNavigationState
    ) {
        Log.d(TAG, "Handling notification navigation state: $state")

        // First, always try to cancel the notification that triggered this navigation.
        try {
            state.userId.let { notificationCanceller.cancelNotificationsForUser(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling notification for user: ${state.userId}", e)
            // Suppress exception to ensure navigation is not blocked.
        }

        // Initial deep-links are handled by a different mechanism on app start.
        if (state.isInitialDestination) {
            Log.d(TAG, "Skipping navigation as it is flagged as an initial destination.")
            return
        }

        val chatRoute = "direct_chat/${state.userId}/${state.username}"

        if (state.skipSplash) {
            // App is already running; navigate to the chat screen immediately.
            Log.d(TAG, "App is active. Navigating directly to: $chatRoute")
            try {
                navController.navigate(chatRoute) {
                    // Pop up to the start destination, but don't pop the start destination itself.
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during direct navigation to chat: ${e.message}", e)
            }
        } else {
            // App is cold-launching; delay navigation to allow the splash screen to be displayed.
            Log.d(TAG, "App is cold-launching. Delaying navigation for splash screen.")
            // Use a Main thread coroutine as navigation must happen on the main thread.
            CoroutineScope(Dispatchers.Main).launch {
                delay(1500) // Wait for splash animation.
                Log.d(TAG, "Splash delay complete. Navigating to: $chatRoute")
                try {
                    navController.navigate(chatRoute) {
                        // Pop up to and including the splash screen to remove it from the back stack.
                        popUpTo(Routes.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during delayed navigation to chat: ${e.message}", e)
                }
            }
        }
    }
}

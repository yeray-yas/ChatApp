package com.yerayyas.chatappkotlinproject.domain.usecases

import android.util.Log
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.notifications.NotificationHelper
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
 * Use case that handles navigation when a chat notification is tapped.
 * It dismisses the tapped notification, skips or waits for the splash screen,
 * and then navigates to the specified chat screen.
 *
 * @property notificationHelper Helper to manage notification cancellation.
 */
@Singleton
class HandleNotificationNavigationUseCase @Inject constructor(
    private val notificationHelper: NotificationHelper
) {
    /**
     * Processes the notification navigation state and performs the required navigation.
     *
     * @param navController Controller used to navigate between routes.
     * @param state State object containing notification deep-link parameters.
     */
    operator fun invoke(
        navController: NavController,
        state: NotificationNavigationState
    ) {
        Log.d(TAG, "Processing notification nav state: $state")

        // Safely cancel the notification for the specified user
        try {
            state.userId.let { notificationHelper.cancelNotificationsForUser(it) }
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException canceling notification", se)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error canceling notification", e)
        }

        // Initial deep links are handled elsewhere; skip if flagged
        if (state.isInitialDestination) {
            Log.d(TAG, "Initial destination - skipping notification navigation: $state")
            return
        }

        val chatRoute = "direct_chat/${state.userId}/${state.username}"

        if (state.skipSplash) {
            // App already running; navigate immediately
            Log.d(TAG, "App active - navigating to chat: $chatRoute")
            try {
                navController.navigate(chatRoute) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
                Log.d(TAG, "Navigation to chat completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to chat: ${e.message}", e)
            }
        } else {
            // App launching; wait for splash then navigate
            Log.d(TAG, "App starting - delaying for splash display")
            CoroutineScope(Dispatchers.Main).launch {
                delay(1500)
                Log.d(TAG, "Splash delay complete - navigating to chat: $chatRoute")
                try {
                    navController.navigate(chatRoute) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    Log.d(TAG, "Navigation to chat completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to chat: ${e.message}", e)
                }
            }
        }
    }
}
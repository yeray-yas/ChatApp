package com.yerayyas.chatappkotlinproject.domain.usecases

import android.util.Log
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.domain.usecases.notification.CancelUserNotificationsUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationNavigation"

/**
 * Use case responsible for handling navigation triggered by notification interactions.
 */
@Singleton
class HandleNotificationNavigationUseCase @Inject constructor(
    private val cancelUserNotificationsUseCase: CancelUserNotificationsUseCase
) {

    /**
     * Executes the notification navigation flow.
     *
     * @param navController The [NavController] for performing navigation operations.
     * @param state The [NotificationNavigationState] containing navigation details.
     */
    operator fun invoke(
        navController: NavController,
        state: NotificationNavigationState
    ) {
        Log.d(TAG, "Handling notification navigation: ${state.destinationName}")
        cancelRelevantNotification(state)
        executeNavigation(navController, state)
    }

    private fun cancelRelevantNotification(state: NotificationNavigationState) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationId = if (state.isGroupChat) state.groupId ?: state.userId else state.userId
                Log.d(TAG, "Canceling notification for: ${state.destinationName}")
                cancelUserNotificationsUseCase(notificationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling notification for: ${state.destinationName}", e)
            }
        }
    }

    private fun executeNavigation(
        navController: NavController,
        state: NotificationNavigationState
    ) {
        val destination = createNavigationRoute(state)
        val chatType = if (state.isGroupChat) "group" else "individual"

        if (state.skipSplash) {
            navigateImmediately(navController, destination, chatType, state.destinationName)
        } else {
            navigateAfterSplash(navController, destination, chatType, state.destinationName)
        }
    }

    private fun createNavigationRoute(state: NotificationNavigationState): String {
        return if (state.isGroupChat) {
            Routes.GroupChat.createRoute(state.groupId!!)
        } else {
            "direct_chat/${state.userId}/${state.username}"
        }
    }

    private fun navigateImmediately(
        navController: NavController,
        route: String,
        chatType: String,
        destinationName: String
    ) {
        Log.d(TAG, "Navigating immediately to $chatType chat: $destinationName")
        try {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during immediate navigation to $chatType chat: $destinationName", e)
        }
    }

    private fun navigateAfterSplash(
        navController: NavController,
        route: String,
        chatType: String,
        destinationName: String
    ) {
        Log.d(TAG, "Executing post-splash navigation to $chatType chat: $destinationName")
        try {
            navController.navigate(route) {
                popUpTo(Routes.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during post-splash navigation to $chatType chat: $destinationName", e)
        }
    }
}

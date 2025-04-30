package com.yerayyas.chatappkotlinproject.domain.usecases

import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles navigation flow when a chat notification is tapped.
 * It clears the splash screen and navigates to Home, then to the target chat screen.
 *
 * @param navController Controller for navigation operations.
 * @param state Navigation parameters from the notification.
 */
@Singleton
class HandleNotificationNavigationUseCase @Inject constructor() {

    operator fun invoke(
        navController: NavController,
        state: NotificationNavigationState
    ) {
        navController.navigate(Routes.Home.route) {
            popUpTo(Routes.Splash.route) { inclusive = true }
            launchSingleTop = true
        }

        val chatRoute = "chat/${state.userId}/${state.username}"
        navController.navigate(chatRoute) {
            launchSingleTop = true
        }
    }
}

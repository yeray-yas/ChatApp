package com.yerayyas.chatappkotlinproject.domain.usecases

import android.util.Log
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.domain.interfaces.AuthenticationService
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DefaultNavigation"

/**
 * Executes the default navigation flow after the splash screen when there is no
 * pending notification deep-link.
 *
 * If the current route is a chat screen, navigation is skipped. Otherwise,
 * it pops the splash screen (if active) and navigates to the Home screen
 * if the user is authenticated, or to the Main screen if not.
 *
 * @constructor Creates a new instance of HandleDefaultNavigationUseCase.
 */
@Singleton
class HandleDefaultNavigationUseCase @Inject constructor(
    private val authService: AuthenticationService
) {

    /**
     * Performs default post-splash navigation logic.
     *
     * @param navController Controller responsible for app navigation.
     * @param currentRoute The route currently displayed, or null if unknown.
     */
    operator fun invoke(
        navController: NavController,
        currentRoute: String?
    ) {
        // Skip navigation if already viewing a chat
        if (currentRoute?.startsWith("chat/") == true) {
            Log.d(TAG, "Already on chat route: $currentRoute. Skipping default navigation.")
            return
        }

        val isUserAuthenticated = authService.isUserAuthenticated()
        Log.d(TAG, "Default navigation: isUserAuthenticated=$isUserAuthenticated, currentRoute=$currentRoute")

        // Only navigate if still on the splash or no route is set
        if (currentRoute == Routes.Splash.route || currentRoute == null) {
            val targetRoute =
                if (isUserAuthenticated) Routes.Home.createRoute() else Routes.Main.route
            navController.navigate(targetRoute) {
                popUpTo(Routes.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

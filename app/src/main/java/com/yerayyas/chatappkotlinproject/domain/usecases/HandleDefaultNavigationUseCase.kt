package com.yerayyas.chatappkotlinproject.domain.usecases

import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles navigation after the splash screen when there is no
 * pending notification deep-link.
 *
 * Pops the splash route if still active, then navigates to Home
 * if the user is authenticated or to Main otherwise.
 */
@Singleton
class HandleDefaultNavigationUseCase @Inject constructor() {

    /**
     * Executes default post-splash navigation.
     *
     * @param navController Controller used to perform navigation.
     * @param isUserAuthenticated True if the user is logged in.
     * @param currentRoute The route currently displayed (may be null).
     */
    operator fun invoke(
        navController: NavController,
        isUserAuthenticated: Boolean,
        currentRoute: String?
    ) {
        if (currentRoute == Routes.Splash.route || currentRoute == null) {
            val targetRoute = if (isUserAuthenticated) Routes.Home.route else Routes.Main.route
            navController.navigate(targetRoute) {
                popUpTo(Routes.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

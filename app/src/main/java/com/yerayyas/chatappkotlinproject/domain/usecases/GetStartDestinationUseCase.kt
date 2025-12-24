package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.domain.interfaces.AuthenticationService
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import javax.inject.Inject

/**
 * Domain Use Case responsible for determining the application's entry point.
 *
 * This class encapsulates the decision logic for the initial navigation route when the app is launched.
 * It acts as a central router that prioritizes deep links (notifications), security (authentication),
 * and user experience (splash screen).
 *
 * Decision Hierarchy (in order of priority):
 * 1. **Deep Linking:** If the app was opened via a notification, navigate directly to the target chat.
 * 2. **Security:** If the user is not authenticated, force navigation to the Login screen.
 * 3. **UX/Loading:** If it's a cold start, show the Splash screen (unless explicitly skipped).
 * 4. **Default:** If none of the above apply, navigate to the Home screen.
 */
class GetStartDestinationUseCase @Inject constructor(
    private val authService: AuthenticationService
) {

    /**
     * Calculates the start destination route based on current app state.
     *
     * @param initialNavState The navigation state derived from a notification intent (nullable).
     * @param skipSplash Flag indicating if the Splash screen should be bypassed (e.g., warm start).
     * @return A valid route string corresponding to [Routes].
     */
    operator fun invoke(
        initialNavState: NotificationNavigationState?,
        skipSplash: Boolean
    ): String {
        // 1. High Priority: Deep Linking via Notifications
        // If the app is launched from a notification, we prioritize that destination.
        if (initialNavState?.isInitialDestination == true) {
            return if (initialNavState.isGroupChat) {
                Routes.GroupChat.createRoute(initialNavState.groupId!!)
            } else {
                "direct_chat/${initialNavState.userId}/${initialNavState.username}"
            }
        }

        // 2. Security Check: Unauthenticated User -> Login
        // If the user has no valid session, they are redirected to the onboarding flow.
        if (!authService.isUserAuthenticated()) {
            return Routes.Login.route
        }

        // 3. UX Flow: Splash Screen Logic
        // Note: While modern architectures often handle Splash as a loading state outside the NavHost,
        // this logic maintains it as a route for legacy support or specific design requirements.
        if (!skipSplash) {
            return Routes.Splash.route
        }

        // 4. Default: Home
        // Standard entry point for an authenticated user on a warm start.
        return Routes.Home.route
    }
}

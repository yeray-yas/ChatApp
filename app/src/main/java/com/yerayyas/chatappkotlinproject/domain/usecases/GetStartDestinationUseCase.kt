package com.yerayyas.chatappkotlinproject.domain.usecases

import com.yerayyas.chatappkotlinproject.domain.interfaces.AuthenticationService
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import javax.inject.Inject

/**
 * Domain Use Case responsible for determining the application's entry point.
 *
 * This class encapsulates the decision logic for the initial navigation route when the app is launched.
 * It acts as a central router that prioritizes the user experience (splash screen) on cold starts,
 * then handles deep links and authentication for warm starts.
 *
 * Decision Hierarchy (in order of priority):
 * 1. **UX/Loading:** On a cold start, always show the Splash screen first.
 * 2. **Deep Linking:** On a warm start from a notification, navigate directly to the target chat.
 * 3. **Security:** On a warm start, if the user is not authenticated, navigate to the Main screen.
 * 4. **Default:** On a warm start for an authenticated user, navigate to the Home screen.
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
        // 1. UX Flow: Splash Screen Logic on Cold Start
        // On a cold start, always show the splash screen first, regardless of other states.
        if (!skipSplash) {
            return Routes.Splash.route
        }

        // --- The following logic applies only to WARM STARTS ---

        // 2. High Priority (Warm Start): Deep Linking via Notifications
        // If the app is launched from a notification, we prioritize that destination.
        if (initialNavState?.isInitialDestination == true) {
            return if (initialNavState.isGroupChat) {
                Routes.GroupChat.createRoute(initialNavState.groupId!!)
            } else {
                "direct_chat/${initialNavState.userId}/${initialNavState.username}"
            }
        }

        // 3. Security Check (Warm Start): Unauthenticated User -> Main
        // If the user has no valid session, they are redirected to the onboarding flow.
        if (!authService.isUserAuthenticated()) {
            return Routes.Main.route
        }

        // 4. Default (Warm Start): Home
        // Standard entry point for an authenticated user.
        return Routes.Home.createRoute()
    }
}

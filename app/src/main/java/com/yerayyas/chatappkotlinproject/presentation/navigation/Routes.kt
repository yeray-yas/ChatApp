package com.yerayyas.chatappkotlinproject.presentation.navigation


import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import com.yerayyas.chatappkotlinproject.utils.Constants.ANIMATION_DURATION

/**
 * Represents the different navigation routes in the application using a sealed class.
 *
 * Each route corresponds to a screen in the app, optionally supporting custom enter
 * and exit transitions for animated navigation. Routes with dynamic parameters include
 * helper methods for generating the full navigation path.
 *
 * @property route The route string used for navigation.
 * @property enterTransition Optional custom enter transition for animated navigation.
 * @property exitTransition Optional custom exit transition for animated navigation.
 */
sealed class Routes(
    val route: String,
    val enterTransition: (AnimatedContentTransitionScope<*>.() -> EnterTransition?)? = null,
    val exitTransition: (AnimatedContentTransitionScope<*>.() -> ExitTransition?)? = null
) {
    /** Splash screen shown on app launch. */
    data object Splash : Routes("splash")

    /** Main screen of the app with custom transitions. */
    data object Main : Routes(
        route = "main",
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(ANIMATION_DURATION)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(ANIMATION_DURATION)
            )
        }
    )

    /** User registration screen. */
    data object SignUp : Routes("signup_screen")

    /** User login screen. */
    data object Login : Routes("login_screen")

    /** Home screen after successful login or registration. */
    data object Home : Routes("home_screen")

    /** Screen for viewing the current user's profile. */
    data object UserProfile : Routes("user_profile")

    /** Screen for editing the user's profile information with custom transitions. */
    data object EditUserProfile : Routes(
        route = "edit_user_profile",
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(ANIMATION_DURATION)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(ANIMATION_DURATION)
            )
        }
    )

    /** Screen for confirming the selected profile photo. */
    data object ConfirmPhoto : Routes("confirm_photo")

    /**
     * Screen for a one-to-one chat.
     *
     * @param userId The ID of the user to chat with.
     * @param username The display name of the user (optional).
     */
    data object Chat : Routes("chat/{userId}?username={username}") {
        fun createRoute(userId: String, username: String = "User") = "chat/$userId?username=$username"
    }

    /**
     * Screen for viewing another user's profile.
     *
     * @param userId The ID of the user whose profile is being viewed.
     * @param username The display name of the user (optional).
     */
    data object OtherUsersProfile : Routes("other_profile/{userId}?username={username}") {
        fun createRoute(userId: String, username: String = "User") = "other_profile/$userId?username=$username"
    }
}
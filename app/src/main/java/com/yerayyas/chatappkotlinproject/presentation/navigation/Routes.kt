package com.yerayyas.chatappkotlinproject.presentation.navigation


import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween

sealed class Routes(
    val route: String,
    val enterTransition: (AnimatedContentTransitionScope<*>.() -> EnterTransition?)? = null,
    val exitTransition: (AnimatedContentTransitionScope<*>.() -> ExitTransition?)? = null
) {
    data object Splash : Routes("splash")
    data object Main : Routes(
        route = "main",
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    )
    data object SignUp : Routes("signup_screen")
    data object Login : Routes("login_screen")
    data object Home : Routes("home_screen")
    data object UserProfile : Routes("user_profile")
    data object EditUserProfile : Routes(
        route = "edit_user_profile",
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    )
    data object ConfirmPhoto : Routes("confirm_photo")
    data object Chat : Routes("chat/{userId}/{username}") {
        fun createRoute(userId: String, username: String = "User") =
            "chat/$userId/${Uri.encode(username)}"
    }
}


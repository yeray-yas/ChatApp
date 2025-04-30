package com.yerayyas.chatappkotlinproject.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleDefaultNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleNotificationNavigationUseCase
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.LoginScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.SignUpScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.ChatScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.FullScreenImageScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.home.HomeScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.main.MainScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.ConfirmProfilePhotoScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.EditUserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.OtherUsersProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.UserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.splash.SplashScreen
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.main.MainScreenViewModel

/**
 * Hosts the app's navigation graph and handles deep-links such as notification taps.
 *
 * @param navController Controller for navigation actions.
 * @param mainActivityViewModel ViewModel exposing pending navigation state.
 * @param handleNotificationNavigation Use case for handling notification-triggered navigation.
 * @param startDestination Initial route of the NavHost; defaults to the splash screen.
 */
@Composable
fun NavigationWrapper(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainActivityViewModel: MainActivityViewModel,
    handleNotificationNavigation: HandleNotificationNavigationUseCase,
    handleDefaultNavigation: HandleDefaultNavigationUseCase,
    startDestination: String = Routes.Splash.route
) {
    var hasShownSplash by rememberSaveable { mutableStateOf(false) }
    val mainScreenViewModel: MainScreenViewModel = hiltViewModel()
    val isUserAuthenticated by mainScreenViewModel.isUserAuthenticated.collectAsState()
    val pendingNavState by mainActivityViewModel.pendingNavigation.collectAsState()

    // Handle notification tap navigation
    LaunchedEffect(pendingNavState) {
        pendingNavState?.let { state: NotificationNavigationState ->
            handleNotificationNavigation(navController, state)
            mainActivityViewModel.clearPendingNavigation()
        }
    }

    // Default navigation after splash, only if no pending notification
    LaunchedEffect(hasShownSplash, pendingNavState, isUserAuthenticated) {
        if (hasShownSplash && pendingNavState == null) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            handleDefaultNavigation(
                navController    = navController,
                isUserAuthenticated = isUserAuthenticated,
                currentRoute     = currentRoute
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Splash.route) { SplashScreen { hasShownSplash = true } }
        composable(Routes.Main.route) { MainScreen(navController, hiltViewModel()) }
        composable(Routes.SignUp.route) { SignUpScreen(navController) }
        composable(Routes.Login.route) { LoginScreen(navController) }
        composable(Routes.Home.route) { HomeScreen(navController) }
        composable(Routes.UserProfile.route) { UserProfileScreen(navController) }
        composable(Routes.EditUserProfile.route) { EditUserProfileScreen(navController) }
        composable(Routes.ConfirmPhoto.route) { ConfirmProfilePhotoScreen(navController) }

        composable(
            route = "chat/{userId}/{username}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val username = backStackEntry.arguments?.getString("username") ?: return@composable

            ChatScreen(
                navController = navController,
                userId        = userId,
                username      = username
            )
        }


        composable("fullScreenImage/{imageId}") {
            val imageId = it.arguments?.getString("imageId") ?: return@composable
            FullScreenImageScreen(navController, imageId)
        }

        composable(
            route = Routes.OtherUsersProfile.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("username") {
                    type = NavType.StringType
                    defaultValue = "User"
                }
            )
        ) {
            val userId = it.arguments?.getString("userId")!!
            val username = it.arguments?.getString("username") ?: "User"
            OtherUsersProfileScreen(navController, userId, username)
        }
    }
}
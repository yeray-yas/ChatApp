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
 * Hosts the app's navigation graph and handles notification-driven and default navigation.
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

    // Authentication state
    val mainScreenViewModel: MainScreenViewModel = hiltViewModel()
    val isUserAuthenticated by mainScreenViewModel.isUserAuthenticated.collectAsState()

    // Collect one-shot notification navigation events
    LaunchedEffect(Unit) {
        mainActivityViewModel.pendingNavigation.collect { state ->
            handleNotificationNavigation(navController, state)
        }
    }

    // Handle default navigation once splash is shown and no immediate notification is in flight
    LaunchedEffect(hasShownSplash, isUserAuthenticated) {
        if (hasShownSplash) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            handleDefaultNavigation(
                navController = navController,
                isUserAuthenticated = isUserAuthenticated,
                currentRoute = currentRoute
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.Splash.route) {
            SplashScreen { hasShownSplash = true }
        }
        composable(Routes.Main.route) {
            MainScreen(navController, hiltViewModel())
        }
        composable(Routes.SignUp.route) {
            SignUpScreen(navController)
        }
        composable(Routes.Login.route) {
            LoginScreen(navController)
        }
        composable(Routes.Home.route) {
            HomeScreen(navController)
        }
        composable(Routes.UserProfile.route) {
            UserProfileScreen(navController)
        }
        composable(Routes.EditUserProfile.route) {
            EditUserProfileScreen(navController)
        }
        composable(Routes.ConfirmPhoto.route) {
            ConfirmProfilePhotoScreen(navController)
        }
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
                userId = userId,
                username = username
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
package com.yerayyas.chatappkotlinproject.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.LoginScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.SignUpScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.ChatScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.home.HomeScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.intro.MainScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.ConfirmProfilePhotoScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.EditUserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.UserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.splash.SplashScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.FullScreenImageScreen
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.main.MainScreenViewModel

@Composable
fun NavigationWrapper(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = Routes.Splash.route,
) {
    val mainScreenViewModel: MainScreenViewModel = hiltViewModel()
    var hasShownSplash by rememberSaveable { mutableStateOf(false) }
    val isUserAuthenticated by mainScreenViewModel.isUserAuthenticated.collectAsState()

    LaunchedEffect(hasShownSplash, isUserAuthenticated) {
        if (hasShownSplash) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Routes.Main.route) {
                navController.navigate(if (isUserAuthenticated) Routes.Home.route else Routes.Main.route) {
                    popUpTo(Routes.Splash.route) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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
            route = "chat/{userId}?username={username}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType; defaultValue = "User" }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!
            val username = backStackEntry.arguments?.getString("username") ?: "User"
            ChatScreen(userId = userId, username = username, navController = navController)
        }
        composable("chat/{userId}/{username}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val username = backStackEntry.arguments?.getString("username") ?: return@composable
            ChatScreen(navController = navController, userId = userId, username = username)
        }
        composable("fullScreenImage/{imageId}") { backStackEntry ->
            val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable
            FullScreenImageScreen(navController = navController, imageId = imageId)
        }
    }
}



package com.yerayyas.chatappkotlinproject.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yerayyas.chatappkotlinproject.Routes
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.ChatScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.ConfirmProfilePhotoScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.EditUserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.home.HomeScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.LoginScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.intro.MainScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.SignUpScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.splash.SplashScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.UserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.MainScreenViewModel

@Composable
fun NavigationWrapper(navController: NavHostController, modifier: Modifier = Modifier) {
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    var hasShownSplash by rememberSaveable  { mutableStateOf(false) }
    val isUserAuthenticated by mainScreenViewModel.isUserAuthenticated.collectAsState()
    val currentSplashState by rememberUpdatedState(hasShownSplash)

    LaunchedEffect(isUserAuthenticated, currentSplashState) {
        when {
            !currentSplashState -> navController.navigate(Routes.Splash.route)
            isUserAuthenticated -> navController.navigate(Routes.Home.route) { popUpTo(0) }
            else -> navController.navigate(Routes.Main.route) { popUpTo(0) }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route,
        modifier = modifier
    ) {
        composable(Routes.Splash.route) {
            SplashScreen { hasShownSplash = true }
        }
        composable(
            route = Routes.Main.route,
            enterTransition = { Routes.Main.enterTransition?.invoke(this) },
            exitTransition = { Routes.Main.exitTransition?.invoke(this) }
        ) {
            MainScreen(navController, viewModel())
        }
        composable(Routes.SignUp.route) { SignUpScreen(navController) }
        composable(Routes.Login.route) { LoginScreen(navController) }
        composable(Routes.Home.route) { HomeScreen(navController) }
        composable(Routes.UserProfile.route) { UserProfileScreen(navController) }
        composable(Routes.EditUserProfile.route) { EditUserProfileScreen(
            navController
        ) }

        // Agregamos la ruta para la pantalla de confirmación de foto
        composable(Routes.ConfirmPhoto.route) {
            ConfirmProfilePhotoScreen(navController)
        }

        composable(
            route = "chat/{userId}?username={username}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType; defaultValue = "User" }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!
            val username = backStackEntry.arguments?.getString("username") ?: "User"

            ChatScreen(
                userId = userId,
                username = username,
                navController = navController,
                chatViewModel = viewModel()
            )
        }
    }
}


package com.yerayyas.chatappkotlinproject.presentation.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.home.HomeViewModel
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.main.MainScreenViewModel

@Composable
fun NavigationWrapper(navController: NavHostController, modifier: Modifier = Modifier) {
    val mainScreenViewModel: MainScreenViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()

    val isUserAuthenticated by mainScreenViewModel.isUserAuthenticated.collectAsState()
    val isHomeDataLoaded by homeViewModel.isLoading.collectAsState()

    // 1) Agregamos un LaunchedEffect que escucha cambios en isUserAuthenticated
    LaunchedEffect(isUserAuthenticated) {
        // 2) Si el usuario YA NO está autenticado y NO estamos en MainScreen,
        //    navegamos a MainScreen
        if (!isUserAuthenticated) {
            // Ojo con popUpTo: decide si quieres “limpiar” el back stack hasta Splash o Main
            navController.navigate(Routes.Main.route) {
                // Elimina absolutamente tod o lo anterior del stack
                popUpTo(0)
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.Splash.route, modifier = modifier) {
        composable(Routes.Splash.route) {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                },
                isUserAuthenticated = isUserAuthenticated,
                isHomeDataLoaded = !isHomeDataLoaded
            )
        }
        composable(Routes.Main.route) { MainScreen(navController, hiltViewModel()) }
        composable(Routes.SignUp.route) { SignUpScreen(navController) }
        composable(Routes.Login.route) { LoginScreen(navController) }
        composable(Routes.Home.route) { HomeScreen(navController) }
        composable(Routes.UserProfile.route) { UserProfileScreen(navController) }
        composable(Routes.EditUserProfile.route) { EditUserProfileScreen(navController) }
        composable(Routes.ConfirmPhoto.route) { ConfirmProfilePhotoScreen(navController) }
        composable(
            route = Routes.Chat.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: run {
                Log.e("Navigation", "No userId found in arguments")
                return@composable
            }
            val username = Uri.decode(backStackEntry.arguments?.getString("username") ?: "User")

            ChatScreen(userId = userId, username = username, navController = navController, chatViewModel = hiltViewModel())
        }
    }
}




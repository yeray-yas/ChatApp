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
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.LoginScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.SignUpScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.ChatScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.home.HomeScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.main.MainScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.ConfirmProfilePhotoScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.EditUserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.UserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.splash.SplashScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.FullScreenImageScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.OtherUsersProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.main.MainScreenViewModel
import android.util.Log // Import Log

private const val NAV_WRAPPER_TAG = "NavigationWrapper" // Tag para logs

/**
 * A composable function that wraps the navigation logic of the application using Jetpack Compose Navigation.
 *
 * This function defines the navigation graph for the app, setting up all the composable destinations
 * and handling navigation logic after the splash screen based on user authentication status.
 *
 * @param modifier Optional [Modifier] to be applied to the NavHost.
 * @param navController The [NavHostController] used for navigating between composable destinations.
 * @param startDestination The route where navigation starts; defaults to [Routes.Splash.route].
 */
@Composable
fun NavigationWrapper(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainActivityViewModel: MainActivityViewModel,
    startDestination: String = Routes.Splash.route,
) {
    val mainScreenViewModel: MainScreenViewModel = hiltViewModel()
    var hasShownSplash by rememberSaveable { mutableStateOf(false) }
    val isUserAuthenticated by mainScreenViewModel.isUserAuthenticated.collectAsState()

    val pendingNavState by mainActivityViewModel.pendingNavigation.collectAsState()

    LaunchedEffect(pendingNavState) { // Se relanza si pendingNavState cambia
        pendingNavState?.let { state ->
            Log.d(NAV_WRAPPER_TAG, "LaunchedEffect: Detected pending navigation state: $state")
            if (state.navigateTo == "chat" && state.userId != null && state.username != null) {
                try {
                    // Asegúrate que la ruta construida coincide EXACTAMENTE con la definición en NavHost
                    val route = "chat/${state.userId}/${state.username}"
                    Log.d(NAV_WRAPPER_TAG, "Navigating to chat route: $route")
                    navController.navigate(route) {
                        launchSingleTop = true
                        // Considera popUpTo si quieres limpiar el backstack al llegar desde notificación
                        // popUpTo(Routes.Home.route) { inclusive = false }
                    }
                } catch (e: Exception) {
                    Log.e(NAV_WRAPPER_TAG, "Error navigating from pending state: ${e.message}", e)
                } finally {
                    // --- IMPORTANTE: Limpiar el estado después de procesarlo ---
                    Log.d(NAV_WRAPPER_TAG, "Clearing pending navigation state.")
                    mainActivityViewModel.clearPendingNavigation()
                }
            } else {
                // Limpiar si el estado es inválido o no es de chat (por si acaso)
                Log.d(NAV_WRAPPER_TAG, "Pending state not for chat or invalid, clearing anyway.")
                mainActivityViewModel.clearPendingNavigation()
            }
        }
    }

    LaunchedEffect(hasShownSplash, isUserAuthenticated) {
        if (hasShownSplash) {
            // Solo navega si no hay una navegación pendiente por notificación
            // O decide qué tiene prioridad
            if (pendingNavState == null) { // <- Añade esta comprobación si la notificación tiene prioridad
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                // Evita navegar si ya estás en una ruta interna o si acabas de navegar por notificación
                if (currentRoute == Routes.Splash.route || currentRoute == null /* Podría ser null inicialmente */) {
                    Log.d(NAV_WRAPPER_TAG, "Post-splash navigation: Authenticated=$isUserAuthenticated")
                    navController.navigate(if (isUserAuthenticated) Routes.Home.route else Routes.Main.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                } else {
                    Log.d(NAV_WRAPPER_TAG, "Post-splash navigation skipped, current route: $currentRoute")
                }
            } else {
                Log.d(NAV_WRAPPER_TAG, "Post-splash navigation skipped due to pending notification navigation.")
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
            route = "chat/{userId}/{username}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val username = backStackEntry.arguments?.getString("username") ?: return@composable
            Log.d(NAV_WRAPPER_TAG, "Composing ChatScreen for userId=$userId, username=$username")
            ChatScreen(navController = navController, userId = userId, username = username)
        }

        composable("fullScreenImage/{imageId}") { backStackEntry ->
            val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable
            FullScreenImageScreen(navController = navController, imageId = imageId)
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
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")!!
            val username = backStackEntry.arguments?.getString("username") ?: "User"
            OtherUsersProfileScreen(
                navController = navController,
                userId = userId,
                username = username
            )
        }
    }
}


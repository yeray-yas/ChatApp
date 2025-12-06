package com.yerayyas.chatappkotlinproject.presentation.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
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
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.LoginScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.auth.SignUpScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.FullScreenImageScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.GroupChatScreenUnified
import com.yerayyas.chatappkotlinproject.presentation.screens.chat.IndividualChatScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.group.CreateGroupScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.group.GroupInfoScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.group.GroupListScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.home.HomeScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.main.MainScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.ConfirmProfilePhotoScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.EditUserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.OtherUsersProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.profile.UserProfileScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.search.SearchScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.settings.SettingsScreen
import com.yerayyas.chatappkotlinproject.presentation.screens.splash.SplashScreen
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.main.MainScreenViewModel

/**
 * Composable hosting the app's navigation graph.
 * Handles splash display, authentication state, notification-driven and default navigation.
 *
 * @param modifier UI modifier applied to NavHost
 * @param navController Controller for navigation operations
 * @param mainActivityViewModel ViewModel managing pending navigation events
 * @param handleNotificationNavigation Use case to process notification navigation
 * @param handleDefaultNavigation Use case to process default navigation
 * @param skipSplash If true, bypasses splash screen display
 * @param initialNavState Optional initial navigation state from notifications
 * @param startDestination Initial route for NavHost
 */
@Composable
fun NavigationWrapper(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainActivityViewModel: MainActivityViewModel,
    handleNotificationNavigation: HandleNotificationNavigationUseCase,
    handleDefaultNavigation: HandleDefaultNavigationUseCase,
    skipSplash: Boolean = false,
    initialNavState: NotificationNavigationState? = null,
    startDestination: String = determineStartDestination(skipSplash, initialNavState)
) {
    var hasShownSplash by rememberSaveable { mutableStateOf(skipSplash) }
    var isHandlingNotification by rememberSaveable { mutableStateOf(initialNavState != null) }

    val mainScreenViewModel: MainScreenViewModel = hiltViewModel()
    val isUserAuthenticated by mainScreenViewModel.isUserAuthenticated.collectAsState()
    val pendingNavigation by mainActivityViewModel.pendingNavigation.collectAsState()

    LaunchedEffect(pendingNavigation) {
        Log.d("NavigationWrapper", "Pending navigation: $pendingNavigation")
        pendingNavigation?.let { state ->
            isHandlingNotification = true
            handleNotificationNavigation(navController, state)
            mainActivityViewModel.clearPendingNavigation()
        }
    }

    LaunchedEffect(hasShownSplash, isUserAuthenticated) {
        if (hasShownSplash && !isHandlingNotification && pendingNavigation == null) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            Log.d("NavigationWrapper", "Processing default navigation, currentRoute: $currentRoute")
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
        // --- Group screens routes ---
        composable(Routes.CreateGroup.route) {
            CreateGroupScreen(
                onNavigateBack = { navController.popBackStack() },
                onGroupCreated = { groupId ->
                    // Navigates to the new group chat
                    navController.navigate(Routes.GroupChat.createRoute(groupId)) {
                        popUpTo(Routes.CreateGroup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.GroupChat.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId")
            if (groupId != null) {
                GroupChatScreenUnified(
                    groupId = groupId,
                    navController = navController
                )
            } else {
                Log.e("NavGraph", "groupId is null for GroupChat route, navigating back.")
                navController.popBackStack()
            }
        }

        composable(
            route = Routes.GroupChat.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupChatScreenUnified(
                groupId = groupId,
                navController = navController
            )

            BackHandler {
                // Ensure we always have a way back to Home
                if (!navController.popBackStack()) {
                    // If popBackStack returns false (no previous destination), navigate to Home with Groups tab selected
                    navController.navigate(Routes.Home.createRoute(2)) {
                        // Clear entire back stack and make Home the new root
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(
            route = Routes.GroupInfo.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupInfoScreen(groupId = groupId, navController = navController)
        }

        composable(Routes.GroupList.route) {
            GroupListScreen(
                navController = navController,
            )
        }

        composable(Routes.Splash.route) {
            SplashScreen {
                hasShownSplash = true
                initialNavState
                    ?.takeIf { it.isInitialDestination }
                    ?.let { state ->
                        val route = if (state.isGroupChat) {
                            Routes.GroupChat.createRoute(state.groupId!!)
                        } else {
                            "direct_chat/${state.userId}/${state.username}"
                        }

                        navController.navigate(route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    }
            }
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
        composable(
            route = Routes.Home.route,
            arguments = emptyList()
        ) {
            HomeScreen(navController = navController)
        }


        composable(Routes.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.Search.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            SearchScreen(
                chatId = chatId,
                onNavigateBack = { navController.popBackStack() },
                onMessageClick = { message ->
                    // Navigates back tho the chat and scrolls to the message
                    navController.popBackStack()
                }
            )
        }

        // Global Search Screen
        composable(Routes.GlobalSearch.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "direct_chat/{userId}/{username}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val username = backStackEntry.arguments?.getString("username") ?: return@composable

            IndividualChatScreen(
                navController = navController,
                userId = userId,
                username = username
            )

            BackHandler {
                // Ensure we always have a way back to Home
                if (!navController.popBackStack()) {
                    // If popBackStack returns false (no previous destination), navigate to Home with default tab
                    navController.navigate(Routes.Home.createRoute(0)) {
                        // Clear entire back stack and make Home the new root
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
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
            IndividualChatScreen(
                navController = navController,
                userId = userId,
                username = username
            )

            BackHandler {
                // Ensure we always have a way back to Home
                if (!navController.popBackStack()) {
                    // If popBackStack returns false (no previous destination), navigate to Home with default tab
                    navController.navigate(Routes.Home.createRoute(0)) {
                        // Clear entire back stack and make Home the new root
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
        composable("fullScreenImage/{imageId}") { backStackEntry ->
            val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable
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

    initialNavState
        ?.takeIf { it.isInitialDestination }
        ?.let {
            LaunchedEffect(Unit) { /* Already at direct_chat, no action needed */ }
        }
}

/**
 * Determines the start destination based on splash preference and notification state.
 *
 * @param skipSplash true to bypass splash
 * @param initialNavState optional notification navigation state
 * @return route string for initial navigation
 */
private fun determineStartDestination(
    skipSplash: Boolean,
    initialNavState: NotificationNavigationState?
): String {
    return when {
        initialNavState?.isInitialDestination == true && !skipSplash ->
            Routes.Splash.route
        initialNavState?.isInitialDestination == true -> {
            if (initialNavState.isGroupChat) {
                Routes.GroupChat.createRoute(initialNavState.groupId!!)
            } else {
                "direct_chat/${initialNavState.userId}/${initialNavState.username}"
            }
        }
        skipSplash ->
            Routes.Home.createRoute()
        else ->
            Routes.Splash.route
    }
}

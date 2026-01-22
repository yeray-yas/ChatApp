package com.yerayyas.chatappkotlinproject.presentation.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleDefaultNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleNotificationNavigationUseCase
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.StartNavigationState
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

/**
 * The central navigation hub of the application.
 *
 * This Composable orchestrates the transition between the initial loading state (Splash)
 * and the main content graph. It observes the [MainActivityViewModel] to resolve the
 * appropriate start destination based on authentication status and notification intents.
 *
 * Key responsibilities:
 * - **State Orchestration:** Switches between [SplashScreen] (loading) and [NavHost] (ready).
 * - **Event Handling:** Listens for one-shot navigation events from ViewModels (e.g., from notifications).
 * - **Graph Definition:** Defines the entire navigation tree and screen arguments.
 * - **Deep Link Handling:** Manages custom back stack behavior for users entering via deep links.
 *
 * @param modifier UI modifier applied to the container.
 * @param navController The central [NavHostController] for navigation operations.
 * @param mainActivityViewModel ViewModel responsible for resolving the start destination.
 * @param handleNotificationNavigation Use case to process navigation logic from notification intents.
 * @param handleDefaultNavigation Use case to process default navigation flows (kept for dependency consistency).
 */
@Composable
fun NavigationWrapper(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainActivityViewModel: MainActivityViewModel,
    handleNotificationNavigation: HandleNotificationNavigationUseCase,
    handleDefaultNavigation: HandleDefaultNavigationUseCase
) {
    // Observe the initialization state (Loading vs Ready)
    val startNavigationState by mainActivityViewModel.startDestinationState.collectAsStateWithLifecycle()

    // Observe side-effect navigation events (e.g., triggered by notifications while app is running)
    val pendingNavigation by mainActivityViewModel.pendingNavigation.collectAsState()
    val navEvent by mainActivityViewModel.navigationEvent.collectAsStateWithLifecycle()

    // --- SIDE EFFECTS & EVENT HANDLING ---

    // Handle explicit navigation events requested by the ViewModel
    LaunchedEffect(navEvent) {
        navEvent?.let { state ->
            val route = state.destinationRoute
            Log.d("NavigationWrapper", "Navigating to event route: $route")

            navController.navigate(route) {
                // Ensure we don't build a huge backstack on top of Home
                popUpTo(Routes.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            mainActivityViewModel.onNavigationHandled()
        }
    }

    // Handle pending notification intents (Deep Linking logic)
    LaunchedEffect(pendingNavigation) {
        Log.d("NavigationWrapper", "Pending navigation: $pendingNavigation")
        pendingNavigation?.let {
            if (navController.currentDestination?.route != Routes.Splash.route) {
                handleNotificationNavigation(navController, it)
                mainActivityViewModel.clearPendingNavigation()
            }
        }
    }

    // --- GRAPH RENDERING ---

    when (val state = startNavigationState) {
        is StartNavigationState.Loading -> {
            // While the ViewModel determines the destination, we display the UI Splash.
            // We pass an empty lambda/no-op because the state transition is driven
            // reactively by 'startNavigationState', not by the Splash timer itself.
            SplashScreen(onNavigateToMain = { /* No-op: State observation handles transition */ })
        }
        is StartNavigationState.Ready -> {
            NavHost(
                navController = navController,
                startDestination = state.startDestination,
                modifier = modifier
            ) {
                // --- Group Creation Flow ---
                composable(Routes.CreateGroup.route) {
                    CreateGroupScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onGroupCreated = { groupId ->
                            navController.navigate(Routes.GroupChat.createRoute(groupId)) {
                                popUpTo(Routes.CreateGroup.route) { inclusive = true }
                            }
                        }
                    )
                }

                // --- Group Chat ---
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
                        // Custom BackHandler: Ensures that if the user entered via deep link,
                        // pressing back takes them to the Home (Groups Tab) instead of exiting the app.
                        BackHandler {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.Home.createRoute(2)) { // Tab 2 = Groups
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    } else {
                        Log.e("NavGraph", "groupId is null for GroupChat route, navigating back.")
                        navController.popBackStack()
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
                    GroupListScreen(navController = navController)
                }

                // --- Splash Route (Internal) ---
                // This route exists for manual navigation to Splash if needed,
                // distinct from the initial Loading state.
                composable(Routes.Splash.route) {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    SplashScreen(onNavigateToMain = {
                        val pendingNav = pendingNavigation
                        if (pendingNav != null) {
                            handleNotificationNavigation(navController, pendingNav)
                            mainActivityViewModel.clearPendingNavigation()
                        } else {
                            handleDefaultNavigation(navController, currentRoute)
                        }
                    })
                }

                // --- Auth & Main Routes ---
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
                    SettingsScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = Routes.Search.route,
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    SearchScreen(
                        chatId = chatId,
                        onNavigateBack = { navController.popBackStack() },
                        onMessageClick = { navController.popBackStack() }
                    )
                }

                composable(Routes.GlobalSearch.route) {
                    SearchScreen(onNavigateBack = { navController.popBackStack() })
                }

                // --- Direct Chat (Standard Route) ---
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

                    // Custom BackHandler: Ensures fallback to Home on deep link exit
                    BackHandler {
                        if (!navController.popBackStack()) {
                            navController.navigate(Routes.Home.createRoute(0)) { // Tab 0 = Chats
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // --- Profile & Media ---
                composable(Routes.UserProfile.route) {
                    UserProfileScreen(navController)
                }
                composable(Routes.EditUserProfile.route) {
                    EditUserProfileScreen(navController)
                }
                composable(Routes.ConfirmPhoto.route) {
                    ConfirmProfilePhotoScreen(navController)
                }

                // --- Direct Chat (Legacy/Alternate Route) ---
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
                        if (!navController.popBackStack()) {
                            navController.navigate(Routes.Home.createRoute(0)) {
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
        }
    }
}

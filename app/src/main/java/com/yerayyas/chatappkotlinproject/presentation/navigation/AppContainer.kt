package com.yerayyas.chatappkotlinproject.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleDefaultNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleNotificationNavigationUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.ui.theme.ChatAppKotlinProjectTheme

/**
 * Root composable for the application. Applies the app theme, configures the scaffold,
 * and initializes navigation.
 *
 * @param activityViewModel The ViewModel associated with the main activity.
 * @param handleNotificationNavigation Use case to navigate based on notification actions.
 * @param handleDefaultNavigation Use case to handle the app's default navigation flow.
 * @param skipSplash When true, bypasses the splash screen on startup.
 * @param initialNavState Optional initial navigation state derived from a notification.
 */
@Composable
fun AppContainer(
    activityViewModel: MainActivityViewModel,
    handleNotificationNavigation: HandleNotificationNavigationUseCase,
    handleDefaultNavigation: HandleDefaultNavigationUseCase,
    skipSplash: Boolean = false,
    initialNavState: NotificationNavigationState? = null
) {
    val navController = rememberNavController()

    ChatAppKotlinProjectTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
        ) { innerPadding ->
            NavigationWrapper(
                navController = navController,
                mainActivityViewModel = activityViewModel,
                handleNotificationNavigation = handleNotificationNavigation,
                handleDefaultNavigation = handleDefaultNavigation,
                skipSplash = skipSplash,
                initialNavState = initialNavState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

package com.yerayyas.chatappkotlinproject.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleDefaultNavigationUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.HandleNotificationNavigationUseCase
import com.yerayyas.chatappkotlinproject.notifications.NotificationNavigationState
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.ui.theme.ChatAppKotlinProjectTheme
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.theme.ThemeViewModel

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
    initialNavState: NotificationNavigationState? = null,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val themePreferences by themeViewModel.themePreferences.collectAsStateWithLifecycle()

    ChatAppKotlinProjectTheme(
        themeMode = themePreferences.themeMode,
        useDynamicColors = themePreferences.useDynamicColors
    ) {
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

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
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.ui.theme.ChatAppKotlinProjectTheme
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.theme.ThemeViewModel

/**
 * The top-level Composable container for the application's UI.
 *
 * This function acts as the composition root, establishing the global environment
 * for all screens. It is responsible for:
 * 1. **Theming:** Applying the app-wide theme (Dark/Light/Dynamic) observed from [ThemeViewModel].
 * 2. **Layout Structure:** Providing a [Scaffold] to correctly handle system insets (status bars, navigation bars).
 * 3. **Navigation:** Initializing the [NavHostController] and embedding the [NavigationWrapper].
 *
 * @param activityViewModel The shared ViewModel associated with the hosting Activity.
 * @param handleNotificationNavigation Use case to process deep-link navigation events.
 * @param handleDefaultNavigation Use case to determine the default start destination.
 * @param themeViewModel ViewModel managing user theme preferences (default injected via Hilt).
 */
@Composable
fun AppContainer(
    activityViewModel: MainActivityViewModel,
    handleNotificationNavigation: HandleNotificationNavigationUseCase,
    handleDefaultNavigation: HandleDefaultNavigationUseCase,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val themePreferences by themeViewModel.themePreferences.collectAsStateWithLifecycle()

    ChatAppKotlinProjectTheme(
        themeMode = themePreferences.themeMode,
        useDynamicColors = themePreferences.useDynamicColors
    ) {
        // Crucial: This Scaffold handles the system bars padding.
        // It ensures that the NavigationWrapper receives the correct 'innerPadding',
        // preventing the bottom navigation bar from overlapping content (like the keyboard).
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
        ) { innerPadding ->
            NavigationWrapper(
                navController = navController,
                mainActivityViewModel = activityViewModel,
                handleNotificationNavigation = handleNotificationNavigation,
                handleDefaultNavigation = handleDefaultNavigation,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

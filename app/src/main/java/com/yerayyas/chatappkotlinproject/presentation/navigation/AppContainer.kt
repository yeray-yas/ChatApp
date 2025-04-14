package com.yerayyas.chatappkotlinproject.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yerayyas.chatappkotlinproject.presentation.ui.theme.ChatAppKotlinProjectTheme

/**
 * Root composable that initializes the navigation controller and applies the app theme.
 *
 * This function wraps the main content of the application inside a [Scaffold],
 * handling the padding and setting up the navigation system via [NavigationWrapper].
 *
 * It also applies the [ChatAppKotlinProjectTheme] to provide consistent styling across the app.
 */
@Composable
fun AppContainer() {
    val navController = rememberNavController()
    ChatAppKotlinProjectTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavigationWrapper(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
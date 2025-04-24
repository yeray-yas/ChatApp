package com.yerayyas.chatappkotlinproject.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yerayyas.chatappkotlinproject.presentation.activity.viewmodel.MainActivityViewModel
import com.yerayyas.chatappkotlinproject.presentation.ui.theme.ChatAppKotlinProjectTheme

/**
 * Root composable - Pasa el ViewModel si es necesario, o NavigationWrapper lo obtendrá
 */
@Composable
fun AppContainer(activityViewModel: MainActivityViewModel) { // Acepta el ViewModel
    val navController = rememberNavController()
    ChatAppKotlinProjectTheme {
        // Scaffold no necesita el ViewModel directamente
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavigationWrapper(
                navController = navController,
                // Pasa el ViewModel a NavigationWrapper
                mainActivityViewModel = activityViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
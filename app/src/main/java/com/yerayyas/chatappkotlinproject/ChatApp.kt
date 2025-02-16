package com.yerayyas.chatappkotlinproject

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yerayyas.chatappkotlinproject.presentation.navigation.NavigationWrapper
import com.yerayyas.chatappkotlinproject.presentation.ui.theme.ChatAppKotlinProjectTheme

@Composable
fun ChatApp() {
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
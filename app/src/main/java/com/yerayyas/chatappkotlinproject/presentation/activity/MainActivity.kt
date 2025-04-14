package com.yerayyas.chatappkotlinproject.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yerayyas.chatappkotlinproject.presentation.navigation.AppContainer
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity is the entry point of the Chat App.
 *
 * This activity is annotated with @AndroidEntryPoint to enable dependency injection via Hilt.
 * It sets up the main content of the app using Jetpack Compose and applies edge-to-edge rendering.
 *
 * The UI content is composed using the [AppContainer] composable function.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is starting. Initializes the Compose UI and enables
     * edge-to-edge layout rendering for modern full-screen experiences.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     * this Bundle contains the data it most recently supplied; otherwise, it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppContainer()
        }
    }
}


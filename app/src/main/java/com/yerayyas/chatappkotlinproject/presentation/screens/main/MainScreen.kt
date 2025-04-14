package com.yerayyas.chatappkotlinproject.presentation.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.main.MainScreenViewModel

/**
 * Displays the main entry screen of the app.
 *
 * This composable determines if the user is authenticated and navigates
 * to the home screen if they are. Otherwise, it shows two buttons that
 * allow the user to navigate to the Sign Up or Log In screens.
 *
 * @param navController Navigation controller used to handle screen transitions.
 * @param viewModel ViewModel that provides authentication state.
 */
@Composable
fun MainScreen(navController: NavController, viewModel: MainScreenViewModel = hiltViewModel()) {
    val isAuthenticated by viewModel.isUserAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) navController.navigate("home_screen") { popUpTo(0) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                navController.navigate(route = "signup_screen")
            }
        ) {
            Text(text = stringResource(R.string.go_to_sign_up_btn))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                navController.navigate(route = "login_screen")
            }
        ) {
            Text(text = stringResource(R.string.go_to_log_in_btn))
        }
    }
}

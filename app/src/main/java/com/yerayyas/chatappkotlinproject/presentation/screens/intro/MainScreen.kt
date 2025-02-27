package com.yerayyas.chatappkotlinproject.presentation.screens.intro

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.main.MainScreenViewModel


@Composable
fun MainScreen(navController: NavController, viewModel: MainScreenViewModel = hiltViewModel()) {
    // Estado actualizado automáticamente
    val isAuthenticated by viewModel.isUserAuthenticated.collectAsState()

    val context = LocalContext.current

    // Interceptamos el botón “Atrás” y cerramos la app
    BackHandler {
        (context as? Activity)?.finish()
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) navController.navigate("home_screen") { popUpTo(0) }
    }

    // Mostrar los botones para navegar a la pantalla de login o registro
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                // Navegar a la pantalla de SignUp
                navController.navigate(route = "signup_screen")
            }
        ) {
            Text(text = stringResource(R.string.go_to_sign_up_btn))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // Navegar a la pantalla de Log In
                navController.navigate(route = "login_screen")
            }
        ) {
            Text(text = stringResource(R.string.go_to_log_in_btn))
        }
    }
}

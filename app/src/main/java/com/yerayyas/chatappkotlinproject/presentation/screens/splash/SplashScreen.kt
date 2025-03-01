package com.yerayyas.chatappkotlinproject.presentation.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.components.Loader
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    isUserAuthenticated: Boolean,
    isHomeDataLoaded: Boolean,
    onNavigateToMain: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    var delayCompleted by remember { mutableStateOf(false) }

    // 1) Se ejecuta SOLO una vez al entrar a SplashScreen
    LaunchedEffect(Unit) {
        // Si NO hay sesión, esperamos 10 segundos
        if (!isUserAuthenticated) {
            delay(3000)
            delayCompleted = true
        }
    }

    // 2) Observa cambios de autenticación, carga y el delay
    LaunchedEffect(isUserAuthenticated, isHomeDataLoaded, delayCompleted) {
        when {
            // Si hay sesión y los datos están listos, vamos a Home
            isUserAuthenticated && isHomeDataLoaded -> {
                onNavigateToHome()
            }
            // Si NO hay sesión y ya terminó el delay, vamos a Main
            !isUserAuthenticated && delayCompleted -> {
                onNavigateToMain()
            }
        }
    }

    // UI del SplashScreen
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Loader()
        Text(
            text = stringResource(id = R.string.app_name),
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 25.sp,
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        Text(
            text = stringResource(id = R.string.developer_name),
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 25.sp,
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
    }
}


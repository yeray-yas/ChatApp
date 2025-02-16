package com.yerayyas.chatappkotlinproject.presentation.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.Routes
import com.yerayyas.chatappkotlinproject.presentation.components.PasswordTextField
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.auth.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(navController: NavController) {
    val viewModel: LoginViewModel = viewModel()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            delay(3000)
            errorMessage = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(5.dp),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(12.dp))
        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            modifier = Modifier.fillMaxWidth().padding(5.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = errorMessage ?: "",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    errorMessage = "Please, fill all fields."
                    return@Button
                }
                isLoading = true
                errorMessage = null

                viewModel.login(email, password) { success, message ->
                    isLoading = false
                    if (!success) {
                        errorMessage = message
                    } else {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Login.route) { inclusive = true }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(5.dp)
        ) {
            Text("Log In")
        }
    }
}
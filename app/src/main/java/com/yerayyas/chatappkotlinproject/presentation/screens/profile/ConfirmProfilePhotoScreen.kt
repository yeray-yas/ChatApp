package com.yerayyas.chatappkotlinproject.presentation.screens.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.profile.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ConfirmProfilePhotoScreen(
    navController: NavHostController,
    userProfileViewModel: UserProfileViewModel = viewModel()
) {
    // Recuperamos la URI guardada en el SavedStateHandle
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val imageUriString = savedStateHandle?.get<String>("selectedImageUri")
    val imageUri = imageUriString?.let { Uri.parse(it) }

    // Si no se encontró una imagen, volvemos a la pantalla anterior
    if (imageUri == null) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    // Interceptamos el botón de retroceso del sistema para navegar a UserProfileScreen
    BackHandler {
        navController.navigate(Routes.UserProfile.route) {
            popUpTo(Routes.UserProfile.route) { inclusive = false }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Confirm profile photo") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Routes.UserProfile.route) {
                            popUpTo(Routes.UserProfile.route) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mostramos la imagen seleccionada
            GlideImage(
                model = imageUri.toString(),
                contentDescription = "Selected photo",
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
            )
            // Fila de botones para aceptar o cancelar
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        // Cancel: navega a UserProfileScreen
                        navController.navigate(Routes.UserProfile.route) {
                            popUpTo(Routes.UserProfile.route) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        // Aceptar: actualizamos la imagen y navegamos a UserProfileScreen
                        userProfileViewModel.updateProfileImage(imageUri)
                        navController.navigate(Routes.UserProfile.route) {
                            popUpTo(Routes.UserProfile.route) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) {
                    Text("Oh yeah!")
                }
            }
        }
    }
}


package com.yerayyas.chatappkotlinproject.presentation.screens.profile

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.profile.UserProfileViewModel
import androidx.core.net.toUri

/**
 * Composable screen for confirming the profile photo selection.
 *
 * This screen allows the user to review and confirm their selected profile photo.
 * If no photo is selected, the user is navigated back to the previous screen.
 * The user can either confirm or cancel the selection, with the appropriate action
 * being taken for each choice.
 *
 * @param navController The NavHostController used for navigation between screens.
 * @param userProfileViewModel The ViewModel that handles the logic for updating the user's profile image.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ConfirmProfilePhotoScreen(
    navController: NavHostController,
    userProfileViewModel: UserProfileViewModel = hiltViewModel()
) {
    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
    val imageUriString = savedStateHandle?.get<String>("selectedImageUri")
    val imageUri = imageUriString?.toUri()

    if (imageUri == null) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

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
            GlideImage(
                model = imageUri.toString(),
                contentDescription = "Selected photo",
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
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

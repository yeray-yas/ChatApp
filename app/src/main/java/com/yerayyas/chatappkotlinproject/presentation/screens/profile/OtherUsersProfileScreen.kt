package com.yerayyas.chatappkotlinproject.presentation.screens.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.profile.OtherUsersProfileViewModel

/**
 * Composable function that displays the profile screen of another user.
 *
 * This screen shows the user's profile image, username, and user ID.
 * It also includes a top app bar with a back navigation button. The user data is fetched
 * using a ViewModel and is displayed upon loading.
 *
 * @param navController The NavHostController used for navigation between screens.
 * @param userId The unique identifier of the user whose profile is being displayed.
 * @param username The username of the user (used as a fallback if user data is not loaded).
 * @param viewModel The ViewModel responsible for fetching and holding the user's profile data.
 *                  This is injected using Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun OtherUsersProfileScreen(
    navController: NavHostController,
    userId: String,
    username: String,
    viewModel: OtherUsersProfileViewModel = hiltViewModel()
) {
    val userData by viewModel.userData.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${userData?.username ?: username}'s Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                GlideImage(
                    model = userData?.profileImage ?: R.drawable.galeria,
                    contentDescription = "Profile picture",
                    modifier = Modifier.fillMaxSize(),
                    requestBuilderTransform = {
                        it.override(100, 100)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("ID: $userId", style = MaterialTheme.typography.bodyMedium)
            userData?.username?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

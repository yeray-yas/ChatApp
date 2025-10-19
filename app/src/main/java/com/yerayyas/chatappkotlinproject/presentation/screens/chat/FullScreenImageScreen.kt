package com.yerayyas.chatappkotlinproject.presentation.screens.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.cache.ImageUrlStore
import java.net.URL

/**
 * Displays a full-screen image based on a provided image ID.
 *
 * The image URL is retrieved from a shared store using the image ID, and the image
 * is displayed using Glide. The screen handles validation of the URL and shows an
 * error message if the URL is invalid or not found. When the screen is dismissed,
 * the image URL is removed from the store to clean up resources.
 *
 * @param navController Navigation controller used to navigate back from the screen.
 * @param imageId Unique identifier associated with the image to be displayed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun FullScreenImageScreen(
    navController: NavHostController,
    imageId: String
) {
    Log.d("FullScreenImageScreen", "Image ID received: $imageId")

    val imageUrl = ImageUrlStore.getImageUrl(imageId)
    Log.d("FullScreenImageScreen", "Image URL: $imageUrl")

    var isValidUrl by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageUrl) {
        if (imageUrl == null) {
            isValidUrl = false
            errorMessage = "Image URL not found"
            return@LaunchedEffect
        }

        try {
            URL(imageUrl)
        } catch (e: Exception) {
            Log.e("FullScreenImageScreen", "Invalid URL: ${e.message}")
            isValidUrl = false
            errorMessage = "Invalid URL: ${e.message}"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ImageUrlStore.removeImageUrl(imageId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (!isValidUrl || imageUrl == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error loading image",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                GlideImage(
                    model = imageUrl,
                    contentDescription = "Full-screen image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

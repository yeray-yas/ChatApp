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
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun FullScreenImageScreen(
    navController: NavHostController,
    imageId: String
) {
    Log.d("FullScreenImageScreen", "ID de imagen recibido: $imageId")
    
    // Obtenemos la URL de la imagen
    val imageUrl = ImageUrlStore.getImageUrl(imageId)
    
    Log.d("FullScreenImageScreen", "URL de la imagen: $imageUrl")
    
    var isValidUrl by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(imageUrl) {
        if (imageUrl == null) {
            isValidUrl = false
            errorMessage = "No se encontró la URL de la imagen"
            return@LaunchedEffect
        }
        
        try {
            URL(imageUrl)
        } catch (e: Exception) {
            Log.e("FullScreenImageScreen", "URL inválida: ${e.message}")
            isValidUrl = false
            errorMessage = "URL inválida: ${e.message}"
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Limpiamos la URL cuando la pantalla se cierra
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
                            contentDescription = "Volver",
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
                        text = "Error al cargar la imagen",
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
                    contentDescription = "Imagen en pantalla completa",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
} 
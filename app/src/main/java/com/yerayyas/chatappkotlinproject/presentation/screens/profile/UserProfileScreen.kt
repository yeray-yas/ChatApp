package com.yerayyas.chatappkotlinproject.presentation.screens.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.utils.animations.HamburgerToArrowAnimation
import com.yerayyas.chatappkotlinproject.Routes
import com.yerayyas.chatappkotlinproject.utils.bitmapToUri
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.profile.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun UserProfileScreen(
    navController: NavHostController,
    viewModel: UserProfileViewModel = viewModel()
) {
    val username by viewModel.username.collectAsState()
    val names by viewModel.names.collectAsState()
    val lastNames by viewModel.lastNames.collectAsState()
    val email by viewModel.email.collectAsState()
    val image by viewModel.image.collectAsState()
    val profession by viewModel.profession.collectAsState()
    val address by viewModel.address.collectAsState()
    val age by viewModel.age.collectAsState()
    val phone by viewModel.phone.collectAsState()

    val scrollState = rememberScrollState()
    var showImagePickerDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Launcher para seleccionar imagen desde la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navController.currentBackStackEntry?.savedStateHandle?.set("selectedImageUri", it.toString())
            navController.navigate(Routes.ConfirmPhoto.route)
        }
    }

    // Launcher para tomar foto con la cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val uri = bitmapToUri(context, it)
            navController.currentBackStackEntry?.savedStateHandle?.set("selectedImageUri", uri.toString())
            navController.navigate(Routes.ConfirmPhoto.route)
        }
    }

    // Interceptamos el botón de retroceso del sistema para navegar a HomeScreen
    BackHandler {
        navController.navigate(Routes.Home.route) {
            popUpTo(Routes.Home.route) { inclusive = false }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "User's Profile") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Home.route) { inclusive = false }
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                GlideImage(
                    model = image,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
                IconButton(
                    onClick = { showImagePickerDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = (-8).dp, y = (8).dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile Image",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HamburgerToArrowAnimation()
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Username: $username",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = "Email: $email",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                    IconButton(
                        onClick = { navController.navigate(Routes.EditUserProfile.route) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Personal Information",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = "First Name: $names",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = "Last Name: $lastNames",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = "Profession: $profession",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = "Address: $address",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = "Age: $age",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = "Phone: $phone",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        if (showImagePickerDialog) {
            AlertDialog(
                onDismissRequest = { showImagePickerDialog = false },
                title = { Text("Seleccionar imagen") },
                text = { Text("Elige una imagen desde la galería o toma una nueva foto.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showImagePickerDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Text("Gallery")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showImagePickerDialog = false
                            cameraLauncher.launch()
                        }
                    ) {
                        Text("Camera")
                    }
                }
            )
        }
    }
}


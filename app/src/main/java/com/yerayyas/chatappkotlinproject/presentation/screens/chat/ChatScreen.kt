package com.yerayyas.chatappkotlinproject.presentation.screens.chat

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.presentation.components.MediaPickerButton
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatViewModel
import com.yerayyas.chatappkotlinproject.utils.bitmapToUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    chatViewModel: ChatViewModel = hiltViewModel(),
    userId: String,
    username: String
) {
    val messages by chatViewModel.messages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Launchers de galería y cámara
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { chatViewModel.sendMediaMessage(userId, it, "image") }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val uri = bitmapToUri(context, it)
            if (uri != null) {
                chatViewModel.sendMediaMessage(userId, uri, "image")
            }
        }
    }

    // Carga mensajes al iniciar
    LaunchedEffect(userId) {
        chatViewModel.loadMessages(userId)
    }

    // Scroll automático al último mensaje cada vez que cambie la lista
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = username.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Lista de mensajes
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
            ) {
                items(messages) { message ->
                    ChatMessageItem(message)
                }
            }

            // Barra de entrada
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaPickerButton(
                    onGalleryClick = { galleryLauncher.launch("image/*") },
                    onCameraClick = { cameraLauncher.launch() },
                    icon = Icons.Default.AttachFile,
                    contentDescription = "Attach a file"
                )

                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        chatViewModel.sendMessage(userId, messageText)
                        messageText = ""
                    })
                )
                IconButton(onClick = {
                    chatViewModel.sendMessage(userId, messageText)
                    messageText = ""
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMe = message.senderId == currentUserId

    // Opcional: obtener ancho de la pantalla para un límite dinámico
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    // Elegimos que el máximo sea 80% del ancho de la pantalla
    val maxBubbleWidth = screenWidth * 0.8f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                // Máximo 80% del ancho de la pantalla
                .widthIn(max = maxBubbleWidth)
                .background(
                    color = if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            if (message.messageType == "image" && message.mediaUrl != null) {
                val painter = rememberGlidePainter(message.mediaUrl, context)
                Image(
                    painter = painter,
                    contentDescription = "Image",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = message.message,
                    color = if (isMe) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun rememberGlidePainter(
    imageUrl: String,
    context: Context,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
): Painter {
    val painterState = remember { mutableStateOf<Painter?>(null) }

    LaunchedEffect(imageUrl) {
        try {
            // Carga la imagen en un hilo de fondo
            val bitmap = withContext(ioDispatcher) {
                Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()
            }
            painterState.value = BitmapPainter(bitmap.asImageBitmap())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Placeholder mientras se carga
    return painterState.value ?: ColorPainter(Color.Gray)
}



package com.yerayyas.chatappkotlinproject.presentation.screens.chat

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatViewModel
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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { chatViewModel.sendImage(userId, it) }
    }

    // Cargar mensajes cuando se abre el chat
    LaunchedEffect(userId) {
        chatViewModel.loadMessages(userId)
    }

    // Scroll automático al final cuando hay nuevos mensajes
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = username.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }, style = MaterialTheme.typography.titleMedium) },
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                state = listState,
                reverseLayout = true
            ) {
                items(messages.reversed().size) { index ->
                    val message = messages.reversed()[index]
                    ChatMessageItem(
                        message = message,
                        currentUserId = chatViewModel.getCurrentUserId(),
                        navController = navController,
                        isLastMessage = index == 0
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar archivo")
                }

                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje...") },
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
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar mensaje")
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun MessageImage(
    url: String,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    GlideImage(
        model = url,
        contentDescription = "Imagen del mensaje",
        modifier = modifier
            .size(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                try {
                    val imageId = url.hashCode().toString()
                    Log.d("ChatMessageItem", "Navegando a imagen con ID: $imageId")
                    navController.navigate("fullScreenImage/$imageId")
                    ImageUrlStore.addImageUrl(imageId, url)
                } catch (e: Exception) {
                    Log.e("ChatMessageItem", "Error al navegar: ${e.message}")
                }
            },
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    currentUserId: String,
    navController: NavHostController,
    isLastMessage: Boolean = false
) {
    val isMe = message.senderId == currentUserId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                when (message.messageType) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.message,
                            color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MessageType.IMAGE -> {
                        message.imageUrl?.let { url ->
                            MessageImage(url = url, navController = navController)
                        }
                    }
                }
                
                if (isMe && isLastMessage) {
                    Text(
                        text = when (message.readStatus) {
                            ReadStatus.SENT -> "Enviado"
                            ReadStatus.DELIVERED -> "Entregado"
                            ReadStatus.READ -> "Visto"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

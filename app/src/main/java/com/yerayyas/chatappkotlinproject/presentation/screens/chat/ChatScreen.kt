package com.yerayyas.chatappkotlinproject.presentation.screens.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    chatViewModel: ChatViewModel = viewModel(),
    userId: String,  // ID del usuario con el que se está chateando
    username: String // Nombre del usuario con el que se está chateando
) {
    val messages by chatViewModel.messages.collectAsState()
    var messageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = username.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.ROOT
                    ) else it.toString()
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
                reverseLayout = true // Para que los mensajes nuevos aparezcan abajo
            ) {
                items(messages) { message ->
                    ChatMessageItem(message = message)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Acción para adjuntar archivos */ }) {
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

@Composable
fun ChatMessageItem(message: ChatMessage) {
  /*  val isMe = message.isFromCurrentUser // Asumiendo que tienes una lógica para esto
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Text(text = message.text, color = if (isMe) Color.White else Color.Black)
        }
    }*/
}

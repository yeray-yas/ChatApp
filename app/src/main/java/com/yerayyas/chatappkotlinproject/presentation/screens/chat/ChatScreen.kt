package com.yerayyas.chatappkotlinproject.presentation.screens.chat

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.cache.ImageUrlStore
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.presentation.components.UserStatusAndActions
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatViewModel
import java.util.Locale

/**
 * ChatScreen displays the UI for a one-on-one chat conversation.
 *
 * It handles message input, sending text and image messages, displaying the chat history,
 * showing loading indicators, and providing UI feedback for errors.
 *
 * @param navController Navigation controller used to navigate between screens.
 * @param chatViewModel ViewModel providing chat data and logic.
 * @param userId The ID of the other user in the chat.
 * @param username The username of the other user, shown in the top app bar.
 */
@Composable
fun ChatScreen(
    navController: NavHostController,
    chatViewModel: ChatViewModel = hiltViewModel(),
    userId: String,
    username: String
) {
    // --- State Collection ---
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val error by chatViewModel.error.collectAsState()
    val currentUserId = remember { chatViewModel.getCurrentUserId() }

    // --- Local UI State ---
    var messageText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // --- Activity Result Launchers ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { chatViewModel.sendImage(userId, it) }
    }

    // --- Side Effects ---
    LaunchedEffect(userId) {
        chatViewModel.loadMessages(userId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatViewModel.clearError()
        }
    }

    // --- Actions ---
    val sendMessageAction = {
        if (!isLoading && messageText.isNotBlank()) {
            chatViewModel.sendMessage(userId, messageText.trim())
            messageText = ""
        }
    }

    val attachFileAction = {
        imagePickerLauncher.launch("image/*")
    }

    // --- UI Structure ---
    Scaffold(
        topBar = {
            ChatTopAppBar(
                username = username,
                onNavigateBack = { navController.popBackStack() },
                actions = {
                    UserStatusAndActions(
                        navController = navController,
                        userId = userId,
                        username = username
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Usa consumeWindowInsets=false y WindowInsets Tamer si manejas insets manualmente
                .padding(paddingValues)
        ) {
            ChatMessagesList(
                messages = messages,
                listState = listState,
                isLoading = isLoading,
                currentUserId = currentUserId,
                navController = navController,
                modifier = Modifier.weight(1f)
            )

            ChatInputArea(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendMessage = sendMessageAction,
                onAttachFile = attachFileAction,
                isLoading = isLoading
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopAppBar(
    username: String,
    onNavigateBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = username.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }, style = MaterialTheme.typography.titleMedium)
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions
    )
}

@Composable
private fun ChatInputArea(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachFile: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val isSendEnabled = !isLoading && messageText.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAttachFile,
            enabled = !isLoading
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
        }

        TextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (isSendEnabled) {
                    onSendMessage()
                }
            }),
            enabled = !isLoading,
            shape = RoundedCornerShape(20.dp)
        )

        IconButton(
            onClick = onSendMessage,
            enabled = isSendEnabled
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                tint = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.5f
                )
            )
        }
    }
}

@Composable
private fun ChatMessagesList(
    messages: List<ChatMessage>,
    listState: LazyListState,
    isLoading: Boolean,
    currentUserId: String,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            state = listState,
            reverseLayout = true
        ) {
            items(messages.reversed().size) { index ->
                val message = messages.reversed()[index]
                ChatMessageItem(
                    message = message,
                    currentUserId = currentUserId,
                    navController = navController,
                    isLastMessage = index == 0 && message.isSentBy(currentUserId)
                )
            }
        }

        if (isLoading && messages.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

/**
 * Displays an image sent in a chat message with optional full-screen preview navigation.
 *
 * @param url URL of the image.
 * @param navController Used to navigate to a full-screen image screen.
 * @param modifier Modifier applied to the image view.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun MessageImage(
    url: String,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    GlideImage(
        model = url,
        contentDescription = "Message image",
        modifier = modifier
            .size(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                try {
                    val imageId = url.hashCode().toString()
                    Log.d("ChatMessageItem", "Navigating to image with ID: $imageId")
                    navController.navigate("fullScreenImage/$imageId")
                    ImageUrlStore.addImageUrl(imageId, url)
                } catch (e: Exception) {
                    Log.e("ChatMessageItem", "Navigation error: ${e.message}")
                }
            },
        contentScale = ContentScale.Crop
    )
}

/**
 * Displays a single chat message with its bubble styling, text or image content,
 * and optionally a delivery/read status for the last message.
 *
 * @param message The chat message to display.
 * @param currentUserId ID of the current user to determine sender alignment.
 * @param navController Navigation controller for image preview.
 * @param isLastMessage Whether this is the last message sent by the user.
 */
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    currentUserId: String,
    navController: NavHostController,
    isLastMessage: Boolean = false
) {
    val isMe = message.isSentBy(currentUserId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = getBubbleColor(isMe),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (message.messageType) {
                MessageType.TEXT -> {
                    Text(
                        text = message.message,
                        color = getTextColor(isMe),
                        modifier = Modifier.wrapContentWidth()
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
                        ReadStatus.SENT -> "Sent"
                        ReadStatus.DELIVERED -> "Delivered"
                        ReadStatus.READ -> "Read"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun getBubbleColor(isMe: Boolean): Color {
    return if (isMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
private fun getTextColor(isMe: Boolean): Color {
    return if (isMe) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}

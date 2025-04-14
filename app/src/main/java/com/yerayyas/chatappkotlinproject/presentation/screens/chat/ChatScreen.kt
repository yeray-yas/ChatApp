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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    chatViewModel: ChatViewModel = hiltViewModel(),
    userId: String,
    username: String
) {
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val error by chatViewModel.error.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { chatViewModel.sendImage(userId, it) }
    }

    LaunchedEffect(userId) {
        chatViewModel.loadMessages(userId)
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = username.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                    }, style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                        end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
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
                                currentUserId = chatViewModel.getCurrentUserId(),
                                navController = navController,
                                isLastMessage = index == 0
                            )
                        }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(1.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
                    }

                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (!isLoading && messageText.isNotBlank()) {
                                chatViewModel.sendMessage(userId, messageText)
                                messageText = ""
                            }
                        }),
                        enabled = !isLoading
                    )

                    IconButton(
                        onClick = {
                            if (!isLoading && messageText.isNotBlank()) {
                                chatViewModel.sendMessage(userId, messageText)
                                messageText = ""
                            }
                        },
                        enabled = !isLoading && messageText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                    }
                }
            }
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

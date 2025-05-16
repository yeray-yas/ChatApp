package com.yerayyas.chatappkotlinproject.presentation.screens.chat

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.cache.ImageUrlStore
import com.yerayyas.chatappkotlinproject.data.model.ChatInputState
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.presentation.components.UserStatusAndActions
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatViewModel
import java.util.Locale

/**
 * Composable for displaying and interacting with a chat conversation.
 * It handles loading messages, sending text and image messages,
 * responding to UI events, and adjusting layout for system insets.
 *
 * @param navController Controller for navigation actions.
 * @param chatViewModel ViewModel powering chat state and operations.
 * @param userId Unique identifier of the chat partner.
 * @param username Display name of the chat partner.
 */
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
    val currentUserId = remember { chatViewModel.getCurrentUserId() }

    val isDirectChat = remember {
        navController.currentBackStackEntry?.destination?.route
            ?.startsWith("direct_chat") == true
    }

    var messageText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // Observe window insets to adjust input area above keyboard and navigation bar.
    val view = LocalView.current
    var imeBottomPx by remember { mutableIntStateOf(0) }
    var navBarHeightPx by remember { mutableIntStateOf(0) }
    DisposableEffect(view) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            imeBottomPx = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            navBarHeightPx = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            insets
        }
        ViewCompat.requestApplyInsets(view)
        onDispose { ViewCompat.setOnApplyWindowInsetsListener(view, null) }
    }
    val offsetY = if (imeBottomPx > 0) -(imeBottomPx - navBarHeightPx) else 0

    // Launcher for selecting an image to send
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { chatViewModel.sendImage(userId, it) } }

    // Load messages on start
    LaunchedEffect(userId) { chatViewModel.loadMessages(userId) }
    // Scroll to newest message when list updates
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }
    // Display errors via Toast
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatViewModel.clearError()
        }
    }

    // Defines actions for sending text and attaching images
    val sendMessage = {
        if (!isLoading && messageText.isNotBlank()) {
            chatViewModel.sendMessage(userId, messageText.trim())
            messageText = ""
        }
    }
    val attachFile = { imagePickerLauncher.launch("image/*") }

    // Customize back navigation behavior
    BackHandler {
        if (isDirectChat) {
            navController.navigate(Routes.Home.route) {
                popUpTo("direct_chat/{userId}/{username}") { inclusive = true }
            }
        } else {
            navController.popBackStack()
        }
    }

    // Track currently open chat in global app state
    DisposableEffect(userId) {
        val appState = chatViewModel.appState
        appState.currentOpenChatUserId = userId
        onDispose {
            if (appState.currentOpenChatUserId == userId) {
                appState.currentOpenChatUserId = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        ChatTopAppBar(
            modifier = Modifier.zIndex(1f),
            username = username,
            onNavigateBack = {
                if (isDirectChat) {
                    navController.navigate(Routes.Home.route) {
                        popUpTo("direct_chat/{userId}/{username}") { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            },
            actions = { UserStatusAndActions(navController, userId, username) }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(x = 0, y = offsetY) }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageItem(
                            message = message,
                            currentUserId = currentUserId,
                            navController = navController,
                            isLastMessage = message.isSentBy(currentUserId)
                        )
                    }
                }

                ChatInputArea(
                    state = ChatInputState(
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        focusRequester = focusRequester
                    ),
                    onSendMessage = sendMessage,
                    onAttachFile = attachFile,
                    isLoading = isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}

/**
 * Top app bar for the chat screen, displaying the partner’s name and navigation controls.
 *
 * @param modifier Modifier for styling and layout.
 * @param username The chat partner’s display name.
 * @param onNavigateBack Callback executed when back navigation is triggered.
 * @param actions Additional action icons to display in the app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopAppBar(
    modifier: Modifier = Modifier,
    username: String,
    onNavigateBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = username.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },
        actions = actions
    )
}

/**
 * Area for composing and sending chat messages and attachments.
 *
 * @param state Holds the current input text and focus requester.
 * @param onSendMessage Invoked when the send action is triggered.
 * @param onAttachFile Invoked when the attach file action is triggered.
 * @param isLoading Disables inputs when true.
 * @param modifier Modifier for styling and layout.
 */
@Composable
private fun ChatInputArea(
    state: ChatInputState,
    onSendMessage: () -> Unit,
    onAttachFile: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val isSendEnabled = !isLoading && state.messageText.isNotBlank()
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachFile, enabled = !isLoading) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
        }
        TextField(
            value = state.messageText,
            onValueChange = state.onMessageChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(state.focusRequester),
            placeholder = { Text("Type a message...") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (isSendEnabled) onSendMessage() }),
            enabled = !isLoading,
            shape = RoundedCornerShape(20.dp)
        )
        IconButton(onClick = onSendMessage, enabled = isSendEnabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                tint = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Displays an image message and navigates to full-screen view on click.
 *
 * @param url URL of the image to display.
 * @param navController Controller to handle navigation actions.
 * @param modifier Modifier for styling and layout.
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
                    navController.navigate("fullScreenImage/$imageId")
                    ImageUrlStore.addImageUrl(imageId, url)
                } catch (e: Exception) {
                    Log.e("MessageImage", "Navigation error: ${e.message}")
                }
            },
        contentScale = ContentScale.Crop
    )
}

/**
 * Renders a chat bubble for text or image messages with styling based on sender.
 *
 * @param message The chat message data.
 * @param currentUserId ID of the current user.
 * @param navController Controller to handle image navigation.
 * @param isLastMessage True if this is the last message sent by the user, to display read status.
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
                .background(color = getBubbleColor(isMe), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (message.messageType) {
                MessageType.TEXT -> Text(
                    text = message.message,
                    color = getTextColor(isMe),
                    modifier = Modifier.wrapContentWidth()
                )
                MessageType.IMAGE -> message.imageUrl?.let { url ->
                    MessageImage(url = url, navController = navController)
                }
            }
            if (isMe && isLastMessage) {
                Text(
                    text = message.readStatus.name.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) },
                    style = MaterialTheme.typography.labelSmall,
                    color = getTextColor(true).copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Returns the background color for a chat bubble based on the sender.
 */
@Composable
private fun getBubbleColor(isMe: Boolean): Color =
    if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

/**
 * Returns the text color for a chat bubble based on the sender.
 */
@Composable
private fun getTextColor(isMe: Boolean): Color =
    if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

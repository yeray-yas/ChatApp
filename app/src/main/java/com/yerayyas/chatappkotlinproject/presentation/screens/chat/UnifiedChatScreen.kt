package com.yerayyas.chatappkotlinproject.presentation.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.presentation.components.ErrorState
import com.yerayyas.chatappkotlinproject.presentation.components.LoadingState
import com.yerayyas.chatappkotlinproject.presentation.components.MessageStatusIndicator
import com.yerayyas.chatappkotlinproject.presentation.components.UserStatusAndActions
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.IndividualAndGroupChatViewModel
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.MessageDeliveryStatus
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.UnifiedMessage
import com.yerayyas.chatappkotlinproject.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.composed
import kotlinx.coroutines.cancel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedChatScreen(
    chatId: String,
    chatType: ChatType,
    chatName: String, // Name of the user or group
    navController: NavHostController,
    viewModel: IndividualAndGroupChatViewModel = hiltViewModel()
) {
    val currentChatType by viewModel.chatType.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val groupInfo by viewModel.groupInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val replyToMessage by viewModel.replyToMessage.collectAsState()
    val scrollToMessageId by viewModel.scrollToMessageId.collectAsState()
    val highlightedMessageId by viewModel.highlightedMessageId.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Observe window insets to adjust input area above keyboard and navigation bar.
    val view = LocalView.current
    var imeBottomPx by remember { mutableIntStateOf(0) }
    var navBarHeightPx by remember { mutableIntStateOf(0) }

    // Track if user is at the bottom of the message list
    var isAtBottom by remember { mutableStateOf(true) }

    // Make it survive if we navigate to another screen and back to the chat
    var hasPerformedInitialScroll by rememberSaveable { mutableStateOf(false) }

    // Track if keyboard is currently open
    var isKeyboardOpen by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            imeBottomPx = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            navBarHeightPx = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            isKeyboardOpen = imeBottomPx > 0
            insets
        }
        ViewCompat.requestApplyInsets(view)
        onDispose { ViewCompat.setOnApplyWindowInsetsListener(view, null) }
    }

    // Calculate smart dynamic padding
    val smartBottomPadding = if (isKeyboardOpen && isAtBottom) {
        with(density) {
            val keyboardHeightDp = (imeBottomPx / density.density).dp
            val inputAreaHeight = 100.dp
            keyboardHeightDp + inputAreaHeight - Constants.TOP_APP_BAR_HEIGHT
        }
    } else {
        80.dp
    }

    // Monitor scroll position to determine if user is at the bottom
    LaunchedEffect(listState.isScrollInProgress, isKeyboardOpen) {
        if (!listState.isScrollInProgress && messages.isNotEmpty() && !isKeyboardOpen) {
            val visibleInfo = listState.layoutInfo.visibleItemsInfo
            val lastVisibleIndex = visibleInfo.lastOrNull()?.index ?: -1
            val totalItems = messages.size
            isAtBottom = lastVisibleIndex >= totalItems - 2
        }
    }

    // Calculate simple offset for input area
    val inputOffset = if (imeBottomPx > 0) -(imeBottomPx - navBarHeightPx) else 0

    // Initialize chat based on type
    LaunchedEffect(chatId, chatType) {
        when (chatType) {
            is ChatType.Individual -> viewModel.initializeIndividualChat(chatId)
            is ChatType.Group -> viewModel.initializeGroupChat(chatId)
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, messages.lastOrNull()?.id) {
        if (messages.isNotEmpty()) {
            val lastMessage = messages.last()

            if (!hasPerformedInitialScroll) {
                listState.scrollToItem(messages.size - 1)
                isAtBottom = true
                hasPerformedInitialScroll = true
            }
            else if (lastMessage.isSentBy(viewModel.getCurrentUserId())) {
                listState.animateScrollToItem(messages.size - 1)
                isAtBottom = true
            }
            else if (isAtBottom) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Auto-scroll when keyboard opens and user is at bottom
    LaunchedEffect(isKeyboardOpen, isAtBottom) {
        if (isKeyboardOpen && isAtBottom && messages.isNotEmpty()) {
            // Small delay to ensure keyboard animation has started
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Scroll to specific message if needed (works for both individual and group chats)
    LaunchedEffect(scrollToMessageId) {
        val messageId = scrollToMessageId
        if (messageId != null) {
            val index = messages.indexOfFirst { it.id == messageId }
            if (index != -1) {
                listState.scrollToItem(index)

                snapshotFlow { listState.layoutInfo }
                    .collect { layoutInfo ->

                        val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

                        if (itemInfo != null) {
                            val viewportHeight = layoutInfo.viewportSize.height
                            val itemHeight = itemInfo.size

                            val targetTopMargin = (viewportHeight - itemHeight) / 2

                            val centeringOffset = -targetTopMargin.coerceAtLeast(0)

                            listState.animateScrollToItem(index, centeringOffset)

                            // Cancel the flow
                            this.cancel()
                        }
                    }
            }
        }
    }

    // Handle error states
    LaunchedEffect(error) {
        val currentError = error
        if (currentError != null) {
            scope.launch {
                snackbarHostState.showSnackbar(currentError)
                viewModel.clearError()
            }
        }
    }

    // Launcher for selecting an image to send
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendMessage(imageUri = it) }
    }

    // Send message action
    val sendMessage = {
        if (!isLoading && messageText.trim().isNotEmpty()) {
            viewModel.sendMessage()
            isAtBottom = true
        }
    }

    val attachFile = { imagePickerLauncher.launch("image/*") }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat content - positioned behind TopAppBar
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading && messages.isEmpty() -> {
                    LoadingState(
                        message = "Loading chat...",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = Constants.TOP_APP_BAR_HEIGHT)
                    )
                }

                error != null && messages.isEmpty() -> {
                    ErrorState(
                        message = error ?: "Unknown error",
                        onRetry = {
                            when (chatType) {
                                is ChatType.Individual -> viewModel.initializeIndividualChat(chatId)
                                is ChatType.Group -> viewModel.initializeGroupChat(chatId)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = Constants.TOP_APP_BAR_HEIGHT)
                    )
                }

                messages.isEmpty() -> {
                    EmptyChatState(
                        chatName = chatName,
                        isGroup = currentChatType is ChatType.Group,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = Constants.TOP_APP_BAR_HEIGHT)
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .padding(top = Constants.TOP_APP_BAR_HEIGHT)
                            .padding(bottom = smartBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                        contentPadding = PaddingValues(
                            vertical = 8.dp
                        )
                    ) {
                        items(messages) { message ->
                            UnifiedMessageBubble(
                                message = message,
                                isFromCurrentUser = message.isSentBy(viewModel.getCurrentUserId()),
                                isGroup = currentChatType is ChatType.Group,
                                currentUserId = viewModel.getCurrentUserId(),
                                onLongPress = { viewModel.setReplyToMessage(message) },
                                onReplyClick = {
                                    val originalMessageId = viewModel.getOriginalMessageId(message)
                                    originalMessageId?.let { messageId ->
                                        viewModel.scrollToOriginalMessage(messageId)
                                    }
                                },
                                highlightedMessageId = highlightedMessageId,
                                navController = navController,
                                chatName = chatName
                            )
                        }
                    }
                }
            }
        }

        // Input area
        if (viewModel.canSendMessages()) {
            UnifiedMessageInputBar(
                messageText = messageText,
                onMessageTextChange = viewModel::updateMessageText,
                onSendMessage = sendMessage,
                onAttachFile = attachFile,
                replyToMessage = replyToMessage,
                currentUserId = viewModel.getCurrentUserId(),
                chatName = chatName,
                onClearReply = { viewModel.clearReply() },
                isEnabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp)
                    .offset { IntOffset(x = 0, y = inputOffset) }
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        // TopAppBar - fixed at top with proper window insets
        UnifiedChatTopBar(
            chatName = chatName,
            chatType = currentChatType,
            groupInfo = groupInfo,
            onBackClick = {
                // Ensure we always have a way back to Home
                if (!navController.popBackStack()) {
                    val targetTab = if (currentChatType is ChatType.Group) 2 else 0
                    navController.navigate(Routes.Home.createRoute(targetTab)) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            onInfoClick = {
                when (currentChatType) {
                    is ChatType.Group -> {
                        groupInfo?.let {
                            navController.navigate(Routes.GroupInfo.createRoute(it.id))
                        }
                    }

                    is ChatType.Individual -> {
                        // Navigate to user profile if necessary
                    }
                }
            },
            onSearchClick = {
                navController.navigate(Routes.Search.createRoute(chatId))
            },
            userActions = {
                if (currentChatType is ChatType.Individual) {
                    UserStatusAndActions(navController, chatId, chatName)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(10f)
        )

        // SnackbarHost positioned to avoid overlap with input area
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isKeyboardOpen) 160.dp else 80.dp)
        )
    }
}

/**
 * Unified top bar that adapts to the chat type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedChatTopBar(
    chatName: String,
    chatType: ChatType,
    groupInfo: com.yerayyas.chatappkotlinproject.data.model.GroupChat?,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit,
    onSearchClick: () -> Unit,
    userActions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            when (chatType) {
                is ChatType.Individual -> {
                    Text(
                        text = chatName.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.ROOT)
                            else it.toString()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                is ChatType.Group -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = groupInfo?.name ?: chatName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (groupInfo != null) {
                            Text(
                                text = "${groupInfo.memberIds.size} members",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search messages"
                )
            }

            when (chatType) {
                is ChatType.Group -> {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Group info"
                        )
                    }
                }

                is ChatType.Individual -> {
                    userActions()
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}

/**
 * Unified message input bar with support for replies
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UnifiedMessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachFile: () -> Unit,
    replyToMessage: UnifiedMessage?,
    currentUserId: String,
    chatName: String,
    onClearReply: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isSendEnabled = isEnabled && messageText.trim().isNotEmpty()

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            replyToMessage?.let { replyMsg ->
                ReplyPreview(
                    replyToMessage = replyMsg,
                    currentUserId = currentUserId,
                    chatName = chatName,
                    onClearReply = onClearReply,
                    onReplyClick = { },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = onAttachFile,
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach file"
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 140.dp)
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { newValue ->
                            onMessageTextChange(newValue)

                            scope.launch {
                                delay(50)
                                bringIntoViewRequester.bringIntoView()

                                if (newValue.length > messageText.length &&
                                    scrollState.value > scrollState.maxValue - 100) {
                                    scrollState.scrollTo(scrollState.maxValue)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .simpleVerticalScrollbar(
                                state = scrollState,
                                width = 4.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            .verticalScroll(scrollState)
                            .bringIntoViewRequester(bringIntoViewRequester),
                        placeholder = { Text("Type a message...") },
                        singleLine = false,
                        enabled = isEnabled,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSendMessage,
                    enabled = isSendEnabled,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (messageText.trim().isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (messageText.trim().isNotEmpty())
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


/**
 * Unified message bubble that adapts to the chat type
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
private fun UnifiedMessageBubble(
    message: UnifiedMessage,
    isFromCurrentUser: Boolean,
    isGroup: Boolean,
    currentUserId: String,
    onLongPress: () -> Unit,
    onReplyClick: () -> Unit,
    highlightedMessageId: String?,
    navController: NavHostController,
    chatName: String
) {
    val isHighlighted = message.id == highlightedMessageId
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart

    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            Color(0xFFFFE082) // Light amber/yellow for highlight
        } else {
            if (isFromCurrentUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        },
        animationSpec = tween(durationMillis = 500),
        label = "MessageHighlight"
    )

    val textColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            Color.Black
        } else {
            if (isFromCurrentUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        },
        animationSpec = tween(durationMillis = 500),
        label = "TextColorHighlight"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 2.dp)
                .combinedClickable(
                    onClick = { },
                    onLongClick = onLongPress
                ),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Show reply bubble if this message is a reply
                if (message.isReply) {
                    ReplyBubbleContent(
                        message = message,
                        isFromCurrentUser = isFromCurrentUser,
                        currentUserId = currentUserId,
                        chatName = chatName,
                        onReplyClick = onReplyClick,
                        isParentHighlighted = isHighlighted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Show sender name only if it's a group and not the current user
                if (isGroup && !isFromCurrentUser) {
                    val senderName = message.getSenderName() ?: "User"
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Message content
                when (message.messageType) {
                    MessageType.TEXT -> {
                        val content = if (isGroup) {
                            formatMessageWithMentions(message.content)
                        } else {
                            AnnotatedString(message.content)
                        }

                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                    MessageType.IMAGE -> {
                        if (message.imageUrl == "PENDING_UPLOAD") {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Receiving photo...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        else {
                            val imageModel = message.localUri ?: message.imageUrl

                            if (imageModel != null) {
                                Box(contentAlignment = Alignment.Center) {
                                    GlideImage(
                                        model = imageModel,
                                        contentDescription = "Message image",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(enabled = message.deliveryStatus == MessageDeliveryStatus.SENT) {
                                                try {
                                                    val imageId = imageModel.hashCode().toString()

                                                    message.imageUrl?.let { url ->
                                                        if (url != "PENDING_UPLOAD") {
                                                            com.yerayyas.chatappkotlinproject.data.cache.ImageUrlStore.addImageUrl(
                                                                imageId,
                                                                url
                                                            )
                                                            navController.navigate("fullScreenImage/$imageId")
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e(
                                                        "UnifiedMessageBubble",
                                                        "Navigation error",
                                                        e
                                                    )
                                                }
                                            }

                                    )

                                    if (message.deliveryStatus == MessageDeliveryStatus.SENDING) {
                                        Box(
                                            modifier = Modifier
                                                .size(200.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Timestamp and message status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFromCurrentUser) {
                        if (message.deliveryStatus == MessageDeliveryStatus.SENT) {
                            MessageStatusIndicator(
                                readStatus = message.getReadStatus(),
                                timestamp = message.timestamp,
                                isOwnMessage = true,
                                showTime = true,
                                animated = true
                            )
                        } else if (message.deliveryStatus == MessageDeliveryStatus.SENDING) {
                            Text(
                                text = formatTimestamp(message.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reply preview
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ReplyPreview(
    replyToMessage: UnifiedMessage,
    currentUserId: String,
    chatName: String,
    onClearReply: () -> Unit,
    onReplyClick: () -> Unit = {}, // Optional callback for reply click
    modifier: Modifier = Modifier
) {
    // Get image information if it's a reply to an image
    val (imageUrl, messageContent, isImageMessage) = when (replyToMessage) {
        is UnifiedMessage.Individual -> {
            Triple(
                replyToMessage.imageUrl, // We can access the image in individual chats
                replyToMessage.content,
                replyToMessage.messageType == MessageType.IMAGE
            )
        }

        is UnifiedMessage.Group -> {
            Triple(
                replyToMessage.imageUrl,
                replyToMessage.content,
                replyToMessage.messageType == MessageType.IMAGE
            )
        }

        is UnifiedMessage.Pending -> TODO()
    }

    Card(
        modifier = modifier.clickable { onReplyClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val replyText = when (replyToMessage) {
                    is UnifiedMessage.Individual -> {
                        if (replyToMessage.senderId == currentUserId) {
                            "Replying to yourself"
                        } else {
                            "Replying to $chatName"
                        }
                    }

                    is UnifiedMessage.Group -> {
                        if (replyToMessage.senderId == currentUserId) {
                            "Replying to yourself"
                        } else {
                            val senderName = replyToMessage.getSenderName() ?: "User"
                            "Replying to $senderName"
                        }
                    }

                    is UnifiedMessage.Pending -> TODO()
                }
                Text(
                    text = replyText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Show message content with thumbnail if it's an image
                if (isImageMessage && !imageUrl.isNullOrEmpty()) {
                    // Show row with thumbnail + "Image" text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Thumbnail of the image
                        GlideImage(
                            model = imageUrl,
                            contentDescription = "Image thumbnail",
                            modifier = Modifier
                                .size(24.dp) // Smaller than in ReplyBubbleContent
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // "Image" text or caption if it exists
                        Text(
                            text = if (messageContent.isNotEmpty() && messageContent.isNotBlank()) {
                                messageContent // Show caption if it exists
                            } else {
                                "📷 Image" // Show "Image" with emoji if no caption
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Normal text message
                    Text(
                        text = messageContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onClearReply) {
                Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Empty chat state
 */
@Composable
private fun EmptyChatState(
    chatName: String,
    isGroup: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isGroup) Icons.Default.Group else Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isGroup) "Welcome to $chatName!" else "Start a conversation with $chatName!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isGroup)
                "Be the first to send a message to the group"
            else
                "Send your first message",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}


/**
 * Format message with mentions (@username)
 */
@Composable
private fun formatMessageWithMentions(message: String): AnnotatedString {
    return buildAnnotatedString {
        val mentionPattern = "@(\\w+)".toRegex()
        var lastIndex = 0

        mentionPattern.findAll(message).forEach { matchResult ->
            // Add normal text before the mention
            append(message.substring(lastIndex, matchResult.range.first))

            // Add the mention with special style
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(matchResult.value)
            }

            lastIndex = matchResult.range.last + 1
        }

        // Add the rest of the text
        append(message.substring(lastIndex))
    }
}

/**
 * Component that shows the original message content inside a reply
 * Simulates WhatsApp style with vertical line and soft background
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ReplyBubbleContent(
    message: UnifiedMessage,
    isFromCurrentUser: Boolean,
    currentUserId: String,
    chatName: String,
    onReplyClick: () -> Unit,
    isParentHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    // Get original message information
    val (originalContent, originalImageUrl, originalMessageType) = when (message) {
        is UnifiedMessage.Individual -> {
            Triple(
                message.message.replyToMessage,
                message.message.replyToImageUrl,
                message.message.replyToMessageType
            )
        }

        is UnifiedMessage.Group -> {
            val replyMsg = message.message.replyToMessage
            Triple(
                replyMsg?.message ?: "Message",
                replyMsg?.imageUrl,
                replyMsg?.messageType
            )
        }

        is UnifiedMessage.Pending -> Triple(
            "Replying...",
            null,
            MessageType.TEXT
        )
    }

    val originalSenderName = when (message) {
        is UnifiedMessage.Individual -> {
            val originalSenderId = message.message.replyToSenderId
            if (originalSenderId == currentUserId) "You" else chatName
        }

        is UnifiedMessage.Group -> {
            val originalSenderId = message.message.replyToMessage?.senderId
            val originalSenderName = message.message.replyToMessage?.senderName

            if (originalSenderId == currentUserId) {
                "You"
            } else {
                originalSenderName ?: "User"
            }
        }

        is UnifiedMessage.Pending -> "..."
    }

    // Determine if the original message is an image
    val isImageReply = when (originalMessageType) {
        MessageType.IMAGE -> true
        com.yerayyas.chatappkotlinproject.data.model.GroupMessageType.IMAGE -> true
        else -> false
    }

    // Colors that adapt to the message theme
    val replyBackgroundColor by animateColorAsState(
        targetValue = if (isFromCurrentUser) {
            if (isParentHighlighted) {
                Color(0xFFFFE082).copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
            }
        } else {
            if (isParentHighlighted) {
                Color(0xFFFFE082).copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            }
        },
        animationSpec = tween(durationMillis = 500),
        label = "ReplyBackgroundHighlight"
    )

    val replyTextColor by animateColorAsState(
        targetValue = if (isParentHighlighted) {
            Color.Black
        } else {
            if (isFromCurrentUser) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            }
        },
        animationSpec = tween(durationMillis = 500),
        label = "ReplyTextHighlight"
    )

    val replyLineColor by animateColorAsState(
        targetValue = if (isFromCurrentUser) {
            if (isParentHighlighted) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.onPrimary
            }
        } else {
            if (isParentHighlighted) {
                Color.Black
            } else {
                MaterialTheme.colorScheme.primary
            }
        },
        animationSpec = tween(durationMillis = 500),
        label = "ReplyLineHighlight"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = replyBackgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .clickable { onReplyClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical line characteristic of replies
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (isImageReply) 48.dp else 40.dp) // Adjusted height for images
                    .background(
                        color = replyLineColor,
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Original sender name
                Text(
                    text = originalSenderName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = replyLineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Original message content
                if (isImageReply && !originalImageUrl.isNullOrEmpty()) {
                    // Show small row with thumbnail + "Image" text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Thumbnail of the image
                        GlideImage(
                            model = originalImageUrl,
                            contentDescription = "Image thumbnail",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // "Image" text or caption if it exists
                        Text(
                            text = if (!originalContent.isNullOrEmpty() && originalContent != "Message") {
                                originalContent // Show caption if it exists
                            } else {
                                "📷 Image" // Show "Image" with emoji if no caption
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = replyTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Normal text message
                    Text(
                        text = originalContent ?: "Message",
                        style = MaterialTheme.typography.bodySmall,
                        color = replyTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp to show time
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val calendar = java.util.Calendar.getInstance()

    // Set calendar to the message date
    calendar.time = date
    val messageDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val messageYear = calendar.get(java.util.Calendar.YEAR)

    // Set calendar to current date
    calendar.time = now
    val currentDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val currentYear = calendar.get(java.util.Calendar.YEAR)

    return if (messageDay == currentDay && messageYear == currentYear) {
        // Same day - show only time
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        // Different day - show date and time
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    }
}
fun Modifier.simpleVerticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    minHeight: Dp = 20.dp,
    rightPadding: Dp = 4.dp,
    cornerRadius: Dp = 24.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.value, state.isScrollInProgress) {
        if (state.maxValue > 0) {
            isVisible = true
            if (!state.isScrollInProgress) {
                delay(600)
                isVisible = false
            }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "ScrollbarAlpha"
    )

    drawWithContent {
        drawContent()

        if (state.maxValue <= 0 || alpha == 0f) return@drawWithContent

        val radiusPx = cornerRadius.toPx()
        val rightPaddingPx = rightPadding.toPx()
        val minBarHeightPx = minHeight.toPx()

        val viewportHeight = this.size.height - (radiusPx * 2)
        val totalContentHeight = viewportHeight + state.maxValue

        val viewableRatio = if (totalContentHeight > 0) {
            viewportHeight / totalContentHeight
        } else {
            1f
        }

        val scrollBarHeightRaw = viewportHeight * viewableRatio
        val targetBarHeight = scrollBarHeightRaw.coerceIn(minBarHeightPx, viewportHeight)

        val scrollableTrackHeight = viewportHeight - targetBarHeight
        val scrollProgress = state.value.toFloat() / state.maxValue.toFloat()

        val offset = scrollProgress * scrollableTrackHeight
        val finalTopOffset = radiusPx + offset

        drawRoundRect(
            color = color,
            topLeft = Offset(
                x = this.size.width - width.toPx() - rightPaddingPx,
                y = finalTopOffset
            ),
            size = Size(width.toPx(), targetBarHeight),
            cornerRadius = CornerRadius(x = width.toPx() / 2, y = width.toPx() / 2),
            alpha = alpha
        )
    }
}

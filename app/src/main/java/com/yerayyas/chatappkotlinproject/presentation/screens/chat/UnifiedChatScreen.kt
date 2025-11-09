package com.yerayyas.chatappkotlinproject.presentation.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.presentation.components.ErrorState
import com.yerayyas.chatappkotlinproject.presentation.components.LoadingState
import com.yerayyas.chatappkotlinproject.presentation.components.UserStatusAndActions
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.IndividualAndGroupChatViewModel
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.UnifiedMessage
import com.yerayyas.chatappkotlinproject.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla unificada de chat que maneja tanto conversaciones individuales como grupales
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedChatScreen(
    chatId: String,
    chatType: ChatType,
    chatName: String, // Nombre del usuario o grupo
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

    // Inicializar el chat según el tipo
    LaunchedEffect(chatId, chatType) {
        when (chatType) {
            is ChatType.Individual -> viewModel.initializeIndividualChat(chatId)
            is ChatType.Group -> viewModel.initializeGroupChat(chatId)
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
            isAtBottom = true
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
        if (scrollToMessageId != null) {
            val index = messages.indexOfFirst { it.id == scrollToMessageId }
            if (index != -1) {
                // Calculate the visible area precisely
                val layoutInfo = listState.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height.toFloat()

                // TopAppBar height in pixels
                val topAppBarHeightPx = with(density) { Constants.TOP_APP_BAR_HEIGHT.toPx() }

                // LazyColumn content padding (8dp top + 8dp bottom)
                val lazyColumnContentPaddingPx = with(density) { 16.dp.toPx() }

                // Calculate the actual bottom boundary based on current state
                val bottomBoundaryPx = if (isKeyboardOpen) {
                    // When keyboard is open: viewport height - keyboard height
                    viewportHeight - imeBottomPx.toFloat()
                } else {
                    // When keyboard is closed: viewport height - input area height - navigation bar
                    viewportHeight - with(density) { 100.dp.toPx() } - navBarHeightPx.toFloat()
                }

                // Calculate the actual visible content area accounting for all padding
                val visibleAreaStart =
                    topAppBarHeightPx + with(density) { 8.dp.toPx() } // LazyColumn top padding
                val visibleAreaEnd =
                    bottomBoundaryPx - with(density) { 8.dp.toPx() } // LazyColumn bottom padding
                val visibleAreaHeight = visibleAreaEnd - visibleAreaStart

                // Ensure we have a positive visible area
                if (visibleAreaHeight <= 0) return@LaunchedEffect

                // First, scroll to the message to get its actual dimensions
                listState.scrollToItem(index)

                // Wait for layout to settle
                delay(150)

                // Get updated layout info after scrolling
                val updatedLayoutInfo = listState.layoutInfo
                val visibleItems = updatedLayoutInfo.visibleItemsInfo
                val targetItem = visibleItems.find { it.index == index }

                if (targetItem != null) {
                    val itemHeight = targetItem.size.toFloat()
                    val itemCurrentTop = targetItem.offset.toFloat()

                    // Calculate where the item's center currently is relative to LazyColumn
                    val itemCurrentCenter = itemCurrentTop + (itemHeight / 2)

                    // Calculate where we want the center to be (middle of visible area)
                    val desiredCenter = (visibleAreaHeight / 2)

                    // Calculate the offset needed to move the item center to desired center
                    val scrollOffset = (itemCurrentCenter - desiredCenter).toInt()

                    // Apply the scroll with animation
                    listState.animateScrollToItem(
                        index = index,
                        scrollOffset = scrollOffset
                    )
                } else {
                    // Fallback: if item is not visible after initial scroll
                    val estimatedItemHeight = with(density) { 80.dp.toPx() }
                    val desiredCenter = visibleAreaHeight / 2

                    // Calculate offset to center the estimated item
                    val scrollOffset = -(desiredCenter - (estimatedItemHeight / 2)).toInt()

                    listState.animateScrollToItem(
                        index = index,
                        scrollOffset = scrollOffset
                    )
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
                        message = "Cargando chat...",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = Constants.TOP_APP_BAR_HEIGHT)
                    )
                }

                error != null && messages.isEmpty() -> {
                    ErrorState(
                        message = error ?: "Error desconocido",
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
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
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
            onBackClick = { navController.popBackStack() },
            onInfoClick = {
                when (currentChatType) {
                    is ChatType.Group -> {
                        groupInfo?.let {
                            navController.navigate(Routes.GroupInfo.createRoute(it.id))
                        }
                    }

                    is ChatType.Individual -> {
                        // Navegar a perfil de usuario individual si es necesario
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
 * Barra superior unificada que se adapta al tipo de chat
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
                                text = "${groupInfo.memberIds.size} miembros",
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
                    contentDescription = "Volver"
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar mensajes"
                )
            }

            when (chatType) {
                is ChatType.Group -> {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información del grupo"
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
 * Barra de entrada de mensajes unificada con soporte para replies
 */
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
            // Show reply preview if replying to a message
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
                        contentDescription = "Adjuntar archivo"
                    )
                }

                TextField(
                    value = messageText,
                    onValueChange = onMessageTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje...") },
                    enabled = isEnabled,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (isSendEnabled) onSendMessage() })
                )

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
                        contentDescription = "Enviar mensaje",
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
 * Burbuja de mensaje unificada que se adapta al tipo de chat
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
                // Mostrar reply bubble si este mensaje es una respuesta
                if (message.isReply) {
                    ReplyBubbleContent(
                        message = message,
                        isFromCurrentUser = isFromCurrentUser,
                        isGroup = isGroup,
                        currentUserId = currentUserId,
                        chatName = chatName,
                        onReplyClick = onReplyClick,
                        isParentHighlighted = isHighlighted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Mostrar nombre del remitente solo si es grupo Y no es el usuario actual
                if (isGroup && !isFromCurrentUser) {
                    val senderName = message.getSenderName() ?: "Usuario"
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Contenido del mensaje
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
                        message.imageUrl?.let { imageUrl ->
                            GlideImage(
                                model = imageUrl,
                                contentDescription = "Imagen del mensaje",
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        try {
                                            val imageId = imageUrl.hashCode().toString()
                                            navController.navigate("fullScreenImage/$imageId")
                                            com.yerayyas.chatappkotlinproject.data.cache.ImageUrlStore.addImageUrl(
                                                imageId,
                                                imageUrl
                                            )
                                        } catch (e: Exception) {
                                            android.util.Log.e(
                                                "UnifiedMessageBubble",
                                                "Navigation error: ${e.message}"
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }

                // Timestamp y estado del mensaje
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )

                    if (isFromCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIndicator(
                            readStatus = message.getReadStatus(),
                            tint = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vista previa de respuesta unificada
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ReplyPreview(
    replyToMessage: UnifiedMessage,
    currentUserId: String,
    chatName: String,
    onClearReply: () -> Unit,
    onReplyClick: () -> Unit = {}, // Callback opcional para hacer click en el reply
    modifier: Modifier = Modifier
) {
    // Obtener información de la imagen si es un reply a imagen
    val (imageUrl, messageContent, isImageMessage) = when (replyToMessage) {
        is UnifiedMessage.Individual -> {
            Triple(
                replyToMessage.imageUrl, // SÍ podemos acceder a la imagen en chats individuales
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
                            "Respondiéndote a ti mismo"
                        } else {
                            "Respondiendo a $chatName"
                        }
                    }
                    is UnifiedMessage.Group -> {
                        if (replyToMessage.senderId == currentUserId) {
                            "Respondiéndote a ti mismo"
                        } else {
                            val senderName = replyToMessage.getSenderName() ?: "Usuario"
                            "Respondiendo a $senderName"
                        }
                    }
                }
                Text(
                    text = replyText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Mostrar contenido del mensaje con miniatura si es imagen
                if (isImageMessage && !imageUrl.isNullOrEmpty()) {
                    // Mostrar fila con miniatura + texto "Imagen"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Miniatura de la imagen
                        GlideImage(
                            model = imageUrl,
                            contentDescription = "Miniatura de imagen",
                            modifier = Modifier
                                .size(24.dp) // Más pequeño que en ReplyBubbleContent
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Texto "Imagen" o caption si existe
                        Text(
                            text = if (messageContent.isNotEmpty() && messageContent.isNotBlank()) {
                                messageContent // Mostrar caption si existe
                            } else {
                                "📷 Imagen" // Mostrar "Imagen" con emoji si no hay caption
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Mensaje de texto normal
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
 * Estado vacío cuando no hay mensajes
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
            text = if (isGroup) "¡Bienvenido a $chatName!" else "¡Comienza una conversación con $chatName!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isGroup)
                "Sé el primero en enviar un mensaje al grupo"
            else
                "Envía tu primer mensaje",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

/**
 * Indicador visual del estado del mensaje
 */
@Composable
private fun MessageStatusIndicator(
    readStatus: ReadStatus,
    tint: Color
) {
    val statusText = when (readStatus) {
        ReadStatus.SENT -> "✓"
        ReadStatus.DELIVERED -> "✓✓"
        ReadStatus.READ -> "✓✓"
    }

    val statusColor = if (readStatus == ReadStatus.READ) {
        Color(0xFF4CAF50) // Verde para leído
    } else {
        tint
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        color = statusColor,
        fontSize = 10.sp
    )
}

/**
 * Formatea el mensaje destacando las menciones (@usuario)
 */
@Composable
private fun formatMessageWithMentions(message: String): AnnotatedString {
    return buildAnnotatedString {
        val mentionPattern = "@(\\w+)".toRegex()
        var lastIndex = 0

        mentionPattern.findAll(message).forEach { matchResult ->
            // Agregar texto normal antes de la mención
            append(message.substring(lastIndex, matchResult.range.first))

            // Agregar la mención con estilo especial
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

        // Agregar el resto del texto
        append(message.substring(lastIndex))
    }
}

/**
 * Componente que muestra el contenido del mensaje original dentro de una respuesta
 * Simula el estilo de WhatsApp con línea vertical y fondo suave
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ReplyBubbleContent(
    message: UnifiedMessage,
    isFromCurrentUser: Boolean,
    isGroup: Boolean,
    currentUserId: String,
    chatName: String,
    onReplyClick: () -> Unit,
    isParentHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    // Obtener información del mensaje original
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
                replyMsg?.message ?: "Mensaje",
                replyMsg?.imageUrl,
                replyMsg?.messageType
            )
        }
    }

    val originalSenderName = when (message) {
        is UnifiedMessage.Individual -> {
            val originalSenderId = message.message.replyToSenderId
            if (originalSenderId == currentUserId) "Tú" else chatName
        }

        is UnifiedMessage.Group -> {
            val originalSenderId = message.message.replyToMessage?.senderId
            val originalSenderName = message.message.replyToMessage?.senderName

            if (originalSenderId == currentUserId) {
                "Tú"
            } else {
                originalSenderName ?: "Usuario"
            }
        }
    }

    // Determinar si el mensaje original es una imagen
    val isImageReply = when (originalMessageType) {
        MessageType.IMAGE -> true
        com.yerayyas.chatappkotlinproject.data.model.GroupMessageType.IMAGE -> true
        else -> false
    }

    // Colores que se adaptan al tema del mensaje
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
        colors = CardDefaults.cardColors(
            containerColor = replyBackgroundColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .clickable { onReplyClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Línea vertical característica de los replies
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (isImageReply) 48.dp else 40.dp) // Altura ajustada para imágenes
                    .background(
                        color = replyLineColor,
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Nombre del remitente original
                Text(
                    text = originalSenderName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = replyLineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Contenido del mensaje original
                if (isImageReply && !originalImageUrl.isNullOrEmpty()) {
                    // Mostrar una pequeña fila con miniatura + texto "Imagen"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Miniatura de la imagen
                        GlideImage(
                            model = originalImageUrl,
                            contentDescription = "Miniatura de imagen",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Texto "Imagen" o caption si existe
                        Text(
                            text = if (!originalContent.isNullOrEmpty() && originalContent != "Mensaje") {
                                originalContent // Mostrar caption si existe
                            } else {
                                "📷 Imagen" // Mostrar "Imagen" con emoji si no hay caption
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = replyTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Mensaje de texto normal
                    Text(
                        text = originalContent ?: "Mensaje",
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
 * Formatea el timestamp para mostrar la hora
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
        // Mismo día - mostrar solo hora
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        // Día diferente - mostrar fecha y hora
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    }
}
package com.yerayyas.chatappkotlinproject.presentation.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.yerayyas.chatappkotlinproject.presentation.components.ErrorState
import com.yerayyas.chatappkotlinproject.presentation.components.LoadingState
import com.yerayyas.chatappkotlinproject.presentation.components.UserStatusAndActions
import com.yerayyas.chatappkotlinproject.presentation.components.chat.EmptyChatState
import com.yerayyas.chatappkotlinproject.presentation.components.chat.UnifiedChatTopBar
import com.yerayyas.chatappkotlinproject.presentation.components.chat.UnifiedMessageBubble
import com.yerayyas.chatappkotlinproject.presentation.components.chat.UnifiedMessageInputBar
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.ChatType
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat.IndividualAndGroupChatViewModel
import com.yerayyas.chatappkotlinproject.utils.Constants
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * The main Composable screen acting as the **Orchestrator** for individual and group chats.
 *
 * This function is primarily responsible for **state observation**, **layout orchestration**,
 * and **complex interaction logic**, such as keyboard handling, dynamic scrolling, and lifecycle management.
 * All complex UI rendering has been delegated to stateless or stateful components (e.g., [UnifiedMessageBubble],
 * [UnifiedMessageInputBar], [UnifiedChatTopBar]).
 *
 * It manages the synchronization between the UI, the [IndividualAndGroupChatViewModel], and Android's
 * system-level features like [WindowInsetsCompat] (for keyboard handling).
 *
 * @param chatId The ID of the current chat (user ID or group ID).
 * @param chatType The static type of the chat (Individual or Group).
 * @param chatName The name of the chat partner or group for display.
 * @param navController The navigation host controller for screen transitions.
 * @param viewModel The Hilt-injected ViewModel managing chat state and business logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedChatScreen(
    chatId: String,
    chatType: ChatType,
    chatName: String,
    navController: NavHostController,
    viewModel: IndividualAndGroupChatViewModel = hiltViewModel()
) {
    // --- State Collection ---
    val currentChatType by viewModel.chatType.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val groupInfo by viewModel.groupInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val replyToMessage by viewModel.replyToMessage.collectAsState()
    val scrollToMessageId by viewModel.scrollToMessageId.collectAsState()
    val highlightedMessageId by viewModel.highlightedMessageId.collectAsState()

    // --- UI/System State Management ---
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var inputBarHeightPx by remember { mutableIntStateOf(0) }

    val view = LocalView.current
    var imeBottomPx by remember { mutableIntStateOf(0) } // Keyboard height in pixels
    var navBarHeightPx by remember { mutableIntStateOf(0) } // Navigation bar height in pixels

    // Track if user is at the bottom of the message list
    var isAtBottom by remember { mutableStateOf(true) }

    // Make it survive if we navigate to another screen and back to the chat
    var hasPerformedInitialScroll by rememberSaveable { mutableStateOf(false) }

    // Track if keyboard is currently open
    var isKeyboardOpen by remember { mutableStateOf(false) }

    // --- Lifecycle and Initialization ---

    /**
     * Observes lifecycle events to inform the ViewModel when the chat screen is visible or hidden,
     * allowing it to manage resources (e.g., marking messages as read).
     */
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.onChatResumed()
                }

                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onChatPaused()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    /**
     * Handles Android [WindowInsetsCompat] to track keyboard and navigation bar visibility/height.
     * This is crucial for dynamic layout adjustments when the soft keyboard is active.
     */
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

    /**
     * Initializes the chat based on the provided [chatId] and [chatType].
     */
    LaunchedEffect(chatId, chatType) {
        when (chatType) {
            is ChatType.Individual -> viewModel.initializeIndividualChat(chatId)
            is ChatType.Group -> viewModel.initializeGroupChat(chatId)
        }
    }

    // --- Dynamic Layout and Scrolling Logic ---

    // Calculate smart dynamic padding to ensure the last message is visible above the keyboard.
    val smartBottomPadding = with(density) {
        val baseHeight = if (inputBarHeightPx > 0) inputBarHeightPx.toDp() else 80.dp

        if (isKeyboardOpen) {
            val keyboardHeightDp = (imeBottomPx / density.density).dp
            val navBarHeightDp = (navBarHeightPx / density.density).dp

            // We subtract navBarHeightDp because the inputOffset is already compensating
            // for it to align with the actual keyboard edge.
            keyboardHeightDp + baseHeight - navBarHeightDp
        } else {
            baseHeight
        }
    }

    // Calculate the simple offset for the input area to follow the IME (keyboard) edge
    val inputOffset = if (imeBottomPx > 0) -(imeBottomPx - navBarHeightPx) else 0

    /**
     * Monitors scroll state to update [isAtBottom] efficiently using LayoutInfo.
     */
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems == 0) {
                    isAtBottom = true
                    return@collect
                }

                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@collect

                // We verify if the last item is the last item in the list
                val lastIndex = totalItems - 1

                // Considered "at bottom" if the last visible item (or almost the last)
                // is at the edge or very close to the end of the list.
                isAtBottom = lastVisibleItem.index >= lastIndex - 1
            }
    }

    /**
     * EFFECT 1: Handles the VERY FIRST scroll to the bottom when the screen is created.
     */
    LaunchedEffect(messages.firstOrNull()?.id) {
        if (messages.isNotEmpty() && !hasPerformedInitialScroll) {
            scope.launch {
                listState.scrollToItem(messages.size - 1)
                isAtBottom = true
                hasPerformedInitialScroll = true
            }
        }
    }

    /**
     * EFFECT 2: Handles AUTO-SCROLLING for NEW messages AFTER the initial load.
     * Uses snapshotFlow.drop(1) to avoid race conditions with initial state restoration.
     */
    LaunchedEffect(listState) {
        snapshotFlow { messages }
            .drop(1) // Drop the first emission to avoid race conditions
            .collect { currentMessages ->
                val lastMessage = currentMessages.lastOrNull() ?: return@collect
                val shouldScroll = lastMessage.isSentBy(viewModel.getCurrentUserId()) || isAtBottom

                if (shouldScroll) {
                    delay(300)

                    listState.scrollToItem(currentMessages.size - 1)
                    isAtBottom = true
                }
            }
    }

    /**
     * Ensures the view scrolls to the bottom when the keyboard is opened, only if the user was already at the bottom.
     */
    LaunchedEffect(isKeyboardOpen, isAtBottom) {
        if (isKeyboardOpen && isAtBottom && messages.isNotEmpty()) {

            delay(100)

            listState.scrollToItem(messages.size - 1)
        }
    }

    /**
     * Handles scrolling to a specific message ID (e.g., when clicking a reply preview).
     * It uses [snapshotFlow] to ensure the scroll position centers the target message within the viewport.
     */
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

                            // Cancel the flow once the item is found and centered
                            this.cancel()
                        }
                    }
            }
        }
    }

    // --- Action Handlers ---

    /**
     * Handles displaying errors via the SnackbarHost
     */
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

    // --- UI Structure (Orchestration) ---

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat Content Area
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
                            .padding(bottom = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = smartBottomPadding
                        )
                    ) {
                        // Delegates rendering of each message to UnifiedMessageBubble component
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
                    .offset { IntOffset(x = 0, y = inputOffset) } // Dynamic offset for keyboard
                    .background(MaterialTheme.colorScheme.surface)
                    .onGloballyPositioned { coordinates ->
                        if (inputBarHeightPx != coordinates.size.height) {
                            inputBarHeightPx = coordinates.size.height
                        }
                    }
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

        // SnackbarHost positioned to avoid overlap with input area/keyboard
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isKeyboardOpen) 160.dp else 80.dp)
        )
    }
}

package com.yerayyas.chatappkotlinproject.presentation.screens.group

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
import androidx.compose.material3.OutlinedTextField
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
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.presentation.components.ErrorState
import com.yerayyas.chatappkotlinproject.presentation.components.LoadingState
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.group.GroupChatViewModel
import com.yerayyas.chatappkotlinproject.utils.AppState
import com.yerayyas.chatappkotlinproject.utils.Constants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal de chat grupal con mensajería en tiempo real
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    navController: NavController,
    viewModel: GroupChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val groupInfo by viewModel.groupInfo.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
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
            // Get keyboard height in dp
            val keyboardHeightDp = (imeBottomPx / density.density).dp
            // Get input area height (estimated with some padding)
            val inputAreaHeight = 100.dp // Slightly more realistic estimate

            // Simple calculation: keyboard height + input area height + small margin
            keyboardHeightDp + inputAreaHeight - Constants.TOP_APP_BAR_HEIGHT
        }
    } else {
        80.dp // Normal padding
    }

    // Monitor scroll position to determine if user is at the bottom
    // Only update isAtBottom when keyboard is not open to prevent closing it
    LaunchedEffect(listState.isScrollInProgress, isKeyboardOpen) {
        if (!listState.isScrollInProgress && messages.isNotEmpty() && !isKeyboardOpen) {
            val visibleInfo = listState.layoutInfo.visibleItemsInfo
            val lastVisibleIndex = visibleInfo.lastOrNull()?.index ?: -1
            val totalItems = messages.size

            // Consider "at bottom" if we can see the last message or the second-to-last
            isAtBottom = lastVisibleIndex >= totalItems - 2
        }
    }

    // Calculate simple offset for input area
    val inputOffset = if (imeBottomPx > 0) -(imeBottomPx - navBarHeightPx) else 0

    // Inicializar el chat al entrar
    LaunchedEffect(groupId) {
        viewModel.initializeGroupChat(groupId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            // When auto-scrolling to newest message, user is at bottom
            isAtBottom = true
        }
    }

    // Handle error states
    LaunchedEffect(uiState.error) {
        val currentError = uiState.error
        if (currentError != null) {
            scope.launch {
                snackbarHostState.showSnackbar(currentError)
                viewModel.clearError()
            }
        }
    }

    // Track currently open group chat in global app state
    DisposableEffect(groupId) {
        val appState = viewModel.getAppState()
        appState.currentOpenGroupChatId = groupId
        onDispose {
            if (appState.currentOpenGroupChatId == groupId) {
                appState.currentOpenGroupChatId = null
            }
        }
    }

    // Send message action
    val sendMessage = {
        if (!uiState.isLoading && messageText.trim().isNotEmpty()) {
            viewModel.sendMessage()
            // When sending a message, user should be considered at bottom
            isAtBottom = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat content - positioned behind TopAppBar
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Add top padding equivalent to TopAppBar height to avoid overlap
            when {
                uiState.isLoading && messages.isEmpty() -> {
                    LoadingState(
                        message = "Cargando chat...",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = Constants.TOP_APP_BAR_HEIGHT)
                    )
                }

                uiState.error != null && messages.isEmpty() -> {
                    ErrorState(
                        message = uiState.error ?: "Error desconocido",
                        onRetry = { viewModel.initializeGroupChat(groupId) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = Constants.TOP_APP_BAR_HEIGHT)
                    )
                }

                messages.isEmpty() -> {
                    EmptyGroupChatState(
                        groupName = groupInfo?.name ?: "Grupo",
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
                            .padding(top = Constants.TOP_APP_BAR_HEIGHT) // TopAppBar height
                            .padding(bottom = smartBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            vertical = 8.dp
                        )
                    ) {
                        items(messages) { message ->
                            GroupMessageBubble(
                                message = message,
                                isFromCurrentUser = message.senderId == viewModel.getCurrentUserId(),
                                groupMembers = groupInfo?.memberIds ?: emptyList()
                            )
                        }
                    }
                }
            }
        }

        // Input area
        if (groupInfo != null && viewModel.canSendMessages()) {
            MessageInputBar(
                messageText = messageText,
                onMessageTextChange = viewModel::updateMessageText,
                onSendMessage = sendMessage,
                isEnabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp)
                    .offset { IntOffset(x = 0, y = inputOffset) }
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        // TopAppBar - fixed at top with proper window insets
        GroupChatTopBar(
            groupInfo = groupInfo,
            onBackClick = { navController.popBackStack() },
            onInfoClick = {
                groupInfo?.let {
                    navController.navigate(Routes.GroupInfo.createRoute(it.id))
                }
            },
            onSearchClick = {
                navController.navigate(Routes.Search.createRoute(groupId))
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
 * Barra superior del chat grupal con información del grupo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupChatTopBar(
    groupInfo: GroupChat?,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = groupInfo?.name ?: "Grupo",
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
            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Información del grupo"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}

/**
 * Barra de entrada de mensajes con botón de envío
 */
@Composable
private fun MessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
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

/**
 * Burbuja de mensaje para chat grupal con información del remitente
 */
@Composable
private fun GroupMessageBubble(
    message: GroupMessage,
    isFromCurrentUser: Boolean,
    groupMembers: List<String>
) {
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 2.dp),
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
                // Mostrar nombre del remitente solo si no es el usuario actual
                if (!isFromCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Contenido del mensaje con menciones resaltadas
                Text(
                    text = formatMessageWithMentions(message.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

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
                            readStatus = message.readStatus,
                            tint = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Estado vacío cuando no hay mensajes en el grupo
 */
@Composable
private fun EmptyGroupChatState(
    groupName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "¡Bienvenido a $groupName!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sé el primero en enviar un mensaje al grupo",
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
    readStatus: com.yerayyas.chatappkotlinproject.data.model.ReadStatus,
    tint: Color
) {
    val statusText = when (readStatus) {
        com.yerayyas.chatappkotlinproject.data.model.ReadStatus.SENT -> "✓"
        com.yerayyas.chatappkotlinproject.data.model.ReadStatus.DELIVERED -> "✓✓"
        com.yerayyas.chatappkotlinproject.data.model.ReadStatus.READ -> "✓✓"
    }

    val statusColor =
        if (readStatus == com.yerayyas.chatappkotlinproject.data.model.ReadStatus.READ) {
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
 * Obtiene el nombre de display del remitente
 */
// Removed this function as it's no longer needed

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
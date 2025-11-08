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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.presentation.components.ErrorState
import com.yerayyas.chatappkotlinproject.presentation.components.LoadingState
import com.yerayyas.chatappkotlinproject.presentation.navigation.Routes
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.group.GroupChatViewModel
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

    // Inicializar el chat al entrar
    LaunchedEffect(groupId) {
        viewModel.initializeGroupChat(groupId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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

    Scaffold(
        topBar = {
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
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (groupInfo != null && viewModel.canSendMessages()) {
                MessageInputBar(
                    messageText = messageText,
                    onMessageTextChange = viewModel::updateMessageText,
                    onSendClick = { viewModel.sendMessage() },
                    isEnabled = !uiState.isLoading
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && messages.isEmpty() -> {
                    LoadingState(
                        message = "Cargando chat...",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.error != null && messages.isEmpty() -> {
                    ErrorState(
                        message = uiState.error ?: "Error desconocido",
                        onRetry = { viewModel.initializeGroupChat(groupId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                messages.isEmpty() -> {
                    EmptyGroupChatState(
                        groupName = groupInfo?.name ?: "Grupo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
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
    onSearchClick: () -> Unit
) {
    CenterAlignedTopAppBar(
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
        )
    )
}

/**
 * Barra de entrada de mensajes con botón de envío
 */
@Composable
private fun MessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") },
                enabled = isEnabled,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = isEnabled && messageText.trim().isNotEmpty(),
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

    return if (date.day == now.day && date.month == now.month && date.year == now.year) {
        // Mismo día - mostrar solo hora
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        // Día diferente - mostrar fecha y hora
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    }
}
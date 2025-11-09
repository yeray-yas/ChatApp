package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.*
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.*
import com.yerayyas.chatappkotlinproject.domain.usecases.group.GetUserGroupsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.group.ManageGroupMembersUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.group.SendGroupMessageUseCase
import com.yerayyas.chatappkotlinproject.utils.AppState
import com.yerayyas.chatappkotlinproject.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tipo de chat que maneja el ViewModel
 */
sealed class ChatType {
    object Individual : ChatType()
    object Group : ChatType()
}

/**
 * ViewModel unificado para manejar tanto chats individuales como grupales
 */
@HiltViewModel
class IndividualAndGroupChatViewModel @Inject constructor(
    // Use cases para chat individual
    private val loadChatMessagesUseCase: LoadChatMessagesUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    private val sendTextMessageReplyUseCase: SendTextMessageReplyUseCase,
    private val sendImageMessageReplyUseCase: SendImageMessageReplyUseCase,
    getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val cancelChatNotificationsUseCase: CancelChatNotificationsUseCase,

    // Use cases para chat grupal
    private val groupChatRepository: GroupChatRepository,
    private val userRepository: UserRepository,
    firebaseAuth: FirebaseAuth,
    private val sendGroupMessageUseCase: SendGroupMessageUseCase,
    private val getUserGroupsUseCase: GetUserGroupsUseCase,
    private val manageGroupMembersUseCase: ManageGroupMembersUseCase,

    val appState: AppState
) : ViewModel() {

    // Estados comunes
    private val _chatType = MutableStateFlow<ChatType>(ChatType.Individual)
    val chatType: StateFlow<ChatType> = _chatType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    // Estados para chat individual
    private val _individualMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _replyToIndividualMessage = MutableStateFlow<ChatMessage?>(null)
    val replyToIndividualMessage: StateFlow<ChatMessage?> = _replyToIndividualMessage.asStateFlow()

    private val _scrollToMessageId = MutableStateFlow<String?>(null)
    val scrollToMessageId: StateFlow<String?> = _scrollToMessageId.asStateFlow()

    private val _highlightedMessageId = MutableStateFlow<String?>(null)
    val highlightedMessageId: StateFlow<String?> = _highlightedMessageId.asStateFlow()

    // Estados para chat grupal
    private val _groupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    private val _groupInfo = MutableStateFlow<GroupChat?>(null)
    val groupInfo: StateFlow<GroupChat?> = _groupInfo.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<User>>(emptyList())
    val groupMembers: StateFlow<List<User>> = _groupMembers.asStateFlow()

    private val _replyToGroupMessage = MutableStateFlow<GroupMessage?>(null)
    val replyToGroupMessage: StateFlow<GroupMessage?> = _replyToGroupMessage.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    private val _mentionedUsers = MutableStateFlow<List<String>>(emptyList())
    val mentionedUsers: StateFlow<List<String>> = _mentionedUsers.asStateFlow()

    // Estados unificados (expuestos)
    val messages: StateFlow<List<UnifiedMessage>> = combine(
        _individualMessages,
        _groupMessages,
        _chatType
    ) { individualMsgs, groupMsgs, type ->
        when (type) {
            is ChatType.Individual -> individualMsgs.map { UnifiedMessage.Individual(it) }
            is ChatType.Group -> groupMsgs.map { UnifiedMessage.Group(it) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val replyToMessage: StateFlow<UnifiedMessage?> = combine(
        _replyToIndividualMessage,
        _replyToGroupMessage,
        _chatType
    ) { individualReply, groupReply, type ->
        when (type) {
            is ChatType.Individual -> individualReply?.let { UnifiedMessage.Individual(it) }
            is ChatType.Group -> groupReply?.let { UnifiedMessage.Group(it) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val currentUserId = firebaseAuth.currentUser?.uid ?: getCurrentUserIdUseCase()
    private var currentChatId: String = ""

    /**
     * Obtiene el ID del usuario actual
     */
    fun getCurrentUserId(): String = currentUserId

    /**
     * Inicializa el chat individual
     */
    fun initializeIndividualChat(otherUserId: String) {
        currentChatId = otherUserId
        _chatType.value = ChatType.Individual
        _isLoading.value = true
        _error.value = null

        // Cancelar notificaciones del chat
        viewModelScope.launch {
            cancelChatNotificationsUseCase(otherUserId)
        }

        // Limpiar mensajes grupales y cargar individuales
        _groupMessages.value = emptyList()

        loadChatMessagesUseCase(otherUserId)
            .onEach { messagesList ->
                _individualMessages.value = messagesList
                _isLoading.value = false
            }
            .catch { exception ->
                _error.value = "${Constants.ERROR_LOADING_MESSAGES}: ${exception.message}"
                _isLoading.value = false
            }
            .launchIn(viewModelScope)

        // Marcar como chat abierto en AppState
        appState.currentOpenChatUserId = otherUserId
    }

    /**
     * Inicializa el chat grupal
     */
    fun initializeGroupChat(groupId: String) {
        currentChatId = groupId
        _chatType.value = ChatType.Group
        println("DEBUG: Initializing group chat for group: $groupId")

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                println("DEBUG: Starting group initialization")

                // Limpiar mensajes individuales
                _individualMessages.value = emptyList()

                // Cancelar notificaciones del grupo
                cancelChatNotificationsUseCase.cancelGroupNotifications(groupId)

                // Cargar información del grupo
                loadGroupInfo(groupId)

                // Cargar miembros del grupo
                loadGroupMembers(groupId)

                // Cargar mensajes del grupo
                loadGroupMessages(groupId)

                // Marcar mensajes como leídos
                markGroupMessagesAsRead(groupId)

                _isLoading.value = false
                println("DEBUG: Group initialization completed successfully")
            } catch (e: Exception) {
                println("DEBUG: Error during group initialization: ${e.message}")
                _error.value = "Error al cargar el chat grupal: ${e.message}"
                _isLoading.value = false
            }
        }

        // Marcar como chat grupal abierto en AppState
        appState.currentOpenGroupChatId = groupId
    }

    /**
     * Envía un mensaje (texto o imagen dependiendo del tipo de chat)
     */
    fun sendMessage(receiverId: String? = null, imageUri: Uri? = null) {
        when (_chatType.value) {
            is ChatType.Individual -> {
                if (imageUri != null) {
                    sendIndividualImageMessage(receiverId ?: currentChatId, imageUri)
                } else {
                    sendIndividualTextMessage(receiverId ?: currentChatId)
                }
            }

            is ChatType.Group -> {
                if (imageUri != null) {
                    sendGroupImageMessage(imageUri)
                } else {
                    sendGroupTextMessage()
                }
            }
        }
    }

    private fun sendIndividualTextMessage(receiverId: String) {
        val trimmed = _messageText.value.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                val currentReplyTo = _replyToIndividualMessage.value
                if (currentReplyTo != null) {
                    sendTextMessageReplyUseCase(receiverId, trimmed, currentReplyTo)
                    clearReply()
                } else {
                    sendTextMessageUseCase(receiverId, trimmed)
                }
                _messageText.value = ""
            }.onFailure { e ->
                _error.value = "${Constants.ERROR_SENDING_MESSAGE}: ${e.message}"
            }
        }
    }

    private fun sendIndividualImageMessage(receiverId: String, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val currentReplyTo = _replyToIndividualMessage.value
                if (currentReplyTo != null) {
                    sendImageMessageReplyUseCase(receiverId, uri, currentReplyTo)
                    clearReply()
                } else {
                    sendImageMessageUseCase(receiverId, uri)
                }
            }.onFailure { e ->
                _error.value = "${Constants.ERROR_SENDING_IMAGE}: ${e.message}"
            }.also {
                _isLoading.value = false
            }
        }
    }

    private fun sendGroupTextMessage() {
        val messageContent = _messageText.value.trim()
        if (messageContent.isEmpty() || currentChatId.isEmpty()) return

        viewModelScope.launch {
            try {
                val currentUser = getCurrentUserInfo()
                val senderName = currentUser?.username ?: "Usuario"
                val senderImageUrl = currentUser?.profileImage

                val processedMessage = processMentions(messageContent)
                val mentions = extractMentions(processedMessage)

                val currentReplyTo = _replyToGroupMessage.value

                val result = if (currentReplyTo != null) {
                    sendGroupMessageUseCase.sendTextMessage(
                        groupId = currentChatId,
                        message = processedMessage,
                        senderName = senderName,
                        senderImageUrl = senderImageUrl,
                        mentionedUsers = mentions,
                        replyToMessageId = currentReplyTo.id,
                        replyToMessage = currentReplyTo
                    )
                } else {
                    sendGroupMessageUseCase.sendTextMessage(
                        groupId = currentChatId,
                        message = processedMessage,
                        senderName = senderName,
                        senderImageUrl = senderImageUrl,
                        mentionedUsers = mentions
                    )
                }

                if (result.isSuccess) {
                    _messageText.value = ""
                    clearReply()
                    _mentionedUsers.value = emptyList()
                } else {
                    _error.value = "Error al enviar mensaje: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                _error.value = "Error al enviar mensaje: ${e.message}"
            }
        }
    }

    private fun sendGroupImageMessage(imageUri: Uri, caption: String = "") {
        if (currentChatId.isEmpty()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val currentUser = getCurrentUserInfo()
                val senderName = currentUser?.username ?: "Usuario"
                val senderImageUrl = currentUser?.profileImage

                val currentReplyTo = _replyToGroupMessage.value

                val result = sendGroupMessageUseCase.sendImageMessage(
                    groupId = currentChatId,
                    imageUri = imageUri,
                    caption = caption,
                    senderName = senderName,
                    senderImageUrl = senderImageUrl,
                    mentionedUsers = extractMentions(caption),
                    replyToMessageId = currentReplyTo?.id,
                    replyToMessage = currentReplyTo
                )

                if (result.isSuccess) {
                    clearReply()
                } else {
                    _error.value = "Error al enviar imagen: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                _error.value = "Error al enviar imagen: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza el texto del mensaje
     */
    fun updateMessageText(text: String) {
        _messageText.value = text

        // Solo para chats grupales - gestionar indicador de "escribiendo"
        if (_chatType.value is ChatType.Group) {
            if (text.isNotEmpty() && !_isTyping.value) {
                _isTyping.value = true
            } else if (text.isEmpty() && _isTyping.value) {
                _isTyping.value = false
            }
            _mentionedUsers.value = extractMentions(text)
        }
    }

    /**
     * Establece mensaje para responder
     */
    fun setReplyToMessage(message: UnifiedMessage) {
        when (message) {
            is UnifiedMessage.Individual -> _replyToIndividualMessage.value = message.message
            is UnifiedMessage.Group -> _replyToGroupMessage.value = message.message
        }
    }

    /**
     * Cancela respuesta
     */
    fun clearReply() {
        _replyToIndividualMessage.value = null
        _replyToGroupMessage.value = null
    }

    /**
     * Scroll a mensaje original (funciona tanto para chat individual como grupal)
     */
    fun scrollToOriginalMessage(messageId: String) {
        viewModelScope.launch {
            _scrollToMessageId.value = messageId
            _highlightedMessageId.value = messageId
            delay(100)
            _scrollToMessageId.value = null
            delay(700)
            _highlightedMessageId.value = null
        }
    }

    /**
     * Extrae el ID del mensaje original desde un UnifiedMessage que es un reply
     */
    fun getOriginalMessageId(replyMessage: UnifiedMessage): String? {
        return when (replyMessage) {
            is UnifiedMessage.Individual -> {
                if (replyMessage.isReply) {
                    replyMessage.message.replyToMessageId
                } else null
            }
            is UnifiedMessage.Group -> {
                if (replyMessage.isReply) {
                    replyMessage.message.replyToMessage?.id
                } else null
            }
        }
    }

    /**
     * Verifica si el usuario actual puede enviar mensajes
     */
    fun canSendMessages(): Boolean {
        return when (_chatType.value) {
            is ChatType.Individual -> true
            is ChatType.Group -> {
                val group = _groupInfo.value ?: return false
                group.canSendMessages(currentUserId)
            }
        }
    }

    /**
     * Verifica si el usuario actual es administrador (solo para grupos)
     */
    fun isCurrentUserAdmin(): Boolean {
        return when (_chatType.value) {
            is ChatType.Individual -> false
            is ChatType.Group -> {
                val group = _groupInfo.value ?: return false
                group.isAdmin(currentUserId)
            }
        }
    }

    /**
     * Limpia errores
     */
    fun clearError() {
        _error.value = null
    }

    // Métodos privados para chat grupal
    private suspend fun loadGroupInfo(groupId: String) {
        try {
            val group = getUserGroupsUseCase.getGroupById(groupId)
            _groupInfo.value = group
        } catch (e: Exception) {
            throw Exception("No se pudo cargar la información del grupo")
        }
    }

    private suspend fun loadGroupMembers(groupId: String) {
        try {
            val group = _groupInfo.value ?: return
            val memberIds = group.memberIds
            val members = userRepository.getUsersByIds(memberIds)
            _groupMembers.value = members
        } catch (e: Exception) {
            throw Exception("No se pudieron cargar los miembros del grupo")
        }
    }

    private fun loadGroupMessages(groupId: String) {
        try {
            println("DEBUG: Loading messages for group: $groupId")

            viewModelScope.launch {
                groupChatRepository.getGroupMessages(groupId).collect { messagesList ->
                    println("DEBUG: Received ${messagesList.size} messages")
                    _groupMessages.value = messagesList.sortedBy { it.timestamp }
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error loading messages: ${e.message}")
            _groupMessages.value = emptyList()
        }
    }

    private suspend fun getCurrentUserInfo(): User? {
        return try {
            userRepository.getCurrentUser()
        } catch (e: Exception) {
            null
        }
    }

    private fun markGroupMessagesAsRead(groupId: String) {
        viewModelScope.launch {
            try {
                val unreadMessages = _groupMessages.value.filter { message ->
                    message.senderId != currentUserId && !message.isReadBy(currentUserId)
                }

                unreadMessages.forEach { message ->
                    groupChatRepository.markMessageAsRead(groupId, message.id, currentUserId)
                }
            } catch (e: Exception) {
                // Handle read receipt errors silently
            }
        }
    }

    private fun processMentions(text: String): String {
        val mentionPattern = "@(\\w+)".toRegex()
        return mentionPattern.replace(text) { matchResult ->
            val username = matchResult.groupValues[1]
            val mentionedUser = _groupMembers.value.find {
                it.username.equals(username, ignoreCase = true)
            }
            if (mentionedUser != null) {
                "@${mentionedUser.username}"
            } else {
                matchResult.value
            }
        }
    }

    private fun extractMentions(text: String): List<String> {
        val mentionPattern = "@(\\w+)".toRegex()
        return mentionPattern.findAll(text).mapNotNull { matchResult ->
            val username = matchResult.groupValues[1]
            _groupMembers.value.find {
                it.username.equals(username, ignoreCase = true)
            }?.id
        }.toList()
    }

    override fun onCleared() {
        super.onCleared()
        // Limpiar estados de AppState
        when (_chatType.value) {
            is ChatType.Individual -> {
                if (appState.currentOpenChatUserId == currentChatId) {
                    appState.currentOpenChatUserId = null
                }
            }

            is ChatType.Group -> {
                if (appState.currentOpenGroupChatId == currentChatId) {
                    appState.currentOpenGroupChatId = null
                }
            }
        }
    }
}

/**
 * Mensaje unificado que puede ser individual o grupal
 */
sealed class UnifiedMessage {
    abstract val id: String
    abstract val senderId: String
    abstract val content: String
    abstract val timestamp: Long
    abstract val messageType: MessageType
    abstract val imageUrl: String?
    abstract val isReply: Boolean

    data class Individual(val message: ChatMessage) : UnifiedMessage() {
        override val id: String = message.id
        override val senderId: String = message.senderId
        override val content: String = message.message
        override val timestamp: Long = message.timestamp
        override val messageType: MessageType = message.messageType
        override val imageUrl: String? = message.imageUrl
        override val isReply: Boolean = message.isReply()
    }

    data class Group(val message: GroupMessage) : UnifiedMessage() {
        override val id: String = message.id
        override val senderId: String = message.senderId
        override val content: String = message.message
        override val timestamp: Long = message.timestamp
        override val messageType: MessageType = when (message.messageType) {
            GroupMessageType.IMAGE -> MessageType.IMAGE
            else -> MessageType.TEXT
        }
        override val imageUrl: String? = message.imageUrl
        override val isReply: Boolean = message.isReply()
    }

    /**
     * Verifica si el mensaje fue enviado por un usuario específico
     */
    fun isSentBy(userId: String): Boolean = senderId == userId

    /**
     * Obtiene el nombre del remitente (solo para mensajes grupales)
     */
    fun getSenderName(): String? = when (this) {
        is Individual -> null
        is Group -> message.senderName
    }

    /**
     * Obtiene el status de lectura
     */
    fun getReadStatus(): ReadStatus = when (this) {
        is Individual -> message.readStatus
        is Group -> message.readStatus
    }
}
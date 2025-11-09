package com.yerayyas.chatappkotlinproject.presentation.viewmodel.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.repository.UserRepository
import com.yerayyas.chatappkotlinproject.domain.usecases.group.SendGroupMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.group.GetUserGroupsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.group.ManageGroupMembersUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.CancelChatNotificationsUseCase
import com.yerayyas.chatappkotlinproject.domain.repository.GroupChatRepository
import com.yerayyas.chatappkotlinproject.utils.AppState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para gestionar la mensajería grupal
 */
@HiltViewModel
class GroupChatViewModel @Inject constructor(
    private val groupChatRepository: GroupChatRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    private val sendGroupMessageUseCase: SendGroupMessageUseCase,
    private val getUserGroupsUseCase: GetUserGroupsUseCase,
    private val manageGroupMembersUseCase: ManageGroupMembersUseCase,
    private val cancelChatNotificationsUseCase: CancelChatNotificationsUseCase,
    private val appState: AppState
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val messages: StateFlow<List<GroupMessage>> = _messages.asStateFlow()

    private val _groupInfo = MutableStateFlow<GroupChat?>(null)
    val groupInfo: StateFlow<GroupChat?> = _groupInfo.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<User>>(emptyList())
    val groupMembers: StateFlow<List<User>> = _groupMembers.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    private val _replyToMessage = MutableStateFlow<GroupMessage?>(null)
    val replyToMessage: StateFlow<GroupMessage?> = _replyToMessage.asStateFlow()

    private val _mentionedUsers = MutableStateFlow<List<String>>(emptyList())
    val mentionedUsers: StateFlow<List<String>> = _mentionedUsers.asStateFlow()

    private val currentUserId = firebaseAuth.currentUser?.uid ?: ""
    private var currentGroupId: String = ""

    /**
     * Obtiene el ID del usuario actual
     */
    fun getCurrentUserId(): String = currentUserId

    /**
     * Inicializa el chat grupal con el ID específico
     */
    fun initializeGroupChat(groupId: String) {
        currentGroupId = groupId
        println("DEBUG: Initializing group chat for group: $groupId")

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                println("DEBUG: Starting group initialization")

                // Cancelar notificaciones del grupo
                cancelChatNotificationsUseCase.cancelGroupNotifications(groupId)

                // Cargar información del grupo
                loadGroupInfo(groupId)

                // Cargar miembros del grupo
                loadGroupMembers(groupId)

                // Cargar mensajes del grupo
                loadGroupMessages(groupId)

                // Marcar mensajes como leídos
                markMessagesAsRead(groupId)

                _uiState.value = _uiState.value.copy(isLoading = false)
                println("DEBUG: Group initialization completed successfully")
            } catch (e: Exception) {
                println("DEBUG: Error during group initialization: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar el chat grupal: ${e.message}"
                )
            }
        }
    }

    /**
     * Envía un mensaje al grupo
     */
    fun sendMessage() {
        val messageContent = _messageText.value.trim()
        if (messageContent.isEmpty() || currentGroupId.isEmpty()) return

        viewModelScope.launch {
            try {
                // Obtener nombre del usuario actual
                val currentUser = getCurrentUserInfo()
                val senderName = currentUser?.username ?: "Usuario"
                val senderImageUrl = currentUser?.profileImage

                // Procesar menciones
                val processedMessage = processMentions(messageContent)
                val mentions = extractMentions(processedMessage)

                val currentReplyTo = _replyToMessage.value

                val result = if (currentReplyTo != null) {
                    // Enviar como respuesta
                    sendGroupMessageUseCase.sendTextMessage(
                        groupId = currentGroupId,
                        message = processedMessage,
                        senderName = senderName,
                        senderImageUrl = senderImageUrl,
                        mentionedUsers = mentions,
                        replyToMessageId = currentReplyTo.id,
                        replyToMessage = currentReplyTo
                    )
                } else {
                    // Enviar mensaje normal
                    sendGroupMessageUseCase.sendTextMessage(
                        groupId = currentGroupId,
                        message = processedMessage,
                        senderName = senderName,
                        senderImageUrl = senderImageUrl,
                        mentionedUsers = mentions
                    )
                }

                if (result.isSuccess) {
                    // Limpiar texto y estado de respuesta
                    _messageText.value = ""
                    _replyToMessage.value = null
                    _mentionedUsers.value = emptyList()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Error al enviar mensaje: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al enviar mensaje: ${e.message}"
                )
            }
        }
    }

    /**
     * Envía un mensaje con imagen
     */
    fun sendImageMessage(imageUri: android.net.Uri, caption: String = "") {
        if (currentGroupId.isEmpty()) return

        viewModelScope.launch {
            try {
                val currentUser = getCurrentUserInfo()
                val senderName = currentUser?.username ?: "Usuario"
                val senderImageUrl = currentUser?.profileImage

                val currentReplyTo = _replyToMessage.value

                val result = sendGroupMessageUseCase.sendImageMessage(
                    groupId = currentGroupId,
                    imageUri = imageUri,
                    caption = caption,
                    senderName = senderName,
                    senderImageUrl = senderImageUrl,
                    mentionedUsers = extractMentions(caption),
                    replyToMessageId = currentReplyTo?.id,
                    replyToMessage = currentReplyTo
                )

                if (result.isSuccess) {
                    _replyToMessage.value = null
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Error al enviar imagen: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al enviar imagen: ${e.message}"
                )
            }
        }
    }

    /**
     * Actualiza el texto del mensaje
     */
    fun updateMessageText(text: String) {
        _messageText.value = text

        // Gestionar indicador de "escribiendo"
        if (text.isNotEmpty() && !_isTyping.value) {
            _isTyping.value = true
        } else if (text.isEmpty() && _isTyping.value) {
            _isTyping.value = false
        }

        // Actualizar menciones
        _mentionedUsers.value = extractMentions(text)
    }

    /**
     * Establece mensaje para responder
     */
    fun setReplyToMessage(message: GroupMessage) {
        _replyToMessage.value = message
    }

    /**
     * Cancela respuesta
     */
    fun cancelReply() {
        _replyToMessage.value = null
    }

    /**
     * Procesa menciones en el mensaje
     */
    private fun processMentions(text: String): String {
        val mentionPattern = "@(\\w+)".toRegex()
        return mentionPattern.replace(text) { matchResult ->
            val username = matchResult.groupValues[1]
            // Buscar usuario mencionado en los miembros del grupo
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

    /**
     * Extrae menciones del texto
     */
    private fun extractMentions(text: String): List<String> {
        val mentionPattern = "@(\\w+)".toRegex()
        return mentionPattern.findAll(text).mapNotNull { matchResult ->
            val username = matchResult.groupValues[1]
            _groupMembers.value.find {
                it.username.equals(username, ignoreCase = true)
            }?.id
        }.toList()
    }

    /**
     * Verifica si el usuario actual puede enviar mensajes
     */
    fun canSendMessages(): Boolean {
        val group = _groupInfo.value ?: return false
        return group.canSendMessages(currentUserId)
    }

    /**
     * Verifica si el usuario actual es administrador
     */
    fun isCurrentUserAdmin(): Boolean {
        val group = _groupInfo.value ?: return false
        return group.isAdmin(currentUserId)
    }

    /**
     * Marca mensajes como leídos
     */
    fun markMessagesAsRead(groupId: String) {
        viewModelScope.launch {
            try {
                val unreadMessages = _messages.value.filter { message ->
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

    /**
     * Busca mensajes en el grupo
     */
    fun searchMessages(query: String) {
        if (query.isEmpty()) {
            viewModelScope.launch {
                loadGroupMessages(currentGroupId)
            }
            return
        }

        viewModelScope.launch {
            try {
                val searchResults = groupChatRepository.searchGroupMessages(currentGroupId, query)
                _messages.value = searchResults
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al buscar mensajes: ${e.message}"
                )
            }
        }
    }

    /**
     * Añade reacción a un mensaje
     */
    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            try {
                // Actualizar localmente primero
                val updatedMessages = _messages.value.map { message ->
                    if (message.id == messageId) {
                        val currentReactions = message.reactions.toMutableMap()
                        val emojiReactions =
                            currentReactions[emoji]?.toMutableMap() ?: mutableMapOf()
                        emojiReactions[currentUserId] = System.currentTimeMillis()
                        currentReactions[emoji] = emojiReactions
                        message.copy(reactions = currentReactions)
                    } else {
                        message
                    }
                }
                _messages.value = updatedMessages

                // TODO: Enviar reacción a Firebase

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al añadir reacción: ${e.message}"
                )
            }
        }
    }

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

            // Cargar información de los miembros del grupo
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

            // Lanzar la colección en el viewModelScope para evitar bloquear
            viewModelScope.launch {
                groupChatRepository.getGroupMessages(groupId).collect { messagesList ->
                    println("DEBUG: Received ${messagesList.size} messages")
                    _messages.value = messagesList.sortedBy { it.timestamp }
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error loading messages: ${e.message}")
            // Si hay error, usar datos mock
            _messages.value = emptyList()
        }
    }

    private suspend fun getCurrentUserInfo(): User? {
        return try {
            userRepository.getCurrentUser()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtiene el AppState para gestionar el estado del chat abierto
     */
    fun getAppState(): AppState = appState

    /**
     * Limpia errores
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * Estado de UI para el chat grupal
 */
data class GroupChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = true,
    val isTyping: Boolean = false
)
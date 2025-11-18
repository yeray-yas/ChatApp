package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.GroupMessage
import com.yerayyas.chatappkotlinproject.data.model.GroupMessageType
import com.yerayyas.chatappkotlinproject.data.model.MessageType
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.domain.usecases.CancelChatNotificationsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.LoadChatMessagesUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendImageMessageReplyUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendImageMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendTextMessageReplyUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendTextMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.LoadGroupInfoUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.LoadGroupMessagesUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.MarkGroupMessagesAsReadUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.SendGroupImageMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.chat.group.SendGroupMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.GetCurrentUserIdUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.GetUsersByIdsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.user.LoadUserProfileUseCase
import com.yerayyas.chatappkotlinproject.utils.AppState
import com.yerayyas.chatappkotlinproject.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Chat type handled by the ViewModel
 */
sealed class ChatType {
    object Individual : ChatType()
    object Group : ChatType()
}

/**
 * Unified ViewModel for handling both individual and group chats.
 * Refactored to use Clean Architecture with UseCases.
 */
@HiltViewModel
class IndividualAndGroupChatViewModel @Inject constructor(
    // --- Use cases for individual chat ---
    private val loadChatMessagesUseCase: LoadChatMessagesUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    private val sendTextMessageReplyUseCase: SendTextMessageReplyUseCase,
    private val sendImageMessageReplyUseCase: SendImageMessageReplyUseCase,
    getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val cancelChatNotificationsUseCase: CancelChatNotificationsUseCase,

    // --- Use cases for group chat & User ---
    private val sendGroupMessageUseCase: SendGroupMessageUseCase,
    private val sendGroupImageMessageUseCase: SendGroupImageMessageUseCase,
    private val loadGroupInfoUseCase: LoadGroupInfoUseCase,
    private val loadGroupMessagesUseCase: LoadGroupMessagesUseCase,
    private val getUsersByIdsUseCase: GetUsersByIdsUseCase,
    private val loadUserProfileUseCase: LoadUserProfileUseCase,
    private val markGroupMessagesAsReadUseCase: MarkGroupMessagesAsReadUseCase,

    val appState: AppState
) : ViewModel() {

    // Common states
    private val _chatType = MutableStateFlow<ChatType>(ChatType.Individual)
    val chatType: StateFlow<ChatType> = _chatType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    // States for individual chat
    private val _individualMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _replyToIndividualMessage = MutableStateFlow<ChatMessage?>(null)
    val replyToIndividualMessage: StateFlow<ChatMessage?> = _replyToIndividualMessage.asStateFlow()

    private val _scrollToMessageId = MutableStateFlow<String?>(null)
    val scrollToMessageId: StateFlow<String?> = _scrollToMessageId.asStateFlow()

    private val _highlightedMessageId = MutableStateFlow<String?>(null)
    val highlightedMessageId: StateFlow<String?> = _highlightedMessageId.asStateFlow()

    // States for group chat
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

    // Unified states (exposed)
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

    private val currentUserId = getCurrentUserIdUseCase() ?: ""
    private var currentChatId: String = ""

    fun getCurrentUserId(): String = currentUserId

    fun initializeIndividualChat(otherUserId: String) {
        currentChatId = otherUserId
        _chatType.value = ChatType.Individual
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            cancelChatNotificationsUseCase(otherUserId)
        }

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

        appState.currentOpenChatUserId = otherUserId
    }

    fun initializeGroupChat(groupId: String) {
        currentChatId = groupId
        _chatType.value = ChatType.Group

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _individualMessages.value = emptyList()

                cancelChatNotificationsUseCase.cancelGroupNotifications(groupId)

                loadGroupInfo(groupId)
                loadGroupMembers(groupId)

                Log.d("CHAT_VM_LOG", "Attempting to mark messages as read for group $groupId")
                val result = markGroupMessagesAsReadUseCase(groupId)
                if (result.isSuccess) {
                    Log.d("CHAT_VM_LOG", "Successfully marked messages as read for group $groupId")
                } else {
                    Log.e(
                        "CHAT_VM_LOG",
                        "Failed to mark messages as read",
                        result.exceptionOrNull()
                    )
                }

                loadGroupMessages(groupId)

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error loading group chat: ${e.message}"
                _isLoading.value = false
            }
        }

        appState.currentOpenGroupChatId = groupId
    }

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
                val senderName = currentUser?.username ?: "User"
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
                    _error.value = "Error sending message: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                _error.value = "Error sending message: ${e.message}"
            }
        }
    }

    private fun sendGroupImageMessage(imageUri: Uri, caption: String = "") {
        if (currentChatId.isEmpty()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                val currentUser = getCurrentUserInfo()
                val senderId = getCurrentUserId()
                val senderName = currentUser?.username ?: "User"
                val senderImageUrl = currentUser?.profileImage

                val currentReplyTo = _replyToGroupMessage.value

                // Usamos el UseCase con los parámetros corregidos
                val result = sendGroupImageMessageUseCase(
                    groupId = currentChatId,
                    senderId = senderId,
                    senderName = senderName,
                    senderImageUrl = senderImageUrl,
                    imageUri = imageUri,
                    replyToMessage = currentReplyTo
                )

                if (result.isSuccess) {
                    clearReply()
                } else {
                    _error.value = "Error sending image: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                _error.value = "Error sending image: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text
        if (_chatType.value is ChatType.Group) {
            if (text.isNotEmpty() && !_isTyping.value) {
                _isTyping.value = true
            } else if (text.isEmpty() && _isTyping.value) {
                _isTyping.value = false
            }
            _mentionedUsers.value = extractMentions(text)
        }
    }

    fun setReplyToMessage(message: UnifiedMessage) {
        when (message) {
            is UnifiedMessage.Individual -> _replyToIndividualMessage.value = message.message
            is UnifiedMessage.Group -> _replyToGroupMessage.value = message.message
        }
    }

    fun clearReply() {
        _replyToIndividualMessage.value = null
        _replyToGroupMessage.value = null
    }

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

    fun canSendMessages(): Boolean {
        return when (_chatType.value) {
            is ChatType.Individual -> true
            is ChatType.Group -> {
                val group = _groupInfo.value ?: return false
                group.canSendMessages(currentUserId)
            }
        }
    }

    fun isCurrentUserAdmin(): Boolean {
        return when (_chatType.value) {
            is ChatType.Individual -> false
            is ChatType.Group -> {
                val group = _groupInfo.value ?: return false
                group.isAdmin(currentUserId)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun loadGroupInfo(groupId: String) {
        try {
            val group = loadGroupInfoUseCase(groupId)
            _groupInfo.value = group
        } catch (e: Exception) {
            throw Exception("Failed to load group information")
        }
    }

    private suspend fun loadGroupMembers(groupId: String) {
        try {
            val group = _groupInfo.value ?: return
            val memberIds = group.memberIds
            val members = getUsersByIdsUseCase(memberIds)
            _groupMembers.value = members
        } catch (e: Exception) {
            throw Exception("Failed to load group members")
        }
    }

    private fun loadGroupMessages(groupId: String) {
        try {
            viewModelScope.launch {
                loadGroupMessagesUseCase(groupId).collect { messagesList ->
                    _groupMessages.value = messagesList.sortedBy { it.timestamp }
                }
            }
        } catch (e: Exception) {
            _groupMessages.value = emptyList()
        }
    }

    private suspend fun getCurrentUserInfo(): User? {
        return try {
            loadUserProfileUseCase().getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun markMessagesAsRead(groupId: String) {
        viewModelScope.launch {
            markGroupMessagesAsReadUseCase(groupId)
                .onFailure { error ->
                    Log.e("GroupChatViewModel", "Failed to mark messages as read", error)
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

    fun isSentBy(userId: String): Boolean = senderId == userId

    fun getSenderName(): String? = when (this) {
        is Individual -> null
        is Group -> message.senderName
    }

    fun getReadStatus(): ReadStatus = when (this) {
        is Individual -> message.readStatus
        is Group -> message.readStatus
    }
}
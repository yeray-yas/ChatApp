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
 * Chat type handled by the ViewModel
 */
sealed class ChatType {
    object Individual : ChatType()
    object Group : ChatType()
}

/**
 * Unified ViewModel for handling both individual and group chats.
 *
 * This ViewModel provides a unified interface for managing different types of chat conversations,
 * handling message loading, sending, and various chat operations for both individual and group chats.
 * It follows Clean Architecture principles and uses use cases for business logic operations.
 *
 * Key features:
 * - Unified message handling for individual and group chats
 * - Reply functionality with message referencing
 * - Real-time message loading and state management
 * - Image and text message support
 * - Group-specific features like mentions and typing indicators
 * - Permission management for group chats
 * - Notification management integration
 *
 * @param loadChatMessagesUseCase Use case for loading individual chat messages
 * @param sendTextMessageUseCase Use case for sending text messages in individual chats
 * @param sendImageMessageUseCase Use case for sending image messages in individual chats
 * @param sendTextMessageReplyUseCase Use case for sending text replies in individual chats
 * @param sendImageMessageReplyUseCase Use case for sending image replies in individual chats
 * @param getCurrentUserIdUseCase Use case for getting current user ID
 * @param cancelChatNotificationsUseCase Use case for canceling chat notifications
 * @param groupChatRepository Repository for group chat operations
 * @param userRepository Repository for user data operations
 * @param firebaseAuth Firebase authentication instance
 * @param sendGroupMessageUseCase Use case for sending messages in group chats
 * @param getUserGroupsUseCase Use case for retrieving user's groups
 * @param manageGroupMembersUseCase Use case for managing group members
 * @param appState Application state for tracking current chat context
 */
@HiltViewModel
class IndividualAndGroupChatViewModel @Inject constructor(
    // Use cases for individual chat
    private val loadChatMessagesUseCase: LoadChatMessagesUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    private val sendTextMessageReplyUseCase: SendTextMessageReplyUseCase,
    private val sendImageMessageReplyUseCase: SendImageMessageReplyUseCase,
    getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val cancelChatNotificationsUseCase: CancelChatNotificationsUseCase,

    // Use cases for group chat
    private val groupChatRepository: GroupChatRepository,
    private val userRepository: UserRepository,
    firebaseAuth: FirebaseAuth,
    private val sendGroupMessageUseCase: SendGroupMessageUseCase,
    private val getUserGroupsUseCase: GetUserGroupsUseCase,
    private val manageGroupMembersUseCase: ManageGroupMembersUseCase,

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

    private val currentUserId = firebaseAuth.currentUser?.uid ?: getCurrentUserIdUseCase()
    private var currentChatId: String = ""

    /**
     * Get the current user ID
     */
    fun getCurrentUserId(): String = currentUserId

    /**
     * Initialize individual chat
     */
    fun initializeIndividualChat(otherUserId: String) {
        currentChatId = otherUserId
        _chatType.value = ChatType.Individual
        _isLoading.value = true
        _error.value = null

        // Cancel chat notifications
        viewModelScope.launch {
            cancelChatNotificationsUseCase(otherUserId)
        }

        // Clear group messages and load individual messages
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

        // Mark as open chat in AppState
        appState.currentOpenChatUserId = otherUserId
    }

    /**
     * Initialize group chat
     */
    fun initializeGroupChat(groupId: String) {
        currentChatId = groupId
        _chatType.value = ChatType.Group

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Clear individual messages
                _individualMessages.value = emptyList()

                // Cancel group notifications
                cancelChatNotificationsUseCase.cancelGroupNotifications(groupId)

                // Load group information
                loadGroupInfo(groupId)

                // Load group members
                loadGroupMembers(groupId)

                // Load group messages
                loadGroupMessages(groupId)

                // Mark messages as read
                markGroupMessagesAsRead(groupId)

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error loading group chat: ${e.message}"
                _isLoading.value = false
            }
        }

        // Mark as open group chat in AppState
        appState.currentOpenGroupChatId = groupId
    }

    /**
     * Send a message (text or image depending on chat type)
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
                val senderName = currentUser?.username ?: "User"
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
                    _error.value = "Error sending image: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                _error.value = "Error sending image: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update message text
     */
    fun updateMessageText(text: String) {
        _messageText.value = text

        // Only for group chats - manage typing indicator
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
     * Set message to reply to
     */
    fun setReplyToMessage(message: UnifiedMessage) {
        when (message) {
            is UnifiedMessage.Individual -> _replyToIndividualMessage.value = message.message
            is UnifiedMessage.Group -> _replyToGroupMessage.value = message.message
        }
    }

    /**
     * Cancel reply
     */
    fun clearReply() {
        _replyToIndividualMessage.value = null
        _replyToGroupMessage.value = null
    }

    /**
     * Scroll to original message (works for both individual and group chats)
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
     * Extract original message ID from a UnifiedMessage that is a reply
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
     * Check if the current user can send messages
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
     * Check if the current user is an administrator (only for groups)
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
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }

    // Private methods for group chat
    private suspend fun loadGroupInfo(groupId: String) {
        try {
            val group = getUserGroupsUseCase.getGroupById(groupId)
            _groupInfo.value = group
        } catch (e: Exception) {
            throw Exception("Failed to load group information")
        }
    }

    private suspend fun loadGroupMembers(groupId: String) {
        try {
            val group = _groupInfo.value ?: return
            val memberIds = group.memberIds
            val members = userRepository.getUsersByIds(memberIds)
            _groupMembers.value = members
        } catch (e: Exception) {
            throw Exception("Failed to load group members")
        }
    }

    private fun loadGroupMessages(groupId: String) {
        try {
            viewModelScope.launch {
                groupChatRepository.getGroupMessages(groupId).collect { messagesList ->
                    _groupMessages.value = messagesList.sortedBy { it.timestamp }
                }
            }
        } catch (e: Exception) {
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
        // Clear states from AppState
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
 * Unified message that can be either individual or group
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
     * Check if the message was sent by a specific user
     */
    fun isSentBy(userId: String): Boolean = senderId == userId

    /**
     * Get the sender's name (only for group messages)
     */
    fun getSenderName(): String? = when (this) {
        is Individual -> null
        is Group -> message.senderName
    }

    /**
     * Get the read status
     */
    fun getReadStatus(): ReadStatus = when (this) {
        is Individual -> message.readStatus
        is Group -> message.readStatus
    }
}
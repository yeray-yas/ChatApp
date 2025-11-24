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
import java.util.UUID
import javax.inject.Inject

/**
 * Defines the type of the active chat session.
 */
sealed class ChatType {
    object Individual : ChatType()
    object Group : ChatType()
}

/**
 * Represents the visual state of a message's delivery process.
 * Used to toggle between loading spinners, sent checks, or error indicators in the UI.
 */
enum class MessageDeliveryStatus {
    /** Message is currently being uploaded or processed (Show Spinner). */
    SENDING,
    /** Message has been successfully sent to the server (Show Check/Hide Spinner). */
    SENT,
    /** An error occurred during delivery (Show Retry icon). */
    ERROR
}

/**
 * Central ViewModel managing state and business logic for both Individual (1-on-1)
 * and Group chat interfaces.
 *
 * It handles message retrieval, sending (text/media), replies, optimistic UI updates,
 * and group-specific features like mentions and admin permissions.
 */
@HiltViewModel
class IndividualAndGroupChatViewModel @Inject constructor(
    private val loadChatMessagesUseCase: LoadChatMessagesUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    private val sendTextMessageReplyUseCase: SendTextMessageReplyUseCase,
    private val sendImageMessageReplyUseCase: SendImageMessageReplyUseCase,
    getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val cancelChatNotificationsUseCase: CancelChatNotificationsUseCase,
    private val sendGroupMessageUseCase: SendGroupMessageUseCase,
    private val sendGroupImageMessageUseCase: SendGroupImageMessageUseCase,
    private val loadGroupInfoUseCase: LoadGroupInfoUseCase,
    private val loadGroupMessagesUseCase: LoadGroupMessagesUseCase,
    private val getUsersByIdsUseCase: GetUsersByIdsUseCase,
    private val loadUserProfileUseCase: LoadUserProfileUseCase,
    private val markGroupMessagesAsReadUseCase: MarkGroupMessagesAsReadUseCase,
    val appState: AppState
) : ViewModel() {

    // region Common States
    private val _chatType = MutableStateFlow<ChatType>(ChatType.Individual)
    val chatType: StateFlow<ChatType> = _chatType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    // endregion

    // region Individual Chat States
    private val _individualMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _replyToIndividualMessage = MutableStateFlow<ChatMessage?>(null)
    val replyToIndividualMessage: StateFlow<ChatMessage?> = _replyToIndividualMessage.asStateFlow()

    private val _scrollToMessageId = MutableStateFlow<String?>(null)
    val scrollToMessageId: StateFlow<String?> = _scrollToMessageId.asStateFlow()

    private val _highlightedMessageId = MutableStateFlow<String?>(null)
    val highlightedMessageId: StateFlow<String?> = _highlightedMessageId.asStateFlow()
    // endregion

    // region Group Chat States
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
    // endregion

    /**
     * Local list for messages currently being uploaded (Optimistic UI).
     * These allow displaying the message/image immediately before the server confirms receipt.
     */
    private val _pendingMessages = MutableStateFlow<List<UnifiedMessage>>(emptyList())

    private val currentUserId = getCurrentUserIdUseCase() ?: ""
    private var currentChatId: String = ""

    /**
     * Unified stream of messages for the UI.
     *
     * Combines server messages (Individual or Group) with local pending messages.
     * It implements logic to filter out server-side "placeholder" messages (PENDING_UPLOAD)
     * if they belong to the current user, preventing duplicate bubbles while the local
     * version is being displayed.
     */
    val messages: StateFlow<List<UnifiedMessage>> = combine(
        _individualMessages,
        _groupMessages,
        _chatType,
        _pendingMessages
    ) { individualMsgs, groupMsgs, type, pendingMsgs ->

        // 1. Convert server messages to Unified type and filter out "ghosts" (server placeholders)
        val serverMessages = when (type) {
            is ChatType.Individual -> {
                individualMsgs.filterNot { msg ->
                    // If it's my message and marked as uploading by server, hide it
                    // (we show the local pending version instead).
                    msg.senderId == currentUserId && msg.imageUrl == "PENDING_UPLOAD"
                }.map { UnifiedMessage.Individual(it) }
            }
            is ChatType.Group -> {
                groupMsgs.filterNot { msg ->
                    msg.senderId == currentUserId && msg.imageUrl == "PENDING_UPLOAD"
                }.map { UnifiedMessage.Group(it) }
            }
        }

        // 2. Combine Server messages (cleaned) + Pending local messages
        val allMessages = serverMessages + pendingMsgs

        // 3. Sort by timestamp for correct display order
        allMessages.sortedBy { it.timestamp }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Unified stream for the message being replied to, adapting based on ChatType.
     */
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

    fun getCurrentUserId(): String = currentUserId

    /**
     * Initializes the ViewModel for an Individual (1-on-1) chat session.
     * Clears previous state, loads messages, and handles notifications.
     */
    fun initializeIndividualChat(otherUserId: String) {
        currentChatId = otherUserId
        _chatType.value = ChatType.Individual
        _isLoading.value = true
        _error.value = null
        _pendingMessages.value = emptyList()

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

    /**
     * Initializes the ViewModel for a Group chat session.
     * Loads group info, members, and messages.
     */
    fun initializeGroupChat(groupId: String) {
        currentChatId = groupId
        _chatType.value = ChatType.Group
        _pendingMessages.value = emptyList()

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _individualMessages.value = emptyList()

                cancelChatNotificationsUseCase.cancelGroupNotifications(groupId)

                loadGroupInfo(groupId)
                loadGroupMembers(groupId)

                markGroupMessagesAsReadUseCase(groupId)
                loadGroupMessages(groupId)

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error loading group chat: ${e.message}"
                _isLoading.value = false
            }
        }

        appState.currentOpenGroupChatId = groupId
    }

    /**
     * Main entry point for sending a message. Routes logic based on ChatType and content (Text/Image).
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
        val textToSend = _messageText.value.trim()
        if (textToSend.isEmpty()) return

        _messageText.value = ""

        viewModelScope.launch {
            runCatching {
                val currentReplyTo = _replyToIndividualMessage.value
                if (currentReplyTo != null) {
                    sendTextMessageReplyUseCase(receiverId, textToSend, currentReplyTo)
                    clearReply()
                } else {
                    sendTextMessageUseCase(receiverId, textToSend)
                }
            }.onFailure { e ->
                _error.value = "${Constants.ERROR_SENDING_MESSAGE}: ${e.message}"
                _messageText.value = textToSend
            }
        }
    }

    /**
     * Sends an image in a 1-on-1 chat using Optimistic UI.
     * Creates a local `Pending` message immediately before initiating the upload.
     */
    private fun sendIndividualImageMessage(receiverId: String, uri: Uri) {
        val tempId = UUID.randomUUID().toString()
        val pendingMessage = UnifiedMessage.Pending(
            id = tempId,
            senderId = currentUserId,
            localUri = uri,
            timestamp = System.currentTimeMillis(),
            isReply = _replyToIndividualMessage.value != null,
            content = "Image"
        )

        _pendingMessages.value = _pendingMessages.value + pendingMessage

        viewModelScope.launch {
            runCatching {
                val currentReplyTo = _replyToIndividualMessage.value
                if (currentReplyTo != null) {
                    sendImageMessageReplyUseCase(receiverId, uri, currentReplyTo)
                    clearReply()
                } else {
                    sendImageMessageUseCase(receiverId, uri)
                }
            }.onSuccess {
                // Server sync will bring the real message via Flow. Remove pending item.
                _pendingMessages.value = _pendingMessages.value.filter { it.id != tempId }
            }.onFailure { e ->
                _pendingMessages.value = _pendingMessages.value.filter { it.id != tempId }
                _error.value = "${Constants.ERROR_SENDING_IMAGE}: ${e.message}"
            }
        }
    }

    private fun sendGroupTextMessage() {
        val textToSend = _messageText.value.trim()
        if (textToSend.isEmpty() || currentChatId.isEmpty()) return

        _messageText.value = ""

        val currentReplyTo = _replyToGroupMessage.value
        clearReply()
        _mentionedUsers.value = emptyList()
        if (_chatType.value is ChatType.Group) {
            _isTyping.value = false
        }

        viewModelScope.launch {
            try {
                val currentUser = getCurrentUserInfo()
                val senderName = currentUser?.username ?: "User"
                val senderImageUrl = currentUser?.profileImage

                val processedMessage = processMentions(textToSend)
                val mentions = extractMentions(processedMessage)

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

                if (!result.isSuccess) {
                    _error.value = "Error sending message: ${result.exceptionOrNull()?.message}"
                    _messageText.value = textToSend
                }
            } catch (e: Exception) {
                _error.value = "Error sending message: ${e.message}"
            }
        }
    }

    private fun sendGroupImageMessage(imageUri: Uri, caption: String = "") {
        if (currentChatId.isEmpty()) return

        val tempId = UUID.randomUUID().toString()
        val pendingMessage = UnifiedMessage.Pending(
            id = tempId,
            senderId = currentUserId,
            localUri = imageUri,
            timestamp = System.currentTimeMillis(),
            isReply = _replyToGroupMessage.value != null,
            content = "Image"
        )

        _pendingMessages.value = _pendingMessages.value + pendingMessage

        viewModelScope.launch {
            try {
                val currentUser = getCurrentUserInfo()
                val senderId = getCurrentUserId()
                val senderName = currentUser?.username ?: "User"
                val senderImageUrl = currentUser?.profileImage

                val currentReplyTo = _replyToGroupMessage.value

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
                    _pendingMessages.value = _pendingMessages.value.filter { it.id != tempId }
                } else {
                    _pendingMessages.value = _pendingMessages.value.filter { it.id != tempId }
                    _error.value = "Error sending image: ${result.exceptionOrNull()?.message}"
                }

            } catch (e: Exception) {
                _pendingMessages.value = _pendingMessages.value.filter { it.id != tempId }
                _error.value = "Error sending image: ${e.message}"
            }
        }
    }

    /**
     * Updates the current message input text.
     *
     * For group chats, this also manages the typing status logic and triggers
     * real-time mention extraction to update the suggestion list.
     */
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

    /**
     * Sets the message to be replied to, updating the UI to show the reply banner.
     */
    fun setReplyToMessage(message: UnifiedMessage) {
        when (message) {
            is UnifiedMessage.Individual -> _replyToIndividualMessage.value = message.message
            is UnifiedMessage.Group -> _replyToGroupMessage.value = message.message
            is UnifiedMessage.Pending -> { /* No reply to pending messages allowed */ }
        }
    }

    /**
     * Clears the current reply state, hiding the reply banner.
     */
    fun clearReply() {
        _replyToIndividualMessage.value = null
        _replyToGroupMessage.value = null
    }

    /**
     * Triggers a temporary scroll-and-highlight animation to locate a specific message.
     *
     * Useful when a user taps on a reply quote to jump to the original message.
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
     * Helper to extract the original message ID from a reply message wrapper.
     * Returns null if the message is not a reply or is pending.
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
            is UnifiedMessage.Pending -> null
        }
    }

    /**
     * Checks if the current user has permission to send messages in the active chat.
     * Crucial for read-only groups or banned users.
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
     * Checks if the current user is an administrator of the active group chat.
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
     * Clears any displayed error messages from the UI state.
     */
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

    /**
     * Marks all messages in the current group as read.
     * Should be called when the chat screen is opened or resumed.
     */
    fun markMessagesAsRead(groupId: String) {
        viewModelScope.launch {
            markGroupMessagesAsReadUseCase(groupId)
                .onFailure { error ->
                    Log.e("GroupChatViewModel", "Failed to mark messages as read", error)
                }
        }
    }

    /**
     * Processes the message text to replace @username mentions with formatted
     * text suitable for display or storage.
     */
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

    /**
     * Extracts User IDs from the text based on @username patterns.
     * Used to notify users that they have been mentioned.
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
     * Cleans up resources and global state when the ViewModel is destroyed.
     * Resets the currently open chat ID in AppState to prevent notification logic errors.
     */
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

/**
 * Polymorphic wrapper that unifies distinct message types (Individual, Group, and Pending)
 * into a single contract for the UI.
 *
 * This enables the ChatScreen to render a single list containing messages from different
 * sources without needing to know the implementation details of each.
 */
sealed class UnifiedMessage {
    abstract val id: String
    abstract val senderId: String
    abstract val content: String
    abstract val timestamp: Long
    abstract val messageType: MessageType
    abstract val imageUrl: String?
    abstract val localUri: Uri?
    abstract val isReply: Boolean
    abstract val deliveryStatus: MessageDeliveryStatus

    /**
     * Wrapper for standard 1-on-1 chat messages.
     */
    data class Individual(val message: ChatMessage) : UnifiedMessage() {
        override val id: String = message.id
        override val senderId: String = message.senderId
        override val content: String = message.message
        override val timestamp: Long = message.timestamp
        override val messageType: MessageType = message.messageType
        override val imageUrl: String? = message.imageUrl
        override val localUri: Uri? = null
        override val isReply: Boolean = message.isReply()
        override val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT
    }

    /**
     * Wrapper for group chat messages.
     */
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
        override val localUri: Uri? = null
        override val isReply: Boolean = message.isReply()
        override val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT
    }

    /**
     * Represents a message currently being uploaded (Optimistic UI state).
     * Contains a local URI to display the image before the server confirms the upload.
     */
    data class Pending(
        override val id: String,
        override val senderId: String,
        override val localUri: Uri?,
        override val timestamp: Long,
        override val isReply: Boolean,
        override val content: String = "Image"
    ) : UnifiedMessage() {
        override val messageType: MessageType = MessageType.IMAGE
        override val imageUrl: String? = null
        override val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENDING
    }

    fun isSentBy(userId: String): Boolean = senderId == userId

    fun getSenderName(): String? = when (this) {
        is Individual -> null
        is Group -> message.senderName
        is Pending -> "Me"
    }

    fun getReadStatus(): ReadStatus = when (this) {
        is Individual -> message.readStatus
        is Group -> message.readStatus
        is Pending -> ReadStatus.SENT
    }
}

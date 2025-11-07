package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.usecases.CancelChatNotificationsUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.GetCurrentUserIdUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.LoadChatMessagesUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendImageMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendImageMessageReplyUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendTextMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendTextMessageReplyUseCase
import com.yerayyas.chatappkotlinproject.utils.AppState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the chat screen.
 *
 * Orchestrates message loading, sending, and error handling,
 * and exposes UI state flows for messages, loading status, and errors.
 * Also interacts with AppState to track the currently open chat and
 * cancels any pending notifications for that chat.
 *
 * @property loadChatMessagesUseCase Fetches a stream of chat messages.
 * @property sendTextMessageUseCase Sends text messages to a recipient.
 * @property sendImageMessageUseCase Sends image messages to a recipient.
 * @property sendTextMessageReplyUseCase Sends text message replies to a recipient.
 * @property sendImageMessageReplyUseCase Sends image message replies to a recipient.
 * @property getCurrentUserIdUseCase Retrieves the current user's ID.
 * @property cancelChatNotificationsUseCase Cancels notifications when opening a chat.
 * @property appState Global application state, including foreground status and current chat.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val loadChatMessagesUseCase: LoadChatMessagesUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    private val sendTextMessageReplyUseCase: SendTextMessageReplyUseCase,
    private val sendImageMessageReplyUseCase: SendImageMessageReplyUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val cancelChatNotificationsUseCase: CancelChatNotificationsUseCase,
    val appState: AppState
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    /** Flow of the current chat message list. */
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /** Flow indicating whether a chat operation is in progress. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    /** Flow emitting error messages to display in the UI. */
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _replyToMessage = MutableStateFlow<ChatMessage?>(null)

    /** Flow emitting the message being replied to, null if not replying. */
    val replyToMessage: StateFlow<ChatMessage?> = _replyToMessage.asStateFlow()

    private val _scrollToMessageId = MutableStateFlow<String?>(null)

    /** Flow emitting the ID of the message to scroll to. */
    val scrollToMessageId: StateFlow<String?> = _scrollToMessageId.asStateFlow()

    private val _highlightedMessageId = MutableStateFlow<String?>(null)

    /** Flow emitting the ID of the message that should be highlighted. */
    val highlightedMessageId: StateFlow<String?> = _highlightedMessageId.asStateFlow()

    /**
     * Returns the current user's ID for UI logic.
     */
    fun getCurrentUserId(): String = getCurrentUserIdUseCase()

    /**
     * Begins collecting messages for the specified [otherUserId] and
     * cancels any notifications related to this chat.
     * Updates loading and error states accordingly.
     *
     * @param otherUserId ID of the chat partner.
     */
    fun loadMessages(otherUserId: String) {
        _isLoading.value = true
        _error.value = null

        // Cancel system notifications for this chat in a separate coroutine
        viewModelScope.launch {
            cancelChatNotificationsUseCase(otherUserId)
        }

        loadChatMessagesUseCase(otherUserId)
            .onEach { messagesList ->
                _messages.value = messagesList
                _isLoading.value = false
            }
            .catch { exception ->
                _error.value = "Error loading messages: ${exception.message}"
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    /**
     * Sends a text message to [receiverId] with the given [text].
     * If there's a message being replied to, sends as a reply instead.
     * Trimmed blank messages are ignored.
     * Errors are surfaced via [error] flow.
     *
     * @param receiverId ID of the message recipient.
     * @param text The message content to send.
     */
    fun sendMessage(receiverId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                val currentReplyTo = _replyToMessage.value
                if (currentReplyTo != null) {
                    sendTextMessageReplyUseCase(receiverId, trimmed, currentReplyTo)
                    clearReply()
                } else {
                    sendTextMessageUseCase(receiverId, trimmed)
                }
            }.onFailure { e ->
                _error.value = "Error sending message: ${e.message}"
            }
        }
    }

    /**
     * Sends an image message to [receiverId] using the provided [uri].
     * If there's a message being replied to, sends as a reply instead.
     * While sending, [isLoading] is set to true. Errors are surfaced via [error] flow.
     *
     * @param receiverId ID of the message recipient.
     * @param uri URI of the image to send.
     */
    fun sendImage(receiverId: String, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val currentReplyTo = _replyToMessage.value
                if (currentReplyTo != null) {
                    sendImageMessageReplyUseCase(receiverId, uri, currentReplyTo)
                    clearReply()
                } else {
                    sendImageMessageUseCase(receiverId, uri)
                }
            }.onFailure { e ->
                _error.value = "Error sending image: ${e.message}"
            }.also {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sets the message to reply to.
     *
     * @param message The message to reply to.
     */
    fun setReplyToMessage(message: ChatMessage) {
        _replyToMessage.value = message
    }

    /**
     * Clears the current reply state.
     */
    fun clearReply() {
        _replyToMessage.value = null
    }

    /**
     * Scrolls to and highlights the original message when a reply preview is tapped.
     *
     * @param messageId The ID of the original message to scroll to.
     */
    fun scrollToOriginalMessage(messageId: String) {
        viewModelScope.launch {
            // Trigger scroll
            _scrollToMessageId.value = messageId

            // Start highlight animation
            _highlightedMessageId.value = messageId

            // Clear scroll trigger after a short delay
            delay(100)
            _scrollToMessageId.value = null

            // Clear highlight after animation duration
            delay(2000) // 2 seconds highlight duration
            _highlightedMessageId.value = null
        }
    }

    /**
     * Clears any current error message.
     */
    fun clearError() {
        _error.value = null
    }
}

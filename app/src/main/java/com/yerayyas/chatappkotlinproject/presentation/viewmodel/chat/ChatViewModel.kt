package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.usecases.GetCurrentUserIdUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.LoadChatMessagesUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendTextMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecases.SendImageMessageUseCase
import com.yerayyas.chatappkotlinproject.utils.AppState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * ViewModel for the chat screen.
 *
 * Delegates business logic to Use Cases and exposes UI state:
 * - messages: latest chat messages
 * - isLoading: loading indicator
 * - error: error messages
 *
 * @param loadChatMessagesUseCase    Use Case to fetch message stream.
 * @param sendTextMessageUseCase     Use Case to send text messages.
 * @param sendImageMessageUseCase    Use Case to send image messages.
 * @param appState                   Tracks app foreground/background and current chat.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val loadChatMessagesUseCase: LoadChatMessagesUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val sendImageMessageUseCase: SendImageMessageUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    val appState: AppState
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Exposes the current user ID for the UI.
     */
    fun getCurrentUserId(): String = getCurrentUserIdUseCase()

    /**
     * Starts collecting messages for [otherUserId].
     */
    fun loadMessages(otherUserId: String) {
        _isLoading.value = true
        _error.value = null

        loadChatMessagesUseCase(otherUserId)
            .onEach { messagesList: List<ChatMessage> ->
                _messages.value = messagesList
                _isLoading.value = false
            }
            .catch { exception: Throwable ->
                _error.value = "Error loading messages: ${exception.message}"
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    /**
     * Sends a text message.
     */
    fun sendMessage(receiverId: String, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            runCatching {
                sendTextMessageUseCase(receiverId, text)
            }.onFailure { e ->
                _error.value = "Error sending message: ${e.message}"
            }
        }
    }

    /**
     * Sends an image message.
     */
    fun sendImage(receiverId: String, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true

            runCatching {
                sendImageMessageUseCase(receiverId, uri)
            }.onFailure { e ->
                _error.value = "Error sending image: ${e.message}"
            }.also {
                _isLoading.value = false
            }
        }
    }

    /** Clears the current error state. */
    fun clearError() {
        _error.value = null
    }
}

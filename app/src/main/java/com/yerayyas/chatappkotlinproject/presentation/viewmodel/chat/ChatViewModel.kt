package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ChatViewModel"

/**
 * ViewModel for the chat screen.
 * Manages presentation logic and UI state.
 *
 * This ViewModel handles fetching chat messages, sending text messages, and sending images.
 * It uses a `ChatRepository` to interact with the backend and Firebase.
 * The UI state includes messages, loading status, and error handling.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    /**
     * A flow of chat messages to be displayed in the UI.
     */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /**
     * A flow representing the loading state of the chat.
     * True when messages are being loaded, false otherwise.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * A flow for error messages to be shown in the UI.
     * It holds an error message if something goes wrong, or null if no error.
     */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var otherUserId: String? = null

    /**
     * Retrieves the ID of the currently authenticated user.
     *
     * @return The ID of the current user, or an empty string if no user is authenticated.
     */
    fun getCurrentUserId(): String = chatRepository.getCurrentUserId()

    /**
     * Loads messages from the chat with another user.
     *
     * @param otherUserId The ID of the other user in the chat.
     */
    fun loadMessages(otherUserId: String) {
        this.otherUserId = otherUserId

        _isLoading.value = true
        _error.value = null

        chatRepository.getMessages(otherUserId)
            .onEach { messagesList ->
                _messages.value = messagesList
                _isLoading.value = false
            }
            .catch { e ->
                Log.e(TAG, "Error loading messages", e)
                _error.value = "Error loading messages: ${e.message}"
                _isLoading.value = false
            }
            .onCompletion {
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

    /**
     * Sends a text message to another user.
     *
     * @param receiverId The ID of the receiving user.
     * @param messageText The content of the message to send.
     */
    fun sendMessage(receiverId: String, messageText: String) {
        if (messageText.isBlank()) return

        viewModelScope.launch {
            try {
                _error.value = null
                chatRepository.sendTextMessage(receiverId, messageText)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                _error.value = "Error sending message: ${e.message}"
            }
        }
    }

    /**
     * Sends an image to another user.
     *
     * @param receiverId The ID of the receiving user.
     * @param imageUri The URI of the image to send.
     */
    fun sendImage(receiverId: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                chatRepository.sendImageMessage(receiverId, imageUri)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending image", e)
                _error.value = "Error sending image: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

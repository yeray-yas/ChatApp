package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import com.yerayyas.chatappkotlinproject.domain.usecase.LoadMessagesUseCase
import com.yerayyas.chatappkotlinproject.domain.usecase.SendMediaMessageUseCase
import com.yerayyas.chatappkotlinproject.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMediaMessageUseCase: SendMediaMessageUseCase,
    private val loadMessagesUseCase: LoadMessagesUseCase
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun sendMessage(receiverId: String, messageText: String) {
        viewModelScope.launch {
            sendMessageUseCase(receiverId, messageText)
        }
    }

    fun sendMediaMessage(receiverId: String, fileUri: Uri, messageType: String) {
        viewModelScope.launch {
            sendMediaMessageUseCase(receiverId, fileUri, messageType)
        }
    }

    fun loadMessages(receiverId: String) {
        viewModelScope.launch {
            loadMessagesUseCase(receiverId).collect { messagesList ->
                _messages.value = messagesList
            }
        }
    }
}



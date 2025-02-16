package com.yerayyas.chatappkotlinproject.presentation.viewmodel.chat

import androidx.lifecycle.ViewModel
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun sendMessage(userId: String, messageText: String) {
        if (messageText.isNotBlank()) {
            /*val newMessage = ChatMessage(message = messageText, isFromCurrentUser = true)
            _messages.value = listOf(newMessage) + _messages.value*/
        }
    }
}

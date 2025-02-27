package com.yerayyas.chatappkotlinproject.domain

import android.net.Uri
import com.yerayyas.chatappkotlinproject.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(receiverId: String, messageText: String)
    suspend fun sendMediaMessage(receiverId: String, fileUri: Uri, messageType: String)
    fun loadMessages(receiverId: String): Flow<List<ChatMessage>>
}

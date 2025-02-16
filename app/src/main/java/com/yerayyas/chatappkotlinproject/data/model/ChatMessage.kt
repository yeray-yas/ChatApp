package com.yerayyas.chatappkotlinproject.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

package com.yerayyas.chatappkotlinproject.data.model

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val mediaUrl: String? = null, // URL del archivo (imagen o vídeo)
    val messageType: String = "text", // "text", "image", "video"
    val timestamp: Long = System.currentTimeMillis()
)


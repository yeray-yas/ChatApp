package com.yerayyas.chatappkotlinproject.data.model

data class ChatListItem(
    val chatId: String,
    val otherUserId: String,
    val otherUsername: String,
    val lastMessage: String,
    val timestamp: Long
) 
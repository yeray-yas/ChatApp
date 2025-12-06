package com.yerayyas.chatappkotlinproject.domain.model

data class ChatHeaderInfo(
    val title: String,
    val imageUrl: String?,
    val subtitle: String? = null
)
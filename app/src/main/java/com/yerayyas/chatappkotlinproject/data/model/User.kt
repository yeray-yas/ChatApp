package com.yerayyas.chatappkotlinproject.data.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val profileImage: String,
    val status: String,
    val isOnline: Boolean,
    val lastSeen: Long = 0
)

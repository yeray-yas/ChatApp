package com.yerayyas.chatappkotlinproject.notifications

data class NotificationNavigationState(
    val navigateTo: String,
    val userId: String,
    val username: String,
    val eventId: Long = System.currentTimeMillis(),
    val skipSplash: Boolean = false,
    val isInitialDestination: Boolean = false
)

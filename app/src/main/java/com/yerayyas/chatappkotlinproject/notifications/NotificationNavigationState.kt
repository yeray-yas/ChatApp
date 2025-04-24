package com.yerayyas.chatappkotlinproject.notifications

// Define un estado para la navegación pendiente desde una notificación
// Puedes usar un sealed class si prevés más tipos de navegación externa
data class NotificationNavigationState(
    val navigateTo: String? = null,
    val userId: String? = null,
    val username: String? = null,
    // Añade un ID único o timestamp para que cada evento sea distinto si usas StateFlow
    val eventId: Long = System.currentTimeMillis()
)
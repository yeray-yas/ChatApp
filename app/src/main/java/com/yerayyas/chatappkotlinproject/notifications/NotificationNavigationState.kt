package com.yerayyas.chatappkotlinproject.notifications

/**
 * Represents a pending navigation event, typically triggered by a push notification.
 *
 * This data class holds all the necessary information to perform a navigation action,
 * such as the target destination route and any required parameters. The `eventId` is used
 * by the UI layer to ensure that the navigation event is consumed only once, preventing
 * duplicate navigations on configuration changes.
 *
 * @property navigateTo The destination route key (e.g., "chat").
 * @property userId The unique identifier of the user to navigate to (e.g., the chat partner).
 * @property username The display name of the user, passed as an argument to the destination screen.
 * @property eventId A unique timestamp or identifier for this specific navigation event to prevent re-consumption.
 * @property skipSplash A flag indicating whether the splash screen should be bypassed during navigation.
 * @property isInitialDestination A flag indicating if this navigation is the first one after app launch.
 */
data class NotificationNavigationState(
    val navigateTo: String,
    val userId: String,
    val username: String,
    val eventId: Long = System.currentTimeMillis(),
    val skipSplash: Boolean = false,
    val isInitialDestination: Boolean = false
)
